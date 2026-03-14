package com.wingedsheep.gameserver.ai

import com.wingedsheep.gameserver.config.AiProperties
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.protocol.LegalActionInfo
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(AiPlayerController::class.java)

/**
 * The AI "brain" that decides what action to take given the current game state.
 *
 * Maintains a rolling conversation history with the LLM and handles
 * retries with fallback to heuristics.
 */
class AiPlayerController(
    private val properties: AiProperties,
    private val openRouterClient: OpenRouterClient,
    private val playerId: EntityId
) {
    private val formatter = GameStateFormatter()
    private val parser = AiResponseParser()

    private val conversationHistory = mutableListOf<ChatMessage>()
    private val maxHistorySize = 10

    init {
        conversationHistory.add(ChatMessage("system", SYSTEM_PROMPT))
    }

    /**
     * Provide the AI with its deck composition so it can make informed decisions.
     * Called once when a game starts. The AI knows what cards are in the deck but not their order.
     */
    fun setDeckList(deckList: Map<String, Int>) {
        val deckDescription = buildString {
            appendLine("=== YOUR DECK (${deckList.values.sum()} cards) ===")
            appendLine("You know the cards in your deck but not their order.")
            appendLine()
            val sorted = deckList.entries.sortedByDescending { it.value }
            for ((name, count) in sorted) {
                appendLine("  ${count}x $name")
            }
        }
        // Insert as a system-level context message right after the main system prompt
        conversationHistory.add(ChatMessage("system", deckDescription))
        logger.info("AI deck context set: {} unique cards, {} total", deckList.size, deckList.values.sum())
    }

    // =========================================================================
    // Public decision methods
    // =========================================================================

    /**
     * Choose an action from the list of legal actions.
     * Returns the chosen GameAction, or null to auto-pass.
     */
    fun chooseAction(
        state: ClientGameState,
        legalActions: List<LegalActionInfo>,
        pendingDecision: PendingDecision?
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

        // Shortcut: pending decision with auto-resolve
        if (pendingDecision != null) {
            val autoResponse = tryAutoResolveDecision(pendingDecision)
            if (autoResponse != null) {
                logger.info("AI auto-resolved decision: {}", pendingDecision::class.simpleName)
                return autoResponse
            }
        }

        // Shortcut: handle combat declarations heuristically (LLM can't build attacker/blocker maps)
        val combatAction = tryCombatAction(state, legalActions)
        if (combatAction != null) return combatAction

        // Format state and query LLM
        val prompt = formatter.format(state, legalActions, pendingDecision)
        logger.info("AI prompt ({} chars):\n{}", prompt.length, prompt)

        val response = queryLlm(prompt)

        if (response != null) {
            logger.info("AI LLM response: {}", response.take(500))
            val parsed = if (pendingDecision != null) {
                parseDecisionResponse(response, pendingDecision, state)
            } else {
                parseActionResponse(response, legalActions, state)
            }
            if (parsed != null) {
                logger.info("AI parsed LLM response successfully: {}", when (parsed) {
                    is ActionResponse.SubmitAction -> "Action(${parsed.action::class.simpleName})"
                    is ActionResponse.SubmitDecision -> "Decision(${parsed.response::class.simpleName})"
                })
                return parsed
            }
            logger.warn("AI failed to parse LLM response, falling back to heuristic")
        } else {
            logger.warn("AI LLM returned null, falling back to heuristic")
        }

        // Fallback: heuristic
        val heuristic = if (pendingDecision != null) {
            heuristicDecision(pendingDecision, state)
        } else {
            heuristicAction(legalActions, state)
        }
        logger.info("AI heuristic result: {}", when (heuristic) {
            is ActionResponse.SubmitAction -> "Action(${heuristic.action::class.simpleName})"
            is ActionResponse.SubmitDecision -> "Decision(${heuristic.response::class.simpleName})"
        })
        return heuristic
    }

    /**
     * Decide whether to keep or mulligan.
     * Returns true to keep, false to mulligan.
     */
    fun decideMulligan(mulliganMessage: ServerMessage.MulliganDecision): Boolean {
        val cards = mulliganMessage.cards
        val cardNames = cards.values.map { it.name }
        val prompt = formatter.formatMulligan(cardNames, mulliganMessage.mulliganCount, mulliganMessage.isOnThePlay)

        // Shortcut: always keep if mulliganCount >= 3 (5 or fewer cards)
        if (mulliganMessage.mulliganCount >= 3) {
            logger.info("AI keeping hand after ${mulliganMessage.mulliganCount} mulligans (auto-keep)")
            return true
        }

        val response = queryLlm(prompt)
        if (response != null) {
            val choice = parser.parseMulliganChoice(response)
            if (choice != null) {
                logger.info("AI mulligan decision: ${if (choice) "keep" else "mulligan"}")
                return choice
            }
        }

        // Heuristic: keep if mulliganCount >= 2, otherwise mulligan
        // We don't have card type info, so just use mulligan count
        val keep = mulliganMessage.mulliganCount >= 2
        logger.info("AI mulligan heuristic: ${if (keep) "keep" else "mulligan"} (mulligan count: ${mulliganMessage.mulliganCount})")
        return keep
    }

    /**
     * Choose cards to put on bottom after mulligan.
     * Returns entity IDs of cards to bottom.
     */
    fun chooseBottomCards(message: ServerMessage.ChooseBottomCards): List<EntityId> {
        val cardCount = message.cardsToPutOnBottom
        val hand = message.hand

        if (cardCount <= 0 || hand.isEmpty()) return emptyList()

        val cards = hand.mapNotNull { id ->
            // We don't have card info in ChooseBottomCards, so we can't do much
            id
        }

        // Heuristic: bottom the last N cards (least desirable)
        // In practice the LLM won't have card info either since ChooseBottomCards
        // only sends entity IDs. Just pick the last ones.
        return cards.takeLast(cardCount)
    }

    // =========================================================================
    // LLM interaction
    // =========================================================================

    private fun queryLlm(prompt: String): String? {
        conversationHistory.add(ChatMessage("user", prompt))

        // Trim history if too long (keep system prompt + last N exchanges)
        while (conversationHistory.size > maxHistorySize * 2 + 1) {
            conversationHistory.removeAt(1) // Remove oldest non-system message
        }

        val response = openRouterClient.chatCompletion(conversationHistory)

        if (response != null) {
            conversationHistory.add(ChatMessage("assistant", response))
        } else {
            // Remove the unanswered user message
            conversationHistory.removeLastOrNull()
        }

        return response
    }

    // =========================================================================
    // Response parsing
    // =========================================================================

    private fun parseActionResponse(response: String, legalActions: List<LegalActionInfo>, state: ClientGameState? = null): ActionResponse? {
        val index = parser.parseActionChoice(response, legalActions.size - 1) ?: return null
        val chosen = legalActions[index]
        val action = maybeAddTargets(chosen, state)
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

        if (!legalAction.targetRequirements.isNullOrEmpty()) {
            // Multiple target requirements — pick first valid for each
            for (req in legalAction.targetRequirements) {
                val validTargets = req.validTargets
                if (validTargets.isEmpty()) continue
                val targetId = validTargets.first()
                val target = resolveTargetType(targetId, req.targetZone, playerIds)
                targets.add(target)
                logger.info("AI auto-targeting req {}: {} -> {} ({})", req.index, req.description, targetId.value, target::class.simpleName)
            }
        } else if (!legalAction.validTargets.isNullOrEmpty()) {
            // Single target requirement — use validTargets directly
            val targetId = legalAction.validTargets.first()
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

        // Modify the action to include targets
        return when (val action = legalAction.action) {
            is CastSpell -> action.copy(targets = targets)
            is ActivateAbility -> action.copy(targets = targets)
            else -> action
        }
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
        // Player target
        if (targetId in playerIds) return ChosenTarget.Player(targetId)

        // Zone-based: explicit zone from LegalActionTargetInfo
        if (targetZone != null) {
            return when (targetZone) {
                "Stack" -> ChosenTarget.Spell(targetId)
                else -> ChosenTarget.Permanent(targetId) // Graveyard, Exile, etc. — engine accepts Permanent for these
            }
        }

        // Infer from game state: check if the entity is on the stack
        if (state != null) {
            val stackZone = state.zones.find { it.zoneId.zoneType == com.wingedsheep.sdk.core.Zone.STACK }
            if (stackZone != null && targetId in stackZone.cardIds) {
                return ChosenTarget.Spell(targetId)
            }
        }

        // Default: battlefield permanent
        return ChosenTarget.Permanent(targetId)
    }

    private fun parseDecisionResponse(
        response: String,
        decision: PendingDecision,
        state: ClientGameState
    ): ActionResponse? {
        val decisionResponse = parseDecisionResponseInner(response, decision, state) ?: return null
        return ActionResponse.SubmitDecision(playerId, decisionResponse)
    }

    private fun parseDecisionResponseInner(
        response: String,
        decision: PendingDecision,
        state: ClientGameState
    ): DecisionResponse? {
        return when (decision) {
            is ChooseTargetsDecision -> {
                // For single target requirement, parse as single choice
                if (decision.targetRequirements.size == 1) {
                    val req = decision.targetRequirements[0]
                    val validTargets = decision.legalTargets[req.index] ?: return null
                    val index = parser.parseActionChoice(response, validTargets.size - 1) ?: return null
                    TargetsResponse(
                        decisionId = decision.id,
                        selectedTargets = mapOf(req.index to listOf(validTargets[index]))
                    )
                } else {
                    // Multi-target: try to parse multiple choices
                    val result = mutableMapOf<Int, List<EntityId>>()
                    for (req in decision.targetRequirements) {
                        val validTargets = decision.legalTargets[req.index] ?: continue
                        val index = parser.parseActionChoice(response, validTargets.size - 1)
                        if (index != null) {
                            result[req.index] = listOf(validTargets[index])
                        }
                    }
                    if (result.isNotEmpty()) {
                        TargetsResponse(decisionId = decision.id, selectedTargets = result)
                    } else null
                }
            }

            is SelectCardsDecision -> {
                val indices = parser.parseMultipleSelections(response, decision.options.size - 1)
                if (indices != null) {
                    val selected = indices.map { decision.options[it] }
                    CardsSelectedResponse(decisionId = decision.id, selectedCards = selected)
                } else null
            }

            is YesNoDecision -> {
                val choice = parser.parseYesNo(response) ?: return null
                YesNoResponse(decisionId = decision.id, choice = choice)
            }

            is ChooseModeDecision -> {
                val index = parser.parseActionChoice(response, decision.modes.size - 1) ?: return null
                ModesChosenResponse(decisionId = decision.id, selectedModes = listOf(decision.modes[index].index))
            }

            is ChooseColorDecision -> {
                val colors = decision.availableColors.toList()
                val index = parser.parseActionChoice(response, colors.size - 1) ?: return null
                ColorChosenResponse(decisionId = decision.id, color = colors[index])
            }

            is ChooseNumberDecision -> {
                val number = parser.parseNumber(response, decision.minValue, decision.maxValue) ?: return null
                NumberChosenResponse(decisionId = decision.id, number = number)
            }

            is DistributeDecision -> {
                val dist = parser.parseDistribution(response, decision.targets.size, decision.totalAmount)
                    ?: return null
                val entityDist = dist.entries.associate { (idx, amount) -> decision.targets[idx] to amount }
                DistributionResponse(decisionId = decision.id, distribution = entityDist)
            }

            is OrderObjectsDecision -> {
                val ordering = parser.parseOrdering(response, decision.objects.size) ?: return null
                OrderedResponse(decisionId = decision.id, orderedObjects = ordering.map { decision.objects[it] })
            }

            is SplitPilesDecision -> {
                // Simple split: try to parse which cards go in pile 1 vs pile 2
                val pile1Indices = parser.parseMultipleSelections(response, decision.cards.size - 1)
                if (pile1Indices != null) {
                    val pile1 = pile1Indices.map { decision.cards[it] }
                    val pile2 = decision.cards.filter { it !in pile1 }
                    PilesSplitResponse(decisionId = decision.id, piles = listOf(pile1, pile2))
                } else null
            }

            is ChooseOptionDecision -> {
                val index = parser.parseActionChoice(response, decision.options.size - 1) ?: return null
                OptionChosenResponse(decisionId = decision.id, optionIndex = index)
            }

            is AssignDamageDecision -> {
                // Use default assignments
                DamageAssignmentResponse(decisionId = decision.id, assignments = decision.defaultAssignments)
            }

            is SearchLibraryDecision -> {
                val indices = parser.parseMultipleSelections(response, decision.options.size - 1)
                val selected = if (indices != null) {
                    indices.take(decision.maxSelections).map { decision.options[it] }
                } else {
                    // Pick first option
                    if (decision.options.isNotEmpty()) listOf(decision.options.first()) else emptyList()
                }
                CardsSelectedResponse(decisionId = decision.id, selectedCards = selected)
            }

            is ReorderLibraryDecision -> {
                val ordering = parser.parseOrdering(response, decision.cards.size) ?: return null
                CardsSelectedResponse(decisionId = decision.id, selectedCards = ordering.map { decision.cards[it] })
            }

            is SelectManaSourcesDecision -> {
                // Always auto-pay
                ManaSourcesSelectedResponse(decisionId = decision.id, autoPay = true)
            }
        }
    }

    // =========================================================================
    // Auto-resolve shortcuts (skip LLM call)
    // =========================================================================

    private fun tryAutoResolveDecision(decision: PendingDecision): ActionResponse? {
        return when (decision) {
            is SelectManaSourcesDecision -> {
                logger.debug("AI auto-paying mana sources")
                ActionResponse.SubmitDecision(
                    playerId,
                    ManaSourcesSelectedResponse(decisionId = decision.id, autoPay = true)
                )
            }

            is AssignDamageDecision -> {
                logger.debug("AI using default damage assignment")
                ActionResponse.SubmitDecision(
                    playerId,
                    DamageAssignmentResponse(
                        decisionId = decision.id,
                        assignments = decision.defaultAssignments
                    )
                )
            }

            else -> null
        }
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

            // Build prompt listing available attackers and opponent's blockers
            val prompt = buildString {
                appendLine("=== DECLARE ATTACKERS ===")
                appendLine("Choose which creatures to attack with. The opponent can block with their untapped creatures.")
                appendLine()
                appendLine("Your creatures that can attack:")
                for ((i, attackerId) in validAttackers.withIndex()) {
                    val card = state.cards[attackerId] ?: continue
                    val keywords = card.keywords.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name.lowercase() } ?: ""
                    val keywordStr = if (keywords.isNotEmpty()) " [$keywords]" else ""
                    appendLine("  [${GameStateFormatter.actionLetter(i)}] ${card.name} ${card.power}/${card.toughness}$keywordStr")
                }
                // Show opponent's potential blockers
                val opponentCreatures = state.cards.values.filter {
                    it.controllerId == opponentId && "Creature" in it.cardTypes && !it.isTapped
                }
                if (opponentCreatures.isNotEmpty()) {
                    appendLine()
                    appendLine("Opponent's untapped creatures (potential blockers):")
                    for (card in opponentCreatures) {
                        val keywords = card.keywords.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name.lowercase() } ?: ""
                        val keywordStr = if (keywords.isNotEmpty()) " [$keywords]" else ""
                        appendLine("  ${card.name} ${card.power}/${card.toughness}$keywordStr")
                    }
                }
                appendLine()
                appendLine("Reply with the letters of creatures to attack with (e.g., \"A, C\"), or \"NONE\" to not attack.")
            }

            val response = queryLlm(prompt)
            val attackerMap = mutableMapOf<EntityId, EntityId>()

            if (response != null) {
                logger.info("AI combat LLM response: {}", response.take(200))
                val upper = response.trim().uppercase()
                if (upper != "NONE" && upper != "PASS" && upper != "NO") {
                    val indices = parser.parseMultipleSelections(response, validAttackers.size - 1)
                    if (indices != null) {
                        for (idx in indices) {
                            if (idx < validAttackers.size) {
                                attackerMap[validAttackers[idx]] = opponentId
                            }
                        }
                    } else {
                        // Parsing failed — fall back to attacking with all
                        logger.info("AI combat: failed to parse attacker selection, attacking with all")
                        for (attackerId in validAttackers) {
                            val card = state.cards[attackerId] ?: continue
                            if ((card.power ?: 0) > 0) attackerMap[attackerId] = opponentId
                        }
                    }
                }
            } else {
                // LLM failed — fall back to attacking with all
                logger.info("AI combat: LLM failed, attacking with all")
                for (attackerId in validAttackers) {
                    val card = state.cards[attackerId] ?: continue
                    if ((card.power ?: 0) > 0) attackerMap[attackerId] = opponentId
                }
            }

            val attackerNames = attackerMap.keys.mapNotNull { state.cards[it]?.name }
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

            // Build prompt listing attackers and available blockers
            val prompt = buildString {
                appendLine("=== DECLARE BLOCKERS ===")
                appendLine("Opponent is attacking. Choose how to block.")
                appendLine()
                appendLine("Attacking creatures:")
                for ((i, attacker) in combat.attackers.withIndex()) {
                    val card = state.cards[attacker.creatureId]
                    val stats = if (card != null) "${card.power}/${card.toughness}" else "?/?"
                    val keywords = card?.keywords?.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name.lowercase() } ?: ""
                    val keywordStr = if (keywords.isNotEmpty()) " [$keywords]" else ""
                    appendLine("  Attacker ${i + 1}: ${attacker.creatureName} $stats$keywordStr")
                }
                appendLine()
                appendLine("Your creatures that can block:")
                for ((i, blockerId) in validBlockers.withIndex()) {
                    val card = state.cards[blockerId] ?: continue
                    val keywords = card.keywords.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.name.lowercase() } ?: ""
                    val keywordStr = if (keywords.isNotEmpty()) " [$keywords]" else ""
                    appendLine("  [${GameStateFormatter.actionLetter(i)}] ${card.name} ${card.power}/${card.toughness}$keywordStr")
                }
                appendLine()
                appendLine("Reply with blocking assignments like \"A blocks 1, C blocks 2\" or \"NONE\" to not block.")
                appendLine("Format: <blocker letter> blocks <attacker number>")
            }

            val response = queryLlm(prompt)
            val blockerMap = mutableMapOf<EntityId, List<EntityId>>()

            if (response != null) {
                logger.info("AI combat LLM response: {}", response.take(200))
                val upper = response.trim().uppercase()
                if (upper != "NONE" && upper != "PASS" && upper != "NO") {
                    // Parse "A blocks 1, C blocks 2" format
                    val blockPattern = Regex("""([A-Z])\s*(?:blocks?|->|:)\s*(\d+)""", RegexOption.IGNORE_CASE)
                    val matches = blockPattern.findAll(response).toList()
                    for (match in matches) {
                        val blockerIdx = GameStateFormatter.letterToIndex(match.groupValues[1])
                        val attackerNum = match.groupValues[2].toIntOrNull()?.minus(1) // 1-based to 0-based
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
                        // Try simpler format: just letters = block the first attacker
                        val indices = parser.parseMultipleSelections(response, validBlockers.size - 1)
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
            // No heuristic fallback — if LLM says nothing, take the damage

            if (blockerMap.isEmpty()) {
                logger.info("AI combat: no blocks declared")
            }

            return ActionResponse.SubmitAction(DeclareBlockers(playerId, blockerMap))
        }

        return null
    }

    // =========================================================================
    // Heuristic fallbacks
    // =========================================================================

    private fun heuristicAction(legalActions: List<LegalActionInfo>, state: ClientGameState? = null): ActionResponse {
        // Prefer playing a land
        val playLand = legalActions.find { it.actionType == "PlayLand" }
        if (playLand != null) {
            logger.info("AI heuristic: playing land")
            return ActionResponse.SubmitAction(playLand.action)
        }
        // Try casting an affordable spell
        val castSpell = legalActions.find {
            (it.actionType == "CastSpell" || it.actionType == "CastFaceDown") && it.isAffordable
        }
        if (castSpell != null) {
            logger.info("AI heuristic: casting {}", castSpell.description)
            return ActionResponse.SubmitAction(maybeAddTargets(castSpell, state))
        }
        // Find PassPriority
        val passAction = legalActions.find { it.actionType == "PassPriority" }
        if (passAction != null) {
            logger.info("AI heuristic: passing priority")
            return ActionResponse.SubmitAction(passAction.action)
        }
        // Otherwise submit first action
        logger.info("AI heuristic: selecting first legal action")
        return ActionResponse.SubmitAction(legalActions.first().action)
    }

    private fun heuristicDecision(decision: PendingDecision, state: ClientGameState): ActionResponse {
        logger.info("AI heuristic for decision: ${decision::class.simpleName}")
        val response: DecisionResponse = when (decision) {
            is ChooseTargetsDecision -> {
                // Pick first valid target for each requirement
                val targets = decision.targetRequirements.associate { req ->
                    val valid = decision.legalTargets[req.index] ?: emptyList()
                    req.index to valid.take(req.minTargets)
                }
                TargetsResponse(decisionId = decision.id, selectedTargets = targets)
            }

            is SelectCardsDecision -> {
                CardsSelectedResponse(
                    decisionId = decision.id,
                    selectedCards = decision.options.take(decision.minSelections)
                )
            }

            is YesNoDecision -> {
                // Default to yes for beneficial effects
                YesNoResponse(decisionId = decision.id, choice = true)
            }

            is ChooseModeDecision -> {
                val firstAvailable = decision.modes.firstOrNull { it.available }
                ModesChosenResponse(
                    decisionId = decision.id,
                    selectedModes = listOf(firstAvailable?.index ?: 0)
                )
            }

            is ChooseColorDecision -> {
                // Pick first available color
                ColorChosenResponse(
                    decisionId = decision.id,
                    color = decision.availableColors.firstOrNull() ?: Color.WHITE
                )
            }

            is ChooseNumberDecision -> {
                NumberChosenResponse(decisionId = decision.id, number = decision.minValue)
            }

            is DistributeDecision -> {
                // Equal distribution
                val perTarget = decision.totalAmount / decision.targets.size
                val remainder = decision.totalAmount % decision.targets.size
                val dist = decision.targets.withIndex().associate { (i, tid) ->
                    tid to (perTarget + if (i < remainder) 1 else 0)
                }
                DistributionResponse(decisionId = decision.id, distribution = dist)
            }

            is OrderObjectsDecision -> {
                OrderedResponse(decisionId = decision.id, orderedObjects = decision.objects)
            }

            is SplitPilesDecision -> {
                val half = decision.cards.size / 2
                PilesSplitResponse(
                    decisionId = decision.id,
                    piles = listOf(decision.cards.take(half), decision.cards.drop(half))
                )
            }

            is ChooseOptionDecision -> {
                OptionChosenResponse(decisionId = decision.id, optionIndex = 0)
            }

            is AssignDamageDecision -> {
                DamageAssignmentResponse(decisionId = decision.id, assignments = decision.defaultAssignments)
            }

            is SearchLibraryDecision -> {
                val selected = if (decision.options.isNotEmpty()) {
                    decision.options.take(decision.maxSelections.coerceAtMost(1))
                } else {
                    emptyList()
                }
                CardsSelectedResponse(decisionId = decision.id, selectedCards = selected)
            }

            is ReorderLibraryDecision -> {
                CardsSelectedResponse(decisionId = decision.id, selectedCards = decision.cards)
            }

            is SelectManaSourcesDecision -> {
                ManaSourcesSelectedResponse(decisionId = decision.id, autoPay = true)
            }
        }
        return ActionResponse.SubmitDecision(playerId, response)
    }

    companion object {
        private val SYSTEM_PROMPT = """
            You are an AI playing Magic: The Gathering. You will be shown the game state and asked to choose ONE action at a time.

            RESPONSE FORMAT — CRITICAL:
            - Reply with EXACTLY ONE letter. Nothing else. Example: B
            - Do NOT chain multiple actions (e.g., "D, E, F, C" is WRONG)
            - You can only take ONE action per prompt. After you act, you will get a new prompt with updated state.
            - For selection decisions (discard, scry, etc.), reply with the letter(s) of the card(s) to select.
            - For yes/no questions, reply with "Yes" or "No".
            - For number choices, reply with just the number.

            GAME FLOW:
            - You take one action, then the game state updates, then you choose again.
            - To cast a spell: first play a land, then on the next prompt you will see updated mana and can cast spells.
            - Mana abilities (like "{T}: Add {G}") are activated automatically when you cast a spell — you do NOT need to activate them manually.
            - Actions marked "(can't afford)" cannot be chosen.

            STRATEGY:
            - Play a land every turn (choose PlayLand actions like "Play Forest")
            - Cast creatures to build your board
            - Attack when your creatures are bigger or opponent is open
            - Use removal on opponent's best creatures
            - Pass priority when you have nothing useful to do
        """.trimIndent()
    }
}

/**
 * Represents the AI's response to a game prompt.
 */
sealed interface ActionResponse {
    /** Submit a game action (cast spell, pass priority, declare attackers, etc.) */
    data class SubmitAction(val action: GameAction) : ActionResponse

    /** Submit a decision response */
    data class SubmitDecision(val playerId: EntityId, val response: DecisionResponse) : ActionResponse
}
