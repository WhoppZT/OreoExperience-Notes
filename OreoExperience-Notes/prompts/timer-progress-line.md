# Cronómetro "línea de progreso" — minimalista, único, responsive

Necesito reemplazar el cronómetro actual de "OreoExperience Notes" por
un diseño nuevo basado en una **fina línea de progreso horizontal**
con dos chips a los costados. Es **un solo componente** que se usa
tanto en el editor como en el modo lectura/discurso (sin duplicar
clases ni layouts).

El cronómetro actual (anillo + card pesada) tiene varios problemas:

- Se ven artefactos visuales (halo glow desbordando el círculo).
- En landscape queda info invisible o desbalanceada.
- Hay duplicación entre el del editor y el del modo lectura.
- Pesa visualmente y no combina con la estética minimalista que
  busco.

Antes de tocar archivos mostrame el plan: archivos que vas a
modificar / crear / borrar, mocks textuales en cada contexto, y
qué decisiones tomaste. Esperá mi OK antes de implementar.

## Contexto técnico

Stack:

- Kotlin 2.0.20, Jetpack Compose, Material3, Compose BOM 2024.09.02
- compose-rich-editor 1.0.0-rc11
- Identidad **Aurora**: acento `#7C3AED`, dark/light, esquinas
  redondeadas (12-24 dp).
- Paleta `OreoPalette` (Accent, AccentSub, AccentDeep, Bg0, Bg1,
  SurfaceCard, SurfaceCardHi, OnSurface, OnSurfaceMuted,
  OnSurfaceFaint, Outline, OutlineFaint, DangerFill, WarnFill,
  OkFill).
- `LocalWindowSizeClass.current.widthSizeClass` ya disponible.

Archivos a tocar:

- **Crear** `ui/components/OreoTimer.kt` (componente nuevo unificado).
- **Borrar** `ui/components/BottomTimerBar.kt` (componente viejo).
- **Borrar** cualquier composable de cronómetro duplicado en
  `ui/reader/ReaderScreen.kt` (TimerRing privado o similar).
- **Modificar** `ui/editor/EditorScreen.kt` para usar el nuevo.
- **Modificar** `ui/reader/ReaderScreen.kt` para usar el nuevo.

## Filosofía del diseño

UN componente. UN visual. Lo único que cambia entre contextos es la
**posición** y **tamaño** vía un parámetro de variante.

Sin anillo. Sin card pesada. Sin glow. El cronómetro es:

1. Una **fina línea horizontal de progreso** (3 dp de alto en
   editor, 4 dp en lectura).
2. A la izquierda: chip con el **tiempo transcurrido**.
3. A la derecha: chip con el **objetivo** + botón **✕**.
4. La línea se llena de izquierda a derecha proporcionalmente.
5. Color de la línea: Accent hasta 85%, WarnFill entre 85% y 100%,
   DangerFill al pasar de 100%.

## API del componente

```kotlin
sealed interface OreoTimerVariant {
    data object Editor : OreoTimerVariant      // pie del editor
    data object Discourse : OreoTimerVariant   // franja superior del reader
}

@Composable
fun OreoTimer(
    targetSec: Int,
    elapsedSec: Int,
    running: Boolean,
    onToggle: () -> Unit,            // play / pause
    onReset: () -> Unit,             // long-press = reset
    onClose: () -> Unit,             // tap en ✕ = quitar cronómetro
    variant: OreoTimerVariant,
    modifier: Modifier = Modifier,
)
```

El estado (elapsedSec, running) lo maneja el caller. El componente
es presentacional puro.

## Diseño detallado

### Layout común a las dos variantes

```
┌────────────────────────────────────────────────────────────┐
│  [⏵ 00:42]  ──────────●──────────────  [3 min]   [✕]      │
└────────────────────────────────────────────────────────────┘
       ↑              ↑                    ↑          ↑
    chip izq    barra de progreso       chip der    cerrar
```

**Chip izquierdo** (tiempo transcurrido):

- Pill compacto con padding 10 dp horizontal, 6 dp vertical.
- Fondo `OreoPalette.SurfaceCard`, esquinas `RoundedCornerShape(50)`
  (full pill).
