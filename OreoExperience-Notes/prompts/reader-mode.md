# Modo lectura / discurso — Prompt para agente

Necesito agregar un MODO LECTURA / DISCURSO al editor de mi app Android
"OreoExperience Notes". Es una vista de solo visualización pensada para
leer la nota mientras hablás en público (discurso, exposición, sermón),
con el cronómetro siempre a la vista.

Antes de tocar archivos mostrame el plan: archivos que vas a crear /
modificar, cómo se navega al modo, y un boceto del layout. Esperá mi OK.

## Contexto técnico

Stack:

- Kotlin 2.0.20, Jetpack Compose, Material3, Compose BOM 2024.09.02
- compose-rich-editor 1.0.0-rc11
- Identidad "Aurora": acento `#7C3AED`, dark/light, paleta `OreoPalette`
  (Accent, AccentSub, Bg0, Bg1, SurfaceCard, SurfaceCardHi, OnSurface,
  OnSurfaceMuted, OnSurfaceFaint, Outline, OutlineFaint, DangerFill,
  WarnFill, OkFill).

Modelo:

- `Discurso` (Room) con `targetDurationSec: Int` (objetivo del
  cronómetro de la nota; 0 = sin objetivo).
- Nota = lista de `NoteBlock`: `Text(id, markdown=HTML)`, `Image`,
  `Video`, `Checklist`.
