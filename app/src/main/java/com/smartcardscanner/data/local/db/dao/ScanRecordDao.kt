package com.smartcardscanner.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartcardscanner.data.local.db.entity.ScanRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanRecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: ScanRecordEntity): Long

    @Query("SELECT * FROM scan_records ORDER BY scan_date DESC")
    fun getAllRecords(): Flow<List<ScanRecordEntity>>

    @Query("SELECT * FROM scan_records ORDER BY scan_date DESC")
    suspend fun getAllRecordsList(): List<ScanRecordEntity>

    @Query("SELECT * FROM scan_records WHERE id = :id")
    suspend fun getById(id: Long): ScanRecordEntity?

    @Query("SELECT COUNT(*) FROM scan_records")
    fun getCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM scan_records")
    suspend fun getCountSync(): Int

    @Query("SELECT * FROM scan_records WHERE name_from_back LIKE '%' || :query || '%' OR name_from_nfc LIKE '%' || :query || '%' OR name_from_database LIKE '%' || :query || '%' OR ssn LIKE '%' || :query || '%' ORDER BY scan_date DESC")
    fun searchRecords(query: String): Flow<List<ScanRecordEntity>>

    @Query("DELETE FROM scan_records")
    suspend fun deleteAll()

    @Query("DELETE FROM scan_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}