- Contenido en `Row` con `verticalAlignment = CenterVertically`:
  - Icono play/pause 14 dp tinted en Accent. Cambia con
    `AnimatedContent` cross-fade 180ms entre `Icons.Outlined.PlayArrow`
    y `Icons.Outlined.Pause`.
  - Spacer 6 dp.
  - Tiempo transcurrido en MONO, OnSurface, SemiBold.
- **Tap en el chip = play / pause** (delega a `onToggle`).
- **Long-press en el chip = reset** (delega a `onReset`, con
  haptic feedback).
- Cuando `isOver`: tiempo en `DangerFill` y prefijo "+", ej "+01:14".

**Barra de progreso** (entre los dos chips):

- Pista (track): 3 dp / 4 dp altura según variante,
  `RoundedCornerShape(50)`, color `OreoPalette.OutlineFaint`.
- Llenado (fill): mismo alto, color animado:
  - 0-85%: Accent
  - 85-100%: WarnFill
  - >100%: DangerFill, **el llenado siempre muestra 100%** y la
    porción excedida pulsa con escala 0.97 ↔ 1.03 cada 1300 ms.
- Animación del fill: `animateFloatAsState` con tween 600 ms easing
  easeInOut. Sin spring (los springs en barras de tiempo se ven mal).
- Ratio = `(elapsedSec / targetSec).coerceIn(0f, 1f)`.

**Chip derecho** (objetivo):

- Mismo estilo de pill que el izquierdo pero MENOS prominente:
  fondo `OreoPalette.SurfaceCardHi`, texto OnSurfaceMuted.
- Solo muestra el objetivo formateado: "3 min", "1 h 30 min",
  "45 min" (función `formatTargetCompact(targetSec)` que ya existe;
  reusar).
- NO tap-able. Es información, no acción.

**Botón ✕** (cerrar cronómetro):

- IconButton 32 dp con `Icons.Outlined.Close` 16 dp tinted en
  `OnSurfaceMuted`.
- Sin fondo. Sin border. Aparece pegado al chip derecho con un
  spacer de 4 dp.
- Tap = `onClose()`.

### Variante 1 — `OreoTimerVariant.Editor`

Vive al pie del editor, encima de la toolbar de formato.

**Tamaños**:

- Altura total del componente: 32 dp (compact) / 36 dp (medium /
  expanded).
- Tiempo: 13 sp / 14 sp.
- Objetivo: 11 sp / 12 sp.
- Barra de progreso: 3 dp altura.
- Padding horizontal del componente: 12 dp.

**Responsive**:

- En `WindowWidthSizeClass.Compact`: ancho completo del editor
  (`fillMaxWidth()`).
- En Medium/Expanded: `widthIn(max = 720.dp)` + `fillMaxWidth()`,
  centrado con un `Box(contentAlignment = Center)` exterior.
  (Para que la barra de progreso no se estire ridículamente en
  pantallas anchas.)

**Posición**: directamente encima de la `FormatToolbar` en
`EditorScreen`. Sin separador visual entre cronómetro y toolbar
(ambos comparten fondo `Bg0`).

### Variante 2 — `OreoTimerVariant.Discourse`

Vive en la franja superior del modo lectura (`ReaderScreen`),
para presentar discurso.

**Tamaños**:

- Altura total: 56 dp (compact) / 64 dp (medium / expanded).
- Tiempo: 22 sp / 26 sp Bold (más grande, visible a 1 m de distancia).
- Objetivo: 14 sp / 16 sp.
- Barra de progreso: 4 dp altura.
- Padding horizontal: 24 dp.

**Posición**: en `ReaderScreen`, dentro del `Box` raíz, alineado
arriba con `Modifier.align(Alignment.TopCenter).padding(top = 16.dp)`.
Ocupa todo el ancho disponible (con maxWidth aplicado).

**Responsive**:

- Compact: `fillMaxWidth()` con padding horizontal 24 dp.
- Medium / Expanded: `widthIn(max = 880.dp)` + `fillMaxWidth()`
  centrado.

**Auto-hide en modo lectura**: el cronómetro forma parte del
mismo grupo de "controles" que el botón ✕ del reader. Cuando los
controles se ocultan tras 4s de inactividad, el cronómetro se
desvanece a `alpha = 0.4f` (NO desaparece — el usuario debe poder
ver el tiempo siempre, aunque sea con menos prominencia). Cualquier
tap en pantalla lo restaura a `alpha = 1f` y reinicia el timer de
auto-hide.

