package com.oreoexperience.notes.data

import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class RegistroCampoTest {

    @Test
    fun `default values are zero or empty`() {
        val r = RegistroCampo(dateMillis = 1000L)
        assertEquals(0.0, r.hours, 0.001)
        assertEquals(0, r.revisits)
        assertEquals(0, r.publications)
        assertEquals(0, r.videos)
        assertEquals(0, r.studies)
        assertEquals("", r.notes)
        assertEquals(0L, r.id)
    }

    @Test
    fun `copy preserves other fields`() {
        val original = RegistroCampo(
            id = 1L,
            dateMillis = 5000L,
            hours = 2.5,
            revisits = 3,
            publications = 1,
            videos = 2,
            studies = 1,
            notes = "test note",
            createdAt = 100L,
            updatedAt = 200L,
        )
        val copied = original.copy(hours = 4.0)
        assertEquals(4.0, copied.hours, 0.001)
        assertEquals(original.id, copied.id)
        assertEquals(original.dateMillis, copied.dateMillis)
        assertEquals(original.revisits, copied.revisits)
        assertEquals(original.notes, copied.notes)
        assertEquals(original.createdAt, copied.createdAt)
    }

    @Test
    fun `equality by field values`() {
        val a = RegistroCampo(id = 1, dateMillis = 100L, hours = 1.5, revisits = 2)
        val b = RegistroCampo(id = 1, dateMillis = 100L, hours = 1.5, revisits = 2)
        assertEquals(a, b)
    }

    @Test
    fun `inequality by different id`() {
        val a = RegistroCampo(id = 1, dateMillis = 100L, hours = 1.5)
        val b = RegistroCampo(id = 2, dateMillis = 100L, hours = 1.5)
        assertNotEquals(a, b)
    }

    @Test
    fun `serialization roundtrip preserves data`() {
        val original = RegistroCampo(
            id = 5,
            dateMillis = 9999L,
            hours = 3.0,
            revisits = 4,
            publications = 2,
            videos = 1,
            studies = 3,
            notes = "hola mundo",
        )
        val json = Json.encodeToString(RegistroCampo.serializer(), original)
        val decoded = Json.decodeFromString(RegistroCampo.serializer(), json)
        assertEquals(original, decoded)
    }
}
