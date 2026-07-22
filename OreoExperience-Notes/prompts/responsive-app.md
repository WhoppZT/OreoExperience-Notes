# App responsive en cualquier dispositivo — Prompt para agente

Necesito que mi app Android "OreoExperience Notes" sea responsive en
**todos** los tamaños de pantalla y densidades: teléfonos chicos
(≤ 360 dp), teléfonos comunes, teléfonos grandes / landscape,
plegables abiertos, tablets de 7" y 10", y Chromebooks. Hoy hay
varios lugares donde el contenido se rompe, se desborda o queda
recortado.

Ejemplos visibles del bug:

- **`UpdateDialog`** en pantallas angostas (≤ 360 dp): el texto
  "Nueva versión" se parte letra por letra de forma vertical
  ("Nu / eva / ver / sión") porque el chip de versión y el label se
  pelean por el ancho disponible y el `Row` no permite que el label
  haga `softWrap` correctamente.
- **`UpdateDialog`** en pantallas anchas / landscape (≥ 720 dp): los
  botones "Actualizar ahora" / "Actualizar después" quedan cortados
  abajo porque la altura del diálogo no es scrolleable cuando supera
  el viewport y los botones quedan fuera del área visible.

Pero el problema NO es solo del UpdateDialog. Está en muchos lados:

- Diálogos con altura fija (`AlertDialog`, sheets) que no se adaptan.
- Listas y editores que asumen un ancho de teléfono típico.
- Top bars y toolbars con espaciados pensados para 360-420 dp.
- Texto que no respeta `fontScale` del usuario (configuración de
  accesibilidad de tamaño de letra).
- Imágenes / videos del editor con anchos fijos.
- Modales / bottom sheets que no usan `imePadding` en algunos casos.

Antes de tocar archivos, leé el código y mostrame:

1. Lista de archivos que vas a tocar.
2. Resumen de cuáles tienen los peores problemas (top 5).
3. El plan de cambios por categoría (responsive, scroll, fontScale,
   etc.).

Esperá mi OK antes de implementar. Trabajá por categorías
(secciones de abajo). Después de cada categoría, mostrame las
capturas de los breakpoints que verificaste mentalmente.

## Contexto técnico del proyecto

Stack:

- Kotlin 2.0.20, Jetpack Compose, Material3, Compose BOM 2024.09.02
- compose-rich-editor 1.0.0-rc11
- Coil 2.7.0 (con coil-video)
- AGP 8.5.2, minSdk 26, targetSdk 34
- DI manual via `AppContainer`

Identidad "Aurora":

- Acento `#7C3AED`, dark/light, esquinas redondeadas (12-28 dp).
- Paleta `OreoPalette` con properties: Accent, AccentSub, AccentDeep,
  Bg0, Bg1, SurfaceCard, SurfaceCardHi, OnSurface, OnSurfaceMuted,
  OnSurfaceFaint, Outline, OutlineFaint, DangerFill, WarnFill,
  OkFill.

Pantallas principales:

- `ui/home/HomeScreen.kt` — listado de notas, FAB, búsqueda, chips
  de categorías.
- `ui/editor/EditorScreen.kt` — editor de la nota con top bar, rich
  text por bloque, FormatToolbar (LazyRow), BottomTimerBar opcional,
  Find & Replace overlay.
- `ui/settings/SettingsScreen.kt` — sección "General", "Datos",
  "Avanzado", "PDF", "Acerca de".
- `ui/components/UpdateDialog.kt` — el diálogo que se ve roto.
- `ui/components/BottomTimerBar.kt` — anillo Workout Ring.
- `ui/editor/ExportPdfSheet.kt`, `ui/editor/FindReplacePanel.kt`
- `ui/auth/AccessScreen.kt`, `ui/onboarding/...`
- `ui/trash/TrashScreen.kt`, `ui/servicio/...`

## Breakpoints objetivo

Usá `WindowSizeClass` de `material3-window-size-class`. Si la
dependencia no está, agregala:

```toml
androidx-compose-material3-window-size-class = { group = "androidx.compose.material3", name = "material3-window-size-class", version.ref = "composeBom" }
```

Tres clases:

- **Compact** (`width < 600.dp`): teléfonos en portrait. La mayoría.
- **Medium** (`600.dp ≤ width < 840.dp`): teléfonos en landscape,
  plegables medianos.
- **Expanded** (`width ≥ 840.dp`): tablets, plegables abiertos,
  Chromebook.

Calcular una sola vez en `MainActivity` con
`calculateWindowSizeClass(activity)` y propagar via
`CompositionLocal` (crear `LocalWindowSizeClass`).

Densidades a soportar: `ldpi/mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi`.
**No hardcodear pixeles**: todo debe ir en `dp` o `sp`. Si encontrás
algún `Modifier.size(N)` con un Float que es `px`, convertilo o
usalo con `with(density) { N.toDp() }`.

Font scale (configuración de accesibilidad del usuario): el sistema
puede multiplicar todos los `sp` por entre 0.85x y 2x. Probá
mentalmente con `fontScale = 1.3` que NADA quede cortado en pantallas
de 360 dp.

