package com.pothole.detection.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [PendingUpload::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun pendingUploadDao(): PendingUploadDao
}
