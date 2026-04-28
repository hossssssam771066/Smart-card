package com.smartcardscanner.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartcardscanner.data.local.db.dao.PersonnelDao
import com.smartcardscanner.data.local.db.dao.ScanRecordDao
import com.smartcardscanner.data.local.db.entity.PersonnelEntity
import com.smartcardscanner.data.local.db.entity.ScanRecordEntity

@Database(
    entities = [PersonnelEntity::class, ScanRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personnelDao(): PersonnelDao
    abstract fun scanRecordDao(): ScanRecordDao
}
