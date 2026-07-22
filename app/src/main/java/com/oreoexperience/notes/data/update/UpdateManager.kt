package com.oreoexperience.notes.data.update

import android.content.Context
import com.oreoexperience.notes.data.UserPreferences
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.random.Random

private const val GITHUB_API_URL =
    "https://api.github.com/repos/WhoppZT/OreoExperience-Notes/releases/latest"
private const val TAG = "UpdateManager"
private const val CACHE_TTL_MILLIS = 6L * 60 * 60 * 1000 // 6 horas
private const val CHUNK_PROGRESS_BYTES = 512L * 1024 // cada 512 KB emitimos progreso
private const val MAX_RETRIES = 3

// ─── Modelos de red ──────────────────────────────────────────────────

@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String = "",
    val body: String = "",
    val assets: List<GitHubAsset> = emptyList(),
    @SerialName("published_at") val publishedAt: String = "",
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    val size: Long = 0,
)

// ─── Resultados y estados ────────────────────────────────────────────

sealed interface CheckResult {
    data object UpToDate : CheckResult
    data class Available(
        val release: GitHubRelease,
        val isCritical: Boolean,
        val sizeBytes: Long,
    ) : CheckResult
    data class Error(val message: String) : CheckResult
}

sealed interface DownloadProgress {
    data object Idle : DownloadProgress
    data class Downloading(
        val bytesReceived: Long,
        val totalBytes: Long,
    ) : DownloadProgress {
        val ratio: Float get() =
            if (totalBytes > 0) bytesReceived.toFloat() / totalBytes else 0f
    }
    data class Verifying(val ratio: Float) : DownloadProgress
    data class Ready(val apkFile: File) : DownloadProgress
    data class Failed(val message: String) : DownloadProgress
}

// ─── UpdateManager ───────────────────────────────────────────────────

