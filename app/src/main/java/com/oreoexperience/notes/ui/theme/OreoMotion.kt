package com.oreoexperience.notes.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * Specs de movimiento compartidos por toda la app.
 *
 * Las curvas siguen la familia "iOS / Material expressive": una salida
 * suave con un toque de elasticidad. Las usamos como un único punto de
 * verdad para que las animaciones se sientan consistentes en todas las
 * pantallas (splash, nav, press feedback, sheets, switches, etc).
 *
 * v2: subimos amplitudes y bajamos stiffness — los springs anteriores
 * eran tan rápidos que prácticamente no se percibían en pantalla.
 */
object OreoMotion {
    // Curvas para tween — útiles cuando una transición debe ser
    // determinística (ej. fades cortos, anims que deben durar X ms).
    val EaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
    val EaseInOut = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
    val EaseEmphasized = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** Spring snappy con un pelín de rebote — press feedback bien notorio. */
    fun <T> SpringCrisp() = spring<T>(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessMedium,
    )

    /** Spring claramente bouncy — sensación tipo iOS al "soltar". */
    fun <T> SpringBouncy() = spring<T>(
        dampingRatio = 0.45f,
        stiffness = 280f,
    )

    /** Spring para entradas de hero (splash, onboarding). Muy elástico. */
    fun <T> SpringHero() = spring<T>(
        dampingRatio = 0.42f,
        stiffness = 120f,
    )

    /** Spring para transiciones de pantalla (nav). Visible pero estable. */
    fun <T> SpringNav() = spring<T>(
        dampingRatio = 0.85f,
        stiffness = 260f,
    )

    /** Spring para drops/cascadas de filas con rebote pronunciado. */
    fun <T> SpringDrop() = spring<T>(
        dampingRatio = 0.55f,
        stiffness = 180f,
    )
}
