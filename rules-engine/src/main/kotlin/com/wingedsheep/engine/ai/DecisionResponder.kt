package com.wingedsheep.engine.ai

import com.wingedsheep.engine.ai.evaluation.BoardEvaluator
import com.wingedsheep.engine.ai.evaluation.BoardPresence
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Handles all [PendingDecision] types by evaluating possible responses
 * and picking the one that leads to the best board state.
 *
 * For decisions with small branching factors (yes/no, single color), it
 * simulates each option. For large branching factors (card selection from
 * many options), it uses heuristics to avoid combinatorial explosion.
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
        // For single-target requirements, try each target and pick the best
        if (decision.targetRequirements.size == 1) {
            val req = decision.targetRequirements.first()
            val targets = decision.legalTargets[req.index] ?: return cancelOrFirst(decision)
            if (targets.isEmpty()) return cancelOrFirst(decision)

            val best = pickBestBySimulation(state, targets, playerId) { target ->
                TargetsResponse(decision.id, mapOf(req.index to listOf(target)))
            }
            return TargetsResponse(decision.id, mapOf(req.index to listOf(best)))
        }

        // Multi-target: pick best for each requirement independently
        val selected = decision.targetRequirements.associate { req ->
            val targets = decision.legalTargets[req.index] ?: emptyList()
            val chosen = if (targets.isNotEmpty()) listOf(targets.first()) else emptyList()
            req.index to chosen
        }
        return TargetsResponse(decision.id, selected)
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

        // Forced selection
        if (min == options.size) {
            return CardsSelectedResponse(decision.id, options)
        }

        // Discard-like: discard worst cards
        if (min > 0 && decision.prompt.contains("discard", ignoreCase = true)) {
            val ranked = rankCardsForDiscard(state, options, playerId)
            return CardsSelectedResponse(decision.id, ranked.take(min))
        }

        // Select best cards up to max (e.g., scry to top, keep in hand)
        if (max > 0 && max < options.size) {
            val ranked = rankCardsByValue(state, options, playerId)
            return CardsSelectedResponse(decision.id, ranked.take(max))
        }

        // Default: select minimum required, preferring best
        val ranked = rankCardsByValue(state, options, playerId)
        return CardsSelectedResponse(decision.id, ranked.take(min.coerceAtLeast(0)))
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

        // Try each single mode and pick best
        val best = available.maxByOrNull { mode ->
            val response = ModesChosenResponse(decision.id, listOf(mode.index))
            evaluateResult(simulator.simulateDecision(state, response), playerId)
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
            val response = ColorChosenResponse(decision.id, color)
            evaluateResult(simulator.simulateDecision(state, response), playerId)
        }!!

        return ColorChosenResponse(decision.id, best)
    }

    // ── Number choice ────────────────────────────────────────────────────

    private fun respondNumber(
        state: GameState,
        decision: ChooseNumberDecision,
        playerId: EntityId
    ): DecisionResponse {
        // For small ranges, try each; for large ranges, sample key values
        val candidates = if (decision.maxValue - decision.minValue <= 10) {
            (decision.minValue..decision.maxValue).toList()
        } else {
            listOf(decision.minValue, decision.maxValue, (decision.minValue + decision.maxValue) / 2)
        }

        val best = candidates.maxByOrNull { n ->
            val response = NumberChosenResponse(decision.id, n)
            evaluateResult(simulator.simulateDecision(state, response), playerId)
        }!!

        return NumberChosenResponse(decision.id, best)
    }

    // ── Distribute ───────────────────────────────────────────────────────

    private fun respondDistribute(
        state: GameState,
        decision: DistributeDecision,
        playerId: EntityId
    ): DecisionResponse {
        val opponentId = state.getOpponent(playerId)

        // Heuristic: concentrate damage on enemy creatures/players, spread minimums to own
        val distribution = mutableMapOf<EntityId, Int>()
        var remaining = decision.totalAmount

        // Assign minimums first
        for (target in decision.targets) {
            val min = decision.minPerTarget
            distribution[target] = min
            remaining -= min
        }

        // Put remaining on the best target (opponent's creature or opponent player)
        val bestTarget = decision.targets.maxByOrNull { target ->
            if (target == opponentId) 10.0 // prefer hitting opponent
            else if (isOpponentCreature(state, target, playerId)) creatureKillValue(state, target)
            else -5.0 // don't want to damage own stuff
        } ?: decision.targets.first()

        distribution[bestTarget] = (distribution[bestTarget] ?: 0) + remaining
        return DistributionResponse(decision.id, distribution)
    }

    // ── Order objects ────────────────────────────────────────────────────

    private fun respondOrder(
        state: GameState,
        decision: OrderObjectsDecision,
        playerId: EntityId
    ): DecisionResponse {
        // For blocker ordering: order by threat (highest power first = kill first)
        val ordered = decision.objects.sortedByDescending { entityId ->
            val projected = state.projectedState
            val power = projected.getPower(entityId) ?: 0
            val toughness = projected.getToughness(entityId) ?: 0
            power + toughness
        }
        return OrderedResponse(decision.id, ordered)
    }

    // ── Split piles ──────────────────────────────────────────────────────

    private fun respondSplitPiles(
        state: GameState,
        decision: SplitPilesDecision,
        playerId: EntityId
    ): DecisionResponse {
        // Split into best half and worst half
        val ranked = rankCardsByValue(state, decision.cards, playerId)
        val half = ranked.size / 2
        val pile1 = ranked.take(half)
        val pile2 = ranked.drop(half)
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
            val response = OptionChosenResponse(decision.id, index)
            evaluateResult(simulator.simulateDecision(state, response), playerId)
        }!!

        return OptionChosenResponse(decision.id, best)
    }

    // ── Damage assignment ────────────────────────────────────────────────

    private fun respondDamageAssignment(
        state: GameState,
        decision: AssignDamageDecision
    ): DecisionResponse {
        // Use the engine's pre-computed default: lethal to each blocker in order, rest to player
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

        // Rank by mana value (prefer cards we can cast soon)
        val ranked = decision.options.sortedByDescending { entityId ->
            val info = decision.cards[entityId]
            searchCardScore(info)
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
        // Put best cards on top (first in list = top of library)
        val ranked = decision.cards.sortedByDescending { entityId ->
            val info = decision.cardInfo[entityId]
            searchCardScore(info)
        }
        return OrderedResponse(decision.id, ranked)
    }

    // ── Mana sources ─────────────────────────────────────────────────────

    private fun respondManaSelection(decision: SelectManaSourcesDecision): DecisionResponse {
        // Always auto-pay — the solver's suggestion is good enough
        return ManaSourcesSelectedResponse(decision.id, autoPay = true)
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private fun evaluateResult(result: SimulationResult, playerId: EntityId): Double {
        val state = result.state
        return evaluator.evaluate(state, state.projectedState, playerId)
    }

    private fun <T> pickBestBySimulation(
        state: GameState,
        candidates: List<T>,
        playerId: EntityId,
        buildResponse: (T) -> DecisionResponse
    ): T {
        return candidates.maxByOrNull { candidate ->
            val response = buildResponse(candidate)
            evaluateResult(simulator.simulateDecision(state, response), playerId)
        } ?: candidates.first()
    }

    private fun cancelOrFirst(decision: ChooseTargetsDecision): DecisionResponse {
        if (decision.canCancel) return CancelDecisionResponse(decision.id)
        // Fallback: pick first legal target for each requirement
        val selected = decision.targetRequirements.associate { req ->
            val targets = decision.legalTargets[req.index] ?: emptyList()
            req.index to targets.take(req.minTargets)
        }
        return TargetsResponse(decision.id, selected)
    }

    /** Rank cards by "what I'd prefer to discard" — low value = discard first. */
    private fun rankCardsForDiscard(state: GameState, cards: List<EntityId>, playerId: EntityId): List<EntityId> {
        return cards.sortedBy { entityId -> cardRetainValue(state, entityId) }
    }

    /** Rank cards by general desirability — high value first. */
    private fun rankCardsByValue(state: GameState, cards: List<EntityId>, playerId: EntityId): List<EntityId> {
        return cards.sortedByDescending { entityId -> cardRetainValue(state, entityId) }
    }

    /** How much we want to keep this card (higher = keep). */
    private fun cardRetainValue(state: GameState, entityId: EntityId): Double {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0.0
        // Creatures are generally more valuable than non-creatures
        var value = card.manaValue.toDouble()
        if (card.isCreature) value += 1.0
        if (card.isLand) value -= 0.5  // lands are less valuable to keep when discarding
        return value
    }

    private fun isOpponentCreature(state: GameState, entityId: EntityId, playerId: EntityId): Boolean {
        val controller = state.projectedState.getController(entityId) ?: return false
        return controller != playerId && state.projectedState.isCreature(entityId)
    }

    private fun creatureKillValue(state: GameState, entityId: EntityId): Double {
        val power = state.projectedState.getPower(entityId) ?: 0
        val toughness = state.projectedState.getToughness(entityId) ?: 0
        return (power + toughness).toDouble()
    }

    private fun searchCardScore(info: SearchCardInfo?): Double {
        if (info == null) return 0.0
        // Prefer non-lands with moderate mana cost
        val isLand = info.typeLine.contains("Land", ignoreCase = true)
        val isCreature = info.typeLine.contains("Creature", ignoreCase = true)
        return when {
            isLand -> 2.0
            isCreature -> 5.0
            else -> 4.0
        }
    }
}
