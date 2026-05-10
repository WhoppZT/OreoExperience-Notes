@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.oreoexperience.notes.ui.onboarding

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.ui.theme.OreoPalette
import com.oreoexperience.notes.ui.theme.OreoMotion
import kotlinx.coroutines.launch

/**
 * Tutorial inicial de tres pasos que se muestra una sola vez en
 * la primera apertura de la app. El estado "completado" se persiste
 * vía [com.oreoexperience.notes.data.UserPreferences] para no
 * volver a aparecer.
 *
 * Diseño:
 *   - Fondo negro puro con un halo violeta sutil detrás del ícono
 *     central de cada slide.
 *   - HorizontalPager para deslizar entre los pasos.
 *   - Botón "Saltar" en la parte superior derecha y "Comenzar" /
 *     "Siguiente" abajo.
 */
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = onboardingPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OreoPalette.Bg0),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Top bar con "Saltar" a la derecha.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onFinish) {
                    Text(
                        text = "Saltar",
                        color = OreoPalette.OnSurfaceMuted,
                        fontSize = 15.sp,
                    )
                }
            }

            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) { page ->
                OnboardingPage(pages[page])
            }

            // Indicadores
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { i ->
                    val active = i == pagerState.currentPage
                    // Indicador estilo iOS: la "píldora" activa se
                    // expande con un spring bouncy. Las inactivas se
                    // mantienen circulares.
                    val w by animateDpAsState(
                        targetValue = if (active) 24.dp else 6.dp,
                        animationSpec = OreoMotion.SpringBouncy(),
                        label = "indicatorW",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(6.dp)
                            .width(w)
                            .background(
                                color = if (active) OreoPalette.Accent
                                else OreoPalette.OnSurfaceFaint,
                                shape = CircleShape,
                            ),
                    )
                }
            }

            // Botón principal
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = {
                        if (pagerState.currentPage < pages.lastIndex) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OreoPalette.Accent,
                        contentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(18.dp),
                ) {
                    Text(
                        text = if (pagerState.currentPage == pages.lastIndex) "Comenzar"
                        else "Siguiente",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private data class OnboardingPageData(
    val icon: ImageVector,
    val title: String,
    val description: String,
)

@Composable
private fun onboardingPages(): List<OnboardingPageData> = listOf(
    OnboardingPageData(
        icon = Icons.Outlined.Edit,
        title = "Tu espacio para escribir",
        description =
            "Toca el botón violeta abajo a la derecha en el inicio para crear " +
                "una nota nueva. Tu título arriba, tu texto abajo — sin distracciones.",
    ),
    OnboardingPageData(
        icon = Icons.Outlined.Image,
        title = "Imágenes y videos",
        description =
            "Desde la barra inferior podés insertar fotos y videos en " +
                "el lugar exacto donde tengas el cursor. Mantené presionado " +
                "para borrarlos.",
    ),
    OnboardingPageData(
        icon = Icons.Outlined.Timer,
        title = "Cronómetro integrado",
        description =
            "Establecé una duración objetivo desde el menú \"⋯\" y aparecerá " +
                "una barra de cronómetro fija abajo, ideal para discursos o " +
                "lecturas con tiempo límite.",
    ),
)

@Composable
private fun OnboardingPage(data: OnboardingPageData) {
    // Hero del slide: el ícono entra con un spring bouncy + fade,
    // dando ese "pop" tipo iOS al deslizar entre páginas.
    var entered by remember(data) { mutableStateOf(false) }
    val heroScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.78f,
        animationSpec = OreoMotion.SpringHero(),
        label = "heroScale",
    )
    val heroAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 360,
            easing = OreoMotion.EaseOut,
        ),
        label = "heroAlpha",
    )
    LaunchedEffect(data) { entered = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Halo violeta + ícono central.
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .scale(heroScale)
                    .alpha(heroAlpha)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                OreoPalette.Accent.copy(alpha = 0.32f),
                                OreoPalette.Accent.copy(alpha = 0f),
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .scale(heroScale)
                    .alpha(heroAlpha)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                OreoPalette.AccentDeep,
                                OreoPalette.Accent,
                                OreoPalette.AccentSub,
                            ),
                        ),
                        shape = RoundedCornerShape(32.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp),
                )
            }
        }
        Spacer(Modifier.height(40.dp))
        Text(
            text = data.title,
            color = OreoPalette.OnSurface,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = data.description,
            color = OreoPalette.OnSurfaceMuted,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
    }
}
