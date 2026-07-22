package com.oreoexperience.notes.ui.auth

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.data.LicenseManager
import com.oreoexperience.notes.ui.LocalAppContainer
import com.oreoexperience.notes.ui.theme.OreoMotion
import com.oreoexperience.notes.ui.theme.OreoPalette

/**
 * Pantalla de credenciales — versión simplificada:
 *
 *   - Sólo dos campos: **Usuario** y **Clave**.
 *   - Sin textos auxiliares por encima/debajo (estilo limpio).
 *   - Valida la clave contra [LicenseManager]; el usuario es libre
 *     (queda registrado como nombre del cliente, sin restricciones de
 *     formato).
 */
@Composable
fun AccessScreen(onUnlocked: (user: String, key: String) -> Unit) {
    val container = LocalAppContainer.current
    val license = container.licenseManager

    var user by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf(false) }

    // Entrada del card: alpha controlada por tween (predecible) y
    // scale por spring bouncy (rebote sutil al asentarse).
    val cardAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 320, easing = OreoMotion.EaseOut),
        label = "accessAlpha",
    )
    val cardScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.92f,
        animationSpec = OreoMotion.SpringPress(),
        label = "accessScale",
    )

    fun tryUnlock() {
        val cleanUser = user.trim()
        val cleanCode = code.trim()
        if (cleanUser.isEmpty()) {
            error = "Ingresá tu usuario."
            return
        }
        if (cleanCode.isEmpty()) {
            error = "Ingresá la clave."
            return
        }
        when (val result = license.tryUnlock(cleanCode)) {
            is LicenseManager.UnlockResult.InvalidKey -> {
                error = "Clave inválida."
            }
            is LicenseManager.UnlockResult.LicenseExpired -> {
                error = "Tu licencia venció. Pedí una nueva clave."
            }
            is LicenseManager.UnlockResult.FirstActivation,
            is LicenseManager.UnlockResult.Rotated,
            is LicenseManager.UnlockResult.Unlocked -> {
                onUnlocked(cleanUser, cleanCode)
            }
        }
    }

    LaunchedEffect(Unit) {
        entered = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OreoPalette.Bg0)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .scale(cardScale)
                .alpha(cardAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Título plano centrado — sin halo, sin gradient sweep,
            // sin card de fondo. La página entera respira el bg base.
            Text(
                text = "OreoExperience",
                color = OreoPalette.OnSurface,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Acceso",
                color = OreoPalette.OnSurfaceFaint,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.height(28.dp))
            AccessField(
                value = user,
                onValueChange = {
                    user = it
                    error = null
                },
                label = "Usuario",
                leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                ),
            )
            Spacer(Modifier.height(12.dp))
            AccessField(
                value = code,
                onValueChange = {
                    code = it
                    error = null
                },
                label = "Clave",
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { tryUnlock() }),
            )
            if (error != null) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = error.orEmpty(),
                    color = OreoPalette.DangerFill,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(18.dp))
            // Botón con press feedback bouncy estilo iOS.
            val unlockInteraction = remember { MutableInteractionSource() }
            val unlockPressed by unlockInteraction.collectIsPressedAsState()
            val unlockScale by animateFloatAsState(
                targetValue = if (unlockPressed) 0.96f else 1f,
                animationSpec = OreoMotion.SpringPress(),
                label = "unlockPressScale",
            )
            Button(
                onClick = { tryUnlock() },
                enabled = user.isNotBlank() && code.isNotBlank(),
                interactionSource = unlockInteraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .scale(unlockScale),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OreoPalette.Accent,
                    contentColor = Color.White,
                    disabledContainerColor = OreoPalette.SurfaceCardHi,
                    disabledContentColor = OreoPalette.OnSurfaceFaint,
                ),
            ) {
                Text("Desbloquear", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun AccessField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    visualTransformation: androidx.compose.ui.text.input.VisualTransformation =
        androidx.compose.ui.text.input.VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        label = { Text(label) },
        leadingIcon = leadingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = OreoPalette.OnSurface,
            unfocusedTextColor = OreoPalette.OnSurface,
            focusedLabelColor = OreoPalette.AccentSub,
            unfocusedLabelColor = OreoPalette.OnSurfaceMuted,
            cursorColor = OreoPalette.Accent,
            focusedBorderColor = OreoPalette.Accent,
            unfocusedBorderColor = OreoPalette.Outline,
            focusedLeadingIconColor = OreoPalette.AccentSub,
            unfocusedLeadingIconColor = OreoPalette.OnSurfaceMuted,
        ),
    )
}
