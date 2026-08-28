package com.wangxq.consumable.data

import kotlinx.coroutines.flow.Flow

class ItemRepository(private val dao: ItemDao) {

    val items: Flow<List<ItemWithReplacements>> = dao.observeItems()
    val locations: Flow<List<String>> = dao.observeLocations()

    fun itemById(id: Long): Flow<ItemWithReplacements?> = dao.observeItem(id)

    suspend fun allItems(): List<ItemWithReplacements> = dao.allItems()

    suspend fun insertItemWithFirst(item: ItemEntity, firstReplacementEpoch: Long): Long {
        val id = dao.insertItem(item)
        dao.insertReplacement(ReplacementEntity(itemId = id, dateEpoch = firstReplacementEpoch))
        return id
    }

    suspend fun insertReplacement(itemId: Long, epoch: Long) =
        dao.insertReplacement(ReplacementEntity(itemId = itemId, dateEpoch = epoch))

    suspend fun deleteReplacement(r: ReplacementEntity) = dao.deleteReplacement(r)

    suspend fun updateReplacement(r: ReplacementEntity) = dao.updateReplacement(r)

    suspend fun deleteItem(item: ItemEntity) = dao.deleteItem(item)

    suspend fun updateItem(item: ItemEntity) = dao.updateItem(item)
}
