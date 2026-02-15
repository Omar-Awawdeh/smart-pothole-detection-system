package com.pothole.detection.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingUploadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(upload: PendingUpload)

    @Query("SELECT * FROM pending_uploads ORDER BY timestamp ASC")
    fun getAll(): Flow<List<PendingUpload>>

    @Query("SELECT * FROM pending_uploads WHERE id = :id")
    suspend fun getById(id: String): PendingUpload?

    @Delete
    suspend fun delete(upload: PendingUpload)

    @Update
    suspend fun update(upload: PendingUpload)

    @Query("SELECT COUNT(*) FROM pending_uploads")
    fun getCount(): Flow<Int>
}
