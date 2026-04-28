package com.smartcardscanner.domain.model

data class ScanRecord(
    val id: Long = 0,
    val scanDate: Long = System.currentTimeMillis(),
    val nameFromBack: String = "",
    val nameFromNfc: String = "",
    val nameFromDatabase: String = "",
    val ssn: String = "",
    val nationalId: String = "",
    val militaryNumber: String = "",
    val rank: String = "",
    val mainUnit: String = "",
    val subUnit: String = "",
    val militaryCard: String = "",
    val matchScore: Double = 0.0,
    val matchType: String = "",
    val nfcStatus: String = "",
    val backScanStatus: String = "",
    val nfcUid: String = "",
    val documentNumber: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val nationality: String = "",
    val notes: String = "",
    val facePhotoPath: String = ""
)
