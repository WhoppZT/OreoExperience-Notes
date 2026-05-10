package com.oreoexperience.notes.ui.nav

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
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
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.auth.AccessScreen
import com.oreoexperience.notes.ui.editor.EditorScreen
import com.oreoexperience.notes.ui.home.HomeScreen
import com.oreoexperience.notes.ui.onboarding.OnboardingScreen
import com.oreoexperience.notes.ui.settings.SettingsScreen
import com.oreoexperience.notes.ui.splash.SplashScreen
import com.oreoexperience.notes.ui.theme.OreoPalette
import com.oreoexperience.notes.ui.trash.TrashScreen

object Routes {
    const val Home = "home"
    const val Editor = "editor/{id}"
    const val Settings = "settings"
    const val Trash = "trash"
    fun editor(id: Long) = "editor/$id"
}

private val IosEaseOut = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

private fun slideSpec(): FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = 360, easing = IosEaseOut)

private fun fadeSpec(): FiniteAnimationSpec<Float> =
    tween(durationMillis = 160, easing = IosEaseOut)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val container = LocalAppContainer.current
    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember {
        mutableStateOf(!container.userPreferences.onboardingDone)
    }
    val accessUnlocked by container.userPreferences.accessUnlockedState

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
                    animationSpec = slideSpec(),
                    initialOffsetX = { it },
                ) + fadeIn(animationSpec = fadeSpec())
            },
            exitTransition = {
                slideOutHorizontally(
                    animationSpec = slideSpec(),
                    targetOffsetX = { -it / 3 },
                ) + fadeOut(animationSpec = fadeSpec())
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = slideSpec(),
                    initialOffsetX = { -it / 3 },
                ) + fadeIn(animationSpec = fadeSpec())
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = slideSpec(),
                    targetOffsetX = { it },
                ) + fadeOut(animationSpec = fadeSpec())
            },
        ) {
            composable(Routes.Home) {
                HomeScreen(
                    onNew = { nav.navigate(Routes.editor(0L)) },
                    onOpen = { id -> nav.navigate(Routes.editor(id)) },
                    onSettings = { nav.navigate(Routes.Settings) },
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

        if (!showSplash && !accessUnlocked) {
            AccessScreen(
                onUnlocked = { email ->
                    container.userPreferences.unlockAccess(email)
                    showOnboarding = !container.userPreferences.onboardingDone
                },
            )
        } else if (!showSplash && showOnboarding) {
            OnboardingScreen(
                onFinish = {
                    container.userPreferences.onboardingDone = true
                    showOnboarding = false
                },
            )
        }
    }
}
