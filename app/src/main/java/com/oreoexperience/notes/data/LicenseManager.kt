package com.oreoexperience.notes.data

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Sistema de licencias por clave.
 *
 * Hay un set fijo de claves válidas (ver [VALID_KEYS]). Cuando un
 * usuario desbloquea la app por primera vez con una clave válida, se
 * registra la clave + el timestamp de activación. La licencia dura
 * [LICENSE_DURATION_DAYS] días corridos.
 *
 * Una vez activada, *la misma* clave puede volver a desbloquear la app
 * en bloqueos sucesivos. Si vence, la app vuelve al estado bloqueado
 * y el manager devuelve [UnlockResult.LicenseExpired].
 *
 * El estado relevante (clave activa + fechas + días restantes) se
 * expone como [MutableState] para que la pantalla de Ajustes se
 * recomponga sin que tengamos que volver a leer prefs manualmente.
 */
class LicenseManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** Clave activa (o `null` si la app nunca se desbloqueó). */
    val activeKeyState: MutableState<String?> by lazy {
        mutableStateOf(prefs.getString(KEY_ACTIVE, null))
    }

    /** Timestamp de activación de la clave actual. 0L = sin clave. */
    val licenseStartState: MutableState<Long> by lazy {
        mutableStateOf(prefs.getLong(KEY_START, 0L))
    }

    val licenseStart: Long get() = licenseStartState.value
    val activeKey: String? get() = activeKeyState.value

    /** Fecha (ms) en la que la licencia caduca. 0 = sin clave. */
    val licenseEnd: Long
        get() {
            val start = licenseStart
            if (start == 0L) return 0L
            return start + LICENSE_DURATION_MILLIS
        }

    /** Días enteros restantes (puede ser negativo si ya venció). */
    val daysRemaining: Int
        get() {
            val end = licenseEnd
            if (end == 0L) return 0
            val diff = end - System.currentTimeMillis()
            return Math.floor(diff.toDouble() / DAY_MILLIS.toDouble()).toInt()
        }

    val isExpired: Boolean get() = activeKey != null && System.currentTimeMillis() >= licenseEnd
    val isActive: Boolean get() = activeKey != null && !isExpired

    /**
     * Intenta desbloquear con una clave dada.
     *
     * Reglas:
     * - Si la clave no está en [VALID_KEYS] → [UnlockResult.InvalidKey].
     * - Si nunca se activó ninguna → se activa esta y empieza el contador.
     * - Si ya hay clave activa y coincide → se acepta (vuelve a entrar).
     * - Si ya hay clave activa y el usuario ingresa otra distinta → se
     *   acepta también (re-activa con la nueva, reiniciando el contador).
     *   Esto permite "cambiar de licencia" sin reinstalar.
     * - Si la clave activa ya venció → [UnlockResult.LicenseExpired]
     *   (a menos que la clave ingresada sea distinta, en cuyo caso se
     *   re-activa).
     */
    fun tryUnlock(rawKey: String): UnlockResult {
        val clean = rawKey.trim().uppercase(Locale.ROOT)
        if (clean !in VALID_KEYS) return UnlockResult.InvalidKey

        val existing = activeKey
        val expired = isExpired

        if (existing == null) {
            // Primer uso: activar ahora.
            activate(clean)
            return UnlockResult.FirstActivation(daysRemaining = LICENSE_DURATION_DAYS)
        }

        if (existing == clean) {
            return if (expired) UnlockResult.LicenseExpired else UnlockResult.Unlocked(daysRemaining)
        }

        // Distinta clave válida: rotar.
        activate(clean)
        return UnlockResult.Rotated(daysRemaining = LICENSE_DURATION_DAYS)
    }

    private fun activate(clean: String) {
        val now = System.currentTimeMillis()
        activeKeyState.value = clean
        licenseStartState.value = now
        prefs.edit()
            .putString(KEY_ACTIVE, clean)
            .putLong(KEY_START, now)
            .apply()
    }

    /** Borra clave activa y fecha — la próxima vez tocará pedir clave nueva. */
    fun reset() {
        activeKeyState.value = null
        licenseStartState.value = 0L
        prefs.edit()
            .remove(KEY_ACTIVE)
            .remove(KEY_START)
            .apply()
    }

    /** Para diagnóstico / panel de Ajustes. */
    fun formatStart(): String =
        if (licenseStart == 0L) "—" else DATE_FORMAT.format(Date(licenseStart))

    fun formatEnd(): String =
        if (licenseEnd == 0L) "—" else DATE_FORMAT.format(Date(licenseEnd))

    /** Clave enmascarada para mostrar en la UI: `OREO-JW-2026-***1`. */
    fun maskedActiveKey(): String {
        val k = activeKey ?: return "—"
        if (k.length <= 4) return "***" + k.takeLast(1)
        val tail = k.takeLast(4)
        return k.dropLast(4).map { if (it.isLetterOrDigit()) '*' else it }
            .joinToString("") + tail
    }

    sealed class UnlockResult {
        data class FirstActivation(val daysRemaining: Int) : UnlockResult()
        data class Unlocked(val daysRemaining: Int) : UnlockResult()
        data class Rotated(val daysRemaining: Int) : UnlockResult()
        data object InvalidKey : UnlockResult()
        data object LicenseExpired : UnlockResult()
    }

    companion object {
        private const val PREF_NAME = "oreo_license_prefs"
        private const val KEY_ACTIVE = "license_active_key"
        private const val KEY_START = "license_start_millis"

        const val LICENSE_DURATION_DAYS = 365
        val LICENSE_DURATION_MILLIS: Long = TimeUnit.DAYS.toMillis(LICENSE_DURATION_DAYS.toLong())
        private val DAY_MILLIS: Long = TimeUnit.DAYS.toMillis(1)

        private val DATE_FORMAT: SimpleDateFormat
            get() = SimpleDateFormat("dd/MM/yyyy", Locale("es", "ES"))

        /**
         * Set fijo de claves válidas que se distribuyen a los usuarios.
         * Cambiarlas requiere recompilar.
         */
        val VALID_KEYS: Set<String> = setOf(
            "OREO-JW-2026-001",
            "OREO-JW-2026-002",
            "OREO-JW-2026-003",
            "OREO-JW-2026-004",
            "OREO-JW-2026-005",
        )
    }
}