### Comportamiento de "sobreexcedido" (isOver)

Cuando `elapsedSec >= targetSec`:

- Tiempo en el chip izq: `+HH:MM` en `DangerFill`.
- Barra de progreso: llenado 100% en `DangerFill`.
- La barra completa pulsa muy sutilmente: `infiniteRepeatable`
  alternando alpha entre 0.85 y 1.0 cada 1300 ms easeInOut. **NO**
  pulsar el ancho ni la altura — solo opacidad. Más contenido,
  más elegante.
- Resto del componente igual.

### Animaciones

- **Aparición** (cuando `targetSec` pasa de 0 a > 0): fadeIn 200 ms
  + slideInVertically desde -8 dp.
- **Cambio de color de la barra** (al cruzar 85% / 100%):
  `animateColorAsState` tween 600 ms.
- **Llenado de la barra**: `animateFloatAsState` tween 600 ms
  easeInOut.
- **Pulso de excedido**: solo opacidad 0.85 ↔ 1.0, infinite tween
  1300 ms easeInOut reverse.
- **Toggle play/pause icon**: `AnimatedContent` cross-fade 180 ms.

### Detalles importantes

1. **Tipografía monoespaciada** para los números (`FontFamily.Monospace`).
   Crucial para que los dígitos no se muevan horizontalmente al
   cambiar.

2. **Haptic feedback** en long-press para reset:
   `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)`
   ANTES de invocar `onReset()`.

3. **Accesibilidad**:
   - `contentDescription` en el chip izquierdo:
     "Cronómetro: {tiempo}, {running ? 'corriendo' : 'pausado'}.
     Tocá para pausar/iniciar."
   - `contentDescription` en el ✕: "Quitar cronómetro".
   - `Modifier.semantics { liveRegion = LiveRegionMode.Polite }`
     en el tiempo, anunciando solo cambios de minuto (no segundos
     — sería molesto).

4. **`fontScale` alto**: en `fontScale = 1.5x` los chips se
   agrandan. La barra entre ellos se acorta proporcionalmente.
   Si la barra queda con menos de 40 dp de ancho, ocultarla y
   mostrar solo los chips:

   ```kotlin
   BoxWithConstraints {
       val barAvailable = maxWidth - leftChipWidth - rightChipWidth - 60.dp
       if (barAvailable >= 40.dp) {
           // mostrar barra
       } else {
           // sólo chips, sin barra
       }
   }
   ```

5. **NO uses `Modifier.blur` para halos de glow**. Compose no
   recorta el blur al shape del padre, queda un borde difuso
   visible. Si querés enfatizar, usá border o shadow.

6. **NO duplicar componentes**. Si en `ReaderScreen` hay un
   `TimerRing` privado o cualquier otro composable de cronómetro,
   borrarlo. Solo queda `OreoTimer`.

## Mocks textuales

### Editor / Compact (phone portrait, 412 dp)

```
EditorScreen
┌─────────────────────────────────────────────────────────┐
│  ... contenido de la nota ...                           │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  [⏵ 00:42]  ──────●──────  [3 min]  [✕]                 │  ← 32dp
├─────────────────────────────────────────────────────────┤
│  [📷] [🎬] [☑] │ B I U S │ T <> A ≡ ✏ 🔗 ✨               │  ← toolbar
└─────────────────────────────────────────────────────────┘
```

### Editor / Medium (phone landscape, 915 dp)

```
EditorScreen
┌──────────────────────────────────────────────────────────────────────────────┐
│  ... contenido de la nota ...                                                │
├──────────────────────────────────────────────────────────────────────────────┤
│         [⏵ 00:42]  ──────────●──────────  [3 min]  [✕]                       │  ← 36dp,
│                       ↑ centrado, maxWidth 720dp                              │     centrado
├──────────────────────────────────────────────────────────────────────────────┤
│         [📷] [🎬] [☑] │ B I U S │ T <> A ≡ ✏ 🔗 ✨                             │
└──────────────────────────────────────────────────────────────────────────────┘
```

### Reader / Compact (phone portrait, 412 dp)

