package com.emoji.overlay.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.emoji.overlay.data.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for category operations.
 */
@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun update(category: CategoryEntity)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteById(categoryId: Long)

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getById(categoryId: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE parent_id IS NULL AND is_visible = 1 ORDER BY sort_order, name")
    fun getRootCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE parent_id = :parentId AND is_visible = 1 ORDER BY sort_order, name")
    fun getSubCategoriesFlow(parentId: Long): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_visible = 1 ORDER BY sort_order, name")
    fun getAllFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE is_visible = 1 ORDER BY sort_order, name")
    suspend fun getAll(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): CategoryEntity?

    @Query("UPDATE categories SET emoji_count = (SELECT COUNT(*) FROM emojis WHERE category_id = :categoryId AND is_deleted = 0) WHERE id = :categoryId")
    suspend fun updateEmojiCount(categoryId: Long)

    @Query("UPDATE categories SET emoji_count = (SELECT COUNT(*) FROM emojis WHERE category_id = categories.id AND is_deleted = 0)")
    suspend fun updateAllEmojiCounts()

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun getCount(): Int
}
