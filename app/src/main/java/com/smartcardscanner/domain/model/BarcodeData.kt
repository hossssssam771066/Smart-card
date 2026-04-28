package com.smartcardscanner.domain.model

data class BarcodeData(
    val rawValue: String = "",
    val format: String = "",
    val nameArabic: String = "",
    val nameEnglish: String = "",
    val nationalId: String = "",
    val dateOfBirth: String = "",
    val additionalData: Map<String, String> = emptyMap()
)
