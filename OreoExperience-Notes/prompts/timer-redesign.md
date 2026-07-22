# Rediseño del cronómetro — minimalista, premium, responsive

Necesito rediseñar el cronómetro de mi app Android "OreoExperience
Notes". Hoy se usa en dos contextos:

1. **Editor** (`BottomTimerBar` al pie del editor, encima de la
   toolbar de formato).
2. **Modo lectura / discurso** (`ReaderScreen` — pantalla full-
   screen para presentar la nota mientras se habla en público).

El diseño actual es funcional pero pesado: card con borde, padding
generoso, info textual a la derecha del anillo. No combina del todo
con la estética premium "Aurora" que tiene el resto de la app y se
ve desbalanceado en ciertas configuraciones.

Quiero rehacerlo desde cero priorizando: **minimalismo**, **buena
estética**, **responsive perfecto** en todos los tamaños, y **buena
ubicación para presentar un discurso** (visible a un metro de
distancia, sin estorbar el texto, sin botones que el usuario pueda
tocar por error mientras lee).

Antes de tocar archivos mostrame el plan: qué archivos vas a
modificar, mock textual (ASCII) de cómo va a quedar en cada
contexto y breakpoint, y qué decisiones tomaste. Esperá mi OK
antes de implementar.

## Contexto técnico

Stack:

- Kotlin 2.0.20, Jetpack Compose, Material3, Compose BOM 2024.09.02
- compose-rich-editor 1.0.0-rc11
- Identidad **Aurora**: acento `#7C3AED`, dark/light, esquinas
  redondeadas (12-24 dp), tipografía SansSerif del sistema.
- Paleta `OreoPalette` con properties: Accent, AccentSub, AccentDeep,
  Bg0, Bg1, SurfaceCard, SurfaceCardHi, OnSurface, OnSurfaceMuted,
  OnSurfaceFaint, Outline, OutlineFaint, DangerFill, WarnFill,
  OkFill.

Archivos relevantes:

- `ui/components/BottomTimerBar.kt` — el cronómetro actual del
  editor.
- `ui/reader/ReaderScreen.kt` — pantalla del modo lectura (revisalo;
  ahí también vive un anillo de cronómetro, posiblemente
  duplicado del del editor). Si todavía no existe, ignoralo.
- `ui/editor/EditorScreen.kt` — donde se monta el `BottomTimerBar`.
- `ui/theme/Color.kt` — paleta.
- `MainActivity.kt` — `LocalWindowSizeClass` ya está expuesto si se
  hizo el responsive previo; reusalo.

Lógica del cronómetro (NO cambia):

- Estado: `targetSec: Int`, `elapsedSec: Int`, `running: Boolean`.
- Tap en el anillo → play / pause.
- Long-press en el anillo → reset (con haptic feedback).
- Color que cambia según ratio: violeta hasta 85%, ámbar entre 85%
  y 100%, rojo + pulso al pasar de 100%.

## Filosofía del rediseño

### Minimalismo

- Sin card con border decorativo.
- Sin padding excesivo.
- Sin label "Objetivo" repetido (ya hay un chip con el porcentaje;
  con eso alcanza).
- Una sola tipografía, dos pesos: Regular y SemiBold.
- Sin color innecesario (el accent solo aparece en el progreso del
  anillo y en el chip de %).

### Estética premium

- Tipografía monoespaciada para los números (sensación de reloj).
- Anillo más fino (4 dp en vez de 5-6 dp) para que se vea elegante,
  no industrial.
- Animación suave del progreso (tween 600 ms easing easeInOut).
- Pulso muy sutil del anillo cuando se sobrepasa el target (escala
  0.98 ↔ 1.02, no 0.97 ↔ 1.03 — más contenido).
- Halo de glow muy suave alrededor del anillo cuando corre, en
  Accent.copy(alpha = 0.15f), 8 dp de blur. Detalle que se siente
  más que se nota.

### Responsive

Usar `LocalWindowSizeClass.current.widthSizeClass`:

- **Compact** (< 600 dp): variante "in-editor" compacta.
- **Medium** (600-840 dp): variante "in-editor" centrada con
  maxWidth.
- **Expanded** (≥ 840 dp): variante "in-editor" centrada con
  maxWidth mayor; anillo levemente más grande.

Para el modo lectura, el cronómetro siempre es la **variante
discurso** (anillo grande flotante), independiente del breakpoint.
Solo cambian los tamaños de fuente y el padding según `WindowSizeClass`.

## Diseño propuesto: dos variantes del mismo componente

Crear UN componente `OreoTimer` con un parámetro `style`:

```kotlin
sealed interface TimerStyle {
    data object EditorBar : TimerStyle      // pie del editor
    data object Discourse : TimerStyle      // modo lectura, flotante
}

@Composable
fun OreoTimer(
    targetSec: Int,
    elapsedSec: Int,
    running: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit,
    onClose: (() -> Unit)? = null,    // null = sin botón cerrar
    style: TimerStyle,
    modifier: Modifier = Modifier,
)
```

El estado (elapsedSec, running) lo maneja el caller (editor o
reader VM); el componente es puramente presentacional.

