package com.oreoexperience.notes.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Specs de movimiento compartidos por toda la app.
 *
 * Estilo EXPRESIVO / JUGUETÓN:
 *   - dampingRatio bajo (0.30–0.55) → rebote pronunciado y divertido.
 *   - stiffness moderado (150–280) → velocidad ágil con personalidad.
 *   - Cada spring tiene su propia "personalidad" para dar vida a la UI.
 *
 * Ver [OreoDuration] para duraciones y delays centralizados.
 */
object OreoMotion {
    // Curvas para tween determinístico (transiciones de nav, fades).
    val EaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val EaseInOut = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
    val EaseEmphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    // ─── Springs (Estilo Expresivo) ────────────────────────────

    /** Press feedback: rebote juguetón al tocar. */
    fun <T> SpringPress() = spring<T>(
        dampingRatio = 0.40f,
        stiffness = 280f,
    )

    /** Card/row entry: bounce pronunciado y divertido. */
    fun <T> SpringCard() = spring<T>(
        dampingRatio = 0.35f,
        stiffness = 200f,
    )

    /** Nav transitions: juguetón pero no mareante. */
    fun <T> SpringNav() = spring<T>(
        dampingRatio = 0.55f,
        stiffness = 220f,
    )

    /** Hero/shared element: elástico, para transiciones de escala grandes. */
    fun <T> SpringHero() = spring<T>(
        dampingRatio = 0.30f,
        stiffness = 160f,
    )

    /** Dismiss/sheet: con vida, rebote al cerrar. */
    fun <T> SpringDismiss() = spring<T>(
        dampingRatio = 0.45f,
        stiffness = 180f,
    )

    /** Staggered cascade: rebote pronunciado y cascada divertida. */
    fun <T> SpringDrop() = spring<T>(
        dampingRatio = 0.35f,
        stiffness = 150f,
    )

    /** Wiggle/jiggle: temblor juguetón al tocar. */
    fun <T> SpringWiggle() = spring<T>(
        dampingRatio = 0.25f,
        stiffness = 350f,
    )

    /** Pop: aparición explosiva y divertida. */
    fun <T> SpringPop() = spring<T>(
        dampingRatio = 0.30f,
        stiffness = 250f,
    )

    /** Nav elegante: transiciones suaves, poco rebote, sensación premium. */
    fun <T> SpringNavElegant() = spring<T>(
        dampingRatio = 0.80f,
        stiffness = 420f,
    )
}
