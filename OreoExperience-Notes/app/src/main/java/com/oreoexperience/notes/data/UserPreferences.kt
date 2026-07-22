package com.oreoexperience.notes.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Wrapper minimalista sobre SharedPreferences. Centraliza las flags
 * persistentes de la app: onboarding visto, modo de tema preferido,
 * autoguardado, escala de letra, etc.
 *
 * Las flags reactivas se exponen como `MutableState` (Compose) además
 * del getter/setter clásico, para que las pantallas que las observan
 * (Settings, Theme) se recompongan automáticamente al cambiar el
 * valor.
 */
class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** True cuando el usuario ya vio (o saltó) el tutorial inicial. */
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    /** Estado del bloqueo de acceso. Si es false, se muestra la pantalla de credenciales. */
    val accessUnlockedState: MutableState<Boolean> by lazy {
        mutableStateOf(prefs.getBoolean(KEY_ACCESS_UNLOCKED, false))
    }
    var accessUnlocked: Boolean
        get() = accessUnlockedState.value
        set(value) {
            accessUnlockedState.value = value
            prefs.edit().putBoolean(KEY_ACCESS_UNLOCKED, value).apply()
        }

    fun unlockAccess(email: String) {
        accessUnlockedState.value = true
        prefs.edit()
            .putBoolean(KEY_ACCESS_UNLOCKED, true)
            .putString(KEY_ACCESS_EMAIL, email.trim())
            .apply()
    }

    fun lockAccess() {
        accessUnlockedState.value = false
        prefs.edit()
            .putBoolean(KEY_ACCESS_UNLOCKED, false)
            .remove(KEY_ACCESS_EMAIL)
            .apply()
    }

    /** Modo de tema preferido. Default = SYSTEM. */
    val themeModeState: MutableState<ThemeMode> by lazy {
        mutableStateOf(
            ThemeMode.fromKey(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.key) ?: ThemeMode.SYSTEM.key),
        )
    }
    var themeMode: ThemeMode
        get() = themeModeState.value
        set(value) {
            themeModeState.value = value
            prefs.edit().putString(KEY_THEME_MODE, value.key).apply()
        }

    /** Auto-guardado del editor (default = true). */
    val autoSaveState: MutableState<Boolean> by lazy {
        mutableStateOf(prefs.getBoolean(KEY_AUTO_SAVE, true))
    }
    var autoSave: Boolean
        get() = autoSaveState.value
        set(value) {
            autoSaveState.value = value
            prefs.edit().putBoolean(KEY_AUTO_SAVE, value).apply()
        }

    /** Criterio de orden de la lista. Default = updated. */
    val sortByState: MutableState<SortBy> by lazy {
        mutableStateOf(
            SortBy.fromKey(prefs.getString(KEY_SORT_BY, SortBy.UPDATED.key) ?: SortBy.UPDATED.key),
        )
    }
    var sortBy: SortBy
        get() = sortByState.value
        set(value) {
            sortByState.value = value
            prefs.edit().putString(KEY_SORT_BY, value.key).apply()
        }

    // -----------------------------------------------------------------
    // Settings de exportación a PDF
    // -----------------------------------------------------------------

    /** Densidad tipográfica del PDF (compacto / normal / amplio). */
    val pdfDensityState: MutableState<PdfDensity> by lazy {
        mutableStateOf(
            PdfDensity.fromKey(prefs.getString(KEY_PDF_DENSITY, PdfDensity.NORMAL.key) ?: PdfDensity.NORMAL.key),
        )
    }
    var pdfDensity: PdfDensity
        get() = pdfDensityState.value
        set(value) {
            pdfDensityState.value = value
            prefs.edit().putString(KEY_PDF_DENSITY, value.key).apply()
        }

    /** Margen del PDF (compacto / normal / amplio). */
    val pdfMarginState: MutableState<PdfMargin> by lazy {
        mutableStateOf(
            PdfMargin.fromKey(prefs.getString(KEY_PDF_MARGIN, PdfMargin.NORMAL.key) ?: PdfMargin.NORMAL.key),
        )
    }
    var pdfMargin: PdfMargin
        get() = pdfMarginState.value
        set(value) {
            pdfMarginState.value = value
            prefs.edit().putString(KEY_PDF_MARGIN, value.key).apply()
        }

    /** Incluir portada con título grande + número de notas + fecha. */
    val pdfIncludeCoverState: MutableState<Boolean> by lazy {
        mutableStateOf(prefs.getBoolean(KEY_PDF_COVER, true))
    }
    var pdfIncludeCover: Boolean
        get() = pdfIncludeCoverState.value
        set(value) {
            pdfIncludeCoverState.value = value
            prefs.edit().putBoolean(KEY_PDF_COVER, value).apply()
        }

    /** Imprimir watermark "OreoExperience Notes" en el footer. */
    val pdfWatermarkState: MutableState<Boolean> by lazy {
        mutableStateOf(prefs.getBoolean(KEY_PDF_WATERMARK, true))
    }
    var pdfWatermark: Boolean
        get() = pdfWatermarkState.value
        set(value) {
            pdfWatermarkState.value = value
            prefs.edit().putBoolean(KEY_PDF_WATERMARK, value).apply()
        }

    /** PDF a color (true) o monocromo en escala de grises (false). */
    val pdfColorState: MutableState<Boolean> by lazy {
        mutableStateOf(prefs.getBoolean(KEY_PDF_COLOR, true))
    }
    var pdfColor: Boolean
        get() = pdfColorState.value
        set(value) {
            pdfColorState.value = value
            prefs.edit().putBoolean(KEY_PDF_COLOR, value).apply()
        }

    /** Snapshot inmutable de los settings para pasar al PdfExportManager. */
    fun pdfExportSettings(): PdfExportSettings = PdfExportSettings(
        density = pdfDensity,
        margin = pdfMargin,
        includeCover = pdfIncludeCover,
        watermark = pdfWatermark,
        colored = pdfColor,
    )

    // -----------------------------------------------------------------
    // Settings de Inteligencia Artificial (EncryptedSharedPreferences)
    // -----------------------------------------------------------------

    /** SharedPreferences cifrado para datos sensibles (API key). */
    private val aiPrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREF_AI_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** Estado reactivo de aiEnabled para que Settings se recomponga. */
    val aiEnabledState: MutableState<Boolean> by lazy {
        mutableStateOf(aiPrefs.getBoolean(KEY_AI_ENABLED, false))
    }
    var aiEnabled: Boolean
        get() = aiEnabledState.value
        set(value) {
            aiEnabledState.value = value
            aiPrefs.edit().putBoolean(KEY_AI_ENABLED, value).apply()
        }

    /** API key cifrada. No se expone como MutableState (no necesita reactividad). */
    var aiApiKey: String
        get() = aiPrefs.getString(KEY_AI_API_KEY, "").orEmpty()
        set(value) = aiPrefs.edit().putString(KEY_AI_API_KEY, value).apply()

    /** Temperatura de generación (0.0–1.0). */
    val aiTemperatureState: MutableState<Float> by lazy {
        mutableStateOf(aiPrefs.getFloat(KEY_AI_TEMPERATURE, 0.3f))
    }
    var aiTemperature: Float
        get() = aiTemperatureState.value
        set(value) {
            aiTemperatureState.value = value
            aiPrefs.edit().putFloat(KEY_AI_TEMPERATURE, value).apply()
        }

    /** True cuando el usuario habilitó IA y tiene una API key configurada. */
    fun hasAiConfigured(): Boolean = aiEnabled && aiApiKey.isNotBlank()

    // -----------------------------------------------------------------
    // Update system (cache del check de actualizaciones)
    // -----------------------------------------------------------------

    /** Epoch millis del último check exitoso. 0 = nunca. */
    var updateLastCheckMillis: Long
        get() = prefs.getLong(KEY_UPDATE_LAST_CHECK, 0L)
        set(value) = prefs.edit().putLong(KEY_UPDATE_LAST_CHECK, value).apply()

    /** tagName de la última release encontrada en el último check. */
    var updateLastTagName: String
        get() = prefs.getString(KEY_UPDATE_LAST_TAG, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_UPDATE_LAST_TAG, value).apply()

    /** True si la última release chequeada era crítica. */
    var updateLastCriticalFlag: Boolean
        get() = prefs.getBoolean(KEY_UPDATE_LAST_CRITICAL, false)
        set(value) = prefs.edit().putBoolean(KEY_UPDATE_LAST_CRITICAL, value).apply()

    /** tagName que el usuario postergó. Vacío = no hay postergación activa. */
    var updatePostponedTag: String
        get() = prefs.getString(KEY_UPDATE_POSTPONED, "").orEmpty()
        set(value) = prefs.edit().putString(KEY_UPDATE_POSTPONED, value).apply()

    // -----------------------------------------------------------------
    // Splash — warm/cold start
    // -----------------------------------------------------------------

    /**
     * Epoch millis de cuándo la Activity fue puesta en background por
     * última vez. 0 = nunca o app recién instalada. Se usa para
     * detectar "warm start" (re-apertura rápida) y acortar/omitir el
     * splash Compose, dejando solo el splash nativo del sistema.
     */
    var lastBackgroundedAt: Long
        get() = prefs.getLong(KEY_LAST_BACKGROUNDED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_BACKGROUNDED_AT, value).apply()

    companion object {
        private const val PREF_NAME = "oreo_notes_prefs"
        private const val PREF_AI_NAME = "oreo_ai_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_ACCESS_UNLOCKED = "access_unlocked"
        private const val KEY_ACCESS_EMAIL = "access_email"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AUTO_SAVE = "auto_save"
        private const val KEY_SORT_BY = "sort_by"
        private const val KEY_PDF_DENSITY = "pdf_density"
        private const val KEY_PDF_MARGIN = "pdf_margin"
        private const val KEY_PDF_COVER = "pdf_cover"
        private const val KEY_PDF_WATERMARK = "pdf_watermark"
        private const val KEY_PDF_COLOR = "pdf_color"
        private const val KEY_AI_ENABLED = "ai_enabled"
        private const val KEY_AI_API_KEY = "ai_api_key"
        private const val KEY_AI_TEMPERATURE = "ai_temperature"
        private const val KEY_UPDATE_LAST_CHECK = "update_last_check"
        private const val KEY_UPDATE_LAST_TAG = "update_last_tag"
        private const val KEY_UPDATE_LAST_CRITICAL = "update_last_critical"
        private const val KEY_UPDATE_POSTPONED = "update_postponed"
        private const val KEY_LAST_BACKGROUNDED_AT = "last_backgrounded_at"
    }
}

