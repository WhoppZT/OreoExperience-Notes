# Cambios realizados

## 1. Rediseño completo de la Top Bar del editor

**Archivos:** `ui/components/EditorTopBar.kt`, `ui/editor/EditorScreen.kt`, `ui/editor/EditorViewModel.kt`

- **Nuevo diseño minimalista**: se eliminó el glassmorphism, blur y partículas. Ahora es una barra plana sobre `Bg0` con una línea divisoria inferior `OutlineFaint` que aparece únicamente al hacer scroll (scroll-aware).
- **Título de nota siempre visible**: 17sp SemiBold OnSurface con subtítulo opcional debajo (conteo de palabras · tiempo relativo, ej: "14 palabras · Editado hace 5 min"). Toda la zona del título es clickeable para enfocar el campo de título.
- **Botón back neutro**: tint `OnSurface` en lugar de `Accent`, sin label "Notas" pegado.
- **Cluster de acciones limpio**: íconos sin pipas separadoras, con espaciado de 4dp. El botón de búsqueda usa `AnimatedContent` con `scaleIn`/`fadeIn` para la transición lupa ↔ close, y se tiñe de `Accent` cuando el panel está abierto.
- **Indicador de guardado sutil**: `AnimatedVisibility` + `AnimatedContent` interno que transiciona entre spinner "Guardando…" y check verde "Guardado", con fade-out automático (1.5s manejado por el ViewModel).
- **Subtítulo dinámico**: se agregaron los helpers `countWords()`, `formatRelativeTime()` y `computeSubtitle()` en `EditorScreen.kt`. Se expusieron `createdAt`/`updatedAt` en `EditorUiState`.
- **Eliminado**: undo/redo de la barra (se mantienen atajos Ctrl+Z/Y), pipas separadoras, glassmorphism, partículas decorativas.

## 2. Modo Lectura / Discurso (nueva funcionalidad)

**Archivos nuevos:** `ui/reader/ReaderScreen.kt`, `ui/reader/ReaderViewModel.kt`
**Archivos modificados:** `ui/nav/AppNav.kt`, `ui/components/EditorTopBar.kt`, `ui/editor/EditorScreen.kt`

- **Pantalla full-screen inmersiva**: oculta status bar y navigation bar (`WindowCompat.setDecorFitsSystemWindows(false)`), mantiene la pantalla encendida (`FLAG_KEEP_SCREEN_ON`). Restaura todo al salir con `DisposableEffect`.
- **Renderizado de notas en solo lectura**: cada bloque se renderiza según su tipo:
  - `NoteBlock.Text` → `BasicRichText` (compose-rich-editor en read-only, 19sp, line-height 30sp)
  - `NoteBlock.Image` → `AsyncImage` de Coil
  - `NoteBlock.Video` → miniatura con overlay de play
  - `NoteBlock.Checklist` → cuadros custom con tachado al completar
- **Cronómetro reutilizado**: se usa el mismo `BottomTimerBar` del editor, anclado al fondo de la pantalla con `navigationBarsPadding()`.
- **Botón cerrar**: 44dp con fondo semitransparente, esquinas redondeadas, en la esquina superior derecha.
- **Navegación**: nueva ruta `"reader/{id}"` en `AppNav`. Dos accesos desde el editor:
  - Botón `PlayArrow` en la top bar (visible solo si la nota tiene contenido textual)
  - Item "Modo lectura" en el menú Oreo "···"
