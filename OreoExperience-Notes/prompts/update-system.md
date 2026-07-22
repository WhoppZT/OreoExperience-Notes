# Sistema de actualizaciones (engine + UI) — Prompt para agente

Necesito mejorar el sistema de actualizaciones de mi app Android
"OreoExperience Notes". Hoy tiene problemas tanto en la **lógica**
(engine de check / descarga / instalación) como en el **diseño** del
diálogo. Quiero ambas cosas refactorizadas para sentirse premium y
ser confiable.

Antes de tocar archivos mostrame el plan: archivos que vas a
modificar / crear, qué cambios concretos en cada uno, y un boceto
del nuevo diálogo. Esperá mi OK antes de implementar.

## Contexto del proyecto

Stack:

- Kotlin 2.0.20, Jetpack Compose, Material3, Compose BOM 2024.09.02
- compose-rich-editor 1.0.0-rc11
- Coil 2.7.0
- AGP 8.5.2, minSdk 26, targetSdk 34
- DI manual via `AppContainer`
- kotlinx-serialization 1.7.2 (ya configurado)

Identidad "Aurora":

- Acento `#7C3AED`, dark/light, esquinas redondeadas (12-32 dp).
- Paleta `OreoPalette` (Accent, AccentSub, AccentDeep, Bg0, Bg1,
  SurfaceCard, SurfaceCardHi, OnSurface, OnSurfaceMuted,
  OnSurfaceFaint, Outline, OutlineFaint, DangerFill, WarnFill,
  OkFill).

Distribución:

- La app NO está en Play Store. Se distribuye sideload firmando con
  `release.keystore` checked-in en el repo (mismo signing en debug
  y release para que actualizaciones no requieran desinstalar).
- Las releases se publican en GitHub Releases del repo
  `WhoppZT/OreoExperience-Notes`.

Archivos relevantes que tenés que leer ANTES:

- `util/UpdateChecker.kt` — engine actual.
- `ui/components/UpdateDialog.kt` — diálogo actual.
- `data/UserPreferences.kt` — para agregar prefs nuevas.
- `data/AppContainer.kt` — para inyectar el nuevo manager.
- `MainActivity.kt` u `OreoApp.kt` — buscá dónde se llama el check
  hoy.
- `AndroidManifest.xml` — verificar permisos / providers.

## Problemas actuales identificados

### En el engine (`UpdateChecker.kt`)

1. **Verificación de conexión con `Runtime.exec("ping ...")`**:
   bloquea hilo, depende de que el dispositivo tenga `ping` (no
   garantizado en Android moderno), y no es confiable. Hay que
   reemplazarlo por `ConnectivityManager.activeNetwork`.
2. **Descarga sin progreso**: el usuario ve "Descargando..." sin
   indicador real, y si la APK pesa 30 MB con red lenta parece
   colgada.
3. **Sin reintentos**: si falla la descarga, no hay reintento
   automático ni botón "Reintentar".
4. **Sin verificación de tamaño / integridad**: no se valida que la
   descarga llegó completa (`Content-Length` vs `outFile.length()`).
5. **Sin verificación de firma**: cualquier APK que GitHub sirva se
   instala. Como el repo es controlado por vos y la API responde
   con HTTPS, hay protección contra MITM, pero conviene validar la
   firma del APK al menos comparando el digest contra una constante
   conocida (ver más abajo).
6. **Instalación con `Intent.ACTION_VIEW`**: funciona pero es el
   método legacy. En Android 12+ se prefiere `PackageInstaller`
   con un `BroadcastReceiver` para feedback. Mantengamos
   `ACTION_VIEW` para no complicar, pero verificar que el flag
   `REQUEST_INSTALL_PACKAGES` está en el manifest (en API 26+ es
   obligatorio).
7. **`parseVersionCode("1.0.1")` → `10001`**: pero el versionCode
   actual es `100` (versionName "1.0.0 ORBETA"). Vamos a mismatchear
   siempre porque `100 < 10001` por más que sea la misma versión.
   Hay que comparar **strings semánticos** (semver), no enteros
   inflados.
8. **Sin cache del check**: cada vez que se abre la app hace request
   a GitHub. Conviene cachear el resultado con TTL (ej: 6 horas) y
   solo refrescar si pasó ese tiempo.
9. **Sin "no avisar de nuevo" persistente**: si el usuario hace tap
   en "Actualizar después", al siguiente boot vuelve a salir el
   mismo diálogo. Eso molesta. Hay que guardar el `tagName` que el
   usuario decidió posponer y NO mostrarlo de nuevo hasta que
   aparezca uno más reciente.
