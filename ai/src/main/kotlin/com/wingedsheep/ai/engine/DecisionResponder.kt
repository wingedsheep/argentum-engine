package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.advisor.AdvisorDecisionContext
import com.wingedsheep.ai.engine.advisor.CardAdvisorRegistry
import com.wingedsheep.ai.engine.budget.BudgetPolicy
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.budget.LegacyBudgetPolicy
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.SkipDrawStepComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * Handles all [PendingDecision] types by evaluating possible responses
 * and picking the one that leads to the best board state.
 *
 * For decisions with small branching factors (yes/no, color, mode), it
 * simulates each option. For larger spaces, it uses MTG-aware heuristics.
 *
 * Card-specific overrides are checked first via [CardAdvisorRegistry].
 *
 * **On the decision budget:** every scan in here is already bounded to at most ~11 simulations by
 * construction (a yes/no is 2, a colour is 5, a number is sampled to 11, targets are pre-ranked and
 * truncated), which is at or below what even [com.wingedsheep.ai.engine.budget.BudgetTier.ROUTINE]
 * allows. The one place a budget can genuinely bind is the target pre-rank cut, so that is the one
 * place it is wired. Threading it through the other twenty responders would be plumbing that
 * changes no number.
 */
class DecisionResponder(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
    private val advisorRegistry: CardAdvisorRegistry = CardAdvisorRegistry(),
    private val budgetPolicy: BudgetPolicy = LegacyBudgetPolicy,
    /**
     * Phase 6: structural card knowledge. Reaches the two places this class ranks *permanents* —
     * which one to sacrifice, and which one a removal decision should kill — so a mid-resolution
     * decision prices an opposing Icy Manipulator the way `Strategist` now does.
     * [IntentCatalog.NONE] is the off position and leaves both at their pre-Phase-6 behaviour.
     */
    private val intents: IntentCatalog = IntentCatalog.NONE,
    /**
     * [AiProfile.castabilityAwareCardSelection]: rank keep / take / discard / search choices with
     * [CardSelectionValue] instead of the battlefield-land-count heuristic. Off leaves every one of
     * those answers exactly as it was.
     */
    private val castabilityAwareCardSelection: Boolean = false,
    /**
     * [AiProfile.informedChoiceDecisions]: break simulation ties on colour, land-type,
     * creature-type and card-name choices with [ChoicePriors], shortlist the open-ended name and
     * creature-type lists instead of simulating every entry, and price a pending "skip your draw
     * step" marker as the card it costs. Off is the historical behaviour, byte for byte.
     */
    private val informedChoices: Boolean = false,
) {
    private val priors: ChoicePriors? = if (informedChoices) ChoicePriors(simulator.cardRegistry) else null

    var forcedPlayPicker: (GameState, EntityId) -> GameAction = simulator::completeForcedPlay

    fun respond(state: GameState, decision: PendingDecision, playerId: EntityId): DecisionResponse {
        // Try card-specific advisor first
        val sourceName = decision.context.sourceName
        if (sourceName != null) {
            val advisor = advisorRegistry.getAdvisor(sourceName)
            if (advisor != null) {
                val ctx = AdvisorDecisionContext(
                    state = state,
                    projected = state.projectedState,
                    playerId = playerId,
                    decision = decision,
                    sourceCardName = sourceName,
                    evaluator = evaluator,
                    simulator = simulator
                )
                advisor.respondToDecision(ctx)?.let { return it }
            }
        }

        // Fall through to generic logic
        return when (decision) {
            is PlayCardDecision -> PlayCardResponse(decision.id,
                forcedPlayPicker(state, decision.playerId))
            is ChooseTargetsDecision ->
                respondTargets(state, decision, playerId, budgetPolicy.budgetForDecision(state, playerId))
            is SelectCardsDecision -> respondSelectCards(state, decision, playerId)
            is YesNoDecision -> respondYesNo(state, decision, playerId)
            is BatchYesNoDecision -> respondBatchYesNo(state, decision, playerId)
            is ChooseModeDecision -> respondModes(state, decision, playerId)
            is ChooseColorDecision -> respondColor(state, decision, playerId)
            is ChooseNumberDecision -> respondNumber(state, decision, playerId)
            is DistributeDecision -> respondDistribute(state, decision, playerId)
            is OrderObjectsDecision -> respondOrder(state, decision, playerId)
            is SplitPilesDecision -> respondSplitPiles(state, decision, playerId)
            is ChooseOptionDecision -> respondOption(state, decision, playerId)
            is ChooseReplacementDecision -> respondReplacement(decision)
            is BudgetModalDecision -> respondBudgetModal(decision)
            is AssignDamageDecision -> respondDamageAssignment(state, decision)
            is CombatResolutionDecision -> respondCombatResolution(decision)
            is SearchLibraryDecision -> respondSearchLibrary(state, decision, playerId)
            is ReorderLibraryDecision -> respondReorderLibrary(state, decision, playerId)
            is SelectManaSourcesDecision -> respondManaSelection(decision)
        }
    }

    // ── Text-change replacement (Crystal Spray, Artificial Evolution) ────

    /**
     * Picks the pre-selected (on-card) FROM word and a valid same-category replacement. A heuristic
     * default — text-changing rarely matters to the AI, so this just makes a legal, non-degenerate
     * choice rather than simulating.
     */
    private fun respondReplacement(decision: ChooseReplacementDecision): DecisionResponse {
        val fromIndex = decision.defaultFromIndex?.takeIf { it in decision.fromOptions.indices } ?: 0
        val allowed = decision.allowedToByFrom.getOrNull(fromIndex)
        val toIndex = allowed?.firstOrNull()
            ?: decision.toOptions.indices.firstOrNull { decision.toOptions[it] != decision.fromOptions.getOrNull(fromIndex) }
            ?: 0
        return ReplacementChosenResponse(decision.id, fromIndex, toIndex)
    }

    // ── Target selection ─────────────────────────────────────────────────

    private fun respondTargets(
        state: GameState,
        decision: ChooseTargetsDecision,
        playerId: EntityId,
        budget: DecisionBudget,
    ): DecisionResponse {
        val maxCandidates = budget.allowances.targetCandidates
        if (decision.targetRequirements.size == 1) {
            val req = decision.targetRequirements.first()
            val targets = decision.legalTargets[req.index] ?: return cancelOrFirst(decision)
            if (targets.isEmpty()) return cancelOrFirst(decision)

            // For small target pools, simulate each; for large pools, use heuristics then simulate top candidates
            val candidates = if (targets.size <= maxCandidates) {
                targets
            } else {
                targets.sortedByDescending { targetHeuristic(state, it, playerId) }.take(maxCandidates)
            }

            val best = pickBestBySimulation(state, candidates, playerId) { target ->
                TargetsResponse(decision.id, mapOf(req.index to listOf(target)))
            }
            val bestResponse = TargetsResponse(decision.id, mapOf(req.index to listOf(best)))

            // For optional targets (minTargets == 0), also consider picking no targets
            if (req.minTargets == 0) {
                val skipResponse = TargetsResponse(decision.id, mapOf(req.index to emptyList()))
                val bestScore = evaluateResult(simulator.simulateDecision(state, bestResponse), playerId)
                val skipScore = evaluateResult(simulator.simulateDecision(state, skipResponse), playerId)
                if (skipScore >= bestScore) return skipResponse
            }

            return bestResponse
        }

        // Multi-target: choose each requirement against a complete, progressively updated answer. Every probe
        // still has to be a *complete* answer to the decision — `DecisionValidators.validateTargets`
        // rejects a response that leaves a mandatory requirement out, and a rejected probe comes
        // back as `SimulationResult.Illegal` carrying the unchanged state, so every candidate would
        // score identically: the pick would collapse to "the first legal target", and the optional
        // pick-vs-skip comparison below would tie and always skip. Varying one requirement against a
        // complete baseline for the others keeps the comparison meaningful. Carry each selected
        // answer into the next probe: independently legal replacements can conflict when combined
        // (two copy slots choosing the same object for one target requirement).
        val selected = minimalCompleteSelection(decision).toMutableMap()
        for (req in decision.targetRequirements) {
            val targets = decision.legalTargets[req.index] ?: emptyList()
            selected[req.index] = if (targets.isEmpty()) {
                emptyList()
            } else {
                val best = pickBestBySimulation(state, targets.take(maxCandidates), playerId) { target ->
                    TargetsResponse(decision.id, selected + (req.index to listOf(target)))
                }
                // For optional targets, compare best pick against skipping
                if (req.minTargets == 0) {
                    val pickResponse = TargetsResponse(decision.id, selected + (req.index to listOf(best)))
                    val skipResponse = TargetsResponse(decision.id, selected + (req.index to emptyList()))
                    val pickScore = evaluateResult(simulator.simulateDecision(state, pickResponse), playerId)
                    val skipScore = evaluateResult(simulator.simulateDecision(state, skipResponse), playerId)
                    if (skipScore >= pickScore) emptyList()
                    else listOf(best)
                } else {
                    listOf(best)
                }
            }
        }
        return TargetsResponse(decision.id, selected.toMap())
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

        // Players — prefer an opponent (any of them; CR 810 teammates are not opponents)
        if (state.isOpponentTo(targetId, playerId)) return 3.0

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

        val untapChoice = (state.peekContinuation() as? Suspension)?.takeIf {
            it.question.id == decision.id
        }?.answer as? UntapChoiceContinuation
        if (untapChoice != null && untapChoice.untapLimits.isNotEmpty()) {
            // Start with a legal keep set, then release valuable permanents while every cap holds.
            // The displayed minimum is only a lower bound when restrictions overlap or are disjoint.
            val keep = options.toMutableSet()
            val ranked = rankCardsContextual(state, options, playerId, wantToKeep = true, legacyOnly = true)
            for (id in ranked) {
                keep.remove(id)
                if (untapChoice.untapLimits.any { limit ->
                        limit.matchingPermanents.count { it !in keep } > limit.max
                    }) keep.add(id)
            }
            return CardsSelectedResponse(decision.id, options.filter { it in keep })
        }

        // A `minTotalManaValue` floor (collect evidence N, CR 701.59a) is a *sum* gate, so none of
        // the count-based branches below can satisfy it — taking `min` cheap cards submits an
        // illegal selection the validator rejects, and taking zero silently declines an optional
        // one (a ward cost) every time. Pay it with the fewest cards, highest mana values first,
        // which is the same choice `CollectEvidenceResolver.autoSelect` makes; submit nothing when
        // the pool can't reach the floor, which CR 701.59b makes the only legal answer anyway.
        decision.minTotalManaValue?.let { floor ->
            val byValueDesc = options.sortedByDescending { manaValueOf(state, it) }
            val payment = mutableListOf<EntityId>()
            var total = 0
            for (cardId in byValueDesc) {
                if (total >= floor || payment.size >= max) break
                payment.add(cardId)
                total += manaValueOf(state, cardId)
            }
            return CardsSelectedResponse(decision.id, if (total >= floor) payment else emptyList())
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
                CardsSelectedResponse(decision.id, minimumLegalSelection(decision, ranked))
            }
            isSacrifice -> {
                // Sacrifice least valuable permanents
                val ranked = options.sortedBy { entityId ->
                    val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@sortedBy 0.0
                    BoardPresence.permanentValue(state, state.projectedState, entityId, card, intents)
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

    /**
     * Mana value of a card in a non-battlefield zone — intrinsic to the card (CR 202.3), so the
     * base [CardComponent] is the correct read. Unreadable entities count 0, which keeps a
     * mana-value floor failing closed.
     */
    private fun manaValueOf(state: GameState, cardId: EntityId): Int =
        state.getEntity(cardId)?.get<CardComponent>()?.manaValue ?: 0

    /** Prefer the smallest legal selection, honoring reductions such as “discard two unless one is a creature.” */
    private fun minimumLegalSelection(
        decision: SelectCardsDecision,
        ranked: List<EntityId>,
    ): List<EntityId> {
        val ordinaryCount = decision.minSelections.coerceAtLeast(1).coerceAtMost(decision.maxSelections)
        val reduced = decision.conditionalMinimums
            .sortedBy { it.minimumSelections }
            .firstNotNullOfOrNull { condition ->
                val matches = ranked.filter { it in condition.matchingOptions }.take(condition.requiredMatches)
                if (matches.size < condition.requiredMatches) return@firstNotNullOfOrNull null
                val count = condition.minimumSelections.coerceAtLeast(matches.size)
                if (count > decision.maxSelections) return@firstNotNullOfOrNull null
                matches + ranked.filterNot(matches::contains).take(count - matches.size)
            }
        return reduced ?: ranked.take(ordinaryCount)
    }

    // ── Yes / No ─────────────────────────────────────────────────────────

    private fun respondYesNo(
        state: GameState,
        decision: YesNoDecision,
        playerId: EntityId
    ): DecisionResponse {
        val yesResult = simulator.simulateDecision(state, YesNoResponse(decision.id, true))
        val noResult = simulator.simulateDecision(state, YesNoResponse(decision.id, false))
        val yesScore = evaluateChoice(yesResult, playerId)
        val noScore = evaluateChoice(noResult, playerId)
        return YesNoResponse(decision.id, yesScore >= noScore)
    }

    /**
     * Batched "you may …" raised once for a run of identical optional triggers. The AI evaluates the
     * two whole-run outcomes (yes-to-all vs no-to-all) and applies the better one to the entire run —
     * the same value the AI would reach answering each instance the same way, in one decision.
     */
    private fun respondBatchYesNo(
        state: GameState,
        decision: BatchYesNoDecision,
        playerId: EntityId
    ): DecisionResponse {
        val yesResult = simulator.simulateDecision(state, BatchYesNoResponse(decision.id, choice = true, applyToAll = true))
        val noResult = simulator.simulateDecision(state, BatchYesNoResponse(decision.id, choice = false, applyToAll = true))
        val yesScore = evaluateChoice(yesResult, playerId)
        val noScore = evaluateChoice(noResult, playerId)
        return BatchYesNoResponse(decision.id, choice = yesScore >= noScore, applyToAll = true)
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
        priors?.let { priors ->
            // Most colour choices are invisible to a one-step simulation — the colour a land will
            // tap for, the colour a creature gains protection from — so every option ties and the
            // first one (white) used to win. Break the tie towards the colour that matters.
            val polarity = priors.polarityOf(state, decision.context.sourceId, ChoicePriors.Polarity.OPPONENT)
            val weights = priors.colorWeights(state, playerId, polarity)
            val best = bestWithTieBreak(decision.availableColors.toList(), { weights[it] ?: 0.0 }) { color ->
                evaluateChoice(simulator.simulateDecision(state, ColorChosenResponse(decision.id, color)), playerId)
            }
            return ColorChosenResponse(decision.id, best)
        }
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
                // Opponent player — good target but creatures first. Any opponent, not just the
                // first one in turn order; a teammate is never one (CR 810).
                state.isOpponentTo(target, playerId) -> 5.0

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
        decision.suggestedPiles?.let { return PilesSplitResponse(decision.id, it) }
        // For Fact or Fiction style: opponent splits, we choose.
        // When WE split: make one pile clearly better so opponent's choice is harder.
        // Simple heuristic: put the best card alone, rest in other pile.
        val ranked = rankCardsByInfo(decision.cards, decision.cardInfo, state, playerId)
        val piles = List(decision.numberOfPiles) { mutableListOf<EntityId>() }
        for ((index, card) in ranked.withIndex()) piles[index % piles.size].add(card)
        return PilesSplitResponse(decision.id, piles)
    }

    // ── Choose option ────────────────────────────────────────────────────

    private fun respondOption(
        state: GameState,
        decision: ChooseOptionDecision,
        playerId: EntityId
    ): DecisionResponse {
        if (decision.options.size == 1) return OptionChosenResponse(decision.id, 0)
        if (priors != null) return respondOptionInformed(state, decision, playerId, priors)

        val best = decision.options.indices.maxByOrNull { index ->
            evaluateResult(
                simulator.simulateDecision(state, OptionChosenResponse(decision.id, index)),
                playerId
            )
        }!!
        return OptionChosenResponse(decision.id, best)
    }

    /**
     * [respondOption] with [ChoicePriors]. Four shapes are recognised by their option lists, since
     * a [ChooseOptionDecision] carries no type tag:
     *
     * - **Basic land types** and **colours**: simulate all five, break ties with the prior. A
     *   landwalk grant leans to the land types the opponent controls; a mana source's own choice
     *   leans to its controller's colours.
     * - **Creature types** and **card names** — the open-ended lists, hundreds or thousands long.
     *   Simulating each was the old cost, and it bought nothing: almost every entry names nothing in
     *   the game, so they all tie and the alphabetically first won ("A Killer Among Us" against a
     *   Lorwyn deck). Only names and types that actually occur among the players' cards are
     *   simulated — the best few of each side — and a tie leans to the opponent's cards for names
     *   (strip, mill, shut off) and to our own for creature types (lords, tribal payoffs).
     *
     * Anything else (modes, opponents, a short custom list) keeps the plain simulation.
     */
    private fun respondOptionInformed(
        state: GameState,
        decision: ChooseOptionDecision,
        playerId: EntityId,
        priors: ChoicePriors,
    ): DecisionResponse {
        val options = decision.options
        val simulate = { index: Int ->
            evaluateChoice(simulator.simulateDecision(state, OptionChosenResponse(decision.id, index)), playerId)
        }
        val sourceId = decision.context.sourceId

        val landTypes = options.map { option ->
            ChoicePriors.BASIC_LAND_COLORS.keys.firstOrNull { it.equals(option, ignoreCase = true) }
        }
        if (landTypes.all { it != null }) {
            val polarity = priors.polarityOf(state, sourceId, ChoicePriors.Polarity.OPPONENT)
            val weights = priors.landTypeWeights(state, playerId, polarity)
            val best = bestWithTieBreak(options.indices.toList(), { weights[landTypes[it]] ?: 0.0 }, simulate)
            return OptionChosenResponse(decision.id, best)
        }

        val colors = options.map { option -> Color.entries.firstOrNull { it.displayName.equals(option, ignoreCase = true) } }
        if (colors.all { it != null }) {
            val polarity = priors.polarityOf(state, sourceId, ChoicePriors.Polarity.OPPONENT)
            val weights = priors.colorWeights(state, playerId, polarity)
            val best = bestWithTieBreak(options.indices.toList(), { weights[colors[it]] ?: 0.0 }, simulate)
            return OptionChosenResponse(decision.id, best)
        }

        if (options.size <= OPEN_LIST_THRESHOLD) {
            return OptionChosenResponse(decision.id, options.indices.maxByOrNull(simulate)!!)
        }

        val creatureTypes = options.all { it.lowercase() in CREATURE_TYPES_LOWER }
        if (!creatureTypes && options.take(3).any { simulator.cardRegistry.getCard(it) == null }) {
            // A long list that is neither creature types nor card names: no prior applies.
            return OptionChosenResponse(decision.id, options.indices.maxByOrNull(simulate)!!)
        }
        val ours = listOf(playerId)
        val theirs = state.getOpponents(playerId)
        val (mine, opposing) = if (creatureTypes) {
            priors.creatureTypeWeights(state, ours) to priors.creatureTypeWeights(state, theirs)
        } else {
            priors.nameWeights(state, ours) to priors.nameWeights(state, theirs)
        }
        val key = { option: String -> if (creatureTypes) option.lowercase() else option }
        fun shortlist(weights: Map<String, Double>): List<Int> = options.indices
            .filter { (weights[key(options[it])] ?: 0.0) > 0.0 }
            .sortedByDescending { weights[key(options[it])] }
            .take(OPEN_LIST_CANDIDATES_PER_SIDE)
        val candidates = (shortlist(mine) + shortlist(opposing)).distinct()
        if (candidates.isEmpty()) return OptionChosenResponse(decision.id, 0)

        val lean = if (creatureTypes) mine else opposing
        val best = bestWithTieBreak(candidates, { lean[key(options[it])] ?: 0.0 }, simulate)
        return OptionChosenResponse(decision.id, best)
    }

    /**
     * The highest-scoring candidate, with ties — equal up to floating-point noise — going to the
     * one [preference] rates highest, and any tie left after that to the earliest.
     */
    private fun <T> bestWithTieBreak(candidates: List<T>, preference: (T) -> Double, score: (T) -> Double): T {
        val scored = candidates.map { it to score(it) }
        val top = scored.maxOf { it.second }
        val tolerance = TIE_EPSILON * maxOf(1.0, kotlin.math.abs(top))
        val tied = scored.filter { (_, s) -> s == top || top - s <= tolerance }.map { it.first }
        var best = tied.first()
        for (candidate in tied) if (preference(candidate) > preference(best)) best = candidate
        return best
    }

    // ── Budget modal ─────────────────────────────────────────────────────

    private fun respondBudgetModal(decision: BudgetModalDecision): DecisionResponse {
        // Greedy: pick cheapest mode repeatedly until budget exhausted
        val sorted = decision.modes.withIndex().sortedBy { it.value.cost }
        val selected = mutableListOf<Int>()
        var remaining = decision.budget
        for ((idx, mode) in sorted) {
            if (mode.cost > remaining) continue
            // A free mode never consumes budget, so repeating it would spin forever.
            // Take it once and move on. (Rare in production, frequent under playouts.)
            if (mode.cost <= 0) {
                selected.add(idx)
                continue
            }
            while (mode.cost <= remaining) {
                selected.add(idx)
                remaining -= mode.cost
            }
        }
        return BudgetModalResponse(decision.id, selected)
    }

    // ── Damage assignment ────────────────────────────────────────────────

    private fun respondDamageAssignment(
        state: GameState,
        decision: AssignDamageDecision
    ): DecisionResponse {
        // Use the engine's defaults — lethal to each in order, rest to player
        return DamageAssignmentResponse(decision.id, decision.defaultAssignments)
    }

    private fun respondCombatResolution(decision: CombatResolutionDecision): DecisionResponse {
        // Confirm the engine-computed default edge amounts (lethal-first, rest to drain).
        return CombatResolutionResponse(decision.id, decision.edges.map { DamageEdgeAmount(it.id, it.amount) })
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
        val ranked = if (castabilityAwareCardSelection) {
            rankCardsContextual(state, decision.options, playerId, wantToKeep = true)
        } else {
            decision.options.sortedByDescending { entityId ->
                searchCardContextualScore(state, decision.cards[entityId], playerId)
            }
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
        val ranked = if (castabilityAwareCardSelection) {
            rankCardsContextual(state, decision.cards, playerId, wantToKeep = true)
        } else {
            decision.cards.sortedByDescending { entityId ->
                searchCardContextualScore(state, decision.cardInfo[entityId], playerId)
            }
        }
        return OrderedResponse(decision.id, ranked)
    }

    // ── Mana sources ─────────────────────────────────────────────────────

    private fun respondManaSelection(decision: SelectManaSourcesDecision): DecisionResponse {
        // Auto-pay uses the engine's solver, which excludes Treasures (sacrifice
        // sub-cost) and Springleaf-Drum-style sources (tap-permanent sub-cost). When
        // the solver finds no solution, [autoPaySuggestion] is empty — submitting
        // autoPay=true would error inside the resumer ("Cannot pay mana cost with
        // auto-pay") and the engine would re-prompt the same decision, freezing the
        // AI in an infinite retry loop.
        if (decision.autoPaySuggestion.isNotEmpty()) {
            return ManaSourcesSelectedResponse(decision.id, autoPay = true)
        }

        // No auto-pay solution. If declining is allowed (e.g. "you may pay"),
        // decline rather than burn permanents on an optional effect.
        if (decision.canDecline) {
            return ManaSourcesSelectedResponse(
                decision.id,
                autoPay = false,
                selectedSources = emptyList()
            )
        }

        // Mandatory payment (ward, counter-unless-pays) — fall back to manual
        // selection of every available source so the resumer sacrifices Treasures
        // and taps lands as needed. Skip sub-cost sources (Springleaf Drum) since
        // the AI can't currently answer the follow-up tap-permanent prompt.
        return ManaSourcesSelectedResponse(
            decision.id,
            autoPay = false,
            selectedSources = decision.availableSources
                .filterNot { it.requiresTappingAnotherPermanent }
                .map { it.entityId }
        )
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
        wantToKeep: Boolean,
        // The untap-limit choice ranks *permanents* by how much they are worth untapped, a
        // different question from which card to hold; it keeps the legacy reading either way.
        legacyOnly: Boolean = false,
    ): List<EntityId> {
        if (castabilityAwareCardSelection && !legacyOnly) {
            val value = CardSelectionValue.of(state, playerId, intents)
            // A stable sort on the score keeps the decision's own order for ties, so identical
            // copies resolve the same way they always did.
            val scored = cards.map { it to value.score(it) }
            return if (wantToKeep) scored.sortedByDescending { it.second }.map { it.first }
            else scored.sortedBy { it.second }.map { it.first }
        }
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

    /**
     * The scoring chokepoint every `respond*` comparison runs through.
     *
     * A candidate whose automatic resolution never finished ranks below every candidate that
     * reached a real boundary: the AI still has to answer the decision it was asked, and refusing
     * to answer at all is how a live game wedges with no backstop able to see it.
     */
    private fun evaluateResult(result: SimulationResult, playerId: EntityId): Double =
        if (result is SimulationResult.Illegal) Double.NEGATIVE_INFINITY
        else result.scoreOrRankLast { evaluator.evaluate(it, it.projectedState, playerId) }

    /**
     * [evaluateResult] plus, under [informedChoices], the price of every pending "skip your next
     * draw step" marker in the result.
     *
     * The marker is the whole cost of a Fasting-style "skip your draw step; if you do, gain 2 life",
     * and a one-step simulation can't see it: the choice is made in the upkeep, the simulation
     * stops at the next quiet state — still the upkeep — and the card that would have been drawn
     * never reaches the board either branch is scored on. So the "yes" branch showed +2 life against
     * nothing, and a player stuck on two lands skipped five draws in a row. Pricing the marker as
     * the card it costs restores the trade the choice actually is. The marker on an opponent is
     * the same card from the other side, and so counts for us.
     */
    private fun evaluateChoice(result: SimulationResult, playerId: EntityId): Double {
        val base = evaluateResult(result, playerId)
        if (!informedChoices || result is SimulationResult.Illegal || base.isInfinite()) return base
        val state = result.state
        if (state.gameOver) return base
        var adjusted = base
        for (player in state.turnOrder) {
            if (state.getEntity(player)?.has<SkipDrawStepComponent>() != true) continue
            adjusted -= drawValue(state, player, playerId)
        }
        return adjusted
    }

    /**
     * What [drawer] drawing one card is worth to [viewer]: the mean evaluation change of that card
     * arriving, over a spread of [drawer]'s library.
     *
     * A spread rather than the top card on purpose — the top card is hidden even from its owner,
     * and pricing the skip by it would let the answer depend on information the player doesn't
     * have. Sampling across the library uses only the library's contents, which its owner knows.
     *
     * A drawn land the drawer has a land drop waiting for is priced **on the battlefield**, not in
     * hand. The draw step is followed by a main phase, so that land is played this turn; pricing it
     * as a card in hand charges a land-starved player the hand curve's marginal rate (0.8 at a hand
     * of five) for the very land that unlocks the hand — Fasting's game-7 victim was on one mana
     * source with two-drops stuck behind it, and that is the trade it kept getting wrong. Any other
     * card is priced in hand.
     */
    private fun drawValue(state: GameState, drawer: EntityId, viewer: EntityId): Double {
        val library = state.getZone(drawer, Zone.LIBRARY)
        if (library.isEmpty()) return 0.0
        val samples = DRAW_VALUE_SAMPLES.coerceAtMost(library.size)
        val stride = library.size.toDouble() / samples
        val before = evaluator.evaluate(state, state.projectedState, viewer)
        val from = ZoneKey(drawer, Zone.LIBRARY)
        val dropWaiting = landDropWaiting(state, drawer)
        return (0 until samples).sumOf { i ->
            val cardId = library[(i * stride).toInt()]
            val isLand = state.getEntity(cardId)?.get<CardComponent>()?.typeLine?.isLand == true
            val drawn = if (isLand && dropWaiting) {
                state.moveToZone(cardId, from, ZoneKey(drawer, Zone.BATTLEFIELD))
                    .updateEntity(cardId) { it.with(ControllerComponent(drawer)) }
            } else {
                state.moveToZone(cardId, from, ZoneKey(drawer, Zone.HAND))
            }
            evaluator.evaluate(drawn, drawn.projectedState, viewer) - before
        } / samples
    }

    /**
     * Whether [player]'s next land drop is still open and no land in hand is already queued for it.
     * Off-turn the drop resets before their next main phase, so it is open by definition.
     */
    private fun landDropWaiting(state: GameState, player: EntityId): Boolean {
        val dropOpen = state.activePlayerId != player ||
            (state.getEntity(player)?.get<LandDropsComponent>()?.remaining ?: 1) > 0
        val landInHand = state.getZone(player, Zone.HAND).any {
            state.getEntity(it)?.get<CardComponent>()?.typeLine?.isLand == true
        }
        return dropOpen && !landInHand
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

    /**
     * The cheapest *complete* answer to [decision]: every declared requirement filled to its
     * minimum from the targets that are legal for it, optional ones left empty.
     *
     * Completeness is the point. `DecisionValidators.validateTargets` rejects a response that omits
     * a mandatory requirement, so a partial map is not a weaker answer but an illegal one — which
     * is why this doubles as the fixed background a per-requirement probe varies one slot against.
     */
    internal fun minimalCompleteSelection(decision: ChooseTargetsDecision): Map<Int, List<EntityId>> =
        decision.targetRequirements.associate { req ->
            req.index to (decision.legalTargets[req.index] ?: emptyList()).take(req.minTargets)
        }

    private fun cancelOrFirst(decision: ChooseTargetsDecision): DecisionResponse {
        if (decision.canCancel) return CancelDecisionResponse(decision.id)
        val selected = minimalCompleteSelection(decision)
        return TargetsResponse(decision.id, selected)
    }

    private fun isOpponentCreature(state: GameState, entityId: EntityId, playerId: EntityId): Boolean {
        val controller = state.projectedState.getController(entityId) ?: return false
        return controller != playerId && state.projectedState.isCreature(entityId)
    }

    private fun creatureKillValue(state: GameState, entityId: EntityId): Double {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0.0
        return BoardPresence.permanentValue(state, state.projectedState, entityId, card, intents)
    }

    private companion object {
        /**
         * An option list longer than this is open-ended — every creature type, every card name —
         * and is shortlisted from the cards in the game rather than simulated entry by entry. Well
         * above any closed list the engine offers (five colours, five land types, a card's modes).
         */
        const val OPEN_LIST_THRESHOLD = 16

        /** Names or creature types simulated per side of an open-ended list. */
        const val OPEN_LIST_CANDIDATES_PER_SIDE = 3

        /** Relative score difference treated as a tie: identical boards, up to summation order. */
        const val TIE_EPSILON = 1e-9

        /** Library cards averaged to price one draw. */
        const val DRAW_VALUE_SAMPLES = 5

        val CREATURE_TYPES_LOWER: Set<String> = Subtype.ALL_CREATURE_TYPES.mapTo(HashSet()) { it.lowercase() }
    }
}