/** Modo de tema disponible para el usuario. */
enum class ThemeMode(val key: String, val label: String) {
    SYSTEM("system", "Sistema"),
    LIGHT("light", "Claro"),
    DARK("dark", "Oscuro");

    companion object {
        fun fromKey(k: String): ThemeMode = values().firstOrNull { it.key == k } ?: SYSTEM
    }
}

/** Criterio de ordenamiento de la lista de notas. */
enum class SortBy(val key: String, val label: String) {
    UPDATED("updated", "Fecha de edición"),
    CREATED("created", "Fecha de creación"),
    TITLE("title", "Título");

    companion object {
        fun fromKey(k: String): SortBy = values().firstOrNull { it.key == k } ?: UPDATED
    }
}

/**
 * Densidad tipográfica del PDF exportado. Cambia el `bodyTextSize`
 * base y la separación entre bloques. "Normal" es lo que veníamos
 * usando hasta este PR; "Compacto" reduce 1 sp todo y "Amplio" suma
 * 1.5 sp + más espaciado para que entre menos por página pero se lea
 * más cómodo.
 */
enum class PdfDensity(
    val key: String,
    val label: String,
    val bodySizeSp: Float,
    val titleSizeSp: Float,
    val lineExtra: Float,
) {
    COMPACT("compact", "Compacta", 11f, 20f, 1.10f),
    NORMAL("normal",  "Normal",   12f, 22f, 1.18f),
    LOOSE("loose",    "Amplia",   13.5f, 24f, 1.26f);

    companion object {
        fun fromKey(k: String): PdfDensity = values().firstOrNull { it.key == k } ?: NORMAL
    }
}

/** Margen lateral del PDF (en pt; 1 pt ≈ 0.353 mm). */
enum class PdfMargin(
    val key: String,
    val label: String,
    val horizontal: Float,
    val vertical: Float,
) {
    TIGHT("tight",   "Compactos", 36f, 40f),
    NORMAL("normal", "Normales",  48f, 56f),
    WIDE("wide",     "Amplios",   64f, 72f);

    companion object {
        fun fromKey(k: String): PdfMargin = values().firstOrNull { it.key == k } ?: NORMAL
    }
}

/**
 * Snapshot inmutable de los settings de exportación. Se pasa al
 * [PdfExportManager] para no acoplar la lógica de PDF a las
 * SharedPreferences.
 */
data class PdfExportSettings(
    val density: PdfDensity = PdfDensity.NORMAL,
    val margin: PdfMargin = PdfMargin.NORMAL,
    val includeCover: Boolean = true,
    val watermark: Boolean = true,
    val colored: Boolean = true,
)
