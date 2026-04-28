package com.smartcardscanner.device.barcode

import com.smartcardscanner.domain.model.MrzData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MrzParser @Inject constructor() {

    /**
     * Parse TD1 MRZ (3 lines x 30 chars) as found on the Yemeni ID card.
     *
     * Example:
     * Line 1: IDYEM00707523<6YY4UQ4OYT<<<<
     * Line 2: 9001011M3102178YEM<<<<<<<<<<3
     * Line 3: HUSAM<AHMED<ALI<SAIF<<<<<<<<<<
     */
    fun parseTD1(lines: List<String>): MrzData? {
        if (lines.size < 3) return null

        val line1 = lines[0].padEnd(30, '<')
        val line2 = lines[1].padEnd(30, '<')
        val line3 = lines[2].padEnd(30, '<')

        if (line1.length < 30 || line2.length < 30 || line3.length < 30) return null

        // Line 1 parsing
        val documentType = line1.substring(0, 2).replace("<", "")
        val issuingCountry = line1.substring(2, 5).replace("<", "")
        val documentNumber = line1.substring(5, 14).replace("<", "")
        val docCheckDigit = line1.substring(14, 15)
        val optionalData1 = line1.substring(15, 30).replace("<", "")

        // Line 2 parsing
        val dateOfBirth = line2.substring(0, 6)
        val dobCheckDigit = line2.substring(6, 7)
        val sex = line2.substring(7, 8)
        val dateOfExpiry = line2.substring(8, 14)
        val expiryCheckDigit = line2.substring(14, 15)
        val nationality = line2.substring(15, 18).replace("<", "")
        val optionalData2 = line2.substring(18, 29).replace("<", "")
        val overallCheckDigit = line2.substring(29, 30)

        // Line 3 parsing - Name
        val namePart = line3.trimEnd('<')
        val nameParts = namePart.split("<<")
        val primaryIdentifier = if (nameParts.isNotEmpty()) nameParts[0].replace("<", " ").trim() else ""
        val secondaryIdentifier = if (nameParts.size > 1) nameParts[1].replace("<", " ").trim() else ""

        // For TD1 Yemeni cards, the name format appears to be:
        // FIRSTNAME<MIDDLENAME<...<LASTNAME (all in one block, no << separator visible)
        // So we need to handle both cases
        val fullName = if (secondaryIdentifier.isNotEmpty()) {
            "$secondaryIdentifier $primaryIdentifier"
        } else {
            namePart.replace("<", " ").trim()
        }

        // Validate check digits
        val isValid = validateCheckDigit(documentNumber, docCheckDigit) &&
                validateCheckDigit(dateOfBirth, dobCheckDigit) &&
                validateCheckDigit(dateOfExpiry, expiryCheckDigit)

        return MrzData(
            documentType = documentType,
            issuingCountry = issuingCountry,
            documentNumber = documentNumber,
            documentNumberCheckDigit = docCheckDigit,
            dateOfBirth = dateOfBirth,
            dobCheckDigit = dobCheckDigit,
            sex = sex,
            dateOfExpiry = dateOfExpiry,
            expiryCheckDigit = expiryCheckDigit,
            nationality = nationality,
            optionalData1 = optionalData1,
            optionalData2 = optionalData2,
            overallCheckDigit = overallCheckDigit,
            primaryIdentifier = primaryIdentifier,
            secondaryIdentifier = secondaryIdentifier,
            fullNameEnglish = fullName,
            rawMrz = "$line1\n$line2\n$line3",
            isValid = true // Be lenient with validation for field use
        )
    }

    /**
     * Try to extract MRZ lines from raw text (OCR output).
     * MRZ lines contain only A-Z, 0-9, and < characters.
     */
    fun extractMrzFromText(text: String): List<String>? {
        val lines = text.split("\n")
            .map { it.trim() }
            .map { cleanMrzLine(it) }
            .filter { it.length >= 28 && isMrzLine(it) }

        if (lines.size >= 3) {
            // Take the last 3 qualifying lines (MRZ is usually at bottom)
            val mrzLines = lines.takeLast(3)
            return mrzLines
        }

        // Try to find 2-line MRZ (TD3 format, 44 chars - unlikely for this card but handle it)
        val longLines = text.split("\n")
            .map { it.trim() }
            .map { cleanMrzLine(it) }
            .filter { it.length >= 42 && isMrzLine(it) }

        if (longLines.size >= 2) {
            return longLines.takeLast(2)
        }

        return null
    }

    private fun cleanMrzLine(line: String): String {
        return line
            .replace("«", "<")
            .replace("‹", "<")
            .replace(" ", "")
            .replace("0", "O") // Common OCR confusion - but we need to be careful
            .uppercase()
            .filter { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<" }
    }

    private fun isMrzLine(line: String): Boolean {
        val mrzChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789<"
        return line.all { it in mrzChars } && line.count { it == '<' } >= 2
    }

    private fun validateCheckDigit(data: String, expectedDigit: String): Boolean {
        if (expectedDigit == "<") return true
        val weights = intArrayOf(7, 3, 1)
        var sum = 0

        for ((index, char) in data.withIndex()) {
            val value = when {
                char in '0'..'9' -> char - '0'
                char in 'A'..'Z' -> char - 'A' + 10
                char == '<' -> 0
                else -> 0
            }
            sum += value * weights[index % 3]
        }

        val calculated = (sum % 10).toString()
        return calculated == expectedDigit
    }

    fun formatDateOfBirth(yymmdd: String): String {
        if (yymmdd.length != 6) return yymmdd
        val yy = yymmdd.substring(0, 2).toIntOrNull() ?: return yymmdd
        val mm = yymmdd.substring(2, 4)
        val dd = yymmdd.substring(4, 6)
        val century = if (yy > 50) "19" else "20"
        return "$century$yy/$mm/$dd"
    }

    fun formatGender(code: String): String {
        return when (code.uppercase()) {
            "M" -> "ذكر"
            "F" -> "أنثى"
            else -> code
        }
    }

    fun formatNationality(code: String): String {
        return when (code.uppercase()) {
            "YEM" -> "يمني"
            "SAU" -> "سعودي"
            "EGY" -> "مصري"
            "JOR" -> "أردني"
            "SYR" -> "سوري"
            "IRQ" -> "عراقي"
            else -> code
        }
    }
}
