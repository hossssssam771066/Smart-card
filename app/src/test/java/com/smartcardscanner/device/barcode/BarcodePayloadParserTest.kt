package com.smartcardscanner.device.barcode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodePayloadParserTest {

    private val parser = BarcodePayloadParser()

    /** The exact payload shape the user reported reading from his card. */
    private val realCardPayload = """
        HUSAM
        AHMED
        ALI
        SAIF
        
        حسام
        احمد
        علي
        سيف
        
        ذي عنقب
        تعز
        مشرعه وحدنان
        19900101
        8964-1965-0481
        امريه حيدر احمد حسن
        MEUCICGpgJWa5qUTCE0aZfEhwXJfL04W0WLKbnpde/fggb2gAiEAguoZXlLTBzjJD7rNRBxdeoxdpP/grMRuC0rxdenMocs=
    """.trimIndent()

    @Test
    fun parses_real_card_payload_into_structured_fields() {
        val result = parser.parse(realCardPayload)

        assertEquals("HUSAM AHMED ALI SAIF", result.nameEnglish)
        assertEquals("حسام احمد علي سيف", result.nameArabic)
        assertEquals("19900101", result.dateOfBirth)
        assertEquals("8964-1965-0481", result.nationalId)
        assertEquals("ذي عنقب", result.additionalData["placeOfBirth"])
        assertEquals("تعز", result.additionalData["governorate"])
        assertEquals("مشرعه وحدنان", result.additionalData["district"])
        assertEquals("امريه حيدر احمد حسن", result.additionalData["motherName"])
        assertTrue(
            "signature should be captured",
            result.additionalData["signature"]?.startsWith("MEUCIC") == true
        )
    }

    @Test
    fun returns_blank_data_for_empty_input() {
        val result = parser.parse("")
        assertEquals("", result.nameEnglish)
        assertEquals("", result.nameArabic)
        assertEquals("", result.nationalId)
    }

    @Test
    fun handles_carriage_return_line_endings() {
        val crlf = realCardPayload.replace("\n", "\r\n")
        val result = parser.parse(crlf)
        assertEquals("حسام احمد علي سيف", result.nameArabic)
    }

    @Test
    fun handles_payload_without_signature() {
        val payload = realCardPayload.lines().dropLast(1).joinToString("\n")
        val result = parser.parse(payload)
        assertEquals("حسام احمد علي سيف", result.nameArabic)
        assertEquals("امريه حيدر احمد حسن", result.additionalData["motherName"])
    }

    @Test
    fun extracts_arabic_name_only_when_english_block_missing() {
        val payload = """
            
            حسام
            احمد
            علي
            سيف
        """.trimIndent()
        val result = parser.parse(payload)
        assertEquals("حسام احمد علي سيف", result.nameArabic)
        assertEquals("", result.nameEnglish)
    }
}
