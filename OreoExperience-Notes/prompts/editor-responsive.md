# Editor responsive (toolbar + cronómetro) — Prompt para agente

Necesito hacer responsive la sección del editor de mi app Android
"OreoExperience Notes". En **portrait** todo se ve bien; el problema
está en **landscape** y pantallas anchas (tablets, plegables abiertos):

1. La **toolbar de formato** (la barra inferior con B / I / U / S /
   listas / color / etc.) en landscape extiende los íconos con
   espacios enormes entre ellos. Pierde la agrupación visual y se
   ve desperdigada en toda la pantalla.

2. El **cronómetro** (`BottomTimerBar`, estilo Workout Ring) en
   landscape queda con el anillo chico a la izquierda y un espacio
   gigante vacío a la derecha. La info ocupa <40% del ancho y el
   resto es vacío.

Ambas barras viven al pie del editor y son visibles a la vez.

Antes de tocar archivos mostrame el plan: archivos que vas a
modificar, qué cambios concretos en cada uno, y un mock textual de
cómo va a quedar en cada breakpoint. Esperá mi OK antes de
implementar.

## Contexto técnico

Stack:

- Kotlin 2.0.20, Jetpack Compose, Material3, Compose BOM 2024.09.02
- compose-rich-editor 1.0.0-rc11
- Identidad "Aurora": acento `#7C3AED`, dark/light, esquinas
  redondeadas. Paleta `OreoPalette` con properties: Accent,
  AccentSub, Bg0, Bg1, SurfaceCard, SurfaceCardHi, OnSurface,
  OnSurfaceMuted, OnSurfaceFaint, Outline, OutlineFaint, DangerFill,
  WarnFill, OkFill.

Archivos relevantes que tenés que leer ANTES:

- `ui/editor/EditorScreen.kt` — es donde vive la `FormatToolbar` que
  está al pie. Buscá el composable `FormatToolbar(...)` (es privado,
  está en el mismo archivo) y los `ToolbarButton`. Hoy usa un
  `LazyRow` con padding horizontal pequeño.
- `ui/components/BottomTimerBar.kt` — el cronómetro. Hoy está armado
  como un `Row` horizontal con el anillo a la izquierda y la info
  al lado, todo dentro de un `Surface` con `fillMaxWidth()`.
- `ui/theme/Color.kt` — paleta.

Probablemente uses `WindowSizeClass` (de
`androidx.compose.material3.windowsizeclass`). Si la dependencia
no está en el `libs.versions.toml`, agregala.

## Breakpoints a considerar

Calcular `WindowSizeClass` desde `MainActivity` y propagar via
`CompositionLocal` (crear `LocalWindowSizeClass` si no existe).

- **Compact width** (< 600 dp): teléfono portrait. Layout actual
  es OK; mantenerlo.
- **Medium width** (600-840 dp): teléfono landscape, plegables
  medianos. Aplicar layout adaptado.
- **Expanded width** (≥ 840 dp): tablets, plegables abiertos.
  Layout adaptado con limitación de ancho máximo.

Las dos barras (toolbar de formato + cronómetro) siempre van al pie,
una arriba de la otra. Ambas tienen que respetar el mismo ancho
máximo y centrarse cuando haya espacio sobrante.

## Problema 1 — Toolbar de formato

### Síntoma actual (landscape)

Los íconos quedan distribuidos a lo ancho con grandes espacios
entre ellos porque el `LazyRow` tiene `horizontalArrangement` que
no rellena, y como el contenido es chico comparado con el ancho de
landscape, queda esparcido. Además se pierden las agrupaciones
lógicas (insertar / formato / heading / listas / etc.).

### Diseño propuesto

**Compact width** (sin cambios — ya funciona):

- `LazyRow` scrollable horizontal a lo ancho del editor.
- Padding horizontal 6 dp.
- Mantener separadores (`VerticalDivider`) para agrupar secciones.

**Medium width** (≥ 600 dp):

- La toolbar deja de ser `LazyRow` que se estira: pasa a ser un
  `Row` con ancho máximo `widthIn(max = 720.dp)` **centrado**
  horizontalmente con `Modifier.align(Alignment.CenterHorizontally)`.
