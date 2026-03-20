package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.evaluation.BoardEvaluator
import com.wingedsheep.engine.ai.evaluation.BoardPresence
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Handles all [PendingDecision] types by evaluating possible responses
 * and picking the one that leads to the best board state.
 *
 * For decisions with small branching factors (yes/no, color, mode), it
 * simulates each option. For larger spaces, it uses MTG-aware heuristics.
 */
class DecisionResponder(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator
) {
    fun respond(state: GameState, decision: PendingDecision, playerId: EntityId): DecisionResponse {
        return when (decision) {
            is ChooseTargetsDecision -> respondTargets(state, decision, playerId)
            is SelectCardsDecision -> respondSelectCards(state, decision, playerId)
            is YesNoDecision -> respondYesNo(state, decision, playerId)
            is ChooseModeDecision -> respondModes(state, decision, playerId)
            is ChooseColorDecision -> respondColor(state, decision, playerId)
            is ChooseNumberDecision -> respondNumber(state, decision, playerId)
            is DistributeDecision -> respondDistribute(state, decision, playerId)
            is OrderObjectsDecision -> respondOrder(state, decision, playerId)
            is SplitPilesDecision -> respondSplitPiles(state, decision, playerId)
            is ChooseOptionDecision -> respondOption(state, decision, playerId)
            is AssignDamageDecision -> respondDamageAssignment(state, decision)
            is SearchLibraryDecision -> respondSearchLibrary(state, decision, playerId)
            is ReorderLibraryDecision -> respondReorderLibrary(state, decision, playerId)
            is SelectManaSourcesDecision -> respondManaSelection(decision)
        }
    }

    // ── Target selection ─────────────────────────────────────────────────

    private fun respondTargets(
        state: GameState,
        decision: ChooseTargetsDecision,
        playerId: EntityId
    ): DecisionResponse {
        if (decision.targetRequirements.size == 1) {
            val req = decision.targetRequirements.first()
            val targets = decision.legalTargets[req.index] ?: return cancelOrFirst(decision)
            if (targets.isEmpty()) return cancelOrFirst(decision)

            // For small target pools, simulate each; for large pools, use heuristics then simulate top candidates
            val candidates = if (targets.size <= 8) {
                targets
            } else {
                // Pre-rank by heuristic, then simulate top 8
                targets.sortedByDescending { targetHeuristic(state, it, playerId) }.take(8)
            }

            val best = pickBestBySimulation(state, candidates, playerId) { target ->
                TargetsResponse(decision.id, mapOf(req.index to listOf(target)))
            }
            return TargetsResponse(decision.id, mapOf(req.index to listOf(best)))
        }

        // Multi-target: simulate the best target for each requirement independently
        val selected = decision.targetRequirements.associate { req ->
            val targets = decision.legalTargets[req.index] ?: emptyList()
            if (targets.isEmpty()) {
                req.index to emptyList()
            } else {
                val best = pickBestBySimulation(state, targets.take(8), playerId) { target ->
                    TargetsResponse(decision.id, mapOf(req.index to listOf(target)))
                }
                req.index to listOf(best)
            }
        }
        return TargetsResponse(decision.id, selected)
    }

    /** Heuristic for pre-ranking targets before simulation. Higher = better target. */
    private fun targetHeuristic(state: GameState, targetId: EntityId, playerId: EntityId): Double {
        val projected = state.projectedState
        val controller = projected.getController(targetId)

        if (projected.isCreature(targetId)) {
            val power = projected.getPower(targetId) ?: 0
            val toughness = projected.getToughness(targetId) ?: 0
            val value = power + toughness.toDouble()
            // Opponent's creatures are better targets for removal
            return if (controller != playerId) value + 5.0 else -value
        }

        // Players — prefer opponent
        val isOpponent = targetId == state.getOpponent(playerId)
        if (isOpponent) return 3.0

        return 0.0
    }

    // ── Card selection ───────────────────────────────────────────────────

    private fun respondSelectCards(
        state: GameState,
        decision: SelectCardsDecision,
        playerId: EntityId
    ): DecisionResponse {
        val options = decision.options
        val min = decision.minSelections
        val max = decision.maxSelections

        if (min == options.size) {
            return CardsSelectedResponse(decision.id, options)
        }

        // Context-aware ranking
        val prompt = decision.prompt.lowercase()
        val isDiscard = prompt.contains("discard")
        val isSacrifice = prompt.contains("sacrifice")
        val isScryBottom = decision.selectedLabel?.lowercase()?.contains("bottom") == true
        val isChooseToKeep = prompt.contains("put") && prompt.contains("hand")

        return when {
            isDiscard || isScryBottom -> {
                // Pick cards we want LEAST (to discard / put on bottom)
                val ranked = rankCardsContextual(state, options, playerId, wantToKeep = false)
                CardsSelectedResponse(decision.id, ranked.take(min.coerceAtLeast(1).coerceAtMost(max)))
            }
            isSacrifice -> {
                // Sacrifice least valuable permanents
                val ranked = options.sortedBy { entityId ->
                    val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@sortedBy 0.0
                    BoardPresence.permanentValue(state, state.projectedState, entityId, card)
                }
                CardsSelectedResponse(decision.id, ranked.take(min.coerceAtLeast(1).coerceAtMost(max)))
            }
            isChooseToKeep -> {
                // Keep best cards
                val ranked = rankCardsContextual(state, options, playerId, wantToKeep = true)
                CardsSelectedResponse(decision.id, ranked.take(max.coerceAtMost(options.size)))
            }
            max > 0 && max < options.size -> {
                // Generic "select up to N" — pick best
                val ranked = rankCardsContextual(state, options, playerId, wantToKeep = true)
                CardsSelectedResponse(decision.id, ranked.take(max))
            }
            else -> {
                val ranked = rankCardsContextual(state, options, playerId, wantToKeep = true)
                CardsSelectedResponse(decision.id, ranked.take(min.coerceAtLeast(0)))
            }
        }
    }

    // ── Yes / No ─────────────────────────────────────────────────────────

    private fun respondYesNo(
        state: GameState,
        decision: YesNoDecision,
        playerId: EntityId
    ): DecisionResponse {
        val yesResult = simulator.simulateDecision(state, YesNoResponse(decision.id, true))
        val noResult = simulator.simulateDecision(state, YesNoResponse(decision.id, false))
        val yesScore = evaluateResult(yesResult, playerId)
        val noScore = evaluateResult(noResult, playerId)
        return YesNoResponse(decision.id, yesScore >= noScore)
    }

    // ── Mode selection ───────────────────────────────────────────────────

    private fun respondModes(
        state: GameState,
        decision: ChooseModeDecision,
        playerId: EntityId
    ): DecisionResponse {
        val available = decision.modes.filter { it.available }
        if (available.size <= decision.minModes) {
            return ModesChosenResponse(decision.id, available.map { it.index })
        }

        val best = available.maxByOrNull { mode ->
            evaluateResult(
                simulator.simulateDecision(state, ModesChosenResponse(decision.id, listOf(mode.index))),
                playerId
            )
        }!!
        return ModesChosenResponse(decision.id, listOf(best.index))
    }

    // ── Color choice ─────────────────────────────────────────────────────

    private fun respondColor(
        state: GameState,
        decision: ChooseColorDecision,
        playerId: EntityId
    ): DecisionResponse {
        val best = decision.availableColors.maxByOrNull { color ->
            evaluateResult(
                simulator.simulateDecision(state, ColorChosenResponse(decision.id, color)),
                playerId
            )
        }!!
        return ColorChosenResponse(decision.id, best)
    }

    // ── Number choice ────────────────────────────────────────────────────

    private fun respondNumber(
        state: GameState,
        decision: ChooseNumberDecision,
        playerId: EntityId
    ): DecisionResponse {
        val range = decision.maxValue - decision.minValue
        val candidates = when {
            range <= 10 -> (decision.minValue..decision.maxValue).toList()
            range <= 50 -> (decision.minValue..decision.maxValue step (range / 10).coerceAtLeast(1)).toList() +
                listOf(decision.maxValue)
            else -> listOf(decision.minValue, decision.maxValue, (decision.minValue + decision.maxValue) / 2)
        }

        val best = candidates.maxByOrNull { n ->
            evaluateResult(
                simulator.simulateDecision(state, NumberChosenResponse(decision.id, n)),
                playerId
            )
        }!!
        return NumberChosenResponse(decision.id, best)
    }

    // ── Distribute ───────────────────────────────────────────────────────

    private fun respondDistribute(
        state: GameState,
        decision: DistributeDecision,
        playerId: EntityId
    ): DecisionResponse {
        val projected = state.projectedState
        val opponentId = state.getOpponent(playerId)

        val distribution = mutableMapOf<EntityId, Int>()
        var remaining = decision.totalAmount

        // Assign minimums
        for (target in decision.targets) {
            distribution[target] = decision.minPerTarget
            remaining -= decision.minPerTarget
        }

        // Smart distribution: try to kill creatures, then hit opponent
        val targetPriority = decision.targets.sortedByDescending { target ->
            when {
                // Opponent player — good target but creatures first
                target == opponentId -> 5.0

                // Opponent creature — value killing it
                isOpponentCreature(state, target, playerId) -> {
                    val toughness = projected.getToughness(target) ?: 0
                    val damage = state.getEntity(target)?.get<com.wingedsheep.engine.state.components.battlefield.DamageComponent>()?.amount ?: 0
                    val remainingToughness = toughness - damage
                    val alreadyAssigned = distribution[target] ?: 0
                    val neededToKill = (remainingToughness - alreadyAssigned).coerceAtLeast(0)

                    // Prioritize creatures we can actually kill with remaining damage
                    if (neededToKill <= remaining) {
                        10.0 + creatureKillValue(state, target)
                    } else {
                        1.0 // can't kill it, low priority
                    }
                }

                // Own creature — avoid
                else -> -10.0
            }
        }

        for (target in targetPriority) {
            if (remaining <= 0) break
            val max = decision.maxPerTarget[target] ?: remaining
            val toAssign = remaining.coerceAtMost(max - (distribution[target] ?: 0))
            distribution[target] = (distribution[target] ?: 0) + toAssign
            remaining -= toAssign
        }

        return DistributionResponse(decision.id, distribution)
    }

    // ── Order objects ────────────────────────────────────────────────────

    private fun respondOrder(
        state: GameState,
        decision: OrderObjectsDecision,
        playerId: EntityId
    ): DecisionResponse {
        // For blocker ordering: kill the most threatening blocker first
        val projected = state.projectedState
        val ordered = decision.objects.sortedByDescending { entityId ->
            val power = projected.getPower(entityId) ?: 0
            val toughness = projected.getToughness(entityId) ?: 0
            val keywords = projected.getKeywords(entityId)

            var threat = power * 2.0 + toughness
            // Deathtouch blockers must die first
            if (Keyword.DEATHTOUCH.name in keywords) threat += 20.0
            // First strike blockers deal damage before us
            if (Keyword.FIRST_STRIKE.name in keywords) threat += 5.0
            if (Keyword.LIFELINK.name in keywords) threat += 3.0
            threat
        }
        return OrderedResponse(decision.id, ordered)
    }

    // ── Split piles ──────────────────────────────────────────────────────

    private fun respondSplitPiles(
        state: GameState,
        decision: SplitPilesDecision,
        playerId: EntityId
    ): DecisionResponse {
        // For Fact or Fiction style: opponent splits, we choose.
        // When WE split: make one pile clearly better so opponent's choice is harder.
        // Simple heuristic: put the best card alone, rest in other pile.
        val ranked = rankCardsByInfo(decision.cards, decision.cardInfo, state, playerId)
        val pile1 = ranked.take(1)
        val pile2 = ranked.drop(1)
        return PilesSplitResponse(decision.id, listOf(pile1, pile2))
    }

    // ── Choose option ────────────────────────────────────────────────────

    private fun respondOption(
        state: GameState,
        decision: ChooseOptionDecision,
        playerId: EntityId
    ): DecisionResponse {
        if (decision.options.size == 1) return OptionChosenResponse(decision.id, 0)

        val best = decision.options.indices.maxByOrNull { index ->
            evaluateResult(
                simulator.simulateDecision(state, OptionChosenResponse(decision.id, index)),
                playerId
            )
        }!!
        return OptionChosenResponse(decision.id, best)
    }

    // ── Damage assignment ────────────────────────────────────────────────

    private fun respondDamageAssignment(
        state: GameState,
        decision: AssignDamageDecision
    ): DecisionResponse {
        // Use the engine's defaults — lethal to each in order, rest to player
        return DamageAssignmentResponse(decision.id, decision.defaultAssignments)
    }

    // ── Library search ───────────────────────────────────────────────────

    private fun respondSearchLibrary(
        state: GameState,
        decision: SearchLibraryDecision,
        playerId: EntityId
    ): DecisionResponse {
        if (decision.options.isEmpty() || decision.maxSelections == 0) {
            return CardsSelectedResponse(decision.id, emptyList())
        }

        // Context-aware search: what does my board need?
        val ranked = decision.options.sortedByDescending { entityId ->
            searchCardContextualScore(state, decision.cards[entityId], playerId)
        }

        val count = decision.maxSelections.coerceAtMost(ranked.size)
        return CardsSelectedResponse(decision.id, ranked.take(count))
    }

    // ── Library reorder ──────────────────────────────────────────────────

    private fun respondReorderLibrary(
        state: GameState,
        decision: ReorderLibraryDecision,
        playerId: EntityId
    ): DecisionResponse {
        val ranked = decision.cards.sortedByDescending { entityId ->
            searchCardContextualScore(state, decision.cardInfo[entityId], playerId)
        }
        return OrderedResponse(decision.id, ranked)
    }

    // ── Mana sources ─────────────────────────────────────────────────────

    private fun respondManaSelection(decision: SelectManaSourcesDecision): DecisionResponse {
        return ManaSourcesSelectedResponse(decision.id, autoPay = true)
    }

    // ═════════════════════════════════════════════════════════════════════
    // Card ranking engine
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Rank cards considering board context. [wantToKeep] = true means higher = better to keep;
     * false means higher = better to discard/bottom.
     */
    private fun rankCardsContextual(
        state: GameState,
        cards: List<EntityId>,
        playerId: EntityId,
        wantToKeep: Boolean
    ): List<EntityId> {
        val projected = state.projectedState
        val myLands = projected.getBattlefieldControlledBy(playerId).count { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.isLand == true
        }
        val myCreatures = projected.getBattlefieldControlledBy(playerId).count { entityId ->
            projected.isCreature(entityId)
        }
        val handSize = state.getZone(playerId, Zone.HAND).size

        val scored = cards.map { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>()
            val score = if (card != null) {
                contextualCardScore(card, myLands, myCreatures, handSize)
            } else 0.0
            entityId to score
        }

        return if (wantToKeep) {
            scored.sortedByDescending { it.second }.map { it.first }
        } else {
            scored.sortedBy { it.second }.map { it.first }
        }
    }

    /**
     * Score a card based on what we need right now.
     * Higher = more valuable to have/keep.
     */
    private fun contextualCardScore(
        card: CardComponent,
        myLands: Int,
        myCreatures: Int,
        handSize: Int
    ): Double {
        val mv = card.manaValue
        val canCastNow = mv <= myLands

        // Lands: valuable early, bad late
        if (card.isLand) {
            return when {
                myLands <= 2 -> 8.0  // desperately need land
                myLands <= 4 -> 5.0  // still want land
                myLands <= 6 -> 2.0  // could use one more
                else -> 0.5          // flood — land is nearly worthless
            }
        }

        var score = 0.0

        // Castable spells are more valuable than ones we can't cast yet
        if (canCastNow) {
            score += 3.0
        } else {
            // Expensive spells we can't cast are less useful right now
            val turnsAway = (mv - myLands).coerceAtLeast(0)
            score -= turnsAway * 0.5
        }

        // Creatures are always useful
        if (card.isCreature) {
            score += 2.0
            if (myCreatures == 0) score += 3.0  // first creature is critical
            // Keyword value
            val keywords = card.baseKeywords
            if (Keyword.FLYING in keywords) score += 1.0
            if (Keyword.DEATHTOUCH in keywords) score += 1.0
            if (Keyword.LIFELINK in keywords) score += 0.5
        }

        // Removal / instants are flexible
        if (card.typeLine.isInstant) score += 2.5
        if (card.typeLine.isSorcery) score += 2.0

        // Higher mana value cards are generally more powerful (but only if castable)
        if (canCastNow) score += mv * 0.3

        return score
    }

    /** Score a SearchCardInfo for library search / reorder, considering board context. */
    private fun searchCardContextualScore(
        state: GameState,
        info: SearchCardInfo?,
        playerId: EntityId
    ): Double {
        if (info == null) return 0.0
        val projected = state.projectedState

        val myLands = projected.getBattlefieldControlledBy(playerId).count { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.isLand == true
        }
        val myCreatures = projected.getBattlefieldControlledBy(playerId).count { entityId ->
            projected.isCreature(entityId)
        }

        val isLand = info.typeLine.contains("Land", ignoreCase = true)
        val isCreature = info.typeLine.contains("Creature", ignoreCase = true)
        val isInstant = info.typeLine.contains("Instant", ignoreCase = true)

        if (isLand) {
            return when {
                myLands <= 2 -> 9.0
                myLands <= 4 -> 5.0
                else -> 1.0
            }
        }

        var score = 3.0
        if (isCreature) {
            score += 2.0
            if (myCreatures == 0) score += 3.0
        }
        if (isInstant) score += 1.0

        return score
    }

    private fun rankCardsByInfo(
        cards: List<EntityId>,
        cardInfo: Map<EntityId, SearchCardInfo>?,
        state: GameState,
        playerId: EntityId
    ): List<EntityId> {
        return cards.sortedByDescending { entityId ->
            searchCardContextualScore(state, cardInfo?.get(entityId), playerId)
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // Helpers
    // ═════════════════════════════════════════════════════════════════════

    private fun evaluateResult(result: SimulationResult, playerId: EntityId): Double {
        return evaluator.evaluate(result.state, result.state.projectedState, playerId)
    }

    private fun <T> pickBestBySimulation(
        state: GameState,
        candidates: List<T>,
        playerId: EntityId,
        buildResponse: (T) -> DecisionResponse
    ): T {
        return candidates.maxByOrNull { candidate ->
            evaluateResult(simulator.simulateDecision(state, buildResponse(candidate)), playerId)
        } ?: candidates.first()
    }

    private fun cancelOrFirst(decision: ChooseTargetsDecision): DecisionResponse {
        if (decision.canCancel) return CancelDecisionResponse(decision.id)
        val selected = decision.targetRequirements.associate { req ->
            req.index to (decision.legalTargets[req.index] ?: emptyList()).take(req.minTargets)
        }
        return TargetsResponse(decision.id, selected)
    }

    private fun isOpponentCreature(state: GameState, entityId: EntityId, playerId: EntityId): Boolean {
        val controller = state.projectedState.getController(entityId) ?: return false
        return controller != playerId && state.projectedState.isCreature(entityId)
    }

    private fun creatureKillValue(state: GameState, entityId: EntityId): Double {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0.0
        return BoardPresence.permanentValue(state, state.projectedState, entityId, card)
    }
}
