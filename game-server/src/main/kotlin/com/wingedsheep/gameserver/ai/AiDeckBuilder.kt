package com.wingedsheep.gameserver.ai

import com.wingedsheep.gameserver.config.AiProperties
import com.wingedsheep.gameserver.deck.Archetype
import com.wingedsheep.gameserver.deck.RandomDeckGenerator
import com.wingedsheep.gameserver.deck.SetArchetypes
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(AiDeckBuilder::class.java)


/**
 * Result of AI deck building — the deck list plus the chosen archetype for gameplay context.
 */
data class AiDeckResult(
    val deckList: Map<String, Int>,
    val archetype: Archetype?,
    val archetypeDescription: String?
)

/**
 * LLM-assisted deckbuilder that uses a two-step process:
 *
 * 1. **Evaluate** — LLM reviews the card pool, finds synergies, and shortlists the best cards
 * 2. **Build** — LLM constructs a 60-card deck from the shortlisted cards
 *
 * Falls back to [RandomDeckGenerator] if either step fails.
 */
class AiDeckBuilder(
    private val properties: AiProperties,
    private val llmClient: LlmClient,
    private val cardPool: List<CardDefinition>,
    private val basicLandVariants: List<CardDefinition>,
    private val setCodes: List<String>
) {

    fun build(): AiDeckResult {
        val archetype = pickArchetype()
        if (archetype == null) {
            logger.info("AI deckbuilder: no archetypes available, falling back to random deck")
            val deck = RandomDeckGenerator(cardPool, basicLandVariants).generate()
            return AiDeckResult(deck, null, null)
        }

        logger.info("AI deckbuilder: picked archetype '{}' ({}) for sets {}",
            archetype.name, archetype.colors.joinToString("/") { it.name }, setCodes)

        val llmDeck = tryLlmDeckbuild(archetype)
        if (llmDeck != null) {
            logger.info("AI deckbuilder: LLM built {} unique cards, {} total",
                llmDeck.size, llmDeck.values.sum())
            return AiDeckResult(
                llmDeck,
                archetype,
                formatArchetypeContext(archetype)
            )
        }

        logger.info("AI deckbuilder: LLM failed, falling back to random deck")
        val fallbackDeck = RandomDeckGenerator(cardPool, basicLandVariants).generate()
        return AiDeckResult(
            fallbackDeck,
            archetype,
            formatArchetypeContext(archetype)
        )
    }

    private fun pickArchetype(): Archetype? {
        val allArchetypes = setCodes.flatMap { code ->
            SetArchetypes.getForSet(code)?.archetypes ?: emptyList()
        }
        return allArchetypes.randomOrNull()
    }

    private fun tryLlmDeckbuild(archetype: Archetype): Map<String, Int>? {
        // Show the full card pool so the LLM can evaluate splashes
        val allSpells = cardPool.filter { !it.isLand }

        if (allSpells.isEmpty()) {
            logger.warn("AI deckbuilder: no spells in card pool")
            return null
        }

        // Collect nonbasic lands from the pool
        val nonbasicLands = cardPool.filter { it.isLand && it.name !in BASIC_LAND_NAMES }

        // Step 1: Evaluate the card pool and find synergies
        val evaluation = stepEvaluate(archetype, allSpells, nonbasicLands) ?: return null

        // Step 2: Build the deck using the evaluation
        return stepBuild(archetype, allSpells, nonbasicLands, evaluation)
    }

    // =========================================================================
    // Step 1: Evaluate the card pool
    // =========================================================================

    private fun stepEvaluate(archetype: Archetype, matchingCards: List<CardDefinition>, nonbasicLands: List<CardDefinition>): String? {
        val prompt = buildEvaluatePrompt(archetype, matchingCards, nonbasicLands)
        logger.info("AI deckbuilder step 1 (evaluate) prompt ({} chars)", prompt.length)

        val messages = listOf(
            ChatMessage("system", EVALUATE_SYSTEM_PROMPT, cacheControl = CacheControl()),
            ChatMessage("user", prompt)
        )

        val response = llmClient.chatCompletion(messages, properties.effectiveDeckbuildingModel, cacheControl = CacheControl())
        if (response == null) {
            logger.warn("AI deckbuilder: step 1 (evaluate) failed")
            return null
        }

        logger.info("AI deckbuilder step 1 response ({} chars)", response.length)
        return response
    }

    private fun buildEvaluatePrompt(archetype: Archetype, matchingCards: List<CardDefinition>, nonbasicLands: List<CardDefinition>): String {
        val colorNames = archetype.colors.joinToString("/") { it.name }

        return buildString {
            appendLine("You are evaluating a sealed card pool. Suggested archetype: ${archetype.name} ($colorNames).")
            appendLine("Archetype guidance: ${archetype.description}")
            if (archetype.creatureTypes.isNotEmpty()) {
                appendLine("Suggested creature types: ${archetype.creatureTypes.joinToString(", ")}")
            }
            appendLine()
            appendLine("The archetype is a suggestion, not a constraint. If you see stronger synergies, a better color pair, or a powerful splash, pursue that instead.")
            appendLine()
            appendLine("CARD POOL:")
            appendLine()
            appendCardPool(matchingCards, nonbasicLands)
        }
    }

    private fun StringBuilder.appendCardPool(matchingCards: List<CardDefinition>, nonbasicLands: List<CardDefinition>) {
        val byType = matchingCards.groupBy { card ->
            when {
                card.typeLine.isCreature -> "Creatures"
                card.typeLine.isInstant -> "Instants"
                card.typeLine.isSorcery -> "Sorceries"
                card.typeLine.isEnchantment -> "Enchantments"
                card.typeLine.isArtifact -> "Artifacts"
                else -> "Other"
            }
        }

        for ((type, cards) in byType.entries.sortedBy { it.key }) {
            appendLine("$type:")
            // Group duplicates and show count
            val grouped = cards.groupBy { it.name }
            for ((_, dupes) in grouped.entries.sortedBy { it.value.first().cmc }) {
                val card = dupes.first()
                val count = dupes.size
                val prefix = if (count > 1) "${count}x " else "  "
                val stats = if (card.creatureStats != null) " ${card.creatureStats}" else ""
                val rarity = card.metadata.rarity.name.lowercase().replaceFirstChar { it.uppercase() }
                val oracle = if (card.oracleText.isNotBlank()) " — ${card.oracleText.flattenOracle()}" else ""
                appendLine("$prefix${card.name} ${card.manaCost} — ${card.typeLine}$stats [$rarity]$oracle")
            }
            appendLine()
        }

        if (nonbasicLands.isNotEmpty()) {
            appendLine("Non-basic Lands:")
            for (land in nonbasicLands.distinctBy { it.name }.sortedBy { it.name }) {
                val oracle = if (land.oracleText.isNotBlank()) " — ${land.oracleText.flattenOracle()}" else ""
                appendLine("  ${land.name} — ${land.typeLine}$oracle")
            }
            appendLine()
        }
    }

    // =========================================================================
    // Step 2: Build the deck
    // =========================================================================

    private fun stepBuild(
        archetype: Archetype,
        matchingCards: List<CardDefinition>,
        nonbasicLands: List<CardDefinition>,
        evaluation: String
    ): Map<String, Int>? {
        val prompt = buildBuildPrompt(archetype, matchingCards, nonbasicLands, evaluation)
        logger.info("AI deckbuilder step 2 (build) prompt ({} chars)", prompt.length)

        val messages = listOf(
            ChatMessage("system", BUILD_SYSTEM_PROMPT, cacheControl = CacheControl()),
            ChatMessage("user", prompt)
        )

        val response = llmClient.chatCompletion(messages, properties.effectiveDeckbuildingModel, cacheControl = CacheControl())
        if (response == null) {
            logger.warn("AI deckbuilder: step 2 (build) failed")
            return null
        }

        logger.info("AI deckbuilder step 2 response ({} chars)", response.length)
        return parseDeckList(response, matchingCards + nonbasicLands)
    }

    private fun buildBuildPrompt(
        archetype: Archetype,
        matchingCards: List<CardDefinition>,
        nonbasicLands: List<CardDefinition>,
        evaluation: String
    ): String {
        val colorNames = archetype.colors.joinToString("/") { it.name }

        // Count available copies per card
        val availableCounts = matchingCards.groupingBy { it.name }.eachCount()
        val landCounts = nonbasicLands.groupingBy { it.name }.eachCount()

        return buildString {
            appendLine("You are building a sealed deck from this card pool.")
            appendLine()
            appendLine("Suggested archetype: ${archetype.name} ($colorNames)")
            appendLine("The archetype is a suggestion — if you see stronger synergies or a better strategy, pursue that instead.")
            appendLine()
            appendLine("GUIDELINES:")
            appendLine("- At least 40 cards (40 is ideal, going slightly over is fine)")
            appendLine("- ~23 non-land cards (creatures + spells) and ~17 lands")
            appendLine("- Pick 2 colors (sometimes splash a 3rd for a bomb). Do NOT play all 5 colors.")
            appendLine("- Only include cards you can actually cast with your lands")
            appendLine("- You may add any number of basic lands: Plains, Island, Swamp, Mountain, Forest")
            appendLine("- Prioritize creatures, removal, synergy, and a good mana curve")
            appendLine("- Include non-basic lands from your pool if they fit your colors")
            appendLine("- Do NOT include more copies than available in the pool")
            appendLine()
            appendLine("YOUR EVALUATION:")
            appendLine(evaluation)
            appendLine()
            appendLine("CARD POOL (available copies):")
            appendLine()
            appendCardPool(matchingCards, nonbasicLands)
            appendLine()
            appendLine("AVAILABLE COPIES:")
            for ((name, count) in (availableCounts + landCounts).entries.sortedBy { it.key }) {
                if (count > 1) appendLine("  ${name}: ${count}x")
            }
        }
    }

    // =========================================================================
    // Parsing
    // =========================================================================

    private fun parseDeckList(response: String, availableCards: List<CardDefinition>): Map<String, Int>? {
        val cardNames = availableCards.map { it.name }.toSet() + BASIC_LAND_NAMES
        val poolCounts = availableCards.groupingBy { it.name }.eachCount()

        // Extract from <answer> tags if present, otherwise use full response
        val answerMatch = Regex("""<answer>(.*?)</answer>""", RegexOption.DOT_MATCHES_ALL).find(response)
        val deckText = answerMatch?.groupValues?.get(1)?.trim() ?: response

        val deckMap = mutableMapOf<String, Int>()
        val linePattern = Regex("""(\d+)\s*x?\s+(.+)""", RegexOption.IGNORE_CASE)

        for (line in deckText.lines()) {
            val match = linePattern.find(line.trim()) ?: continue
            val count = match.groupValues[1].toIntOrNull() ?: continue
            val name = match.groupValues[2].trim()

            val exactMatch = cardNames.find { it.equals(name, ignoreCase = true) }
            if (exactMatch != null && count >= 1) {
                deckMap[exactMatch] = (deckMap[exactMatch] ?: 0) + count
            }
        }

        val totalCards = deckMap.values.sum()
        if (totalCards < 40) {
            logger.warn("AI deckbuilder: deck too small ({} cards), rejecting", totalCards)
            return null
        }

        // Enforce pool limits: non-basics can't exceed available copies
        for ((name, count) in deckMap.toMap()) {
            if (name !in BASIC_LAND_NAMES) {
                val maxCopies = poolCounts[name] ?: 4
                if (count > maxCopies) {
                    logger.info("AI deckbuilder: clamping {} from {} to {} (pool limit)", name, count, maxCopies)
                    deckMap[name] = maxCopies
                }
            }
        }

        logger.info("AI deckbuilder: parsed deck with {} unique cards, {} total",
            deckMap.size, deckMap.values.sum())
        return deckMap
    }

    companion object {
        private val BASIC_LAND_NAMES = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

        fun formatArchetypeContext(archetype: Archetype): String {
            val colorNames = archetype.colors.joinToString("/") { it.name }
            return buildString {
                appendLine("Your deck archetype: ${archetype.name} ($colorNames) — ${archetype.description}")
                if (archetype.creatureTypes.isNotEmpty()) {
                    appendLine("Prioritize creature types: ${archetype.creatureTypes.joinToString(", ")}")
                }
            }.trim()
        }

        private val EVALUATE_SYSTEM_PROMPT = """
            You are an expert Magic: The Gathering sealed deckbuilder evaluating a card pool.

            Look at the cards available and think about:
            - What are the strongest individual cards? Prioritize rares/uncommons and efficient removal.
            - What synergies exist between cards? Which cards make other cards better?
            - Is the suggested archetype the best use of this pool, or do you see a stronger strategy?
            - What is a realistic game plan — how does this deck win?
            - What removal and interaction is available?
            - Is the mana curve good? You need enough 2-drops and 3-drops to not fall behind.
            - Are there any bombs (high-impact rares) that pull you toward specific colors?

            Write your analysis. Be specific — name the cards and explain what makes them good together.
        """.trimIndent()

        private val BUILD_SYSTEM_PROMPT = """
            You are an expert Magic: The Gathering sealed deckbuilder constructing a final deck list.

            You've already evaluated the card pool. Now build the deck.

            Reply with ONLY the deck list, one entry per line. No explanation, no reasoning.

            Example format:
            1x Heir of the Wilds
            2x Alpine Grizzly
            1x Opulent Palace
            9x Forest
            8x Island

            Guidelines:
            - At least 40 cards (~23 spells + ~17 lands). 40 is ideal, slightly over is fine.
            - Only use cards from the pool and basic lands (Plains, Island, Swamp, Mountain, Forest)
            - Do not exceed the available copies of each card
            - Match your land base to your spell colors
            - Build for synergy — cards that work well together beat individually strong cards
        """.trimIndent()
    }
}
