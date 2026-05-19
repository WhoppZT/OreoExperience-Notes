package com.oreoexperience.notes.ui.home

import com.oreoexperience.notes.data.Discurso
import com.oreoexperience.notes.data.NoteBlock
import com.oreoexperience.notes.data.NoteBlockSerializer
import java.util.Locale
import kotlin.math.ln

/**
 * Agrupador **semántico** de notas. Lo usa la Home cuando el usuario
 * elige el sort "Orden semántico" — no se trata de un orden temporal,
 * sino *temático*: las notas se agrupan por palabras clave compartidas.
 *
 * Es deliberadamente ligero (puro Kotlin, sin librerías ni red): no
 * pretende ser un modelo de lenguaje, simplemente un **bag-of-words
 * con TF-IDF** sobre título + cuerpo, recortado a tokens razonables.
 *
 * Algoritmo:
 *
 *   1. Tokenizamos cada nota (título con peso ×3 + cuerpo de texto)
 *      a palabras minúsculas, sin acentos ni signos, sin stopwords
 *      en español/inglés ni números cortos.
 *   2. Calculamos un *document frequency* `df[token]` y un *score*
 *      por token y nota = `tf * log(N / df)`.
 *   3. Cada nota recibe su "tópico" = el token con mejor score
 *      (`null` si no hay ninguno por encima de un umbral mínimo).
 *   4. Agrupamos las notas por ese tópico; los grupos se ordenan de
 *      mayor a menor por cantidad de notas y, en empate, por nota
 *      más reciente. Dentro de cada grupo las notas se ordenan por
 *      `updatedAt` desc.
 *   5. Las notas sin tópico claro caen en un único grupo final
 *      llamado "Otros" (intencionalmente al final).
 *
 * El resultado es una `List<Pair<String, List<Discurso>>>` donde el
 * String es la etiqueta de la sección (`"Tópico — palabra"` en
 * mayúscula inicial). Ese formato calza con `groupByMonth` para que
 * la home renderice las secciones sin lógica adicional.
 *
 * Coste: O(N · L_promedio) — para los volúmenes típicos de la app
 * (cientos de notas con kilobytes de texto cada una) es invisible.
 */
object SemanticGrouper {

    /** Stopwords es+en muy comunes — se descartan al tokenizar. */
    private val STOPWORDS = setOf(
        // Artículos y conectores ES
        "el", "la", "los", "las", "un", "una", "unos", "unas",
        "de", "del", "al", "a", "en", "y", "o", "u", "e", "que",
        "por", "para", "con", "sin", "su", "sus", "lo", "le", "les",
        "se", "me", "te", "ya", "no", "sí", "si", "es", "son", "ser",
        "fue", "era", "esta", "este", "esto", "estos", "estas", "esa",
        "ese", "eso", "esos", "esas", "como", "cuando", "donde", "más",
        "menos", "muy", "tan", "también", "ni", "pero", "porque", "sino",
        "aunque", "mientras", "todo", "toda", "todos", "todas", "nada",
        "algo", "alguno", "alguna", "algunos", "algunas", "ningún",
        "tu", "tus", "mi", "mis", "yo", "él", "ella", "ellos", "ellas",
        "nosotros", "vosotros", "ustedes", "usted",
        // Artículos y conectores EN
        "the", "a", "an", "of", "to", "and", "or", "in", "on", "at",
        "for", "with", "without", "by", "from", "as", "is", "are",
        "was", "were", "be", "been", "being", "this", "that", "these",
        "those", "it", "its", "i", "you", "he", "she", "we", "they",
        "his", "her", "our", "their", "my", "your", "not", "but",
        "if", "then", "so", "than", "do", "does", "did", "have", "has",
        "had", "will", "would", "can", "could", "should", "shall",
    )

    private val TOKEN_REGEX = Regex("[^\\p{L}\\p{Nd}]+")

