package com.smartcardscanner.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.smartcardscanner.data.local.db.entity.PersonnelEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonnelDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(personnel: List<PersonnelEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(personnel: PersonnelEntity): Long

    @Query("SELECT * FROM personnel WHERE ssn = :ssn LIMIT 1")
    suspend fun findBySsn(ssn: String): PersonnelEntity?

    @Query("SELECT * FROM personnel WHERE normalized_name = :normalizedName LIMIT 5")
    suspend fun findByExactName(normalizedName: String): List<PersonnelEntity>

    @Query("SELECT * FROM personnel WHERE normalized_name LIKE '%' || :query || '%' LIMIT 20")
    suspend fun searchByName(query: String): List<PersonnelEntity>

    @Query("SELECT * FROM personnel WHERE name_tokens LIKE '%' || :token || '%' LIMIT 50")
    suspend fun searchByToken(token: String): List<PersonnelEntity>

    @Query("SELECT * FROM personnel WHERE military_number = :militaryNumber LIMIT 1")
    suspend fun findByMilitaryNumber(militaryNumber: String): PersonnelEntity?

    @Query("SELECT COUNT(*) FROM personnel")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM personnel")
    fun getCountFlow(): Flow<Int>

    @Query("DELETE FROM personnel")
    suspend fun deleteAll()

    @Query("SELECT * FROM personnel LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<PersonnelEntity>

    @Query("SELECT * FROM personnel")
    suspend fun getAll(): List<PersonnelEntity>
}
