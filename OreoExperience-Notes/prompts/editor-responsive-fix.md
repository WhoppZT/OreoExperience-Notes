# Fix: info del cronómetro invisible en landscape

Después de aplicar el responsive del editor, el `BottomTimerBar`
muestra mal en landscape (ver capturas adjuntas):

- En portrait: se ven correctamente el anillo + "Objetivo 6 min ·
  0%" + "Quedan 06:00" + ✕.
- En landscape: solo se ve el anillo y el ✕. La info textual del
  centro **NO aparece**.

## Causa probable

El componente original tiene este layout:

```
Row(fillMaxWidth) {
    TimerRing(76.dp)
    Spacer
    Column(weight = 1f) {  ← info acá
        Row { "Objetivo X min" + chip "%" }
        Text("Quedan ...")
    }
    IconButton(close)
}
```

Cuando se aplicó la responsividad, probablemente lo envolviste así:

```
Box(fillMaxWidth, contentAlignment = Center) {
    Row(
        modifier = Modifier
            .widthIn(max = 720.dp)
            // sin fillMaxWidth → el Row toma el ancho intrínseco
            //                    de sus hijos = anillo + spacer + ✕
            //                    porque la Column con weight(1f)
            //                    colapsa a 0dp si no hay fillMaxWidth.
    ) { ... }
}
```

El `weight(1f)` necesita que el contenedor padre **tenga ancho
definido o ilimitado**. Cuando le quitaste `fillMaxWidth()` al `Row`
y solo le pusiste `widthIn(max = 720.dp)`, el `Row` adopta el ancho
mínimo de sus hijos no-weighted, y la `Column.weight(1f)` se queda
sin espacio para crecer → colapsa a 0 dp → info invisible.

## Fix

El `Row` interno tiene que **expandirse** dentro del Box centrado.
Hay dos formas, las dos correctas:

### Opción A (recomendada — fillMaxWidth dentro del widthIn)

```kotlin
Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center,
) {
    Row(
        modifier = Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()                    // ← clave
            .background(OreoPalette.SurfaceCard, RoundedCornerShape(...))
            .padding(...),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TimerRing(...)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {  // ahora sí tiene
            ...                                   // espacio
        }
        IconButton(onClick = onClose) { ... }
    }
}
```

`widthIn(max = 720.dp)` + `fillMaxWidth()` juntos significan:
"ocupá todo el ancho disponible, **pero hasta un máximo de 720 dp**".
Eso da lo que quería el responsive original (la card no se estira
infinitamente en pantallas anchas) **y** mantiene el `weight(1f)`
funcionando.

### Opción B (alternativa — sin weight, con fillMaxWidth en Column)

Si por algún motivo no querés `fillMaxWidth` en el `Row`, sacale
el `weight(1f)` a la `Column` y dale `fillMaxWidth()` directo al
`Row` con `horizontalArrangement = Arrangement.spacedBy(14.dp)`.
Pero la opción A es más limpia, andá por esa.

## Aplicá lo mismo al cuerpo del editor y a la toolbar de formato

Si en la `FormatToolbar` o en el `Column` del scroll del cuerpo del
editor pasa el mismo bug (contenido que colapsa o se ve raro en
landscape), aplicá el mismo patrón:

```kotlin
Box(fillMaxWidth, contentAlignment = Center) {
    Row(
        modifier = Modifier
            .widthIn(max = MAX_WIDTH)
            .fillMaxWidth()             // ← este es el detalle
            ...
    ) { ... }
}
```

## Test mental para confirmar

1. Phone portrait (412 dp): toolbar y cronómetro ocupan todo el
   ancho como antes.
2. Phone landscape (915 dp): la card del cronómetro queda centrada
   con un máximo de 720 dp, **pero adentro la info textual se ve
   completa** (anillo + "Objetivo X · Y%" + "Quedan HH:MM" + ✕).
3. Tablet landscape (1280 dp): igual que landscape pero con
   `maxWidth = 820 dp`.

Mostrame el cambio que hiciste (las 5-10 líneas relevantes) antes
de cerrar.
