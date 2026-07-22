# Cronómetro "Liquid Glass" — progreso como fondo gradiente

Necesito refinar el cronómetro `OreoTimer` actual de "OreoExperience
Notes". La estructura horizontal y la API se mantienen, pero el
look queda **más cohesivo y premium**: en vez de una línea fina
de progreso separada, el progreso ES el fondo de la pastilla, en
forma de gradiente horizontal estilo Apple "Liquid Glass" / mini-
player de Spotify.

Antes de tocar archivos mostrame el plan: qué archivos vas a
modificar, mocks textuales en cada contexto, y qué decisiones
tomaste sobre los gradientes y el contraste en light mode. Esperá
mi OK antes de implementar.

## Contexto

- Stack: Kotlin 2.0.20, Jetpack Compose, Material3, Compose BOM
  2024.09.02. Identidad Aurora con `OreoPalette`.
- Componente actual: `ui/components/OreoTimer.kt` con dos variantes
  (`OreoTimerVariant.Editor` y `OreoTimerVariant.Discourse`). Ya
  existe y funciona; solo cambia el look interno.
- En el modo lectura el cronómetro NO debe mostrar el botón ✕ (lo
  decide el caller pasando `onClose = null` o vía la variante).

## Diseño "Liquid Glass"

Una sola **pastilla horizontal** que contiene todo el cronómetro.
La pastilla tiene:

1. **Track** (pista) — fondo plano de `OreoPalette.SurfaceCard` con
   esquinas `RoundedCornerShape(50)` (full pill).
2. **Fill** (llenado) — gradiente horizontal que va de
   `Accent.copy(alpha = 0.32f)` (izq) hasta transparente (der) en
   el punto del progreso. Esto se dibuja sobre el track, dentro de
   la misma pastilla, recortado al mismo shape.
3. **Contenido**: ícono play + tiempo a la izquierda, objetivo a la
   derecha. Todo sobre el fill. Sin línea de progreso separada.

Mock textual:

```
╭───────────────────────────────────────────────────────────╮
│▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│   ← gradiente fill
│  ⏵  00:42                                       5 min   │   ← contenido sobre el fill
╰───────────────────────────────────────────────────────────╯

(✕ afuera, a la derecha — solo en variante Editor)
```

### Cómo dibujar el gradiente

El gradiente se hace con `Brush.horizontalGradient` con `colorStops`
**dinámicos según el ratio**:

```kotlin
val ratio = (elapsedSec.toFloat() / targetSec.toFloat()).coerceIn(0f, 1f)
val fillColor = when {
    isOver        -> OreoPalette.DangerFill
    ratio >= 0.85f-> OreoPalette.WarnFill
    else          -> OreoPalette.Accent
}

// alpha más fuerte en dark, más fuerte aún en light para que se vea
val isDark = isSystemInDarkTheme()
val fillAlpha = if (isDark) 0.32f else 0.45f

// el "punto de corte" del gradiente sigue al ratio. Por encima del
// punto, transparente. Para que la transición sea suave, hacemos
// un fade de ~5% antes del corte.
val brush = Brush.horizontalGradient(
    colorStops = arrayOf(
        0f                          to fillColor.copy(alpha = fillAlpha),
        (ratio - 0.05f).coerceAtLeast(0f) to fillColor.copy(alpha = fillAlpha),
        ratio                        to fillColor.copy(alpha = 0f),
        1f                           to fillColor.copy(alpha = 0f),
    ),
)
```

Animar el `ratio` con `animateFloatAsState` (tween 600 ms easeInOut)
para que el gradiente fluya suavemente segundo a segundo.

### Estructura visual del componente

```kotlin
Box(
    modifier = Modifier
        .height(timerHeight)  // ej 40dp
        .fillMaxWidth()
        .clip(RoundedCornerShape(50))
        .background(OreoPalette.SurfaceCard)   // track
        .background(brush)                     // fill encima
        .border(1.dp, OreoPalette.OutlineFaint, RoundedCornerShape(50))
        .combinedClickable(
            onClick = onToggle,
            onLongClick = onReset,
        )
) {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(playPauseIcon, contentDescription = ..., tint = Accent, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(formatHms(elapsedSec), fontFamily = Monospace, ...)
        Spacer(Modifier.weight(1f))
        Text(formatTargetCompact(targetSec), color = OnSurfaceMuted, ...)
    }
}
```

