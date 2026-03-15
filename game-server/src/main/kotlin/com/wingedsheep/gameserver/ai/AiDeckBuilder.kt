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
        val archetypeColors = archetype.colors.toSet()

        // Filter spells to matching colors + colorless
        val matchingSpells = cardPool.filter { card ->
            !card.isLand && (card.colors.isEmpty() || card.colors.all { it in archetypeColors })
        }

        if (matchingSpells.isEmpty()) {
            logger.warn("AI deckbuilder: no cards match archetype colors")
            return null
        }

        // Collect nonbasic lands from the pool
        val nonbasicLands = cardPool.filter { it.isLand && it.name !in BASIC_LAND_NAMES }

        // Step 1: Evaluate the card pool and find synergies
        val evaluation = stepEvaluate(archetype, matchingSpells, nonbasicLands) ?: return null

        // Step 2: Build the deck using the evaluation
        return stepBuild(archetype, matchingSpells, nonbasicLands, evaluation)
    }

    // =========================================================================
    // Step 1: Evaluate the card pool
    // =========================================================================

    private fun stepEvaluate(archetype: Archetype, matchingCards: List<CardDefinition>, nonbasicLands: List<CardDefinition>): String? {
        val prompt = buildEvaluatePrompt(archetype, matchingCards, nonbasicLands)
        logger.info("AI deckbuilder step 1 (evaluate) prompt ({} chars)", prompt.length)

        val messages = listOf(
            ChatMessage("system", EVALUATE_SYSTEM_PROMPT),
            ChatMessage("user", prompt)
        )

        val response = llmClient.chatCompletion(messages, properties.effectiveDeckbuildingModel)
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
            appendLine("You are evaluating a card pool for a ${archetype.name} ($colorNames) deck.")
            appendLine("Archetype guidance: ${archetype.description}")
            if (archetype.creatureTypes.isNotEmpty()) {
                appendLine("Suggested creature types: ${archetype.creatureTypes.joinToString(", ")}")
            }
            appendLine()
            appendLine("The archetype is a starting point, not a constraint. If you see stronger synergies or a better strategy in the card pool, pursue that instead.")
            appendLine()
            appendLine("CARD POOL:")

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
                appendLine()
                appendLine("$type:")
                for (card in cards.sortedBy { it.cmc }) {
                    val stats = if (card.creatureStats != null) " ${card.creatureStats}" else ""
                    val oracle = if (card.oracleText.isNotBlank()) " — ${card.oracleText}" else ""
                    appendLine("  ${card.name} ${card.manaCost} — ${card.typeLine}$stats$oracle")
                }
            }

            if (nonbasicLands.isNotEmpty()) {
                appendLine()
                appendLine("Nonbasic Lands:")
                for (land in nonbasicLands.distinctBy { it.name }.sortedBy { it.name }) {
                    val oracle = if (land.oracleText.isNotBlank()) " — ${land.oracleText}" else ""
                    appendLine("  ${land.name} — ${land.typeLine}$oracle")
                }
            }
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
            ChatMessage("system", BUILD_SYSTEM_PROMPT),
            ChatMessage("user", prompt)
        )

        val response = llmClient.chatCompletion(messages, properties.effectiveDeckbuildingModel)
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
        val spellNames = matchingCards.map { it.name }.toSet()
        val landNames = nonbasicLands.map { it.name }.toSet()

        return buildString {
            appendLine("Build a deck based on your evaluation below.")
            appendLine()
            appendLine("Archetype: ${archetype.name} ($colorNames)")
            appendLine()
            appendLine("YOUR EVALUATION:")
            appendLine(evaluation)
            appendLine()
            appendLine("CONSTRAINTS:")
            appendLine("- At least 40 cards (spells + lands)")
            appendLine("- Maximum 4 copies of any non-basic card")
            appendLine("- Available basic lands: Plains, Island, Swamp, Mountain, Forest")
            appendLine()
            appendLine("Available spells: ${spellNames.sorted().joinToString(", ")}")
            if (landNames.isNotEmpty()) {
                appendLine("Available nonbasic lands: ${landNames.sorted().joinToString(", ")}")
            }
        }
    }

    // =========================================================================
    // Parsing
    // =========================================================================

    private fun parseDeckList(response: String, availableCards: List<CardDefinition>): Map<String, Int>? {
        val cardNames = availableCards.map { it.name }.toSet() + BASIC_LAND_NAMES

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
            if (exactMatch != null && count in 1..4) {
                deckMap[exactMatch] = (deckMap[exactMatch] ?: 0) + count
            }
        }

        val totalCards = deckMap.values.sum()
        if (totalCards < 40) {
            logger.warn("AI deckbuilder: deck too small ({} cards), rejecting", totalCards)
            return null
        }

        // Enforce max 4 copies for non-basics
        for ((name, count) in deckMap.toMap()) {
            if (name !in BASIC_LAND_NAMES && count > 4) {
                deckMap[name] = 4
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
            You are an expert Magic: The Gathering deckbuilder evaluating a card pool.

            Look at the cards available and think about:
            - What are the strongest individual cards?
            - What synergies exist between cards? Which cards make other cards better?
            - Is the suggested archetype the best use of this pool, or do you see a stronger strategy?
            - What is a realistic game plan — how does this deck win?
            - What removal and interaction is available?
            - Are there enough cheap plays for the early game?

            Write your analysis. Be specific — name the cards and explain what makes them good together.
        """.trimIndent()

        private val BUILD_SYSTEM_PROMPT = """
            You are an expert Magic: The Gathering deckbuilder constructing a final deck list.

            You've already evaluated the card pool. Now build the deck.

            Reply using this EXACT format:

            <answer>
            4x Card Name
            3x Another Card
            24x Forest
            </answer>

            The <answer> section must contain ONLY the deck list, one entry per line.
            Use only cards from the available card names list and basic lands.
            The deck must have at least 40 cards. Use about 40% lands.
        """.trimIndent()
    }
}