10. **Sin distinción entre actualización opcional y crítica**: si
    una release tiene un fix de seguridad importante, el usuario no
    debería poder posponer.

### En el diseño (`UpdateDialog.kt`)

1. **Botón "Actualizar después" en rojo**: `DangerFill` confunde.
   Posponer NO es destructivo. Tiene que ser un botón terciario
   discreto, no rojo.
2. **Header gradiente toma demasiado alto** (24dp + 24dp + content
   pesado). En pantallas chicas come la mitad del diálogo. Hay que
   reducirlo.
3. **`dismissOnBackPress = false`** + **`dismissOnClickOutside =
   false`**: bloquea totalmente. Para releases opcionales debería
   ser dismissable; para críticas, sí bloquearlo.
4. **Falta texto sobre tamaño de descarga** y **conexión actual**:
   el usuario no sabe si va a descargar 30 MB con datos móviles.
5. **Sin barra de progreso real**: solo dice "Descargando..." sin
   porcentaje ni MB descargados.
6. **Markdown sin renderizar**: el `release.body` viene con `###`,
   `**bold**`, viñetas `-`, y se muestra crudo. Hay que parsearlo a
   `AnnotatedString` con un parser básico (sin librería pesada).
7. **`screenWidthDp > 360`** condiciona ocultar el label "Nueva
   versión" — está bien, pero el FlowRow ya estaba para wrappear,
   redundante.

## Refactor propuesto

### 1. Nuevo módulo: `data/update/UpdateManager.kt`

Reemplaza el `object UpdateChecker` por una clase inyectable en
`AppContainer`:

```kotlin
class UpdateManager(
    private val context: Context,
    private val httpClient: HttpClient,   // reusar el de AI/Ktor si existe
    private val prefs: UserPreferences,
) {

    sealed interface CheckResult {
        data object UpToDate : CheckResult
        data class Available(
            val release: GitHubRelease,
            val isCritical: Boolean,
            val sizeBytes: Long,
        ) : CheckResult
        data object Postponed : CheckResult   // hay update pero el
                                              // usuario lo pospuso
        data class Error(val message: String) : CheckResult
    }

    sealed interface DownloadProgress {
        data object Idle : DownloadProgress
        data class Downloading(
            val bytesReceived: Long,
            val totalBytes: Long,
        ) : DownloadProgress {
            val ratio: Float get() =
                if (totalBytes > 0) bytesReceived.toFloat() / totalBytes else 0f
        }
        data class Verifying(val ratio: Float) : DownloadProgress
        data class Ready(val apkFile: File) : DownloadProgress
        data class Failed(val message: String) : DownloadProgress
    }

    val downloadProgress: StateFlow<DownloadProgress>

    suspend fun checkForUpdate(force: Boolean = false): CheckResult
    fun downloadAndInstall(release: GitHubRelease): Job
    fun cancelDownload()

    fun postpone(tagName: String)         // marca esta versión como
                                          // "no avisar"
    fun isPostponed(tagName: String): Boolean
}
```

#### Comparación de versiones (semver simple)

```kotlin
data class SemVer(val major: Int, val minor: Int, val patch: Int) :
    Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        return patch - other.patch
    }
    companion object {
        fun parse(s: String): SemVer? {
            val clean = s.trimStart('v', 'V').takeWhile {
                it.isDigit() || it == '.'
            }
            val parts = clean.split('.').mapNotNull { it.toIntOrNull() }
            return when (parts.size) {
                3 -> SemVer(parts[0], parts[1], parts[2])
                2 -> SemVer(parts[0], parts[1], 0)
                1 -> SemVer(parts[0], 0, 0)
                else -> null
            }
        }
    }
}
```

Comparar `versionName` actual del package contra `release.tagName`
parseado a `SemVer`. Si `remote > current`, hay update.

#### Detección de conectividad

```kotlin
private fun isOnline(): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    val net = cm?.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(net) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
           caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

private fun isMetered(): Boolean {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    return cm?.isActiveNetworkMetered ?: true
}
```

Usar `isMetered()` para mostrar advertencia "Estás usando datos
móviles, descargar igual?" antes de iniciar.

#### Releases críticas

Convención: si el `release.body` (Markdown) contiene la línea
`<!-- critical -->` o el `tagName` termina en `-critical`, marcar
como crítica. El diálogo crítico no se puede dismissar.

#### Descarga con progreso