- En el editor existe un componente `BottomTimerBar` (estilo "Workout
  Ring") que ya implementa cronómetro con anillo de progreso, play /
  pause, long-press para reset, contador "X de Y", colores que cambian
  al 85% (ámbar) y 100% (rojo + pulso). Ese composable lo vamos a
  reusar.

Archivos relevantes que tenés que leer ANTES:

- `ui/editor/EditorScreen.kt` (top bar, menú "···", BottomTimerBar)
- `ui/components/BottomTimerBar.kt`
- `ui/nav/AppNav.kt` o el archivo donde están las rutas
- `data/NoteBlock.kt` (modelo de bloques)
- `ui/theme/Color.kt` (paleta)
- `data/MediaStorage.kt` (para mostrar imágenes / videos)

## Qué tiene que hacer el modo lectura

Es una pantalla NUEVA, no un toggle dentro del editor. Permite:

1. Ver el contenido de la nota renderizado (texto con formato,
   imágenes, videos, checklists con cuadritos), SOLO LECTURA. No se
   puede tocar el contenido para editar; el cursor no aparece, el
   teclado nunca se abre.
2. Ver el cronómetro siempre visible (anillo grande, prominente),
   con la misma lógica del `BottomTimerBar` pero adaptado a la nueva
   pantalla. Arranca automáticamente al entrar.
3. Navegar el contenido con scroll suave, en un layout pensado para
   leer parado a un metro de distancia (texto más grande, line-height
   más generoso, márgenes amplios).
4. Salir con un gesto de back, swipe down, o tap en un botón "✕" que
   se autoesconde tras unos segundos de inactividad.

## Cómo se entra al modo lectura

Dos accesos:

**A.** Botón "▷ Lectura" en la TOP BAR del editor, a la izquierda del
   icono de búsqueda. Solo visible si la nota tiene al menos un
   bloque de texto con contenido
   (`state.blocks.any { it is NoteBlock.Text && it.markdown.isNotBlank() }`).
   Icono: `Icons.Outlined.PlayCircleOutline`.
   Tap: ejecuta `vm.saveBlocking()` y navega a la pantalla
   `ReaderScreen` pasando el id de la nota.

**B.** Item "Modo lectura" en el menú "···" del editor, con icono
   `Icons.Outlined.PlayCircleOutline` tinted en `Accent`. Mismo
   comportamiento que el botón A.

## Navegación

Agregar una ruta nueva en el grafo de navegación (probablemente en
`ui/nav/AppNav.kt` o equivalente):

```
ROUTE: "reader/{discursoId}"
ARG:   discursoId: Long
```

Composable nuevo: `ui/reader/ReaderScreen.kt`

Crear un `ReaderViewModel` que:

- Carga el `Discurso` desde el repositorio una sola vez.
- NO permite ediciones, no instancia `RichTextState` mutable, no
  registra `blockStates`.
- Expone:

  ```kotlin
  val state: StateFlow<ReaderUiState>
  fun toggleTimer()
  fun resetTimer()
  fun setTargetMinutes(min: Int)   // por si el usuario quiere
                                   // ajustar desde la pantalla

  data class ReaderUiState(
      val title: String = "",
      val blocks: List<NoteBlock> = emptyList(),
      val targetSec: Int = 0,
      val running: Boolean = true,   // arranca corriendo solo
      val elapsedSec: Int = 0,
      val loaded: Boolean = false,
  )
  ```

Lógica del cronómetro en el VM (mismo patrón que `BottomTimerBar`):

```kotlin
init {
    viewModelScope.launch {
        while (isActive) {
            delay(1000)
            if (state.value.running) increment elapsedSec
        }
    }
}
```

## Diseño del Reader Screen

Full-screen inmersivo (oculta status bar y nav bar):

```kotlin
SideEffect {
    val window = (context as Activity).window
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, window.decorView).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}
```

Restaurar al salir con `DisposableEffect`.

Layout (en `Box` `fillMaxSize`, fondo `OreoPalette.Bg0`):

```
┌─────────────────────────────────────────────────────────┐
│                                                         │
│   ╭───────────────╮                                     │  ← Anillo
│   │   01:42       │                                     │    cronómetro
│   │   ⏵ / 5 min   │                                     │    (top-left)
│   ╰───────────────╯                              [✕]   │  ← Cerrar
│                                                         │     (top-right)
│   ───── divisor sutil OutlineFaint ─────────────────    │
│                                                         │
│      Título de la nota                                  │
│      ─────────────────                                  │
│                                                         │
│      Lorem ipsum dolor sit amet,                        │
│      consectetur adipiscing elit.                       │
│      Ut enim ad minim veniam, quis                      │
│      nostrud exercitation ullamco                       │
│      laboris nisi ut aliquip...                         │
│                                                         │
│      [imagen renderizada]                               │
│                                                         │
│      ☐ Item de checklist                                │
│      ☑ Item completado (tachado)                        │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

Especificaciones:

- **Anillo del cronómetro** (top-left, padding 20dp desde los bordes):
  Reusar la lógica visual de `BottomTimerBar` (arco de progreso, color
  que cambia al 85% / 100%, pulso al sobreexceder) pero rediseñado a
  un tamaño más grande, anillo de 110dp con stroke 6dp, en una pieza
  flotante (no card horizontal). Tap = play / pause. Long-press = reset.

  Dentro del anillo:

  - Tiempo transcurrido en mono, 22.sp Bold, OnSurface
  - Debajo: "/ 5 min" en mono 11.sp, OnSurfaceMuted
  - Mini icono play / pause de 14dp tinted en Accent (debajo del
    tiempo objetivo)

  Si la nota NO tiene `targetDurationSec` (o == 0): mostrar solo
  cronómetro libre sin objetivo (sin "/ X min", sin progreso ring,
  solo un círculo con el tiempo y el play / pause).

- **Botón cerrar [✕]** (top-right, padding 20dp):
  `IconButton` 44dp con fondo `SurfaceCard.copy(alpha = 0.6f)`
  circular, icono `Outlined.Close` 22dp tinted en `OnSurface`.
  Auto-hide: tras 4 segundos sin interacción, fade a alpha 0.3.
  Cualquier tap en pantalla lo restaura a alpha 1.0 y reinicia el
  timer de auto-hide.
  Tap = navega back, mismo efecto que back de Android.

- **Divisor**: 1dp altura, `OutlineFaint`, padding horizontal 24dp.
  Solo visible cuando el scroll position > 8dp.

- **Contenido scrolleable**:
  `Column` con `verticalScroll` y padding horizontal 32dp,
  vertical 24dp.

  Renderizado por bloque:

  - `NoteBlock.Text` →
    RichText (compose-rich-editor) configurado en read-only:

    ```kotlin
    val state = rememberRichTextState()
    LaunchedEffect(block.markdown) { state.setHtml(block.markdown) }
    BasicRichText(state = state, modifier = ...)
    ```

    `textStyle`: 19.sp, `lineHeight` 30.sp, `OnSurface`,
    `fontWeight = Normal`.
    Esto es CLAVE: usar `BasicRichText` (no `BasicRichTextEditor`)
    para que sea read-only sin teclado.
    Si la library no tiene un `BasicRichText` read-only en rc11,
    usar `BasicRichTextEditor` con `enabled = false` Y NO dar
    focus a ningún bloque.

  - `NoteBlock.Image` →
    `AsyncImage` de Coil cargando desde
    `mediaStorage.uriFor(fileName)`, con
    `Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))`.
    Tap NO abre preview (es solo lectura).

  - `NoteBlock.Video` →
    `AsyncImage` del primer frame (Coil con `coil-video`) +
    overlay un play-icon 48dp en el centro tinted en blanco con
    background semi-transparente. Tap reproduce el video en un
    diálogo aparte usando ExoPlayer o, más simple, lanza un
    `Intent.ACTION_VIEW`. Si es complejo, dejarlo como
    "miniatura sin reproducción" en esta primera versión y
    documentarlo.

  - `NoteBlock.Checklist` →
    `Column` con cada item en una Row:

    - Cuadrito 22dp con borde `Outline` (1.5dp) o relleno
      `Accent` si `checked`, con ✓ blanco.
    - Texto 19.sp, `OnSurface` si `!checked`, `OnSurfaceFaint`
      con `textDecoration = LineThrough` si `checked`.

    IMPORTANTE: solo lectura; tap NO togglea el `checked`.

- **Título de la nota** (encima del primer bloque):
  `Text` 28.sp Bold, `OnSurface`, padding-bottom 16dp.

- **Espaciado entre bloques**: 16dp.

- **Padding inferior del scroll**: 80dp para que el último párrafo no
  quede pegado al borde.

## Interacciones

1. **Tap simple en cualquier parte que no sea el anillo o el ✕**:
   muestra / oculta los controles (anillo + ✕) con fade 200ms.

   - Estado visible por default al entrar.
   - Auto-hide tras 4s de inactividad.
   - Un tap mientras están ocultos → los muestra.
   - Esto es estándar de modos "presentación".

2. **Tap en el anillo**: play / pause del cronómetro, NO oculta nada.

3. **Long-press en el anillo**: reset (vuelve a 00:00, sigue en pause
   o running según estaba). Confirmar con haptic feedback.

4. **Tap en ✕**: navega back.

5. **Back gesture / hardware back**: navega back. Antes de salir,
   pausar el cronómetro automáticamente (no resetear: el tiempo se
   conserva por si el usuario reentra rápidamente).

6. **Swipe horizontal NO hace nada** (a propósito — no queremos
   navegación entre notas accidental).

7. **Mantener pantalla encendida**:

   ```kotlin
   DisposableEffect(Unit) {
       (context as Activity).window.addFlags(
           WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
       )
       onDispose {
           (context as Activity).window.clearFlags(
               WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
           )
       }
   }
   ```

   La pantalla NO se apaga mientras esté en modo lectura.

## Accesibilidad

- `contentDescription` en todos los `IconButton`.
- El contenido scrolleable mantiene el orden de lectura natural
  (top-down).
- El cronómetro se anuncia con `liveRegion = Polite` cuando cambia
  cada minuto (no cada segundo, sería molesto para screen readers).

## Edge cases

1. Nota vacía (sin bloques de texto con contenido) → no debe permitir
   entrar al modo lectura (botón oculto, ítem del menú deshabilitado).
2. Nota con `targetDurationSec = 0` → cronómetro libre (sin anillo
   de progreso, solo conteo ascendente).
3. Cronómetro al cruzar el target → mismo comportamiento del
   `BottomTimerBar` (color rojo, pulso). No mostrar diálogo bloqueante;
   solo feedback visual.
4. Si el usuario gira el dispositivo: mantener cronómetro corriendo
   (`rememberSaveable` + estado en VM, no en composable).
5. Si el VM se destruye y vuelve (ej: low memory): el cronómetro
   resetea (compromiso aceptable, mismo que `BottomTimerBar`).

## Estilo de trabajo

- Mostrame plan antes de tocar archivos. Esperá mi OK.
- Comentarios en español, "por qué" no "qué".
- Textos visibles en español rioplatense neutro ("tocá", "escribí").
- Solo `OreoPalette` para colores.
- No agregues dependencias (Coil, ExoPlayer, etc. ya existen si están
  en el proyecto; verificá antes).
- No toques otros features (Find & Replace, undo / redo, export PDF,
  etc.).

## Entregable

- Plan de archivos.
- `ReaderScreen.kt` + `ReaderViewModel.kt` creados.
- Ruta nueva en el grafo de navegación.
- Botón en la top bar del editor + ítem en el menú "···".
- Modo full-screen inmersivo con keep-screen-on.
- Renderizado por bloques en read-only.
- Cronómetro grande con todas las interacciones (tap, long-press).
- Auto-hide de controles tras 4s.
