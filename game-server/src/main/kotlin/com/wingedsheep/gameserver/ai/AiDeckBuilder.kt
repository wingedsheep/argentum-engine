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
 * LLM-assisted deckbuilder that uses set archetypes to build coherent decks.
 *
 * Flow:
 * 1. Pick a random archetype for the available sets
 * 2. Format the card pool for the LLM with archetype context
 * 3. Parse the LLM's deck list response
 * 4. Fall back to [RandomDeckGenerator] if the LLM fails
 */
class AiDeckBuilder(
    private val properties: AiProperties,
    private val openRouterClient: OpenRouterClient,
    private val cardPool: List<CardDefinition>,
    private val basicLandVariants: List<CardDefinition>,
    private val setCodes: List<String>
) {

    fun build(): AiDeckResult {
        // Pick an archetype from the available sets
        val archetype = pickArchetype()
        if (archetype == null) {
            logger.info("AI deckbuilder: no archetypes available, falling back to random deck")
            val deck = RandomDeckGenerator(cardPool, basicLandVariants).generate()
            return AiDeckResult(deck, null, null)
        }

        logger.info("AI deckbuilder: picked archetype '{}' ({}) for sets {}",
            archetype.name, archetype.colors.joinToString("/") { it.name }, setCodes)

        // Try LLM-assisted deckbuilding
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

        // Fallback: random deck using archetype colors
        logger.info("AI deckbuilder: LLM failed, falling back to random deck with archetype colors")
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

        // Filter card pool to matching colors + colorless
        val matchingCards = cardPool.filter { card ->
            !card.isLand && (card.colors.isEmpty() || card.colors.all { it in archetypeColors })
        }

        if (matchingCards.isEmpty()) {
            logger.warn("AI deckbuilder: no cards match archetype colors")
            return null
        }

        val prompt = buildDeckbuildPrompt(archetype, matchingCards)
        logger.info("AI deckbuilder prompt ({} chars)", prompt.length)

        val messages = listOf(
            ChatMessage("system", DECKBUILDING_SYSTEM_PROMPT),
            ChatMessage("user", prompt)
        )

        val response = openRouterClient.chatCompletion(messages, properties.effectiveDeckbuildingModel) ?: return null
        logger.info("AI deckbuilder response ({} chars)", response.length)

        return parseDeckList(response, matchingCards)
    }

    private fun buildDeckbuildPrompt(archetype: Archetype, matchingCards: List<CardDefinition>): String {
        val colorNames = archetype.colors.joinToString("/") { it.name }

        return buildString {
            appendLine("Build a 60-card Magic: The Gathering deck.")
            appendLine()
            appendLine("Your archetype: ${archetype.name} ($colorNames) — ${archetype.description}")
            if (archetype.creatureTypes.isNotEmpty()) {
                appendLine("Prioritize these creature types: ${archetype.creatureTypes.joinToString(", ")}")
            }
            appendLine()
            appendLine("DECK CONSTRAINTS:")
            appendLine("- Exactly 60 cards total (spells + lands)")
            appendLine("- Maximum 4 copies of any non-basic card")
            appendLine("- 22-26 lands depending on mana curve")
            appendLine()
            appendLine("DECKBUILDING STRATEGY:")
            appendLine("- Follow your archetype's strategy")
            appendLine("- Build a good mana curve: ~4 one-drops, ~8 two-drops, ~8 three-drops, ~6 four-drops, ~3 five+ drops")
            appendLine("- Include 4-8 removal/interaction spells")
            if (archetype.colors.size >= 3) {
                appendLine("- For 3-color decks, lean toward the primary color for consistency")
            }
            appendLine("- Colorless cards can fill gaps in any archetype")
            appendLine()
            appendLine("AVAILABLE CARDS:")

            // Group by type
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

            appendLine()
            appendLine("AVAILABLE BASIC LANDS: Plains, Island, Swamp, Mountain, Forest")
            appendLine()
            appendLine("First reason about your card choices inside <reasoning> tags, then put your deck list inside <answer> tags.")
        }
    }

    private fun parseDeckList(response: String, availableCards: List<CardDefinition>): Map<String, Int>? {
        val cardNames = availableCards.map { it.name }.toSet() +
            setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

        // Extract from <answer> tags if present, otherwise use full response
        val answerMatch = Regex("""<answer>(.*?)</answer>""", RegexOption.DOT_MATCHES_ALL).find(response)
        val deckText = answerMatch?.groupValues?.get(1)?.trim() ?: response

        val deckMap = mutableMapOf<String, Int>()
        val linePattern = Regex("""(\d+)\s*x?\s+(.+)""", RegexOption.IGNORE_CASE)

        for (line in deckText.lines()) {
            val match = linePattern.find(line.trim()) ?: continue
            val count = match.groupValues[1].toIntOrNull() ?: continue
            val name = match.groupValues[2].trim()

            // Find closest match in card pool
            val exactMatch = cardNames.find { it.equals(name, ignoreCase = true) }
            if (exactMatch != null && count in 1..4) {
                deckMap[exactMatch] = (deckMap[exactMatch] ?: 0) + count
            }
        }

        // Validate
        val totalCards = deckMap.values.sum()
        if (totalCards < 40) {
            logger.warn("AI deckbuilder: deck too small ({} cards), rejecting", totalCards)
            return null
        }

        // Enforce max 4 copies for non-basics
        val basics = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
        for ((name, count) in deckMap.toMap()) {
            if (name !in basics && count > 4) {
                deckMap[name] = 4
            }
        }

        // Pad or trim to 60
        val currentTotal = deckMap.values.sum()
        if (currentTotal < 60) {
            // Add basic lands to reach 60
            val primaryLand = when (availableCards.firstOrNull()?.colors?.firstOrNull()) {
                Color.WHITE -> "Plains"
                Color.BLUE -> "Island"
                Color.BLACK -> "Swamp"
                Color.RED -> "Mountain"
                Color.GREEN -> "Forest"
                else -> "Forest"
            }
            deckMap[primaryLand] = (deckMap[primaryLand] ?: 0) + (60 - currentTotal)
        }

        logger.info("AI deckbuilder: parsed deck with {} unique cards, {} total",
            deckMap.size, deckMap.values.sum())
        return deckMap
    }

    companion object {
        fun formatArchetypeContext(archetype: Archetype): String {
            val colorNames = archetype.colors.joinToString("/") { it.name }
            return buildString {
                appendLine("Your deck archetype: ${archetype.name} ($colorNames) — ${archetype.description}")
                if (archetype.creatureTypes.isNotEmpty()) {
                    appendLine("Prioritize creature types: ${archetype.creatureTypes.joinToString(", ")}")
                }
            }.trim()
        }

        private val DECKBUILDING_SYSTEM_PROMPT = """
            You are an expert Magic: The Gathering deckbuilder. Build the best possible deck from the available card pool following the given archetype strategy.

            Reply using this EXACT format:

            <reasoning>
            Analyze the card pool and archetype. Consider:
            - What is the win condition for this archetype?
            - Which cards are the key payoffs and which are filler?
            - What does the mana curve look like? Are there enough early plays?
            - How much removal/interaction is available?
            - What is the right land split for the colors needed?
            </reasoning>
            <answer>
            4x Card Name
            3x Another Card
            24x Forest
            </answer>

            The <answer> section must contain ONLY the deck list, one entry per line.
        """.trimIndent()
    }
}
