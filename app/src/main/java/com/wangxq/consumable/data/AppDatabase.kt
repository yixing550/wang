package com.wangxq.consumable.data

import androidx.room.*
import androidx.room.migration.Migration
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String,
    val category: String,
    val cycleValue: Int,
    val cycleUnit: String, // "月" or "年"
    val note: String = "",
    val calendarEventId: Long? = null, // 写入系统日历的事件 id，用于更新/删除
    val remindEnabled: Boolean = true // 是否写入系统日历日程提醒
)

@Entity(
    tableName = "replacements",
    foreignKeys = [ForeignKey(
        entity = ItemEntity::class,
        parentColumns = ["id"],
        childColumns = ["itemId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class ReplacementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val dateEpoch: Long // LocalDate.toEpochDay()
)

data class ItemWithReplacements(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val replacements: List<ReplacementEntity>
)

@Dao
interface ItemDao {
    @Transaction
    @Query("SELECT * FROM items ORDER BY name ASC")
    fun observeItems(): Flow<List<ItemWithReplacements>>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :id")
    fun observeItem(id: Long): Flow<ItemWithReplacements?>

    @Insert
    suspend fun insertItem(item: ItemEntity): Long

    @Update
    suspend fun updateItem(item: ItemEntity)

    @Delete
    suspend fun deleteItem(item: ItemEntity)

    @Insert
    suspend fun insertReplacement(r: ReplacementEntity)

    @Update
    suspend fun updateReplacement(r: ReplacementEntity)

    @Delete
    suspend fun deleteReplacement(r: ReplacementEntity)

    @Query("SELECT DISTINCT location FROM items ORDER BY location ASC")
    fun observeLocations(): Flow<List<String>>

    @Transaction
    @Query("SELECT * FROM items")
    suspend fun allItems(): List<ItemWithReplacements>
}

val MIGRATION_5_6 = Migration(5, 6) {
    it.execSQL("ALTER TABLE items ADD COLUMN remindEnabled INTEGER NOT NULL DEFAULT 1")
}

@Database(
    entities = [ItemEntity::class, ReplacementEntity::class, SparePartEntity::class],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun sparePartDao(): SparePartDao

    companion object {
        fun build(app: android.content.Context): AppDatabase =
            Room.databaseBuilder(app, AppDatabase::class.java, "consumable.db")
                .addMigrations(MIGRATION_5_6)
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
    }
}
