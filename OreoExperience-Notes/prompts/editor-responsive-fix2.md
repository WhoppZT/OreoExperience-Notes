# Fix DEFINITIVO: cronómetro sin info + toolbar desalineada

Después de los intentos previos, sigue habiendo dos problemas en
landscape (ver captura adjunta):

1. **`BottomTimerBar`**: solo se ven el anillo y el botón ✕. La
   info textual del centro ("Objetivo X min · Y%", "Quedan HH:MM")
   NO aparece.

2. **`FormatToolbar`** (barra inferior con B/I/U/etc): los íconos
   quedaron alineados a la izquierda con un montón de espacio
   vacío a la derecha. Antes estaban centrados.

Esta vez te paso el código EXACTO que tiene que tener cada uno.
NO modifiques la lógica, solo aplicá la estructura indicada.
Después mostrame las 10-20 líneas relevantes para que verifique.

## Paso previo

PRIMERO leé los dos archivos completos:

- `ui/components/BottomTimerBar.kt`
- `ui/editor/EditorScreen.kt` (buscá el composable
  `private fun FormatToolbar(...)`)

Después aplicá los cambios de abajo.

## Fix 1 — `BottomTimerBar`

El componente DEBE tener esta estructura. Los nombres de variables
internas, animaciones y subcomposables que ya existen (TimerRing,
remainingOrElapsedText, formatHms, etc.) se mantienen como están.
Lo único que cambia es el LAYOUT EXTERIOR.

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BottomTimerBar(
    targetSec: Int,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (targetSec <= 0) return

    // ... toda la lógica de estado existente (elapsed, running,
    //     showElapsedHint, animaciones, ringColor, etc.) intacta.

    val windowSizeClass = LocalWindowSizeClass.current
    val maxCardWidth = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> Dp.Infinity
        WindowWidthSizeClass.Medium  -> 720.dp
        else                          -> 820.dp
    }

    // Box exterior: ocupa TODO el ancho de la pantalla y centra
    // horizontalmente la card del cronómetro.
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // Card interna: tope de ancho según breakpoint, pero
        // fillMaxWidth para que la Column con weight(1f) tenga
        // espacio. SIN fillMaxWidth, el weight colapsa a 0dp y
        // la info se vuelve invisible.
        Row(
            modifier = Modifier
                .widthIn(max = maxCardWidth)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .background(
                    color = OreoPalette.SurfaceCard,
                    shape = RoundedCornerShape(22.dp),
                )
                .border(
                    width = 1.dp,
                    color = if (isOver) ringColor.copy(alpha = pulseAlpha)
                            else OreoPalette.Outline,
                    shape = RoundedCornerShape(22.dp),
                )
                .padding(start = 12.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // ----- Anillo grande con tiempo adentro -----
            TimerRing(
                running = running,
                isOver = isOver,
                elapsed = elapsed,
                targetSec = targetSec,
                ringColor = ringColor,
                animatedRatio = animatedRatio,
                scale = if (isOver && running) pulseScale else 1f,
                onTap = { running = !running },
                onLongPress = {
                    elapsed = 0
                    running = false
                },
            )

            Spacer(Modifier.width(14.dp))

            // ----- Texto de info a la derecha (CON weight(1f)) -----
            Column(
                modifier = Modifier
                    .weight(1f)        // ← esto es CRÍTICO
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showElapsedHint = !showElapsedHint },
                    ),
                verticalArrangement = Arrangement.Center,
            ) {
                // Línea 1: "Objetivo X min" + chip de porcentaje
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Objetivo ${formatTargetCompact(targetSec)}",
                        color = OreoPalette.OnSurface,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                color = ringColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${(ratio * 100).toInt()}%",
                            color = ringColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                // Línea 2: Quedan / Transcurrido / Excedido
                Text(
                    text = remainingOrElapsedText(
                        isOver = isOver,
                        elapsed = elapsed,
                        targetSec = targetSec,
                        showElapsed = showElapsedHint,
                    ),
                    color = if (isOver) OreoPalette.DangerFill
                            else OreoPalette.OnSurfaceMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // ----- Botón cerrar -----
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(34.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Eliminar cronómetro",
                    tint = OreoPalette.OnSurfaceMuted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
```

**Detalles críticos** (no los podés omitir):

- El `Row` interno tiene `widthIn(max = maxCardWidth).fillMaxWidth()`
  juntos. Las dos líneas seguidas. Sin la segunda, el `weight(1f)`
  de la Column colapsa.
- La `Column` con la info usa `Modifier.weight(1f)`. NO se puede
  reemplazar por `fillMaxWidth()` ni por nada más.
- El `Spacer(Modifier.width(14.dp))` entre el anillo y la Column es
  fijo, NO un weight.

## Fix 2 — `FormatToolbar`

Buscá el composable `private fun FormatToolbar(...)` en
`EditorScreen.kt`. Tiene que quedar con esta estructura:

```kotlin
@Composable
private fun FormatToolbar(
    activeState: RichTextState?,
    onPickImage: () -> Unit,
    onPickVideo: () -> Unit,
    onInsertChecklist: () -> Unit,
    // ... el resto de parámetros que ya tenga
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val maxToolbarWidth = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact -> Dp.Infinity
        WindowWidthSizeClass.Medium  -> 720.dp
        else                          -> 820.dp
    }

    // Surface exterior con fondo Bg0 que cubre todo el ancho.
    Surface(
        color = OreoPalette.Bg0,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .border(
                width = 0.5.dp,
                color = OreoPalette.OutlineFaint,
                shape = androidx.compose.foundation.shape.RectangleShape,
            ),
    ) {
        // Box que centra el contenido cuando hay max-width.
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,   // ← centra
        ) {
            // Row interno con tope de ancho. Si los items no entran
            // (fontScale alto), permitimos scroll horizontal.
            Row(
                modifier = Modifier
                    .widthIn(max = maxToolbarWidth)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(
                    space = 0.dp,
                    alignment = Alignment.CenterHorizontally,   // ← centra los íconos
                ),
            ) {
                // ----- TODOS los ToolbarButton y VerticalDivider que ya existían -----
                // No cambiar el orden, no cambiar los íconos, no agregar/sacar nada.
                // Solo asegurate de que estén dentro de este Row.

                ToolbarButton(icon = Icons.Outlined.Image, ...)
                ToolbarButton(icon = Icons.Outlined.Videocam, ...)
                ToolbarButton(icon = Icons.Outlined.CheckBox, ...)
                VerticalDivider()
                ToolbarButton(icon = Icons.Outlined.FormatBold, ...)
                ToolbarButton(icon = Icons.Outlined.FormatItalic, ...)
                ToolbarButton(icon = Icons.Outlined.FormatUnderlined, ...)
                ToolbarButton(icon = Icons.Outlined.FormatStrikethrough, ...)
                VerticalDivider()
                // HeadingDropdownButton, CodeButton, etc. — todo lo que tenía.
                // ...
            }
        }
    }
}
```

**Detalles críticos**:

- El `Box` exterior con `contentAlignment = Alignment.Center`. Sin
  esto, el Row queda alineado a la izquierda.
- El `Row` interno con `widthIn(max = maxToolbarWidth)` Y
  `horizontalScroll(rememberScrollState())`. El scroll horizontal
  es por si en `fontScale = 1.5x` los íconos no entran en el
  máximo.
- `Arrangement.spacedBy(0.dp, alignment = Alignment.CenterHorizontally)`
  centra los íconos cuando sobra espacio dentro del Row. Si tu
  versión de Compose no soporta el parámetro `alignment` en
  `spacedBy`, usá `Arrangement.Center` solo.
- NO uses `LazyRow` en este caso. El `Row` con
  `horizontalScroll` es más simple y permite el centrado.

## Importante: si LocalWindowSizeClass no existe

El responsive previo debería haber agregado un
`CompositionLocal<WindowSizeClass>` en `MainActivity` y propagado.
Si no existe, créalo:

```kotlin
// En MainActivity.kt o un archivo de tema:
val LocalWindowSizeClass = compositionLocalOf<WindowSizeClass> {
    error("WindowSizeClass not provided")
}

// En setContent {} de MainActivity:
val windowSizeClass = calculateWindowSizeClass(this)
CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
    OreoExperienceTheme { ... }
}
```

Si ya existe con otro nombre, usalo. No dupliques.

## Tests mentales antes de cerrar

1. Phone portrait (412 dp):
   - Cronómetro: card cubre todo el ancho, info textual visible
     (anillo + "Objetivo X · Y%" + "Quedan HH:MM" + ✕).
   - Toolbar: scrollable horizontal, íconos a la izquierda como
     siempre.

2. Phone landscape (915 dp):
   - Cronómetro: card centrada con maxWidth 720dp, info textual
     **visible** (no debe estar vacío al lado del anillo).
   - Toolbar: card centrada con maxWidth 720dp, íconos centrados
     dentro de la card (no al borde izquierdo).

3. Tablet landscape (1280 dp):
   - Igual que landscape pero maxWidth 820dp.

## Entregable

Pegame las líneas relevantes (estructura del Row exterior con sus
modificadores) de los dos composables después del cambio. No me
des todo el archivo, solo la estructura externa para verificar que
están los tres elementos clave:

- `Box.fillMaxWidth().contentAlignment = Center`
- `Row.widthIn(max = X).fillMaxWidth()` (timer) o
  `Row.widthIn(max = X).horizontalScroll(...)` (toolbar)
- `weight(1f)` en la Column de info del timer
- `Arrangement.Center` o `spacedBy(..., CenterHorizontally)` en la
  toolbar

Si falta alguno de los tres, no funciona.
