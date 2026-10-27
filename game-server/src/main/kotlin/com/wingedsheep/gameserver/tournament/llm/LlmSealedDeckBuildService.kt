package com.wingedsheep.gameserver.tournament.llm

import com.wingedsheep.ai.engine.buildHeuristicSealedDeck
import com.wingedsheep.ai.llm.AiConfig
import com.wingedsheep.ai.llm.ChatMessage
import com.wingedsheep.ai.llm.LlmClient
import com.wingedsheep.ai.llm.LlmUsage
import com.wingedsheep.gameserver.config.AiProperties
import com.wingedsheep.gameserver.config.GameProperties
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.CardDefinition
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Builds a 40-card **sealed** deck from a pool. Shared by the human tournament lobby
 * ([com.wingedsheep.gameserver.handler.LobbyHandler]) and the dev LLM tournament.
 *
 * - **heuristic**: [com.wingedsheep.ai.engine.buildHeuristicSealedDeck] — fast, free, deterministic.
 * - **llm**: a single-shot sealed prompt to the given model; parsed + validated against the pool,
 *   padded/trimmed to 40, basics as plain `Forest`/`Mountain`/… names. Falls back to heuristic if
 *   the LLM is unavailable or returns an unusable list.
 *
 * NB: emits **plain card names** (incl. plain basic-land names) so the engine's `CardRegistry`
 * resolves every entry. Do NOT use the constructed-deck `AiDeckBuilder` here — it targets 60 cards
 * and its random fallback emits collector-numbered basic variants (e.g. `Mountain#272`) that the
 * registry can't resolve.
 */
