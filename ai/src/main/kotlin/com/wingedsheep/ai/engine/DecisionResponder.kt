package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.advisor.AdvisorDecisionContext
import com.wingedsheep.ai.engine.advisor.CardAdvisorRegistry
import com.wingedsheep.ai.engine.budget.BudgetPolicy
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.budget.LegacyBudgetPolicy
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.evaluation.CardAdvantage
import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
import com.wingedsheep.ai.engine.knowledge.GraveyardSynergy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.engine.mechanics.ManaValues
import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.player.SkipDrawStepComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.SecretChoice
import kotlin.random.Random

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
    /**
     * [AiProfile.fillUpToMaxTargets]: grow an "up to N" target answer past its first pick while
     * each added target improves the simulated result, and always fill a requirement to its
     * minimum. Off answers every requirement with exactly one target, as it always has.
     */
    private val fillUpToMaxTargets: Boolean = false,
    /**
     * [AiProfile.selectionCountsByValue]: an unlabelled "choose up to N" takes N even when the
     * options don't outnumber it, and a scry-to-bottom or surveil-to-graveyard choice sends exactly
     * the cards worth less than [LOOK_KEEP_THRESHOLD] — possibly none, possibly all. Off is the
     * historical count logic.
     */
    private val selectionCountsByValue: Boolean = false,
    /**
     * [AiProfile.holdUnusablePumps]: decline an optional payment whose only payoff is an
     * end-of-turn grant on creatures that cannot attack or block this turn. Off is the historical
     * tie-to-yes.
     */
    private val holdUnusablePumps: Boolean = false,
    /**
     * [AiProfile.pumpsNeedAPlannedAttack]: under [holdUnusablePumps], a beginning-of-combat
     * payment on our own turn asks this advisor which creatures it would attack with on the yes
     * branch, and grants on the creatures left home count as unspendable. Null (the default)
     * keeps "can attack" as the only test.
     */
    private val combatAdvisor: CombatAdvisor? = null,
    /**
     * [AiProfile.surplusAwareLands]: an optional target is not picked when the pick returns one of
     * our own lands to a hand whose other lands already cover this turn's and next turn's land
     * drops ([strandsALand]). Off is the historical evaluator-only comparison.
     */
    private val surplusAwareLands: Boolean = false,
    /** Secret commitments are chosen independently of other players' unrevealed answers. */
    private val secretChoiceRandom: Random = Random.Default,
    /**
     * [AiProfile.targetPolarityFromEffect]: a target slot the ability's effect reads as
     * [TargetPolarity.BENEFICIAL] — a shield counter, a pump, an untap — draws from our own side
     * whenever our side has a legal target, and ties break toward our most valuable permanent
     * instead of toward the best *removal* target. Off is the historical removal-oriented ranking.
     */
    private val targetPolarityFromEffect: Boolean = false,
    /**
     * [AiProfile.selectionSeesWhoseCards]: a card choice off the battlefield is read by what the
     * choice does to whom ([selectionFate]) — which player's card moves, and whether it leaves a
     * hand or arrives in one — instead of by its prompt's wording. An opponent's discard or
     * exile takes the card most valuable to *them*; our own takes our least valuable; a choice
     * that hands us cards takes as many as it may. Off is the prompt-word routing.
     */
    private val selectionSeesWhoseCards: Boolean = false,
    /**
     * [AiProfile.topOrBottomWeighsTheDraw]: "put it on the top or bottom of its owner's library"
     * keeps the card on top only when it is worth at least an average draw from that library
     * ([CardSelectionValue], so a land-short hand digs for a land). Off is the simulation tie,
     * which always resolved to the first option, the top.
     */
    private val topOrBottomWeighsTheDraw: Boolean = false,
) {
    private val priors: ChoicePriors? = if (informedChoices) ChoicePriors(simulator.cardRegistry) else null

    var forcedPlayPicker: (GameState, EntityId) -> GameAction = simulator::completeForcedPlay

    fun respond(state: GameState, decision: PendingDecision, playerId: EntityId): DecisionResponse {
        // Secret commitments must bypass both simulation and advisors: either can inspect the
        // saved private answers and let the last chooser respond to information they cannot see.
        val secret = (state.peekContinuation() as? Suspension)?.takeIf {
            it.question.id == decision.id
        }?.answer as? SecretChoiceContinuation
        if (secret != null) {
            when (decision) {
                is ChooseNumberDecision -> {
                    val numeric = secret.effect.choice as? SecretChoice.Number
                    // Ranked bids can cost the chosen amount of life. Stay at the allowed floor
                    // rather than risking a lethal bid or consulting opponents' hidden numbers.
                    val ranked = numeric != null && (numeric.highestEffect != null ||
                        numeric.lowestEffect != null || numeric.tiedEffect != null)
                    return NumberChosenResponse(decision.id, if (ranked) decision.minValue else
                        secretChoiceRandom.nextLong(decision.minValue.toLong(), decision.maxValue.toLong() + 1).toInt())
                }
                is SelectCardsDecision -> return respondSecretEntity(state, decision, playerId)
                else -> {}
            }
        }
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
            // The AI never turns automatic ordering off; answering for a seat that did (a timeout)
            // keeps the order the abilities triggered in, which is what automatic ordering does.
            is OrderTriggersDecision -> TriggerOrderResponse(decision.id, decision.triggers.indices.toList())
            is SplitPilesDecision -> respondSplitPiles(state, decision, playerId)
            is ChooseOptionDecision -> respondOption(state, decision, playerId)
            is ChooseReplacementDecision -> respondReplacement(decision)
            is BudgetModalDecision -> respondBudgetModal(decision)
            is FlipOntoBattlefieldDecision -> FlipAim.respond(state, decision, playerId)
            is AssignDamageDecision -> respondDamageAssignment(state, decision)
            is CombatResolutionDecision -> respondCombatResolution(state, decision, playerId)
            is SearchLibraryDecision -> respondSearchLibrary(state, decision, playerId)
            is ReorderLibraryDecision -> respondReorderLibrary(state, decision, playerId)
            is SelectManaSourcesDecision -> respondManaSelection(decision)
        }
    }

    /** Ballot desirability uses only the visible board, never earlier private votes. */
    private fun respondSecretEntity(
        state: GameState,
        decision: SelectCardsDecision,
        playerId: EntityId,
    ): CardsSelectedResponse {
        val projected = state.projectedState
        fun publicValue(id: EntityId): Int =
            (projected.getPower(id) ?: 0).coerceAtLeast(0) + (projected.getToughness(id) ?: 0).coerceAtLeast(0)
        val candidates = decision.options.filter {
            projected.isCreature(it) &&
                projected.getController(it)?.let { controller -> state.isOpponentOf(controller, playerId) } == true &&
                !projected.hasKeyword(it, Keyword.INDESTRUCTIBLE)
        }
        val best = candidates.maxByOrNull(::publicValue)
            ?: if (decision.minSelections > 0) decision.options.minByOrNull(::publicValue) else null
        return CardsSelectedResponse(decision.id, listOfNotNull(best))
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
        val polarities = if (targetPolarityFromEffect) {
            TargetSelection.decisionPolarities(state, decision, intents)
        } else {
            emptyMap()
        }
        fun heuristicFor(req: TargetRequirementInfo): (EntityId) -> Double =
            if (polarities[req.index] == TargetPolarity.BENEFICIAL) {
                { id -> beneficialTargetHeuristic(state, id, playerId) }
            } else {
                { id -> targetHeuristic(state, id, playerId) }
            }
        if (decision.targetRequirements.size == 1) {
            val req = decision.targetRequirements.first()
            if (req.exclusiveGroups != null || req.distinctCategories != null) {
                val picks = constrainedTargetSelection(decision.legalTargetsFor(req.index, emptyMap()), req.minTargets, req.maxTargets,
                    req.selectionGroups, req.exclusiveGroups, heuristicFor(req), req.distinctCategories)
                return TargetsResponse(decision.id, mapOf(req.index to picks))
            }
            val equalityGroups = req.selectionGroups
            if (equalityGroups != null) {
                val answers = equalityGroups.flatMap { group ->
                    val ranked = group.sortedByDescending { targetHeuristic(state, it, playerId) }
                    val minimum = maxOf(1, req.minTargets)
                    val maximum = if (fillUpToMaxTargets) minOf(req.maxTargets, ranked.size) else minOf(minimum, ranked.size)
                    (minimum..maximum).map { ranked.take(it) }
                }.toMutableList()
                if (req.minTargets == 0) answers.add(emptyList())
                val best = answers.maxByOrNull { picks ->
                    evaluateResult(state, simulator.simulateDecision(state,
                        TargetsResponse(decision.id, mapOf(req.index to picks))), playerId)
                } ?: emptyList()
                return TargetsResponse(decision.id, mapOf(req.index to best))
            }
            val legal = decision.legalTargets[req.index]
                // A target whose own mana value is over a "total mana value N or less" cap can never
                // be picked; growTargets' simulation rejects the combinations that go over.
                ?.let { all -> req.totalManaValueAtMost?.let { cap -> all.filter { ManaValues.of(state, it) <= cap } } ?: all }
                ?: return cancelOrFirst(decision)
            if (legal.isEmpty()) return cancelOrFirst(decision)
            val targets = polarityPool(state, legal, playerId, polarities[req.index], optional = req.minTargets == 0)
            // An optional slot whose every legal target is on the wrong side stays empty.
            if (targets.isEmpty()) return TargetsResponse(decision.id, mapOf(req.index to emptyList()))
            val heuristic = heuristicFor(req)

            // For small target pools, simulate each; for large pools, use heuristics then simulate top candidates
            val candidates = if (targets.size <= maxCandidates) {
                targets
            } else {
                targets.sortedByDescending(heuristic).take(maxCandidates)
            }

            val optional = req.minTargets == 0
            val best = pickBestBySimulation(
                state, candidates, playerId,
                reject = { optional && strandsALand(state, it.state, playerId) },
                heuristic = heuristic,
                beneficial = polarities[req.index] == TargetPolarity.BENEFICIAL,
            ) { target ->
                TargetsResponse(decision.id, mapOf(req.index to listOf(target)))
            }
            val picks = if (fillUpToMaxTargets) {
                growTargets(state, playerId, req, best, candidates, targets) { picked ->
                    TargetsResponse(decision.id, mapOf(req.index to picked))
                }
            } else {
                listOf(best)
            }
            val bestResponse = TargetsResponse(decision.id, mapOf(req.index to picks))

            // For optional targets (minTargets == 0), also consider picking no targets
            if (optional) {
                val skipResponse = TargetsResponse(decision.id, mapOf(req.index to emptyList()))
                val bestResult = simulator.simulateDecision(state, bestResponse)
                if (strandsALand(state, bestResult.state, playerId)) return skipResponse
                val bestScore = evaluateResult(state, bestResult, playerId)
                val skipScore = evaluateResult(state,simulator.simulateDecision(state, skipResponse), playerId)
                // An even trade is still worth taking when it strips the opponent's graveyard (or
                // mills them) — the part of the result the evaluator can't see.
                val even = skipScore == bestScore ||
                    kotlin.math.abs(skipScore - bestScore) <= TIE_EPSILON * maxOf(1.0, kotlin.math.abs(bestScore))
                val evenButUseful = even && bestScore != Double.NEGATIVE_INFINITY &&
                    libraryAndGraveyardPreference(state, bestResult.state, playerId) > 0.0
                if (skipScore >= bestScore && !evenButUseful) return skipResponse
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
        // A slot that reads an earlier target draws from the earlier picks' table, and every probe
        // re-picks the later slots that probe would unseat, so no probe is illegal for that reason.
        var selected = minimalCompleteSelection(decision)
        fun probe(index: Int, picks: List<EntityId>) =
            TargetsResponse(decision.id, decision.repairSlotsAfter(index, selected + (index to picks)))
        for (req in decision.targetRequirements) {
            val targets = polarityPool(
                state, decision.legalTargetsFor(req.index, selected), playerId, polarities[req.index],
                optional = req.minTargets == 0,
            )
            val heuristic = heuristicFor(req)
            val groups = req.selectionGroups
            val answer = if (req.exclusiveGroups != null || req.distinctCategories != null) {
                constrainedTargetSelection(targets, req.minTargets, req.maxTargets, req.selectionGroups,
                    req.exclusiveGroups, heuristic, req.distinctCategories)
            } else if (groups != null) {
                val answers = groups.flatMap { group ->
                    val ranked = group.filter { it in targets }.sortedByDescending { targetHeuristic(state, it, playerId) }
                    val minimum = maxOf(1, req.minTargets)
                    val maximum = if (fillUpToMaxTargets) minOf(req.maxTargets, ranked.size) else minOf(minimum, ranked.size)
                    (minimum..maximum).map { ranked.take(it) }
                }.toMutableList()
                if (req.minTargets == 0) answers.add(emptyList())
                answers.maxByOrNull { picks -> evaluateResult(state,
                    simulator.simulateDecision(state, probe(req.index, picks)), playerId) } ?: emptyList()
            } else if (targets.isEmpty()) {
                emptyList()
            } else {
                val best = pickBestBySimulation(
                    state, targets.take(maxCandidates), playerId,
                    reject = { req.minTargets == 0 && strandsALand(state, it.state, playerId) },
                    heuristic = heuristic,
                    beneficial = polarities[req.index] == TargetPolarity.BENEFICIAL,
                ) { target ->
                    probe(req.index, listOf(target))
                }
                val picks = if (fillUpToMaxTargets) {
                    growTargets(state, playerId, req, best, targets.take(maxCandidates), targets) { picked ->
                        probe(req.index, picked)
                    }
                } else {
                    listOf(best)
                }
                // For optional targets, compare best pick against skipping
                if (req.minTargets == 0) {
                    val pickResponse = probe(req.index, picks)
                    val skipResponse = probe(req.index, emptyList())
                    val pickResult = simulator.simulateDecision(state, pickResponse)
                    val pickScore = evaluateResult(state, pickResult, playerId)
                    val skipScore = evaluateResult(state,simulator.simulateDecision(state, skipResponse), playerId)
                    if (skipScore >= pickScore || strandsALand(state, pickResult.state, playerId)) emptyList()
                    else picks
                } else {
                    picks
                }
            }
            selected = decision.repairSlotsAfter(req.index, selected + (req.index to answer))
        }
        return TargetsResponse(decision.id, selected)
    }

    /**
     * Grow a one-target answer for [req] toward [TargetRequirementInfo.maxTargets], under
     * [fillUpToMaxTargets]. Each round simulates every remaining candidate added to the picks so
     * far and keeps the best one if it beats the current answer — "up to two other target
     * creatures" takes the second counter when a second creature is worth countering, and stops
     * before one on the opponent's side. Below [TargetRequirementInfo.minTargets] a pick is taken
     * whatever it scores (only a legal one), drawing on [allTargets] once [candidates] run out: a
     * mandatory "two target creatures" answered with one is not a weaker answer but an illegal one.
     */
    private fun growTargets(
        state: GameState,
        playerId: EntityId,
        req: TargetRequirementInfo,
        first: EntityId,
        candidates: List<EntityId>,
        allTargets: List<EntityId>,
        respond: (List<EntityId>) -> DecisionResponse,
    ): List<EntityId> {
        val picks = mutableListOf(first)
        if (req.maxTargets <= 1 && req.minTargets <= 1) return picks
        var score = evaluateResult(state,simulator.simulateDecision(state, respond(picks)), playerId)
        while (picks.size < req.maxTargets) {
            val mandatory = picks.size < req.minTargets
            val pool = (if (mandatory) candidates + allTargets else candidates).distinct().filterNot(picks::contains)
            val scored = pool.map { it to evaluateResult(state,simulator.simulateDecision(state, respond(picks + it)), playerId) }
            val (next, nextScore) = scored.maxByOrNull { it.second } ?: break
            // Short of the minimum every probe is illegal, so an unscorable pick is still taken.
            val stillShort = picks.size + 1 < req.minTargets
            if (nextScore == Double.NEGATIVE_INFINITY && !stillShort) break
            if (!mandatory && nextScore <= score) break
            picks += next
            score = nextScore
        }
        return picks
    }

    /**
     * The legal targets a slot of [polarity] should draw from: for a [TargetPolarity.BENEFICIAL]
     * slot, our own side when it has any legal target, else anything but an opponent's, else
     * everything (a mandatory slot still needs an answer). Every other polarity keeps [legal].
     *
     * A filter rather than a tie-break because the simulation rarely separates the sides: a
     * triggered ability's target is scored with the ability still on the stack, so a shield counter
     * on our creature and on theirs evaluate the same, and the removal-oriented tie-break then
     * hands it to them.
     *
     * An [optional] slot ("up to one target …") has no "a mandatory slot still needs an answer" to
     * fall back on: it draws only from the right side — anything but an opponent's for a
     * BENEFICIAL slot, anything but ours for a HARMFUL one — and may come back empty, which leaves
     * the slot empty.
     */
    private fun polarityPool(
        state: GameState,
        legal: List<EntityId>,
        playerId: EntityId,
        polarity: TargetPolarity?,
        optional: Boolean,
    ): List<EntityId> {
        if (optional) {
            return when (polarity) {
                TargetPolarity.BENEFICIAL -> legal.filter { TargetSelection.isOurs(state, it, playerId) }
                    .ifEmpty { legal.filterNot { TargetSelection.isTheirs(state, it, playerId) } }
                TargetPolarity.HARMFUL -> legal.filterNot { TargetSelection.isOurs(state, it, playerId) }
                TargetPolarity.UNKNOWN, null -> legal
            }
        }
        if (polarity != TargetPolarity.BENEFICIAL) return legal
        return legal.filter { TargetSelection.isOurs(state, it, playerId) }
            .ifEmpty { legal.filterNot { TargetSelection.isTheirs(state, it, playerId) } }
            .ifEmpty { legal }
    }

    /**
     * [targetHeuristic]'s mirror for a [TargetPolarity.BENEFICIAL] slot: our side first, our most
     * valuable creature ahead of a lesser one, an opponent's permanent last.
     */
    private fun beneficialTargetHeuristic(state: GameState, targetId: EntityId, playerId: EntityId): Double {
        val side = when {
            TargetSelection.isOurs(state, targetId, playerId) -> 10.0
            TargetSelection.isTheirs(state, targetId, playerId) -> -10.0
            else -> 0.0
        }
        val projected = state.projectedState
        val value = if (projected.isCreature(targetId)) {
            (projected.getPower(targetId) ?: 0) + (projected.getToughness(targetId) ?: 0).toDouble()
        } else {
            0.0
        }
        return side + if (side < 0) -value else value
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
        if (decision.selectionLimits != null) {
            val allocation = CounterAllocation.fullPayment(decision)
            val decline = CardsSelectedResponse(decision.id, emptyList())
            if (allocation == null) return decline
            val payment = CardsSelectedResponse(decision.id, allocation)
            if (decision.minSelections > 0) return payment
            val payScore = evaluateChoice(state, simulator.simulateDecision(state, payment), playerId)
            val declineScore = evaluateChoice(state, simulator.simulateDecision(state, decline), playerId)
            return if (payScore >= declineScore) payment else decline
        }

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
        // A library search says so on the decision; its prompt and label name the filter and the
        // destination ("… to put into your graveyard"), so the word-matching below must not read it.
        val isSearch = decision.librarySearch
        val isDiscard = !isSearch && prompt.contains("discard")
        val isSacrifice = !isSearch && prompt.contains("sacrifice")
        val isScryBottom = !isSearch && decision.selectedLabel?.lowercase()?.contains("bottom") == true
        val isSurveilGraveyard = !isSearch && decision.selectedLabel?.lowercase()?.contains("graveyard") == true
        val isChooseToKeep = prompt.contains("put") && prompt.contains("hand")

        if (selectionCountsByValue && !isDiscard && (isScryBottom || isSurveilGraveyard) &&
            decision.conditionalMinimums.isEmpty()
        ) {
            return CardsSelectedResponse(decision.id, cardsBelowKeepThreshold(state, decision, playerId))
        }

        if (selectionSeesWhoseCards && !isSearch && castabilityAwareCardSelection) {
            selectionByFate(state, decision, playerId)?.let { return it }
        }

        return when {
            isDiscard || isScryBottom -> {
                // Pick cards we want LEAST (to discard / put on bottom)
                val ranked = rankCardsContextual(state, options, playerId, wantToKeep = false, drawnLater = isScryBottom)
                CardsSelectedResponse(decision.id, minimumLegalSelection(decision, ranked))
            }
            isSacrifice -> {
                // Sacrifice least valuable permanents
                val ranked = options.sortedBy { entityId ->
                    val card = state.getEntity(entityId)?.get<CardComponent>() ?: return@sortedBy 0.0
                    BoardPresence.permanentValue(state, state.projectedState, entityId, card, intents)
                }
                val pick = CardsSelectedResponse(decision.id, ranked.take(min.coerceAtLeast(1).coerceAtMost(max)))
                if (min > 0) return pick
                // An optional sacrifice ("you may sacrifice …. If you do, …") asked as one pick:
                // weigh paying with the cheapest permanent against declining, as the yes/no did.
                val decline = CardsSelectedResponse(decision.id, emptyList())
                val pickScore = evaluateChoice(state,simulator.simulateDecision(state, pick), playerId)
                val declineScore = evaluateChoice(state,simulator.simulateDecision(state, decline), playerId)
                if (pickScore >= declineScore) pick else decline
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
            selectionCountsByValue && (isSearch || decision.selectedLabel == null) && max > 0 -> {
                // A library search is a gain (a Landscape's [Plains]), as is any other unlabelled
                // "choose up to N". The branch above already takes N when the options outnumber
                // it; taking `min` (zero) here declined every search with ≤ N candidates.
                val ranked = rankCardsContextual(state, options, playerId, wantToKeep = true)
                CardsSelectedResponse(decision.id, ranked.take(max.coerceAtMost(options.size)))
            }
            else -> {
                val ranked = rankCardsContextual(state, options, playerId, wantToKeep = true)
                CardsSelectedResponse(decision.id, ranked.take(min.coerceAtLeast(0)))
            }
        }
    }

    /** Who a card selection moves a card for, and whether that player gains or loses it. */
    private data class SelectionFate(val player: EntityId, val gains: Boolean)

    /** Re-entrancy guard: the probe's own simulation answers nested selections the old way. */
    private var probingSelectionFate = false

    /**
     * What choosing a card in [decision] does, read off a simulation of choosing one: a card that
     * leaves a hand for a graveyard, exile or a library is its owner's loss (a discard, Null
     * Summoner's exile); a card that arrives in a hand, or on the battlefield, is a gain for its
     * new holder (Seismic Sense's "put it into your hand"). Null when the options are permanents,
     * span several owners, or the probe moves nothing before the next decision — the prompt-word
     * routing answers those.
     *
     * The prompt cannot carry this: Tinybones' "each opponent discards a card" asks "Choose 1
     * card", and the generic branch then *kept* the best card, i.e. discarded it.
     */
    private fun selectionFate(state: GameState, decision: SelectCardsDecision): SelectionFate? {
        if (probingSelectionFate || decision.maxSelections <= 0) return null
        val options = decision.options
        if (options.isEmpty()) return null
        val owners = options.map { id ->
            val entity = state.getEntity(id)
            entity?.get<OwnerComponent>()?.playerId ?: entity?.get<CardComponent>()?.ownerId
        }
        val owner = owners.first() ?: return null
        if (owners.any { it != owner }) return null
        val battlefield = state.getBattlefield().toSet()
        if (options.any { it in battlefield }) return null

        val probe = options.take(decision.minSelections.coerceAtLeast(1).coerceAtMost(decision.maxSelections))
        val result = try {
            probingSelectionFate = true
            simulator.simulateDecision(state, CardsSelectedResponse(decision.id, probe))
        } finally {
            probingSelectionFate = false
        }
        if (result is SimulationResult.Illegal) return null
        val move = result.events.filterIsInstance<ZoneChangeEvent>().firstOrNull { it.entityId == probe.first() }
            ?: return null
        return when {
            move.fromZone == Zone.HAND && move.toZone in LOSS_ZONES -> SelectionFate(move.ownerId, gains = false)
            move.toZone == Zone.HAND && move.fromZone != Zone.HAND -> SelectionFate(move.ownerId, gains = true)
            move.toZone == Zone.BATTLEFIELD -> SelectionFate(
                result.state.projectedState.getController(move.entityId) ?: move.ownerId, gains = true,
            )
            else -> null
        }
    }

    /**
     * [AiProfile.selectionSeesWhoseCards]: answer a card choice by [selectionFate] — the card
     * valued from the point of view of the player it moves for. Null leaves the choice to the
     * prompt-word routing.
     */
    private fun selectionByFate(state: GameState, decision: SelectCardsDecision, playerId: EntityId): CardsSelectedResponse? {
        val fate = selectionFate(state, decision) ?: return null
        val options = decision.options
        val ours = fate.player == playerId
        if (!ours && !state.isOpponentTo(fate.player, playerId)) return null
        val holder = CardSelectionValue.of(state, fate.player, intents, denial = !ours && !fate.gains)
        val scored = options.map { it to holder.score(it) }
        val best = scored.sortedByDescending { it.second }.map { it.first }
        val worst = scored.sortedBy { it.second }.map { it.first }
        val selected = when {
            // Our discard / exile: the cards we can best spare, as few as the decision allows.
            ours && !fate.gains -> minimumLegalSelection(decision, worst)
            // Cards coming to us: the best of them, as many as we may take.
            ours -> best.take(decision.maxSelections)
            // An opponent loses what we pick: what they would most want to keep, as many as allowed.
            !fate.gains -> best.take(decision.maxSelections)
            // An opponent gets what we pick: the least useful to them, as few as allowed.
            else -> worst.take(decision.minSelections.coerceAtMost(decision.maxSelections))
        }
        return CardsSelectedResponse(decision.id, selected)
    }

    /**
     * Scry / surveil: send away exactly the looked-at cards not worth drawing — every card whose
     * [CardSelectionValue] falls below [LOOK_KEEP_THRESHOLD], widened to the decision's minimum
     * with the least valuable cards and capped at its maximum. Zero is a legal and common answer.
     */
    private fun cardsBelowKeepThreshold(
        state: GameState,
        decision: SelectCardsDecision,
        playerId: EntityId,
    ): List<EntityId> {
        val value = CardSelectionValue.of(state, playerId, intents, drawnLater = true)
        val scored = decision.options.map { it to value.score(it) }.sortedBy { it.second }
        val count = scored.count { it.second < LOOK_KEEP_THRESHOLD }
            .coerceIn(decision.minSelections.coerceAtMost(decision.maxSelections), decision.maxSelections)
        return scored.take(count).map { it.first }
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
        val yesScore = evaluateChoice(state, yesResult, playerId)
        val noScore = evaluateChoice(state, noResult, playerId)
        if (holdUnusablePumps && yesScore >= noScore && payoffIsUnspendable(state, yesResult, noResult, yesScore, noScore, playerId)) {
            return YesNoResponse(decision.id, false)
        }
        return YesNoResponse(decision.id, yesScore >= noScore)
    }

    /**
     * Whether saying yes buys nothing but end-of-turn grants on creatures that cannot use them —
     * the yes/no half of [AiProfile.holdUnusablePumps].
     *
     * The leaf reads an until-end-of-turn pump as if it were permanent, so a "you may pay {E}{E}.
     * When you do, …" whose payoff is a pump always scores at least as well as declining — and the
     * energy it spends has no price at all. 2026-10-10 logs, game 3 turn 15: Voltstorm Angel paid
     * {E}{E} at the beginning of combat the turn it was cast, to give itself vigilance and lifelink
     * it could not attack with.
     *
     * Read off the simulation rather than the card: the grants are the floating effects the yes
     * branch created, so a modal payoff is judged by the mode the AI would actually pick. Each one
     * that lasts until end of turn and lands only on our creatures, none of which can still attack
     * or block this turn ([ExpiringGrantWindow.creatureCanStillFight]), is stripped, and the yes
     * branch is re-scored without them. Anything else it bought — a card, a counter, a token —
     * survives the strip, so yes still wins when it buys something real; a bare tie goes to no.
     *
     * The same tie with nothing stripped — a mode that pumped nobody — goes to no when yes spent
     * energy: the leaf has no price for energy, so a tie there is a resource given away for nothing.
     *
     * With a [combatAdvisor] ([AiProfile.pumpsNeedAPlannedAttack]), a creature that can attack but
     * that the yes branch's attack plan leaves home counts as unable to use its grant too
     * ([plannedAttackers]) — game 3 turn 19, a ready Angel that paid and then never attacked.
     */
    private fun payoffIsUnspendable(
        state: GameState,
        yesResult: SimulationResult,
        noResult: SimulationResult,
        yesScore: Double,
        noScore: Double,
        playerId: EntityId,
    ): Boolean {
        if (yesResult !is SimulationResult.Terminal && yesResult !is SimulationResult.NeedsDecision) return false
        val yesState = yesResult.state
        if (yesState.gameOver) return false
        val grants = newExpiringCreatureGrants(state, yesState, playerId)
        val idle: (EntityId) -> Boolean = { id -> !ExpiringGrantWindow.creatureCanStillFight(state, playerId, id) }
        var unspendable = grants.filter { effect -> effect.effect.affectedEntities.all(idle) }
        if (unspendable.size < grants.size) {
            // Some grant lands on a creature that *can* attack. Whether it will is the attack
            // plan's call — asked only when that plan is the very next thing this turn decides.
            val planned = plannedAttackers(state, yesState, playerId)
            if (planned != null) {
                unspendable = grants.filter { effect ->
                    effect.effect.affectedEntities.all { id -> idle(id) || id !in planned }
                }
            }
        }
        if (unspendable.isEmpty()) {
            return yesScore == noScore && energyOf(yesState, playerId) < energyOf(noResult.state, playerId)
        }
        val stripped = yesState.copy(floatingEffects = yesState.floatingEffects - unspendable.toSet())
        val strippedScore = evaluateChoice(state, SimulationResult.Terminal(stripped, yesResult.events), playerId)
        return strippedScore <= noScore
    }

    /**
     * The until-end-of-turn effects [result] created that land only on [playerId]'s creatures —
     * the grants whose whole value has to be spent in a fight this turn.
     */
    private fun newExpiringCreatureGrants(root: GameState, result: GameState, playerId: EntityId): List<ActiveFloatingEffect> {
        val before = root.floatingEffects.mapTo(HashSet()) { it.id }
        val projected = root.projectedState
        return result.floatingEffects.filter { effect ->
            effect.id !in before &&
                effect.duration == Duration.EndOfTurn &&
                effect.effect.dynamicGroupFilter == null &&
                effect.effect.affectedEntities.isNotEmpty() &&
                effect.effect.affectedEntities.all { id ->
                    projected.isCreature(id) && projected.getController(id) == playerId
                }
        }
    }

    /**
     * Whether [result] spent an until-end-of-turn grant where no fight this turn can use it: it
     * made at least one such grant on our creatures, and none of them lands on a creature that
     * can still attack or block ([ExpiringGrantWindow.creatureCanStillFight]). A permanent
     * payoff — a counter, a token — makes no floating effect, so it never reads as wasted.
     *
     * [pickBestBySimulation]'s tie-break ahead of the target heuristic: under
     * [com.wingedsheep.ai.engine.evaluation.CreatureValuation.expiringStatsNeedAFight] a pump on
     * a creature that is not yet attacking scores at its settled stats, so every pick ties and
     * the heuristic's bigger body won.
     * 2026-10-16 AI-vs-AI logs, game 2 turn 11: Ezekiel Sims's beginning-of-combat +2/+2 went to a
     * Spider-Man cast that main phase, and Ezekiel attacked unpumped.
     */
    private fun wastesExpiringGrant(root: GameState, result: GameState, playerId: EntityId): Boolean {
        val grants = newExpiringCreatureGrants(root, result, playerId)
        return grants.isNotEmpty() && grants.none { grant ->
            grant.effect.affectedEntities.any { ExpiringGrantWindow.creatureCanStillFight(root, playerId, it) }
        }
    }

    /**
     * The creatures [combatAdvisor] would attack with on the yes branch — the
     * [AiProfile.pumpsNeedAPlannedAttack] half of [payoffIsUnspendable]. Null whenever the plan
     * cannot be read cheaply and exactly: the flag is off, the decision is not a
     * beginning-of-combat one on our own turn, it is being answered inside another simulation
     * (where a full attack search per yes/no would compound), or advancing the yes state does not
     * land on our declare-attackers step. Null falls back to "can attack".
     *
     * The yes state is advanced by passing priority — exactly what the AI does at a quiet
     * beginning of combat — and the plan is the one `Strategist` would submit there, local search
     * included, so a creature the AI would keep home reads as kept home.
     */
    private fun plannedAttackers(state: GameState, yesState: GameState, playerId: EntityId): Set<EntityId>? {
        val advisor = combatAdvisor ?: return null
        if (simulator.resolvingNestedDecision) return null
        if (!state.isActiveTurnFor(playerId) || state.step != Step.BEGIN_COMBAT) return null
        if (yesState.step != Step.BEGIN_COMBAT && yesState.step != Step.DECLARE_ATTACKERS) return null
        return advisor.plannedAttackersFrom(yesState, playerId) { budgetPolicy.budgetForDecision(it, playerId) }
    }

    private fun energyOf(state: GameState, playerId: EntityId): Int =
        state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.ENERGY) ?: 0

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
        val yesScore = evaluateChoice(state, yesResult, playerId)
        val noScore = evaluateChoice(state, noResult, playerId)
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
            evaluateResult(state,
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
                evaluateChoice(state,simulator.simulateDecision(state, ColorChosenResponse(decision.id, color)), playerId)
            }
            return ColorChosenResponse(decision.id, best)
        }
        val best = decision.availableColors.maxByOrNull { color ->
            evaluateResult(state,
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
            evaluateResult(state,
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
        if (topOrBottomWeighsTheDraw && castabilityAwareCardSelection) {
            topOrBottomByDraw(state, decision)?.let { return it }
        }
        if (priors != null) return respondOptionInformed(state, decision, playerId, priors)

        val best = decision.options.indices.maxByOrNull { index ->
            evaluateResult(state,
                simulator.simulateDecision(state, OptionChosenResponse(decision.id, index)),
                playerId
            )
        }!!
        return OptionChosenResponse(decision.id, best)
    }

    /**
     * [AiProfile.topOrBottomWeighsTheDraw]: "its owner puts it on the top or bottom of their
     * library" (Plan for All Outcomes). The card on top *is* the
     * owner's next draw; on the bottom, the next draw is a random library card. So the card stays
     * on top only when its [CardSelectionValue] (as a card drawn later) is at least the mean value
     * of the library's cards — a land-short hand bottoms a spell to dig for the land it needs.
     * The simulation can't tell the two apart (the library's order is not on the board), and its
     * tie always fell to the first option, the top. Valued from the owner's side, who is the one
     * choosing; null when the decision is not that choice or the library is empty.
     */
    private fun topOrBottomByDraw(state: GameState, decision: ChooseOptionDecision): OptionChosenResponse? {
        val choice = (state.peekContinuation() as? Suspension)?.takeIf { it.question.id == decision.id }
            ?.answer as? PutOnTopOrBottomContinuation ?: return null
        val positions = choice.positions.takeIf { it.size == decision.options.size } ?: return null
        val top = positions.indexOf(com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition.Top)
        val bottom = positions.indexOf(com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition.Bottom)
        if (top < 0 || bottom < 0) return null
        val library = state.getZone(choice.ownerId, Zone.LIBRARY)
        if (library.isEmpty()) return null
        val value = CardSelectionValue.of(state, choice.ownerId, intents, drawnLater = true)
        val averageDraw = library.sumOf { value.score(it) } / library.size
        return OptionChosenResponse(decision.id, if (value.score(choice.cardId) >= averageDraw) top else bottom)
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
            evaluateChoice(state,simulator.simulateDecision(state, OptionChosenResponse(decision.id, index)), playerId)
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

    private fun respondCombatResolution(
        state: GameState,
        decision: CombatResolutionDecision,
        playerId: EntityId,
    ): DecisionResponse {
        // The engine's defaults are lethal-in-declaration-order, which can waste a kill; divide the
        // damage to kill the most valuable creatures instead (CR 510.1c has no assignment order).
        val projected = state.projectedState
        return CombatDamagePlanner.plan(
            decision,
            playerId,
            controllerOf = projected::getController,
            value = { id -> if (projected.hasKeyword(id, Keyword.INDESTRUCTIBLE)) 0.0 else creatureKillValue(state, id) },
        )
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
            rankCardsContextual(state, decision.cards, playerId, wantToKeep = true, drawnLater = true)
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
        // The cards stay in the library for a later draw (see [CardSelectionValue.of]).
        drawnLater: Boolean = false,
    ): List<EntityId> {
        if (castabilityAwareCardSelection && !legacyOnly) {
            val value = CardSelectionValue.of(state, playerId, intents, drawnLater)
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
    private fun evaluateResult(root: GameState, result: SimulationResult, playerId: EntityId): Double {
        if (result is SimulationResult.Illegal) return Double.NEGATIVE_INFINITY
        // Rooted: every caller compares answers to one decision taken in [root], and a term that
        // prices what a line cost (the land-sequencing refund) has to know where the line started.
        val rooted = evaluator.rootedAt(root, playerId)
        return result.scoreOrRankLast { rooted.evaluate(it, it.projectedState, playerId) }
    }

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
    private fun evaluateChoice(root: GameState, result: SimulationResult, playerId: EntityId): Double {
        val base = evaluateResult(root, result, playerId)
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
        // Rooted at the undrawn position, so the drawn card is priced as a card and never as
        // mana the drawer's tapped lands "would have" spent on it.
        val rooted = evaluator.rootedAt(state, viewer)
        val before = rooted.evaluate(state, state.projectedState, viewer)
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
            rooted.evaluate(drawn, drawn.projectedState, viewer) - before
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

    /**
     * The target whose simulated result scores best, with ties broken deliberately rather than by
     * list order.
     *
     * Ties are common for effects the board evaluator can't see — it has no library or graveyard
     * term, so "target player mills N" aimed at either player, or "exile target card from a
     * graveyard" aimed at either graveyard, scores identically. Breaking that tie toward the first
     * legal target is how a Screaming Swarm milled its own controller ten cards over four attacks
     * (the 2026-10-12 VOW-vs-LRW engine log). Among tied candidates the pick goes to the best
     * [libraryAndGraveyardPreference] of its result, then to [targetHeuristic], then to the earliest.
     */
    /**
     * Whether the line from [root] to [leaf] returned one of [playerId]'s own lands to their hand
     * while the other lands there already cover this turn's land drop and the next — so the land
     * can only come back by pushing every later land drop back a turn. [AiProfile.surplusAwareLands].
     *
     * A static evaluation sees that line lose one land for one turn; it actually loses a land drop
     * for the rest of the game. 2026-10-12 AI-vs-AI logs (`live`), game 2 turn 9: Mirkwood
     * Nurturer's "return up to one other target permanent you control … if you do, put a +1/+1
     * counter on this creature" sent the AI's own tapped Plains back with Hobbit Hole and Forest in
     * hand. The evaluator put that pick 2.9 above skipping — the counter alone outscores a fifth land
     * on its scale — so this is a rule, not a re-weighting. A bounce that is free (no other land in
     * hand, or one with this turn's drop still unused) is left to the evaluator.
     */
    private fun strandsALand(root: GameState, leaf: GameState, playerId: EntityId): Boolean {
        if (!surplusAwareLands) return false
        val leafHand = leaf.getZone(playerId, Zone.HAND).toSet()
        if (leafHand.isEmpty()) return false
        val projected = root.projectedState
        val returned = projected.getBattlefieldControlledBy(playerId)
            .any { it in leafHand && projected.hasType(it, "LAND") }
        return returned && CardAdvantage.surplusLands(leaf, playerId, lookahead = 1) > 0
    }

    private fun pickBestBySimulation(
        state: GameState,
        candidates: List<EntityId>,
        playerId: EntityId,
        /** A candidate whose result this accepts ranks last, whatever it scores. */
        reject: (SimulationResult) -> Boolean = { false },
        /** The last tie-break between equally scored candidates. */
        heuristic: (EntityId) -> Double = { targetHeuristic(state, it, playerId) },
        /**
         * The slot hands its target a gift ([TargetPolarity.BENEFICIAL]): only then does the
         * [wastesExpiringGrant] tie-break apply. A harmful or unknown end-of-turn effect (a -X/-X,
         * a "can't block") is one we would rather land where no fight needs that creature.
         */
        beneficial: Boolean = false,
        buildResponse: (EntityId) -> DecisionResponse
    ): EntityId {
        val scored = candidates.map { candidate ->
            val result = simulator.simulateDecision(state, buildResponse(candidate))
            val score = if (reject(result)) Double.NEGATIVE_INFINITY else evaluateResult(state, result, playerId)
            Triple(candidate, result, score)
        }
        val top = scored.maxOfOrNull { it.third } ?: return candidates.first()
        if (top == Double.NEGATIVE_INFINITY) return candidates.first()
        val tolerance = TIE_EPSILON * maxOf(1.0, kotlin.math.abs(top))
        val tied = scored.filter { (_, _, score) -> score == top || top - score <= tolerance }
        if (tied.size == 1) return tied.single().first
        return tied.maxWith(
            compareBy<Triple<EntityId, SimulationResult, Double>>(
                { libraryAndGraveyardPreference(state, it.second.state, playerId) },
                { !beneficial || !wastesExpiringGrant(state, it.second.state, playerId) },
                { heuristic(it.first) },
            )
        ).first
    }

    /**
     * What the library and graveyard changes between [root] and [result] are worth to [playerId] —
     * the term the board evaluator lacks, used only to break ties between otherwise equal answers.
     *
     * Per player, it reads cards **milled** (moved from library to graveyard) and cards **removed
     * from the graveyard** (exiled, shuffled away):
     *
     *  - Milling an opponent is mildly good, and strongly good once their library is near empty —
     *    unless their deck feeds on its graveyard ([GraveyardSynergy]) and decking is still far off,
     *    in which case it is a gift.
     *  - Milling yourself is good only when your deck feeds on its graveyard *and* your library stays
     *    above [SAFE_LIBRARY_SIZE]; otherwise it is lost cards. It outbids a routine opponent mill,
     *    never one that threatens to deck them.
     *  - Emptying an opponent's graveyard is good (more so against a graveyard deck); emptying your
     *    own is a small loss, a large one when your deck uses it.
     */
    private fun libraryAndGraveyardPreference(root: GameState, result: GameState, playerId: EntityId): Double {
        var value = 0.0
        for (player in root.turnOrder) {
            val libraryBefore = root.getZone(player, Zone.LIBRARY)
            val libraryAfter = result.getZone(player, Zone.LIBRARY)
            val graveyardBefore = root.getZone(player, Zone.GRAVEYARD)
            val graveyardAfter = result.getZone(player, Zone.GRAVEYARD).toSet()
            val milled = libraryBefore.count { it in graveyardAfter }
            val removed = graveyardBefore.count { it !in graveyardAfter }
            if (milled == 0 && removed == 0) continue
            val synergy = GraveyardSynergy.benefitsFromGraveyard(root, player, simulator.cardRegistry)
            val nearDecked = libraryAfter.size <= NEAR_DECKED_LIBRARY_SIZE
            if (player == playerId) {
                val millValue = if (synergy && libraryAfter.size >= SAFE_LIBRARY_SIZE) SELF_MILL_PAYOFF else -SELF_MILL_COST
                value += milled * millValue - removed * (if (synergy) GRAVEYARD_LOSS_SYNERGY else GRAVEYARD_LOSS_PLAIN)
            } else if (root.isOpponentTo(player, playerId)) {
                val millValue = when {
                    nearDecked -> OPPONENT_MILL_NEAR_DECKED
                    synergy -> -SELF_MILL_PAYOFF
                    else -> OPPONENT_MILL
                }
                value += milled * millValue + removed * (if (synergy) GRAVEYARD_LOSS_SYNERGY else GRAVEYARD_LOSS_PLAIN)
            }
        }
        return value
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
        decision.minimalSelection()

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

        /** A library this size or smaller after a mill is close enough to decking to aim for. */
        const val NEAR_DECKED_LIBRARY_SIZE = 8

        /** The smallest library a graveyard deck will mill itself down to. */
        const val SAFE_LIBRARY_SIZE = 15

        /** Per card: milling a graveyard deck's own library (outbids [OPPONENT_MILL]). */
        const val SELF_MILL_PAYOFF = 0.5

        /** Per card: milling your own library with no use for the graveyard. */
        const val SELF_MILL_COST = 0.5

        /** Per card: milling an opponent whose library is not yet in danger. */
        const val OPPONENT_MILL = 0.25

        /** Per card: milling an opponent close to decking. */
        const val OPPONENT_MILL_NEAR_DECKED = 2.0

        /** Per card leaving a graveyard: the owner uses its graveyard / does not. */
        const val GRAVEYARD_LOSS_SYNERGY = 1.0
        const val GRAVEYARD_LOSS_PLAIN = 0.25

        /** Relative score difference treated as a tie: identical boards, up to summation order. */
        const val TIE_EPSILON = 1e-9

        /** Library cards averaged to price one draw. */
        const val DRAW_VALUE_SAMPLES = 5

        /**
         * [CardSelectionValue] below which a scried / surveiled card is not worth drawing. A spell
         * castable within a turn or two scores well above it; a land with six or more already
         * coming (1.5 or less) and a spell several land draws away fall below it.
         */
        const val LOOK_KEEP_THRESHOLD = 2.0

        /** Where a card that leaves a hand is lost to its owner ([selectionFate]). */
        private val LOSS_ZONES = setOf(Zone.GRAVEYARD, Zone.EXILE, Zone.LIBRARY)

        val CREATURE_TYPES_LOWER: Set<String> = Subtype.ALL_CREATURE_TYPES.mapTo(HashSet()) { it.lowercase() }
    }
}
