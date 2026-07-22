@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.oreoexperience.notes.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.ui.LocalOreoWindowSizeClass
import com.oreoexperience.notes.ui.theme.OreoPalette
import com.oreoexperience.notes.ui.theme.OreoMotion

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages = onboardingPages()
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val windowSize = LocalOreoWindowSizeClass.current
    val isTablet = !windowSize.isCompact

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OreoPalette.Bg0),
    ) {
        // Fondo decorativo con gradiente sutil
        DecorativeBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                OnboardingPage(
                    data = pages[page],
                    isTablet = isTablet,
                )
            }

            // Indicadores
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(pages.size) { i ->
                    val active = i == pagerState.currentPage
                    val w by animateDpAsState(
                        targetValue = if (active) 32.dp else 10.dp,
                        animationSpec = OreoMotion.SpringPress(),
                        label = "indicatorW",
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(10.dp)
                            .width(w)
                            .clip(CircleShape)
                            .background(
                                brush = if (active) {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            OreoPalette.AccentDeep,
                                            OreoPalette.Accent,
                                        )
                                    )
                                } else {
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            OreoPalette.OnSurfaceFaint,
                                            OreoPalette.OnSurfaceFaint,
                                        )
                                    )
                                }
                            ),
                    )
                }
            }

            // Botón "Comenzar" solo en la última página
            val isLastPage = pagerState.currentPage == pages.lastIndex
            if (isLastPage) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Button(
                        onClick = onFinish,
                        modifier = Modifier
                            .widthIn(max = 320.dp)
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = OreoPalette.Accent,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(
                            text = "Comenzar",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(88.dp))
            }
        }
    }
}

@Composable
private fun DecorativeBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { rotationZ = rotation }
            .blur(120.dp)
            .alpha(0.15f)
    ) {
        // Gradiente violeta rotante en el centro
        Box(
            modifier = Modifier
                .size(400.dp)
                .align(Alignment.Center)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            OreoPalette.Accent,
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )
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
        title = "Tu lienzo, tu estilo",
        description = "Negrita, cursiva, listas, títulos, checklist — todo lo que necesitás para escribir como quieras. Y sí, se guarda solo.",
    ),
    OnboardingPageData(
        icon = Icons.Outlined.Image,
        title = "La nota cobra vida",
        description = "Mandale fotos y videos desde tu galería. Un toque para verlos grandes, presioná para borrar. Así de fácil.",
    ),
    OnboardingPageData(
        icon = Icons.Outlined.Timer,
        title = "No te pases de tiempo",
        description = "Ponete un plazo para ese discurso o lectura. La barra te avisa si te vas de largo — sin excusas.",
    ),
    OnboardingPageData(
        icon = Icons.Outlined.AutoFixHigh,
        title = "Tu escritor personal",
        description = "Gemini AI te ayuda a corregir, reescribir, traducir o expandir tu texto. Es como tener un editor, pero gratis.",
    ),
    OnboardingPageData(
        icon = Icons.Outlined.Category,
        title = "Todo en su lugar",
        description = "Separá tus notas en Discursos, Consideraciones y lo que quieras. Fijá las más importantes y buscalas al toque.",
    ),
    OnboardingPageData(
        icon = Icons.Outlined.PictureAsPdf,
        title = "Llevatelo contigo",
        description = "Exportá a PDF con diseño pro, compartilo por WhatsApp o guardá un respaldo de todo por las dudas.",
    ),
)

@Composable
private fun OnboardingPage(
    data: OnboardingPageData,
    isTablet: Boolean,
) {
    var entered by remember(data) { mutableStateOf(false) }
    val heroScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.6f,
        animationSpec = OreoMotion.SpringHero(),
        label = "heroScale",
    )
    val heroAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = OreoMotion.EaseOut),
        label = "heroAlpha",
    )
    val textAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 600, delayMillis = 150, easing = OreoMotion.EaseOut),
        label = "textAlpha",
    )
    val textOffset by animateFloatAsState(
        targetValue = if (entered) 0f else 30f,
        animationSpec = tween(durationMillis = 600, delayMillis = 150, easing = OreoMotion.EaseOut),
        label = "textOffset",
    )
    LaunchedEffect(data) { entered = true }

    if (isTablet) {
        TabletPage(
            data = data,
            heroScale = heroScale,
            heroAlpha = heroAlpha,
            textAlpha = textAlpha,
            textOffset = textOffset,
        )
    } else {
        PhonePage(
            data = data,
            heroScale = heroScale,
            heroAlpha = heroAlpha,
            textAlpha = textAlpha,
            textOffset = textOffset,
        )
    }
}

@Composable
private fun PhonePage(
    data: OnboardingPageData,
    heroScale: Float,
    heroAlpha: Float,
    textAlpha: Float,
    textOffset: Float,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Icono con glow
            HeroIcon(
                icon = data.icon,
                scale = heroScale,
                alpha = heroAlpha,
            )

            Spacer(Modifier.height(56.dp))

            // Título con slide up
            Text(
                text = data.title,
                color = OreoPalette.OnSurface,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha)
                    .graphicsLayer { translationY = textOffset },
            )

            Spacer(Modifier.height(18.dp))

            // Descripción con slide up
            Text(
                text = data.description,
                color = OreoPalette.OnSurfaceMuted,
                fontSize = 16.sp,
                lineHeight = 25.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .alpha(textAlpha)
                    .graphicsLayer { translationY = textOffset },
            )
        }
    }
}

@Composable
private fun TabletPage(
    data: OnboardingPageData,
    heroScale: Float,
    heroAlpha: Float,
    textAlpha: Float,
    textOffset: Float,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .widthIn(max = 800.dp)
                .padding(horizontal = 64.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icono
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                HeroIcon(
                    icon = data.icon,
                    scale = heroScale,
                    alpha = heroAlpha,
                    iconSize = 72.dp,
                    glowSize = 280.dp,
                    iconBoxSize = 150.dp,
                )
            }

            Spacer(modifier = Modifier.width(64.dp))

            // Texto
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .alpha(textAlpha)
                    .graphicsLayer { translationY = textOffset },
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = data.title,
                    color = OreoPalette.OnSurface,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = data.description,
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = 19.sp,
                    lineHeight = 30.sp,
                )
            }
        }
    }
}

@Composable
private fun HeroIcon(
    icon: ImageVector,
    scale: Float,
    alpha: Float,
    iconSize: androidx.compose.ui.unit.Dp = 56.dp,
    glowSize: androidx.compose.ui.unit.Dp = 260.dp,
    iconBoxSize: androidx.compose.ui.unit.Dp = 120.dp,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(contentAlignment = Alignment.Center) {
        // Glow exterior difuso
        Box(
            modifier = Modifier
                .size(glowSize)
                .scale(scale * pulse)
                .alpha(alpha * 0.4f)
                .blur(40.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            OreoPalette.Accent,
                            OreoPalette.Accent.copy(alpha = 0.3f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        // Glow interior
        Box(
            modifier = Modifier
                .size(glowSize * 0.7f)
                .scale(scale)
                .alpha(alpha * 0.6f)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            OreoPalette.AccentSub.copy(alpha = 0.5f),
                            Color.Transparent,
                        ),
                    ),
                    shape = CircleShape,
                ),
        )

        // Icono con gradiente
        Box(
            modifier = Modifier
                .size(iconBoxSize)
                .scale(scale)
                .alpha(alpha)
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
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
