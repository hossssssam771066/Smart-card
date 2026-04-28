package com.smartcardscanner.domain.matching

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArabicNameNormalizer @Inject constructor() {

    private val compoundNames = mapOf(
        "عبد الله" to "عبدالله",
        "عبد الرحمن" to "عبدالرحمن",
        "عبد العزيز" to "عبدالعزيز",
        "عبد الكريم" to "عبدالكريم",
        "عبد الحكيم" to "عبدالحكيم",
        "عبد الرحيم" to "عبدالرحيم",
        "عبد المجيد" to "عبدالمجيد",
        "عبد الحميد" to "عبدالحميد",
        "عبد الواحد" to "عبدالواحد",
        "عبد القادر" to "عبدالقادر",
        "عبد الرزاق" to "عبدالرزاق",
        "عبد الباري" to "عبدالباري",
        "عبد السلام" to "عبدالسلام",
        "عبد اللطيف" to "عبداللطيف",
        "عبد الملك" to "عبدالملك",
        "عبد الناصر" to "عبدالناصر",
        "عبد الفتاح" to "عبدالفتاح",
        "عبد الغني" to "عبدالغني",
        "عبد الصمد" to "عبدالصمد",
        "عبد الجبار" to "عبدالجبار",
        "عبد الرقيب" to "عبدالرقيب",
        "عبد الخالق" to "عبدالخالق",
        "عبد الوهاب" to "عبدالوهاب",
        "عبد المنعم" to "عبدالمنعم",
        "عبد الحق" to "عبدالحق",
        "عبد الإله" to "عبدالاله",
        "عبد ربه" to "عبدربه"
    )

    // Arabic diacritics (tashkeel) unicode range
    private val diacriticsRegex = Regex("[\\u0610-\\u061A\\u064B-\\u065F\\u0670\\u06D6-\\u06DC\\u06DF-\\u06E4\\u06E7\\u06E8\\u06EA-\\u06ED]")

    fun normalize(name: String): String {
        var result = name.trim()

        // Remove diacritics (tashkeel)
        result = diacriticsRegex.replace(result, "")

        // Normalize hamza variants to alef
        result = result.replace('أ', 'ا')
        result = result.replace('إ', 'ا')
        result = result.replace('آ', 'ا')
        result = result.replace('ٱ', 'ا')

        // Normalize taa marbouta to haa
        result = result.replace('ة', 'ه')

        // Normalize alef maqsura to yaa
        result = result.replace('ى', 'ي')

        // Remove tatweel (kashida)
        result = result.replace("\u0640", "")

        // Normalize spaces (multiple spaces to single)
        result = result.replace(Regex("\\s+"), " ")

        // Merge compound names
        for ((separated, merged) in compoundNames) {
            val normalizedSeparated = separated
                .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
                .replace('ة', 'ه').replace('ى', 'ي')
            result = result.replace(normalizedSeparated, merged, ignoreCase = false)
        }

        return result.trim()
    }

    fun tokenize(normalizedName: String): List<String> {
        return normalizedName.split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun normalizeEnglishName(name: String): String {
        return name.trim()
            .uppercase()
            .replace(Regex("\\s+"), " ")
            .replace("<", " ")
            .trim()
    }

    fun tokenizeEnglish(name: String): List<String> {
        return normalizeEnglishName(name).split(" ")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }
}