## Categoría 1 — Diálogos y sheets

Regla general: **todo diálogo o sheet con contenido textual debe ser
scrolleable verticalmente**, con `widthIn(max = 560.dp)` y
`heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.9f)`
para que NO ocupe toda la pantalla en tablet.

### `UpdateDialog`

Bug 1 (texto "Nueva versión" partido letra por letra):

- El header tiene un `Row` con el chip de versión y el label "Nueva
  versión" sin permitir wrap. El label no tiene `softWrap`.
- Fix: envolver el `Row` con `FlowRow` (`androidx.compose.foundation.layout.FlowRow`)
  para que cuando no quepan en una línea, el label baje a la línea
  siguiente. O directamente moverlo a una `Column`:

  ```
  Row { icon  →  Title }
  Row { chip versión   →   "Nueva versión" }   // FlowRow
  ```

- Si la pantalla es ≤ 360 dp width, ocultar el label "Nueva versión"
  (es redundante, el chip ya dice la versión).

Bug 2 (botones cortados en landscape):

- El cuerpo del diálogo (donde está el markdown del changelog) NO
  está dentro de un `Column.verticalScroll(rememberScrollState())`.
- Fix: envolver el contenido en un `Column` scrolleable, dentro de
  `Surface(modifier = Modifier.heightIn(max = ...))`. Los botones
  deben ir FUERA del scroll, fijos al fondo del diálogo.

  Estructura final:

  ```
  Surface(modifier = Modifier
      .widthIn(max = 560.dp)
      .heightIn(max = (screenHeight * 0.9f).dp)
  ) {
      Column {
          Header(...)              // fixed
          Box(modifier = Modifier
              .weight(1f, fill = false)
              .verticalScroll(scroll)
          ) {
              Markdown(changelog)
          }
          Row(...) { Buttons() }   // fixed at bottom
      }
  }
  ```

### Otros diálogos / sheets a auditar y aplicar la misma regla

- `EnumPicker` (en SettingsScreen) — tiene `Column` con todas las
  opciones. Si las opciones son muchas, no scrollea. Envolver en
  `Column.verticalScroll`.
- `TimerDurationDialog` en EditorScreen.
- `LinkDialog` en EditorScreen.
- `AlertDialog` de "Eliminar nota".
- `ExportPdfSheet` (ModalBottomSheet) — verificar que la preview no
  empuje los botones fuera del viewport en pantallas chicas.
- `AiSuggestionSheet` (si existe).
- `FindReplacePanel`.

Para `ModalBottomSheet`:

- Setear `sheetMaxWidth = 640.dp` cuando WindowSizeClass es Expanded
  (en tablets el sheet full-width se ve raro).
- Usar `WindowInsets.imePadding()` siempre.
- El contenido en `Column` con `Modifier.verticalScroll` si puede
  desbordar.

## Categoría 2 — Editor (`EditorScreen`)

- **Rich text por bloque**: ya está bien (cada bloque es width-flex).
  Verificar que el padding horizontal del editor sea proporcional:

  ```
  val horizontalPadding = when (windowSizeClass.widthSizeClass) {
      Compact  -> 16.dp
      Medium   -> 32.dp
      Expanded -> 64.dp   // contenido centrado tipo Notion
  }
  ```

  Y limitar el ancho máximo de la columna de texto a 720 dp en
  Expanded para que sea cómodo de leer (líneas no ultra largas).

- **`FormatToolbar`** (LazyRow): ya es scrolleable horizontal. OK.
  Pero los botones tienen tamaño fijo de 38 dp; en `fontScale = 1.5x`
  los iconos no escalan pero los labels de los popups (heading,
  highlight) sí. Auditar y arreglar para que el popup respete el
  `fontScale` con `widthIn(max = ...)`.

- **`BottomTimerBar`**: el anillo de 76 dp es OK; no escala con
  `fontScale` (no debería). Verificar que la fila inferior con
  "Objetivo X · Y%" + botones no haga overflow en `fontScale = 1.5x`
  + width 360 dp. Si ocurre, usar `Modifier.basicMarquee()` en el
  texto largo o `FlowRow`.

- **Find & Replace `FindReplacePanel`**: en pantallas Expanded el
  panel debe centrarse y limitarse a 640 dp de ancho, no estirarse
  full width.

- **Imágenes y videos en bloques**: limitar height a
  `min(originalHeight, screenHeight * 0.6)` en mobile,
  `screenHeight * 0.5` en tablet, para que no tomen toda la pantalla
  cuando son cuadradas o portrait.

## Categoría 3 — Home (`HomeScreen`)

- **Compact**: lista vertical de cards de notas (como ahora).
- **Medium / Expanded**: grid de 2 columnas (Medium) o 3 columnas
  (Expanded). Usar `LazyVerticalStaggeredGrid` con
  `StaggeredGridCells.Adaptive(minSize = 280.dp)`. En la nota más
  larga el card será más alto, queda estilo Pinterest.

