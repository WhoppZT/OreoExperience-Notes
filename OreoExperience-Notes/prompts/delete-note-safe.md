# Eliminar notas con seguridad — swipe revelado + confirmación

Necesito mejorar el sistema de eliminar notas en la pantalla
principal de "OreoExperience Notes". Hoy el swipe-to-delete elimina
la nota directamente al deslizar lo suficiente, lo que es peligroso:
se borra una nota importante con un toque accidental.

Quiero **4 candados de seguridad** apilados:

1. El swipe NO elimina, solo **revela** un botón "Eliminar" debajo
   de la card.
2. Tap en "Eliminar" abre un **diálogo de confirmación** con el
   botón destructivo deshabilitado los primeros 600 ms.
3. Al confirmar, la nota va a papelera y aparece un **Snackbar con
   "Deshacer"** activo durante 7 segundos.
4. Las notas **fijadas (pinned) NO se pueden swipear**. El usuario
   tiene que desfijarlas primero.

Trabajá paso por paso. Antes de tocar archivos mostrame el plan:
qué archivos vas a modificar, y un boceto del flujo. Esperá mi OK.

## Contexto técnico

Stack:

- Kotlin 2.0.20, Jetpack Compose, Material3, Compose BOM
  2024.09.02.
- Identidad Aurora: paleta `OreoPalette` (Accent, AccentSub,
  AccentDeep, Bg0, Bg1, SurfaceCard, SurfaceCardHi, OnSurface,
  OnSurfaceMuted, OnSurfaceFaint, Outline, OutlineFaint,
  DangerFill, WarnFill, OkFill).
- Modelo: `Discurso` (Room) con `pinned: Boolean` y
  `deletedAt: Long?`. La papelera se implementa con
  `deletedAt != null`.

Archivos relevantes que tenés que leer ANTES:

- `ui/home/HomeScreen.kt` — donde está el listado de notas y
  probablemente el `SwipeToDelete`.
- `ui/components/SwipeToDeleteRow.kt` (si existe) — el composable
  del swipe actual.
- `ui/home/HomeViewModel.kt` — VM con la lógica de eliminar /
  restaurar.
- `data/DiscursoRepository.kt` — métodos `trash(d)` y
  `restore(d)` (verificá los nombres reales).
- `ui/components/ConfirmDialog.kt` (si existe) — diálogo de
  confirmación reusable.
- `ui/theme/Color.kt` — paleta.

## Comportamiento detallado

### Candado 1 — Swipe revelado (no elimina automático)

Reemplazar el swipe actual que elimina al cruzar un threshold por
un swipe estilo iOS Mail / Gmail:

- Usuario desliza la card a la izquierda con el dedo.
- La card se va moviendo bajo el dedo, revelando un panel rojo
  detrás con el botón "Eliminar".
- Si suelta antes del **30% del ancho**: la card vuelve a su
  posición original con animación spring (la acción se cancela).
- Si suelta después del **30% del ancho**: la card se queda
  "revelada" en una posición fija que muestra el botón "Eliminar"
  completo. NO elimina automáticamente.
- **NO existe full-swipe que borre directo**. Por más que el
  usuario deslice toda la card hasta el otro borde, la card se
  queda en la posición revelada. Para borrar SIEMPRE hay que
  tocar el botón.
- Tap fuera de la card revelada (en otra parte de la pantalla):
  vuelve a posición original.
- Tap en otra card mientras una está revelada: cierra la primera y
  no abre swipe en la nueva (UX estándar).

#### Ancho del botón "Eliminar"

- 88 dp de ancho (tap target cómodo).
- Color de fondo `OreoPalette.DangerFill`.
- Icono `Icons.Outlined.Delete` blanco 24 dp + label "Eliminar"
  blanco 13 sp en una `Column` centrada.
- Se ve solo cuando la card está movida hacia la izquierda
  exponiéndolo.

#### Detección "está revelada"

Mantener el `dragOffset` por card en un `MutableStateMap<Long,
Float>` o equivalente, indexado por `discurso.id`. Cuando se
queda revelada, persiste el offset hasta que se cierre.

### Candado 2 — Diálogo de confirmación

Tap en el botón "Eliminar" abre un `AlertDialog` con:

- Título: "Eliminar nota"
- Texto: "La nota \"{title}\" se va a la papelera y se queda
  guardada por 30 días. Después se borra para siempre."
  (Si la nota no tiene título, usar "(Sin título)" o el primer
  fragmento del cuerpo, lo que ya use el resto de la app.)
- Dos botones:
  - Izquierda: "Cancelar" — `TextButton`, `OnSurfaceMuted`,
    siempre habilitado.
  - Derecha: "Eliminar" — `TextButton` con `contentColor =
    DangerFill`, fontWeight SemiBold.

#### Botón "Eliminar" deshabilitado 600 ms

Para evitar tap accidental por doble click:

```kotlin
var enabledAt by remember { mutableStateOf(0L) }
LaunchedEffect(Unit) {
    enabledAt = System.currentTimeMillis() + 600
}
val now by produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(50)
    }
}
val deleteEnabled = now >= enabledAt
```

Cuando `deleteEnabled == false`:
- El botón se ve con `alpha = 0.4f`.
- No responde al click (`enabled = false` en TextButton).

Cuando se cumple el delay:
- El botón se ilumina a `alpha = 1f` con un fadeIn 200 ms.
- Ya responde al click.

Si el diálogo se cierra (con cancelar, back, o dismiss), nunca se
debe ejecutar el delete.

### Candado 3 — Snackbar con Deshacer

Al confirmar el delete:

1. Llamar `vm.trash(discurso)` — la nota pasa a `deletedAt = now()`.
2. La card desaparece de la lista con animación de slide horizontal
   + fadeOut.
3. Mostrar un Snackbar con:
   - Mensaje: "Nota eliminada"
   - Acción: "Deshacer"
   - Duración: 7 segundos (no `Short`, no `Long` — usar custom
     `SnackbarDuration.Indefinite` y dismissar manualmente con un
     `delay(7000)`).
4. Si el usuario toca "Deshacer" antes de los 7 s:
   - Llamar `vm.restore(discurso)`.
   - La nota vuelve a la lista con fadeIn slide horizontal.
   - Snackbar se cierra inmediatamente.
5. Si pasan los 7 s sin acción:
   - Snackbar se cierra.
   - La nota queda en papelera. NO se elimina permanentemente — eso
     ya lo hace el job periódico de la app a los 30 días (no toques
     esa lógica).

#### Estilo del Snackbar

- Color de fondo: `OreoPalette.SurfaceCard`.
- Color de texto: `OreoPalette.OnSurface`.
- Color de la acción "Deshacer": `OreoPalette.Accent`.
- Esquinas: `RoundedCornerShape(16.dp)`.
- Posicionado abajo, sobre el FAB de "nueva nota" (con padding
  bottom suficiente para que no se superpongan).

### Candado 4 — Notas pinneadas no se pueden swipear

Si `discurso.pinned == true`:

- El `Modifier.pointerInput` que detecta el drag horizontal NO se
  registra (gate condicional).
- Si el usuario intenta swipearla, **no pasa nada** — no se mueve.
- En vez de eso, mostrar un Snackbar breve (3 s, `SnackbarDuration.Short`)
  con: "Las notas fijadas no se pueden eliminar. Quitá el pin
  primero." Sin acción.
- Esto se dispara con un `pointerInput` separado que detecta solo
  el inicio del drag horizontal y muestra el snackbar, pero NO
  consume el gesto (deja que el resto del scroll funcione).

Alternativa más simple: simplemente no registrar el gesto para
notas fijadas y NO mostrar nada cuando intenten swipear (silencio
completo). Si elegís esta, agregá un mini ícono de candado al
costado del icono de pin para que el usuario entienda. Decidilo
vos según qué se vea más limpio; documentá la elección en el plan.

## Animaciones

- **Drag**: la card sigue al dedo en tiempo real (`offsetX = drag`).
- **Snap a posición revelada**: spring con
  `dampingRatio = MediumBouncy`, `stiffness = MediumLow`.
- **Snap a posición cerrada**: spring igual, pero hacia 0.
- **Aparición del botón "Eliminar"**: el botón está siempre
  detrás de la card, "se descubre" naturalmente al moverse la
  card. Sin animación propia.
