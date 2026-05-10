package com.oreoexperience.notes.ui.auth

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oreoexperience.notes.BuildConfig
import com.oreoexperience.notes.ui.theme.OreoPalette

private val AccessEase = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

@Composable
fun AccessScreen(onUnlocked: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var entered by remember { mutableStateOf(false) }

    val cardAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(durationMillis = 280, easing = AccessEase),
        label = "accessAlpha",
    )
    val cardScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.96f,
        animationSpec = tween(durationMillis = 360, easing = AccessEase),
        label = "accessScale",
    )

    fun tryUnlock() {
        val cleanEmail = email.trim()
        val cleanCode = code.trim()
        if (!cleanEmail.contains("@") || cleanEmail.length < 5) {
            error = "Ingresá el correo del cliente."
            return
        }
        if (cleanCode != BuildConfig.ACCESS_CODE) {
            error = "Clave de acceso inválida."
            return
        }
        onUnlocked(cleanEmail)
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
            .padding(20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .scale(cardScale)
                .alpha(cardAlpha)
                .background(
                    color = OreoPalette.SurfaceCard,
                    shape = RoundedCornerShape(28.dp),
                )
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "OreoExperience",
                style = LocalTextStyle.current.copy(
                    brush = Brush.linearGradient(
                        listOf(OreoPalette.AccentSub, OreoPalette.Accent),
                    ),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = "Acceso privado",
                color = OreoPalette.OnSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ingresá las credenciales de cliente para desbloquear la aplicación.",
                color = OreoPalette.OnSurfaceMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            AccessField(
                value = email,
                onValueChange = {
                    email = it
                    error = null
                },
                label = "Correo del cliente",
                leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
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
                label = "Clave de acceso",
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
            Button(
                onClick = { tryUnlock() },
                enabled = email.isNotBlank() && code.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = OreoPalette.Accent,
                    contentColor = Color.White,
                    disabledContainerColor = OreoPalette.SurfaceCardHi,
                    disabledContentColor = OreoPalette.OnSurfaceFaint,
                ),
            ) {
                Text("Desbloquear", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = "La clave se puede cambiar al compilar con -PoreoAccessCode=TU_CLAVE.",
                color = OreoPalette.OnSurfaceFaint,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
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
