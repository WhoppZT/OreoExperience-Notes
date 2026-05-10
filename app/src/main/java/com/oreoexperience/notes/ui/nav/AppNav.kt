package com.oreoexperience.notes.ui.nav

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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

// Transiciones estilo iOS: push horizontal completo desde la derecha
// (slide del 100%) + fade-out muy sutil del de origen. Más rápidas que
// la versión anterior — duraciones tipo iOS (260/220 ms).
private const val NAV_DURATION_MS = 260
private const val NAV_EXIT_DURATION_MS = 220

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
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(NAV_DURATION_MS, easing = FastOutSlowInEasing),
                    initialOffsetX = { it },
                )
            },
            exitTransition = {
                fadeOut(tween(NAV_EXIT_DURATION_MS, easing = FastOutSlowInEasing)) +
                    slideOutHorizontally(
                        animationSpec = tween(NAV_DURATION_MS, easing = FastOutSlowInEasing),
                        targetOffsetX = { -it / 4 },
                    )
            },
            popEnterTransition = {
                fadeIn(tween(NAV_EXIT_DURATION_MS, easing = FastOutSlowInEasing)) +
                    slideInHorizontally(
                        animationSpec = tween(NAV_DURATION_MS, easing = FastOutSlowInEasing),
                        initialOffsetX = { -it / 4 },
                    )
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(NAV_DURATION_MS, easing = FastOutSlowInEasing),
                    targetOffsetX = { it },
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
