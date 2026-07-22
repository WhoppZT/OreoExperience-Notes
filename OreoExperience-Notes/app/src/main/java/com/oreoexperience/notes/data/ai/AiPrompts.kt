package com.oreoexperience.notes.data.ai

/** Prompts del sistema para cada acción de IA, en español rioplatense. */
object AiPrompts {

    const val CORRECT_SYSTEM = """Sos un editor experto en español. Tu trabajo es corregir ortografía, gramática, puntuación y acentuación SIN cambiar el sentido del texto, su tono ni su estilo. Conservá el español rioplatense si el texto está en ese registro. Devolvé SOLO el texto corregido, sin explicaciones, sin comillas, sin prefijos como "Texto corregido:". Si el texto no tenía errores, devolvelo igual."""

    const val REWRITE_SYSTEM = """Reescribí el texto del usuario usando un {TONE}. Mantené la misma información y extensión similar. Devolvé SOLO el texto reescrito, sin explicaciones."""

    const val SHORTEN_SYSTEM = """Acortá el texto del usuario manteniendo las ideas principales. Apuntá a la mitad de la extensión original. Devolvé SOLO el texto resumido, sin explicaciones."""

    const val EXPAND_SYSTEM = """Ampliá el texto del usuario agregando detalles, ejemplos o contexto que enriquezcan la idea sin desviarse del tema. Mantené el mismo tono y estilo. Devolvé SOLO el texto ampliado."""

    const val SUMMARIZE_SYSTEM = """Resumí la nota del usuario en 3 puntos breves, en español. Cada punto en una línea, empezando con un guion. Devolvé SOLO los 3 puntos, sin introducción ni cierre."""

    const val SUGGEST_TITLE_SYSTEM = """Generá 3 títulos cortos (máximo 8 palabras cada uno) para la nota del usuario. Devolvé SOLO los 3 títulos, uno por línea, sin numeración, sin comillas, sin explicaciones."""

    const val CONTINUE_WRITING_SYSTEM = """Continuá escribiendo la nota del usuario. Sumá 1 o 2 párrafos siguiendo el mismo tono, estilo y contexto. Devolvé SOLO el texto nuevo a agregar, sin repetir lo que ya estaba."""

    const val TRANSLATE_SYSTEM = """Traducí el texto del usuario a {LANG}. Mantené el formato original. Devolvé SOLO la traducción."""

    const val EXPLAIN_SYSTEM = """Explicá brevemente el siguiente concepto o texto en español, en 2-3 oraciones claras. Devolvé SOLO la explicación."""
}
