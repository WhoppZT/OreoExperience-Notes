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
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.editor.EditorScreen
import com.oreoexperience.notes.ui.home.HomeScreen
import com.oreoexperience.notes.ui.onboarding.OnboardingScreen
import com.oreoexperience.notes.ui.reader.ReaderScreen
import com.oreoexperience.notes.ui.settings.SettingsScreen
import com.oreoexperience.notes.ui.splash.SplashScreen
import com.oreoexperience.notes.ui.theme.OreoPalette
import com.oreoexperience.notes.ui.trash.TrashScreen

object Routes {
    const val Home = "home"
    const val Editor = "editor/{id}"
    const val Reader = "reader/{id}"
    const val Settings = "settings"
    const val Trash = "trash"
    fun editor(id: Long) = "editor/$id"
    fun reader(id: Long) = "reader/$id"
}

/**
 * Animaciones de navegación estilo iOS:
 *
 *   - Push horizontal completo desde la derecha con un spring
 *     **críticamente amortiguado** (sin overshoot) y stiffness medio,
 *     que reproduce la curva nativa de UIKit (`spring(response: 0.45,
 *     dampingFraction: 1.0)`).
 *   - El destino que se va se desliza un tercio a la izquierda
 *     (parallax típico de iOS) con fade simultáneo.
 *   - Fade rápido (180 ms con curva fast-out-slow-in) para que la
 *     transición se sienta fluida.
 *   - Scale de entrada sutil (0.985 → 1.0) — apenas perceptible,
 *     suficiente para dar la sensación de "asentar".
 */
private fun slideSpring() = spring<androidx.compose.ui.unit.IntOffset>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 380f,
)

private fun scaleSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = 420f,
)

private fun fadeSpec() = tween<Float>(
    durationMillis = 180,
    easing = androidx.compose.animation.core.FastOutSlowInEasing,
)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val container = LocalAppContainer.current
    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember {
        mutableStateOf(!container.userPreferences.onboardingDone)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OreoPalette.Bg0),
    ) {
        NavHost(
            navController = nav,
            startDestination = Routes.Home,
            // Push iOS: el destino entra desde la derecha con spring
            // críticamente amortiguado (sin overshoot) + fade rápido +
            // scale apenas perceptible.
            enterTransition = {
                slideInHorizontally(
                    animationSpec = slideSpring(),
                    initialOffsetX = { it },
                ) + fadeIn(animationSpec = fadeSpec()) +
                    scaleIn(
                        animationSpec = scaleSpring(),
                        initialScale = 0.985f,
                    )
            },
            // El de origen se va con parallax (un tercio a la
            // izquierda) + fade. Mismo spring que la entrada.
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = slideSpring(),
                    targetOffsetX = { -it / 3 },
                ) + fadeOut(animationSpec = fadeSpec())
            },
            // Pop: la origen vuelve desde la izquierda (parallax
            // inverso).
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = slideSpring(),
                    initialOffsetX = { -it / 3 },
                ) + fadeIn(animationSpec = fadeSpec())
            },
            // Pop: el destino se va deslizando hacia la derecha.
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = slideSpring(),
                    targetOffsetX = { it },
                ) + fadeOut(animationSpec = fadeSpec()) +
                    scaleOut(
                        animationSpec = scaleSpring(),
                        targetScale = 0.985f,
                    )
            },
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    onNew = { nav.navigate(Routes.editor(0L)) },
                    onOpen = { id -> nav.navigate(Routes.editor(id)) },
                    onSettings = { nav.navigate(Routes.Settings) },
                    onOpenReader = { id -> nav.navigate(Routes.reader(id)) },
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
                    onOpenReader = { savedId -> nav.navigate(Routes.reader(savedId)) },
                )
            }
            composable(
                Routes.Reader,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ReaderScreen(
                    discursoId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { _ -> nav.popBackStack() },
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onTrash = { nav.navigate(Routes.Trash) },
                )
            }
            composable(Routes.Trash) {
                TrashScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { id -> nav.navigate(Routes.editor(id)) },
                )
            }
        }

        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        }

        // Onboarding queda por encima del splash sólo en el primer
        // arranque. Una vez completado se persiste el flag para que
        // no vuelva a aparecer.
        if (!showSplash && showOnboarding) {
            OnboardingScreen(
                onFinish = {
                    container.userPreferences.onboardingDone = true
                    showOnboarding = false
                },
            )
        }
    }
}