- **Fade out de la card al confirmar**: tween 240 ms con
  `slideOutHorizontally(targetOffsetX = { -it })` + `fadeOut`.

## Implementación: estructura del composable

```kotlin
@Composable
fun NoteCardWithSwipe(
    discurso: Discurso,
    onTap: () -> Unit,
    onRequestDelete: () -> Unit,    // dispara el flujo de Candado 2
    onPinnedSwipeAttempt: () -> Unit, // dispara el snackbar de Candado 4
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val revealedOffsetPx = with(density) { -88.dp.toPx() }
    val thresholdPx = with(density) { -88.dp.toPx() * 0.30f } // 30% del botón

    val animatedOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxWidth()) {
        // Botón "Eliminar" detrás
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(OreoPalette.DangerFill, RoundedCornerShape(...))
                .padding(end = 0.dp),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Column(
                modifier = Modifier
                    .width(88.dp)
                    .clickable { onRequestDelete() }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Eliminar",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // Card encima, que se mueve con el drag
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(animatedOffset.value.toInt(), 0) }
                .then(
                    if (discurso.pinned) {
                        // Si está pinned: NO drag horizontal.
                        // Detectamos solo el intento para mostrar
                        // un mensaje (Candado 4).
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = { onPinnedSwipeAttempt() },
                                onHorizontalDrag = { _, _ -> /* swallow */ },
                            )
                        }
                    } else {
                        Modifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    scope.launch {
                                        if (animatedOffset.value < thresholdPx) {
                                            animatedOffset.animateTo(
                                                revealedOffsetPx,
                                                spring(...)
                                            )
                                        } else {
                                            animatedOffset.animateTo(0f, spring(...))
                                        }
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    val newOffset = (animatedOffset.value + dragAmount)
                                        .coerceIn(revealedOffsetPx, 0f)
                                    scope.launch {
                                        animatedOffset.snapTo(newOffset)
                                    }
                                },
                            )
                        }
                    }
                )
                .clickable(enabled = animatedOffset.value == 0f) { onTap() }
                .background(OreoPalette.SurfaceCard, RoundedCornerShape(...)),
        ) {
            // Contenido normal de la nota
        }
    }
}
```

(El código de arriba es ilustrativo de la estructura — adaptarlo
al estilo y composables que ya existan en `HomeScreen.kt`.)

## Diálogo de confirmación

```kotlin
@Composable
fun ConfirmDeleteDialog(
    noteTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val enableAt = remember { System.currentTimeMillis() + 600 }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (System.currentTimeMillis() < enableAt) {
            delay(50)
            now = System.currentTimeMillis()
        }
    }
    val deleteEnabled = now >= enableAt
    val deleteAlpha by animateFloatAsState(
        targetValue = if (deleteEnabled) 1f else 0.4f,
        animationSpec = tween(200),
        label = "deleteAlpha",
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eliminar nota") },
        text = {
            Text(
                "La nota \"$noteTitle\" se va a la papelera y se queda " +
                "guardada por 30 días. Después se borra para siempre."
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = deleteEnabled,
                modifier = Modifier.alpha(deleteAlpha),
            ) {
                Text(
                    "Eliminar",
                    color = OreoPalette.DangerFill,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = OreoPalette.OnSurfaceMuted)
            }
        },
        containerColor = OreoPalette.SurfaceCard,
        titleContentColor = OreoPalette.OnSurface,
        textContentColor = OreoPalette.OnSurfaceMuted,
        shape = RoundedCornerShape(24.dp),
    )
}
```

## Snackbar con Deshacer

Si el `HomeScreen` no tiene `SnackbarHostState` ya, agregalo. Al
confirmar el delete:

```kotlin
scope.launch {
    vm.trash(discurso)
    val result = snackbarHostState.showSnackbar(
        message = "Nota eliminada",
        actionLabel = "Deshacer",
        duration = SnackbarDuration.Short, // ~5-7s, basta con Short
        // Si querés exactamente 7s, usá Indefinite y manualmente
        // cierralo con un job que dure 7000ms.
    )
    if (result == SnackbarResult.ActionPerformed) {
        vm.restore(discurso)
    }
}
```

Si en `HomeViewModel.kt` no hay método `restore(discurso)`, agregalo.
Debería invertir el `trash`: `repository.update(discurso.copy(
deletedAt = null))`.

