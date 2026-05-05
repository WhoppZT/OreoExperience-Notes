# OreoExperience · Notes

Edición móvil (Android) de la familia **OreoExperience**: una app de notas
estructurada para crear, organizar y consultar **discursos** (bosquejos,
escrituras de apoyo, notas libres) — manteniendo la estética *Aurora*
(fondo violeta starry + cristal translúcido) de la versión desktop.

> Compañera móvil de
> [OreoExperience · Edición Aurora (desktop)](https://github.com/WhoppZT/OreoExperienceAurora).

---

## Características

- **Lista de discursos** con buscador (título, escrituras, tags, notas).
- **Editor** con título, texto base / escrituras, etiquetas, **bosquejo
  jerárquico** (puntos + sub-puntos) y notas libres en markdown.
- **Vista de lectura** optimizada para usar en el atril (tipografía
  grande, sin distracciones).
- **Exportar** un discurso a **TXT / Markdown / PDF** y compartir.
- **Backup local en JSON** (export/import) para mover tus discursos
  entre dispositivos.
- **Almacenamiento offline** con Room (no envía nada a internet).
- **Tema Aurora** fijo (violeta starry) — sin cambios automáticos por
  hora del día.

## Stack

| | |
|---|---|
| Lenguaje | Kotlin 2.0 |
| UI | Jetpack Compose + Material 3 |
| DB | Room (SQLite) |
| Persistencia | Local-only (sin backend) |
| Min SDK | 26 (Android 8.0) |
| Target / Compile SDK | 34 (Android 14) |

## Compilar

```bash
# Requiere Android SDK + JDK 17 (las builds están probadas con Gradle 8.9)
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

Para abrirlo en Android Studio: `File → Open` → seleccioná la raíz del repo.

## Estructura

```
app/
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/oreoexperience/notes/
│   │   ├── OreoApp.kt              # Application
│   │   ├── MainActivity.kt
│   │   ├── data/                   # Room + Repository + Backup
│   │   ├── ui/
│   │   │   ├── theme/              # Paleta Aurora + AuroraBackground
│   │   │   ├── components/         # GlassCard, ConfirmDialog
│   │   │   ├── home/               # HomeScreen + HomeViewModel
│   │   │   ├── editor/             # EditorScreen + EditorViewModel
│   │   │   ├── viewer/             # ViewerScreen
│   │   │   └── nav/                # NavHost
│   │   └── util/                   # Export TXT/MD/PDF
│   └── res/                        # strings, themes, ic_launcher
└── build.gradle.kts
```

## Licencia

[MIT](LICENSE) — copiá, modificá, redistribuí libremente.
