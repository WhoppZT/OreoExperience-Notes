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
    // ─── Fondos ──────────────────────────────────────────────
    val Bg0: Color,
    val Bg1: Color,
    val SurfaceCard: Color,
    val SurfaceCardHi: Color,

    // ─── Texto ───────────────────────────────────────────────
    val OnSurface: Color,
    val OnSurfaceMuted: Color,
    val OnSurfaceFaint: Color,

    // ─── Acento Aurora ───────────────────────────────────────
    val Accent: Color,
    val AccentSub: Color,
    val AccentDeep: Color,

    // ─── Líneas ──────────────────────────────────────────────
    val Outline: Color,
    val OutlineFaint: Color,

    // ─── Estados semánticos ──────────────────────────────────
    val DangerFill: Color,
    val WarnFill: Color,
    val OkFill: Color,

    // ─── Superficies elevadas ────────────────────────────────
    val SurfaceOverlay: Color,
    val SurfaceDialog: Color,
    val SurfaceNavBar: Color,
    val SurfaceChipBadge: Color,

    // ─── Bordes / Dividers ───────────────────────────────────
    val BorderSubtle: Color,

    // ─── Texto terciario (inactivo / hint) ───────────────────
    val TextTertiary: Color,

    // ─── Acentos extendidos ──────────────────────────────────
    val AccentLight: Color,
    val AccentGlow: Color,
    val StarDot: Color,
    val StarLight: Color,

    // ─── Categorías ──────────────────────────────────────────
    val CategoryDiscursoso: Color,
    val CategoryConsideracion: Color,
    val CategoryGeneral: Color,

    // ─── Colores semánticos extendidos ───────────────────────
    val SemanticGreen: Color,
    val SemanticOrange: Color,
    val SemanticRed: Color,
    val SemanticBlue: Color,

    // ─── Colores de edición (text palette del editor) ────────
    val EditorTextBlack: Color,
    val EditorTextRed: Color,
    val EditorTextOrange: Color,
    val EditorTextGreen: Color,
    val EditorTextBlue: Color,
    val EditorHighlightViolet: Color,
    val EditorHighlightYellow: Color,
    val EditorHighlightGreen: Color,

    // ─── FAB ─────────────────────────────────────────────────
    val AccentFabEnd: Color,

    // ─── Toggle ──────────────────────────────────────────────
    val ToggleTrackOff: Color,
)

/** Paleta oscura — Midnight Purple (diseño #15). */
val DarkPalette = OreoPaletteData(
    // Fondos — deep purple tinted
    Bg0 = Color(0xFF0F0A1A),
    Bg1 = Color(0xFF120E20),
    SurfaceCard = Color(0xFF1A1528),
    SurfaceCardHi = Color(0xFF231D34),
    // Texto — lavender tones
    OnSurface = Color(0xFFD4C5F9),
    OnSurfaceMuted = Color(0xFF9B8EC0),
    OnSurfaceFaint = Color(0xFF5B4F7A),
    // Acento Aurora
    Accent = Color(0xFF7C3AED),
    AccentSub = Color(0xFFA78BFA),
    AccentDeep = Color(0xFF5B21B6),
    // Líneas
    Outline = Color(0x33A78BFA),
    OutlineFaint = Color(0x1AA78BFA),
    // Estados
    DangerFill = Color(0xFFFF453A),
    WarnFill = Color(0xFFFF9F0A),
    OkFill = Color(0xFF30D158),
    // Superficies elevadas
    SurfaceOverlay = Color(0xD9120E20),
    SurfaceDialog = Color(0xFF1E1830),
    SurfaceNavBar = Color(0xF80F0A1A),
    SurfaceChipBadge = Color(0xFF231D34),
    // Bordes
    BorderSubtle = Color(0x407C3AED),
    // Texto terciario
    TextTertiary = Color(0xFF5B4F7A),
    // Acentos extendidos
    AccentLight = Color(0xFFC084FC),
    AccentGlow = Color(0xFF9B6DFF),
    StarDot = Color(0xDDD4C5F9),
    StarLight = Color(0xFFC4A8FF),
    // Categorías
    CategoryDiscursoso = Color(0xFF6366F1),
    CategoryConsideracion = Color(0xFFF59E0B),
    CategoryGeneral = Color(0xFF10B981),
    // Semánticos extendidos
    SemanticGreen = Color(0xFF22C55E),
    SemanticOrange = Color(0xFFF97316),
    SemanticRed = Color(0xFFEF4444),
    SemanticBlue = Color(0xFF3B82F6),
    // Editor
    EditorTextBlack = Color(0xFF111111),
    EditorTextRed = Color(0xFFDC2626),
    EditorTextOrange = Color(0xFFD97706),
    EditorTextGreen = Color(0xFF059669),
    EditorTextBlue = Color(0xFF2563EB),
    EditorHighlightViolet = Color(0x809333EA),
    EditorHighlightYellow = Color(0x80FACC15),
    EditorHighlightGreen = Color(0x8022C55E),
    // FAB
    AccentFabEnd = Color(0xFF5A3AC8),
    // Toggle
    ToggleTrackOff = Color(0xFF3A3A4A),
)

