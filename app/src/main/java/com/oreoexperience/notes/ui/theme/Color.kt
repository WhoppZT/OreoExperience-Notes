package com.oreoexperience.notes.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta monocromática inspirada en iOS Notes (modo oscuro):
 *
 *   - Fondo **negro puro** (`#000000`) y dos grises sutiles para la jerarquía
 *     de cards / divisores.
 *   - Texto **blanco** principal y blanco con alfa para el texto secundario.
 *   - Único acento **amarillo / dorado iOS** (`#FFCC00`) para FAB, botones
 *     destacados, links, handles de selección.
 *
 * El nombre del object se mantiene `OreoPalette` para minimizar el churn de
 * imports en el resto del código — pero internamente todos los valores son
 * los nuevos. Si más adelante quieren cambiar a un acento distinto, sólo
 * hay que tocar este archivo.
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

    // Acento iOS amarillo / dorado
    val Accent     = Color(0xFFFFCC00)
    val AccentSub  = Color(0xCCFFCC00)           // 80% del acento

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
