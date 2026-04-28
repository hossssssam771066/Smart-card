package com.smartcardscanner.domain.matching

import com.smartcardscanner.domain.model.MatchResult
import com.smartcardscanner.domain.model.MatchType
import com.smartcardscanner.domain.model.MatchedBy
import com.smartcardscanner.domain.model.Personnel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.max

@Singleton
class ArabicNameMatcher @Inject constructor(
    private val normalizer: ArabicNameNormalizer
) {

    fun matchByNationalId(nationalId: String, personnelList: List<Personnel>): MatchResult? {
        if (nationalId.isBlank()) return null
        val cleaned = nationalId.replace("-", "").replace(" ", "").trim()
        val match = personnelList.find {
            it.ssn.replace("-", "").replace(" ", "").trim() == cleaned
        }
        return match?.let {
            MatchResult(
                personnel = it,
                score = 1.0,
                matchType = MatchType.EXACT,
                matchedBy = MatchedBy.NATIONAL_ID
            )
        }
    }

    fun matchByName(
        extractedName: String,
        personnelList: List<Personnel>,
        maxResults: Int = 5
    ): List<MatchResult> {
        if (extractedName.isBlank()) return emptyList()

        val normalizedInput = normalizer.normalize(extractedName)
        val inputTokens = normalizer.tokenize(normalizedInput)

        val results = mutableListOf<MatchResult>()

        for (person in personnelList) {
            val normalizedDbName = person.normalizedName.ifEmpty {
                normalizer.normalize(person.name)
            }

            // Exact match after normalization
            if (normalizedInput == normalizedDbName) {
                results.add(
                    MatchResult(
                        personnel = person,
                        score = 1.0,
                        matchType = MatchType.EXACT,
                        matchedBy = MatchedBy.NAME_EXACT
                    )
                )
                continue
            }

            val dbTokens = normalizer.tokenize(normalizedDbName)
            val score = calculateTokenScore(inputTokens, dbTokens)

            if (score >= 0.60) {
                val matchType = when {
                    score >= 0.95 -> MatchType.EXACT
                    score >= 0.85 -> MatchType.STRONG
                    score >= 0.60 -> MatchType.POSSIBLE
                    else -> MatchType.NONE
                }
                results.add(
                    MatchResult(
                        personnel = person,
                        score = score,
                        matchType = matchType,
                        matchedBy = MatchedBy.NAME_FUZZY
                    )
                )
            }
        }

        return results
            .sortedByDescending { it.score }
            .take(maxResults)
    }

    fun calculateTokenScore(inputTokens: List<String>, dbTokens: List<String>): Double {
        if (inputTokens.isEmpty() || dbTokens.isEmpty()) return 0.0

        var totalScore = 0.0
        val usedIndices = mutableSetOf<Int>()

        for (inputToken in inputTokens) {
            var bestMatch = 0.0
            var bestIndex = -1

            for ((index, dbToken) in dbTokens.withIndex()) {
                if (index in usedIndices) continue
                val similarity = tokenSimilarity(inputToken, dbToken)
                if (similarity > bestMatch) {
                    bestMatch = similarity
                    bestIndex = index
                }
            }

            if (bestIndex >= 0) {
                usedIndices.add(bestIndex)
            }
            totalScore += bestMatch
        }

        val maxTokens = max(inputTokens.size, dbTokens.size)
        return totalScore / maxTokens
    }

    private fun tokenSimilarity(a: String, b: String): Double {
        if (a == b) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0

        // One is a prefix of the other
        if (a.startsWith(b) || b.startsWith(a)) {
            val shorter = min(a.length, b.length)
            val longer = max(a.length, b.length)
            return shorter.toDouble() / longer.toDouble() * 0.95
        }

        val distance = levenshteinDistance(a, b)
        val maxLen = max(a.length, b.length)

        if (distance <= 1 && maxLen >= 3) return 0.9
        if (distance <= 2 && maxLen >= 5) return 0.75

        val similarity = 1.0 - (distance.toDouble() / maxLen.toDouble())
        return max(0.0, similarity)
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length

        if (m == 0) return n
        if (n == 0) return m

        val dp = Array(m + 1) { IntArray(n + 1) }

        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j

        for (i in 1..m) {
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[m][n]
    }
}
