package com.oreoexperience.notes.data

import com.oreoexperience.notes.data.fake.FakeRegistroCampoDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class RegistroCampoRepositoryTest {

    private lateinit var dao: FakeRegistroCampoDao
    private lateinit var repo: RegistroCampoRepository

    @Before
    fun setup() {
        dao = FakeRegistroCampoDao()
        repo = RegistroCampoRepository(dao)
    }

    @Test
    fun `observeAll returns flow`() = runTest {
        val flow = repo.observeAll()
        assertNotNull(flow)
        assertTrue(flow.first().isEmpty())
    }

    @Test
    fun `observeMonth returns flow`() = runTest {
        val flow = repo.observeMonth(2025, 0)
        assertNotNull(flow)
        assertTrue(flow.first().isEmpty())
    }

    @Test
    fun `upsert inserts new record`() = runTest {
        val r = RegistroCampo(dateMillis = dayMillis(2025, 0, 15), hours = 2.0)
        repo.upsert(r)
        val result = repo.getByDay(dayMillis(2025, 0, 15))
        assertNotNull(result)
        assertEquals(2.0, result!!.hours, 0.001)
    }

    @Test
    fun `upsert updates existing record same day`() = runTest {
        val day = dayMillis(2025, 0, 15)
        repo.upsert(RegistroCampo(dateMillis = day, hours = 1.0, revisits = 1))
        repo.upsert(RegistroCampo(dateMillis = day, hours = 3.0, revisits = 5))
        val result = repo.getByDay(day)
        assertNotNull(result)
        assertEquals(3.0, result!!.hours, 0.001)
        assertEquals(5, result.revisits)
    }

    @Test
    fun `upsert preserves createdAt`() = runTest {
        val day = dayMillis(2025, 0, 15)
        repo.upsert(RegistroCampo(dateMillis = day, hours = 1.0))
        val first = repo.getByDay(day)!!
        val originalCreatedAt = first.createdAt

        Thread.sleep(10)
        repo.upsert(RegistroCampo(dateMillis = day, hours = 2.0))
        val second = repo.getByDay(day)!!
        assertEquals(originalCreatedAt, second.createdAt)
    }

    @Test
    fun `upsert updates updatedAt`() = runTest {
        val day = dayMillis(2025, 0, 15)
        repo.upsert(RegistroCampo(dateMillis = day, hours = 1.0))
        val first = repo.getByDay(day)!!

        Thread.sleep(10)
        repo.upsert(RegistroCampo(dateMillis = day, hours = 2.0))
        val second = repo.getByDay(day)!!
        assertTrue(second.updatedAt >= first.updatedAt)
    }

    @Test
    fun `delete removes record`() = runTest {
        val day = dayMillis(2025, 0, 15)
        val r = RegistroCampo(dateMillis = day, hours = 1.0)
        repo.upsert(r)
        val inserted = repo.getByDay(day)!!
        repo.delete(inserted)
        assertNull(repo.getByDay(day))
    }

    @Test
    fun `deleteAll removes all records`() = runTest {
        repo.upsert(RegistroCampo(dateMillis = dayMillis(2025, 0, 1), hours = 1.0))
        repo.upsert(RegistroCampo(dateMillis = dayMillis(2025, 0, 2), hours = 2.0))
        repo.deleteAll()
        assertTrue(repo.observeAll().first().isEmpty())
    }

    @Test
    fun `getByDay returns null for empty`() = runTest {
        assertNull(repo.getByDay(dayMillis(2025, 0, 15)))
    }

    @Test
    fun `getByDay returns correct record`() = runTest {
        repo.upsert(RegistroCampo(dateMillis = dayMillis(2025, 0, 10), hours = 1.0))
        repo.upsert(RegistroCampo(dateMillis = dayMillis(2025, 0, 15), hours = 2.5))
        val result = repo.getByDay(dayMillis(2025, 0, 15))
        assertNotNull(result)
        assertEquals(2.5, result!!.hours, 0.001)
    }

    @Test
    fun `startOfDayMillis normalizes to midnight`() {
        val now = System.currentTimeMillis()
        val normalized = RegistroCampoRepository.startOfDayMillis(now)
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = normalized }
        assertEquals(0, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(java.util.Calendar.MINUTE))
        assertEquals(0, cal.get(java.util.Calendar.SECOND))
        assertEquals(0, cal.get(java.util.Calendar.MILLISECOND))
    }

    private fun dayMillis(year: Int, month: Int, day: Int): Long {
        return java.util.Calendar.getInstance().apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }.timeInMillis
    }
}
