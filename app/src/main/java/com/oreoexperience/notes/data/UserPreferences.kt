package com.oreoexperience.notes.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

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

    companion object {
        private const val PREF_NAME = "oreo_notes_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_ACCESS_UNLOCKED = "access_unlocked"
        private const val KEY_ACCESS_EMAIL = "access_email"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_AUTO_SAVE = "auto_save"
        private const val KEY_SORT_BY = "sort_by"
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
