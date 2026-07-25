package com.example.core.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.core.data.model.NoteEntity
import kotlinx.coroutines.flow.Flow

/**
 * NoteDao.kt
 * 
 * ব্রাউজারে ইন্টিগ্রেটেড নোটস ডেটাবেস পরিচালনা করার জন্য DAO।
 */
@Dao
interface NoteDao {
    @Query("SELECT * FROM notes ORDER BY isPinned DESC, updatedDate DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isPinned = 1 ORDER BY updatedDate DESC")
    fun getPinnedNotes(): Flow<List<NoteEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM notes")
    suspend fun clearAllNotes()
}
