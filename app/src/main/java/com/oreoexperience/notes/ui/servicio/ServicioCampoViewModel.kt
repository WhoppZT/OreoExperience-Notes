package com.oreoexperience.notes.ui.servicio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.oreoexperience.notes.data.RegistroCampo
import com.oreoexperience.notes.data.RegistroCampoRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MonthRef(val year: Int, val month: Int) {
    /** Acuerdo: `month` es 0-indexado estilo java.util.Calendar.MONTH. */
    fun previous(): MonthRef {
        val c = Calendar.getInstance().apply {
            clear()
            set(year, month, 1)
            add(Calendar.MONTH, -1)
        }
        return MonthRef(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    }

    fun next(): MonthRef {
        val c = Calendar.getInstance().apply {
            clear()
            set(year, month, 1)
            add(Calendar.MONTH, 1)
        }
        return MonthRef(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
    }

    companion object {
        fun today(): MonthRef {
            val c = Calendar.getInstance()
            return MonthRef(c.get(Calendar.YEAR), c.get(Calendar.MONTH))
        }
    }
}

/**
 * Totales agregados de un mes — se calculan en el ViewModel para que
 * la UI sea declarativa.
 */
data class MonthTotals(
    val totalHours: Double = 0.0,
    val totalRevisits: Int = 0,
    val totalPublications: Int = 0,
    val totalVideos: Int = 0,
    val totalStudies: Int = 0,
    val totalDays: Int = 0,
)

data class ServicioCampoUiState(
    val month: MonthRef = MonthRef.today(),
    val records: List<RegistroCampo> = emptyList(),
    val totals: MonthTotals = MonthTotals(),
    val selectedDayMillis: Long = RegistroCampoRepository.startOfDayMillis(System.currentTimeMillis()),
)

@OptIn(ExperimentalCoroutinesApi::class)
class ServicioCampoViewModel(
    private val repository: RegistroCampoRepository,
) : ViewModel() {

    private val month = MutableStateFlow(MonthRef.today())
    private val selectedDay = MutableStateFlow(
        RegistroCampoRepository.startOfDayMillis(System.currentTimeMillis())
    )

    private val monthRecords: StateFlow<List<RegistroCampo>> = month
        .flatMapLatest { m -> repository.observeMonth(m.year, m.month) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<ServicioCampoUiState> = combine(
        monthRecords,
        month,
        selectedDay,
    ) { records, m, day ->
        val totals = MonthTotals(
            totalHours = records.sumOf { it.hours },
            totalRevisits = records.sumOf { it.revisits },
            totalPublications = records.sumOf { it.publications },
            totalVideos = records.sumOf { it.videos },
            totalStudies = records.sumOf { it.studies },
            totalDays = records.size,
        )
        ServicioCampoUiState(
            month = m,
            records = records,
            totals = totals,
            selectedDayMillis = day,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServicioCampoUiState())

    val monthState: StateFlow<MonthRef> = month.asStateFlow()
    val selectedDayState: StateFlow<Long> = selectedDay.asStateFlow()

    fun nextMonth() {
        month.value = month.value.next()
    }

    fun previousMonth() {
        month.value = month.value.previous()
    }

    fun goToCurrentMonth() {
        month.value = MonthRef.today()
        selectedDay.value = RegistroCampoRepository.startOfDayMillis(System.currentTimeMillis())
    }

    fun selectDay(dayMillis: Long) {
        selectedDay.value = RegistroCampoRepository.startOfDayMillis(dayMillis)
    }

    /**
     * Resuelve el registro del día seleccionado (puede no existir).
     */
    fun recordForDay(dayMillis: Long): RegistroCampo? {
        val normalized = RegistroCampoRepository.startOfDayMillis(dayMillis)
        return monthRecords.value.firstOrNull { it.dateMillis == normalized }
    }

    /** Guarda o actualiza el registro del día (upsert por día). */
    fun upsert(record: RegistroCampo) {
        viewModelScope.launch {
            val normalized = RegistroCampoRepository.startOfDayMillis(record.dateMillis)
            repository.upsert(record.copy(dateMillis = normalized))
        }
    }

    fun delete(record: RegistroCampo) {
        viewModelScope.launch { repository.delete(record) }
    }
}
