package com.wangxq.consumable.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/** 备件库存：与易耗品更换记录完全独立的模块 */
@Entity(tableName = "spare_parts")
data class SparePartEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String,            // 地址（可自由输入）
    @ColumnInfo(name = "storage_location")
    val storageLocation: String = "", // 存放位置（如：客厅储物柜第2格）
    val quantity: Int,
    val note: String = ""
)

@Dao
interface SparePartDao {
    @Query("SELECT * FROM spare_parts ORDER BY name ASC")
    fun observeAll(): Flow<List<SparePartEntity>>

    @Query("SELECT * FROM spare_parts WHERE id = :id")
    fun observeById(id: Long): Flow<SparePartEntity?>

    @Insert
    suspend fun insert(part: SparePartEntity): Long

    @Update
    suspend fun update(part: SparePartEntity)

    @Delete
    suspend fun delete(part: SparePartEntity)

    @Query("SELECT DISTINCT location FROM spare_parts ORDER BY location ASC")
    fun observeLocations(): Flow<List<String>>
}
