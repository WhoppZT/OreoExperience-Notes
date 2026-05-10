package com.oreoexperience.notes.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta de la app. Soporta tema oscuro y claro: el set de colores
 * activo lo elige [OreoExperienceTheme] en función del [ThemeMode]
 * preferido del usuario.
 *
 * El **acento violeta Aurora** es el mismo en los dos modos para
 * mantener la identidad de marca; lo que cambia es el fondo, las
 * superficies, el texto y los divisores.
 *
 * Se mantiene la API histórica `OreoPalette.X` con propiedades
 * delegadas a [ActivePalette] — un `var` global que se actualiza al
 * principio de cada paso del tema. Esto evita refactorizar las +100
 * referencias dispersas por la UI.
 */
data class OreoPaletteData(
    // Fondos
    val Bg0: Color,
    val Bg1: Color,
    val SurfaceCard: Color,
    val SurfaceCardHi: Color,
    // Texto
    val OnSurface: Color,
    val OnSurfaceMuted: Color,
    val OnSurfaceFaint: Color,
    // Acento Aurora
    val Accent: Color,
    val AccentSub: Color,
    val AccentDeep: Color,
    // Líneas
    val Outline: Color,
    val OutlineFaint: Color,
    // Estados
    val DangerFill: Color,
    val WarnFill: Color,
    val OkFill: Color,
)

/** Paleta oscura — el modo principal histórico de la app. */
val DarkPalette = OreoPaletteData(
    Bg0 = Color(0xFF000000),
    Bg1 = Color(0xFF0A0A0A),
    SurfaceCard = Color(0xFF1C1C1E),
    SurfaceCardHi = Color(0xFF2C2C2E),
    OnSurface = Color(0xFFFFFFFF),
    OnSurfaceMuted = Color(0xB3FFFFFF),
    OnSurfaceFaint = Color(0x66FFFFFF),
    Accent = Color(0xFF7C3AED),
    AccentSub = Color(0xFFA78BFA),
    AccentDeep = Color(0xFF5B21B6),
    Outline = Color(0x33FFFFFF),
    OutlineFaint = Color(0x14FFFFFF),
    DangerFill = Color(0xFFFF453A),
    WarnFill = Color(0xFFFF9F0A),
    OkFill = Color(0xFF30D158),
)

/**
 * Paleta clara — equivalente "iOS Notes Light":
 *   - Fondo blanco puro y cards sobre `systemGray6` claro.
 *   - Texto en negro con la misma jerarquía de alfas (70% / 40%).
 *   - El acento Aurora se mantiene saturado.
 */
val LightPalette = OreoPaletteData(
    Bg0 = Color(0xFFFFFFFF),
    Bg1 = Color(0xFFF8F8FA),
    SurfaceCard = Color(0xFFF2F2F7),
    SurfaceCardHi = Color(0xFFE5E5EA),
    OnSurface = Color(0xFF000000),
    OnSurfaceMuted = Color(0xB3000000),
    OnSurfaceFaint = Color(0x66000000),
    Accent = Color(0xFF7C3AED),
    AccentSub = Color(0xFFA78BFA),
    AccentDeep = Color(0xFF5B21B6),
    Outline = Color(0x33000000),
    OutlineFaint = Color(0x14000000),
    DangerFill = Color(0xFFFF3B30),
    WarnFill = Color(0xFFFF9500),
    OkFill = Color(0xFF34C759),
)

/**
 * Paleta activa global. La actualiza [OreoExperienceTheme] al inicio
 * de cada paso de composición. Como `OreoExperienceTheme` envuelve
 * todo el árbol de UI, los hijos siempre leen el valor más reciente
 * cuando recomponen.
 */
internal var ActivePalette: OreoPaletteData = DarkPalette

/**
 * API histórica `OreoPalette.X`. Las propiedades son `get()`-delegadas
 * a [ActivePalette] para que el tema activo se aplique sin refactor
 * masivo de las referencias dispersas en la UI.
 */
object OreoPalette {
    val Bg0:           Color get() = ActivePalette.Bg0
    val Bg1:           Color get() = ActivePalette.Bg1
    val SurfaceCard:   Color get() = ActivePalette.SurfaceCard
    val SurfaceCardHi: Color get() = ActivePalette.SurfaceCardHi

    val OnSurface:      Color get() = ActivePalette.OnSurface
    val OnSurfaceMuted: Color get() = ActivePalette.OnSurfaceMuted
    val OnSurfaceFaint: Color get() = ActivePalette.OnSurfaceFaint

    val Accent:     Color get() = ActivePalette.Accent
    val AccentSub:  Color get() = ActivePalette.AccentSub
    val AccentDeep: Color get() = ActivePalette.AccentDeep

    val Outline:      Color get() = ActivePalette.Outline
    val OutlineFaint: Color get() = ActivePalette.OutlineFaint

    val DangerFill: Color get() = ActivePalette.DangerFill
    val WarnFill:   Color get() = ActivePalette.WarnFill
    val OkFill:     Color get() = ActivePalette.OkFill

    // Aliases legacy (existían en la versión anterior — no se eliminan
    // para no romper imports de cards "glass" antiguas).
    val Mesh1: Color get() = Bg1
    val Mesh2: Color get() = SurfaceCard
    val Mesh3: Color get() = SurfaceCardHi
    val GlassFill:         Color get() = SurfaceCard
    val GlassFillStrong:   Color get() = SurfaceCardHi
    val GlassFillFrosted:  Color get() = SurfaceCard
}
