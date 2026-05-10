package com.oreoexperience.notes.ui.nav

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oreoexperience.notes.ui.editor.EditorScreen
import com.oreoexperience.notes.ui.home.HomeScreen
import com.oreoexperience.notes.ui.splash.SplashScreen
import com.oreoexperience.notes.ui.theme.OreoPalette

object Routes {
    const val Home = "home"
    const val Editor = "editor/{id}"
    fun editor(id: Long) = "editor/$id"
}

/**
 * Animaciones de navegación estilo iOS:
 *
 *   - Push horizontal completo desde la derecha con un **spring**
 *     (low-bouncy + stiffness medium-low) en lugar de un tween — así
 *     se asienta con un sutil rebote en lugar de cortarse seco.
 *   - El destino que se va se desliza un cuarto de pantalla a la
 *     izquierda (parallax típico de iOS) con fade simultáneo.
 *   - El fade y el scale (sutil, 0.97 → 1.0) usan tween corto
 *     (220 ms) para que el deslizamiento no se vea entrecortado al
 *     comienzo.
 *
 * Centralizo los specs en estas funciones para que todas las
 * pantallas se vean consistentes.
 */
private fun slideSpring() = spring<androidx.compose.ui.unit.IntOffset>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

private fun scaleSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

private fun fadeSpec() = tween<Float>(durationMillis = 220)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    var showSplash by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OreoPalette.Bg0),
    ) {
        NavHost(
            navController = nav,
            startDestination = Routes.Home,
            // Entrada del destino nuevo: slide desde la derecha con
            // spring + fade + scale sutil. El scale agrega esa
            // sensación de "modal" que tiene iOS al abrir una nota.
            enterTransition = {
                slideInHorizontally(
                    animationSpec = slideSpring(),
                    initialOffsetX = { it },
                ) + fadeIn(animationSpec = fadeSpec()) +
                    scaleIn(
                        animationSpec = scaleSpring(),
                        initialScale = 0.97f,
                    )
            },
            // El de origen se va parallax (un cuarto a la izquierda)
            // + fade-out sutil. Mismo spring que la entrada para que
            // el movimiento se sienta acoplado.
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = slideSpring(),
                    targetOffsetX = { -it / 4 },
                ) + fadeOut(animationSpec = fadeSpec())
            },
            // Pop: el origen vuelve desde la izquierda (parallax
            // inverso) con el mismo spring.
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = slideSpring(),
                    initialOffsetX = { -it / 4 },
                ) + fadeIn(animationSpec = fadeSpec())
            },
            // Pop: el destino se va deslizando completo hacia la
            // derecha + scale-out sutil.
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = slideSpring(),
                    targetOffsetX = { it },
                ) + fadeOut(animationSpec = fadeSpec()) +
                    scaleOut(
                        animationSpec = scaleSpring(),
                        targetScale = 0.97f,
                    )
            },
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    onNew = { nav.navigate(Routes.editor(0L)) },
                    onOpen = { id -> nav.navigate(Routes.editor(id)) },
                )
            }
            composable(
                Routes.Editor,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                EditorScreen(
                    discursoId = id,
                    onBack = { nav.popBackStack() },
                    onSaved = { _ -> nav.popBackStack() },
                )
            }
        }

        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        }
    }
}
