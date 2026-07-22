package com.oreoexperience.notes.data.ai

import com.oreoexperience.notes.data.UserPreferences
import kotlinx.coroutines.flow.Flow

/** Tono de reescritura para la acción "Reescribir". */
enum class RewriteTone(val label: String, val instruction: String) {
    Formal("Formal", "tono formal y profesional"),
    Casual("Casual", "tono casual y cercano"),
    Confident("Confiado", "tono seguro y directo"),
    Friendly("Amigable", "tono cálido y empático"),
}

/**
 * Fachada de alto nivel para las acciones de IA. Construye los prompts
 * usando [AiPrompts] y delega el streaming a [GeminiClient].
 */
class AiService(
    val geminiClient: GeminiClient,
    private val prefs: UserPreferences,
) {
    fun isAvailable(): Boolean = prefs.hasAiConfigured()

    fun correct(text: String): Flow<AiChunk> =
        geminiClient.stream(AiPrompts.CORRECT_SYSTEM, text)

    fun rewrite(text: String, tone: RewriteTone): Flow<AiChunk> =
        geminiClient.stream(
            AiPrompts.REWRITE_SYSTEM.replace("{TONE}", tone.instruction),
            text,
        )

    fun shorten(text: String): Flow<AiChunk> =
        geminiClient.stream(AiPrompts.SHORTEN_SYSTEM, text)

    fun expand(text: String): Flow<AiChunk> =
        geminiClient.stream(AiPrompts.EXPAND_SYSTEM, text)

    fun summarize(plainText: String): Flow<AiChunk> =
        geminiClient.stream(AiPrompts.SUMMARIZE_SYSTEM, plainText)

    fun suggestTitle(plainText: String): Flow<AiChunk> =
        geminiClient.stream(AiPrompts.SUGGEST_TITLE_SYSTEM, plainText)

    fun continueWriting(prefix: String): Flow<AiChunk> =
        geminiClient.stream(AiPrompts.CONTINUE_WRITING_SYSTEM, prefix)

    fun translate(text: String, targetLang: String): Flow<AiChunk> =
        geminiClient.stream(
            AiPrompts.TRANSLATE_SYSTEM.replace("{LANG}", targetLang),
            text,
        )

    fun explainSelection(text: String): Flow<AiChunk> =
        geminiClient.stream(AiPrompts.EXPLAIN_SYSTEM, text)

    suspend fun testConnection(): Result<String> = geminiClient.testConnection()
}
