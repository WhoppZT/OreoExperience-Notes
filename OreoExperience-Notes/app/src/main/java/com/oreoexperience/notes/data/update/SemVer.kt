package com.oreoexperience.notes.data.update

/**
 * Versión semántica simple (major.minor.patch).
 * Se usa para comparar versionName de la app contra tags de GitHub Releases.
 */
data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {

    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        return patch - other.patch
    }

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /**
         * Parsea un string como "1.0.1", "v2.0.0", "1.2" o "3".
         * Tira cualquier sufijo tras el número (ej: "1.0.0 ORBETA" → 1.0.0).
         */
        fun parse(s: String): SemVer? {
            val clean = s.trimStart('v', 'V')
                .takeWhile { it.isDigit() || it == '.' }
            val parts = clean.split('.').mapNotNull { it.toIntOrNull() }
                    // La versión "1.0.0 ORBETA" → extrae solo los números
            return when (parts.size) {
                3 -> SemVer(parts[0], parts[1], parts[2])
                2 -> SemVer(parts[0], parts[1], 0)
                1 -> SemVer(parts[0], 0, 0)
                else -> null
            }
        }
    }
}
