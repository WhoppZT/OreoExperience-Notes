package com.oreoexperience.notes.ui.servicio

import app.cash.turbine.test
import com.oreoexperience.notes.data.RegistroCampo
import com.oreoexperience.notes.data.RegistroCampoRepository
import com.oreoexperience.notes.data.fake.FakeRegistroCampoDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class ServicioCampoViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var dao: FakeRegistroCampoDao
    private lateinit var repo: RegistroCampoRepository
    private lateinit var vm: ServicioCampoViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dao = FakeRegistroCampoDao()
        repo = RegistroCampoRepository(dao)
        vm = ServicioCampoViewModel(repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun dayMillis(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }.timeInMillis
    }

    // ── Initial state (3 tests) ────────────────────────────────

    @Test
    fun `initial state is current month`() = runTest {
        vm.state.test {
            val state = awaitItem()
            val today = MonthRef.today()
            assertEquals(today.year, state.month.year)
            assertEquals(today.month, state.month.month)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial records is empty`() = runTest {
        vm.state.test {
            assertTrue(awaitItem().records.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial totals are zero`() = runTest {
        vm.state.test {
            val s = awaitItem()
            assertEquals(0.0, s.totals.totalHours, 0.001)
            assertEquals(0, s.totals.totalRevisits)
            assertEquals(0, s.totals.totalStudies)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Month navigation (3 tests) ─────────────────────────────

    @Test
    fun `nextMonth advances to next month`() = runTest {
        vm.state.test {
            val before = awaitItem().month
            vm.nextMonth()
            val after = expectMostRecentItem().month
            assertTrue(
                after.year > before.year ||
                    (after.year == before.year && after.month > before.month)
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `previousMonth goes back one month`() = runTest {
        vm.state.test {
            val before = awaitItem().month
            vm.previousMonth()
            val after = expectMostRecentItem().month
            assertTrue(
                after.year < before.year ||
                    (after.year == before.year && after.month < before.month)
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `goToCurrentMonth resets to today`() = runTest {
        vm.state.test {
            awaitItem()
            vm.previousMonth()
            vm.previousMonth()
            expectMostRecentItem()
            vm.goToCurrentMonth()
            assertEquals(MonthRef.today(), expectMostRecentItem().month)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── Day selection (3 tests) ─────────────────────────────────

    @Test
    fun `selectDay updates selectedDayMillis`() = runTest {
        vm.state.test {
            awaitItem()
            val target = dayMillis(2025, Calendar.JANUARY, 15)
            vm.selectDay(target)
            assertEquals(
                RegistroCampoRepository.startOfDayMillis(target),
                expectMostRecentItem().selectedDayMillis
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recordForDay returns null when empty`() = runTest {
        vm.state.test {
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
        assertNull(vm.recordForDay(dayMillis(2025, Calendar.JANUARY, 10)))
    }

    @Test
    fun `recordForDay returns record`() = runTest {
        vm.state.test {
            awaitItem()
            val day = dayMillis(2025, Calendar.MARCH, 10)
            vm.upsert(RegistroCampo(dateMillis = day, hours = 2.5))
            expectMostRecentItem()
            cancelAndIgnoreRemainingEvents()
        }
        val result = vm.recordForDay(dayMillis(2025, Calendar.MARCH, 10))
        assertNotNull(result)
        assertEquals(2.5, result!!.hours, 0.001)
    }

    // ── CRUD operations (4 tests) ───────────────────────────────

    @Test
    fun `upsert adds record to state`() = runTest {
        vm.state.test {
            awaitItem()
            vm.upsert(RegistroCampo(dateMillis = dayMillis(2025, Calendar.MARCH, 5), hours = 3.0))
            assertTrue(expectMostRecentItem().records.isNotEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `delete removes record from state`() = runTest {
        vm.state.test {
            awaitItem()
            vm.upsert(RegistroCampo(dateMillis = dayMillis(2025, Calendar.MARCH, 5), hours = 2.0))
            val afterUpsert = expectMostRecentItem()
            assertEquals(1, afterUpsert.records.size)

            val record = afterUpsert.records.first()
            vm.delete(record)
            assertTrue(expectMostRecentItem().records.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `totals update when records change`() = runTest {
        vm.state.test {
            awaitItem()
            vm.upsert(RegistroCampo(dateMillis = dayMillis(2025, Calendar.MARCH, 5), hours = 2.5, revisits = 3))
            val state = expectMostRecentItem()
            assertEquals(2.5, state.totals.totalHours, 0.001)
            assertEquals(3, state.totals.totalRevisits)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recordForDay normalizes input timestamp`() = runTest {
        vm.state.test {
            awaitItem()
            val midnight = dayMillis(2025, Calendar.MARCH, 10)
            vm.upsert(RegistroCampo(dateMillis = midnight, hours = 1.0))
            expectMostRecentItem()
            cancelAndIgnoreRemainingEvents()
        }
        val withHour = dayMillis(2025, Calendar.MARCH, 10) + 3600_000L * 14
        val result = vm.recordForDay(withHour)
        assertNotNull(result)
    }

    // ── Chart data (2 tests) ────────────────────────────────────

    @Test
    fun `chartData groups by month`() = runTest {
        vm.state.test {
            awaitItem()
            vm.upsert(RegistroCampo(dateMillis = dayMillis(2025, Calendar.JANUARY, 5), hours = 1.0))
            vm.upsert(RegistroCampo(dateMillis = dayMillis(2025, Calendar.JANUARY, 10), hours = 2.0))
            vm.upsert(RegistroCampo(dateMillis = dayMillis(2025, Calendar.FEBRUARY, 3), hours = 1.5))
            assertEquals(2, expectMostRecentItem().chartData.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `chartData takes last 12 months`() = runTest {
        vm.state.test {
            awaitItem()
            (0 until 15).forEach { i ->
                val cal = Calendar.getInstance().apply {
                    clear()
                    set(2023, i, 1)
                }
                vm.upsert(RegistroCampo(id = (i + 1).toLong(), dateMillis = cal.timeInMillis, hours = 1.0))
            }
            assertTrue(expectMostRecentItem().chartData.size <= 12)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
