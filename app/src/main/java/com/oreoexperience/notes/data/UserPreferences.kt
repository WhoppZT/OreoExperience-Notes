package com.oreoexperience.notes.data

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf

/**
 * Preferencias simples del usuario, persistidas en SharedPreferences y
 * expuestas como State observables para Compose.
 *
 * Por ahora sólo contiene la **escala de tipografía** del visor de
 * discurso (rango 0.8x – 2.0x), pero está pensado para crecer
 * (modo oscuro/claro override, color de tema, etc.).
 */
object UserPreferences {

    private const val PREFS = "oreo_notes_prefs"
    private const val KEY_FONT_SCALE = "font_scale"

    private val _fontScale = mutableFloatStateOf(1.0f)
    val fontScale: State<Float> = _fontScale

    private var initialized = false

    fun init(ctx: Context) {
        if (initialized) return
        initialized = true
        val prefs = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        _fontScale.floatValue = prefs.getFloat(KEY_FONT_SCALE, 1.0f).coerceIn(0.8f, 2.0f)
    }

    fun setFontScale(ctx: Context, value: Float) {
        val v = value.coerceIn(0.8f, 2.0f)
        _fontScale.floatValue = v
        ctx.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(KEY_FONT_SCALE, v)
            .apply()
    }
}
