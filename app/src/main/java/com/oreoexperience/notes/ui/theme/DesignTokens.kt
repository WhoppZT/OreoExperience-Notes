package com.oreoexperience.notes.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Tokens de espaciado centralizados.
 *
 * Reemplaza todos los números mágicos de padding/margin/spacing
 * en la app por referencias semánticas consistentes.
 *
 * Escala: 4dp base (material grid).
 */
object OreoSpacing {
    /** 2dp — separación mínima, entre elementos muy cercanos */
    val xxs = 2.dp
    /** 4dp — separación íntima, entre icono y texto */
    val xs  = 4.dp
    /** 8dp — separación estándar entre elementos hermanos */
    val sm  = 8.dp
    /** 12dp — separación entre grupos pequeños */
    val md  = 12.dp
    /** 16dp — separación estándar de contenido (padding horizontal típico) */
    val lg  = 16.dp
    /** 24dp — separación entre secciones */
    val xl  = 24.dp
    /** 32dp — separación grande entre bloques */
    val xxl = 32.dp
    /** 48dp — padding de pantalla amplio */
    val xxxl = 48.dp
    /** 64dp — padding máximo (tableta / expanded) */
    val huge = 64.dp
}

/**
 * Tokens de duración de animación centralizados.
 *
 * Todos los tween/spring durations de la app deben usar estas
 * constantes para mantener consistencia temporal.
 */
object OreoDuration {
    /** 100ms — microinteracciones (tap feedback, color changes) */
    val INSTANT    = 100
    /** 200ms — transiciones rápidas (nav, opacity) */
    val FAST       = 200
    /** 300ms — transiciones estándar (sheet, card entry) */
    val NORMAL     = 300
    /** 500ms — transiciones lentas (hero, morph) */
    val SLOW       = 500
    /** 800ms — transiciones dramáticas (splash exit, reveal) */
    val DRAMATIC   = 800
    /** 1200ms — transiciones cinematográficas (splash full) */
    val CINEMATIC  = 1200

    // ─── Stagger delays (cascada entre elementos) ───
    /** 30ms — cascada ultra-rápida (pocas partículas) */
    val STAGGER_TINY   = 30
    /** 60ms — cascada estándar (listas de cards) */
    val STAGGER_NORMAL = 60
    /** 100ms — cascada lenta (elementos grandes) */
    val STAGGER_SLOW   = 100

    // ─── Infinite loop durations ───
    /** 2000ms — loops de pulso / glow */
    val PULSE      = 2000
    /** 2500ms — loops de gradiente */
    val GRADIENT   = 2500
    /** 3000ms — loops de glow lento */
    val GLOW       = 3000
    /** 20000ms — loops de rotación lenta (estrellas) */
    val ROTATION   = 20000
}

/**
 * Tokens de elevación y profundidad.
 *
 * Define niveles de elevación consistentes que combinan
 * sombra (shadow) y tinte de superficie (tonal elevation).
 */
object OreoElevation {
    /** 0dp — superficie plana, sin sombra (fondo de pantalla) */
    val None    = 0.dp
    /** 1dp — sutil elevación (cards en reposo) */
    val Low     = 1.dp
    /** 3dp — elevación media (cards interactivas, FAB) */
    val Medium  = 3.dp
    /** 6dp — elevación alta (sheets, diálogos) */
    val High    = 6.dp
    /** 12dp — elevación máxima (modales, popups) */
    val Highest = 12.dp
}

/**
 * Tokens tipográficos de uso común (tamaños que no están en Material3 TypeScale).
 */
object OreoFontSize {
    val Caption   = 10.sp
    val Overline  = 9.sp
}