## Tests mentales

1. Swipe ligero (10% del ancho) → suelto → card vuelve sola sin
   pasar nada.
2. Swipe medio (40%) → suelto → card se queda revelada con el
   botón "Eliminar" visible.
3. Swipe full (100%) → suelto → card se queda revelada (NO
   elimina). Para borrar tengo que tocar el botón.
4. Tocar el botón "Eliminar" → diálogo abre, botón rojo en alpha
   0.4. Espero 600 ms → se ilumina a alpha 1.
5. Toco "Eliminar" en el diálogo dentro de los 600 ms → no pasa
   nada (botón disabled).
6. Espero los 600 ms y toco "Eliminar" → nota desaparece, snackbar
   "Nota eliminada · Deshacer" aparece.
7. Toco "Deshacer" en 3 s → nota vuelve a la lista.
8. No toco nada → snackbar desaparece a los 7 s, nota queda en
   papelera.
9. Intento swipear una nota fijada → no se mueve, aparece snackbar
   "Las notas fijadas no se pueden eliminar..." (o silencio + ícono
   de candado, según lo que decidas).
10. Cancelo el diálogo de confirmación → no pasa nada.
11. Pulso back con el diálogo abierto → diálogo se cierra, nada
    pasa.
12. Mientras una card está revelada, toco otra parte de la pantalla
    (por ejemplo otra card) → la primera se cierra y la nueva se
    abre normalmente (tap = abrir nota). NO se debe abrir un swipe
    nuevo por error.

## Detalles importantes (NO los podés omitir)

1. **NO usar `SwipeToDismiss` de Material3**. Ese componente
   elimina automáticamente al cruzar el threshold y no se puede
   "revelar sin eliminar". Implementá el drag manualmente con
   `pointerInput + detectHorizontalDragGestures + Animatable`.

2. **`change.consume()`** dentro del drag handler para que el
   gesto NO se propague al `LazyColumn` (que sino haría scroll
   vertical en diagonal).

3. **`coerceIn(revealedOffsetPx, 0f)`** para que la card NO se
   pueda mover hacia la derecha (positivo) ni más allá del botón
   revelado.

4. **El click en la card NO debe disparar tap si la card está
   revelada**. Usar `clickable(enabled = animatedOffset.value == 0f)`.

5. **Solo UNA card puede estar revelada a la vez**. Cuando se
   abre una, cerrar las demás. Esto se logra manteniendo el
   estado de "qué card está revelada" en el VM o en un
   `rememberSaveable` del HomeScreen.

6. **Comentarios en español** explicando el "por qué" de cada
   candado, no el "qué".

7. **NO agregues animaciones extras** que no estén descritas
   (sin shake, sin haptic en el swipe, sin colores que cambian a
   medida que se desliza). Solo el spring de snap.

8. **Si la API actual usa `SwipeToDeleteRow.kt`**, podés borrarlo
   y crear un nuevo componente `SwipeRevealRow.kt` con la nueva
   lógica. Si preferís reutilizar el archivo, modificarlo
   completamente.

## Migración

1. Crear o modificar `ui/components/SwipeRevealRow.kt` (o el nombre
   actual si lo querés mantener).
2. Modificar `ui/home/HomeScreen.kt` para usar el nuevo
   composable y manejar el estado del diálogo + snackbar.
3. Asegurar que `HomeViewModel.kt` tiene `trash(d)` y `restore(d)`.
   Agregar el segundo si no existe.
4. Borrar el código viejo de swipe-to-delete que elimina automático.

## Estilo de trabajo

- Comentarios en español, "por qué" no "qué".
- Solo `OreoPalette` para colores.
- No agregues dependencias.
- No toques otros features (editor, find&replace, export PDF, AI,
  cronómetro, papelera/auto-purga).

## Entregable

1. Plan de archivos.
2. Decisión sobre el Candado 4 cuando intentan swipear pinned:
   ¿snackbar explicativo o silencio + ícono de candado?
3. Código completo del nuevo `SwipeRevealRow` (o equivalente).
4. Cambios en `HomeScreen.kt` para integrar el nuevo flujo.
5. Lista de tests mentales 1-12 confirmando que cada uno funciona
   como se espera.
