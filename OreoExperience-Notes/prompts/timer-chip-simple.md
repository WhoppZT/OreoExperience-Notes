# Cronómetro "Chip flotante" — diseño simple, sin trucos

Necesito reemplazar el cronómetro actual de "OreoExperience Notes"
por un diseño **muy simple**: un chip horizontal con el tiempo y
el objetivo, sin barra de progreso, sin gradientes, sin Canvas.
El feedback de progreso es **solo el color del border** del chip,
que cambia según el ratio.

Trabajá paso por paso. NO inventes elementos que no estén
descritos. NO uses `Modifier.background()` apilados. NO uses
`Brush.horizontalGradient`. NO uses `LinearProgressIndicator`.
Solo lo que está explícitamente abajo.

Antes de tocar archivos mostrame el plan: archivos que vas a
modificar, y el composable propuesto en pseudocódigo. Esperá mi
OK antes de implementar.

## Contexto técnico

- Stack: Kotlin 2.0.20, Jetpack Compose, Material3, Compose BOM
  2024.09.02. Identidad Aurora con `OreoPalette` (Accent,
  AccentSub, Bg0, Bg1, SurfaceCard, SurfaceCardHi, OnSurface,
  OnSurfaceMuted, OnSurfaceFaint, Outline, OutlineFaint,
  DangerFill, WarnFill, OkFill).
- `LocalWindowSizeClass.current` ya disponible.
- Componente actual: `ui/components/OreoTimer.kt`. Tiene API
  `OreoTimer(targetSec, elapsedSec, running, onToggle, onReset,
  onClose, variant, modifier)` con dos variantes:
  `OreoTimerVariant.Editor` y `OreoTimerVariant.Discourse`.

## Diseño exacto

UN chip con esquinas full-pill (`RoundedCornerShape(50)`). Adentro,
en una `Row`:

- ícono play / pause (chico, 14 dp)
- spacer 6 dp
- tiempo transcurrido (mono, SemiBold)
- separador "·" (OnSurfaceFaint, padding 8 dp horizontal)
- objetivo (mono, OnSurfaceMuted)

Afuera del chip, separado por 6 dp, el botón ✕ (solo si
`variant = Editor` y `onClose != null`).

```
╭──────────────────────────────────╮
│  ⏵  00:42  ·  5 min              │   ✕
╰──────────────────────────────────╯
       ↑
   border 1.5dp con color que cambia según el ratio
```

### Tamaños

```kotlin
val chipHeight = when (variant) {
    Editor    -> if (windowSizeClass.widthSizeClass == Compact) 36.dp else 40.dp
    Discourse -> if (windowSizeClass.widthSizeClass == Compact) 56.dp else 64.dp
}

val timeFontSize = when (variant) {
    Editor    -> 14.sp
    Discourse -> if (compact) 22.sp else 26.sp
}

val targetFontSize = when (variant) {
    Editor    -> 12.sp
    Discourse -> if (compact) 14.sp else 16.sp
}

val iconSize = when (variant) {
    Editor    -> 14.dp
    Discourse -> if (compact) 18.dp else 22.dp
}
```

### Color del border (animado)

```kotlin
val ratio = (elapsedSec.toFloat() / targetSec.toFloat()).coerceIn(0f, 1f)
val isOver = elapsedSec >= targetSec

val borderColor by animateColorAsState(
    targetValue = when {
        isOver        -> OreoPalette.DangerFill
        ratio >= 0.85f -> OreoPalette.WarnFill
        ratio > 0f    -> OreoPalette.Accent
        else          -> OreoPalette.Outline
    },
    animationSpec = tween(600),
    label = "borderColor",
)
```

Border de 1.5 dp en `Editor`, 2 dp en `Discourse`.

### Estado "sobreexcedido"

Cuando `isOver`:

- El tiempo se prefija con "+" y se muestra en `DangerFill`.
  Ej: "+01:14".
- El border, ya en `DangerFill`, **pulsa la opacidad** entre
  0.7 y 1.0 cada 1300 ms (`infiniteRepeatable` con
  `RepeatMode.Reverse`, easing `EaseInOut`). NO escala, NO mueve
  nada, solo cambia opacidad del border. Para esto, animá el
  alpha del color del border y aplicá `borderColor.copy(alpha =
  pulseAlpha)`.

### Layout completo del chip (estructura exacta)

