package com.wingedsheep.gameserver.ai

import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(AiResponseParser::class.java)

/**
 * Parses LLM text responses into structured action/decision choices.
 */
class AiResponseParser {

    /**
     * Parse a response that should contain a lettered action choice (e.g., "B", "[B]", "I choose B").
     * Returns the 0-based index of the chosen action, or null if parsing fails.
     */
    fun parseActionChoice(response: String, maxIndex: Int): Int? {
        val cleaned = response.trim()

        // Try exact single letter
        if (cleaned.length == 1 && cleaned[0].isLetter()) {
            val index = GameStateFormatter.letterToIndex(cleaned)
            if (index != null && index <= maxIndex) return index
        }

        // Try [X] pattern
        val bracketPattern = Regex("""\[([A-Z]{1,2})]""")
        val bracketMatch = bracketPattern.find(cleaned.uppercase())
        if (bracketMatch != null) {
            val index = GameStateFormatter.letterToIndex(bracketMatch.groupValues[1])
            if (index != null && index <= maxIndex) return index
        }

        // Try "Action: X" or "I choose X" or "My choice is X" patterns
        val choicePatterns = listOf(
            Regex("""(?:action|choice|choose|select|pick|answer)[:\s]+\[?([A-Z]{1,2})\]?""", RegexOption.IGNORE_CASE),
            Regex("""^(?:I (?:will |would )?(?:choose|select|pick|go with) )\[?([A-Z]{1,2})\]?""", RegexOption.IGNORE_CASE),
            Regex("""(?:^|\n)\[?([A-Z])\]?[.\s—–-]"""),
        )
        for (pattern in choicePatterns) {
            val match = pattern.find(cleaned)
            if (match != null) {
                val index = GameStateFormatter.letterToIndex(match.groupValues[1])
                if (index != null && index <= maxIndex) return index
            }
        }

        // Last resort: take the first valid letter found in the response
        val standaloneLetterPattern = Regex("""[A-Z]""")
        val allLetters = standaloneLetterPattern.findAll(cleaned.uppercase()).toList()
        for (letterMatch in allLetters) {
            val index = GameStateFormatter.letterToIndex(letterMatch.value)
            if (index != null && index <= maxIndex) {
                if (allLetters.size > 1) {
                    logger.info("Multiple letters found in response, taking first valid: ${letterMatch.value}")
                }
                return index
            }
        }

        logger.warn("Failed to parse action choice from: ${cleaned.take(200)}")
        return null
    }

    /**
     * Parse a response that should contain multiple selections (e.g., "1, 3, 5" or "A, C, E").
     * Returns a list of 0-based indices.
     */
    fun parseMultipleSelections(response: String, maxIndex: Int): List<Int>? {
        val cleaned = response.trim().uppercase()

        // Try comma-separated letters: "A, C, E"
        val letterPattern = Regex("""[A-Z]""")
        val letters = letterPattern.findAll(cleaned).toList()
        if (letters.isNotEmpty()) {
            val indices = letters.mapNotNull { GameStateFormatter.letterToIndex(it.value) }
                .filter { it <= maxIndex }
                .distinct()
            if (indices.isNotEmpty()) return indices
        }

        // Try comma-separated numbers: "1, 3, 5" (1-based)
        val numberPattern = Regex("""\d+""")
        val numbers = numberPattern.findAll(cleaned).toList()
        if (numbers.isNotEmpty()) {
            val indices = numbers.mapNotNull { it.value.toIntOrNull()?.minus(1) }
                .filter { it in 0..maxIndex }
                .distinct()
            if (indices.isNotEmpty()) return indices
        }

        logger.warn("Failed to parse multiple selections from: ${cleaned.take(200)}")
        return null
    }

