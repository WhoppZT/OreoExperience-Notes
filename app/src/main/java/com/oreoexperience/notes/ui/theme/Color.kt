package com.oreoexperience.notes.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta "starry / Aurora" portada del desktop OreoExperience.
 * El fondo no es plano: lo dibuja [com.oreoexperience.notes.ui.theme.AuroraBackground]
 * usando varios "blobs" radiales con estos colores como tinte.
 */
object OreoPalette {
    // Bases del fondo
    val Bg0       = Color(0xFF08090F)   // negro azulado base
    val Bg1       = Color(0xFF0E0F2E)   // azul muy oscuro
    val Mesh1     = Color(0xFF1F0E5A)   // violeta profundo
    val Mesh2     = Color(0xFF3A1A6B)   // violeta medio
    val Mesh3     = Color(0xFF2A1066)   // violeta saturado

    // Acentos / texto
    val Accent    = Color(0xFFB68CFF)   // violeta claro (botones primarios, marcas)
    val AccentSub = Color(0xFF8C6BFF)   // violeta medio (highlights secundarios)
    val OnSurface = Color(0xFFE9E4FF)   // texto principal (lavanda muy clara)
    val OnSurfaceMuted = Color(0xFFB7B2D6)
    val Outline   = Color(0x33B68CFF)   // contornos sutiles del cristal
    val GlassFill = Color(0x18FFFFFF)   // relleno semi-transparente de las cards
    val GlassFillStrong = Color(0x26FFFFFF)
    /** Variante "frosted" muy opaca para cards de lectura: oscurece bastante
     *  el fondo Aurora detrás para que el texto resalte como si estuviera
     *  sobre vidrio escarchado. */
    val GlassFillFrosted = Color(0xCC0E0F2E)
    val DangerFill = Color(0xFFFF6B8A)
    val WarnFill   = Color(0xFFFFC857)
    val OkFill     = Color(0xFF7CE7B1)
}
