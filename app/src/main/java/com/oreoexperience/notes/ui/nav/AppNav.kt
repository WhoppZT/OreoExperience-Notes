package com.oreoexperience.notes.ui.nav

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import com.oreoexperience.notes.ui.components.UpdateDialog
import com.oreoexperience.notes.ui.reader.ReaderScreen
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette
import com.oreoexperience.notes.ui.trash.TrashScreen
import com.oreoexperience.notes.data.update.CheckResult
import com.oreoexperience.notes.data.update.GitHubRelease
import kotlinx.coroutines.launch

object Routes {
    const val Home = "home"
    const val Editor = "editor/{id}?cat={cat}"
    const val Settings = "settings"
    const val Trash = "trash"
    const val ServicioCampo = "servicio_campo"
    const val Reader = "reader/{id}"
    fun editor(id: Long, category: String? = null): String =
        if (category == null) "editor/$id?cat=" else "editor/$id?cat=$category"
    fun reader(id: Long): String = "reader/$id"
}

// ─── Spring-based nav specs using OreoMotion ──────────────────
private val navSpringDamping = OreoMotion.SpringNavElegant<Float>().dampingRatio
private val navSpringStiffness = OreoMotion.SpringNavElegant<Float>().stiffness
private val cardSpringDamping = OreoMotion.SpringCard<Float>().dampingRatio
private val cardSpringStiffness = OreoMotion.SpringCard<Float>().stiffness

private fun slideInSpec() = spring<IntOffset>(
    dampingRatio = navSpringDamping,
    stiffness = navSpringStiffness,
)

private fun slideOutSpec() = spring<IntOffset>(
    dampingRatio = navSpringDamping,
    stiffness = navSpringStiffness,
)

private fun fadeSpec() = spring<Float>(
    dampingRatio = navSpringDamping,
    stiffness = navSpringStiffness,
)

private fun scaleInSpec() = spring<Float>(
    dampingRatio = cardSpringDamping,
    stiffness = cardSpringStiffness,
)

private fun scaleOutSpec() = spring<Float>(
    dampingRatio = cardSpringDamping,
    stiffness = cardSpringStiffness,
)

// Transiciones de entrada/salida elegantes: slide sutil + fade suave.
private fun pushEnter(): EnterTransition =
    slideInHorizontally(animationSpec = slideInSpec(), initialOffsetX = { it }) +
        fadeIn(animationSpec = fadeSpec(), initialAlpha = 0.5f)

private fun pushExit(): ExitTransition =
    slideOutHorizontally(animationSpec = slideOutSpec(), targetOffsetX = { -it / 3 }) +
        fadeOut(animationSpec = fadeSpec(), targetAlpha = 0.5f)

private fun popEnter(): EnterTransition =
    slideInHorizontally(animationSpec = slideInSpec(), initialOffsetX = { -it / 3 }) +
        fadeIn(animationSpec = fadeSpec(), initialAlpha = 0.5f)

private fun popExit(): ExitTransition =
    slideOutHorizontally(animationSpec = slideOutSpec(), targetOffsetX = { it }) +
        fadeOut(animationSpec = fadeSpec(), targetAlpha = 0.5f)

// Transiciones hero para el Editor: misma lógica slide que push/pop.
private fun heroEnter(): EnterTransition =
    slideInHorizontally(animationSpec = slideInSpec(), initialOffsetX = { it }) +
        fadeIn(animationSpec = fadeSpec(), initialAlpha = 0.5f)

private fun heroExit(): ExitTransition =
    slideOutHorizontally(animationSpec = slideOutSpec(), targetOffsetX = { -it / 3 }) +
        fadeOut(animationSpec = fadeSpec(), targetAlpha = 0.5f)

private fun heroPopEnter(): EnterTransition =
    slideInHorizontally(animationSpec = slideInSpec(), initialOffsetX = { -it / 3 }) +
        fadeIn(animationSpec = fadeSpec(), initialAlpha = 0.5f)

private fun heroPopExit(): ExitTransition =
    slideOutHorizontally(animationSpec = slideOutSpec(), targetOffsetX = { it }) +
        fadeOut(animationSpec = fadeSpec(), targetAlpha = 0.5f)

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val container = LocalAppContainer.current
    var showOnboarding by remember {
        mutableStateOf(!container.userPreferences.onboardingDone)
    }
    var pendingUpdate by remember { mutableStateOf<GitHubRelease?>(null) }
    var pendingCritical by remember { mutableStateOf(false) }
    var pendingSize by remember { mutableStateOf(0L) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Pre-warm: arrancar el chequeo de updates en paralelo con el splash
    // para que el resultado esté listo cuando la app sea visible.
    LaunchedEffect(Unit) {
        when (val result = container.updateManager.checkForUpdate()) {
            is CheckResult.Available -> {
                pendingUpdate = result.release
                pendingCritical = result.isCritical
                pendingSize = result.sizeBytes
            }
            else -> {} // up-to-date, postponed o error
        }
    }

    // Mostrar el dialog apenas el update esté listo y no esté ya visible.
    LaunchedEffect(pendingUpdate) {
        if (pendingUpdate != null) {
            showUpdateDialog = true
        }
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
        // Push transitions con spring — la pantalla nueva entra desde
        // la derecha con inercia natural y rebote sutil al llegar.
        NavHost(
            navController = nav,
            startDestination = Routes.Home,
            enterTransition = { pushEnter() },
            exitTransition = { pushExit() },
            popEnterTransition = { popEnter() },
            popExitTransition = { popExit() },
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
                // Hero transition: scale + fade con spring para que
                // la tarjeta parezca "expandirse" hacia el editor.
                enterTransition = { heroEnter() },
                exitTransition = { heroExit() },
                popEnterTransition = { heroPopEnter() },
                popExitTransition = { heroPopExit() },
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
                    onReaderMode = { noteId -> nav.navigate(Routes.reader(noteId)) },
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onTrash = { nav.navigate(Routes.Trash) },
                    onNavigateTo = { route ->
                        nav.navigate(route) {
                            launchSingleTop = true
                        }
                    },
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
                    onNavigateTo = { route ->
                        nav.navigate(route) {
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(
                Routes.Reader,
                arguments = listOf(navArgument("id") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: return@composable
                ReaderScreen(
                    discursoId = id,
                    onBack = { nav.popBackStack() },
                )
            }
        }

        if (showOnboarding) {
            OnboardingScreen(
                onFinish = {
                    container.userPreferences.onboardingDone = true
                    showOnboarding = false
                },
            )
        }

        if (showUpdateDialog && pendingUpdate != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(32.dp)
                    .background(Color.Black.copy(alpha = 0.5f)),
            )
            UpdateDialog(
                release = pendingUpdate!!,
                isCritical = pendingCritical,
                sizeBytes = pendingSize,
                updateManager = container.updateManager,
                onDismiss = {
                    showUpdateDialog = false
                    pendingUpdate = null
                },
            )
        }
    }
}

