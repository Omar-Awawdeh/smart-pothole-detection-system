package com.pothole.detection.di

import android.content.Context
import android.content.SharedPreferences
import androidx.room.Room
import com.pothole.detection.data.local.AppDatabase
import com.pothole.detection.data.local.PendingUploadDao
import com.pothole.detection.data.repository.PotholeRepository
import com.pothole.detection.deduplication.PotholeDeduplicator
import com.pothole.detection.detection.PotholeDetector
import com.pothole.detection.location.LocationProvider
import com.pothole.detection.network.ApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePotholeDetector(@ApplicationContext context: Context): PotholeDetector {
        return PotholeDetector(context)
    }

    @Provides
    @Singleton
    fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider {
        return LocationProvider(context)
    }

    @Provides
    @Singleton
    fun providePotholeDeduplicator(): PotholeDeduplicator {
        return PotholeDeduplicator()
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pothole_detection_db"
        ).build()
    }

    @Provides
    @Singleton
    fun providePendingUploadDao(database: AppDatabase): PendingUploadDao {
        return database.pendingUploadDao()
    }

    @Provides
    @Singleton
    fun provideApiService(sharedPreferences: SharedPreferences): ApiService {
        val baseUrl = sharedPreferences.getString("api_base_url", "https://api.yoursite.com")
            ?: "https://api.yoursite.com"
        return ApiService(baseUrl)
    }

    @Provides
    @Singleton
    fun providePotholeRepository(
        dao: PendingUploadDao,
        apiService: ApiService
    ): PotholeRepository {
        return PotholeRepository(dao, apiService)
    }

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("pothole_detection_prefs", Context.MODE_PRIVATE)
    }
}