```kotlin
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OreoTimer(
    targetSec: Int,
    elapsedSec: Int,
    running: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    variant: OreoTimerVariant,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val windowSizeClass = LocalWindowSizeClass.current
    val isCompact = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact

    // ... tamaños según la tabla de arriba (chipHeight, timeFontSize, etc.)

    val ratio = (elapsedSec.toFloat() / targetSec.toFloat()).coerceIn(0f, 1f)
    val isOver = elapsedSec >= targetSec

    // Pulso de opacidad cuando isOver
    val pulseAlpha by if (isOver) {
        rememberInfiniteTransition(label = "overPulse").animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1300, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "overAlpha",
        )
    } else {
        remember { mutableStateOf(1f) }
    }

    val targetBorder = when {
        isOver         -> OreoPalette.DangerFill
        ratio >= 0.85f -> OreoPalette.WarnFill
        ratio > 0f     -> OreoPalette.Accent
        else           -> OreoPalette.Outline
    }
    val borderColor by animateColorAsState(targetBorder, tween(600), label = "border")

    // El maxWidth depende de la variante y del breakpoint
    val maxWidth = when {
        variant == OreoTimerVariant.Editor && !isCompact    -> 720.dp
        variant == OreoTimerVariant.Discourse && !isCompact -> 880.dp
        else                                                -> Dp.Infinity
    }

    // Box exterior centra cuando hay maxWidth
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth(),  // crítico para que el chip pueda
                                  // expandirse hasta el max
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // El chip
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(chipHeight)
                    .clip(RoundedCornerShape(50))
                    .background(OreoPalette.SurfaceCard)
                    .border(
                        width = if (variant == OreoTimerVariant.Editor) 1.5.dp else 2.dp,
                        color = borderColor.copy(alpha = pulseAlpha),
                        shape = RoundedCornerShape(50),
                    )
                    .combinedClickable(
                        onClick = onToggle,
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onReset()
                        },
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Icono play / pause
                Icon(
                    imageVector = if (running) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                    contentDescription = if (running) "Pausar" else "Iniciar",
                    tint = OreoPalette.Accent,
                    modifier = Modifier.size(iconSize),
                )
                Spacer(Modifier.width(6.dp))
                // Tiempo transcurrido
                Text(
                    text = if (isOver) "+${formatHms(elapsedSec - targetSec)}"
                           else formatHms(elapsedSec),
                    color = if (isOver) OreoPalette.DangerFill
                            else OreoPalette.OnSurface,
                    fontSize = timeFontSize,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.width(8.dp))
                // Separador "·"
                Text(
                    text = "·",
                    color = OreoPalette.OnSurfaceFaint,
                    fontSize = timeFontSize,
                )
                Spacer(Modifier.width(8.dp))
                // Objetivo
                Text(
                    text = formatTargetCompact(targetSec),
                    color = OreoPalette.OnSurfaceMuted,
                    fontSize = targetFontSize,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // Botón ✕ (solo en Editor)
            if (variant == OreoTimerVariant.Editor) {
                Spacer(Modifier.width(6.dp))
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Quitar cronómetro",
                        tint = OreoPalette.OnSurfaceMuted,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
```

## Detalles importantes (NO los podés omitir)

1. **`combinedClickable` requiere `@OptIn(ExperimentalFoundationApi::class)`**.
   Agregalo al archivo o al composable.

2. **`weight(1f)` en el chip funciona si el `Row` padre tiene
   `fillMaxWidth()`**. Por eso la cadena
   `widthIn(max = maxWidth).fillMaxWidth()` en el Row exterior es
   crítica. Sin la segunda, el `weight(1f)` colapsa a 0 dp.

3. **`combinedClickable` se aplica al chip ANTES del `padding`**.
   Si va después, el área tappable se reduce.

4. **`clip` antes de `background` y `border`**. Sino el border se
   ve cortado en las esquinas.

5. **NO uses `Modifier.background()` con un `Brush`**. NO uses
   gradientes. NO uses `LinearProgressIndicator`. NO uses Canvas.
   El feedback visual del progreso es ÚNICAMENTE el color del
   border que cambia con `animateColorAsState`.

6. **NO agregues elementos que no estén descritos**. Sin barra de
   progreso, sin línea, sin chip de porcentaje, sin "Quedan X",
   sin "Objetivo X" como label. Solo lo que muestra el mock.

7. **Separador "·"** entre tiempo y objetivo: es un Text simple
   con el caracter "·" de color `OnSurfaceFaint`.

8. **`formatHms(elapsedSec)`** y **`formatTargetCompact(targetSec)`**
   ya existen en el componente actual. Reusalas.

9. **Tipografía monoespaciada** para tiempo y objetivo
   (`FontFamily.Monospace`), para que los dígitos no se muevan
   horizontalmente al cambiar.