    /**
     * Parse a yes/no response. Returns true for yes, false for no, null if unparseable.
     */
    fun parseYesNo(response: String): Boolean? {
        val lower = response.trim().lowercase()

        // Check for clear yes/no
        if (lower.startsWith("yes") || lower == "y" || lower == "[a]" || lower == "a") return true
        if (lower.startsWith("no") || lower == "n" || lower == "[b]" || lower == "b") return false

        // Check for yes/no anywhere in response
        val yesCount = Regex("""\byes\b""", RegexOption.IGNORE_CASE).findAll(response).count()
        val noCount = Regex("""\bno\b""", RegexOption.IGNORE_CASE).findAll(response).count()
        if (yesCount > 0 && noCount == 0) return true
        if (noCount > 0 && yesCount == 0) return false

        logger.warn("Failed to parse yes/no from: ${response.take(200)}")
        return null
    }

    /**
     * Parse a number from the response.
     */
    fun parseNumber(response: String, min: Int, max: Int): Int? {
        val numberPattern = Regex("""\d+""")
        val matches = numberPattern.findAll(response.trim()).toList()
        for (match in matches) {
            val num = match.value.toIntOrNull()
            if (num != null && num in min..max) return num
        }
        logger.warn("Failed to parse number ($min-$max) from: ${response.take(200)}")
        return null
    }

    /**
     * Parse a mulligan keep/mulligan choice.
     * Returns true for keep, false for mulligan, null if unparseable.
     */
    fun parseMulliganChoice(response: String): Boolean? {
        val lower = response.trim().lowercase()

        if (lower.contains("keep") || lower == "a" || lower == "[a]") return true
        if (lower.contains("mulligan") || lower == "b" || lower == "[b]") return false

        return parseYesNo(response)?.let { true } // "yes" = keep
    }

    /**
     * Parse distribution amounts (e.g., "A:2, B:1" or "2, 1").
     * Returns map of 0-based index to amount, or null if unparseable.
     */
    fun parseDistribution(response: String, targetCount: Int, total: Int): Map<Int, Int>? {
        val cleaned = response.trim().uppercase()

        // Try "A:2, B:1" format
        val namedPattern = Regex("""([A-Z]):?\s*(\d+)""")
        val namedMatches = namedPattern.findAll(cleaned).toList()
        if (namedMatches.isNotEmpty()) {
            val result = mutableMapOf<Int, Int>()
            for (match in namedMatches) {
                val index = GameStateFormatter.letterToIndex(match.groupValues[1]) ?: continue
                val amount = match.groupValues[2].toIntOrNull() ?: continue
                if (index < targetCount) result[index] = amount
            }
            if (result.values.sum() == total) return result
        }

        // Try plain numbers: "2, 1"
        val numberPattern = Regex("""\d+""")
        val numbers = numberPattern.findAll(cleaned).mapNotNull { it.value.toIntOrNull() }.toList()
        if (numbers.size == targetCount && numbers.sum() == total) {
            return numbers.withIndex().associate { (i, v) -> i to v }
        }

        logger.warn("Failed to parse distribution from: ${cleaned.take(200)}")
        return null
    }

    /**
     * Parse an ordering response (e.g., "B, A, C").
     * Returns list of 0-based indices in the specified order, or null if unparseable.
     */
    fun parseOrdering(response: String, itemCount: Int): List<Int>? {
        val cleaned = response.trim().uppercase()

        val letterPattern = Regex("""[A-Z]""")
        val letters = letterPattern.findAll(cleaned).toList()
        if (letters.isNotEmpty()) {
            val indices = letters.mapNotNull { GameStateFormatter.letterToIndex(it.value) }
                .filter { it < itemCount }
                .distinct()
            if (indices.size == itemCount) return indices
        }

        // Try numbers
        val numberPattern = Regex("""\d+""")
        val numbers = numberPattern.findAll(cleaned).mapNotNull { it.value.toIntOrNull()?.minus(1) }.toList()
            .filter { it in 0 until itemCount }
            .distinct()
        if (numbers.size == itemCount) return numbers

        logger.warn("Failed to parse ordering from: ${cleaned.take(200)}")
        return null
    }
}