/**
 * Paleta clara — equivalente "iOS Notes Light":
 *   - Fondo blanco puro y cards sobre `systemGray6` claro.
 *   - Texto en negro con la misma jerarquía de alfas (70% / 40%).
 *   - El acento Aurora se mantiene saturado.
 */
val LightPalette = OreoPaletteData(
    // Fondos
    Bg0 = Color(0xFFFFFFFF),
    Bg1 = Color(0xFFF8F8FA),
    SurfaceCard = Color(0xFFF2F2F7),
    SurfaceCardHi = Color(0xFFE5E5EA),
    // Texto
    OnSurface = Color(0xFF000000),
    OnSurfaceMuted = Color(0xB3000000),
    OnSurfaceFaint = Color(0x66000000),
    // Acento Aurora
    Accent = Color(0xFF7C3AED),
    AccentSub = Color(0xFFA78BFA),
    AccentDeep = Color(0xFF5B21B6),
    // Líneas
    Outline = Color(0x33000000),
    OutlineFaint = Color(0x14000000),
    // Estados
    DangerFill = Color(0xFFFF3B30),
    WarnFill = Color(0xFFFF9500),
    OkFill = Color(0xFF34C759),
    // Superficies elevadas
    SurfaceOverlay = Color(0xD9F0F0F5),
    SurfaceDialog = Color(0xFFF2F2F7),
    SurfaceNavBar = Color(0xF8F8F8FA),
    SurfaceChipBadge = Color(0xFFE5E5EA),
    // Bordes
    BorderSubtle = Color(0x408264FF),
    // Texto terciario
    TextTertiary = Color(0xFF7A7A9A),
    // Acentos extendidos
    AccentLight = Color(0xFFC084FC),
    AccentGlow = Color(0xFF9B6DFF),
    StarDot = Color(0xDD000000),
    StarLight = Color(0xFFC4A8FF),
    // Categorías
    CategoryDiscursoso = Color(0xFF6366F1),
    CategoryConsideracion = Color(0xFFF59E0B),
    CategoryGeneral = Color(0xFF10B981),
    // Semánticos extendidos
    SemanticGreen = Color(0xFF22C55E),
    SemanticOrange = Color(0xFFF97316),
    SemanticRed = Color(0xFFEF4444),
    SemanticBlue = Color(0xFF3B82F6),
    // Editor
    EditorTextBlack = Color(0xFF111111),
    EditorTextRed = Color(0xFFDC2626),
    EditorTextOrange = Color(0xFFD97706),
    EditorTextGreen = Color(0xFF059669),
    EditorTextBlue = Color(0xFF2563EB),
    EditorHighlightViolet = Color(0x809333EA),
    EditorHighlightYellow = Color(0x80FACC15),
    EditorHighlightGreen = Color(0x8022C55E),
    // FAB
    AccentFabEnd = Color(0xFF5A3AC8),
    // Toggle
    ToggleTrackOff = Color(0xFFCACACE),
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
    // ─── Fondos ──────────────────────────────────────────────
    val Bg0:           Color get() = ActivePalette.Bg0
    val Bg1:           Color get() = ActivePalette.Bg1
    val SurfaceCard:   Color get() = ActivePalette.SurfaceCard
    val SurfaceCardHi: Color get() = ActivePalette.SurfaceCardHi

    // ─── Texto ───────────────────────────────────────────────
    val OnSurface:      Color get() = ActivePalette.OnSurface
    val OnSurfaceMuted: Color get() = ActivePalette.OnSurfaceMuted
    val OnSurfaceFaint: Color get() = ActivePalette.OnSurfaceFaint

    // ─── Acento Aurora ───────────────────────────────────────
    val Accent:     Color get() = ActivePalette.Accent
    val AccentSub:  Color get() = ActivePalette.AccentSub
    val AccentDeep: Color get() = ActivePalette.AccentDeep

    // ─── Líneas ──────────────────────────────────────────────
    val Outline:      Color get() = ActivePalette.Outline
    val OutlineFaint: Color get() = ActivePalette.OutlineFaint

    // ─── Estados ─────────────────────────────────────────────
    val DangerFill: Color get() = ActivePalette.DangerFill
    val WarnFill:   Color get() = ActivePalette.WarnFill
    val OkFill:     Color get() = ActivePalette.OkFill

    // ─── Superficies elevadas ────────────────────────────────
    val SurfaceOverlay:  Color get() = ActivePalette.SurfaceOverlay
    val SurfaceDialog:   Color get() = ActivePalette.SurfaceDialog
    val SurfaceNavBar:   Color get() = ActivePalette.SurfaceNavBar
    val SurfaceChipBadge: Color get() = ActivePalette.SurfaceChipBadge

    // ─── Bordes / Dividers ───────────────────────────────────
    val BorderSubtle: Color get() = ActivePalette.BorderSubtle

    // ─── Texto terciario ─────────────────────────────────────
    val TextTertiary: Color get() = ActivePalette.TextTertiary

    // ─── Acentos extendidos ──────────────────────────────────
    val AccentLight: Color get() = ActivePalette.AccentLight
    val AccentGlow:  Color get() = ActivePalette.AccentGlow
    val StarDot:     Color get() = ActivePalette.StarDot
    val StarLight:   Color get() = ActivePalette.StarLight

    // ─── Categorías ──────────────────────────────────────────
    val CategoryDiscursoso:    Color get() = ActivePalette.CategoryDiscursoso
    val CategoryConsideracion: Color get() = ActivePalette.CategoryConsideracion
    val CategoryGeneral:       Color get() = ActivePalette.CategoryGeneral

    // ─── Semánticos extendidos ───────────────────────────────
    val SemanticGreen:  Color get() = ActivePalette.SemanticGreen
    val SemanticOrange: Color get() = ActivePalette.SemanticOrange
    val SemanticRed:    Color get() = ActivePalette.SemanticRed
    val SemanticBlue:   Color get() = ActivePalette.SemanticBlue

    // ─── Editor ──────────────────────────────────────────────
    val EditorTextBlack:       Color get() = ActivePalette.EditorTextBlack
    val EditorTextRed:         Color get() = ActivePalette.EditorTextRed
    val EditorTextOrange:      Color get() = ActivePalette.EditorTextOrange
    val EditorTextGreen:       Color get() = ActivePalette.EditorTextGreen
    val EditorTextBlue:        Color get() = ActivePalette.EditorTextBlue
    val EditorHighlightViolet: Color get() = ActivePalette.EditorHighlightViolet
    val EditorHighlightYellow: Color get() = ActivePalette.EditorHighlightYellow
    val EditorHighlightGreen:  Color get() = ActivePalette.EditorHighlightGreen

    // ─── FAB ─────────────────────────────────────────────────
    val AccentFabEnd: Color get() = ActivePalette.AccentFabEnd

    // ─── Toggle ──────────────────────────────────────────────
    val ToggleTrackOff: Color get() = ActivePalette.ToggleTrackOff

    // ─── Aliases legacy ──────────────────────────────────────
    val Mesh1: Color get() = Bg1
    val Mesh2: Color get() = SurfaceCard
    val Mesh3: Color get() = SurfaceCardHi
    val GlassFill:         Color get() = SurfaceCard
    val GlassFillStrong:   Color get() = SurfaceCardHi
    val GlassFillFrosted:  Color get() = SurfaceCard
}
