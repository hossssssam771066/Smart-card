package com.smartcardscanner.domain.model

data class MatchResult(
    val personnel: Personnel,
    val score: Double,
    val matchType: MatchType,
    val matchedBy: MatchedBy
)

enum class MatchType {
    EXACT,      // 100% match
    STRONG,     // 85-99%
    POSSIBLE,   // 60-84%
    NONE        // < 60%
}

enum class MatchedBy {
    NATIONAL_ID,   // Matched by national ID / SSN
    NAME_EXACT,    // Matched by exact name
    NAME_FUZZY,    // Matched by fuzzy name matching
    MANUAL         // Manually selected
}