Usar Ktor `prepareGet` + `bodyAsChannel().toInputStream()` para leer
en streaming y emitir progreso al StateFlow cada 512 KB. Reintentar
hasta 3 veces con backoff exponencial (1s, 2s, 4s) si falla la red.

Validar `Content-Length` contra el tamaño descargado. Si difiere,
descarte y reintento.

#### Cache del check

Guardar en `UserPreferences`:

- `updateLastCheckMillis: Long`
- `updateLastTagName: String`
- `updateLastCriticalFlag: Boolean`
- `updatePostponedTag: String` (qué versión decidió posponer el user)

`checkForUpdate(force = false)` devuelve cache si fue hace menos de
6 horas. `force = true` ignora cache (úsalo desde Settings).

### 2. Permisos y manifest

Verificar que `AndroidManifest.xml` tenga:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
```

Y que el `FileProvider` ya configurado para `cache-path name="updates"`
sigue siendo correcto.

### 3. UI nueva: `UpdateDialog`

Reemplazar el diseño actual. Layout:

```
╭─────────────────────────────────────────╮
│ ◷  Actualización disponible        [✕]  │   ← header simple,
│    1.0.1 · 14 MB · Hace 2 días          │     sin gradiente alto
├─────────────────────────────────────────┤
│                                         │
│  Novedades                              │   ← scroll interno
│                                         │
│  • Búsqueda y reemplazo en el editor    │
│  • Modo lectura para discursos          │
│  • Mejoras de rendimiento               │
│  ...                                    │
│                                         │
├─────────────────────────────────────────┤
│  [▼ Actualizar ahora]                   │   ← botón primario
│                                         │
│   Más tarde                             │   ← link discreto
╰─────────────────────────────────────────╯
```

**Header** (más bajo y elegante):

- Sin gradiente full-width. Fondo `OreoPalette.SurfaceCard`.
- Icono `Outlined.Update` 22dp en un círculo de 36dp con
  `Accent.copy(alpha = 0.15f)` y border `1.dp` Accent.
- Título "Actualización disponible" 16.sp SemiBold OnSurface.
- Subtítulo: "{version} · {sizeFormatted} · {publishedRelative}"
  12.sp OnSurfaceMuted (los 3 separados por "·"). Si la versión es
  crítica, agregar un chip mini "Crítica" en `WarnFill`.
- Botón [✕] arriba a la derecha (solo si no es crítica).

**Cuerpo**:

- Label "Novedades" 12.sp SemiBold Accent letterSpacing 0.5.sp.
- Espacio de 8 dp.
- **Markdown renderizado**:
  - `###` headings → 14.sp SemiBold OnSurface.
  - `**bold**` → spanstyle bold.
  - `*italic*` o `_italic_` → spanstyle italic.
  - Líneas que empiezan con `- ` o `* ` → bullet 6dp Accent + texto.
  - Líneas que empiezan con `> ` → cita con border-left 2dp Accent
    y padding-left 12dp.
  - El resto: párrafo normal 14.sp OnSurface lineHeight 22.sp.
  - Si hay links `[texto](url)`, hacerlos tappeables que abren
    Intent.ACTION_VIEW.
- Container scrolleable (`Modifier.verticalScroll`) con
  `weight(1f, fill = false)` y `heightIn(max = ...)` para que no
  empuje los botones fuera de la pantalla en landscape.

**Estados de descarga**:

Cuando el usuario toca "Actualizar ahora", el cuerpo se reemplaza
por una vista de progreso:

```
╭─────────────────────────────────────────╮
│ ◷  Descargando 1.0.1                    │
├─────────────────────────────────────────┤
│                                         │
│        [▰▰▰▰▰▰▰▰▱▱▱▱▱▱]                 │   ← LinearProgressIndicator
│        62%  ·  8.7 MB de 14 MB          │
│                                         │
├─────────────────────────────────────────┤
│  [Cancelar]                             │
╰─────────────────────────────────────────╯
```

Mientras descarga:

- LinearProgressIndicator con `progress` real, color Accent.
- Texto "{ratio}% · {downloaded} MB de {total} MB" en mono.
- Botón "Cancelar" (cancela el job y vuelve a la pantalla
  inicial).

Si falla:

```
╭─────────────────────────────────────────╮
│ ⚠ No se pudo descargar                  │
├─────────────────────────────────────────┤
│                                         │
│ Error de conexión: {detalle}            │
│                                         │
├─────────────────────────────────────────┤
│  [Reintentar]   [Cerrar]                │
╰─────────────────────────────────────────╯
```

Cuando llega a `Ready`: el sistema lanza el instalador automáticamente
y el diálogo se cierra (el resto lo maneja Android nativo).