Notar:

- Los **dos `.background()` apilados** dan el efecto: primero el
  color de track, después el gradiente encima. Compose los aplica
  en orden, así que el segundo va arriba.
- El `border` muy sutil (`OutlineFaint` 1 dp) ayuda a que la
  pastilla se "delimite" del contenido del editor de fondo. Sin
  border, en light mode se mezcla.
- El contenido (Row con Icon + Text) está SOBRE los backgrounds.
  No hay línea de progreso separada.

### Tap / long-press del componente

- **Tap en cualquier parte de la pastilla**: `onToggle()`
  (play / pause).
- **Long-press**: `onReset()` con haptic feedback.
- El `combinedClickable` se aplica al Box principal (toda la
  pastilla es interactiva).

### Estado "overflow" (excedido)

Cuando `elapsedSec >= targetSec`:

- Tiempo en el contenido: prefijo `+` y color `DangerFill`
  (ej "+01:14").
- Fill al 100%: el gradiente queda lleno hasta el final con
  `DangerFill.copy(alpha = fillAlpha)`. Sin transición transparente
  — el rojo cubre todo.
- Pulso muy sutil de la opacidad del fill: alterna entre 0.85x y
  1.0x del `fillAlpha` cada 1300 ms (`infiniteRepeatable`,
  RepeatMode.Reverse, easing easeInOut). NO escala, NO mueve nada
  — solo opacidad.

### Botón ✕ — solo variante Editor

El ✕ vive **fuera** de la pastilla, a la derecha, separado por un
spacer de 4 dp:

```kotlin
Row(verticalAlignment = CenterVertically) {
    OreoTimerPill(...)              // la pastilla con gradiente
    Spacer(Modifier.width(4.dp))
    if (variant == Editor) {
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(Outlined.Close, ..., size = 16.dp, tint = OnSurfaceMuted)
        }
    }
}
```

En `Discourse` el ✕ no se muestra — la pastilla ocupa todo el
ancho disponible.

## Variantes

### `Editor`

- Altura de la pastilla: 36 dp (Compact) / 40 dp (Medium / Expanded).
- Tipografía: tiempo 14 sp, objetivo 12 sp.
- Padding horizontal interno: 14 dp.
- Posición: pie del editor, encima de la `FormatToolbar`.
- Responsive:
  - Compact: pastilla ocupa todo el ancho disponible.
  - Medium / Expanded: `widthIn(max = 720.dp)` + `fillMaxWidth()`,
    centrado con `Box(contentAlignment = Center)`.
- Botón ✕ visible.

### `Discourse`

- Altura: 56 dp (Compact) / 64 dp (Medium / Expanded).
- Tipografía: tiempo 22 sp / 26 sp Bold (visible a 1 m), objetivo
  14 sp / 16 sp.
- Padding horizontal: 24 dp.
- Posición: en `ReaderScreen`, alineado arriba con
  `Modifier.align(TopCenter).padding(top = 16.dp)`.
- Responsive:
  - Compact: ancho completo con padding 24 dp.
  - Medium / Expanded: `widthIn(max = 880.dp)` + `fillMaxWidth()`
    centrado.
- Sin botón ✕.
- Si los controles del Reader están ocultos (auto-hide tras 4s),
  la pastilla baja a `alpha = 0.4f` (NO desaparece — el discurso
  necesita el tiempo siempre visible). Cualquier tap en pantalla
  la restaura a alpha 1f.

## Detalles importantes

1. **Light mode vs dark mode**: el `fillAlpha` debe ser más alto en
   light para que el gradiente se vea. Usar `isSystemInDarkTheme()`
   para decidir (0.32f dark / 0.45f light).

2. **Animación del gradiente**: animar el `ratio` con
   `animateFloatAsState`. NO recalcular el `Brush` en cada
   recomposición sin `remember(ratio)` — Compose lo hace bien con
   inputs animados, pero verificalo: el `Brush.horizontalGradient`
   debe aceptar el `ratio` animado como key.

