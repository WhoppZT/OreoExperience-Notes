package com.oreoexperience.notes.data.ai

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.readUTF8Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

sealed class AiChunk {
    data class Delta(val text: String) : AiChunk()
    data object Done : AiChunk()
    data class Error(val message: String) : AiChunk()
}

class GeminiClient(
    private val apiKey: () -> String,
    private val temperature: () -> Float = { 0.3f },
) {
    private val httpClient = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    // Rate-limit tracking: sliding window de 60s, límite 15 requests.
    private val requestTimestamps = mutableListOf<Long>()
    
    // Tiempo de espera indicado por Google (header Retry-After o cuerpo del error).
    @Volatile
    private var googleRetryAfterSeconds: Long = 0

    /** Cantidad de requests usados en la ventana actual. */
    fun getRequestsUsed(): Int = synchronized(requestTimestamps) {
        val now = System.currentTimeMillis()
        requestTimestamps.removeAll { it < now - 60_000 }
        requestTimestamps.size
    }

    /** Segundos restantes hasta que se libere un slot (0 si hay espacio). */
    fun getCooldownSeconds(): Long {
        // Primero chequear el tiempo que dijo Google
        if (googleRetryAfterSeconds > 0) {
            return googleRetryAfterSeconds
        }
        // Fallback al sliding window local
        return synchronized(requestTimestamps) {
            val now = System.currentTimeMillis()
            requestTimestamps.removeAll { it < now - 60_000 }
            if (requestTimestamps.size < 15) 0L
            else {
                val oldest = requestTimestamps.first()
                ((oldest + 60_000 - now) / 1000).coerceAtLeast(1)
            }
        }
    }

    /** Si hay espacio en la ventana, registra el request y devuelve true. */
    private fun tryReserveSlot(): Boolean = synchronized(requestTimestamps) {
        val now = System.currentTimeMillis()
        requestTimestamps.removeAll { it < now - 60_000 }
        if (requestTimestamps.size < 15) {
            requestTimestamps.add(now)
            true
        } else {
            false
        }
    }

    fun stream(system: String, user: String): Flow<AiChunk> = flow {
        val key = apiKey().trim()
        if (key.isBlank()) {
            emit(AiChunk.Error("Configurá tu API key en Ajustes"))
            return@flow
        }

        if (!tryReserveSlot()) {
            val cooldown = getCooldownSeconds()
            emit(AiChunk.Error("Límite de 15 requests/minuto alcanzado. Esperá ${cooldown}s."))
            return@flow
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:streamGenerateContent?alt=sse&key=$key"

        val bodyJson = buildJsonObject {
            putJsonObject("systemInstruction") {
                put("parts", kotlinx.serialization.json.buildJsonArray {
                    add(buildJsonObject { put("text", system) })
                })
            }
            put("contents", kotlinx.serialization.json.buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", kotlinx.serialization.json.buildJsonArray {
                        add(buildJsonObject { put("text", user) })
                    })
                })
            })
            putJsonObject("generationConfig") {
                put("temperature", temperature())
                put("maxOutputTokens", 8192)
            }
        }

        val response: HttpResponse = try {
            httpClient.post(url) {
                contentType(ContentType.Application.Json)
                header("Content-Type", "application/json")
                setBody(bodyJson.toString())
            }
        } catch (e: Exception) {
            emit(AiChunk.Error(mapNetworkError(e)))
            return@flow
        }

        if (response.status.value !in 200..299) {
            // Si es 429, leer header Retry-After y cuerpo del error
            if (response.status.value == 429) {
                val retryAfterHeader = response.headers[HttpHeaders.RetryAfter]
                val retrySeconds = retryAfterHeader?.toLongOrNull()
                if (retrySeconds != null && retrySeconds > 0) {
                    googleRetryAfterSeconds = retrySeconds
                }
                
                // También intentar leer el cuerpo para más detalles
                val errorBody = runCatching { response.bodyAsText() }.getOrNull()
                val serverMessage = parseGoogleError(errorBody)
                
                val waitTime = googleRetryAfterSeconds
                val baseMsg = serverMessage ?: "Llegaste al límite de requests por minuto."
                val msg = if (waitTime > 0) {
                    "$baseMsg Esperá ${waitTime}s."
                } else {
                    "$baseMsg Esperá un momento."
                }
                emit(AiChunk.Error(msg))
            } else {
                emit(AiChunk.Error(mapHttpError(response.status.value)))
            }
            return@flow
        }

        val channel = response.bodyAsChannel()
        // readUTF8Line() es BLOQUEANTE: espera hasta recibir \n o que se cierre
        // el canal. Esto permite streaming real sin cortar prematuramente.
        var line: String?
        while (channel.readUTF8Line().also { line = it } != null) {
            val trimmed = line!!.trim()
            if (!trimmed.startsWith("data:")) continue

            val payload = trimmed.substringAfter("data:").trim()
            if (payload.isBlank()) continue

            try {
                val json = Json.parseToJsonElement(payload).jsonObject
                val candidates = json["candidates"]?.jsonArray
                if (candidates.isNullOrEmpty()) continue

                val candidate = candidates[0].jsonObject
                val finishReason = candidate["finishReason"]?.jsonPrimitive?.content

                if (finishReason == "SAFETY") {
                    emit(AiChunk.Error("El servicio rechazó este pedido por filtros de seguridad."))
                    return@flow
                }

                val content = candidate["content"]?.jsonObject
                val parts = content?.get("parts")?.jsonArray
                if (!parts.isNullOrEmpty()) {
                    val textPart = parts[0].jsonObject["text"]?.jsonPrimitive?.content
                    if (!textPart.isNullOrBlank()) {
                        emit(AiChunk.Delta(textPart))
                    }
                }

                if (finishReason == "STOP") {
                    emit(AiChunk.Done)
                    return@flow
                }
            } catch (_: Exception) {
                // Ignorar chunks malformados.
            }
        }

        emit(AiChunk.Done)
    }.flowOn(Dispatchers.IO)

    suspend fun testConnection(): Result<String> {
        val key = apiKey().trim()
        if (key.isBlank()) {
            return Result.failure(IllegalStateException("Configurá tu API key en Ajustes"))
        }

        if (!tryReserveSlot()) {
            val cooldown = getCooldownSeconds()
            return Result.failure(IllegalStateException("Límite de 15 requests/minuto. Esperá ${cooldown}s."))
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$key"

        val bodyJson = buildJsonObject {
            put("contents", kotlinx.serialization.json.buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", kotlinx.serialization.json.buildJsonArray {
                        add(buildJsonObject { put("text", "Decí 'hola'") })
                    })
                })
            })
            putJsonObject("generationConfig") {
                put("temperature", 0.3)
                put("maxOutputTokens", 10)
            }
        }

        return try {
            val response = httpClient.post(url) {
                contentType(ContentType.Application.Json)
                header("Content-Type", "application/json")
                setBody(bodyJson.toString())
            }

            if (response.status.value !in 200..299) {
                Result.failure(IllegalStateException(mapHttpError(response.status.value)))
            } else {
                Result.success("Conectado a gemini-2.5-flash")
            }
        } catch (e: Exception) {
            Result.failure(IllegalStateException(mapNetworkError(e)))
        }
    }

    private fun mapHttpError(code: Int): String = when (code) {
        400 -> "El texto es demasiado largo o contiene contenido no permitido."
        401, 403 -> "API key incorrecta. Revisá tu clave en Ajustes."
        429 -> "Llegaste al límite de 15 requests por minuto. Esperá un momento."
        503 -> "Gemini está sobrecargado, probá en un minuto."
        else -> "Error del servidor (código $code). Intentá de nuevo."
    }

    /** Parsea el cuerpo del error de Google para extraer el mensaje y el tiempo de espera. */
    private fun parseGoogleError(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return try {
            val json = Json.parseToJsonElement(body).jsonObject
            val error = json["error"]?.jsonObject ?: return null
            val message = error["message"]?.jsonPrimitive?.content
            
            // Google a veces incluye "quota" info en el mensaje
            message
        } catch (_: Exception) {
            null
        }
    }

    private fun mapNetworkError(e: Exception): String = when (e) {
        is java.io.IOException -> "Sin conexión a internet."
        else -> "Error de red: ${e.message ?: "desconocido"}"
    }
}