**Botones**:

- "Actualizar ahora": `FilledTonalButton` con
  `containerColor = Accent`, `contentColor = White`,
  `RoundedCornerShape(14.dp)`, height 48dp, fontWeight SemiBold.
- "Más tarde": `TextButton` discreto, color `OnSurfaceMuted`, sin
  fondo. Tap → llama `updateManager.postpone(release.tagName)` y
  cierra. SOLO visible si la release no es crítica.
- "Cancelar" (durante descarga): `OutlinedButton` con border
  `OnSurfaceMuted`.
- "Reintentar" (en error): igual que "Actualizar ahora".

**Para releases críticas**:

- Sin botón [✕] arriba.
- Sin botón "Más tarde".
- Sin `dismissOnBackPress`.
- El header muestra el chip "Crítica" en `WarnFill`.
- Texto extra en el cuerpo: "Esta actualización corrige un problema
  importante. Te recomendamos instalarla ahora."

### 4. Ajustes de tamaño de pantalla

- `widthIn(max = 480.dp)` en pantallas Compact, `widthIn(max = 560.dp)`
  en Medium/Expanded.
- `heightIn(max = (screenHeightDp * 0.85f).dp)`.
- Header con altura fija pequeña (`72.dp` aprox).
- Cuerpo con `weight(1f, fill = false)` + scroll.
- Botones fuera del scroll.

### 5. Integración

- `AppContainer` expone `updateManager: UpdateManager`.
- El check al boot se hace en `MainActivity` con un
  `LaunchedEffect(Unit)` que llama
  `updateManager.checkForUpdate(force = false)` y, según el
  resultado, muestra el diálogo.
- Agregar opción "Buscar actualizaciones" en `SettingsScreen` que
  dispara `force = true` y muestra Snackbar con el resultado:
  "Estás al día" / "Hay una nueva versión disponible".

### 6. Helpers nuevos

```kotlin
fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
    }
}

fun formatRelativeTime(epochMillis: Long): String {
    // "hace 5 min" / "hace 2 horas" / "hace 3 días" / "hoy" / "ayer"
}
```

Si ya existen helpers de `formatRelativeTime` en el proyecto,
reusarlos.

## Tests mentales antes de declarar terminado

1. Abrir la app sin internet → no debe colgar ni mostrar errors;
   simplemente no aparece el diálogo.
2. Internet con datos móviles + release de 30 MB → debe avisar
   antes de descargar.
3. Tocar "Actualizar ahora" y desconectar Wi-Fi a la mitad → debe
   reintentar 3 veces, luego mostrar error con botón Reintentar.
4. Tocar "Más tarde" → no debe volver a aparecer hasta que haya una
   versión más nueva.
5. Release marcada `<!-- critical -->` → no debe poder cerrarse
   con back ni con botón ✕ ni "Más tarde".
6. Pantalla pequeña (320 dp width) + release notes largas → todo
   debe scrollear bien sin que los botones queden cortados.
7. Pantalla grande (tablet) → diálogo centrado con `widthIn(max =
   560.dp)`, no full-width.
8. Cambiar la fecha del sistema +1 hora después del último check →
   sin `force=true` no debe hacer nuevo request (cache).
9. Tocar "Buscar actualizaciones" en Settings con la última
   versión instalada → Snackbar "Estás al día".

## Estilo de trabajo

- Comentarios en español, "por qué" no "qué".
- Textos visibles en español rioplatense neutro ("tocá", "andá",
  "está al día").
- Solo `OreoPalette` para colores.
- No agregues dependencias (Ktor ya está si lo usaste para AI; si
  no, podés usar el `HttpURLConnection` actual mejorado).
- No toques otros features (editor, find&replace, export PDF, AI).

## Entregables

1. Plan de archivos.
2. `data/update/UpdateManager.kt` nuevo.
3. `data/update/SemVer.kt` nuevo.
4. `ui/components/UpdateDialog.kt` rediseñado.
5. `ui/components/MarkdownText.kt` (parser básico inline).
6. `data/UserPreferences.kt` extendido con prefs de update.
7. `data/AppContainer.kt` con `updateManager`.
8. `MainActivity.kt` u `OreoApp.kt` con el check al boot.
9. `SettingsScreen.kt` con la opción "Buscar actualizaciones".
10. `UpdateChecker.kt` viejo: borrarlo.

Cuando termines, mostrame un resumen de líneas (no diffs):
"Archivo X: agregué método Y, removí Z, refactoricé W".