- Los íconos se mantienen del tamaño actual, pero quedan agrupados
  en el centro de la pantalla en vez de esparcidos a lo ancho.
- Si el `Row` excede el ancho disponible (por `fontScale` alto),
  hacer fallback a `LazyRow` con `horizontalArrangement =
  Arrangement.Center`.

**Expanded width** (≥ 840 dp):

- Igual que Medium pero con `widthIn(max = 800.dp)`.

### Implementación

Convertir la `FormatToolbar` actual en un `Box(fillMaxWidth)` que
contiene un `Row(widthIn(max = ...).align(CenterHorizontally))`
con los mismos `ToolbarButton` y `VerticalDivider`. En Compact, el
ancho máximo es `Dp.Infinity` (sin límite). En Medium, 720.dp. En
Expanded, 800.dp.

```kotlin
@Composable
private fun FormatToolbar(...) {
    val windowSizeClass = LocalWindowSizeClass.current
    val maxWidth = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact  -> Dp.Infinity
        WindowWidthSizeClass.Medium   -> 720.dp
        WindowWidthSizeClass.Expanded -> 800.dp
        else                           -> Dp.Infinity
    }

    Surface(
        color = OreoPalette.Bg0,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            // Si maxWidth = Infinity → LazyRow tradicional (Compact)
            // Si maxWidth limitado → Row centrado con scroll horizontal
            //   por si los items no entran (fontScale alto).
            val rowModifier = Modifier
                .widthIn(max = maxWidth)
                .horizontalScroll(rememberScrollState())   // por si no entra

            Row(
                modifier = rowModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                ToolbarButton(...)
                ToolbarButton(...)
                VerticalDivider()
                ToolbarButton(...)
                // ... el mismo orden que ya tiene
            }
        }
    }
}
```

Si la implementación actual usa `LazyRow`, podés **mantenerla**
para Compact y crear una variante con `Row + horizontalScroll` para
Medium/Expanded. O, más simple, usar siempre `Row + horizontalScroll`
y solo cambiar el `widthIn(max = ...)` según el WindowSizeClass.

Cualquiera funciona; elegí la que toque menos código.

## Problema 2 — Cronómetro (`BottomTimerBar`)

### Síntoma actual (landscape)

El componente actual es un `Row` con:

- Anillo "Workout Ring" (76 dp) a la izquierda.
- `Column` con info ("Objetivo X · Y%", "Quedan/Excedido") al lado.
- Botón ✕ a la derecha.

En landscape, la info ocupa < 40% del ancho y el resto queda vacío.
Se ve desbalanceado.

### Diseño propuesto

**Compact width** (sin cambios — actual):

- Layout horizontal compacto: anillo + info + ✕, todo apretado a la
  izquierda, ancho completo.

**Medium width**:

- Mismo layout horizontal pero **centrar la card** con
  `widthIn(max = 600.dp).align(CenterHorizontally)`.
- Aumentar levemente el padding interno.
- El anillo sigue siendo de 76 dp.

**Expanded width**:

- `widthIn(max = 720.dp)` centrado.
- Anillo más grande (88 dp) porque hay espacio.
- Info con más padding y tipografía un tick más grande.

### Implementación

Misma estrategia que la toolbar: un `Box(fillMaxWidth)` que
contiene la card con `widthIn(max = ...).align(CenterHorizontally)`.

```kotlin
@Composable
fun BottomTimerBar(
    targetSec: Int,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val maxWidth = when (windowSizeClass.widthSizeClass) {
        WindowWidthSizeClass.Compact  -> Dp.Infinity
        WindowWidthSizeClass.Medium   -> 600.dp
        WindowWidthSizeClass.Expanded -> 720.dp
        else                           -> Dp.Infinity
    }
    val ringSize = if (windowSizeClass.widthSizeClass ==
        WindowWidthSizeClass.Expanded) 88.dp else 76.dp

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        // ... contenido actual envuelto con widthIn(max = maxWidth)
        Row(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .padding(...)
                .background(OreoPalette.SurfaceCard, RoundedCornerShape(...))
                ...
        ) {
            TimerRing(size = ringSize, ...)
            ...
        }
    }
}
```

