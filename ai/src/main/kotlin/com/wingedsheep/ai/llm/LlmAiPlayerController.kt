package com.wingedsheep.ai.llm

import com.wingedsheep.ai.ActionResponse
import com.wingedsheep.ai.AiPlayerController
import com.wingedsheep.ai.llm.decision.AiDecisionHandlerRegistry
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(LlmAiPlayerController::class.java)

/**
 * LLM-based AI controller that sends game state to an LLM API and parses responses.
 *
 * Maintains a rolling conversation history with the LLM and handles
 * retries with fallback to an optional [fallback] controller (typically the engine AI).
 */
class LlmAiPlayerController(
    private val properties: AiConfig,
    private val llmClient: LlmClient,
    private val playerId: EntityId,
    private val fallback: AiPlayerController? = null
) : AiPlayerController {
    private val decisionRegistry = AiDecisionHandlerRegistry()
    private val formatter = GameStateFormatter(decisionRegistry)
    private val parser = AiResponseParser()

    private val conversationHistory = mutableListOf<ChatMessage>()
    private val maxHistorySize = 10

    // Draft-specific conversation history for prompt caching.
    // Accumulates pick-by-pick exchanges so the message prefix is stable and cacheable.
    private val draftHistory = mutableListOf<ChatMessage>()

    init {
        // Mark system prompt with an explicit cache breakpoint — this content is identical
        // for every game and should be cached across requests.
        conversationHistory.add(ChatMessage("system", SYSTEM_PROMPT, cacheControl = CacheControl()))
    }

    /**
     * Provide the AI with its deck composition so it can make informed decisions.
     * Called once when a game starts. The AI knows what cards are in the deck but not their order.
     */
    override fun setDeckList(deckList: Map<String, Int>, archetype: String?) {
        val deckDescription = buildString {
            if (archetype != null) {
                appendLine("=== YOUR DECK ARCHETYPE ===")
                appendLine(archetype)
                appendLine()
            }
            appendLine("=== YOUR DECK (${deckList.values.sum()} cards) ===")
            appendLine("You know the cards in your deck but not their order.")
            appendLine()
            val sorted = deckList.entries.sortedByDescending { it.value }
            for ((name, count) in sorted) {
                appendLine("  ${count}x $name")
            }
        }
        // Insert as a system-level context message right after the main system prompt.
        // Explicit cache breakpoint: deck context is stable for the entire game and should
        // be cached. This supersedes the system prompt breakpoint as the outermost cached prefix.
        conversationHistory.add(ChatMessage("system", deckDescription, cacheControl = CacheControl()))
        logger.info("AI deck context set: {} unique cards, {} total", deckList.size, deckList.values.sum())
    }

    // =========================================================================
    // Public decision methods
    // =========================================================================

    /**
     * Choose an action from the list of legal actions.
     * Returns the chosen GameAction, or null to auto-pass.
     */
    override fun chooseAction(
        state: ClientGameState,
        legalActions: List<LegalActionInfo>,
        pendingDecision: PendingDecision?,
        recentGameLog: List<String>
    ): ActionResponse {
        // Shortcut: only one legal action (usually PassPriority)
        // Exception: DeclareAttackers/DeclareBlockers come as the only action but need
        // their attacker/blocker maps filled in — don't auto-submit with empty maps.
        if (legalActions.size == 1 && pendingDecision == null) {
            val onlyAction = legalActions[0]
            if (onlyAction.actionType != "DeclareAttackers" && onlyAction.actionType != "DeclareBlockers") {
                logger.info("AI auto-selecting only legal action: {}", onlyAction.actionType)
                return ActionResponse.SubmitAction(onlyAction.action)
            }
        }

        // Shortcut: pending decision with auto-resolve via handler
        if (pendingDecision != null) {
            val handler = decisionRegistry.getHandler(pendingDecision)
            if (handler != null) {
                if (handler.canAutoResolve(pendingDecision)) {
                    logger.info("AI auto-resolved decision: {}", pendingDecision::class.simpleName)
                    return ActionResponse.SubmitDecision(playerId, handler.autoResolve(pendingDecision))
                }
            }
        }

        // Shortcut: handle combat declarations heuristically (LLM can't build attacker/blocker maps)
        val combatAction = tryCombatAction(state, legalActions)
        if (combatAction != null) return combatAction

        // Filter out mana abilities — the engine auto-pays mana when casting spells,
        // so manually tapping lands is never useful and wastes resources.
        val filteredActions = legalActions.filter { !it.isManaAbility }

        // If filtering removed everything except pass, just pass
        if (filteredActions.size == 1 && filteredActions[0].actionType == "PassPriority") {
            logger.info("AI auto-passing: only non-mana action is PassPriority")
            return ActionResponse.SubmitAction(filteredActions[0].action)
        }

        // Format state and query LLM
        val prompt = formatter.format(state, filteredActions, pendingDecision, recentGameLog)
        logger.info("AI prompt ({} chars):\n{}", prompt.length, prompt)

        // Try LLM with one retry on parse failure
        for (attempt in 0..1) {
            val queryPrompt = if (attempt == 0) {
                prompt
            } else {
                "Invalid response. Reply with ONLY your choice. Example: B"
            }
            val response = queryLlm(queryPrompt)

            if (response != null) {
                logger.info("AI LLM response (attempt {}): {}", attempt, response)
                val parsed = if (pendingDecision != null) {
                    parseDecisionResponse(response, pendingDecision, state)
                } else {
                    parseActionResponse(response, filteredActions, state)
                }
                if (parsed != null) {
                    // Validate: reject unaffordable actions
                    if (parsed is ActionResponse.SubmitAction) {
                        val chosenAction = filteredActions.find { it.action == parsed.action }
                            ?: filteredActions.find { it.action::class == parsed.action::class }
                        if (chosenAction != null && !chosenAction.isAffordable) {
                            logger.warn("AI chose unaffordable action: {}, retrying", chosenAction.description)
                            continue
                        }
                    }
                    logger.info("AI parsed LLM response successfully: {}", when (parsed) {
                        is ActionResponse.SubmitAction -> "Action(${parsed.action::class.simpleName})"
                        is ActionResponse.SubmitDecision -> "Decision(${parsed.response::class.simpleName})"
                    })
                    return parsed
                }
                logger.warn("AI failed to parse LLM response (attempt {})", attempt)
            } else {
                logger.warn("AI LLM returned null (attempt {})", attempt)
                break // No point retrying if the API itself failed
            }
        }

        // Fallback: delegate to engine AI if available
        if (fallback != null) {
            logger.warn("AI falling back to engine AI after LLM failure")
            return fallback.chooseAction(state, legalActions, pendingDecision, recentGameLog)
        }

        // Last-resort: no fallback configured, just pass
        logger.warn("AI no fallback configured, passing priority")
        val passAction = legalActions.find { it.actionType == "PassPriority" }
        if (passAction != null) return ActionResponse.SubmitAction(passAction.action)
        return ActionResponse.SubmitAction(legalActions.first().action)
    }

    /**
     * Decide whether to keep or mulligan.
     * Returns true to keep, false to mulligan.
     */
    override fun decideMulligan(mulliganMessage: MulliganInfo): Boolean {
        val cards = mulliganMessage.cards
        val cardDisplays = mulliganMessage.hand.map { entityId ->
            val info = cards[entityId]
            MulliganCardDisplay(
                name = info?.name ?: "Unknown",
                manaCost = info?.manaCost,
                typeLine = info?.typeLine,
                power = info?.power,
                toughness = info?.toughness,
                oracleText = info?.oracleText
            )
        }
        val prompt = formatter.formatMulligan(cardDisplays, mulliganMessage.mulliganCount, mulliganMessage.isOnThePlay)

        // Shortcut: always keep if mulliganCount >= 3 (5 or fewer cards)
        if (mulliganMessage.mulliganCount >= 3) {
            logger.info("AI keeping hand after ${mulliganMessage.mulliganCount} mulligans (auto-keep)")
            return true
        }

        logger.info("AI mulligan prompt ({} chars):\n{}", prompt.length, prompt)
        val response = queryLlmEphemeral(prompt)
        if (response != null) {
            val cleaned = extractAnswer(response) ?: response
            val choice = parser.parseMulliganChoice(cleaned)
            if (choice != null) {
                logger.info("AI mulligan decision: ${if (choice) "keep" else "mulligan"}")
                return choice
            }
        }

        // Fallback: delegate to engine AI
        if (fallback != null) {
            logger.warn("AI mulligan falling back to engine AI")
            return fallback.decideMulligan(mulliganMessage)
        }

        // Last-resort: keep if mulliganCount >= 2
        val keep = mulliganMessage.mulliganCount >= 2
        logger.info("AI mulligan last-resort: ${if (keep) "keep" else "mulligan"}")
        return keep
    }

    /**
     * Choose cards to put on bottom after mulligan.
     * Returns entity IDs of cards to bottom.
     */
    override fun chooseBottomCards(message: BottomCardsInfo): List<EntityId> {
        val cardCount = message.cardsToPutOnBottom
        val hand = message.hand

        if (cardCount <= 0 || hand.isEmpty()) return emptyList()

        // If we have card info, ask the LLM
        if (message.cards.isNotEmpty()) {
            val cardDisplays = hand.map { entityId ->
                val info = message.cards[entityId]
                MulliganCardDisplay(
                    name = info?.name ?: "Unknown",
                    manaCost = info?.manaCost,
                    typeLine = info?.typeLine,
                    power = info?.power,
                    toughness = info?.toughness
                )
            }
            val prompt = formatter.formatChooseBottomCards(cardDisplays, hand, cardCount)
            logger.info("AI bottom cards prompt ({} chars):\n{}", prompt.length, prompt)
            val response = queryLlmEphemeral(prompt)
            if (response != null) {
                val indices = parser.parseMultipleSelections(response, hand.size - 1)
                if (indices != null && indices.size == cardCount) {
                    val bottomCards = indices.map { hand[it] }
                    logger.info("AI LLM chose bottom cards: {}", bottomCards)
                    return bottomCards
                }
            }
        }

        // Fallback: delegate to engine AI
        if (fallback != null) {
            logger.warn("AI bottom cards falling back to engine AI")
            return fallback.chooseBottomCards(message)
        }

        // Last-resort: bottom the last N cards
        return hand.takeLast(cardCount)
    }

    // =========================================================================
    // LLM interaction
    // =========================================================================

    private fun queryLlm(prompt: String): String? {
        conversationHistory.add(ChatMessage("user", prompt))

        // Trim history if too long — keep all system messages (prompt + deck context)
        // and the last N user/assistant exchanges
        while (conversationHistory.size > maxHistorySize * 2 + 2) {
            // Find the first non-system message and remove it
            val firstNonSystem = conversationHistory.indexOfFirst { it.role != "system" }
            if (firstNonSystem >= 0) {
                conversationHistory.removeAt(firstNonSystem)
            } else {
                break
            }
        }

        val response = llmClient.chatCompletion(conversationHistory, cacheControl = CacheControl())

        if (response != null) {
            conversationHistory.add(ChatMessage("assistant", response))
        } else {
            // Remove the unanswered user message
            conversationHistory.removeLastOrNull()
        }

        return response
    }

    /**
     * Query the LLM without persisting the exchange in conversation history.
     * Used for combat declarations, mulligans, and other self-contained prompts
     * that don't benefit from — and would pollute — the main conversation context.
     */
    private fun queryLlmEphemeral(prompt: String): String? {
        // Build a temporary message list: system messages + this prompt only
        val messages = conversationHistory.filter { it.role == "system" }.toMutableList()
        messages.add(ChatMessage("user", prompt))
        return llmClient.chatCompletion(messages, cacheControl = CacheControl())
    }

    // =========================================================================
    // Response parsing
    // =========================================================================

    private fun parseActionResponse(response: String, legalActions: List<LegalActionInfo>, state: ClientGameState? = null): ActionResponse? {
        // Extract <answer> tags first to isolate the actual choice from reasoning text
        val cleaned = extractAnswer(response) ?: response

        // Try parsing action + target together (e.g., "C2")
        val actionWithTarget = parseActionWithTarget(cleaned, legalActions, state)
        if (actionWithTarget != null) return actionWithTarget

        val index = parser.parseActionChoice(cleaned, legalActions.size - 1) ?: return null
        val chosen = legalActions[index]
        val action = maybeAddTargets(chosen, state)
        return ActionResponse.SubmitAction(action)
    }

    /**
     * Parse responses like "C2", "C 2", "C, target 2" where the letter is the action
     * and the number is the target index (1-based).
     */
    private fun parseActionWithTarget(
        response: String,
        legalActions: List<LegalActionInfo>,
        state: ClientGameState?
    ): ActionResponse? {
        val cleaned = response.trim().removeSurrounding("[", "]").trim().uppercase()
        // Match patterns: "C2", "C 2", "C,2", "C, 2", "[C2]", "[C 2]"
        val pattern = Regex("""^([A-Z]{1,2})\s*[,:]?\s*(\d+)\s*$""")
        val match = pattern.find(cleaned) ?: return null

        val actionIndex = GameStateFormatter.letterToIndex(match.groupValues[1]) ?: return null
        val targetNum = match.groupValues[2].toIntOrNull()?.minus(1) ?: return null // 1-based to 0-based

        if (actionIndex >= legalActions.size) return null
        val chosen = legalActions[actionIndex]

        if (!chosen.requiresTargets) return null

        val targetReqs = chosen.targetRequirements
        val chosenValidTargets = chosen.validTargets
        val allValidTargets = if (!targetReqs.isNullOrEmpty()) {
            targetReqs.flatMap { req -> req.validTargets }
        } else if (!chosenValidTargets.isNullOrEmpty()) {
            chosenValidTargets
        } else {
            return null
        }

        if (targetNum !in allValidTargets.indices) return null

        val targetId = allValidTargets[targetNum]
        val playerIds = state?.players?.map { it.playerId }?.toSet() ?: emptySet()
        val target = resolveTargetType(targetId, null, playerIds, state)

        val action = when (val base = chosen.action) {
            is CastSpell -> base.copy(targets = listOf(target))
            is ActivateAbility -> base.copy(targets = listOf(target))
            else -> return null
        }

        logger.info("AI parsed action+target: action={}, target={} ({})",
            chosen.description, targetId.value, target::class.simpleName)
        return ActionResponse.SubmitAction(action)
    }

    /**
     * If a legal action requires targets, auto-select the first valid target for each requirement
     * and return the action with targets filled in.
     */
    private fun maybeAddTargets(legalAction: LegalActionInfo, state: ClientGameState? = null): GameAction {
        if (!legalAction.requiresTargets) return legalAction.action

        val playerIds = state?.players?.map { it.playerId }?.toSet() ?: emptySet()
        val targets = mutableListOf<ChosenTarget>()

        val laTargetReqs = legalAction.targetRequirements
        val laValidTargets = legalAction.validTargets
        if (!laTargetReqs.isNullOrEmpty()) {
            for (req in laTargetReqs) {
                val validTargets = req.validTargets
                if (validTargets.isEmpty()) continue
                val targetId = pickBestTarget(validTargets, legalAction, state)
                val target = resolveTargetType(targetId, req.targetZone, playerIds)
                targets.add(target)
                logger.info("AI auto-targeting req {}: {} -> {} ({})", req.index, req.description, targetId.value, target::class.simpleName)
            }
        } else if (!laValidTargets.isNullOrEmpty()) {
            val targetId = pickBestTarget(laValidTargets, legalAction, state)
            val target = resolveTargetType(targetId, null, playerIds, state)
            targets.add(target)
            logger.info("AI auto-targeting (single): {} -> {} ({})",
                legalAction.targetDescription ?: legalAction.description, targetId.value, target::class.simpleName)
        }

        if (targets.isEmpty()) {
            logger.warn("AI maybeAddTargets: requiresTargets=true but no valid targets found for {}",
                legalAction.description)
            return legalAction.action
        }

        logger.info("AI targeting: {} targets for {}", targets.size, legalAction.description)

        return when (val action = legalAction.action) {
            is CastSpell -> action.copy(targets = targets)
            is ActivateAbility -> action.copy(targets = targets)
            else -> action
        }
    }

    /**
     * Heuristic target selection when the LLM didn't specify a target.
     *
     * For harmful effects (destroy, damage, exile, -X/-X, etc.) → prefer opponent's creatures.
     * For beneficial effects (pump, prevent, protect, etc.) → prefer own creatures.
     * Falls back to first valid target if we can't determine the effect type.
     */
    private fun pickBestTarget(
        validTargets: List<EntityId>,
        legalAction: LegalActionInfo,
        state: ClientGameState?
    ): EntityId {
        if (validTargets.size == 1 || state == null) return validTargets.first()

        val description = legalAction.description.lowercase() +
            " " + (legalAction.targetDescription?.lowercase() ?: "")

        // Also check the oracle text of the card being cast for effect classification
        val oracleText = legalAction.action.let { action ->
            if (action is CastSpell) {
                state.cards[action.cardId]?.oracleText?.lowercase() ?: ""
            } else ""
        }
        val fullText = "$description $oracleText"

        val isHarmful = fullText.containsAny(
            "destroy", "damage", "exile", "sacrifice", "return to",
            "-1/-1", "-2/-2", "-3/-3", "-4/-4", "-5/-5",
            "debilitating", "murder", "kill", "burn", "remove",
            "loses", "can't attack", "can't block", "tap target"
        )
        val isBeneficial = fullText.containsAny(
            "prevent", "protect", "regenerate", "+1/+1", "+2/+2", "+3/+3",
            "+1/+0", "+2/+0", "+3/+0", "+0/+1", "+0/+2", "+0/+3",
            "pump", "buff", "indestructible", "hexproof", "counter on",
            "equip", "enchant", "attach", "gets +", "gains ",
            "first strike", "haste", "flying", "trample", "lifelink",
            "vigilance", "deathtouch", "double strike", "unblockable"
        )

        val myId = state.viewingPlayerId

        // Separate targets into own vs opponent's
        val opponentTargets = validTargets.filter { tid ->
            val card = state.cards[tid]
            card != null && card.controllerId != myId
        }
        val ownTargets = validTargets.filter { tid ->
            val card = state.cards[tid]
            card != null && card.controllerId == myId
        }

        val preferred = when {
            isBeneficial && ownTargets.isNotEmpty() -> ownTargets
            isHarmful && opponentTargets.isNotEmpty() -> opponentTargets
            // Default: for spells WE cast, assume harmful → target opponent
            !isBeneficial && opponentTargets.isNotEmpty() -> opponentTargets
            else -> validTargets
        }

        // Among preferred targets, pick the biggest threat (highest power)
        val best = preferred.maxByOrNull { tid ->
            state.cards[tid]?.power ?: 0
        } ?: preferred.first()

        logger.info("AI pickBestTarget: harmful={}, beneficial={}, chose {} from {} valid targets",
            isHarmful, isBeneficial, state.cards[best]?.name ?: best.value, validTargets.size)

        return best
    }

    private fun String.containsAny(vararg keywords: String): Boolean =
        keywords.any { this.contains(it) }

    private fun extractAnswer(response: String): String? {
        val match = Regex("""<answer>(.*?)</answer>""", RegexOption.DOT_MATCHES_ALL).find(response)
        return match?.groupValues?.get(1)?.trim()
    }

    /**
     * Determine the correct ChosenTarget variant based on zone and entity type.
     */
    private fun resolveTargetType(
        targetId: EntityId,
        targetZone: String?,
        playerIds: Set<EntityId>,
        state: ClientGameState? = null
    ): ChosenTarget {
        if (targetId in playerIds) return ChosenTarget.Player(targetId)

        if (targetZone != null) {
            return when (targetZone) {
                "Stack" -> ChosenTarget.Spell(targetId)
                else -> ChosenTarget.Permanent(targetId)
            }
        }

        if (state != null) {
            val stackZone = state.zones.find { it.zoneId.zoneType == com.wingedsheep.sdk.core.Zone.STACK }
            if (stackZone != null && targetId in stackZone.cardIds) {
                return ChosenTarget.Spell(targetId)
            }
        }

        return ChosenTarget.Permanent(targetId)
    }

    private fun parseDecisionResponse(
        response: String,
        decision: PendingDecision,
        state: ClientGameState
    ): ActionResponse? {
        // Extract <answer> tags to isolate the choice from reasoning text
        val cleaned = extractAnswer(response) ?: response
        val handler = decisionRegistry.getHandler(decision) ?: return null
        val decisionResponse = handler.parse(cleaned, decision, state, parser) ?: return null
        return ActionResponse.SubmitDecision(playerId, decisionResponse)
    }

    /**
     * Handle combat declarations by asking the LLM which creatures to attack/block with.
     * Falls back to heuristics if parsing fails.
     */
    private fun tryCombatAction(
        state: ClientGameState,
        legalActions: List<LegalActionInfo>
    ): ActionResponse? {
        val declareAttackers = legalActions.find { it.actionType == "DeclareAttackers" }
        if (declareAttackers != null) {
            val validAttackers = declareAttackers.validAttackers ?: emptyList()
            if (validAttackers.isEmpty()) {
                logger.info("AI combat: no valid attackers, declaring no attacks")
                return ActionResponse.SubmitAction(declareAttackers.action)
            }

            val opponentId = state.players.find { it.playerId != state.viewingPlayerId }?.playerId
                ?: return ActionResponse.SubmitAction(declareAttackers.action)

            // Build attack target list: opponent player + any planeswalkers that can be attacked
            val validAttackTargets = declareAttackers.validAttackTargets ?: emptyList()
            val planeswalkerTargets = validAttackTargets.filter { it != opponentId }
                .mapNotNull { pwId -> state.cards[pwId]?.let { pwId to it } }

            val you = state.players.find { it.playerId == state.viewingPlayerId }
            val opp = state.players.find { it.playerId != state.viewingPlayerId }

            val prompt = buildString {
                appendLine("=== DECLARE ATTACKERS ===")
                appendLine("Your life: ${you?.life ?: "?"} | Opponent's life: ${opp?.life ?: "?"}")
                appendLine("Choose which creatures to attack with. The opponent can block with their untapped creatures.")
                appendLine("Note: Creatures with summoning sickness cannot attack, but they CAN block.")
                appendLine()

                // Show attack targets if there are planeswalkers
                if (planeswalkerTargets.isNotEmpty()) {
                    appendLine("Attack targets:")
                    appendLine("  [P] Opponent (life: ${opp?.life ?: "?"})")
                    for ((i, pwPair) in planeswalkerTargets.withIndex()) {
                        val (_, pw) = pwPair
                        val loyaltyStr = pw.counters.entries
                            .find { it.key.name.equals("LOYALTY", ignoreCase = true) }
                            ?.let { " (loyalty: ${it.value})" } ?: ""
                        appendLine("  [${i + 1}] ${pw.name}$loyaltyStr")
                    }
                    appendLine()
                }

                appendLine("Your creatures that can attack:")
                for ((i, attackerId) in validAttackers.withIndex()) {
                    val card = state.cards[attackerId] ?: continue
                    val keywords = card.keywords.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name.lowercase() } ?: ""
                    val keywordStr = if (keywords.isNotEmpty()) " [$keywords]" else ""
                    val abilityFlags = card.abilityFlags.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.displayName } ?: ""
                    val flagStr = if (abilityFlags.isNotEmpty()) " [$abilityFlags]" else ""
                    val oracle = card.oracleText.takeIf { it.isNotBlank() }?.let { " — \"${it.flattenOracle()}\"" } ?: ""
                    appendLine("  [${GameStateFormatter.actionLetter(i)}] ${card.name} ${card.power}/${card.toughness}$keywordStr$flagStr$oracle")
                }
                val opponentCreatures = state.cards.values.filter {
                    it.controllerId == opponentId && "Creature" in it.cardTypes && !it.isTapped
                }
                if (opponentCreatures.isNotEmpty()) {
                    appendLine()
                    appendLine("Opponent's untapped creatures (potential blockers):")
                    for (card in opponentCreatures) {
                        val keywords = card.keywords.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name.lowercase() } ?: ""
                        val keywordStr = if (keywords.isNotEmpty()) " [$keywords]" else ""
                        val oracle = card.oracleText.takeIf { it.isNotBlank() }?.let { " — \"${it.flattenOracle()}\"" } ?: ""
                        appendLine("  ${card.name} ${card.power}/${card.toughness}$keywordStr$oracle")
                    }
                } else {
                    appendLine()
                    appendLine("Opponent has NO untapped creatures to block.")
                }
                appendLine()
                appendLine("Reply with ONLY the creature letters to attack with, or NONE.")
                if (planeswalkerTargets.isNotEmpty()) {
                    appendLine("By default creatures attack the opponent. To attack a planeswalker, use \"letter->number\" (e.g., \"C->1\" means creature C attacks planeswalker 1).")
                    appendLine("Example: A, C->1")
                } else {
                    appendLine("Example: A, C")
                }
            }

            logger.info("AI attack prompt ({} chars):\n{}", prompt.length, prompt)
            val response = queryLlmEphemeral(prompt)
            val attackerMap = mutableMapOf<EntityId, EntityId>()

            if (response != null) {
                logger.info("AI combat LLM response: {}", response)
                val answerText = extractAnswer(response) ?: response
                val upper = answerText.trim().uppercase()
                if (upper != "NONE" && upper != "PASS" && upper != "NO") {
                    // Parse attacker assignments, supporting "A, B->1, C" format
                    val assignmentPattern = Regex("""([A-Z])\s*(?:->|→)\s*(\d+)""", RegexOption.IGNORE_CASE)
                    val assignmentMatches = assignmentPattern.findAll(answerText).associate { m ->
                        val attackerIdx = GameStateFormatter.letterToIndex(m.groupValues[1])
                        val pwNum = m.groupValues[2].toIntOrNull()?.minus(1)
                        attackerIdx to pwNum
                    }

                    val indices = parser.parseMultipleSelections(answerText, validAttackers.size - 1)
                    if (indices != null) {
                        for (idx in indices) {
                            if (idx < validAttackers.size) {
                                // Check if this attacker has a planeswalker assignment
                                val pwIdx = assignmentMatches[idx]
                                val targetId = if (pwIdx != null && pwIdx in planeswalkerTargets.indices) {
                                    planeswalkerTargets[pwIdx].first
                                } else {
                                    opponentId
                                }
                                attackerMap[validAttackers[idx]] = targetId
                            }
                        }
                    } else {
                        logger.info("AI combat: failed to parse attacker selection, attacking with all")
                        for (attackerId in validAttackers) {
                            val card = state.cards[attackerId] ?: continue
                            if ((card.power ?: 0) > 0) attackerMap[attackerId] = opponentId
                        }
                    }
                }
            } else {
                logger.info("AI combat: LLM failed, attacking with all")
                for (attackerId in validAttackers) {
                    val card = state.cards[attackerId] ?: continue
                    if ((card.power ?: 0) > 0) attackerMap[attackerId] = opponentId
                }
            }

            val attackerNames = attackerMap.entries.mapNotNull { (eid, targetId) ->
                val name = state.cards[eid]?.name ?: return@mapNotNull null
                val targetName = if (targetId == opponentId) "" else state.cards[targetId]?.name?.let { " -> $it" } ?: ""
                "$name$targetName"
            }
            logger.info("AI combat: attacking with {} creatures: {}", attackerMap.size, attackerNames.joinToString(", "))
            return ActionResponse.SubmitAction(DeclareAttackers(playerId, attackerMap))
        }

        val declareBlockers = legalActions.find { it.actionType == "DeclareBlockers" }
        if (declareBlockers != null) {
            val validBlockers = declareBlockers.validBlockers ?: emptyList()
            if (validBlockers.isEmpty()) {
                logger.info("AI combat: no valid blockers, declaring no blocks")
                return ActionResponse.SubmitAction(declareBlockers.action)
            }

            val combat = state.combat
            if (combat == null || combat.attackers.isEmpty()) {
                logger.info("AI combat: no attackers to block, declaring no blocks")
                return ActionResponse.SubmitAction(declareBlockers.action)
            }

            val you = state.players.find { it.playerId == state.viewingPlayerId }
            val opp = state.players.find { it.playerId != state.viewingPlayerId }
            val totalAttackPower = combat.attackers.sumOf { atk ->
                state.cards[atk.creatureId]?.power ?: 0
            }

            val prompt = buildString {
                appendLine("=== DECLARE BLOCKERS ===")
                appendLine("Your life: ${you?.life ?: "?"} | Opponent's life: ${opp?.life ?: "?"}")
                appendLine("Opponent is attacking for $totalAttackPower total damage. Choose how to block.")
                appendLine("Note: Creatures with summoning sickness CAN block — only attacking is restricted.")
                if (you != null && totalAttackPower >= you.life) {
                    appendLine("WARNING: This attack is LETHAL if unblocked! You must block to survive!")
                }
                appendLine()
                appendLine("Attacking creatures:")
                for ((i, attacker) in combat.attackers.withIndex()) {
                    val card = state.cards[attacker.creatureId]
                    val stats = if (card != null) "${card.power}/${card.toughness}" else "?/?"
                    val keywords = card?.keywords?.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name.lowercase() } ?: ""
                    val keywordStr = if (keywords.isNotEmpty()) " [$keywords]" else ""
                    val abilityFlags = card?.abilityFlags?.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.displayName } ?: ""
                    val flagStr = if (abilityFlags.isNotEmpty()) " [$abilityFlags]" else ""
                    val oracle = card?.oracleText?.takeIf { it.isNotBlank() }?.let { " — \"${it.flattenOracle()}\"" } ?: ""
                    appendLine("  Attacker ${i + 1}: ${attacker.creatureName} $stats$keywordStr$flagStr$oracle")
                }
                appendLine()
                appendLine("Your creatures that can block:")
                for ((i, blockerId) in validBlockers.withIndex()) {
                    val card = state.cards[blockerId] ?: continue
                    val keywords = card.keywords.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name.lowercase() } ?: ""
                    val keywordStr = if (keywords.isNotEmpty()) " [$keywords]" else ""
                    val oracle = card.oracleText.takeIf { it.isNotBlank() }?.let { " — \"${it.flattenOracle()}\"" } ?: ""
                    appendLine("  [${GameStateFormatter.actionLetter(i)}] ${card.name} ${card.power}/${card.toughness}$keywordStr$oracle")
                }
                appendLine()
                appendLine("Reply with ONLY the blocking assignments, or NONE to not block.")
                appendLine("Format: <blocker letter> blocks <attacker number>")
                appendLine("Multiple blockers CAN block the same attacker (gang-block): \"A blocks 1, B blocks 1\"")
                appendLine("Example: A blocks 1, B blocks 1, C blocks 2")
            }

            logger.info("AI block prompt ({} chars):\n{}", prompt.length, prompt)
            val response = queryLlmEphemeral(prompt)
            val blockerMap = mutableMapOf<EntityId, List<EntityId>>()

            if (response != null) {
                logger.info("AI combat LLM response: {}", response)
                val answerText = extractAnswer(response) ?: response
                val upper = answerText.trim().uppercase()
                if (upper != "NONE" && upper != "PASS" && upper != "NO") {
                    val blockPattern = Regex("""([A-Z])\s*(?:blocks?|->|→|:)\s*(\d+)""", RegexOption.IGNORE_CASE)
                    val matches = blockPattern.findAll(answerText).toList()
                    // Group by blocker — each blocker blocks one attacker (the blockerMap key is blocker, value is list of attackers)
                    for (m in matches) {
                        val blockerIdx = GameStateFormatter.letterToIndex(m.groupValues[1])
                        val attackerNum = m.groupValues[2].toIntOrNull()?.minus(1)
                        if (blockerIdx != null && blockerIdx < validBlockers.size &&
                            attackerNum != null && attackerNum < combat.attackers.size) {
                            val blockerId = validBlockers[blockerIdx]
                            val attackerId = combat.attackers[attackerNum].creatureId
                            blockerMap[blockerId] = listOf(attackerId)
                            logger.info("AI combat: {} blocks {}",
                                state.cards[blockerId]?.name, state.cards[attackerId]?.name)
                        }
                    }
                    if (matches.isEmpty()) {
                        val indices = parser.parseMultipleSelections(answerText, validBlockers.size - 1)
                        if (indices != null && combat.attackers.isNotEmpty()) {
                            val firstAttacker = combat.attackers.first().creatureId
                            for (idx in indices) {
                                if (idx < validBlockers.size) {
                                    blockerMap[validBlockers[idx]] = listOf(firstAttacker)
                                }
                            }
                        }
                    }
                }
            }

            if (blockerMap.isEmpty()) {
                logger.info("AI combat: no blocks declared")
            }

            return ActionResponse.SubmitAction(DeclareBlockers(playerId, blockerMap))
        }

        return null
    }

    // =========================================================================
    // Draft Picking (LLM)
    // =========================================================================

    override fun chooseDraftPick(
        pack: List<CardSummary>,
        pickedSoFar: List<CardSummary>,
        packNumber: Int,
        pickNumber: Int,
        picksRequired: Int,
        passDirection: String
    ): List<String> {
        // Initialize draft history with system prompt on first pick.
        // Explicit cache breakpoint on the system prompt — stable across all picks.
        if (draftHistory.isEmpty()) {
            draftHistory.add(ChatMessage("system", DRAFT_SYSTEM_PROMPT, cacheControl = CacheControl()))
        }

        val prompt = formatDraftPickPrompt(pack, pickedSoFar, packNumber, pickNumber, picksRequired, passDirection)
        draftHistory.add(ChatMessage("user", prompt))
        logger.info("AI draft pick prompt ({} chars, {} messages in history)", prompt.length, draftHistory.size)

        val response = llmClient.chatCompletion(draftHistory, cacheControl = CacheControl())
        if (response != null) {
            draftHistory.add(ChatMessage("assistant", response))
            val answer = extractAnswer(response) ?: response
            val picked = parseDraftPickResponse(answer, pack, picksRequired)
            if (picked.isNotEmpty()) {
                logger.info("AI LLM draft pick P{}p{}: {}", packNumber, pickNumber, picked.joinToString(", "))
                return picked
            }
            logger.warn("AI failed to parse draft pick LLM response: {}", response)
        } else {
            // Remove unanswered user message
            draftHistory.removeLastOrNull()
        }

        // Fallback: delegate to engine AI
        if (fallback != null) {
            logger.warn("AI draft pick falling back to engine AI P{}p{}", packNumber, pickNumber)
            return fallback.chooseDraftPick(pack, pickedSoFar, packNumber, pickNumber, picksRequired, passDirection)
        }

        // Last-resort: pick by rarity
        val sorted = pack.sortedByDescending { rarityScore(it.rarity ?: "") }.take(picksRequired).map { it.name }
        logger.info("AI draft pick last-resort P{}p{}: {}", packNumber, pickNumber, sorted.joinToString(", "))
        return sorted
    }

    override fun chooseWinstonAction(
        pileCards: List<CardSummary>,
        pileIndex: Int,
        pileSizes: List<Int>,
        pickedSoFar: List<CardSummary>
    ): Boolean {
        if (draftHistory.isEmpty()) {
            draftHistory.add(ChatMessage("system", WINSTON_SYSTEM_PROMPT))
        }

        val prompt = formatWinstonPrompt(pileCards, pileIndex, pileSizes, pickedSoFar)
        draftHistory.add(ChatMessage("user", prompt))
        logger.info("AI Winston prompt ({} chars, {} messages in history)", prompt.length, draftHistory.size)

        val response = llmClient.chatCompletion(draftHistory, cacheControl = CacheControl())
        if (response != null) {
            draftHistory.add(ChatMessage("assistant", response))
            val answer = (extractAnswer(response) ?: response).trim().lowercase()
            if (answer.contains("take")) return true
            if (answer.contains("skip")) return false
        } else {
            draftHistory.removeLastOrNull()
        }

        // Fallback: delegate to engine AI
        if (fallback != null) {
            logger.warn("AI Winston falling back to engine AI")
            return fallback.chooseWinstonAction(pileCards, pileIndex, pileSizes, pickedSoFar)
        }

        // Last-resort: take if pile has 3+ cards or a rare
        val hasRare = pileCards.any { it.rarity?.uppercase() in listOf("RARE", "MYTHIC") }
        return pileCards.size >= 3 || hasRare
    }

    override fun chooseGridDraftPick(
        grid: List<CardSummary?>,
        availableSelections: List<String>,
        pickedSoFar: List<CardSummary>
    ): String {
        if (draftHistory.isEmpty()) {
            draftHistory.add(ChatMessage("system", GRID_SYSTEM_PROMPT))
        }

        val prompt = formatGridDraftPrompt(grid, availableSelections, pickedSoFar)
        draftHistory.add(ChatMessage("user", prompt))
        logger.info("AI grid draft prompt ({} chars, {} messages in history)", prompt.length, draftHistory.size)

        val response = llmClient.chatCompletion(draftHistory, cacheControl = CacheControl())
        if (response != null) {
            draftHistory.add(ChatMessage("assistant", response))
            val answer = (extractAnswer(response) ?: response).trim().uppercase()
            val match = availableSelections.find { answer.contains(it) }
            if (match != null) {
                logger.info("AI LLM grid draft pick: {}", match)
                return match
            }
        } else {
            draftHistory.removeLastOrNull()
        }

        // Fallback: delegate to engine AI
        if (fallback != null) {
            logger.warn("AI grid draft falling back to engine AI")
            return fallback.chooseGridDraftPick(grid, availableSelections, pickedSoFar)
        }

        // Last-resort: pick selection with most non-null cards
        return availableSelections.maxByOrNull { sel ->
            getGridIndices(sel).count { i -> i < grid.size && grid[i] != null }
        } ?: availableSelections.first()
    }

    private fun formatDraftPickPrompt(
        pack: List<CardSummary>,
        pickedSoFar: List<CardSummary>,
        packNumber: Int,
        pickNumber: Int,
        picksRequired: Int,
        passDirection: String
    ): String = buildString {
        appendLine("Pack $packNumber, Pick $pickNumber (passing $passDirection). Pick $picksRequired card${if (picksRequired > 1) "s" else ""}.")
        appendLine()
        appendLine("PACK (choose from these):")
        for ((i, card) in pack.withIndex()) {
            val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
            val oracle = if (!card.oracleText.isNullOrBlank()) " — ${card.oracleText}" else ""
            appendLine("  ${('A' + i)}. ${card.name} ${card.manaCost ?: ""} [${card.typeLine}]$stats [${card.rarity}]$oracle")
        }

        if (pickedSoFar.isNotEmpty()) {
            appendLine()
            appendLine("YOUR PICKS SO FAR (${pickedSoFar.size} cards):")
            val grouped = pickedSoFar.groupBy { it.name }
            for ((name, copies) in grouped.entries.sortedBy { it.value.first().manaCost ?: "zzz" }) {
                val card = copies.first()
                val count = if (copies.size > 1) "${copies.size}x " else ""
                val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
                val oracle = if (!card.oracleText.isNullOrBlank()) " — ${card.oracleText}" else ""
                appendLine("  $count${name} ${card.manaCost ?: ""} [${card.typeLine}]$stats$oracle")
            }

            appendLine()
            appendLine("COLOR DISTRIBUTION:")
            val colorCounts = mutableMapOf<Char, Int>()
            for (card in pickedSoFar) {
                val cost = card.manaCost ?: continue
                for (ch in cost) {
                    if (ch in "WUBRG") colorCounts[ch] = (colorCounts[ch] ?: 0) + 1
                }
            }
            if (colorCounts.isEmpty()) {
                appendLine("  No colored cards picked yet")
            } else {
                val colorNames = mapOf('W' to "White", 'U' to "Blue", 'B' to "Black", 'R' to "Red", 'G' to "Green")
                val summary = colorCounts.entries.sortedByDescending { it.value }
                    .joinToString(", ") { "${colorNames[it.key] ?: it.key} (${it.value} symbols)" }
                appendLine("  $summary")
            }
        }

        appendLine()
        appendLine("Reply with ONLY the letter(s) of your pick. Example: A")
    }

    private fun formatWinstonPrompt(
        pileCards: List<CardSummary>,
        pileIndex: Int,
        pileSizes: List<Int>,
        pickedSoFar: List<CardSummary>
    ): String = buildString {
        appendLine("Winston Draft: You are looking at pile ${pileIndex + 1} of ${pileSizes.size}.")
        appendLine("Pile sizes: ${pileSizes.joinToString(", ")}")
        appendLine()
        appendLine("CURRENT PILE (${pileCards.size} cards):")
        for (card in pileCards) {
            val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
            val oracle = if (!card.oracleText.isNullOrBlank()) " — ${card.oracleText}" else ""
            appendLine("  ${card.name} ${card.manaCost ?: ""} [${card.typeLine}]$stats [${card.rarity}]$oracle")
        }

        if (pickedSoFar.isNotEmpty()) {
            appendLine()
            appendLine("YOUR PICKS SO FAR (${pickedSoFar.size} cards):")
            pickedSoFar.groupBy { it.name }.forEach { (name, copies) ->
                val card = copies.first()
                val count = if (copies.size > 1) "${copies.size}x " else ""
                val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
                val oracle = if (!card.oracleText.isNullOrBlank()) " — ${card.oracleText}" else ""
                appendLine("  $count$name ${card.manaCost ?: ""} [${card.typeLine}]$stats$oracle")
            }
        }

        appendLine()
        appendLine("Reply with ONLY: TAKE or SKIP")
    }

    private fun formatGridDraftPrompt(
        grid: List<CardSummary?>,
        availableSelections: List<String>,
        pickedSoFar: List<CardSummary>
    ): String = buildString {
        appendLine("Grid Draft: Choose a row or column from the 3x3 grid.")
        appendLine()
        appendLine("GRID:")
        for (row in 0..2) {
            appendLine("  Row $row:")
            for (col in 0..2) {
                val card = grid.getOrNull(row * 3 + col)
                if (card != null) {
                    val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
                    val oracle = if (!card.oracleText.isNullOrBlank()) " — ${card.oracleText}" else ""
                    appendLine("    ${card.name} ${card.manaCost ?: ""} [${card.typeLine}]$stats [${card.rarity}]$oracle")
                } else {
                    appendLine("    [empty]")
                }
            }
        }

        appendLine()
        appendLine("AVAILABLE SELECTIONS: ${availableSelections.joinToString(", ")}")

        if (pickedSoFar.isNotEmpty()) {
            appendLine()
            appendLine("YOUR PICKS SO FAR (${pickedSoFar.size} cards):")
            pickedSoFar.groupBy { it.name }.forEach { (name, copies) ->
                val card = copies.first()
                val count = if (copies.size > 1) "${copies.size}x " else ""
                val stats = if (card.power != null) " ${card.power}/${card.toughness}" else ""
                val oracle = if (!card.oracleText.isNullOrBlank()) " — ${card.oracleText}" else ""
                appendLine("  $count$name ${card.manaCost ?: ""} [${card.typeLine}]$stats$oracle")
            }
        }

        appendLine()
        appendLine("Reply with ONLY the selection (e.g., ROW_0 or COL_1)")
    }

    private fun parseDraftPickResponse(response: String, pack: List<CardSummary>, picksRequired: Int): List<String> {
        val cleaned = response.trim().uppercase()
        val picks = mutableListOf<String>()

        // Try letter-based picks (A, B, C, ...)
        for (ch in cleaned) {
            if (ch in 'A'..'Z') {
                val index = ch - 'A'
                if (index < pack.size) {
                    picks.add(pack[index].name)
                }
            }
            if (picks.size >= picksRequired) break
        }

        if (picks.size >= picksRequired) return picks

        // Try matching card names directly
        picks.clear()
        for (card in pack) {
            if (cleaned.contains(card.name.uppercase())) {
                picks.add(card.name)
                if (picks.size >= picksRequired) break
            }
        }

        return picks
    }

    private fun getGridIndices(selection: String): List<Int> {
        return when {
            selection.startsWith("ROW_") -> {
                val row = selection.removePrefix("ROW_").toIntOrNull() ?: return emptyList()
                listOf(row * 3, row * 3 + 1, row * 3 + 2)
            }
            selection.startsWith("COL_") -> {
                val col = selection.removePrefix("COL_").toIntOrNull() ?: return emptyList()
                listOf(col, col + 3, col + 6)
            }
            else -> emptyList()
        }
    }

    private fun rarityScore(rarity: String): Int = when (rarity.uppercase()) {
        "MYTHIC" -> 4
        "RARE" -> 3
        "UNCOMMON" -> 2
        "COMMON" -> 1
        else -> 0
    }

    companion object {
        private val DRAFT_SYSTEM_PROMPT = """
            You are an expert Magic: The Gathering drafter. You will be shown packs of cards and asked to pick cards for your draft pool.

            RESPONSE FORMAT:
            - Reply with ONLY the letter(s) of your pick. No explanation, no reasoning.
            - Example: A

            DRAFT STRATEGY:
            - Early picks (1-3): take the best card regardless of color
            - Mid picks (4-8): start committing to 2 colors based on what's open
            - Late picks (9+): stay in your colors, fill curve gaps
            - Prioritize: removal > efficient creatures > evasion > card advantage > fillers
            - Good mana curve: enough 2-drops and 3-drops to not fall behind
            - Pay attention to pass direction: cards you see late in a pack indicate those colors are open from that direction
        """.trimIndent()

        private val WINSTON_SYSTEM_PROMPT = """
            You are an expert Magic: The Gathering drafter playing Winston Draft.

            In Winston Draft, you examine piles one at a time and decide to TAKE the entire pile or SKIP to the next pile.
            If you skip all piles, you take a random card from the remaining pool.

            RESPONSE FORMAT:
            - Reply with ONLY: TAKE or SKIP. No explanation, no reasoning.

            STRATEGY:
            - Take piles with strong cards (good removal, efficient creatures, bombs)
            - Consider pile size: larger piles give more cards, even if some are mediocre
            - Prioritize staying in 2 colors once you've committed
            - Rares and mythics in a pile make it worth taking
            - Skip weak piles early to see other options; be less picky on the last pile
        """.trimIndent()

        private val GRID_SYSTEM_PROMPT = """
            You are an expert Magic: The Gathering drafter playing Grid Draft.

            In Grid Draft, cards are laid out in a 3x3 grid. You choose a full row or column to draft all cards in that selection.

            RESPONSE FORMAT:
            - Reply with ONLY the selection (e.g., ROW_0 or COL_1). No explanation, no reasoning.

            STRATEGY:
            - Pick the row or column with the highest total card quality
            - Prioritize: removal > efficient creatures > evasion > card advantage
            - Stay in 2 colors when possible
            - Consider mana curve — variety of costs is better than all expensive or all cheap
        """.trimIndent()

        private val SYSTEM_PROMPT = """
            You are an expert Magic: The Gathering player. You will be shown the game state and asked to choose ONE action at a time.

            RESPONSE FORMAT — CRITICAL:
            - Reply with ONLY your choice. No explanation, no reasoning, no tags.
            - For actions: B or C2 (action C, target 2)
            - For selections: A, C
            - For yes/no: Yes or No
            - For numbers: 3
            - Do NOT chain multiple actions. You can only take ONE action per prompt.
            - Actions marked "(can't afford)" cannot be chosen — pick something else.

            GAME FLOW:
            - You take one action, then the game state updates, then you choose again.
            - To cast a spell: first play a land, then on the next prompt you will see updated mana and can cast spells.
            - Mana abilities (like "{T}: Add {G}") are activated automatically when you cast a spell — you do NOT need to activate them manually.
            - NEVER activate mana abilities manually (tapping lands for mana). The engine handles mana payment automatically. Tapping a land when you have nothing to cast wastes it for no benefit.

            HOW TO THINK:
            Before every action, read the board. Consider life totals, creatures in play, cards in hand, and mana available — for both players. Think about how the game is going and what matters most right now.

            Ask yourself: if I use this card now, what happens if the opponent plays something better next turn? Every card you spend is one you won't have later. Think about whether acting now or waiting gives you a better game.

            Think about what your opponent is likely to do. What creatures might they cast? What attacks might they make? How does that change what you should do with your mana and your cards this turn?

            FUNDAMENTALS:
            - Play a land every turn. Play your land before casting spells.
            - Consider what combination of spells best uses your mana this turn.
            - Passing with mana open is a real option. Not every turn requires casting a spell.
            - Instant-speed spells are strongest when used at the moment they matter most — reacting to the opponent, or as a surprise during combat.
            - In combat, think about what each attack or block costs you versus what it gains you.
        """.trimIndent()
    }
}