- **Top bar**: en Compact mantiene el botón hamburguesa o lo que
  haya hoy. En Expanded considerar mostrar la búsqueda inline
  permanente en lugar de un IconButton que abre un sheet.

- **FAB de "nueva nota"**: en Expanded podés moverlo de bottom-end a
  un botón Extended FAB en la top bar (más fácil de alcanzar con
  mouse o stylus).

## Categoría 4 — Settings (`SettingsScreen`)

- **Compact**: como hoy, una columna scrolleable.
- **Expanded**: layout de dos paneles (lista de secciones a la
  izquierda, contenido a la derecha) tipo iPad Settings. Si es mucho
  trabajo, dejarlo como una columna centrada con `widthIn(max = 720.dp)`.

- Auditar cada `ChevronRow`/`ToggleRow`: que no use texto fijo que
  desborde en `fontScale = 1.5x`. Los `Text` deben tener
  `overflow = Ellipsis` o estar dentro de FlowRow.

## Categoría 5 — Onboarding y Auth

- `OnboardingScreen` y `AccessScreen`: verificar que el contenido
  central no se vea perdido en tablets (limitar ancho a 480 dp,
  centrar). En landscape, evitar que las imágenes ocupen 100% del
  height.

## Categoría 6 — Insets y system bars

Verificar que TODAS las pantallas top-level usen
`Modifier.systemBarsPadding()` (o `Modifier.statusBarsPadding()` +
`navigationBarsPadding()`). El editor tiene un caso especial con la
top bar y el panel de Find que ya están bien; revisar el resto:

- `HomeScreen`
- `SettingsScreen`
- `TrashScreen`
- `OnboardingScreen`

Para teclado: `Modifier.imePadding()` en todo `Composable` con
campos de texto. No usar `windowSoftInputMode = adjustResize` y el
padding manual como única solución; usar `imePadding()` que se
adapta automáticamente.

## Categoría 7 — Foldables y display cutout

- `Modifier.displayCutoutPadding()` en pantallas que usan el ancho
  completo (status bar areas en notch).
- Para foldables: los layouts de Expanded ya cubren el caso del
  plegable abierto. Para detectar el "fold posture" hace falta
  WindowManager Jetpack — lo dejamos fuera de esta tanda.

## Categoría 8 — Tipografía y `fontScale`

- Verificar que NINGÚN texto use `Modifier.height(N.dp)` que asume
  un line-height fijo. En su lugar, usar
  `Modifier.heightIn(min = N.dp)`.
- Los `Text` con altura específica → cambiar a auto-height + padding.
- En `fontScale = 1.5x`, ningún botón debe quedar con texto cortado:
  agregar `wrapContentWidth()` y `maxLines = 1` solo donde tenga
  sentido.

Test mental: configurá en tu cabeza `fontScale = 1.5` y
`width = 360 dp` y revisá cada pantalla. Donde haya overflow,
arreglar.

## Categoría 9 — Landscape

- Editor en landscape: la `FormatToolbar` LazyRow ya scrollea
  horizontal. OK.
- En landscape de teléfono, el `BottomTimerBar` puede tapar mucho
  del contenido. Considerá hacerlo colapsar a un mini-chip flotante
  cuando la altura disponible es < 480 dp.
- Diálogos en landscape de teléfono: limitar `heightIn(max = ...)`
  con scrolleable interno (cubierto en Categoría 1).

## Categoría 10 — Imágenes y assets

- Verificar que todas las imágenes/iconos usen `vector drawables`
  (.xml). Si hay PNGs, deben estar en mdpi + xhdpi + xxhdpi mínimo.
- Logos / illustrations grandes: usar `Modifier.fillMaxWidth(0.7f)`
  con `widthIn(max = 360.dp)` para que no se vean enormes en tablet.

## Estilo de trabajo

- Comentarios en español, "por qué" no "qué".
- Textos visibles en español rioplatense neutro.
- Solo `OreoPalette` para colores.
- No agregues dependencias además de `material3-window-size-class`
  si no está ya.
- No toques la lógica (VM, Repository, navegación). Solo UI.

## Entregables

Después de cada Categoría, mostrame una lista de archivos
modificados con el cambio resumido en una línea. No me des todo el
diff, solo el resumen.

Categorías 1-3 son las prioritarias. Categorías 4-10 son polish.
Hagamos primero 1-3, las verifico, y después seguimos.

## Test mental antes de declarar terminado

Mentalmente, verificá cada pantalla en estos 4 escenarios:

| Escenario | Width | Height | fontScale |
|-----------|-------|--------|-----------|
| Phone chico vertical | 320 dp | 568 dp | 1.0x |
| Phone normal vertical | 412 dp | 915 dp | 1.0x |
| Phone normal landscape | 915 dp | 412 dp | 1.0x |
| Phone con accesibilidad | 360 dp | 800 dp | 1.5x |
| Tablet vertical | 800 dp | 1280 dp | 1.0x |
| Tablet landscape | 1280 dp | 800 dp | 1.0x |

En ningún escenario debe haber: texto cortado, botones invisibles,
overflow horizontal, ni texto roto letra por letra.
