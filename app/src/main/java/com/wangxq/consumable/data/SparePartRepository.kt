package com.wangxq.consumable.data

import kotlinx.coroutines.flow.Flow

class SparePartRepository(private val dao: SparePartDao) {
    val parts: Flow<List<SparePartEntity>> = dao.observeAll()
    val locations: Flow<List<String>> = dao.observeLocations()

    fun byId(id: Long): Flow<SparePartEntity?> = dao.observeById(id)

    suspend fun insert(part: SparePartEntity): Long = dao.insert(part)
    suspend fun update(part: SparePartEntity) = dao.update(part)
    suspend fun delete(part: SparePartEntity) = dao.delete(part)
}