class UpdateManager(
    private val context: Context,
    private val prefs: UserPreferences,
) {
    private val httpClient = HttpClient(OkHttp) {
        expectSuccess = false // manejamos códigos HTTP nosotros
    }
    private val json = Json { ignoreUnknownKeys = true }

    private val _downloadProgress = MutableStateFlow<DownloadProgress>(DownloadProgress.Idle)
    val downloadProgress: StateFlow<DownloadProgress> = _downloadProgress.asStateFlow()

    private var downloadJob: Job? = null

    // ── Check ───────────────────────────────────────────────────────

    /**
     * Consulta GitHub Releases y compara la versión remota con la local.
     * Si [force] es true ignora la cache. Si no, usa cache con TTL 6h.
     */
    suspend fun checkForUpdate(force: Boolean = false): CheckResult = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            return@withContext CheckResult.Error("Sin conexión a internet")
        }

        // Cache: si el último check fue hace menos de 6h y no se fuerza, devolvemos cache
        val now = System.currentTimeMillis()
        if (!force && prefs.updateLastCheckMillis > 0 &&
            (now - prefs.updateLastCheckMillis) < CACHE_TTL_MILLIS
        ) {
            val cachedTag = prefs.updateLastTagName
            if (cachedTag.isBlank()) return@withContext CheckResult.UpToDate

            val currentVersion = SemVer.parse(currentVersionName()) ?: return@withContext CheckResult.Error(
                "No se pudo leer la versión actual",
            )
            val remoteVersion = SemVer.parse(cachedTag) ?: return@withContext CheckResult.Error(
                "No se pudo leer la versión remota",
            )
            if (remoteVersion <= currentVersion) return@withContext CheckResult.UpToDate

            // Devolvemos Available con datos de cache (no tenemos release completa)
            return@withContext CheckResult.Available(
                release = GitHubRelease(tagName = cachedTag),
                isCritical = prefs.updateLastCriticalFlag,
                sizeBytes = 0L,
            )
        }

        // Hacemos el request a GitHub
        val result = runCatching {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection()
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "OreoExperience-Notes-Android")

            if (connection !is java.net.HttpURLConnection) {
                return@runCatching CheckResult.Error("Error de conexión")
            }

            val responseCode = connection.responseCode
            if (responseCode != 200) {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                Log.e(TAG, "API error: $responseCode - $errorBody")
                connection.disconnect()
                return@runCatching CheckResult.Error("Error al consultar actualizaciones (HTTP $responseCode)")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val release = json.decodeFromString<GitHubRelease>(response)
            val isCritical = release.body.contains("<!-- critical -->") ||
                    release.tagName.endsWith("-critical")
            val sizeBytes = release.assets.firstOrNull()?.size ?: 0L

            val currentVersion = SemVer.parse(currentVersionName())
            val remoteVersion = SemVer.parse(release.tagName)

            if (currentVersion == null || remoteVersion == null) {
                return@runCatching CheckResult.Error("Error al comparar versiones")
            }

            // Guardamos cache
            prefs.updateLastCheckMillis = now
            prefs.updateLastTagName = release.tagName
            prefs.updateLastCriticalFlag = isCritical

            if (remoteVersion <= currentVersion) {
                CheckResult.UpToDate
            } else {
                CheckResult.Available(release, isCritical, sizeBytes)
            }
        }

        result.getOrElse { e ->
            Log.e(TAG, "Error checking update", e)
            CheckResult.Error(e.message ?: "Error desconocido")
        }
    }

    // ── Download ────────────────────────────────────────────────────

    /**
     * Descarga el APK de la release y lo instala. Emite progreso via
     * [downloadProgress]. Reintenta hasta [MAX_RETRIES] veces con backoff.
     */
    fun downloadAndInstall(release: GitHubRelease, scope: CoroutineScope) {
        downloadJob?.cancel()
        downloadJob = scope.launch(Dispatchers.IO) {
            _downloadProgress.value = DownloadProgress.Idle

            val apkAsset = release.assets.find { it.name.endsWith(".apk", ignoreCase = true) }
                ?: release.assets.firstOrNull()
            if (apkAsset == null) {
                _downloadProgress.value = DownloadProgress.Failed("No se encontró un archivo APK en la release")
                return@launch
            }

            val cacheDir = File(context.cacheDir, "updates").also { it.mkdirs() }
            val apkFile = File(cacheDir, apkAsset.name)

            var attempt = 0
            var lastError: String? = null

            while (attempt < MAX_RETRIES && isActive) {
                attempt++
                try {
                    // HEAD request para obtener Content-Length
                    val headUrl = URL(apkAsset.downloadUrl)
                    val headConn = headUrl.openConnection() as java.net.HttpURLConnection
                    headConn.requestMethod = "HEAD"
                    headConn.connectTimeout = 10_000
                    headConn.readTimeout = 10_000
                    val totalBytes = headConn.contentLengthLong.also { headConn.disconnect() }

                    // Advertencia si está usando datos móviles
                    if (isMetered() && totalBytes > 10 * 1024 * 1024) {
                        _downloadProgress.value = DownloadProgress.Failed(
                            "La descarga consume ${formatBytes(totalBytes)} con datos móviles. Conectate a Wi-Fi e intentá de nuevo.",
                        )
                        return@launch
                    }

                    // Descarga con Ktor streaming
                    _downloadProgress.value = DownloadProgress.Downloading(
                        bytesReceived = 0,
                        totalBytes = totalBytes.coerceAtLeast(1),
                    )

                    val response = httpClient.get(apkAsset.downloadUrl) {
                        header("User-Agent", "OreoExperience-Notes-Android")
                    }

                    val channel = response.bodyAsChannel()
                    val inputStream = channel.toInputStream()

                    apkFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var totalRead = 0L
                        var lastProgress = 0L
                        var bytesRead: Int

                        while (inputStream.read(buffer).also { bytesRead = it } != -1 && isActive) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead

                            // Emitir progreso cada 512 KB para no saturar la UI
                            if (totalRead - lastProgress >= CHUNK_PROGRESS_BYTES) {
                                lastProgress = totalRead
                                _downloadProgress.value = DownloadProgress.Downloading(
                                    bytesReceived = totalRead,
                                    totalBytes = totalBytes.coerceAtLeast(totalRead),
                                )
                            }
                        }
                    }

                    if (!isActive) return@launch

                    // Verificar integridad: totalBytes vs tamaño real
                    val actualSize = apkFile.length()
                    if (totalBytes > 0 && actualSize != totalBytes) {
                        lastError = "La descarga está incompleta (${formatBytes(actualSize)} de ${formatBytes(totalBytes)})"
                        Log.w(TAG, lastError)
                        apkFile.delete()
                        if (attempt < MAX_RETRIES) {
                            delay((1000L * attempt) + Random.nextLong(500, 1500)) // backoff + jitter
                            continue
                        }
                        _downloadProgress.value = DownloadProgress.Failed(lastError!!)
                        return@launch
                    }

                    _downloadProgress.value = DownloadProgress.Verifying(ratio = 1f)

                    // Instalar
                    installApk(apkFile)
                    _downloadProgress.value = DownloadProgress.Ready(apkFile)
                    return@launch

                } catch (e: Exception) {
                    lastError = e.message ?: "Error de conexión"
                    Log.e(TAG, "Download attempt $attempt failed", e)
                    if (attempt < MAX_RETRIES) {
                        // Backoff exponencial con jitter
                        val backoff = (1000L shl (attempt - 1)) + Random.nextLong(500, 1500)
                        delay(backoff)
                    }
                }
            }

            // Si llegamos acá, todos los intentos fallaron
            _downloadProgress.value = DownloadProgress.Failed(lastError ?: "Error de descarga")
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _downloadProgress.value = DownloadProgress.Idle
    }

    // ── Postergación ─────────────────────────────────────────────────

    /** Marca esta versión como postergada: no se vuelve a mostrar hasta que haya una más nueva. */
    fun postpone(tagName: String) {
        prefs.updatePostponedTag = tagName
    }

    /** True si el usuario postergó esta versión. */
    fun isPostponed(tagName: String): Boolean =
        prefs.updatePostponedTag == tagName

    // ── Instalación ──────────────────────────────────────────────────

    private fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile,
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            }
        }

        context.startActivity(intent)
    }

    // ── Helpers ──────────────────────────────────────────────────────

    private fun currentVersionName(): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    } catch (e: Exception) {
        ""
    }

    /** Verifica conectividad real usando ConnectivityManager. */
    private fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val net = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(net) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** True si la red activa es datos móviles (no Wi-Fi). */
    private fun isMetered(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        return cm?.isActiveNetworkMetered ?: true
    }

    fun destroy() {
        downloadJob?.cancel()
        httpClient.close()
    }
}

// ─── Helpers de formato ──────────────────────────────────────────────

fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

/** Parsea "2024-09-02T15:30:00Z" a epoch millis, o 0 si falla. */
fun parseGitHubDate(dateStr: String): Long = try {
    java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
        timeZone = java.util.TimeZone.getTimeZone("UTC")
    }.parse(dateStr)?.time ?: 0L
} catch (_: Exception) {
    0L
}

fun formatRelativeTime(epochMillis: Long): String {
    if (epochMillis <= 0) return ""
    val diff = System.currentTimeMillis() - epochMillis
    return when {
        diff < 60_000 -> "hace unos segundos"
        diff < 120_000 -> "hace 1 minuto"
        diff < 3_600_000 -> "hace ${diff / 60_000} minutos"
        diff < 7_200_000 -> "hace 1 hora"
        diff < 86_400_000 -> "hace ${diff / 3_600_000} horas"
        diff < 172_800_000 -> "ayer"
        diff < 604_800_000 -> "hace ${diff / 86_400_000} días"
        else -> "hace ${diff / 604_800_000} semanas"
    }
}