@Service
class LlmSealedDeckBuildService(
    private val gameProperties: GameProperties
) {
    private val logger = LoggerFactory.getLogger(LlmSealedDeckBuildService::class.java)

    private val basics = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

    /**
     * @param useLlm caller's intent to use model-driven building (ANDed with config + key availability)
     * @param modelOverride the model to build with (null → server's configured deckbuilding model)
     * @param usageSink optional token/cost callback for the LLM call
     */
    fun build(
        pool: List<CardDefinition>,
        useLlm: Boolean,
        modelOverride: String? = null,
        usageSink: ((LlmUsage) -> Unit)? = null
    ): Map<String, Int> {
        val ai = gameProperties.ai
        // An explicit per-tournament "LLM" choice (useLlm) overrides the server's
        // `heuristic-deckbuilding` convenience flag — that flag only governs casual vs-AI play.
        val canLlm = useLlm && ai.enabled && ai.effectiveApiKey.isNotBlank()
        if (!canLlm) {
            logger.info(
                "Sealed deckbuild → HEURISTIC. requestedLlm={}, aiEnabled={}, apiKeyPresent={}",
                useLlm, ai.enabled, ai.effectiveApiKey.isNotBlank()
            )
        }
        if (canLlm) {
            try {
                val deck = tryLlmSealedDeck(pool, ai, modelOverride, usageSink)
                if (deck != null) return deck
                logger.info("LLM sealed deckbuild returned nothing, falling back to heuristic")
            } catch (e: Exception) {
                logger.warn("LLM sealed deckbuild failed (model={}), falling back to heuristic: {}",
                    modelOverride ?: ai.effectiveDeckbuildingModel, e.message)
            }
        }
        val deck = buildHeuristicSealedDeck(pool)
        logger.info("Heuristic sealed deck ({} cards)", deck.values.sum())
        return deck
    }

    private fun tryLlmSealedDeck(
        pool: List<CardDefinition>,
        ai: AiProperties,
        modelOverride: String?,
        usageSink: ((LlmUsage) -> Unit)?
    ): Map<String, Int>? {
        val model = modelOverride ?: ai.effectiveDeckbuildingModel
        val nonLands = pool.filter { !it.typeLine.isLand }
        val poolLands = pool.filter { it.typeLine.isLand && (!it.typeLine.isBasicLand || it.typeLine.isSnow) }

        val prompt = buildString {
            appendLine("You are building a 40-card sealed deck from this card pool.")
            appendLine()
            appendLine("RULES:")
            appendLine("- Exactly 40 cards total")
            appendLine("- ~23 non-land cards (creatures + spells) and ~17 lands")
            appendLine("- Pick 2 colors (sometimes splash a 3rd). Do NOT play all 5 colors.")
            appendLine("- Only include cards you can actually cast with your lands")
            appendLine("- You may add any number of basic lands: Plains, Island, Swamp, Mountain, Forest")
            appendLine("- Prioritize creatures, removal, and a good mana curve")
            appendLine("- Include non-basic lands from your pool if they fit your colors")
            appendLine()
            appendLine("YOUR CARD POOL:")

            val byType = nonLands.groupBy { card ->
                when {
                    card.typeLine.isCreature -> "Creatures"
                    card.typeLine.isInstant || card.typeLine.isSorcery -> "Spells"
                    card.typeLine.isEnchantment -> "Enchantments"
                    card.typeLine.isArtifact -> "Artifacts"
                    else -> "Other"
                }
            }
            for ((type, cards) in byType.entries.sortedBy { it.key }) {
                appendLine()
                appendLine("$type:")
                val grouped = cards.groupBy { it.name }
                for ((_, copies) in grouped.entries.sortedBy { it.value.first().cmc }) {
                    val card = copies.first()
                    val stats = if (card.creatureStats != null) " ${card.creatureStats}" else ""
                    val oracle = if (card.oracleText.isNotBlank()) " — ${card.oracleText.replace("\n", " / ")}" else ""
                    val count = if (copies.size > 1) "${copies.size}x " else ""
                    appendLine("  $count${card.name} ${card.manaCost} — ${card.typeLine}$stats$oracle")
                }
            }
            if (poolLands.isNotEmpty()) {
                appendLine()
                appendLine("Non-basic lands in pool:")
                for ((_, copies) in poolLands.groupBy { it.name }) {
                    val card = copies.first()
                    val count = if (copies.size > 1) "${copies.size}x " else ""
                    val oracle = if (card.oracleText.isNotBlank()) " — ${card.oracleText.replace("\n", " / ")}" else ""
                    appendLine("  $count${card.name}$oracle")
                }
            }
            appendLine()
            appendLine("Reply ONLY with the deck list, one entry per line:")
            appendLine("1x Card Name")
            appendLine("9x Forest")
        }

        val client = LlmClient(ai.toAiConfig(), usageSink)
        val messages = listOf(
            ChatMessage("system",
                "You are an expert Magic: The Gathering limited deckbuilder. " +
                    "Analyze the sealed pool, pick the best 2 colors (with optional light splash), " +
                    "and build a strong 40-card deck. Reply ONLY with the deck list."),
            ChatMessage("user", prompt)
        )

        val response = client.chatCompletion(messages, modelOverride = model) ?: return null
        return parseSealedDeckList(response, pool)
    }

    private fun parseSealedDeckList(response: String, pool: List<CardDefinition>): Map<String, Int>? {
        val poolCounts = pool.groupBy { it.name }.mapValues { it.value.size }
        val validNames = poolCounts.keys + basics

        val deckMap = mutableMapOf<String, Int>()
        val linePattern = Regex("""(\d+)\s*x?\s+(.+)""", RegexOption.IGNORE_CASE)

        for (line in response.lines()) {
            val match = linePattern.find(line.trim()) ?: continue
            val count = match.groupValues[1].toIntOrNull() ?: continue
            val name = match.groupValues[2].trim()
            val exactMatch = validNames.find { it.equals(name, ignoreCase = true) } ?: continue
            if (count < 1) continue
            val maxAllowed = if (exactMatch in basics) count else poolCounts[exactMatch] ?: 0
            val actual = count.coerceAtMost(maxAllowed)
            if (actual > 0) deckMap[exactMatch] = (deckMap[exactMatch] ?: 0) + actual
        }

        val total = deckMap.values.sum()
        if (total < 30) {
            logger.warn("LLM sealed deckbuild: deck too small ({} cards), rejecting", total)
            return null
        }
        if (total < 40) {
            val primaryLand = guessPrimaryBasicLand(deckMap, pool)
            deckMap[primaryLand] = (deckMap[primaryLand] ?: 0) + (40 - total)
        }
        while (deckMap.values.sum() > 40) {
            val landToTrim = basics.filter { (deckMap[it] ?: 0) > 0 }.maxByOrNull { deckMap[it] ?: 0 } ?: break
            deckMap[landToTrim] = (deckMap[landToTrim] ?: 0) - 1
            if (deckMap[landToTrim] == 0) deckMap.remove(landToTrim)
        }

        logger.info("LLM sealed deck ({} cards): {}", deckMap.values.sum(),
            deckMap.entries.sortedByDescending { it.value }.joinToString(", ") { "${it.value}x ${it.key}" })
        return deckMap
    }

    private fun guessPrimaryBasicLand(deckMap: Map<String, Int>, pool: List<CardDefinition>): String {
        val poolByName = pool.associateBy { it.name }
        val colorCounts = mutableMapOf<Color, Int>()
        for ((name, count) in deckMap) {
            if (name in basics) continue
            val card = poolByName[name] ?: continue
            for (color in card.colors) colorCounts[color] = (colorCounts[color] ?: 0) + count
        }
        return when (colorCounts.maxByOrNull { it.value }?.key) {
            Color.WHITE -> "Plains"
            Color.BLUE -> "Island"
            Color.BLACK -> "Swamp"
            Color.RED -> "Mountain"
            Color.GREEN -> "Forest"
            else -> "Forest"
        }
    }

    private fun AiProperties.toAiConfig() = AiConfig(
        enabled = enabled, mode = mode, baseUrl = baseUrl,
        apiKey = apiKey, openRouterApiKey = openRouterApiKey,
        model = model, deckbuildingModel = deckbuildingModel,
        reasoningEffort = reasoningEffort, maxRetries = maxRetries,
        timeoutMs = timeoutMs, thinkingDelayMs = thinkingDelayMs
    )
}
