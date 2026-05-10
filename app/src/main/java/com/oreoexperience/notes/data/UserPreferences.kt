package com.oreoexperience.notes.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Wrapper minimalista sobre SharedPreferences. Centraliza las flags
 * persistentes de la app (onboarding completado, escala de letra del
 * viewer cuando se reactive, etc.) para no duplicar las claves en
 * múltiples sitios.
 */
class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** True cuando el usuario ya vio (o saltó) el tutorial inicial. */
    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    companion object {
        private const val PREF_NAME = "oreo_notes_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}
