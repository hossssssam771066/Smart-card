package com.smartcardscanner.data.repository

import com.smartcardscanner.data.local.db.dao.ScanRecordDao
import com.smartcardscanner.data.local.db.entity.ScanRecordEntity
import com.smartcardscanner.domain.model.ScanRecord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScanRepository @Inject constructor(
    private val scanRecordDao: ScanRecordDao
) {

    fun getAllRecords(): Flow<List<ScanRecord>> {
        return scanRecordDao.getAllRecords().map { list ->
            list.map { it.toDomain() }
        }
    }

    fun getCount(): Flow<Int> = scanRecordDao.getCount()

    suspend fun getCountSync(): Int = scanRecordDao.getCountSync()

    suspend fun insert(record: ScanRecord): Long {
        return scanRecordDao.insert(record.toEntity())
    }

    suspend fun getById(id: Long): ScanRecord? {
        return scanRecordDao.getById(id)?.toDomain()
    }

    suspend fun getAllRecordsList(): List<ScanRecord> {
        return scanRecordDao.getAllRecordsList().map { it.toDomain() }
    }

    fun searchRecords(query: String): Flow<List<ScanRecord>> {
        return scanRecordDao.searchRecords(query).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun deleteAll() {
        scanRecordDao.deleteAll()
    }

    private fun ScanRecord.toEntity(): ScanRecordEntity {
        return ScanRecordEntity(
            id = id,
            scanDate = scanDate,
            nameFromBack = nameFromBack,
            nameFromNfc = nameFromNfc,
            nameFromDatabase = nameFromDatabase,
            ssn = ssn,
            nationalId = nationalId,
            militaryNumber = militaryNumber,
            rank = rank,
            mainUnit = mainUnit,
            subUnit = subUnit,
            militaryCard = militaryCard,
            matchScore = matchScore,
            matchType = matchType,
            nfcStatus = nfcStatus,
            backScanStatus = backScanStatus,
            nfcUid = nfcUid,
            documentNumber = documentNumber,
            dateOfBirth = dateOfBirth,
            gender = gender,
            nationality = nationality,
            notes = notes,
            facePhotoPath = facePhotoPath
        )
    }

    private fun ScanRecordEntity.toDomain(): ScanRecord {
        return ScanRecord(
            id = id,
            scanDate = scanDate,
            nameFromBack = nameFromBack,
            nameFromNfc = nameFromNfc,
            nameFromDatabase = nameFromDatabase,
            ssn = ssn,
            nationalId = nationalId,
            militaryNumber = militaryNumber,
            rank = rank,
            mainUnit = mainUnit,
            subUnit = subUnit,
            militaryCard = militaryCard,
            matchScore = matchScore,
            matchType = matchType,
            nfcStatus = nfcStatus,
            backScanStatus = backScanStatus,
            nfcUid = nfcUid,
            documentNumber = documentNumber,
            dateOfBirth = dateOfBirth,
            gender = gender,
            nationality = nationality,
            notes = notes,
            facePhotoPath = facePhotoPath
        )
    }
}
