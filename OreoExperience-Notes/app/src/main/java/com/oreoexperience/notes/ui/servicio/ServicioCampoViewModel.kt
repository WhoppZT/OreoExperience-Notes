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

data class MonthTotals(
    val totalHours: Double = 0.0,
    val totalRevisits: Int = 0,
    val totalPublications: Int = 0,
    val totalVideos: Int = 0,
    val totalStudies: Int = 0,
    val totalDays: Int = 0,
)

/** Datos de un mes para la gráfica de barras. */
data class MonthChartData(
    val year: Int,
    val month: Int,
    val totalHours: Double,
    val totalRevisits: Int,
    val totalStudies: Int,
) {
    val label: String
        get() {
            val cal = Calendar.getInstance().apply {
                clear()
                set(year, month, 1)
            }
            return java.text.SimpleDateFormat("MMM", java.util.Locale("es", "ES"))
                .format(cal.time)
                .replaceFirstChar { it.uppercase() }
        }
}

data class ServicioCampoUiState(
    val month: MonthRef = MonthRef.today(),
    val records: List<RegistroCampo> = emptyList(),
    val totals: MonthTotals = MonthTotals(),
    val selectedDayMillis: Long = RegistroCampoRepository.startOfDayMillis(System.currentTimeMillis()),
    val chartData: List<MonthChartData> = emptyList(),
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

    private val allRecords: StateFlow<List<RegistroCampo>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val state: StateFlow<ServicioCampoUiState> = combine(
        monthRecords,
        month,
        selectedDay,
        allRecords,
    ) { records, m, day, all ->
        val totals = MonthTotals(
            totalHours = records.sumOf { it.hours },
            totalRevisits = records.sumOf { it.revisits },
            totalPublications = records.sumOf { it.publications },
            totalVideos = records.sumOf { it.videos },
            totalStudies = records.sumOf { it.studies },
            totalDays = records.size,
        )
        val chartData = buildChartData(all)
        ServicioCampoUiState(
            month = m,
            records = records,
            totals = totals,
            selectedDayMillis = day,
            chartData = chartData,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ServicioCampoUiState())

    val monthState: StateFlow<MonthRef> = month.asStateFlow()
    val selectedDayState: StateFlow<Long> = selectedDay.asStateFlow()

    fun nextMonth() { month.value = month.value.next() }
    fun previousMonth() { month.value = month.value.previous() }
    fun goToCurrentMonth() {
        month.value = MonthRef.today()
        selectedDay.value = RegistroCampoRepository.startOfDayMillis(System.currentTimeMillis())
    }
    fun selectDay(dayMillis: Long) {
        selectedDay.value = RegistroCampoRepository.startOfDayMillis(dayMillis)
    }
    fun recordForDay(dayMillis: Long): RegistroCampo? {
        val normalized = RegistroCampoRepository.startOfDayMillis(dayMillis)
        return monthRecords.value.firstOrNull { it.dateMillis == normalized }
    }
    fun upsert(record: RegistroCampo) {
        viewModelScope.launch {
            val normalized = RegistroCampoRepository.startOfDayMillis(record.dateMillis)
            repository.upsert(record.copy(dateMillis = normalized))
        }
    }
    fun delete(record: RegistroCampo) {
        viewModelScope.launch { repository.delete(record) }
    }

    /** Agrupa todos los registros por mes y devuelve los últimos 12 meses con datos. */
    private fun buildChartData(all: List<RegistroCampo>): List<MonthChartData> {
        if (all.isEmpty()) return emptyList()
        val grouped = all.groupBy { rec ->
            val cal = Calendar.getInstance().apply { timeInMillis = rec.dateMillis }
            cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH)
        }
        return grouped.map { (key, records) ->
            val year = key / 100
            val monthIdx = key % 100
            MonthChartData(
                year = year,
                month = monthIdx,
                totalHours = records.sumOf { it.hours },
                totalRevisits = records.sumOf { it.revisits },
                totalStudies = records.sumOf { it.studies },
            )
        }
            .sortedBy { it.year * 100 + it.month }
            .takeLast(12)
    }
}
