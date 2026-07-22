package com.oreoexperience.notes.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import kotlin.math.sin

/**
 * Modifier de entrada en cascada con bounce pronunciado y divertido.
 * La animación se ejecuta cuando cambia [triggerKey] o en la primera composición.
 */
fun Modifier.staggeredEntry(
    index: Int,
    triggerKey: Any? = null,
    perItemDelayMs: Int = 60,
    key: Any? = null,
): Modifier = composed {
    val anim = remember { Animatable(0f) }
    
    LaunchedEffect(triggerKey) {
        anim.snapTo(0f)
        kotlinx.coroutines.delay(index * perItemDelayMs.toLong())
        anim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.35f,
                stiffness = 200f,
            ),
        )
    }
    
    val p = anim.value
    this.alpha(p)
}

/**
 * Feedback de presión con escala Y rotación sutil — estilo juguetón.
 * Devuelve (modifier, interactionSource, isPressed).
 */
@Composable
fun Modifier.pressFeedback(
    scaleDown: Float = 0.92f,
    springSpec: androidx.compose.animation.core.SpringSpec<Float> = OreoMotion.SpringPress(),
): Triple<Modifier, MutableInteractionSource, Boolean> {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) scaleDown else 1f,
        animationSpec = springSpec,
        label = "pressFeedback",
    )
    val rotation by animateFloatAsState(
        targetValue = if (pressed) -2f else 0f,
        animationSpec = springSpec,
        label = "pressRotation",
    )
    return Triple(
        this.graphicsLayer {
            scaleX = scale
            scaleY = scale
            rotationZ = rotation
        },
        interaction,
        pressed,
    )
}

/**
 * Halo pulsante para notas fijadas — más pronunciado y con cambio de color.
 */
fun Modifier.pinnedGlow(
    color: Color = OreoPalette.Accent,
): Modifier = composed {
    val inf = rememberInfiniteTransition(label = "pinnedGlow")
    val alphaFactor by inf.animateFloat(
        initialValue = 0.03f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(3_000, easing = OreoMotion.EaseInOut),
        ),
        label = "pinnedGlowAlpha",
    )
    val glowScale by inf.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween<Float>(3_000, easing = OreoMotion.EaseInOut),
        ),
        label = "pinnedGlowScale",
    )
    this.drawBehind {
        val pad = 3.dp.toPx()
        val cornerPx = 18.dp.toPx()
        drawRoundRect(
            color = color.copy(alpha = alphaFactor),
            topLeft = Offset(-pad * glowScale, -pad * glowScale),
            size = Size(
                size.width + pad * 2 * glowScale,
                size.height + pad * 2 * glowScale,
            ),
            cornerRadius = CornerRadius(cornerPx, cornerPx),
        )
    }
}

/**
 * Entrada para secciones completas — con bounce.
 */
fun Modifier.sectionEntry(
    triggerKey: Any? = null,
): Modifier = composed {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(triggerKey) { started = true }
    val progress by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = OreoMotion.SpringCard(),
        label = "sectionEntry",
    )
    val translationYpx = (1f - progress) * with(LocalDensity.current) { 20.dp.toPx() }
    this.graphicsLayer {
        alpha = progress
        translationY = translationYpx
    }
}

/**
 * Jiggle/temblor juguetón al tocar.
 */
fun Modifier.wiggle(): Modifier = composed {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        anim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = 0.25f,
                stiffness = 350f,
            ),
        )
    }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    LaunchedEffect(pressed) {
        if (pressed) {
            anim.animateTo(
                targetValue = 1f,
                animationSpec = OreoMotion.SpringWiggle(),
            )
        } else {
            anim.snapTo(0f)
        }
    }
    val wiggleAngle = sin(anim.value * 6f) * 4f * (1f - anim.value)
    this.graphicsLayer {
        rotationZ = wiggleAngle
    }
}

/**
 * Aparición explosiva y divertida: scale 0 → 1.2 → 1.0.
 */
fun Modifier.popIn(
    triggerKey: Any? = null,
): Modifier = composed {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(triggerKey) { started = true }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(started) {
        if (started) {
            anim.animateTo(
                targetValue = 1f,
                animationSpec = OreoMotion.SpringPop(),
            )
        } else {
            anim.snapTo(0f)
        }
    }
    val overshoot = if (anim.value < 0.8f) {
        anim.value * 1.25f
    } else {
        1f + (1f - anim.value) * 0.5f
    }
    this.graphicsLayer {
        scaleX = overshoot
        scaleY = overshoot
        alpha = anim.value
    }
}

/**
 * Rebote al aparecer en pantalla — scale de 0.5 → 1.15 → 1.0.
 */
fun Modifier.bounceEntry(
    delayMs: Int = 0,
): Modifier = composed {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        anim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.30f,
                stiffness = 200f,
            ),
        )
    }
    val bounceScale = 0.5f + 0.65f * anim.value
    this.graphicsLayer {
        scaleX = bounceScale
        scaleY = bounceScale
        alpha = anim.value
    }
}

/**
 * Sacudida para errores — shake horizontal pronunciado.
 */
fun Modifier.shake(
    triggerKey: Any? = null,
): Modifier = composed {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(triggerKey) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.20f,
                stiffness = 400f,
            ),
        )
    }
    val shakeOffset = sin(anim.value * 12f) * 8f * (1f - anim.value)
    this.graphicsLayer {
        translationX = shakeOffset
    }
}
