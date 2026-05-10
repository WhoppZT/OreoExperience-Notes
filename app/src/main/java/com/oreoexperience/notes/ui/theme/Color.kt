package com.oreoexperience.notes.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta inspirada en iOS Notes (modo oscuro) con acento **violeta
 * Aurora** — el mismo lenguaje de color que el resto del ecosistema
 * OreoExperience:
 *
 *   - Fondo **negro puro** (`#000000`) y dos grises sutiles para la
 *     jerarquía de cards / divisores.
 *   - Texto **blanco** principal y blanco con alfa para el texto
 *     secundario.
 *   - Acento principal **violeta Aurora** (`#7C3AED`) para FAB,
 *     botones destacados, links, handles de selección.
 *   - Sub-acento lavanda (`#A78BFA`) para gradientes y resaltes
 *     secundarios.
 *
 * El nombre del object se mantiene `OreoPalette` para minimizar el
 * churn de imports en el resto del código.
 */
object OreoPalette {
    // Fondos
    val Bg0       = Color(0xFF000000)            // negro puro
    val Bg1       = Color(0xFF0A0A0A)            // panel ligeramente más claro
    val SurfaceCard = Color(0xFF1C1C1E)          // gris iOS systemGray6 (cards)
    val SurfaceCardHi = Color(0xFF2C2C2E)        // gris iOS systemGray5 (hover)

    // Texto
    val OnSurface       = Color(0xFFFFFFFF)
    val OnSurfaceMuted  = Color(0xB3FFFFFF)      // 70% blanco
    val OnSurfaceFaint  = Color(0x66FFFFFF)      // 40% blanco (placeholders, fechas)

    // Acento Aurora violeta
    val Accent     = Color(0xFF7C3AED)           // violeta principal (primary)
    val AccentSub  = Color(0xFFA78BFA)           // lavanda (resaltes/handles)
    val AccentDeep = Color(0xFF5B21B6)           // violeta profundo (gradiente base)

    // Divisores y outlines sutiles
    val Outline    = Color(0x33FFFFFF)           // 20% blanco
    val OutlineFaint = Color(0x14FFFFFF)         // 8% blanco (separadores de filas)

    // Aliases legacy (para no romper imports existentes)
    val Mesh1     = Bg1
    val Mesh2     = SurfaceCard
    val Mesh3     = SurfaceCardHi
    val GlassFill = SurfaceCard
    val GlassFillStrong = SurfaceCardHi
    val GlassFillFrosted = SurfaceCard

    // Estados (timer)
    val DangerFill = Color(0xFFFF453A)           // rojo iOS
    val WarnFill   = Color(0xFFFF9F0A)           // naranja iOS
    val OkFill     = Color(0xFF30D158)           // verde iOS
}
