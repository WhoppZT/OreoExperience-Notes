# Fix: el gradiente del cronómetro está afuera de la pastilla

El intento previo de "Liquid Glass" salió mal. En la captura se ve
claramente que el gradiente Aurora está pintado **abajo** de la
pastilla del cronómetro, ocupando una franja entre el cronómetro
y la `FormatToolbar`. Eso NO es el diseño deseado.

El gradiente DEBE ser el FONDO INTERNO de la pastilla del
cronómetro, no una barra separada por fuera.

## Bug actual

```
╭───────────────────────────────────────────────╮  ← pastilla SurfaceCard (sin gradiente adentro)
│  ⏵  00:00                          5 min      │
╰───────────────────────────────────────────────╯  ✕
▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░    ← ESTA es la barra mal puesta (afuera, separada)
─────────────────────────────────────────────────
[📷] [🎬] [☑] │ B I U S │ ...                       ← FormatToolbar
```

## Diseño correcto

```
╭───────────────────────────────────────────────╮
│▓▓▓▓▓░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░░│   ← gradiente DENTRO de la pastilla
│  ⏵  00:00                          5 min      │   ← contenido encima del gradiente
╰───────────────────────────────────────────────╯  ✕
─────────────────────────────────────────────────
[📷] [🎬] [☑] │ B I U S │ ...                       ← FormatToolbar
```

NO hay barra/gradiente fuera de la pastilla. El track + el fill
del progreso son DOS backgrounds apilados en el MISMO Box, dentro
de la pastilla con `clip(RoundedCornerShape(50))`.

## Causa probable y fix

El agente probablemente puso el gradiente como un Box hermano
debajo de la pastilla, así:

```kotlin
// MAL
Column {
    Box(modifier = Modifier
        .clip(RoundedCornerShape(50))
        .background(SurfaceCard)
    ) {
        // contenido
    }
    Box(modifier = Modifier
        .height(3.dp)
        .background(brush = horizontalGradient(...))   // ← afuera!
    )
}
```

Lo correcto es **un solo Box** con DOS backgrounds apilados:

```kotlin
// BIEN
Box(
    modifier = Modifier
        .height(40.dp)
        .clip(RoundedCornerShape(50))
        .background(OreoPalette.SurfaceCard)         // ← track
        .background(brush = horizontalGradient(...)) // ← fill encima del track
        .border(1.dp, OreoPalette.OutlineFaint, RoundedCornerShape(50))
        .combinedClickable(onClick = onToggle, onLongClick = onReset),
) {
    // contenido (Row con icon + tiempo + objetivo) sobre los backgrounds
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(...)
        Spacer(Modifier.width(8.dp))
        Text(formatHms(elapsedSec), ...)
        Spacer(Modifier.weight(1f))
        Text(formatTargetCompact(targetSec), ...)
    }
}
```

El truco es que en Compose **se pueden encadenar varios `.background()`**
y se aplican en orden: primero el plano (`SurfaceCard`) y encima
el gradiente. Como el `clip` viene antes, los dos backgrounds se
recortan al shape de la pastilla. Sin barra suelta, sin línea por
fuera.

### Brush correcto

El gradiente con fade de 5% antes del corte:

```kotlin
val ratio = (elapsedSec.toFloat() / targetSec.toFloat()).coerceIn(0f, 1f)
val animatedRatio by animateFloatAsState(
    targetValue = ratio,
    animationSpec = tween(600, easing = EaseInOut),
    label = "timerRatio",
)
val fillColor by animateColorAsState(
    targetValue = when {
        isOver        -> OreoPalette.DangerFill
        ratio >= 0.85f-> OreoPalette.WarnFill
        else          -> OreoPalette.Accent
    },
    animationSpec = tween(600),
    label = "timerColor",
)
val isDark = isSystemInDarkTheme()
val fillAlpha = if (isDark) 0.32f else 0.45f

val brush = Brush.horizontalGradient(
    colorStops = arrayOf(
        0f                                       to fillColor.copy(alpha = fillAlpha),
        (animatedRatio - 0.05f).coerceAtLeast(0f) to fillColor.copy(alpha = fillAlpha),
        animatedRatio                             to fillColor.copy(alpha = 0f),
        1f                                       to fillColor.copy(alpha = 0f),
    ),
)
```

Pasale `brush` directamente al segundo `.background(brush)`.

### Botón ✕ FUERA de la pastilla, NO dentro

```kotlin
Row(verticalAlignment = Alignment.CenterVertically) {
    OreoTimerPill(...)          // la pastilla con su gradiente interno
    Spacer(Modifier.width(4.dp))
    if (showClose) {
        IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Outlined.Close, ..., size = 16.dp,
                 tint = OreoPalette.OnSurfaceMuted)
        }
    }
}
```

## Test mental

Después del fix, en la captura debe verse:

- UNA SOLA pastilla con esquinas redondeadas conteniendo
  `⏵  00:00 ............................. 5 min` con un gradiente
  Aurora que va de izq a der, más fuerte donde está el progreso y
  desvaneciéndose hacia la derecha.
- El ✕ a la derecha de la pastilla, fuera, sin fondo propio.
- **NADA** entre la pastilla y la `FormatToolbar`. Ni barra, ni
  línea, ni gradiente suelto.

## Entregable

1. Pegame las 15-20 líneas relevantes (el `Box` principal de la
   pastilla con sus dos `.background()` encadenados).
2. Confirmación de que NO hay ningún Box hermano con gradiente
   debajo de la pastilla.
