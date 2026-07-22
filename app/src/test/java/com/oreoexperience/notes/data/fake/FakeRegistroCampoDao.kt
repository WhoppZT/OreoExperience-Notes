package com.oreoexperience.notes.data.fake

import com.oreoexperience.notes.data.RegistroCampo
import com.oreoexperience.notes.data.RegistroCampoDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeRegistroCampoDao : RegistroCampoDao {

    private val _store = MutableStateFlow(emptyList<RegistroCampo>())
    private var nextId = 1L

    override fun observeAll(): Flow<List<RegistroCampo>> =
        _store.map { list -> list.sortedByDescending { it.dateMillis } }

    override fun observeRange(startMillis: Long, endMillis: Long): Flow<List<RegistroCampo>> =
        _store.map { list ->
            list.filter { it.dateMillis >= startMillis && it.dateMillis < endMillis }
                .sortedBy { it.dateMillis }
        }

    override suspend fun getByDay(dayMillis: Long): RegistroCampo? =
        _store.value.firstOrNull { it.dateMillis == dayMillis }

    override suspend fun insert(r: RegistroCampo): Long {
        val id = nextId++
        val inserted = r.copy(id = id)
        _store.value = _store.value + inserted
        return id
    }

    override suspend fun update(r: RegistroCampo) {
        _store.value = _store.value.map { if (it.id == r.id) r else it }
    }

    override suspend fun delete(r: RegistroCampo) {
        _store.value = _store.value.filter { it.id != r.id }
    }

    override suspend fun deleteAll() {
        _store.value = emptyList()
    }

    /** Insert directly into the store for test setup (bypasses id generation). */
    fun insertDirect(record: RegistroCampo) {
        val withId = if (record.id == 0L) record.copy(id = nextId++) else record
        _store.value = _store.value + withId
    }

    /** Insert multiple records at once for test setup. */
    fun insertAll(records: List<RegistroCampo>) {
        records.forEach { insertDirect(it) }
    }
}
