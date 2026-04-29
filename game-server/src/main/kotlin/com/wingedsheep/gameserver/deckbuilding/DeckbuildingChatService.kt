package com.wingedsheep.gameserver.deckbuilding

import com.fasterxml.jackson.annotation.JsonInclude
import com.wingedsheep.ai.engine.deck.SetArchetypes
import com.wingedsheep.ai.llm.AiConfig
import com.wingedsheep.ai.llm.ChatMessage as LlmChatMessage
import com.wingedsheep.ai.llm.LlmClient
import com.wingedsheep.gameserver.config.AiProperties
import com.wingedsheep.gameserver.config.GameProperties
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private val logger = LoggerFactory.getLogger(DeckbuildingChatService::class.java)

private const val DEFAULT_DECKBUILDING_MODEL = "minimax/minimax-m2.5:free"
private val BASIC_LAND_NAMES = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")

@Service
class DeckbuildingChatService(
    gameProperties: GameProperties
) {
    private val aiProperties: AiProperties = gameProperties.ai

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun isAvailable(): Boolean = aiProperties.effectiveApiKey.isNotBlank()

    fun respond(request: ChatRequest): ChatResponse {
        val llmConfig = buildLlmConfig()
        val client = LlmClient(llmConfig)

        val systemPrompt = buildSystemPrompt(request)
        val messages = buildList {
            add(LlmChatMessage(role = "system", content = systemPrompt))
            for (msg in request.messages) {
                val role = if (msg.role.equals("assistant", ignoreCase = true)) "assistant" else "user"
                add(LlmChatMessage(role = role, content = msg.content))
            }
        }

        val raw = client.chatCompletion(messages, modelOverride = llmConfig.effectiveDeckbuildingModel)
        if (raw.isNullOrBlank()) {
            return ChatResponse(
                assistantMessage = "Sorry, I couldn't reach the language model just now. Please try again.",
                actions = emptyList(),
                error = "llm_call_failed"
            )
        }

        return parseAndValidate(raw, request)
    }

    private fun buildLlmConfig(): AiConfig {
        // The chat advisor uses the deckbuilding-specific model if set, otherwise a free chat-tuned default.
        // We deliberately don't fall back to the gameplay `model` — gameplay models are tuned for engine
        // turn submission, not free-form natural language.
        val effectiveDeckbuildingModel = aiProperties.deckbuildingModel.ifBlank { DEFAULT_DECKBUILDING_MODEL }
        return AiConfig(
            enabled = true,
            mode = "llm",
            baseUrl = aiProperties.baseUrl,
            apiKey = aiProperties.apiKey,
            openRouterApiKey = aiProperties.openRouterApiKey,
            model = effectiveDeckbuildingModel,
            deckbuildingModel = effectiveDeckbuildingModel,
            reasoningEffort = "medium",
            maxRetries = aiProperties.maxRetries,
            timeoutMs = aiProperties.timeoutMs,
            thinkingDelayMs = aiProperties.thinkingDelayMs
        )
    }

    private fun buildSystemPrompt(request: ChatRequest): String = buildString {
        appendLine("You are a Magic: The Gathering deckbuilding assistant embedded in a sealed/draft deckbuilder UI.")
        appendLine("You help the player understand their card pool, find specific cards, and build decks.")
        appendLine()
        appendLine("PERSONALITY")
        appendLine("- Knowledgeable, friendly, with a dry wit. Think drafting buddy at the LGS, not a rules judge.")
        appendLine("- Roasting is fine — call out a 1/1 vanilla for what it is, raise an eyebrow at a four-color splash, drag a bad rare for being a bad rare. Roast the cards and the choices, never the player. No punching down.")
        appendLine("- When recommending an archetype or color pair, be opinionated. Pick a side, say why, throw in a quip if there's one to be had: \"Your white is a graveyard with extra steps — I'd go Black-Green and lean into it,\" or \"Red-Green has the bombs, the curve, and the dignity; the others have… enthusiasm.\"")
        appendLine("- Light MTG flavor is welcome (mana, planes, the stack, bombs/removal/grindy/curve). Avoid deep lore drops, Vorthos rants, and forced puns.")
        appendLine("- Be concise. One or two short sentences for the message field unless the player asks for detail.")
        appendLine("- Stay genuinely useful — the joke is the seasoning, not the meal. When the player asks a real question, answer it straight (and *then* you can salt it).")
        appendLine()
        appendLine("RESPONSE FORMAT — IMPORTANT")
        appendLine("Reply with a single JSON object on its own line, no markdown fences, no prose outside the JSON.")
        appendLine("Schema:")
        appendLine("{")
        appendLine("  \"message\": string,            // Short human-readable reply shown in chat (1-3 sentences)")
        appendLine("  \"actions\": [                  // Optional list of UI actions to perform")
        appendLine("    {\"type\": \"highlight_cards\", \"card_names\": [\"Name 1\", ...]},")
        appendLine("    {\"type\": \"clear_highlights\"},")
        appendLine("    {\"type\": \"set_deck\",")
        appendLine("       \"main_deck\": [{\"name\": \"Name\", \"count\": 2}, ...],")
        appendLine("       \"lands\": [{\"name\": \"Mountain\", \"count\": 9}, ...] }")
        appendLine("  ]")
        appendLine("}")
        appendLine()
        appendLine("TOOL USE — what each action does and when to fire it")
        appendLine()
        appendLine("The UI: pool of cards on the left, the player's current deck on the right. The player can click cards manually too — you're collaborating on the same deck.")
        appendLine()
        appendLine("1) highlight_cards { card_names: [...] }")
        appendLine("   What it does: Puts a glowing border on those cards in the pool view. The new set REPLACES any prior highlights (it is not additive). Clearing them is done via clear_highlights or by the player clicking an archetype filter.")
        appendLine("   Use it when: the player asks to find/show/highlight something (\"show me the removal\", \"highlight cheap creatures\", \"which cards work with Goblins?\").")
        appendLine("   Tip: highlights are also useful as a soft suggestion before a destructive set_deck — \"these are the cards I'd build around\" is a friendlier first step than rebuilding their deck unprompted.")
        appendLine("   Use exact card names from the POOL list. Don't highlight basic lands.")
        appendLine()
        appendLine("2) clear_highlights {}")
        appendLine("   What it does: Removes the current highlight set from the pool view.")
        appendLine("   Use it when: the player says \"clear highlights\", \"never mind\", \"reset\", or when you're switching topic and prior highlights would now be misleading.")
        appendLine()
        appendLine("3) set_deck { main_deck: [{name, count}], lands: [{name, count}] }")
        appendLine("   What it does: COMPLETELY REPLACES the player's deck and basic land counts in one shot. Destructive — there is no undo prompt.")
        appendLine("   Use it when: the player has clearly asked you to build, rebuild, or swap the deck (\"build me a Blue-Red deck\", \"replace this with mono-red aggro\", \"add 17 lands for me\"). For \"add 17 lands\", combine the player's existing deck with the new lands and submit the full result — set_deck is whole-deck.")
        appendLine("   Do NOT use it when: the player is just asking a question, chatting, or asked you to highlight. \"What archetypes does my pool support?\" is not an instruction to rebuild.")
        appendLine("   Constraints:")
        appendLine("   • Total cards (main_deck counts + lands counts) MUST be ≥ 40. Aim for exactly 40 unless asked otherwise.")
        appendLine("   • Typical sealed mana base: 16–18 lands. Default to 17 if you have no reason to deviate.")
        appendLine("   • Per-card count for non-basics MUST NOT exceed availableCount + that card's current copies in deck (i.e. its totalCount in the pool).")
        appendLine("   • Basic lands (Plains/Island/Swamp/Mountain/Forest) go in \"lands\". Everything else — including non-basic lands from the pool — goes in \"main_deck\".")
        appendLine("   • Use exact names. No nicknames, no abbreviations.")
        appendLine()
        appendLine("If you have nothing to do — pure conversation, a question, an opinion — return actions: []. Empty actions are perfectly valid; not every reply needs to do something.")
        appendLine()
        appendLine("Order of actions: if you both clear and re-highlight, list clear_highlights first. Avoid combining set_deck with highlights in the same turn; the player will be looking at their new deck, not the pool.")
        appendLine()

        val setCodes = request.setCodes.orEmpty()
        if (setCodes.isNotEmpty()) {
            appendLine("SETS: ${setCodes.joinToString(", ")}")
            for (code in setCodes) {
                val syn = SetArchetypes.getForSet(code) ?: continue
                appendLine()
                appendLine("ARCHETYPES for ${syn.setName} (${syn.setCode}):")
                for (arch in syn.archetypes) {
                    val colorStr = arch.colors.joinToString("") { it.name.first().uppercase() }
                    val tribes = if (arch.creatureTypes.isNotEmpty()) " — tribes: ${arch.creatureTypes.joinToString("/")}" else ""
                    appendLine("- ${arch.name} [$colorStr]$tribes: ${arch.description}")
                }
            }
        }

        appendLine()
        appendLine("CARD POOL (available copies = total in pool minus copies currently in deck):")
        if (request.pool.isEmpty()) {
            appendLine("(empty)")
        } else {
            for (card in request.pool) {
                val cost = card.manaCost?.takeIf { it.isNotBlank() } ?: "—"
                val pt = if (card.power != null && card.toughness != null) " ${card.power}/${card.toughness}" else ""
                val text = card.oracleText?.replace("\n", " // ")?.trim().orEmpty()
                appendLine("- ${card.name} [${card.rarity}] ($cost) ${card.typeLine}$pt | available: ${card.availableCount}/${card.totalCount}${if (text.isNotEmpty()) " | $text" else ""}")
                if (card.isDoubleFaced && !card.backFaceName.isNullOrBlank()) {
                    val backType = card.backFaceTypeLine?.takeIf { it.isNotBlank() } ?: "—"
                    val backText = card.backFaceOracleText?.replace("\n", " // ")?.trim().orEmpty()
                    appendLine("    back face: ${card.backFaceName} | $backType${if (backText.isNotEmpty()) " | $backText" else ""}")
                }
            }
        }

        appendLine()
        appendLine("BASIC LANDS available (unlimited):")
        appendLine(if (request.basicLands.isEmpty()) "(unknown — assume Plains/Island/Swamp/Mountain/Forest)"
            else request.basicLands.joinToString(", ") { it.name })

        appendLine()
        appendLine("CURRENT DECK (non-basic):")
        if (request.deck.isEmpty()) appendLine("(empty)") else {
            val grouped = request.deck.groupingBy { it }.eachCount()
            for ((name, count) in grouped.entries.sortedBy { it.key }) {
                appendLine("- ${count}x $name")
            }
        }

        appendLine()
        appendLine("CURRENT BASIC LANDS:")
        val landTotal = request.landCounts.values.sum()
        if (landTotal == 0) appendLine("(none)") else {
            for ((name, count) in request.landCounts.entries.sortedBy { it.key }) {
                if (count > 0) appendLine("- ${count}x $name")
            }
        }

        appendLine()
        appendLine("Total cards in current deck: ${request.deck.size + landTotal}")
    }

    private fun parseAndValidate(raw: String, request: ChatRequest): ChatResponse {
        val jsonObj = extractJsonObject(raw)
        if (jsonObj == null) {
            // Treat the whole response as plain assistant text.
            return ChatResponse(assistantMessage = raw.trim(), actions = emptyList())
        }

        val message = jsonObj["message"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
        val actionsRaw = jsonObj["actions"] as? JsonArray ?: JsonArray(emptyList())

        val poolByName = request.pool.associateBy { it.name }
        val parsedActions = mutableListOf<ChatAction>()
        for (item in actionsRaw) {
            val obj = item as? JsonObject ?: continue
            val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: continue
            when (type) {
                "highlight_cards" -> {
                    val names = (obj["card_names"] as? JsonArray)
                        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.trim() }
                        ?.filter { it.isNotEmpty() }
                        ?.distinct()
                        ?: emptyList()
                    parsedActions.add(ChatAction.HighlightCards(names))
                }
                "clear_highlights" -> parsedActions.add(ChatAction.ClearHighlights())
                "set_deck" -> {
                    // Cap each entry by the pool's total copies (set_deck replaces the deck wholesale).
                    val main = parseNameCountList(obj["main_deck"] as? JsonArray).mapNotNull { entry ->
                        val info = poolByName[entry.name] ?: return@mapNotNull null
                        val capped = entry.count.coerceIn(0, info.totalCount)
                        if (capped > 0) NameCount(entry.name, capped) else null
                    }
                    val lands = parseNameCountList(obj["lands"] as? JsonArray)
                        .filter { it.name in BASIC_LAND_NAMES && it.count > 0 }
                    parsedActions.add(ChatAction.SetDeck(main, lands))
                }
                else -> logger.debug("Unknown action type from LLM: {}", type)
            }
        }

        val finalMessage = message.ifBlank {
            // Some models forget the message field — fall back to a brief acknowledgement.
            if (parsedActions.isEmpty()) raw.trim() else "Done."
        }
        return ChatResponse(assistantMessage = finalMessage, actions = parsedActions)
    }

    private fun parseNameCountList(arr: JsonArray?): List<NameCount> {
        if (arr == null) return emptyList()
        val result = mutableListOf<NameCount>()
        for (item in arr) {
            val obj = item as? JsonObject ?: continue
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim() ?: continue
            val count = obj["count"]?.jsonPrimitive?.intOrNull ?: 1
            if (name.isNotEmpty() && count > 0) result.add(NameCount(name, count))
        }
        return result
    }

    private fun extractJsonObject(raw: String): JsonObject? {
        val trimmed = raw.trim()
        // Direct parse
        runCatching { return json.parseToJsonElement(trimmed).jsonObject }
        // Try to find the first {...} block (handles markdown fences or trailing text)
        val first = trimmed.indexOf('{')
        val last = trimmed.lastIndexOf('}')
        if (first >= 0 && last > first) {
            val candidate = trimmed.substring(first, last + 1)
            runCatching { return json.parseToJsonElement(candidate).jsonObject }
        }
        return null
    }

    // ============================================================
    // DTOs
    // ============================================================

    data class ChatRequest(
        val messages: List<ChatMessageDTO> = emptyList(),
        val pool: List<PoolCardDTO> = emptyList(),
        val basicLands: List<BasicLandDTO> = emptyList(),
        val deck: List<String> = emptyList(),
        val landCounts: Map<String, Int> = emptyMap(),
        val setCodes: List<String>? = null
    )

    data class ChatMessageDTO(
        val role: String,
        val content: String
    )

    data class PoolCardDTO(
        val name: String,
        val manaCost: String? = null,
        val typeLine: String = "",
        val rarity: String = "COMMON",
        val oracleText: String? = null,
        val power: Int? = null,
        val toughness: Int? = null,
        val isDoubleFaced: Boolean = false,
        val backFaceName: String? = null,
        val backFaceTypeLine: String? = null,
        val backFaceOracleText: String? = null,
        val totalCount: Int = 1,
        val availableCount: Int = 1
    )

    data class BasicLandDTO(val name: String)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class ChatResponse(
        val assistantMessage: String,
        val actions: List<ChatAction>,
        val error: String? = null
    )

    sealed interface ChatAction {
        val type: String

        data class HighlightCards(val cardNames: List<String>) : ChatAction {
            override val type: String = "highlight_cards"
        }

        class ClearHighlights : ChatAction {
            override val type: String = "clear_highlights"
        }

        data class SetDeck(
            val mainDeck: List<NameCount>,
            val lands: List<NameCount>
        ) : ChatAction {
            override val type: String = "set_deck"
        }
    }

    data class NameCount(val name: String, val count: Int)
}