3. **Cambio de color** (al cruzar 85% / 100%): usar
   `animateColorAsState` para el `fillColor`, tween 600 ms.

4. **Tipografía monoespaciada** (`FontFamily.Monospace`) para los
   números — los dígitos no deben moverse al cambiar.

5. **`combinedClickable`** requiere `@OptIn(ExperimentalFoundationApi::class)`.

6. **Haptic en long-press**:
   `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)`
   ANTES de `onReset()`.

7. **Acceso a fontScale**: en `fontScale = 1.5x`, los números
   crecen y la pastilla puede no tener espacio. Si el contenido
   excede el ancho, comprimir el padding interno a 10 dp en vez
   de 14 dp. NO cortar texto.

8. **NO uses `Modifier.blur`** en el gradiente — el blur de
   Compose no recorta al shape y se desborda visualmente. Si
   querés más "glow" en el límite del progreso, agregale un
   colorStop intermedio con alpha mayor (ej: en `ratio - 0.02f`
   con alpha `fillAlpha * 1.3f`) para crear un mini-pico visual.

9. **Track + fill apilados**: NO uses un solo background con
   gradiente que tiene SurfaceCard en el resto. Eso pierde el
   contraste de la pastilla con el fondo del editor. Track y fill
   son DOS backgrounds apilados en el mismo Box.

10. **Border 1 dp `OutlineFaint`**: necesario para delimitar la
    pastilla del fondo del editor, sobre todo en light mode.

## Tests mentales

| Contexto             | Width   | Tema  | Resultado esperado                                                         |
|----------------------|---------|-------|----------------------------------------------------------------------------|
| Editor portrait      | 412 dp  | Dark  | Pastilla 36dp completa, gradiente Accent al 22%, ✕ a la derecha            |
| Editor landscape     | 915 dp  | Dark  | Pastilla 40dp centrada maxWidth 720dp, ✕ visible                           |
| Editor light mode    | 412 dp  | Light | Mismo, pero fillAlpha 0.45f para que el gradiente se vea                   |
| Editor a11y          | 360 dp  | Dark  | Tipografía más grande, padding interno se comprime a 10dp                  |
| Reader phone         | 412 dp  | Dark  | Pastilla 56dp top center, sin ✕, gradiente más prominente                  |
| Reader tablet        | 1280 dp | Dark  | Pastilla 64dp centrada maxWidth 880dp, sin ✕                               |
| Reader controles ocultos| -    | -     | Pastilla a alpha 0.4f                                                      |
| Cualquiera 100%+     | -       | -     | Tiempo "+01:14" rojo, fill DangerFill al 100% pulsando opacidad sutilmente |

NUNCA debe verse:

- Halo violeta o glow desbordado del shape.
- Borde duro vertical en el punto del progreso (el gradiente con
  el fade de 5% lo evita).
- Gradiente invisible en light mode.
- Pastilla mezclándose con el fondo del editor (el border
  `OutlineFaint` la delimita).
- Botón ✕ adentro de la pastilla.
- Pastilla y ✕ desalineados verticalmente.

## Migración

Solo modificar `ui/components/OreoTimer.kt`. La API pública del
componente NO cambia — solo cambia el render interno. Los callers
en `EditorScreen.kt` y `ReaderScreen.kt` no se tocan.

Si en el `OreoTimer.kt` actual hay una `Row` con
`[chip-tiempo] [LinearProgressIndicator] [chip-objetivo] [✕]`,
reemplazarla por:

- Pastilla `Box` con dos backgrounds apilados (track + gradient).
- Adentro: `Row` con icon play / pause + tiempo + spacer weight(1f)
  + objetivo.
- Afuera (si Editor): IconButton ✕ con spacer 4 dp.

## Estilo de trabajo

- Comentarios en español, "por qué" no "qué".
- Solo `OreoPalette`.
- No agregues dependencias.
- No toques otros features.

## Entregable

1. Plan de archivos.
2. Mocks textuales en cada breakpoint y tema (dark / light).
3. Implementación.
4. Resumen "archivo X: cambié Y" al terminar.