## Variantes — diferencias

| Param           | Editor         | Discourse      |
|-----------------|----------------|----------------|
| Altura          | 36 / 40 dp     | 56 / 64 dp     |
| Tiempo fontSize | 14 sp          | 22 / 26 sp     |
| Objetivo fontSize | 12 sp        | 14 / 16 sp     |
| Icono           | 14 dp          | 18 / 22 dp     |
| Border          | 1.5 dp         | 2 dp           |
| MaxWidth medium/expanded | 720 dp | 880 dp         |
| Botón ✕         | sí             | NO             |

## Posicionamiento del componente

- En `EditorScreen.kt`: el componente vive donde estaba el
  `OreoTimer` actual (al pie del editor, encima de la
  `FormatToolbar`). Mismo padding y posición.
- En `ReaderScreen.kt`: dentro del `Box` raíz, alineado arriba con
  `Modifier.align(Alignment.TopCenter).padding(top = 16.dp,
  start = 24.dp, end = 24.dp)`. NO tiene botón ✕.

## Mocks textuales

### Editor / Compact (412 dp portrait)

```
... contenido de la nota ...
─────────────────────────────────────────────
╭───────────────────────────────────────╮  ✕     ← chip 36dp
│  ⏵  00:42  ·  5 min                   │
╰───────────────────────────────────────╯
─────────────────────────────────────────────
[📷] [🎬] [☑] │ B I U S │ T <> A ≡ ✏ 🔗 ✨        ← FormatToolbar
```

### Editor / Medium (915 dp landscape)

```
... contenido ...
─────────────────────────────────────────────
       ╭──────────────────────────────╮  ✕         ← chip 40dp,
       │  ⏵  00:42  ·  5 min          │             centrado
       ╰──────────────────────────────╯             maxWidth 720dp
─────────────────────────────────────────────
       [📷] [🎬] [☑] │ ...
```

### Reader / Compact (412 dp portrait)

```
            ╭───────────────────────────╮          ← chip 56dp,
            │  ⏵  00:42  ·  5 min       │            top center
            ╰───────────────────────────╯            sin ✕

  Título de la nota
  ─────────────────
  Lorem ipsum...
```

### Reader / Expanded (1280 dp landscape)

```
                      ╭────────────────────────────────╮      ← chip 64dp
                      │  ⏵  00:42  ·  5 min            │        maxWidth 880dp
                      ╰────────────────────────────────╯        sin ✕

                  Título de la nota
                  ─────────────────
                  Lorem ipsum...
```

## Tests mentales

| Contexto            | Width   | Resultado esperado                                          |
|---------------------|---------|-------------------------------------------------------------|
| Editor portrait     | 412 dp  | Chip 36dp completo + ✕ a la derecha                        |
| Editor landscape    | 915 dp  | Chip centrado maxWidth 720dp + ✕                           |
| Editor a11y 1.5x    | 360 dp  | Tipografía más grande, padding interno mantiene 14dp       |
| Reader phone        | 412 dp  | Chip 56dp top center, sin ✕                                |
| Reader tablet       | 1280 dp | Chip 64dp centrado maxWidth 880dp, sin ✕                  |
| Cualquiera 100%+    | -       | Tiempo "+01:14" rojo, border rojo pulsando opacidad        |

NUNCA debe verse:

- Barra de progreso lineal o curva.
- Gradiente de cualquier tipo.
- Card adicional debajo o arriba del chip.
- Línea de "progreso" entre el chip y la `FormatToolbar`.
- Más de UN chip en pantalla.
- Botón ✕ DENTRO del chip.

## Migración

Solo modificar `ui/components/OreoTimer.kt`. Reemplazar
COMPLETAMENTE el cuerpo del composable `OreoTimer` por la
estructura indicada arriba. **No** modifiques nada de los callers
en `EditorScreen.kt` ni `ReaderScreen.kt` — la API no cambia.

Si el archivo actual tiene código de gradientes, `Brush`,
`LinearProgressIndicator`, Canvas, `drawBehind`, `Modifier.background()`
con brush, BORRALO. Solo queda lo descrito arriba.

## Estilo de trabajo

- Comentarios en español, "por qué" no "qué".
- Solo `OreoPalette` para colores.
- No agregues dependencias.
- No toques otros features.

## Entregable

1. Plan: confirmá qué archivo vas a modificar y qué vas a borrar.
2. Pegá el código completo del nuevo `OreoTimer` (no más de 100
   líneas).
3. Confirmá: "no hay backgrounds apilados, no hay gradientes, no
   hay LinearProgressIndicator, no hay Canvas".
