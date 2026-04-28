package com.smartcardscanner.device.barcode

import com.smartcardscanner.domain.model.BarcodeData
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parses the structured payload encoded inside the PDF417 barcode on the back of
 * Yemeni biometric ID cards (eID).
 *
 * The payload is a newline-separated text with the following observed shape:
 *
 *   line 0..3   : English given / father / grandfather / family name (4 lines, latin)
 *   blank line
 *   line 5..8   : Arabic given / father / grandfather / family name (4 lines, RTL)
 *   line 9      : place of birth   (city, e.g. "ذي عنقب")
 *   line 10     : governorate      (e.g. "تعز")
 *   line 11     : district / sub-region
 *   line 12     : date of birth as YYYYMMDD (8 digits)
 *   line 13     : national id (xxxx-xxxx-xxxx)
 *   line 14     : mother's full name (Arabic)
 *   line 15     : digital signature (base64 / DER) — ignored for matching
 *
 * Real cards may differ slightly (extra blanks, trailing whitespace, missing
 * sections). The parser is therefore lenient: it treats the first contiguous
 * latin block as the english name, the first contiguous arabic block as the
 * arabic name, then assigns the remaining tokens by content (digits → DOB,
 * dashed digits → national id, base64-looking chars → signature, the rest
 * → place / governorate / district / mother).
 */
@Singleton
class BarcodePayloadParser @Inject constructor() {

    private val arabicCharRegex = Regex("[\\u0600-\\u06FF]")
    private val latinCharRegex = Regex("[A-Za-z]")
    private val dobRegex = Regex("^(19|20)\\d{6}$")
    private val nationalIdRegex = Regex("^\\d{4}-\\d{4}-\\d{4}$")
    private val base64Regex = Regex("^[A-Za-z0-9+/=]{40,}$")

    fun parse(rawValue: String): BarcodeData {
        if (rawValue.isBlank()) return BarcodeData(rawValue = rawValue, format = "PDF417")

        // Normalise newline variants and split, keeping blank lines so we can
        // detect block boundaries.
        val lines = rawValue
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .split("\n")
            .map { it.trim() }

        val englishParts = mutableListOf<String>()
        val arabicParts = mutableListOf<String>()
        val otherLines = mutableListOf<String>()

        var phase = Phase.ENGLISH

        for (line in lines) {
            when {
                line.isBlank() -> {
                    // A blank line advances the phase, but only forward.
                    phase = when (phase) {
                        Phase.ENGLISH -> Phase.ARABIC
                        Phase.ARABIC -> Phase.OTHER
                        Phase.OTHER -> Phase.OTHER
                    }
                }
                isLatin(line) && phase == Phase.ENGLISH -> englishParts.add(line)
                isArabic(line) && (phase == Phase.ENGLISH || phase == Phase.ARABIC) -> {
                    arabicParts.add(line)
                    if (phase == Phase.ENGLISH) phase = Phase.ARABIC
                }
                else -> {
                    // Stop accumulating arabic name once we see a non-name field
                    // (digits, dashes, place names mixed with numbers, etc.)
                    if (phase == Phase.ARABIC && containsDigit(line)) phase = Phase.OTHER
                    if (phase == Phase.ARABIC && arabicParts.size >= 4) phase = Phase.OTHER
                    if (phase == Phase.ARABIC && isArabic(line)) {
                        arabicParts.add(line)
                    } else {
                        otherLines.add(line)
                        phase = Phase.OTHER
                    }
                }
            }
        }

        // Take at most 4 name parts from each block — extras are pushed to other.
        if (arabicParts.size > 4) {
            otherLines.addAll(0, arabicParts.subList(4, arabicParts.size))
        }
        val englishName = englishParts.take(4).joinToString(" ")
        val arabicName = arabicParts.take(4).joinToString(" ")

        // Classify the remaining lines.
        var dateOfBirth = ""
        var nationalId = ""
        var signature = ""
        val placeBuckets = mutableListOf<String>()
        var motherName = ""

        for (line in otherLines.filter { it.isNotBlank() }) {
            when {
                dateOfBirth.isEmpty() && dobRegex.matches(line) -> dateOfBirth = line
                nationalId.isEmpty() && nationalIdRegex.matches(line) -> nationalId = line
                signature.isEmpty() && base64Regex.matches(line) -> signature = line
                isArabic(line) -> placeBuckets.add(line)
                else -> placeBuckets.add(line)
            }
        }

        // Heuristic: among the arabic place lines, the LAST one with 3+ tokens
        // tends to be the mother's full name. Everything before it is
        // place / governorate / district.
        val (placeLines, mother) = splitPlaceAndMother(placeBuckets)
        motherName = mother

        val placeOfBirth = placeLines.getOrNull(0).orEmpty()
        val governorate = placeLines.getOrNull(1).orEmpty()
        val district = placeLines.getOrNull(2).orEmpty()

        val additional = buildMap {
            if (placeOfBirth.isNotBlank()) put("placeOfBirth", placeOfBirth)
            if (governorate.isNotBlank()) put("governorate", governorate)
            if (district.isNotBlank()) put("district", district)
            if (motherName.isNotBlank()) put("motherName", motherName)
            if (signature.isNotBlank()) put("signature", signature)
        }

        return BarcodeData(
            rawValue = rawValue,
            format = "PDF417",
            nameArabic = arabicName,
            nameEnglish = englishName,
            nationalId = nationalId,
            dateOfBirth = dateOfBirth,
            additionalData = additional
        )
    }

    private fun splitPlaceAndMother(lines: List<String>): Pair<List<String>, String> {
        if (lines.isEmpty()) return emptyList<String>() to ""
        // The last line that looks like a full personal name (4 tokens of
        // letters, no digits) is the mother's name.
        for (i in lines.indices.reversed()) {
            val l = lines[i]
            if (!containsDigit(l) && l.split(Regex("\\s+")).size >= 3 && isArabic(l)) {
                return lines.subList(0, i) to l
            }
        }
        return lines to ""
    }

    private fun isLatin(line: String): Boolean =
        latinCharRegex.containsMatchIn(line) && !arabicCharRegex.containsMatchIn(line)

    private fun isArabic(line: String): Boolean =
        arabicCharRegex.containsMatchIn(line)

    private fun containsDigit(line: String): Boolean =
        line.any { it.isDigit() }

    private enum class Phase { ENGLISH, ARABIC, OTHER }
}