```
ReaderScreen (fullscreen, modo discurso)
┌─────────────────────────────────────────────────────────┐
│  [⏵ 00:42]  ─────────●──────  [3 min]   [✕ del reader]  │  ← 56dp
│                                                         │
│                                                         │
│           Título de la nota                             │
│           ─────────────────                             │
│                                                         │
│           Lorem ipsum dolor sit amet,                   │
│           consectetur adipiscing elit...                │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### Reader / Expanded (tablet landscape, 1280 dp)

```
ReaderScreen
┌────────────────────────────────────────────────────────────────────────────────┐
│              [⏵ 00:42]  ──────────●──────────  [3 min]   [✕ del reader]        │  ← 64dp,
│                                                                                │     centrado
│                                                                                │     maxWidth 880dp
│                                                                                │
│                       Título de la nota                                        │
│                       ─────────────────                                        │
│                                                                                │
│                       Lorem ipsum...                                           │
└────────────────────────────────────────────────────────────────────────────────┘
```

## Tests mentales

| Contexto             | Width   | fontScale | Resultado esperado                                          |
|----------------------|---------|-----------|-------------------------------------------------------------|
| Editor portrait      | 412 dp  | 1.0x      | 32dp altura, ancho completo                                 |
| Editor landscape     | 915 dp  | 1.0x      | 36dp altura, centrado maxWidth 720dp                        |
| Editor tablet        | 1280 dp | 1.0x      | 36dp altura, centrado maxWidth 720dp                        |
| Editor a11y          | 360 dp  | 1.5x      | Chips más grandes; si la barra colapsa, mostrar solo chips  |
| Reader phone         | 412 dp  | 1.0x      | 56dp altura, top center, ancho completo                     |
| Reader tablet        | 1280 dp | 1.0x      | 64dp altura, top center, centrado maxWidth 880dp            |
| Reader controles ocultos | -    | 1.0x      | Cronómetro a alpha 0.4 (visible pero discreto)              |
| Cualquiera sobreexcedido | -    | -         | Tiempo "+01:14" rojo, barra roja al 100% pulsando opacidad  |

NUNCA debe verse:

- Halo violeta o glow desbordado.
- Anillo (eliminado del diseño).
- Card con border decorativo.
- Aire vacío entre el chip izquierdo y la barra (la barra debe
  empezar inmediatamente después del chip).
- Barra estirada a 1280 dp en tablet (debe respetar maxWidth).
- Componente duplicado entre editor y reader.

## Migración

1. Crear `ui/components/OreoTimer.kt` con la API descrita arriba y
   las dos variantes implementadas en el mismo composable usando
   `when (variant)` para los tamaños.
2. Borrar `ui/components/BottomTimerBar.kt`.
3. Buscar en `ui/reader/ReaderScreen.kt` cualquier composable de
   cronómetro local (TimerRing, ReaderTimer, etc.) y borrarlo.
4. En `EditorScreen.kt`, reemplazar el llamado a `BottomTimerBar(...)`
   por `OreoTimer(..., variant = OreoTimerVariant.Editor)`. El
   estado interno (`elapsedSec`, `running`) probablemente esté en
   un `remember { mutableIntStateOf(0) }` y un `LaunchedEffect`
   que incrementa cada segundo. Mantenerlos como están — solo
   cambia la UI.
5. En `ReaderScreen.kt`, reemplazar el cronómetro actual por
   `OreoTimer(..., variant = OreoTimerVariant.Discourse)`,
   posicionado dentro del `Box` raíz con
   `Modifier.align(TopCenter).padding(top = 16.dp)`.
6. Actualizar la lógica de auto-hide del Reader para que el
   `OreoTimer` también responda a `controlsVisible` (con `alpha`
   animado).

## Estilo de trabajo

- Comentarios en español, "por qué" no "qué".
- Textos visibles en español rioplatense neutro.
- Solo `OreoPalette` para colores.
- No agregues dependencias.
- No toques otros features (find&replace, export PDF, AI,
  responsive de otras pantallas).

## Entregables

1. Plan de archivos.
2. Mocks textuales en cada breakpoint (mínimo 4 mocks).
3. Implementación.
4. Lista corta "archivo X: cambié Y" al terminar, confirmando que
   `BottomTimerBar.kt` fue **borrado** y que no quedó otro
   composable de cronómetro en ningún lado.
