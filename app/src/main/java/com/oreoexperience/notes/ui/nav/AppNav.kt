package com.oreoexperience.notes.ui.nav

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.oreoexperience.notes.ui.theme.AuroraBackground
import com.oreoexperience.notes.ui.viewer.ViewerScreen

object Routes {
    const val Home = "home"
    const val Editor = "editor/{id}"
    const val Viewer = "viewer/{id}"

    fun editor(id: Long) = "editor/$id"
    fun viewer(id: Long) = "viewer/$id"
}

private const val NAV_DURATION_MS = 280

@Composable
fun AppNav() {
    val nav = rememberNavController()
    // Una vez que la pantalla de carga termina su fade-out, dejamos de
    // componerla. Mientras tanto se queda encima del NavHost bloqueando
    // los toques (su fondo gradient es opaco).
    var showSplash by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo aurora compartido por toda la app (debajo de las pantallas).
        AuroraBackground()

        NavHost(
            navController = nav,
            startDestination = Routes.Home,
            // Las transiciones por defecto: el destino entra deslizando
            // desde la derecha y desvanece, y al volver hace lo opuesto.
            // Cada `composable` puede sobrescribirlas si quiere algo
            // distinto, pero por ahora el lookup uniforme se siente bien.
            enterTransition = {
                fadeIn(tween(NAV_DURATION_MS)) +
                    slideIntoContainer(SlideDirection.Start, tween(NAV_DURATION_MS))
            },
            exitTransition = {
                fadeOut(tween(NAV_DURATION_MS / 2)) +
                    slideOutOfContainer(SlideDirection.Start, tween(NAV_DURATION_MS))
            },
            popEnterTransition = {
                fadeIn(tween(NAV_DURATION_MS)) +
                    slideIntoContainer(SlideDirection.End, tween(NAV_DURATION_MS))
            },
            popExitTransition = {
                fadeOut(tween(NAV_DURATION_MS / 2)) +
                    slideOutOfContainer(SlideDirection.End, tween(NAV_DURATION_MS))
            },
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    onNew = { nav.navigate(Routes.editor(0L)) },
                    onOpen = { id -> nav.navigate(Routes.viewer(id)) },
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
                    onSaved = { newId ->
                        nav.popBackStack()
                        if (id == 0L && newId > 0L) nav.navigate(Routes.viewer(newId))
                    },
                )
            }
            composable(
                Routes.Viewer,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                ViewerScreen(
                    discursoId = id,
                    onBack = { nav.popBackStack() },
                    onEdit = { nav.navigate(Routes.editor(id)) },
                )
            }
        }

        // Pantalla de carga sobre todo lo demás. Se desmonta sola al
        // terminar el fade-out (vía [onFinished]).
        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        }
    }
}