## Aspectos comunes a las dos barras

1. **Ancho consistente**: la toolbar y el cronómetro deben usar el
   **mismo** maxWidth en cada breakpoint para que se vean alineados.
   Si la toolbar va con 720 dp en Medium, el cronómetro también
   720 dp (no 600). Ajustá los maxWidths para que coincidan:

   | Breakpoint  | maxWidth común |
   |-------------|----------------|
   | Compact     | sin límite     |
   | Medium      | 720 dp         |
   | Expanded    | 820 dp         |

2. **Insets**: ambas usan `imePadding()` y `navigationBarsPadding()`
   ya. Verificarlo.

3. **No tocar el contenido funcional**: ningún ícono cambia,
   ningún feature se modifica. Solo cambia cómo se distribuye el
   espacio horizontal.

4. **Ambas barras heredan un fondo común**: el contenedor que las
   envuelve tiene `OreoPalette.Bg0`. La card del cronómetro tiene
   `SurfaceCard`. Si en Medium/Expanded queda raro que la card
   esté centrada con dos costados negros, agregar un divisor
   sutil arriba de la toolbar (`OutlineFaint`, 1 dp) que ayude a
   delimitar visualmente.

## Top bar (mismo principio)

La top bar del editor (back, título, búsqueda, menú) **también**
debería respetar el mismo maxWidth en Medium/Expanded. Aplicar la
misma estrategia: `Box(fillMaxWidth)` con un `Row` interno con
`widthIn(max = 820.dp).align(CenterHorizontally)`.

Si ya está implementado de otra forma, dejarlo y solo ajustar el
maxWidth.

## Contenido del editor (cuerpo de la nota)

Mismo problema potencial: en pantallas anchas el cuerpo del editor
ocupa todo el ancho y las líneas se vuelven incómodas de leer (más
de 100 caracteres por línea).

Aplicar `widthIn(max = 720.dp)` al `Column` del scroll vertical del
editor en Medium/Expanded. En Compact, `Dp.Infinity`. Esto matchea
con el patrón de Notion/Bear.

## Tests mentales antes de declarar terminado

Mentalmente verificá en estos escenarios:

| Escenario              | Width   | Resultado esperado                              |
|------------------------|---------|-------------------------------------------------|
| Phone portrait         | 412 dp  | Layout actual sin cambios                       |
| Phone landscape        | 915 dp  | Toolbar y timer centrados con maxWidth ~720dp   |
| Phone landscape pequeño| 720 dp  | Toolbar y timer centrados con maxWidth ~720dp   |
| Tablet portrait        | 800 dp  | Centrados con maxWidth ~820dp                   |
| Tablet landscape       | 1280 dp | Centrados, anillo 88dp, cuerpo limitado         |
| fontScale 1.5x phone   | 360 dp  | Toolbar scrollable horizontal sin desbordar     |

En ningún caso debe haber:

- Íconos esparcidos con espacios enormes entre ellos.
- Cronómetro con un costado vacío gigante.
- Líneas de texto del editor que no se pueden leer cómodamente
  por ser demasiado anchas.
- Toolbar o timer cortados / desbordados / con scroll que no
  funciona.

## Estilo de trabajo

- Comentarios en código en español, "por qué" no "qué".
- Textos visibles en español rioplatense neutro ("tocá", "andá").
- Solo `OreoPalette` para colores.
- Mismo `WindowSizeClass` que use el resto de la app — si no está,
  agregalo y propagalo via `CompositionLocal`.
- No agregues dependencias además de
  `material3-window-size-class` (si no está ya).
- No toques otros features (find&replace, export PDF, AI, undo).

## Entregables

1. Plan de archivos.
2. Mock textual de cómo va a quedar la toolbar y el timer en cada
   breakpoint (3 mocks por barra).
3. Implementación.
4. Lista corta de "archivo X: cambié Y línea por Z" al terminar.

Empezá por la toolbar (Problema 1). Cuando confirme, seguís con
el cronómetro (Problema 2). Y al final, top bar y cuerpo del editor.
