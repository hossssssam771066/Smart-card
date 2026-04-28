package com.smartcardscanner.domain.model

/**
 * Parsed MRZ data from TD1 (ID card) format - 3 lines of 30 characters each.
 *
 * Line 1: IDYEM00707523<6YY4UQ4OYT<<<<
 *   - ID = document type
 *   - YEM = issuing country
 *   - 00707523 = document number
 *   - optional data follows
 *
 * Line 2: 9001011M3102178YEM<<<<<<<<<<3
 *   - 900101 = DOB (YYMMDD)
 *   - 1 = DOB check digit
 *   - M = sex
 *   - 310217 = expiry (YYMMDD)
 *   - 8 = expiry check digit
 *   - YEM = nationality
 *   - optional data
 *   - 3 = overall check digit
 *
 * Line 3: HUSAM<AHMED<ALI<SAIF<<<<<<<<<<
 *   - Name: SURNAME<<GIVEN NAMES (separated by <)
 */
data class MrzData(
    val documentType: String = "",
    val issuingCountry: String = "",
    val documentNumber: String = "",
    val documentNumberCheckDigit: String = "",
    val dateOfBirth: String = "",
    val dobCheckDigit: String = "",
    val sex: String = "",
    val dateOfExpiry: String = "",
    val expiryCheckDigit: String = "",
    val nationality: String = "",
    val optionalData1: String = "",
    val optionalData2: String = "",
    val overallCheckDigit: String = "",
    val primaryIdentifier: String = "",
    val secondaryIdentifier: String = "",
    val fullNameEnglish: String = "",
    val rawMrz: String = "",
    val isValid: Boolean = false
) {
    val bacKey: BacKey
        get() = BacKey(
            documentNumber = documentNumber,
            dateOfBirth = dateOfBirth,
            dateOfExpiry = dateOfExpiry
        )
}

data class BacKey(
    val documentNumber: String,
    val dateOfBirth: String,
    val dateOfExpiry: String
)
