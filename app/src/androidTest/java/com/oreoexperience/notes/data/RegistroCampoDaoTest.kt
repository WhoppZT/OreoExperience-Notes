package com.oreoexperience.notes.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Calendar

@RunWith(AndroidJUnit4::class)
class RegistroCampoDaoTest {

    private lateinit var db: com.oreoexperience.notes.data.AppDatabase
    private lateinit var dao: RegistroCampoDao

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, com.oreoexperience.notes.data.AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.registroCampoDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `insert and retrieve by day`() = runTest {
        val day = dayMillis(2025, Calendar.MARCH, 10)
        dao.insert(RegistroCampo(dateMillis = day, hours = 2.5))
        val result = dao.getByDay(day)
        assertNotNull(result)
        assertEquals(2.5, result!!.hours, 0.001)
    }

    @Test
    fun `insert replace on conflict same day`() = runTest {
        val day = dayMillis(2025, Calendar.MARCH, 10)
        dao.insert(RegistroCampo(dateMillis = day, hours = 1.0, revisits = 1))
        dao.insert(RegistroCampo(dateMillis = day, hours = 3.0, revisits = 5))
        val result = dao.getByDay(day)
        assertNotNull(result)
        assertEquals(3.0, result!!.hours, 0.001)
        assertEquals(5, result.revisits)
    }

    @Test
    fun `observeRange filters correctly`() = runTest {
        val jan1 = dayMillis(2025, Calendar.JANUARY, 1)
        val jan15 = dayMillis(2025, Calendar.JANUARY, 15)
        val feb1 = dayMillis(2025, Calendar.FEBRUARY, 1)

        dao.insert(RegistroCampo(dateMillis = jan1, hours = 1.0))
        dao.insert(RegistroCampo(dateMillis = jan15, hours = 2.0))
        dao.insert(RegistroCampo(dateMillis = feb1, hours = 3.0))

        val febStart = dayMillis(2025, Calendar.FEBRUARY, 1)
        val febEnd = dayMillis(2025, Calendar.MARCH, 1)
        val febRecords = dao.observeRange(febStart, febEnd).first()

        assertEquals(1, febRecords.size)
        assertEquals(3.0, febRecords[0].hours, 0.001)
    }

    @Test
    fun `observeAll ordered by date descending`() = runTest {
        dao.insert(RegistroCampo(dateMillis = dayMillis(2025, Calendar.JANUARY, 1), hours = 1.0))
        dao.insert(RegistroCampo(dateMillis = dayMillis(2025, Calendar.MARCH, 1), hours = 3.0))
        dao.insert(RegistroCampo(dateMillis = dayMillis(2025, Calendar.FEBRUARY, 1), hours = 2.0))

        val all = dao.observeAll().first()
        assertEquals(3, all.size)
        assertTrue(all[0].dateMillis >= all[1].dateMillis)
        assertTrue(all[1].dateMillis >= all[2].dateMillis)
    }

    @Test
    fun `delete removes record`() = runTest {
        val day = dayMillis(2025, Calendar.MARCH, 10)
        val r = RegistroCampo(dateMillis = day, hours = 1.0)
        dao.insert(r)
        val inserted = dao.getByDay(day)!!
        dao.delete(inserted)
        assertNull(dao.getByDay(day))
    }

    @Test
    fun `deleteAll clears table`() = runTest {
        dao.insert(RegistroCampo(dateMillis = dayMillis(2025, Calendar.JANUARY, 1), hours = 1.0))
        dao.insert(RegistroCampo(dateMillis = dayMillis(2025, Calendar.FEBRUARY, 1), hours = 2.0))
        dao.deleteAll()
        assertTrue(dao.observeAll().first().isEmpty())
    }

    private fun dayMillis(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            clear()
            set(year, month, day, 0, 0, 0)
        }.timeInMillis
    }
}
