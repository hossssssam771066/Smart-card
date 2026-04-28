package com.smartcardscanner.data.repository

import com.smartcardscanner.data.local.db.dao.PersonnelDao
import com.smartcardscanner.data.local.db.entity.PersonnelEntity
import com.smartcardscanner.domain.matching.ArabicNameNormalizer
import com.smartcardscanner.domain.model.Personnel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PersonnelRepository @Inject constructor(
    private val personnelDao: PersonnelDao,
    private val normalizer: ArabicNameNormalizer
) {

    fun getCountFlow(): Flow<Int> = personnelDao.getCountFlow()

    suspend fun getCount(): Int = personnelDao.getCount()

    suspend fun insertBatch(personnelList: List<Personnel>) {
        val entities = personnelList.map { it.toEntity() }
        personnelDao.insertAll(entities)
    }

    suspend fun findBySsn(ssn: String): Personnel? {
        return personnelDao.findBySsn(ssn)?.toDomain()
    }

    suspend fun findByExactName(name: String): List<Personnel> {
        val normalized = normalizer.normalize(name)
        return personnelDao.findByExactName(normalized).map { it.toDomain() }
    }

    suspend fun searchByName(query: String): List<Personnel> {
        val normalized = normalizer.normalize(query)
        val results = mutableSetOf<PersonnelEntity>()

        // Search by normalized name
        results.addAll(personnelDao.searchByName(normalized))

        // Search by individual tokens
        val tokens = normalizer.tokenize(normalized)
        for (token in tokens) {
            if (token.length >= 2) {
                results.addAll(personnelDao.searchByToken(token))
            }
        }

        return results.map { it.toDomain() }
    }

    suspend fun findByMilitaryNumber(militaryNumber: String): Personnel? {
        return personnelDao.findByMilitaryNumber(militaryNumber)?.toDomain()
    }

    suspend fun deleteAll() {
        personnelDao.deleteAll()
    }

    suspend fun getAll(): List<Personnel> {
        return personnelDao.getAll().map { it.toDomain() }
    }

    private fun Personnel.toEntity(): PersonnelEntity {
        val normalized = normalizer.normalize(name)
        val tokens = normalizer.tokenize(normalized).joinToString(",")
        return PersonnelEntity(
            id = id,
            serialNumber = serialNumber,
            ssn = ssn,
            militaryNumber = militaryNumber,
            rank = rank,
            name = name,
            normalizedName = normalized,
            nameTokens = tokens,
            mainUnit = mainUnit,
            subUnit = subUnit,
            militaryCard = militaryCard
        )
    }

    private fun PersonnelEntity.toDomain(): Personnel {
        return Personnel(
            id = id,
            serialNumber = serialNumber,
            ssn = ssn,
            militaryNumber = militaryNumber,
            rank = rank,
            name = name,
            normalizedName = normalizedName,
            mainUnit = mainUnit,
            subUnit = subUnit,
            militaryCard = militaryCard
        )
    }
}
