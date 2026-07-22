package com.oreoexperience.notes.ui.editor

import com.mohamedrejeb.richeditor.model.RichTextState
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.data.ai.AiChunk
import com.oreoexperience.notes.data.ai.AiService
import com.oreoexperience.notes.data.ai.RewriteTone
import kotlinx.coroutines.flow.Flow

/** Datos del sheet de IA, capturados al momento de disparar la acción. */
data class AiSheetData(
    val label: String,
    val text: String,
    val createFlow: () -> Flow<AiChunk>,
    /** Si es corrección de toda la nota, guardamos los IDs de todos los bloques de texto. */
    val allTextBlockIds: List<String>?,
    val richState: RichTextState?,
    val selStart: Int,
    val selEnd: Int,
    val blockId: String?,
    val onReplace: (String, List<String>?, RichTextState?, Int, Int) -> Unit,
    val onInsertBelow: (String) -> Unit,
)

/** Acción de IA seleccionada por el usuario. */
sealed class AiAction(val label: String) {
    abstract fun createFlow(service: AiService, text: String): Flow<AiChunk>

    data object Correct : AiAction("Corregir") {
        override fun createFlow(service: AiService, text: String) = service.correct(text)
    }
    data class Rewrite(val tone: RewriteTone) : AiAction("Reescribir (${tone.label})") {
        override fun createFlow(service: AiService, text: String) = service.rewrite(text, tone)
    }
    data object Shorten : AiAction("Acortar") {
        override fun createFlow(service: AiService, text: String) = service.shorten(text)
    }
    data object Expand : AiAction("Ampliar") {
        override fun createFlow(service: AiService, text: String) = service.expand(text)
    }
    data object Explain : AiAction("Explicar") {
        override fun createFlow(service: AiService, text: String) = service.explainSelection(text)
    }
    data class Translate(val lang: String) : AiAction("Traducir al $lang") {
        override fun createFlow(service: AiService, text: String) = service.translate(text, lang)
    }
}
