package com.smartcardscanner.domain.matching

import com.smartcardscanner.domain.model.MatchType
import com.smartcardscanner.domain.model.Personnel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArabicNameMatcherTest {

    private val normalizer = ArabicNameNormalizer()
    private val matcher = ArabicNameMatcher(normalizer)

    private val db = listOf(
        person(1, "حسام أحمد علي سيف", ssn = "8964-1965-0481"),
        person(2, "حسام احمد علي سيف الحوثي"),
        person(3, "علي عبدالله صالح"),
        person(4, "محمد أحمد علي"),
        person(5, "حسام احمد علي صائب")
    )

    @Test
    fun exact_arabic_name_match_after_normalization() {
        val results = matcher.matchByName("حسام احمد علي سيف", db)
        assertTrue(results.isNotEmpty())
        val top = results.first()
        assertEquals("حسام أحمد علي سيف", top.personnel.name)
        assertEquals(MatchType.EXACT, top.matchType)
    }

    @Test
    fun handles_alef_and_hamza_variants() {
        // Hamza variants أ / إ / آ / ا should all be considered the same.
        val results = matcher.matchByName("حسام إحمد علي سيف", db)
        assertEquals("حسام أحمد علي سيف", results.first().personnel.name)
    }

    @Test
    fun fuzzy_match_when_one_letter_differs() {
        // "صائب" vs "سيف" — different last name but same first three.
        val results = matcher.matchByName("حسام احمد علي سيف", db)
        // The exact match should win, but the fuzzy candidate should also appear.
        assertTrue(results.size >= 2)
    }

    @Test
    fun national_id_match_is_exact() {
        val match = matcher.matchByNationalId("8964-1965-0481", db)
        assertNotNull(match)
        assertEquals(MatchType.EXACT, match!!.matchType)
        assertEquals("8964-1965-0481", match.personnel.ssn)
    }

    @Test
    fun national_id_match_ignores_dashes_and_spaces() {
        val match = matcher.matchByNationalId("896419650481", db)
        assertNotNull(match)
        assertEquals("8964-1965-0481", match!!.personnel.ssn)
    }

    @Test
    fun does_not_match_completely_different_names() {
        val results = matcher.matchByName("سعيد عبد ربه منصور هادي", db)
        // None of the database entries are close enough.
        assertTrue(results.all { it.score < 0.85 })
    }

    private fun person(id: Long, name: String, ssn: String = ""): Personnel =
        Personnel(
            id = id,
            ssn = ssn,
            name = name,
            normalizedName = normalizer.normalize(name)
        )
}
