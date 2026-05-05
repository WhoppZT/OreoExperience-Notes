package com.oreoexperience.notes.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oreoexperience.notes.ui.editor.EditorScreen
import com.oreoexperience.notes.ui.home.HomeScreen
import com.oreoexperience.notes.ui.theme.AuroraBackground
import com.oreoexperience.notes.ui.viewer.ViewerScreen

object Routes {
    const val Home = "home"
    const val Editor = "editor/{id}"
    const val Viewer = "viewer/{id}"

    fun editor(id: Long) = "editor/$id"
    fun viewer(id: Long) = "viewer/$id"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    Box(modifier = Modifier.fillMaxSize()) {
        // Fondo aurora compartido por toda la app (debajo de las pantallas).
        AuroraBackground()

        NavHost(navController = nav, startDestination = Routes.Home) {
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
    }
}
