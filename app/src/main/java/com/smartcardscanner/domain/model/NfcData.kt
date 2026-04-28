package com.smartcardscanner.domain.model

import android.graphics.Bitmap

data class NfcData(
    val uid: String = "",
    val documentNumber: String = "",
    val fullName: String = "",
    val dateOfBirth: String = "",
    val dateOfExpiry: String = "",
    val nationality: String = "",
    val gender: String = "",
    val issuingState: String = "",
    val personalNumber: String = "",
    val facePhoto: Bitmap? = null,
    val isChipAuthenticated: Boolean = false,
    val chipInfo: String = "",
    val rawMrzFromChip: String = ""
)
