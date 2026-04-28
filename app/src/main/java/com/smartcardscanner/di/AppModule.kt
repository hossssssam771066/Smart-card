package com.smartcardscanner.di

import android.content.Context
import androidx.room.Room
import com.smartcardscanner.data.local.db.AppDatabase
import com.smartcardscanner.data.local.db.dao.PersonnelDao
import com.smartcardscanner.data.local.db.dao.ScanRecordDao
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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "smart_card_scanner.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun providePersonnelDao(database: AppDatabase): PersonnelDao {
        return database.personnelDao()
    }

    @Provides
    fun provideScanRecordDao(database: AppDatabase): ScanRecordDao {
        return database.scanRecordDao()
    }
}