    /**
     * Recibe la lista de notas (asume que ya viene ordenada como la
     * home quiere por tiempo) y devuelve la lista agrupada por tópico,
     * lista para iterar con el mismo contrato que `groupByMonth`.
     */
    fun group(items: List<Discurso>): List<Pair<String, List<Discurso>>> {
        if (items.isEmpty()) return emptyList()
        // Si hay muy pocas notas no vale la pena calcular nada: las
        // metemos todas bajo un único título "Todas".
        if (items.size < 3) {
            return listOf("Todas" to items.sortedByDescending { it.updatedAt })
        }

        // 1. Tokenización por nota.
        val tokensByNote: Map<Long, List<String>> = items.associate { d ->
            d.id to tokenize(noteCorpus(d))
        }

        // 2. df por token.
        val df = HashMap<String, Int>()
        for ((_, tokens) in tokensByNote) {
            val seen = HashSet<String>()
            for (t in tokens) {
                if (seen.add(t)) df.merge(t, 1) { acc, _ -> acc + 1 }
            }
        }
        val N = items.size
        // Stopwords "estadísticas": si una palabra aparece en >=80% de
        // las notas es ruido, la descartamos del scoring.
        val ceiling = (N * 0.8).toInt().coerceAtLeast(2)

        // 3. Mejor tópico por nota.
        val topicByNote: Map<Long, String?> = items.associate { d ->
            val tokens = tokensByNote[d.id] ?: emptyList()
            if (tokens.isEmpty()) {
                d.id to null
            } else {
                val tf = HashMap<String, Int>()
                for (t in tokens) tf.merge(t, 1) { acc, _ -> acc + 1 }
                val best = tf.entries
                    .filter { it.value > 0 }
                    .filter { (df[it.key] ?: 0) in 1..ceiling }
                    .maxByOrNull { (token, count) ->
                        val freq = (df[token] ?: 1).coerceAtLeast(1)
                        count * ln((N + 1.0) / freq)
                    }
                d.id to best?.key
            }
        }

        // 4. Agrupar.
        val groups = LinkedHashMap<String, MutableList<Discurso>>()
        val orphan = mutableListOf<Discurso>()
        for (d in items) {
            val topic = topicByNote[d.id]
            if (topic == null) {
                orphan.add(d)
            } else {
                groups.getOrPut(topic) { mutableListOf() }.add(d)
            }
        }

        // Si el cluster tiene <2 notas, mandamos las notas a "Otros"
        // para no saturar la home de secciones de una sola nota.
        val collapsed = LinkedHashMap<String, MutableList<Discurso>>()
        for ((topic, list) in groups) {
            if (list.size >= 2) collapsed[topic] = list else orphan.addAll(list)
        }

        // 5. Ordenar por relevancia: secciones más grandes primero,
        // luego por fecha más reciente; dentro de cada sección por
        // updatedAt desc.
        val sortedSections = collapsed.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, MutableList<Discurso>>> { it.value.size }
                    .thenByDescending { it.value.maxOf { d -> d.updatedAt } },
            )
            .map { (topic, list) ->
                val label = "Tópico · " + topic.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale("es")) else it.toString()
                }
                label to list.sortedByDescending { it.updatedAt }
            }

        val result = sortedSections.toMutableList()
        if (orphan.isNotEmpty()) {
            result.add("Otros" to orphan.sortedByDescending { it.updatedAt })
        }
        return result
    }

    /** Genera el "corpus" de una nota: título con peso ×3 + texto. */
    private fun noteCorpus(d: Discurso): String {
        val blocks = NoteBlockSerializer.decode(d.notes)
        val body = blocks
            .filterIsInstance<NoteBlock.Text>()
            .joinToString(separator = "\n") { it.markdown }
        val tagBoost = d.tags.replace(",", " ")
        return buildString {
            // El título y las tags pesan más: los repetimos.
            repeat(3) {
                append(d.title)
                append(' ')
                append(tagBoost)
                append(' ')
            }
            append(body)
        }
    }

    /**
     * Normaliza un texto: minúsculas + sin acentos + sin signos.
     * Devuelve los tokens >=4 caracteres no-stopword no-numéricos.
     */
    private fun tokenize(text: String): List<String> {
        val lowered = text.lowercase(Locale("es"))
        val stripped = stripAccents(lowered)
        return TOKEN_REGEX.split(stripped)
            .asSequence()
            .filter { it.length >= 4 }
            .filter { it !in STOPWORDS }
            .filter { it.any { c -> c.isLetter() } }
            .toList()
    }

    /** Saca acentos en latín de un string (NFD + strip combining). */
    private fun stripAccents(s: String): String {
        val normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        val sb = StringBuilder(normalized.length)
        for (c in normalized) {
            if (Character.getType(c) != Character.NON_SPACING_MARK.toInt()) {
                sb.append(c)
            }
        }
        return sb.toString()
    }
}