### Variante 1 — `TimerStyle.EditorBar`

Diseño compacto que vive al pie del editor.

**Mock textual** (Compact / portrait):

```
┌────────────────────────────────────────────────────────────┐
│                                                            │
│   ╭───╮  00:42                       Quedan 02:18    [✕]  │
│   │ ◯ │  ────  Objetivo 3 min · 22%                        │
│   ╰───╯                                                    │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

Especificaciones:

- **Sin card con border**. Solo un fondo `SurfaceCard` con esquinas
  redondeadas a 18 dp.
- Padding 12 dp horizontal, 10 dp vertical.
- **Anillo a la izquierda**, 56 dp de diámetro, stroke 4 dp.
- Dentro del anillo: solo el icono play / pause centrado, 20 dp,
  tinted en Accent (sin tiempo adentro — el tiempo va al lado).
- A la derecha del anillo, en `Column` con `weight(1f)`:
  - **Línea 1**: tiempo transcurrido en MONO 22 sp Bold OnSurface
    (visible siempre).
  - **Línea 2**: subtítulo en 12 sp OnSurfaceMuted con dos partes
    separadas por "·":
    - Si NO se excedió: "Objetivo {Xmin} · {Y%}"
    - Si se excedió: "Excedido +{HH:MM}" en `DangerFill`
    - El "Quedan {HH:MM}" del diseño anterior se elimina —
      redundante con el porcentaje.
- A la derecha, un IconButton 32 dp con ✕ (color OnSurfaceFaint).
  Solo visible si `onClose != null`.

**Responsive**:

- Compact: ancho completo del editor.
- Medium / Expanded: `widthIn(max = 720.dp)` + `fillMaxWidth()`,
  centrado con un `Box(contentAlignment = Center)` exterior.
  (Requisito para que `weight(1f)` no colapse.)

### Variante 2 — `TimerStyle.Discourse`

Diseño grande pensado para presentar discurso. Anillo centrado,
prominente, con el tiempo dentro del anillo. Sin card de fondo.

**Mock textual** (full-screen):

```
┌──────────────────────────────────────────────────────────┐
│                                                          │
│                                                          │
│                ╭─────────────╮                           │
│              ╭─                ─╮                        │
│             │                    │                       │
│             │      00:42         │                       │
│             │     ⏵ / 03:00      │                       │
│             │                    │                       │
│              ╰─                ─╯                        │
│                ╰─────────────╯                           │
│                                                          │
│                  Objetivo 3 min                          │
│                                                          │
└──────────────────────────────────────────────────────────┘
```

Especificaciones:

- **Sin card, sin fondo**. Es flotante: lo que hay detrás es la nota.
- **Anillo centrado**, tamaños responsive:
  - Compact: 140 dp diámetro, stroke 5 dp
  - Medium: 160 dp, stroke 5 dp
  - Expanded: 180 dp, stroke 6 dp
- Dentro del anillo, en `Column` centrada:
  - **Tiempo transcurrido** en MONO 30 sp (Compact), 36 sp (Medium),
    42 sp (Expanded), Bold, OnSurface. Si supera la hora, baja a
    24/28/32 sp para que no se desborde.
  - Debajo: ícono play / pause (16 dp) + "/ {target}" (mono 13 sp,
    OnSurfaceMuted) en una `Row` centrada. Ej: "▷ / 03:00".
- DEBAJO del anillo, separado por 16 dp:
  - "Objetivo X min" en 13 sp, OnSurfaceMuted, centrado. Si está
    excedido: "Excedido +{HH:MM}" en `DangerFill`.
- Tap en cualquier parte del anillo → play / pause.
- Long-press en el anillo → reset (con haptic). NO mostrar botón
  reset visual: el long-press es suficiente. Para discurso, menos
  botones = menos toques accidentales.
- **No hay botón cerrar**. El cerrar es responsabilidad del padre
  (la pantalla `ReaderScreen` ya tiene su propio botón ✕).

**Posición en el ReaderScreen**:

- Anillo posicionado en la **esquina superior izquierda** con
  padding 24 dp desde los bordes (top + start).
- En Expanded podés alternativamente ponerlo arriba-centro si te
  parece más estético, pero la esquina top-left es estándar de
  apps de presentación porque no compite con el contenido.

### Halo de glow (común a las dos variantes)

Cuando `running == true` y NO se excedió, el anillo tiene un halo
muy sutil:

```kotlin
Box(
    modifier = Modifier
        .size(ringSize + 16.dp)
        .blur(8.dp)
        .background(
            color = ringColor.copy(alpha = 0.20f),
            shape = CircleShape,
        )
)
```

Detrás del anillo. Crea un efecto premium sin ser molesto.

Cuando `isOver`, el halo se vuelve `DangerFill.copy(alpha = 0.25f)`
y pulsa con la misma animación del anillo (0.98 ↔ 1.02).

### Animaciones

- **Aparición** (cuando `targetSec` pasa de 0 a > 0): fadeIn 200 ms
  + scaleIn 0.9 → 1.0 con spring (DampingRatioMediumBouncy).
- **Cambio de color del anillo** (al cruzar 85% / 100%):
  `animateColorAsState` con tween 600 ms.
- **Llenado del anillo**: `animateFloatAsState` para `ratio` con
  tween 600 ms easing easeInOut. Sin spring (los springs en
  progreso de tiempo se ven raros).
- **Pulso al exceder**: `infiniteRepeatable(tween(1300, easeInOut),
  RepeatMode.Reverse)` afectando `scale` 0.98 ↔ 1.02 y opacity del
  halo 0.55 ↔ 1.0.
- **Toggle play / pause**: el ícono morph entre `Icons.Outlined.PlayArrow`
  y `Icons.Outlined.Pause` con `AnimatedContent` y crossfade 180ms.

## Detalles importantes que NO se pueden olvidar

1. **`weight(1f)` en EditorBar**: la `Column` de info necesita
   `weight(1f)` y para que funcione el `Row` padre debe tener
   `fillMaxWidth()` (NO solo `widthIn(max)`). Si falta, la info
   colapsa y se ve solo el anillo + ✕.

2. **MaxWidth con centrado**: en Medium/Expanded el `Box` exterior
   debe tener `Modifier.fillMaxWidth()` y `contentAlignment =
   Alignment.Center`, y el contenido interno
   `widthIn(max = X).fillMaxWidth()`.

3. **TipoOgrafía monoespaciada**: para todos los números de tiempo
   (00:42, 03:00, +01:14). Mono = `FontFamily.Monospace`. Sin esto
   los números se mueven horizontalmente al cambiar.

4. **Haptic en long-press**: usar
   `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)`
   ANTES de ejecutar el reset, para feedback inmediato.

5. **Accesibilidad**: `contentDescription` en el anillo
   ("Cronómetro: {tiempo}, {running ? 'corriendo' : 'pausado'}").
   Para screen readers, anunciar cambios de minuto con
   `Modifier.semantics { liveRegion = LiveRegionMode.Polite }` —
   no anunciar segundos para no marear.

6. **`fontScale` alto**: en Compact con `fontScale = 1.5x`, el
   tiempo de 22 sp pasa a 33 sp. Si no entra en su línea, hacer
   wrap a la siguiente línea NO es opción (rompe el layout). En su
   lugar: `Text(... maxLines = 1, overflow = Visible,
   softWrap = false)` y el container se acomoda.

## Migración

1. Crear `ui/components/OreoTimer.kt` con el componente nuevo y la
   sealed `TimerStyle`.
2. En `EditorScreen.kt`, reemplazar el llamado a `BottomTimerBar(...)`
   por `OreoTimer(..., style = TimerStyle.EditorBar)`. El estado
   (elapsed/running) lo seguís pasando desde donde lo tenías
   (probablemente un `remember { mutableIntStateOf(0) }` y un
   `LaunchedEffect(running) { while (running) { delay(1000); ++ } }`).
3. En `ReaderScreen.kt`, reemplazar el anillo actual por
   `OreoTimer(..., style = TimerStyle.Discourse)` posicionado en la
   esquina superior izquierda con `Modifier.align(Alignment.TopStart)
   .padding(24.dp)` dentro del Box raíz de la pantalla.
4. Borrar `BottomTimerBar.kt` y cualquier composable de cronómetro
   duplicado en ReaderScreen (`TimerRing` privado, etc.).

## Tests mentales

Verificá en estos escenarios:

| Contexto       | Width   | fontScale | Resultado esperado                                              |
|----------------|---------|-----------|-----------------------------------------------------------------|
| Editor portrait| 412 dp  | 1.0x      | Card ancho completo, anillo izq, info centro, ✕ derecha         |
| Editor landscape| 915 dp | 1.0x      | Card centrada maxWidth 720dp, info textual visible              |
| Editor tablet  | 1280 dp | 1.0x      | Card centrada maxWidth 820dp                                    |
| Editor a11y    | 360 dp  | 1.5x      | Tiempo más grande, no se corta, info wrappea si hace falta      |
| Reader phone   | 412 dp  | 1.0x      | Anillo 140dp top-left, "Objetivo X min" debajo                  |
| Reader tablet  | 1280 dp | 1.0x      | Anillo 180dp top-left con padding generoso                      |
| Reader a11y    | 412 dp  | 1.5x      | Tiempo escala (30 → 45 sp) sin desbordar el anillo              |

En NINGÚN caso:

- Info textual invisible o cortada.
- Anillo ovalado / deformado.
- Números que se "mueven" horizontalmente al cambiar.
- Botón ✕ pegado al borde sin padding.
- Halo glow visible cuando `running == false` (debería estar
  apagado).

## Estilo de trabajo

- Comentarios en español, "por qué" no "qué".
- Textos visibles en español rioplatense neutro.
- Solo `OreoPalette` para colores.
- No agregues dependencias.
- No toques otros features (editor, find&replace, export PDF, AI,
  responsive de otras pantallas).

## Entregable

1. Plan de archivos.
2. Mocks textuales de las DOS variantes en cada breakpoint
   (mínimo 4 mocks).
3. Implementación.
4. Lista corta "archivo X: cambié Y" al terminar.
