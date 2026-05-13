package com.oreoexperience.notes.ui.nav

import androidx.compose.animation.core.FiniteAnimationSpec
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
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.editor.EditorScreen
import com.oreoexperience.notes.ui.home.HomeScreen
import com.oreoexperience.notes.ui.onboarding.OnboardingScreen
import com.oreoexperience.notes.ui.servicio.ServicioCampoScreen
import com.oreoexperience.notes.ui.settings.SettingsScreen
import com.oreoexperience.notes.ui.splash.SplashScreen
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import com.oreoexperience.notes.ui.trash.TrashScreen

object Routes {
    const val Home = "home"
    const val Editor = "editor/{id}?cat={cat}"
    const val Settings = "settings"
    const val Trash = "trash"
    const val ServicioCampo = "servicio_campo"
    fun editor(id: Long, category: String? = null): String =
        if (category == null) "editor/$id?cat=" else "editor/$id?cat=$category"
}

// Slide deliberadamente largo (380ms) con curva emphasized — un spring
// sin rebote completaba el viaje en menos de 200ms y se sentía
// "instantáneo". Tween con emphasized da una transición visible.
private fun slideSpec(): FiniteAnimationSpec<IntOffset> =
    tween(durationMillis = 380, easing = OreoMotion.EaseEmphasized)
private fun fadeSpec(): FiniteAnimationSpec<Float> =
    tween(durationMillis = 260, easing = OreoMotion.EaseEmphasized)
private fun scaleSpec(): FiniteAnimationSpec<Float> =
    tween(durationMillis = 360, easing = OreoMotion.EaseEmphasized)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val container = LocalAppContainer.current
    var showSplash by remember { mutableStateOf(true) }
    var showOnboarding by remember {
        mutableStateOf(!container.userPreferences.onboardingDone)
    }
    // El sistema de credenciales fue removido en v0.9.2-jw: la app
    // entra directo al Home tras el splash. LicenseManager y
    // AccessScreen siguen existiendo en el repo por si se quiere
    // restituir el gate más adelante.

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OreoPalette.Bg0),
    ) {
        // Push transitions usan slide + fade. La pantalla nueva entra
        // desde la derecha empujando a la actual; al volver, se invierte.
        // Las curvas son spring para que se sientan vivas en lugar de
        // un tween rígido.
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
                    targetOffsetX = { -it / 4 },
                ) + fadeOut(animationSpec = fadeSpec())
            },
            popEnterTransition = {
                slideInHorizontally(
                    animationSpec = slideSpec(),
                    initialOffsetX = { -it / 4 },
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
                    onNew = { category ->
                        nav.navigate(Routes.editor(0L, category))
                    },
                    onOpen = { id -> nav.navigate(Routes.editor(id)) },
                    onSettings = { nav.navigate(Routes.Settings) },
                    onOpenServicio = { nav.navigate(Routes.ServicioCampo) },
                )
            }
            composable(
                Routes.Editor,
                // Hero feel: cuando se abre una nota desde el Home, la pantalla
                // del editor entra escalando desde 0.92 + un fade-in. Combinado
                // con el press-feedback de las cards en el Home, da la
                // sensación de que la tarjeta se "expande" hacia el editor.
                // Al volver hace exactamente lo inverso.
                enterTransition = {
                    scaleIn(
                        animationSpec = scaleSpec(),
                        initialScale = 0.92f,
                    ) + fadeIn(animationSpec = fadeSpec())
                },
                exitTransition = {
                    scaleOut(
                        animationSpec = scaleSpec(),
                        targetScale = 1.04f,
                    ) + fadeOut(animationSpec = fadeSpec())
                },
                popEnterTransition = {
                    scaleIn(
                        animationSpec = scaleSpec(),
                        initialScale = 1.04f,
                    ) + fadeIn(animationSpec = fadeSpec())
                },
                popExitTransition = {
                    scaleOut(
                        animationSpec = scaleSpec(),
                        targetScale = 0.92f,
                    ) + fadeOut(animationSpec = fadeSpec())
                },
                arguments = listOf(
                    navArgument("id") { type = NavType.LongType },
                    navArgument("cat") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val cat = entry.arguments?.getString("cat").orEmpty()
                EditorScreen(
                    discursoId = id,
                    initialCategoryKey = cat.ifBlank { null },
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
            composable(Routes.ServicioCampo) {
                ServicioCampoScreen(
                    onBack = { nav.popBackStack() },
                )
            }
        }

        if (showSplash) {
            SplashScreen(onFinished = { showSplash = false })
        } else if (showOnboarding) {
            OnboardingScreen(
                onFinish = {
                    container.userPreferences.onboardingDone = true
                    showOnboarding = false
                },
            )
        }
    }
}

