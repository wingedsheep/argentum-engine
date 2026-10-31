package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.advisor.CardAdvisorRegistry
import com.wingedsheep.ai.engine.advisor.CastContext
import com.wingedsheep.ai.engine.budget.BudgetPolicy
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.budget.LegacyBudgetPolicy
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.evaluation.EvaluationWeights
import com.wingedsheep.ai.engine.evaluation.LibraryClock
import com.wingedsheep.ai.engine.evaluation.ThreatAssessment
import com.wingedsheep.ai.engine.knowledge.EffectWalker
import com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TimingVerdict
import com.wingedsheep.ai.engine.rollout.CandidateEvaluator
import com.wingedsheep.ai.engine.rollout.PlayoutPolicy
import com.wingedsheep.ai.engine.rollout.RolloutCandidateEvaluator
import com.wingedsheep.ai.engine.rollout.StaticCandidateEvaluator
import com.wingedsheep.ai.insight.AiActionOption
import com.wingedsheep.ai.insight.AiDecisionInsight
import com.wingedsheep.ai.insight.AiDecisionKind
import com.wingedsheep.ai.insight.AiInsightLabels
import com.wingedsheep.ai.insight.AiInsightSink
import com.wingedsheep.ai.insight.CombatPlan
import com.wingedsheep.ai.insight.CombatPlanTrace
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.engine.handlers.costs.GraveyardTotalExileResolver
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.MeaningfulActionFilter
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.LandDropsComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ScryEffect
import com.wingedsheep.sdk.scripting.effects.SurveilEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Chooses which [LegalAction] to take when the AI has priority.
 *
 * **One simulation per candidate, then a leaf score.** Candidates come from the enumerator, each is
 * simulated once to the quiet state it produces, and the best-scoring one wins if it beats passing.
 *
 * What "leaf score" means is [candidateEvaluator]'s business, and that is the whole Phase 7 seam:
 * [StaticCandidateEvaluator] is one `BoardEvaluator.evaluate` call — the greedy 1-ply AI, what
 * `AiProfile.LEGACY_V0` runs — and [RolloutCandidateEvaluator] replaces it with the mean of several
 * short playouts. Everything else in this file is identical either way, including the target
 * refinement, the hold policy and the [CardAdvisorRegistry] override path, which is the point: a
 * per-card advisor keeps working, over a much better base.
 *
 * There used to be a second, multi-ply alpha-beta pass here (`Searcher`). It was
 * unreachable — its `recommendDepth` gated on "can the opponent respond?", which opened
 * with `state.priorityPlayerId != playerId` and so was always false on our own priority —
 * and it carried a `Double.MIN_VALUE / 2` "−∞" sentinel that is actually `0.0`. It was
 * deleted rather than repaired, and Phase 7 replaced the mechanism outright rather than reviving
 * it. Its one good idea, extending the search on a close call, survives as
 * `RolloutSettings.criticalHorizonBonus`.
 *
 * Combat decisions are delegated to [CombatAdvisor].
 */
class Strategist(
    private val simulator: GameSimulator,
    private val evaluator: BoardEvaluator,
    private val combatAdvisor: CombatAdvisor = CombatAdvisor(simulator, evaluator),
    private val advisorRegistry: CardAdvisorRegistry = CardAdvisorRegistry(),
    /**
     * Phase 4a: only propose actions the AI can actually take.
     *
     * Two halves. Candidates come from [MeaningfulActionFilter] instead of the ad-hoc
     * `affordable && !isManaAbility` filter, so a spell whose mandatory target slot is empty stops
     * being a candidate; and [TargetSelection.fillableRequirements] fills the slots it *can*
     * rather than abandoning the whole spell. Together they close the "889 of 945 rejected actions were
     * `CastSpell: No valid targets available`" finding Phase 1 quantified and left open.
     */
    private val useMeaningfulFilter: Boolean = false,
    /** Phase 4b. How much search each decision may spend. */
    private val budgetPolicy: BudgetPolicy = LegacyBudgetPolicy,
    /**
     * Phase 6: structural card knowledge. [IntentCatalog.NONE] is the off position and leaves
     * both consumers here — [TargetSelection.rank] and the hold policy — at their pre-Phase-6
     * behaviour.
     */
    private val intents: IntentCatalog = IntentCatalog.NONE,
    /** [AiProfile.combatTricksWaitForBlocks] — passed straight through to [HoldPolicy]. */
    private val combatTricksWaitForBlocks: Boolean = false,
    /**
     * [AiProfile.holdRemovalForBetterTargets] — passed straight through to [HoldPolicy], which
     * hands it to [com.wingedsheep.ai.engine.knowledge.RemovalPatience].
     */
    private val holdRemovalForBetterTargets: Boolean = false,
    /**
     * [AiProfile.holdCountersForBetterSpells] — passed straight through to [HoldPolicy], which
     * hands it to [com.wingedsheep.ai.engine.knowledge.CounterPatience].
     */
    private val holdCountersForBetterSpells: Boolean = false,
    /** [AiProfile.cashCantripsInTheEndStep] — passed straight through to [HoldPolicy]. */
    private val cashCantripsInTheEndStep: Boolean = false,
    /**
     * [AiProfile.holdFlashPermanentsForAmbush] — passed straight through to [HoldPolicy], which
     * hands it to [com.wingedsheep.ai.engine.knowledge.AmbushWindow].
     */
    private val holdFlashPermanentsForAmbush: Boolean = false,
    /**
     * [AiProfile.holdExpiringGrantsForCombat] — passed straight through to [HoldPolicy], which
     * hands it to [com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow].
     */
    private val holdExpiringGrantsForCombat: Boolean = false,
    /**
     * [AiProfile.refuseUnspendableGrants] — passed straight through to [HoldPolicy], which hands
     * the activation half to [com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow].
     */
    private val refuseUnspendableGrants: Boolean = false,
    /** [AiProfile.holdUnusablePumps] — passed straight through to [HoldPolicy]. */
    private val holdUnusablePumps: Boolean = false,
    /**
     * [AiProfile.refuseDeadSearches] — passed straight through to [HoldPolicy], which hands it to
     * [com.wingedsheep.ai.engine.knowledge.DeadSearch].
     */
    private val refuseDeadSearches: Boolean = false,
    /**
     * [AiProfile.refuseEmptyPayoffs] — passed straight through to [HoldPolicy], which hands it to
     * [com.wingedsheep.ai.engine.knowledge.EmptyPayoff].
     */
    private val refuseEmptyPayoffs: Boolean = false,
    /**
     * [AiProfile.drawWithIdleMana] — passed through to [HoldPolicy]; its last-main-phase bonus is
     * counted only when no other candidate beats passing (see [TimingVerdict.Adjust.onlyWhenIdle]).
     */
    private val drawWithIdleMana: Boolean = false,
    /**
     * [AiProfile.targetPolarityFromEffect]: rank targets by what the effect does to them, drop a
     * candidate whose every legal target is on the wrong side, and keep "cast without the kicker"
     * as a candidate beside the kicked cast.
     */
    private val targetPolarityFromEffect: Boolean = false,
    /**
     * [AiProfile.permanentCastIsNotCardLoss] — refund the hand-curve charge on a sorcery-speed
     * permanent cast. See [deploymentCredit].
     */
    private val permanentCastIsNotCardLoss: Boolean = false,
    /**
     * [AiProfile.fuelLoyaltyIsNotBoardValue] — refund the board price of loyalty a minus-only
     * planeswalker spends on a loyalty ability. See [fuelLoyaltyCredit].
     */
    private val fuelLoyaltyIsNotBoardValue: Boolean = false,
    /**
     * [AiProfile.defendPlaneswalkers] — here, only that [fuelLoyaltyCredit] refunds nothing to a
     * walker the activation leaves dead to the next attack.
     */
    private val defendPlaneswalkers: Boolean = false,
    /**
     * [AiProfile.spendFuelWalkers] — a minus-only walker's loyalty activation is priced as what it
     * buys, granted loyalty abilities included. See [fuelWalkerSpend].
     */
    private val spendFuelWalkers: Boolean = false,
    /**
     * [AiProfile.landDropUnlocksSpells] — a land drop scores as the best spell it makes castable.
     * See [landDropFollowUps].
     */
    private val landDropUnlocksSpells: Boolean = false,
    /**
     * [AiProfile.libraryClock] — the Momir avatar does not wait for a land drop that would drain
     * our library. The evaluator half lives in `LibraryClock`. See [momirActivations].
     */
    private val libraryClock: Boolean = false,
    /**
     * [AiProfile.activatedPumpsNeedAPlannedAttack] — before our attack, an activation that buys
     * nothing but end-of-turn creature grants is floored below passing unless the attack plan sends
     * a creature it lands on. See [pumpLeftHomeByThePlan].
     */
    private val activatedPumpsNeedAPlannedAttack: Boolean = false,
    /**
     * [AiProfile.enablersFirst] — a spell scores as the best spell it makes cheaper. See
     * [enablerFollowUps].
     */
    private val enablersFirst: Boolean = false,
    /**
     * [AiProfile.fetchedLandDropFollowUp] — a line that puts a land in hand with the land drop
     * unused scores as at least playing that land. See [fetchedLandFollowUps].
     */
    private val fetchedLandDropFollowUp: Boolean = false,
    /**
     * [AiProfile.recastBouncedPermanents] — a line that returns one of our permanents to our hand
     * scores as at least recasting it, and a self-bounce aims where that recast is worth most. See
     * [bouncedPermanentFollowUps] and [bestReplayOfBounced].
     */
    private val recastBouncedPermanents: Boolean = false,
    /**
     * [AiProfile.precombatPumpsForThePlan] — in our precombat main, a pump that changes who the
     * attack plan sends is priced by the attack it buys. See [pumpThatBuysTheAttack].
     */
    private val precombatPumpsForThePlan: Boolean = false,
    /**
     * [AiProfile.holdInstantSpeedDeploys] — passed straight through to [HoldPolicy], which hands it
     * to [com.wingedsheep.ai.engine.knowledge.AmbushWindow] along with each candidate's leaf.
     */
    private val holdInstantSpeedDeploys: Boolean = false,
    /**
     * [AiProfile.taxCountersReadTheirMana] — passed straight through to [HoldPolicy], which hands
     * it to [com.wingedsheep.ai.engine.knowledge.CounterTax].
     */
    private val taxCountersReadTheirMana: Boolean = false,
    /**
     * [AiProfile.refuseWastedPayoffs] — passed straight through to [HoldPolicy], which hands it to
     * [com.wingedsheep.ai.engine.knowledge.TargetlessEntry] and
     * [com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow].
     */
    private val refuseWastedPayoffs: Boolean = false,
    /**
     * [AiProfile.lifelinkWaitsForBlocks] — passed straight through to [HoldPolicy], which hands it
     * to [com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow].
     */
    private val lifelinkWaitsForBlocks: Boolean = false,
    /**
     * [AiProfile.tapToGrowAtTheirEndStep] — passed straight through to [HoldPolicy], which hands it
     * to [com.wingedsheep.ai.engine.knowledge.SelfTapGrowthWindow].
     */
    private val tapToGrowAtTheirEndStep: Boolean = false,
    /**
     * [AiProfile.borrowedCreaturesGoHome] — passed straight through to [HoldPolicy], which hands
     * it to [com.wingedsheep.ai.engine.knowledge.ExpiringGrantWindow].
     */
    private val borrowedCreaturesGoHome: Boolean = false,
    /**
     * [AiProfile.convokeLandsFirst] — a convoke spell taps creatures only for what mana cannot
     * pay, cheapest creatures first. See [withAutomaticConvoke].
     */
    private val convokeLandsFirst: Boolean = false,
    /**
     * The profile's `EvaluationWeights.boardPresence`. Only [HoldPolicy] reads it, to quote a
     * patience discount in the same units the leaf score prices board value in.
     */
    private val boardPresenceWeight: Double = EvaluationWeights.DEFAULT.boardPresence,
    /**
     * Phase 7: how a candidate's post-action state is scored. Defaults to the pre-Phase-7 leaf, so
     * a caller that doesn't opt in gets the greedy 1-ply AI unchanged.
     */
    private val candidateEvaluator: CandidateEvaluator = StaticCandidateEvaluator(evaluator),
    /** Phase 8: a fair complete world, sampled once before any candidate simulation. */
    private val stateSampler: ((GameState, EntityId) -> GameState)? = null,
    /**
     * Local testing mode: where the per-candidate scores this class computes go, instead of being
     * dropped once the winner is picked. Null in production, and the only thing that reads it is a
     * null check — the numbers are already being computed either way, so recording adds no search.
     */
    private val insightSink: AiInsightSink? = null,
) {
    private val holdPolicy = HoldPolicy(
        intents,
        tricksWaitForBlocks = combatTricksWaitForBlocks,
        holdRemovalForBetterTargets = holdRemovalForBetterTargets,
        holdCountersForBetterSpells = holdCountersForBetterSpells,
        cashCantripsInTheEndStep = cashCantripsInTheEndStep,
        holdFlashPermanentsForAmbush = holdFlashPermanentsForAmbush,
        holdExpiringGrantsForCombat = holdExpiringGrantsForCombat,
        refuseUnspendableGrants = refuseUnspendableGrants,
        refuseDeadSearches = refuseDeadSearches,
        refuseEmptyPayoffs = refuseEmptyPayoffs,
        holdUnusablePumps = holdUnusablePumps,
        drawWithIdleMana = drawWithIdleMana,
        holdInstantSpeedDeploys = holdInstantSpeedDeploys,
        taxCountersReadTheirMana = taxCountersReadTheirMana,
        refuseWastedPayoffs = refuseWastedPayoffs,
        lifelinkWaitsForBlocks = lifelinkWaitsForBlocks,
        tapToGrowAtTheirEndStep = tapToGrowAtTheirEndStep,
        borrowedCreaturesGoHome = borrowedCreaturesGoHome,
        boardPresenceWeight = boardPresenceWeight,
        legalActions = simulator::getLegalActions,
    )

    /**
     * Positions this player has already taken a non-pass action from, oldest first.
     *
     * The AI's only memory across decisions, and it exists for one reason: a leaf score cannot see
     * that it is being handed the same position over and over. See [StateProgress] and [remember].
     */
    private val positionsActedFrom = ArrayDeque<Long>()

    fun chooseAction(
        state: GameState,
        legalActions: List<LegalAction>,
        playerId: EntityId
    ): LegalAction {
        val startNanos = if (insightSink != null) System.nanoTime() else 0L
        val forcedPlay = state.pendingDecision is com.wingedsheep.engine.core.PlayCardDecision
        val evaluationState = stateSampler?.invoke(state, playerId) ?: state
        // Combat declaration steps need the CombatAdvisor to fill in attacker/blocker maps
        // even when there's only one legal action (which is the common case — the enumerator
        // returns a single DeclareAttackers/DeclareBlockers with an empty default map).
        val combatAction = legalActions.find { it.actionType == "DeclareAttackers" || it.actionType == "DeclareBlockers" }
        if (combatAction != null) {
            val budget = budgetPolicy.budgetFor(state, playerId, listOf(combatAction))
            return handleCombatDeclaration(evaluationState, combatAction, playerId, budget, startNanos)
        }

        if (legalActions.size == 1) {
            // Nothing to compare against, so no candidate expansion — but X still has to be chosen,
            // or this shortcut would submit the one available action at the enumerator's X=0.
            // Which shapes get an X bound is [expandXCostAbilities]' decision, not a second one:
            // a targeted activated ability is left alone here for the same reason it is there, so
            // that the engine's own choose-X pause (which DecisionResponder answers by simulation)
            // keeps handling it.
            val single = legalActions.first()
            val only = if (bindsXWithoutTheEnginesHelp(single)) {
                XCostSelection.bindBestX(state, single)
            } else {
                single
            }
            return only.copy(action = chooseCommittedTargets(state, only, playerId))
        }

        val pass = legalActions.find { it.actionType == "PassPriority" }
        val candidates = if (forcedPlay) {
            legalActions.filter { it.affordable && !it.hasUnfillableTargetRequirement }
        } else candidatesFrom(legalActions)
        // A forced play has to play *something*, so nothing is vetoed there.
        val aimed = if (targetPolarityFromEffect && !forcedPlay) {
            candidates.filterNot { TargetSelection.aimsOnlyAtTheWrongSide(state, it, playerId, intents) }
        } else {
            candidates
        }
        val affordable = expandXCostAbilities(state, preferKickerVariants(aimed), playerId)

        if (affordable.isEmpty()) return pass ?: legalActions.first()

        val budget = budgetPolicy.budgetFor(state, playerId, affordable)
        val here = StateProgress.digest(evaluationState)

        // ── Pass 1: one simulation per candidate, to the quiet state it leads to ──
        // The anytime contract: candidates are simulated in order and the budget only cuts the
        // tail short. `maxByOrNull` over a partial list is still a valid (if worse) answer.
        //
        // The pass is simulated first and scored alongside the rest, so an evaluator that
        // allocates effort across candidates (Phase 7's sequential halving) treats "do nothing" as
        // the real option it is rather than as a separately-computed threshold.
        val leaves = mutableListOf<LegalAction>()
        val leafStates = mutableListOf<GameState>()
        // Local testing mode only: the candidates that never reached scoring. Reading the panel
        // without them makes the AI look like it never considered a play it in fact discarded.
        val dropped = if (insightSink != null) mutableListOf<AiActionOption>() else null
        var searched = 0
        // Leaves whose line starts with a sacrifice-mana activation rather than the action itself —
        // see [sacrificeManaFirst]. Keyed by identity: the leaf is scored as the cast it leads to.
        val manaFirst = java.util.IdentityHashMap<LegalAction, LegalAction>()
        if (pass != null) {
            val passSimulation = simulator.simulate(evaluationState, pass.action)
            leaves += pass
            // The pass leaf is positional — `leafScores.first()` below is this entry — so an
            // unfinished simulation cannot drop it. Fall back to the position we are standing in,
            // which is the same "do nothing" reference the no-pass branch uses.
            leafStates += if (passSimulation is SimulationResult.StoppedAtLimit) {
                evaluationState
            } else {
                passSimulation.state
            }
        }
        for (action in affordable) {
            searched++
            val (materialized, materializedSimulation) = materialize(evaluationState, action, playerId, budget, here)
            // Only on failure, so a cast auto-pay can cover costs nothing extra.
            val rescue = if (materializedSimulation is SimulationResult.Illegal && !forcedPlay) {
                sacrificeManaFirst(evaluationState, legalActions, action, materialized, materializedSimulation)
            } else null
            val simulation = rescue?.second ?: materializedSimulation
            val usable = when {
                // LegalAction affordability is necessarily a preview for costs such as convoke and
                // modal/additional payments. If materializing the concrete action cannot pass the
                // authoritative processor, it is not a candidate the AI may submit.
                simulation is SimulationResult.Illegal -> false
                // Automatic resolution ran out of transitions, so the retained board is mid-flight.
                // Scoring it would rank a candidate on a position it never actually reaches.
                simulation is SimulationResult.StoppedAtLimit -> false
                simulation is SimulationResult.NeedsDecision -> true
                // A line that walks back into a position we have already acted from has accomplished
                // nothing, whatever the leaf score says — and it is not a one-off mistake, because it
                // hands us back the very position that made it look good. Aphetto Alchemist untapping
                // itself is the degenerate case (`here`); two of them untapping each other is the same
                // thing one step longer. See [StateProgress].
                else -> StateProgress.digest(simulation.state)
                    .let { leaf -> leaf != here && leaf !in positionsActedFrom }
            }
            if (usable) {
                val leaf = action.copy(action = materialized)
                if (rescue != null) manaFirst[leaf] = rescue.first
                leaves += leaf
                leafStates += simulation.state
            } else {
                val illegal = simulation is SimulationResult.Illegal
                dropped?.add(
                    droppedOption(
                        evaluationState, action, materialized,
                        note = when {
                            illegal -> "dropped — illegal once materialized"
                            simulation is SimulationResult.StoppedAtLimit ->
                                "dropped — simulation ran out of automatic transitions"
                            else -> "dropped — leads back to a position already acted from"
                        },
                        submittable = !illegal,
                    )
                )
            }
            // Every candidate cost a simulation whether or not it survived those filters, so the
            // anytime cut is taken on all of them. Checking only after a survivor was recorded
            // would let a board full of inert candidates run straight past the budget.
            if (budget.expired()) break
        }
        if (dropped != null) {
            for (action in affordable.drop(searched)) {
                dropped += droppedOption(
                    evaluationState, action, action.action,
                    note = "not searched — decision budget expired",
                    // Never materialized, so its targets are unfilled — not something to hand the
                    // processor. Listed so the panel can say the budget, not the AI, ruled it out.
                    submittable = false,
                )
            }
        }

        // ── Pass 1b: what each land drop makes castable ──
        // Scored in the same batch as the leaves, from the same root, so the follow-ups compete
        // with the other candidates on one scale. See [landDropFollowUps].
        val followUps = buildList {
            if (landDropUnlocksSpells && !forcedPlay) {
                addAll(landDropFollowUps(evaluationState, legalActions, leaves, leafStates, playerId, budget))
            }
            // ── Pass 1c: what each spell makes cheaper — see [enablerFollowUps] ──
            if (enablersFirst && !forcedPlay) {
                addAll(enablerFollowUps(evaluationState, legalActions, leaves, leafStates, playerId, budget))
            }
            // ── Pass 1d: the land drop a line puts in hand — see [fetchedLandFollowUps] ──
            if (fetchedLandDropFollowUp && !forcedPlay) {
                addAll(fetchedLandFollowUps(evaluationState, leaves, leafStates, playerId, budget))
            }
            // ── Pass 1e: the permanent a line returns to our hand, cast again — see [bouncedPermanentFollowUps] ──
            if (recastBouncedPermanents && !forcedPlay) {
                addAll(bouncedPermanentFollowUps(evaluationState, leaves, leafStates, playerId, budget))
            }
        }

        // ── Pass 2: score every leaf at once ──
        val allScores = candidateEvaluator.scoreAll(
            evaluationState, leafStates + followUps.map { it.leaf }, playerId, budget,
        )
        val leafScores = allScores.subList(0, leafStates.size)
        val followUpScores = allScores.subList(leafStates.size, allScores.size)
        val passScore = if (pass != null) {
            leafScores.first()
        } else {
            // No pass on offer: the "do nothing" reference is the current position itself.
            candidateEvaluator.score(evaluationState, evaluationState, playerId, budget)
        }

        // ── Pass 3: per-card timing and advisor adjustments, in raw evaluator units ──
        val firstCandidate = if (pass != null) 1 else 0
        val owns = (0 until leaves.size).map { i ->
            if (i < firstCandidate) null
            else if (forcedPlay) AdjustedScore(leafScores[i])
            else adjustScore(evaluationState, leaves[i], playerId, leafScores[i], passScore, leafStates[i])
        }
        val rooted by lazy(LazyThreadSafetyMode.NONE) { evaluator.rootedAt(evaluationState, playerId) }
        val adjusted = (firstCandidate until leaves.size).map { i ->
            val own = owns[i]!!
            // A land drop is worth at least the best spell it unlocks, judged as that spell's own
            // decision would judge it — from the position right after the land. So is a spell that
            // makes another cheaper ([enablerFollowUps]), unless its own window floored it: a card
            // that does nothing here is not made a play by what it discounts.
            val unlocked = followUps.indices.filter { followUps[it].landIndex == i && !own.floored }.map { f ->
                val followUp = followUps[f]
                val sibling = followUp.sibling
                followUp to if (sibling != null) {
                    // The discounted spell is also a candidate on its own. Price the pair as that
                    // candidate's score plus what the pair's board adds over it, both read by the
                    // static evaluator from this root: the two leaves differ only by the enabler
                    // and the mana saved, and a playout estimate of each — sequential halving
                    // gives the follow-up a fraction of the sibling's playouts — is noise on a
                    // difference that size.
                    val siblingScore = owns[sibling]!!
                    val pairGain = rooted.evaluate(followUp.leaf, followUp.leaf.projectedState, playerId) -
                        rooted.evaluate(leafStates[sibling], leafStates[sibling].projectedState, playerId)
                    siblingScore.copy(score = siblingScore.score + pairGain)
                } else {
                    adjustScore(leafStates[i], followUp.action, playerId, followUpScores[f], passScore, followUp.leaf)
                }
            }.maxByOrNull { it.second.score }
            val adjustment = if (unlocked != null && unlocked.second.score > own.score) {
                val name = resolveCardName(leafStates[i], unlocked.first.action) ?: "a spell"
                val why = when {
                    unlocked.first.note != null -> unlocked.first.note
                    leaves[i].action is PlayLand -> "land first: unlocks"
                    unlocked.first.action.action is PlayLand -> "then plays"
                    else -> "first: discounts"
                }
                AdjustedScore(
                    unlocked.second.score,
                    listOfNotNull(own.note, "$why $name %.2f".format(unlocked.second.score))
                        .joinToString("; "),
                    unlocked.second.idleBonus,
                )
            } else own
            Triple(leaves[i], leafScores[i], adjustment)
        }

        // On the opponent's end step, unspent mana is about to be wasted. Reduce the pass threshold
        // so the AI is more willing to use instants rather than letting mana evaporate.
        //
        // Phase 6 retires this blanket discount for agents with card knowledge: [HoldPolicy] makes
        // the same point per card, and better — a removal spell gets a *larger* end-step bonus,
        // while a pump that is about to wear off in cleanup gets a penalty instead of an
        // encouragement. Keeping both would double-count the first and cancel the second.
        val adjustedPassScore =
            if (!holdPolicy.isEnabled && !state.isActiveTurnFor(playerId) && state.step == Step.END) {
                passScore - 1.5
            } else {
                passScore
            }

        // A bonus for spending mana nothing else will is void the moment something else will: if
        // any candidate beats passing without one, every such bonus comes off and the candidates
        // compete on their merits. See [TimingVerdict.Adjust.onlyWhenIdle].
        val manaHasAUse = adjusted.any { (_, _, adjustment) ->
            adjustment.idleBonus == 0.0 && adjustment.score > adjustedPassScore
        }
        val scored = adjusted.map { (action, _, adjustment) ->
            action to if (manaHasAUse) adjustment.score - adjustment.idleBonus else adjustment.score
        }

        val best = scored.maxByOrNull { it.second }
        // A candidate has to beat passing by more than rounding: two lines that score the same in
        // exact arithmetic can land an ulp apart depending on summation order, and that ulp is not
        // a reason to spend mana. Off a production game, a team pump whose stats wore off before
        // anything used them tied passing to the 14th decimal and won.
        val takeAction = best != null && (forcedPlay || best.second > adjustedPassScore + SCORE_EPSILON)
        val chosen = if (takeAction) {
            remember(here)
            // Fill in targets on the returned action so the processor can execute it.
            // The committed target is chosen by simulation (not just the heuristic) so the
            // AI sees the real resolved board, including effects already on the stack.
            // A line that needs sacrificed mana commits its first activation now; the mana floats
            // and the next decision casts with it, since auto-pay spends the pool first.
            manaFirst[best.first] ?: best.first
        } else {
            if (forcedPlay) {
                val fallback = affordable.first()
                fallback.copy(action = chooseCommittedTargets(state, fallback, playerId, budget))
            } else pass ?: legalActions.first()
        }

        if (insightSink != null) {
            recordPriorityInsight(
                state, evaluationState, playerId, startNanos,
                pass = pass, passScore = passScore, adjustedPassScore = adjustedPassScore,
                adjusted = adjusted, dropped = dropped.orEmpty(),
                chosenAction = if (takeAction) best.first else null,
            )
        }
        return chosen
    }

    /**
     * Hand the scores this decision produced to [insightSink], best-first, with passing sitting in
     * the ranking at its own score so the waterline every option had to clear is visible.
     */
    private fun recordPriorityInsight(
        state: GameState,
        evaluationState: GameState,
        playerId: EntityId,
        startNanos: Long,
        pass: LegalAction?,
        passScore: Double,
        adjustedPassScore: Double,
        adjusted: List<Triple<LegalAction, Double, AdjustedScore>>,
        dropped: List<AiActionOption>,
        chosenAction: LegalAction?,
    ) {
        val sink = insightSink ?: return
        val options = mutableListOf<AiActionOption>()
        if (pass != null) {
            options += AiActionOption(
                label = "Pass priority",
                actionType = "PassPriority",
                score = adjustedPassScore,
                rawScore = passScore.takeIf { it != adjustedPassScore },
                advantage = 0.0,
                chosen = chosenAction == null,
                baseline = true,
                note = if (adjustedPassScore != passScore) {
                    "opponent's end step — pass discounted so unspent mana isn't wasted"
                } else {
                    null
                },
                action = pass.action,
            )
        }
        for ((action, leafScore, adjustment) in adjusted) {
            options += AiActionOption(
                label = AiInsightLabels.describe(evaluationState, action, action.action),
                actionType = action.actionType,
                cardName = AiInsightLabels.cardName(evaluationState, action.action),
                targets = AiInsightLabels.targetNames(evaluationState, action.action),
                score = adjustment.score,
                rawScore = leafScore.takeIf { it != adjustment.score },
                advantage = adjustment.score - adjustedPassScore,
                chosen = action === chosenAction,
                note = adjustment.note,
                action = action.action,
            )
        }
        options.sortByDescending { it.score }
        options += dropped

        sink.record(
            evaluationState,
            AiDecisionInsight(
                kind = AiDecisionKind.PRIORITY,
                playerId = playerId,
                turnNumber = state.turnNumber,
                step = state.step.name,
                activePlayerId = state.activePlayerId,
                onOwnTurn = state.isActiveTurnFor(playerId),
                baselineLabel = "Pass priority",
                baselineScore = adjustedPassScore,
                chosenLabel = options.firstOrNull { it.chosen }?.label ?: "Pass priority",
                thinkTimeMs = (System.nanoTime() - startNanos) / 1_000_000,
                options = options,
            ),
        )
    }

    /**
     * An option that never reached scoring, so the panel can say what happened to it.
     *
     * [submittable] is false for a candidate the processor already rejected — it stays visible (the
     * AI did consider it) but carries no action, so the local testing mode can't offer to play a
     * line the engine would refuse.
     */
    private fun droppedOption(
        state: GameState,
        action: LegalAction,
        materialized: GameAction,
        note: String,
        submittable: Boolean,
    ): AiActionOption = AiActionOption(
        label = AiInsightLabels.describe(state, action, materialized),
        actionType = action.actionType,
        cardName = AiInsightLabels.cardName(state, materialized),
        targets = AiInsightLabels.targetNames(state, materialized),
        note = note,
        action = materialized.takeIf { submittable },
    )

    /**
     * The concrete action the AI would submit for [action], and the position it leads to.
     *
     * Targets come from [chooseCommittedTargets], and are re-committed **with** simulation when the
     * cheap pick turns out to be inert. That second attempt is the whole point of this function.
     * Below [com.wingedsheep.ai.engine.budget.BudgetTier.NORMAL] the committed targets are
     * [TargetSelection.rank]'s, which scores a target's board value and has no notion of whether the
     * ability does anything *to* it — so on the quiet opponent's-turn window this guard exists for,
     * it can hand back Aphetto Alchemist untapping itself while a tapped creature sits right there.
     * Writing the ability off on that pick would trade a loop for a missed play.
     *
     * Only the inert path pays: the extra simulations are bounded by
     * [RESCUE_TARGET_CANDIDATES] per requirement and are never reached by a candidate that already
     * does something.
     */
    private fun materialize(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        budget: DecisionBudget,
        here: Long,
    ): Pair<GameAction, SimulationResult> {
        val materialized = chooseCommittedTargets(state, action, playerId, budget)
        val simulation = simulator.simulate(state, materialized)
        // Ordered so the budget that already refines pays nothing at all here — no digest, no
        // second simulation. `chooseAction` digests the leaf it keeps either way.
        if (budget.allowances.refineTargetsBySimulation) return materialized to simulation
        // The three results whose state `chooseAction` never digests. A StoppedAtLimit board is
        // mid-flight, so refining targets against its digest would compare against a position the
        // candidate does not actually reach — hand it back and let the caller drop it.
        if (simulation is SimulationResult.Illegal ||
            simulation is SimulationResult.NeedsDecision ||
            simulation is SimulationResult.StoppedAtLimit
        ) {
            return materialized to simulation
        }
        if (StateProgress.digest(simulation.state) != here) return materialized to simulation

        val refined = chooseCommittedTargets(state, action, playerId, budget, forceTargetRefinement = true)
        if (refined == materialized) return materialized to simulation
        return refined to simulator.simulate(state, refined)
    }

    /**
     * Rescue an affordable action whose auto-pay failed because part of its mana can only come from
     * **sacrificing** a source — Treasure, Gold, Eldrazi Spawn and Scion tokens.
     *
     * The enumerator counts that mana (`ManaSolver.canPay`), but auto-pay never sacrifices a
     * permanent on the player's behalf, so the submitted AutoPay action is refused and the candidate
     * used to be dropped: an MH3 deck held Devourer of Destiny for four turns because its second {C}
     * had to come from a Spawn. Mana abilities are not candidates of their own (see
     * [candidatesFrom]), so nothing else would float that mana either.
     *
     * The sources are activated cheapest first — tokens before anything else, then by board value —
     * and only until the action simulates legal, an any-color source naming a color of the cost.
     * Returns the first activation to commit with the simulation of the whole line, or null when no
     * prefix of sacrifices makes the action payable. The caller commits only that activation: the
     * mana floats, and on the next decision the cast is plain auto-pay, which spends the pool first.
     */
    private fun sacrificeManaFirst(
        state: GameState,
        legalActions: List<LegalAction>,
        candidate: LegalAction,
        payment: GameAction,
        failure: SimulationResult.Illegal,
    ): Pair<LegalAction, SimulationResult>? {
        val autoPaid = when (payment) {
            is CastSpell -> payment.paymentStrategy == PaymentStrategy.AutoPay
            is ActivateAbility -> payment.paymentStrategy == PaymentStrategy.AutoPay
            else -> false
        }
        // Every refusal of an unpayable cost names mana; anything else is not a payment problem.
        if (!autoPaid || !failure.reason.contains("mana", ignoreCase = true)) return null
        val ownSource = (payment as? ActivateAbility)?.sourceId
        val projected = state.projectedState
        val sources = legalActions
            .filter { it.isManaAbility && it.affordable && sacrificesItself(state, it.action) }
            .filter { (it.action as ActivateAbility).sourceId != ownSource }
            .sortedWith(compareBy({ !isToken(state, it) }, { sacrificeCost(state, projected, it) }))
        if (sources.isEmpty()) return null

        val costColors = candidate.manaCostString?.let { ManaCost.parse(it).colors }.orEmpty()
        var current = state
        var first: LegalAction? = null
        for (source in sources) {
            val activations = manaColorOptions(source, costColors).map { color ->
                (source.action as ActivateAbility).copy(manaColorChoice = color)
            }
            var fallback: Pair<GameState, ActivateAbility>? = null
            for (activation in activations) {
                val activated = simulator.simulate(current, activation)
                if (activated !is SimulationResult.Terminal) continue
                val line = simulator.simulate(activated.state, payment)
                if (line !is SimulationResult.Illegal) {
                    return (first ?: source.copy(action = activation)) to line
                }
                if (fallback == null) fallback = activated.state to activation
            }
            val (next, activation) = fallback ?: continue
            current = next
            if (first == null) first = source.copy(action = activation)
        }
        return null
    }

    /** Whether [action] activates a mana ability whose cost sacrifices its own source. */
    private fun sacrificesItself(state: GameState, action: GameAction): Boolean {
        val activation = action as? ActivateAbility ?: return false
        val card = state.getEntity(activation.sourceId)?.get<CardComponent>() ?: return false
        val ability = simulator.cardRegistry.getCard(card.cardDefinitionId)?.script?.activatedAbilities
            ?.firstOrNull { it.id == activation.abilityId } ?: return false
        return when (val cost = ability.cost) {
            is AbilityCost.SacrificeSelf -> true
            is AbilityCost.Composite -> cost.costs.any { it is AbilityCost.SacrificeSelf }
            else -> false
        }
    }

    private fun isToken(state: GameState, source: LegalAction): Boolean =
        state.getEntity((source.action as ActivateAbility).sourceId)?.has<TokenComponent>() == true

    private fun sacrificeCost(
        state: GameState,
        projected: ProjectedState,
        source: LegalAction,
    ): Double {
        val id = (source.action as ActivateAbility).sourceId
        val card = state.getEntity(id)?.get<CardComponent>() ?: return 0.0
        return BoardPresence.permanentValue(state, projected, id, card, intents)
    }

    /**
     * The colors worth trying for [source]: none for a fixed-color ability, else the cost's colors
     * first and then one more, so a generic shortfall still gets paid.
     */
    private fun manaColorOptions(source: LegalAction, costColors: Set<Color>): List<Color?> {
        if (!source.requiresManaColorChoice) return listOf(null)
        val producible = source.availableManaColors ?: Color.entries
        val preferred = producible.filter { it in costColors }
        val rest = producible.filterNot { it in costColors }.take(1)
        return (preferred + rest).ifEmpty { listOf(null) }
    }

    /**
     * Record a position we are about to act from, so a later candidate that leads back to it is
     * recognised as the circle it is.
     *
     * Only positions we *act* from go in: passing may repeat as often as it likes, and recording it
     * would fill the memory with the windows where the AI does nothing. Bounded because a loop is
     * always short — the two-Alchemist cycle is length two — while a game is thousands of positions,
     * and because a digest carries its turn and step, so an entry can only ever match inside the
     * window where matching means going in circles.
     */
    private fun remember(digest: Long) {
        if (digest in positionsActedFrom) return
        positionsActedFrom.addLast(digest)
        if (positionsActedFrom.size > POSITION_MEMORY) positionsActedFrom.removeFirst()
    }

    /**
     * The candidate actions worth scoring.
     *
     * Mana abilities are excluded either way: activating one on its own is never the AI's move —
     * mana is produced as part of paying for something, by the engine's own auto-tap.
     */
    private fun candidatesFrom(legalActions: List<LegalAction>): List<LegalAction> =
        if (useMeaningfulFilter) {
            MeaningfulActionFilter.filterMeaningful(legalActions).filter { it.affordable && !it.isManaAbility }
        } else {
            legalActions.filter { it.affordable && !it.isManaAbility && it.actionType != "PassPriority" }
        }

    private fun handleCombatDeclaration(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId,
        budget: DecisionBudget,
        startNanos: Long,
    ): LegalAction {
        val trace = if (insightSink != null) CombatPlanTrace() else null
        val action = when (legalAction.actionType) {
            "DeclareAttackers" -> combatAdvisor.chooseAttackers(state, legalAction, playerId, budget, trace)
            "DeclareBlockers" -> combatAdvisor.chooseBlockers(
                state, legalAction, playerId, useSimulation = true, budget = budget, trace = trace
            )
            else -> legalAction.action
        }
        if (trace != null) recordCombatInsight(state, legalAction, playerId, action, trace, startNanos)
        return legalAction.copy(action = action)
    }

    /**
     * Hand the combat plans local search simulated to [insightSink], measured against declaring
     * nothing.
     *
     * The trace is empty on the paths that skip local search altogether — a lethal alpha strike, an
     * opponent with no creatures to block with, no legal blockers — so the submitted plan is
     * recorded on its own with that said plainly, rather than as an unexplained single row.
     */
    private fun recordCombatInsight(
        state: GameState,
        legalAction: LegalAction,
        playerId: EntityId,
        action: GameAction,
        trace: CombatPlanTrace,
        startNanos: Long,
    ) {
        val sink = insightSink ?: return
        val attacking = legalAction.actionType == "DeclareAttackers"
        val chosenPlan: Any = when (action) {
            is DeclareAttackers -> action.attackers
            is DeclareBlockers -> action.blockers.filterValues { it.isNotEmpty() }
            else -> emptyMap<EntityId, EntityId>()
        }
        val describe: (CombatPlan) -> String = { plan ->
            when (plan) {
                is CombatPlan.Attack -> AiInsightLabels.describeAttackPlan(state, plan.attackers)
                is CombatPlan.Block -> AiInsightLabels.describeBlockPlan(state, plan.blockers)
            }
        }
        val planOf: (CombatPlan) -> Any = { plan ->
            when (plan) {
                is CombatPlan.Attack -> plan.attackers
                is CombatPlan.Block -> plan.blockers
            }
        }
        val actionOf: (CombatPlan) -> GameAction = { plan ->
            when (plan) {
                is CombatPlan.Attack -> DeclareAttackers(playerId, plan.attackers)
                is CombatPlan.Block -> DeclareBlockers(playerId, plan.blockers)
            }
        }
        val baselineLabel = if (attacking) "No attacks" else "No blocks"
        val baselineScore = trace.plans
            .firstOrNull { describe(it) == baselineLabel }
            ?.score
            ?: trace.plans.minOfOrNull { it.score }
            ?: 0.0

        val options = trace.plans
            .map { plan ->
                AiActionOption(
                    label = describe(plan),
                    actionType = legalAction.actionType,
                    score = plan.score,
                    advantage = plan.score - baselineScore,
                    chosen = planOf(plan) == chosenPlan,
                    baseline = describe(plan) == baselineLabel,
                    action = actionOf(plan),
                )
            }
            .sortedByDescending { it.score }
            .toMutableList()

        val chosenLabel = if (attacking) {
            AiInsightLabels.describeAttackPlan(state, (action as? DeclareAttackers)?.attackers.orEmpty())
        } else {
            AiInsightLabels.describeBlockPlan(state, (action as? DeclareBlockers)?.blockers.orEmpty())
        }
        if (options.none { it.chosen }) {
            options.add(
                0,
                AiActionOption(
                    label = chosenLabel,
                    actionType = legalAction.actionType,
                    chosen = true,
                    note = "heuristic seed — local search did not run for this declaration",
                    action = action,
                ),
            )
        }

        sink.record(
            state,
            AiDecisionInsight(
                kind = if (attacking) AiDecisionKind.DECLARE_ATTACKERS else AiDecisionKind.DECLARE_BLOCKERS,
                playerId = playerId,
                turnNumber = state.turnNumber,
                step = state.step.name,
                activePlayerId = state.activePlayerId,
                onOwnTurn = state.isActiveTurnFor(playerId),
                baselineLabel = baselineLabel,
                baselineScore = baselineScore,
                chosenLabel = chosenLabel,
                thinkTimeMs = (System.nanoTime() - startNanos) / 1_000_000,
                options = options,
            ),
        )
    }

    /**
     * Apply the two per-card adjustments to a leaf score, in raw evaluator units.
     *
     * Both are deltas on top of whatever the leaf said, which is what lets the rollout evaluator
     * slot in underneath without any of this changing: a hold-policy penalty and a `CardAdvisor`
     * override compose with a rollout mean exactly as they composed with a static evaluation.
     */
    private fun adjustScore(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        rawLeafScore: Double,
        passScore: Double,
        leafState: GameState,
    ): AdjustedScore {
        if (spendFuelWalkers) fuelWalkerSpend(state, action, playerId, passScore, leafState)?.let { return it }
        val deployed = deploymentCredit(state, action, leafState, playerId)
        val fuel = fuelLoyaltyCredit(state, action, leafState, playerId)
        val credit = deployed + fuel
        val leafScore = rawLeafScore + credit
        val creditNote = listOfNotNull(
            if (deployed > 0.0) "deployed, not spent %+.2f".format(deployed) else null,
            if (fuel > 0.0) "loyalty is only fuel %+.2f".format(fuel) else null,
        ).joinToString("; ").ifEmpty { null }
        rideForTheAttack(state, action, playerId, leafState, passScore)?.let { return it }
        val cardName = resolveCardName(state, action) ?: return AdjustedScore(leafScore, creditNote)

        // A precombat pump the attack plan needs is priced by the attack it buys, ahead of the
        // floors below that would otherwise defer it to a window the AI never stops in.
        if (precombatPumpsForThePlan) {
            pumpThatBuysTheAttack(state, action, playerId, cardName, leafState)?.let { gain ->
                return AdjustedScore(passScore + gain, "pump changes the attack plan %+.2f".format(gain))
            }
        }

        // Phase 6: what the board looks like after this resolves is only half the question; the
        // other half is whether this was the window — and, for removal, whether this was the target
        // worth spending the card on. The materialized action is what carries the committed
        // targets, so the hold policy is asked about the same spell the processor would receive.
        //
        // The activation is handed over for the same reason: an ability's window is a question about
        // the *ability*, and `cardName` only ever names the permanent it is printed on.
        val timing = holdPolicy.verdictFor(
            state, playerId, cardName,
            cast = action.action as? CastSpell,
            activation = action.action as? ActivateAbility,
            leaf = leafState,
        )
        if (timing is TimingVerdict.NoWindow) {
            // The card does nothing here, so nothing the simulation reports should make it beat
            // passing. See [TimingVerdict.NoWindow] for why this is a floor and not a penalty.
            return AdjustedScore(passScore - 1.0, "hold policy: wrong window — floored below passing", floored = true)
        }
        if (activatedPumpsNeedAPlannedAttack && pumpLeftHomeByThePlan(state, action, playerId, cardName, leafState)) {
            // The same floor, for the same reason: what it bought is gone at cleanup, and the attack
            // that could have spent it either leaves every creature it lands on at home or sends
            // them without it.
            return AdjustedScore(passScore - 1.0, "attack plan does not need the pump — floored below passing", floored = true)
        }
        val timingDelta = (timing as? TimingVerdict.Adjust)?.delta ?: 0.0
        val idleBonus = if ((timing as? TimingVerdict.Adjust)?.onlyWhenIdle == true) timingDelta else 0.0
        val timingReason = (timing as? TimingVerdict.Adjust)?.reason ?: "timing"
        val timingNote = listOfNotNull(
            creditNote,
            if (timingDelta != 0.0) "hold policy $timingReason %+.2f".format(timingDelta) else null,
        ).joinToString("; ").ifEmpty { null }

        // Check for card-specific advisor override. Timing is applied outside it, so a per-card
        // advisor still sees the pure board score as its `defaultScore` and a card with both
        // keeps both.
        val advisor = advisorRegistry.getAdvisor(cardName)
            ?: return AdjustedScore(leafScore + timingDelta, timingNote, idleBonus)
        val context = CastContext(
            state = state,
            projected = state.projectedState,
            playerId = playerId,
            action = action,
            passScore = passScore,
            defaultScore = leafScore,
            evaluator = evaluator,
            simulator = simulator
        )
        val override = advisor.evaluateCast(context)
        val advisorNote = override?.let { "${advisor::class.simpleName} replaced the board score" }
        return AdjustedScore(
            (override ?: leafScore) + timingDelta,
            listOfNotNull(advisorNote, timingNote).joinToString("; ").ifEmpty { null },
            idleBonus,
        )
    }

    /**
     * Whether [action] — an activated ability whose every payoff is an until-end-of-turn creature
     * grant — is being bought before our attack for creatures the attack plan then keeps home.
     *
     * The leaf cannot tell "could attack" from "will attack", and neither can the rollouts: a
     * playout's attack is `CombatSeed`'s, read off the pumped board, while the attack actually
     * declared is [combatAdvisor]'s. Off a production game (2026-10-14): at 2 life, Shatterwing
     * Pegasus's `{4}{W}: Creatures you control get +1/+1 until end of turn` in our precombat main,
     * scored above passing by the playouts, and then no attack — five mana for a bonus that wore off
     * before the opponent's lethal crack-back. So this asks the planner itself: walk the leaf to
     * declare attackers and see whom it sends. A grant that lands on none of them is a payment for
     * nothing, and is floored like [TimingVerdict.NoWindow]. So is an instant-speed grant on
     * creatures the plan sends *anyway*: it changes no attack, and the same pump can be bought once
     * they are attacking.
     *
     * The same claim [AiProfile.pumpsNeedAPlannedAttack] makes about a beginning-of-combat yes/no,
     * applied to an activation, from either precombat window. Only our own precombat main or
     * beginning of combat with an empty stack, never inside a nested simulation, and only when the
     * leaf actually created such a grant; anything unreadable answers false and leaves the score
     * alone. The attack search runs once per qualifying candidate.
     */
    private fun pumpLeftHomeByThePlan(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        cardName: String,
        leafState: GameState,
    ): Boolean {
        val activation = action.action as? ActivateAbility ?: return false
        if (!state.isActiveTurnFor(playerId) || state.stack.isNotEmpty()) return false
        if (state.step != Step.PRECOMBAT_MAIN && state.step != Step.BEGIN_COMBAT) return false
        if (simulator.resolvingNestedDecision) return false
        val ability = intents.activatedAbility(cardName, activation.abilityId) ?: return false
        if (!ExpiringGrantWindow.buysOnlyExpiringCreatureGrants(ability)) return false
        if (leafState.gameOver || leafState.pendingDecision != null) return false

        val before = state.floatingEffects.mapTo(HashSet()) { it.id }
        val projected = leafState.projectedState
        val pumped = leafState.floatingEffects
            .filter { it.id !in before && it.duration == Duration.EndOfTurn }
            .flatMapTo(HashSet()) { it.effect.affectedEntities }
            .filter { projected.isCreature(it) && projected.getController(it) == playerId }
        if (pumped.isEmpty()) return false

        val planned = plannedAttackers(leafState, playerId) ?: return false
        val sentWithThePump = pumped.filter { it in planned }
        if (sentWithThePump.isEmpty()) return true

        // A creature the plan sends with or without the pump does not need it bought now: an
        // instant-speed pump can still go on it once it is attacking — before blocks for an evasion
        // grant, after them for stats — with strictly more information. Only a pump that changes
        // who attacks has to be in force when the attack is chosen. A sorcery-speed one has no later
        // window, so for it "the plan uses it" is enough.
        if (ability.timing != TimingRule.InstantSpeed) return false
        val plannedAnyway = plannedAttackers(state, playerId) ?: return false
        return sentWithThePump.all { it in plannedAnyway }
    }

    /**
     * [CombatAdvisor.plannedAttackersFrom] for [state], remembered for the last state asked about —
     * every candidate of one decision shares the root, so [pumpLeftHomeByThePlan]'s "would it
     * attack anyway" plan is searched once per decision, not once per pump.
     */
    private fun plannedAttackers(state: GameState, playerId: EntityId): Set<EntityId>? {
        val memo = lastPlan
        if (memo != null && memo.first === state) return memo.second
        val plan = combatAdvisor.plannedAttackersFrom(state, playerId) { budgetPolicy.budgetForDecision(it, playerId) }
        lastPlan = state to plan
        return plan
    }

    /**
     * What a precombat pump is worth when the attack plan **needs** it — [AiProfile.precombatPumpsForThePlan] —
     * or null when it is not that shape and the ordinary floors decide.
     *
     * The engine AI never stops in its own beginning of combat: `MeaningfulActionFilter` passes it
     * unconditionally, on the server and in [AIPlayer]'s fast path. So [HoldPolicy]'s "a trick
     * waits for combat" and `ExpiringGrantWindow`'s "defer to beginning of combat" both defer a
     * precombat pump to a window that never comes, and the pump that would have let a creature
     * attack — a 2/2 made a 5/5 against their 3/3 — is never cast before attacks are chosen.
     * (PR #3211's known gap.) Nor could the leaf price it if it were: under
     * `expiringStatsNeedAFight` an until-end-of-turn bonus on a creature not yet attacking counts
     * for nothing, rightly, since most such pumps are spent on no fight at all.
     *
     * So this asks the attack planner directly, from both sides of the cast. The pump is the plan's
     * when a creature it lands on is in the plan read off the pumped board **and not** in the plan
     * read off the board as it stands — the pump changes who attacks. Then its value is what the
     * planner itself says that attack is worth over the unpumped one: each plan simulated through
     * combat, blocks by heuristic, crack-back priced. A pump on a creature the plan sends anyway is
     * not this — it can still be cast once the creature is attacking, after blocks, with strictly
     * more information — and neither is one the plan ignores, which is the no-attack pump #3211
     * floors.
     *
     * Our own precombat main, empty stack, never inside a nested simulation; spells read as a
     * combat trick or team pump, activations whose every payoff is an end-of-turn creature grant.
     * Two attack searches per qualifying candidate, the unpumped one shared across the decision.
     */
    private fun pumpThatBuysTheAttack(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        cardName: String,
        leafState: GameState,
    ): Double? {
        if (!state.isActiveTurnFor(playerId) || state.stack.isNotEmpty()) return null
        if (state.step != Step.PRECOMBAT_MAIN || simulator.resolvingNestedDecision) return null
        if (leafState.gameOver || leafState.pendingDecision != null) return null
        val isPump = when (val gameAction = action.action) {
            is CastSpell -> intents.forCast(cardName, gameAction)?.let { IntentTag.COMBAT_TRICK in it.tags || it.expiringGroupPump } == true
            is ActivateAbility -> intents.activatedAbility(cardName, gameAction.abilityId)
                ?.let(ExpiringGrantWindow::buysOnlyExpiringCreatureGrants) == true
            else -> false
        }
        if (!isPump) return null
        val pumped = pumpedThisTurn(state, leafState, playerId)
        if (pumped.isEmpty()) return null
        val withPump = combatAdvisor.plannedAttackFrom(leafState, playerId) { budgetPolicy.budgetForDecision(it, playerId) }
            ?: return null
        val without = plannedAttack(state, playerId) ?: return null
        if (pumped.none { it in withPump.attackers && it !in without.attackers }) return null
        // Facing lethal on board, every plan's value is the crack-back cliff plus whatever the
        // heuristic blocker happens to do, and a pump that "saves" us by baiting a bad block is a
        // reading of the model, not of the game (#3211's T15). There only a pump that wins now is
        // priced here; anything else is left to the floors and the survival logic.
        if (ThreatAssessment.lethalOnBoardAgainst(state, state.projectedState, playerId) &&
            withPump.value < Double.MAX_VALUE / 4
        ) {
            return null
        }
        val gain = withPump.value - without.value
        return gain.takeIf { it > SCORE_EPSILON }
    }

    /**
     * Crew N / Saddle N on our own turn, priced by the attack it buys — or null to leave the leaf's
     * score alone (the opponent's turn, where a crewed Vehicle can block; a nested simulation; a
     * plan that can't be read).
     *
     * The leaf can't see why we crew: a crewed Vehicle reads as one more creature on the board, and
     * a saddled Mount as nothing at all, since "whenever this attacks while saddled" pays out only
     * in combat. Both last until end of turn and both tap the crew, which then can't block on the
     * opponent's turn. So on our turn the only reason to ride is the attack: in our precombat main
     * the attack planner is asked, from both sides of the activation, whether it sends the Vehicle
     * or Mount and what that attack is worth over the one without it (the same pricing
     * [pumpThatBuysTheAttack] gives a pump). A ride the plan doesn't send, or that buys nothing,
     * and any ride later in our turn, once attackers can no longer be declared, is floored below
     * passing.
     */
    private fun rideForTheAttack(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        leafState: GameState,
        passScore: Double,
    ): AdjustedScore? {
        val ride = when (val gameAction = action.action) {
            is CrewVehicle -> gameAction.vehicleId
            is SaddleMount -> gameAction.mountId
            else -> return null
        }
        if (!state.isActiveTurnFor(playerId) || simulator.resolvingNestedDecision) return null
        val floor = AdjustedScore(passScore - 1.0, "rides into no attack — floored below passing", floored = true)
        if (state.step != Step.PRECOMBAT_MAIN) {
            // Before attacks (untap, upkeep, draw, beginning of combat) the ride still has its
            // attack ahead of it; only once declarations are past is it certainly for nothing.
            return if (state.step.ordinal >= Step.DECLARE_ATTACKERS.ordinal) floor else null
        }
        if (state.stack.isNotEmpty() || leafState.gameOver || leafState.pendingDecision != null) return null
        // A Vehicle that came in this turn (or is tapped, or can't attack) rides into nothing; the
        // planner has no attack to read off it when it is the only would-be attacker, so say so here.
        val leafProjected = leafState.projectedState
        val rideEntity = leafState.getEntity(ride) ?: return null
        val rideCanAttack = leafProjected.isCreature(ride) &&
            !rideEntity.has<TappedComponent>() &&
            !leafProjected.cantAttack(ride) &&
            Keyword.DEFENDER.name !in leafProjected.getKeywords(ride) &&
            (!rideEntity.has<SummoningSicknessComponent>() ||
                Keyword.HASTE.name in leafProjected.getKeywords(ride) ||
                leafProjected.canAttackAsThoughHasty(ride))
        if (!rideCanAttack) return floor
        val withRide = combatAdvisor.plannedAttackFrom(leafState, playerId) { budgetPolicy.budgetForDecision(it, playerId) }
            ?: return null
        if (ride !in withRide.attackers) return floor
        val without = plannedAttack(state, playerId) ?: return null
        val gain = withRide.value - without.value
        if (gain <= SCORE_EPSILON) return floor
        return AdjustedScore(passScore + gain, "ride buys the attack %+.2f".format(gain))
    }

    /** Our creatures an until-end-of-turn effect created between [root] and [leaf] lands on. */
    private fun pumpedThisTurn(root: GameState, leaf: GameState, playerId: EntityId): List<EntityId> {
        val before = root.floatingEffects.mapTo(HashSet()) { it.id }
        val projected = leaf.projectedState
        return leaf.floatingEffects
            .filter { it.id !in before && it.duration == Duration.EndOfTurn }
            .flatMapTo(HashSet()) { it.effect.affectedEntities }
            .filter { projected.isCreature(it) && projected.getController(it) == playerId }
    }

    /** [CombatAdvisor.plannedAttackFrom] for [state], remembered like [plannedAttackers]. */
    private fun plannedAttack(state: GameState, playerId: EntityId): CombatAdvisor.PlannedAttack? {
        val memo = lastPlannedAttack
        if (memo != null && memo.first === state) return memo.second
        val plan = combatAdvisor.plannedAttackFrom(state, playerId) { budgetPolicy.budgetForDecision(it, playerId) }
        lastPlannedAttack = state to plan
        return plan
    }

    private var lastPlannedAttack: Pair<GameState, CombatAdvisor.PlannedAttack?>? = null

    /** The memo behind [plannedAttackers]: the state last asked about, by identity, and its plan. */
    private var lastPlan: Pair<GameState, Set<EntityId>?>? = null

    /** A spell a land drop made castable, and the quiet state casting it from the land's leaf reaches. */
    private class LandFollowUp(
        val landIndex: Int,
        val action: LegalAction,
        val leaf: GameState,
        /**
         * The leaf index of the same card cast on its own from the root, when it was a candidate
         * there too — set only by [enablerFollowUps], whose pair is then priced against it.
         */
        val sibling: Int? = null,
        /** How the panel names the pair, when the default wording for its shape would mislabel it. */
        val note: String? = null,
    )

    /**
     * For each land-drop leaf, the spells castable after the land that were not castable before it
     * — [AiProfile.landDropUnlocksSpells].
     *
     * The search is one action deep, so a land drop is scored as a land and the spell it pays for
     * is invisible until the next decision — by which time a cheaper spell may have spent the mana
     * that spell needed. Only our own main phase with an empty stack, where the next decision is
     * ours and the land's mana is still there for it. "Not castable before" is the raw enumerated
     * action, so an evoke cast that was already affordable does not count, and the hard cast of the
     * same card does. Stops when the budget does.
     *
     * An X-cost spell or ability that was already affordable counts too, at the X values only the
     * land's mana reaches: spent first, it is cast one X smaller and the land adds nothing to it.
     */
    private fun landDropFollowUps(
        root: GameState,
        legalActions: List<LegalAction>,
        leaves: List<LegalAction>,
        leafStates: List<GameState>,
        playerId: EntityId,
        budget: DecisionBudget,
    ): List<LandFollowUp> {
        if (!root.isActiveTurnFor(playerId) || root.stack.isNotEmpty()) return emptyList()
        if (root.step != Step.PRECOMBAT_MAIN && root.step != Step.POSTCOMBAT_MAIN) return emptyList()
        val castableNow = legalActions.filter { it.affordable }.map { it.action }.toSet()
        val rootMaxX = HashMap<Any, Int>()
        for (action in legalActions) {
            if (!action.affordable || !action.hasXCost) continue
            val key = xCostSourceKey(action.action) ?: continue
            rootMaxX.merge(key, action.maxAffordableX ?: 0, ::maxOf)
        }
        val followUps = mutableListOf<LandFollowUp>()
        for (i in leaves.indices) {
            if (leaves[i].action !is PlayLand) continue
            val afterLand = leafStates[i]
            if (afterLand.pendingDecision != null || afterLand.stack.isNotEmpty() ||
                afterLand.step != root.step || afterLand.priorityPlayerId != playerId
            ) continue
            val candidates = candidatesFrom(simulator.getLegalActions(afterLand, playerId))
            val newlyCastable = candidates.filter { it.action is CastSpell && it.action !in castableNow }
            // Already castable, but the land raises its X: only the X values past the root's reach.
            fun rootX(action: LegalAction): Int? = xCostSourceKey(action.action)?.let { rootMaxX[it] }
            val biggerX = expandXCostAbilities(
                afterLand,
                candidates.filter { it.hasXCost && (it.maxAffordableX ?: 0) > (rootX(it) ?: Int.MAX_VALUE) },
                playerId,
            ).filter { (xValueOf(it.action) ?: 0) > (rootX(it) ?: Int.MAX_VALUE) }
            val unlocked = (newlyCastable + biggerX).filter {
                !it.hasUnfillableTargetRequirement &&
                    !(targetPolarityFromEffect && TargetSelection.aimsOnlyAtTheWrongSide(afterLand, it, playerId, intents))
            }
            if (unlocked.isEmpty()) continue
            val here = StateProgress.digest(afterLand)
            for (action in unlocked) {
                if (budget.expired()) return followUps
                val (materialized, simulation) = materialize(afterLand, action, playerId, budget, here)
                if (simulation is SimulationResult.Illegal || simulation is SimulationResult.StoppedAtLimit) continue
                followUps += LandFollowUp(i, action.copy(action = materialized), simulation.state)
            }
        }
        return followUps
    }

    /** What identifies an X-cost action across positions: the spell's card, or the ability's source and index. */
    private fun xCostSourceKey(action: GameAction): Any? = when (action) {
        is CastSpell -> action.cardId
        is ActivateAbility -> action.sourceId to action.abilityId
        else -> null
    }

    private fun xValueOf(action: GameAction): Int? = when (action) {
        is CastSpell -> action.xValue
        is ActivateAbility -> action.xValue
        else -> null
    }

    /**
     * For each spell leaf, the spells in our hand it made **cheaper** to cast — or castable at all —
     * from the position right after it: [AiProfile.enablersFirst].
     *
     * The search is one action deep, so it cannot see that casting a noncreature spell first takes
     * {2} off a "costs {2} less if you've cast a noncreature spell this turn" creature, or that a
     * cost reducer pays for itself the moment it lands. Greedy single picks then cast the payoff at
     * full price and strand the enabler. The misplay (2026-10-14, Reality Fracture draft, turn 10):
     * Traxos, Academy Guardian cast for {3}{U} with Tam's Resistance ({1}{G/U} sorcery) in hand —
     * the same four mana would have cast both.
     *
     * Read off the engine's own price, never off the card: a follow-up counts when its enumerated
     * cost in the leaf has a lower mana value than at the root, or it was not castable at the root.
     * Only cards already in our hand at the root, so a spell a cantrip draws is not credited to the
     * cantrip as if it had been known. Our own main phase with an empty stack, a quiet leaf where we
     * still hold priority — the shape [landDropFollowUps] uses, whose credit this shares. Stops when
     * the budget does.
     */
    private fun enablerFollowUps(
        root: GameState,
        legalActions: List<LegalAction>,
        leaves: List<LegalAction>,
        leafStates: List<GameState>,
        playerId: EntityId,
        budget: DecisionBudget,
    ): List<LandFollowUp> {
        if (!root.isActiveTurnFor(playerId) || root.stack.isNotEmpty()) return emptyList()
        if (root.step != Step.PRECOMBAT_MAIN && root.step != Step.POSTCOMBAT_MAIN) return emptyList()
        val hand = root.getZone(ZoneKey(playerId, Zone.HAND)).toSet()
        val rootPrice = HashMap<EntityId, Int>()
        for (action in legalActions) {
            if (!action.affordable) continue
            val cast = action.action as? CastSpell ?: continue
            val price = priceOf(action) ?: continue
            rootPrice.merge(cast.cardId, price, ::minOf)
        }
        val followUps = mutableListOf<LandFollowUp>()
        for (i in leaves.indices) {
            val enabler = leaves[i].action as? CastSpell ?: continue
            val after = leafStates[i]
            if (after.pendingDecision != null || after.stack.isNotEmpty() ||
                after.step != root.step || after.priorityPlayerId != playerId
            ) continue
            val discounted = candidatesFrom(simulator.getLegalActions(after, playerId)).filter { action ->
                val cast = action.action as? CastSpell ?: return@filter false
                if (cast.cardId == enabler.cardId || cast.cardId !in hand) return@filter false
                if (action.hasUnfillableTargetRequirement) return@filter false
                if (targetPolarityFromEffect && TargetSelection.aimsOnlyAtTheWrongSide(after, action, playerId, intents)) {
                    return@filter false
                }
                val price = priceOf(action) ?: return@filter false
                val before = rootPrice[cast.cardId]
                before == null || price < before
            }
            if (discounted.isEmpty()) continue
            val here = StateProgress.digest(after)
            for (action in discounted) {
                if (budget.expired()) return followUps
                val (materialized, simulation) = materialize(after, action, playerId, budget, here)
                if (simulation is SimulationResult.Illegal || simulation is SimulationResult.StoppedAtLimit) continue
                val cardId = (action.action as CastSpell).cardId
                val sibling = leaves.indices.firstOrNull { j -> (leaves[j].action as? CastSpell)?.cardId == cardId }
                followUps += LandFollowUp(i, action.copy(action = materialized), simulation.state, sibling)
            }
        }
        return followUps
    }

    /**
     * For each non-land leaf, the land drops it puts in our hand — [AiProfile.fetchedLandDropFollowUp].
     *
     * The search is one action deep, so a landcycle, a land tutor or a draw that finds a land is
     * scored as "a land in hand" — and a land in hand is priced well below the business card it
     * cost — never as the land on the battlefield it becomes a moment later when our land drop is
     * still unused. The misplay (2026-10-16 AI self-play, 2p game 03, turns 7 and 11): stuck on
     * three lands with only four-to-six drops in hand, the AI passed both main phases rather than
     * Plainscycling Noble Templar for the fourth land.
     *
     * Only lands that were not in our hand at the root: a land already there is a root candidate of
     * its own, and crediting it to an unrelated spell would argue for that spell on the land's
     * merits. The leaf must be quiet — no pending decision (the simulator's decision resolver has
     * already answered the search with the AI's own pick), empty stack, the same main step, our
     * priority — so the land play is legal and the next decision is ours. Stops when the budget does.
     */
    private fun fetchedLandFollowUps(
        root: GameState,
        leaves: List<LegalAction>,
        leafStates: List<GameState>,
        playerId: EntityId,
        budget: DecisionBudget,
    ): List<LandFollowUp> {
        if (!root.isActiveTurnFor(playerId) || root.stack.isNotEmpty()) return emptyList()
        if (root.step != Step.PRECOMBAT_MAIN && root.step != Step.POSTCOMBAT_MAIN) return emptyList()
        // No land drop left: nothing a leaf fetches can be played this turn.
        if ((root.getEntity(playerId)?.get<LandDropsComponent>()?.remaining ?: 0) <= 0) return emptyList()
        val hand = root.getZone(ZoneKey(playerId, Zone.HAND)).toSet()
        val followUps = mutableListOf<LandFollowUp>()
        for (i in leaves.indices) {
            if (budget.expired()) return followUps
            val action = leaves[i].action
            if (action is PlayLand || action is PassPriority) continue
            val after = leafStates[i]
            if (after.pendingDecision != null || after.stack.isNotEmpty() ||
                after.step != root.step || after.priorityPlayerId != playerId
            ) continue
            val newLands = after.getZone(ZoneKey(playerId, Zone.HAND)).filterTo(HashSet()) {
                it !in hand && after.getEntity(it)?.get<CardComponent>()?.typeLine?.isLand == true
            }
            if (newLands.isEmpty()) continue
            val fetched = simulator.getLegalActions(after, playerId).filter {
                val play = it.action as? PlayLand
                it.affordable && play != null && play.cardId in newLands
            }
            for (land in fetched) {
                if (budget.expired()) return followUps
                val simulation = simulator.simulate(after, land.action)
                if (simulation is SimulationResult.Illegal || simulation is SimulationResult.StoppedAtLimit) continue
                followUps += LandFollowUp(i, land, simulation.state)
            }
        }
        return followUps
    }

    /**
     * For each non-land leaf, casting again the permanent it returned from our battlefield to our
     * hand — [AiProfile.recastBouncedPermanents].
     *
     * The search is one action deep, so "return target permanent you control to its owner's hand"
     * is scored as a permanent lost and a card gained, never as the enters-the-battlefield trigger
     * that recasting it buys. The misplay (Rescue Girl, First Responder, reported from live games):
     * the AI tapped her to return its own Island while Mindseeker Oculus — "when this creature
     * enters, empower Jace 4" — sat beside it with the mana to recast it.
     *
     * Same shape as [fetchedLandFollowUps]: our own main phase, a quiet leaf where we still hold
     * priority, and only cards that were on our battlefield at the root. A returned land is left to
     * that pass; this one follows spells. Stops when the budget does.
     */
    private fun bouncedPermanentFollowUps(
        root: GameState,
        leaves: List<LegalAction>,
        leafStates: List<GameState>,
        playerId: EntityId,
        budget: DecisionBudget,
    ): List<LandFollowUp> {
        val followUps = mutableListOf<LandFollowUp>()
        for (i in leaves.indices) {
            if (budget.expired()) return followUps
            val action = leaves[i].action
            if (action is PlayLand || action is PassPriority) continue
            val after = leafStates[i]
            val recasts = replaysOfBounced(root, after, playerId).filter { it.action is CastSpell }
            if (recasts.isEmpty()) continue
            val here = StateProgress.digest(after)
            for (recast in recasts) {
                if (budget.expired()) return followUps
                val (materialized, simulation) = materialize(after, recast, playerId, budget, here)
                if (simulation is SimulationResult.Illegal || simulation is SimulationResult.StoppedAtLimit) continue
                followUps += LandFollowUp(i, recast.copy(action = materialized), simulation.state, note = "then recasts")
            }
        }
        return followUps
    }

    /**
     * The best static score among the plays that put back what [leaf] returned from our
     * battlefield to our hand — a land played again, a permanent spell recast — or
     * `-Infinity` when there is none. [chooseCommittedTargets] takes it over the leaf's own score,
     * so a self-bounce aims at the permanent whose return is worth most rather than the one whose
     * loss costs least: Mindseeker Oculus to recast for its trigger, not the Island that only
     * comes back as the same Island, nor a Jace token that would cease to exist. Targets of the
     * replay are the heuristic pick: this runs inside target refinement and must stay cheap.
     */
    private fun bestReplayOfBounced(root: GameState, leaf: GameState, playerId: EntityId): Double =
        replaysOfBounced(root, leaf, playerId).maxOfOrNull { replay ->
            val action = if (replay.action is PlayLand) replay.action else heuristicTargets(leaf, replay, playerId)
            simulator.simulate(leaf, action).scoreOrRankLast { after ->
                evaluator.evaluate(after, after.projectedState, playerId)
            }
        } ?: Double.NEGATIVE_INFINITY

    /**
     * The affordable land plays and spell casts, from [leaf], of cards that were on [playerId]'s
     * battlefield at [root] and are in their hand at [leaf]. Empty outside our own main phase with
     * an empty stack, or when [leaf] is not a quiet position where we hold priority.
     */
    private fun replaysOfBounced(root: GameState, leaf: GameState, playerId: EntityId): List<LegalAction> {
        if (!root.isActiveTurnFor(playerId) || root.stack.isNotEmpty()) return emptyList()
        if (root.step != Step.PRECOMBAT_MAIN && root.step != Step.POSTCOMBAT_MAIN) return emptyList()
        if (leaf.pendingDecision != null || leaf.stack.isNotEmpty() ||
            leaf.step != root.step || leaf.priorityPlayerId != playerId
        ) return emptyList()
        val wasOurs = root.getBattlefield(playerId).toHashSet()
        val returned = leaf.getZone(ZoneKey(playerId, Zone.HAND)).filterTo(HashSet()) { it in wasOurs }
        if (returned.isEmpty()) return emptyList()
        return candidatesFrom(simulator.getLegalActions(leaf, playerId)).filter { replay ->
            val cardId = when (val action = replay.action) {
                is CastSpell -> action.cardId
                is PlayLand -> action.cardId
                else -> return@filter false
            }
            cardId in returned && !replay.hasUnfillableTargetRequirement &&
                !(targetPolarityFromEffect && TargetSelection.aimsOnlyAtTheWrongSide(leaf, replay, playerId, intents))
        }
    }

    /** The mana value of an enumerated cast's price, or null when it has none to compare (X). */
    private fun priceOf(action: LegalAction): Int? {
        if (action.hasXCost) return null
        val cost = action.manaCostString ?: return null
        return runCatching { ManaCost.parse(cost).cmc }.getOrNull()
    }

    /**
     * What the evaluator charged for taking [action]'s card out of our hand, refunded when the cast
     * merely *deployed* it — [AiProfile.permanentCastIsNotCardLoss].
     *
     * Measured with the evaluator itself rather than by re-deriving `CardAdvantage`'s curve: the
     * charge is `evaluate(root) − evaluate(root without that card in hand)`, so whichever hand model
     * the profile runs (the land-drop earmark, lands priced as mana, a fitted raw vector) is refunded
     * at exactly the price it charged, and nothing here can drift from it.
     *
     * Four conditions, each the reason the refund is honest:
     *  - **Our own main phase, empty stack** — sorcery timing, where passing cannot buy a better
     *    window, only a later main phase with the same mana.
     *  - **The card was in our hand** — a cast from the graveyard or exile never had hand value.
     *  - **The leaf has it on our battlefield** — the simulation, not the type line, says it became a
     *    permanent, so an Adventure cast as its spell half, or a creature that was countered, gets
     *    nothing.
     *  - **Never negative** — the refund cancels a charge; it is not a second opinion on the hand.
     *
     * The board side then decides alone: a body worth having beats passing, a permanent that makes
     * the board worse (or only costs mana the hand could have spent) still does not.
     */
    private fun deploymentCredit(
        root: GameState,
        action: LegalAction,
        leaf: GameState,
        playerId: EntityId,
    ): Double {
        if (!permanentCastIsNotCardLoss) return 0.0
        val cast = action.action as? CastSpell ?: return 0.0
        if (!root.isActiveTurnFor(playerId) || root.stack.isNotEmpty()) return 0.0
        if (root.step != Step.PRECOMBAT_MAIN && root.step != Step.POSTCOMBAT_MAIN) return 0.0
        val hand = ZoneKey(playerId, Zone.HAND)
        if (cast.cardId !in root.getZone(hand)) return 0.0
        if (cast.cardId !in leaf.getBattlefield(playerId)) return 0.0
        val without = root.removeFromZone(hand, cast.cardId)
        val charge = evaluator.evaluate(root, root.projectedState, playerId) -
            evaluator.evaluate(without, without.projectedState, playerId)
        return charge.coerceAtLeast(0.0)
    }

    /**
     * What the evaluator charged for the loyalty a loyalty ability spent, refunded when the walker
     * can only ever spend it — [AiProfile.fuelLoyaltyIsNotBoardValue].
     *
     * `BoardFeatures.permanentValue` prices a planeswalker at its loyalty, so a −N activation is
     * charged N counters' worth of board value and has to buy more than that on the board to beat
     * passing. For a walker with a + ability that is a real trade-off. For one whose every loyalty
     * ability costs loyalty (the empower-Jace token) it is not: an activation it skips is lost for
     * good, and the counters it keeps can only be spent later on the same abilities. Measured with
     * the evaluator itself, as [deploymentCredit] is: `evaluate(leaf with the spent loyalty put
     * back) − evaluate(leaf)`.
     *
     * Only while the walker survives the activation: one that spends its last counter dies, and
     * losing the permanent is still charged — a −1 at one loyalty must buy the walker's worth.
     * Under [defendPlaneswalkers], only while it survives the next attack too
     * ([PlaneswalkerThreat.doomed]): counters a doomed walker keeps are about to be lost, so
     * spending them cost nothing and there is nothing to refund — while refunding them as if the
     * walker kept them would make the −1 that leaves it dead score like the −1 that leaves it safe,
     * and outbid the −3 that cashes the whole walker in before it goes. Never negative.
     */
    private fun fuelLoyaltyCredit(
        root: GameState,
        action: LegalAction,
        leaf: GameState,
        playerId: EntityId,
    ): Double {
        if (!fuelLoyaltyIsNotBoardValue) return 0.0
        val activation = action.action as? ActivateAbility ?: return 0.0
        val sourceId = activation.sourceId
        val name = root.getEntity(sourceId)?.get<CardComponent>()?.name ?: return 0.0
        if (intents.activatedAbility(name, activation.abilityId)?.isPlaneswalkerAbility != true) return 0.0
        if (!intents.loyaltyIsOnlyFuel(name)) return 0.0
        if (sourceId !in leaf.getBattlefield(playerId)) return 0.0
        val before = root.getEntity(sourceId)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: return 0.0
        val counters = leaf.getEntity(sourceId)?.get<CountersComponent>() ?: return 0.0
        val spent = before - counters.getCount(CounterType.LOYALTY)
        if (spent <= 0) return 0.0
        if (defendPlaneswalkers && PlaneswalkerThreat.doomed(leaf, leaf.projectedState, sourceId)) return 0.0
        val restored = leaf.updateEntity(sourceId) { it.with(counters.withAdded(CounterType.LOYALTY, spent)) }
        val charge = evaluator.evaluate(restored, restored.projectedState, playerId) -
            evaluator.evaluate(leaf, leaf.projectedState, playerId)
        return charge.coerceAtLeast(0.0)
    }

    /**
     * A loyalty activation of a walker that can only spend loyalty, priced as what it buys —
     * [AiProfile.spendFuelWalkers]. Null for anything else, which keeps the ordinary scoring.
     *
     * Such a walker's value *is* its loyalty: with no + ability (printed or granted —
     * [IntentCatalog.loyaltyIsOnlyFuel]) a counter it keeps can only be spent later on the same
     * abilities, so spending it, even its last one, is not a loss. The price is therefore the static
     * gain of [leaf] over [root] with the loyalty already spent — the walker gone when that was
     * its last counter — plus [SELECTION_VALUE] per card the ability surveils or scries, which
     * nothing the evaluator reads would otherwise see. A draw is a card; a surveil is a small,
     * positive selection; a granted −2 that makes a 2/2 is a 2/2. Laid on top of [passScore]
     * rather than the leaf's own score, as [pumpThatBuysTheAttack] does: the difference is a card
     * or less, and a rollout's estimate of two leaves that differ only by it is noise that size.
     *
     * The one reason to hold: an activation that kills the walker while the next opposing attack
     * would be lethal to us is floored below passing — the walker is the only other thing that
     * attack can be pointed at.
     *
     * The ability is matched among the walker's loyalty abilities as it stands
     * ([IntentCatalog.loyaltyAbilities]), so a minus granted by a "Way of the X" enchantment —
     * whose id names the enchantment, not the walker — is a loyalty spend like a printed one.
     */
    private fun fuelWalkerSpend(
        root: GameState,
        action: LegalAction,
        playerId: EntityId,
        passScore: Double,
        leaf: GameState,
    ): AdjustedScore? {
        val activation = action.action as? ActivateAbility ?: return null
        val walkerId = activation.sourceId
        val projected = root.projectedState
        if (!projected.isPlaneswalker(walkerId) || projected.getController(walkerId) != playerId) return null
        val ability = intents.loyaltyAbilities(root, walkerId).find { it.id == activation.abilityId } ?: return null
        if (!intents.loyaltyIsOnlyFuel(root, walkerId)) return null
        val owner = root.turnOrder.firstOrNull { walkerId in root.getBattlefield(it) } ?: return null
        val before = PlaneswalkerThreat.loyalty(root, walkerId)
        val after = if (walkerId in leaf.getBattlefield(owner)) PlaneswalkerThreat.loyalty(leaf, walkerId) else 0
        val spent = before - after
        if (spent <= 0) return null
        // Drawing past the end of the library loses the game (CR 704.5b), which nothing the board
        // evaluator reads shows and the drawn-card floor below would price as a card.
        val drawn = EffectWalker.fold(ability.effect, CardsDrawn)
        if (drawn > root.getLibrary(playerId).size) {
            return AdjustedScore(passScore - 1.0, "draws from an empty library — floored below passing", floored = true)
        }
        val dies = after <= 0
        if (dies && PlaneswalkerThreat.expectedDamage(root, projected, playerId) >= root.lifeTotal(playerId)) {
            return AdjustedScore(
                passScore - 1.0,
                "walker may soak a lethal attack — its last loyalty held, floored below passing",
                floored = true,
            )
        }
        val spentRoot = if (dies) root.removeFromZone(ZoneKey(owner, Zone.BATTLEFIELD), walkerId)
        else root.updateEntity(walkerId) { c ->
            c.with((c.get<CountersComponent>() ?: return@updateEntity c).withRemoved(CounterType.LOYALTY, spent))
        }
        val rooted = evaluator.rootedAt(root, playerId)
        val gain = rooted.evaluate(leaf, leaf.projectedState, playerId) -
            rooted.evaluate(spentRoot, spentRoot.projectedState, playerId)
        // The leaf drew the real top card, which the player cannot know: a land the hand curve
        // prices at nothing was still a random card when the loyalty was spent.
        val total = maxOf(gain, DRAWN_CARD_FLOOR * drawn) + SELECTION_VALUE * EffectWalker.fold(ability.effect, CardsLookedAt)
        return AdjustedScore(passScore + total, "minus-only walker spent for what it buys %+.2f".format(total))
    }

    /** Cards an effect surveils or scries, read off its tree; a branch counts its larger arm. */
    private object CardsLookedAt : EffectWalker.Fold<Int> {
        override fun leaf(effect: Effect): Int = when (effect) {
            is SurveilEffect -> effect.count
            is ScryEffect -> effect.count
            else -> 0
        }
        override fun composite(parts: List<Int>): Int = parts.sum()
        override fun conditional(thenValue: Int, elseValue: Int?): Int = maxOf(thenValue, elseValue ?: 0)
        override fun may(thenValue: Int): Int = thenValue
    }

    /** Cards an effect has its controller draw, read off its tree (fixed counts only). */
    private object CardsDrawn : EffectWalker.Fold<Int> {
        override fun leaf(effect: Effect): Int =
            if (effect is DrawCardsEffect && effect.target == EffectTarget.Controller) {
                (effect.count as? DynamicAmount.Fixed)?.amount ?: 0
            } else 0
        override fun composite(parts: List<Int>): Int = parts.sum()
        override fun conditional(thenValue: Int, elseValue: Int?): Int = minOf(thenValue, elseValue ?: 0)
        override fun may(thenValue: Int): Int = thenValue
    }

    /**
     * A leaf score after per-card adjustment, plus what did the adjusting.
     *
     * The [note] exists for the local testing mode: a candidate the AI passed over despite a strong
     * board score is only explicable if the panel can say *which* policy floored it.
     */
    /** [idleBonus] is the part of [score] that only stands when nothing else beats passing. */
    private data class AdjustedScore(
        val score: Double,
        val note: String? = null,
        val idleBonus: Double = 0.0,
        /** A floor below passing: the action does nothing in this window, whatever follows it. */
        val floored: Boolean = false,
    )

    /**
     * Pick the targets the AI actually commits to for a chosen targeted action, by simulation
     * rather than [TargetSelection]'s static rank. Simulating each candidate resolves the stack —
     * including spells/abilities already on it — so the evaluator scores the *real* board.
     *
     * This is what stops the classic blunder of aiming two "target creature can't block" effects
     * at the same creature: while the first is still on the stack the heuristic sees that creature
     * at full value and re-picks it, but a simulation that resolves both effects shows re-hitting it
     * gains nothing over neutralizing a second, still-able blocker (which [BoardPresence] now prices
     * lower). Requirements are resolved greedily — others held at their heuristic best — and only
     * the top `budget.allowances.targetCandidates` per requirement are simulated to bound cost. A
     * budget below [com.wingedsheep.ai.engine.budget.BudgetTier.NORMAL] skips the refinement and
     * keeps the heuristic pick: this loop is the most expensive thing a routine priority window can
     * pay for. [materialize] buys it back for the one case where the heuristic pick is not merely
     * worse but useless — see [forceTargetRefinement].
     *
     * Deliberately **not** used inside a rollout playout — see [PlayoutPolicy]. Simulating to pick
     * targets inside a simulation is what would make a playout quadratic.
     */
    private fun chooseCommittedTargets(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        budget: DecisionBudget = DecisionBudget.legacy(),
        /**
         * Refine by simulation whatever the budget says, and raise the per-requirement cap to
         * [RESCUE_TARGET_CANDIDATES]. Set only by [materialize], and only for an action the cheap
         * pick already made inert — a tier that skips refinement can also cap candidates at 1,
         * which would leave the rescue with nothing to choose between.
         */
        forceTargetRefinement: Boolean = false,
    ): com.wingedsheep.engine.core.GameAction {
        val baseAction = withAutomaticPayments(state, action)
        if (TargetSelection.targetsAlreadyFilled(baseAction) != false) {
            return withSumGatedExilePayment(state, action, baseAction)
        }
        if (!budget.allowances.refineTargetsBySimulation && !forceTargetRefinement) {
            return heuristicTargets(state, action, playerId)
        }
        if (action.modalEnumeration != null) {
            return chooseModesBySimulation(state, action, baseAction, playerId, budget)
                ?: heuristicTargets(state, action, playerId)
        }
        val targetInfos = TargetSelection.fillableRequirements(action, useMeaningfulFilter)
            ?: return heuristicTargets(state, action, playerId)

        if (targetInfos.any { it.validTargetsByPrefix != null }) {
            return heuristicTargets(state, action, playerId)
        }
        val rankTarget = TargetSelection.ranker(state, action, playerId, intents, targetPolarityFromEffect)
        val slotPolarities =
            if (targetPolarityFromEffect) TargetSelection.polarities(state, action, intents) else emptyList()
        fun worth(index: Int, candidates: List<EntityId>) = TargetSelection.worthTargeting(
            state, targetInfos[index], candidates, playerId, slotPolarities.getOrNull(index),
        )
        // Heuristic baseline for every requirement, then refine each one by simulation.
        val chosenTargets = mutableListOf<com.wingedsheep.engine.state.components.stack.ChosenTarget>()
        val chosenIds = mutableSetOf<EntityId>()
        val chosenTargetIds = mutableListOf<EntityId>()
        for ((index, info) in targetInfos.withIndex()) {
            val available = if (info.mustDifferFromEarlier) {
                info.validTargets.filterNot(chosenIds::contains)
            } else {
                info.validTargets
            }
            // An optional slot left empty by choice: the heuristic path knows how to announce it.
            val worthy = worth(index, available)
            if (worthy.isEmpty()) return heuristicTargets(state, action, playerId)
            val picks = TargetSelection.pick(state, info, worthy, rankTarget)
            if (picks.isEmpty() || picks.size < info.minTargets) return heuristicTargets(state, action, playerId)
            picks.forEach { chosenTargets += TargetSelection.toChosenTarget(state, info, it, playerId) }
            chosenIds += picks
            chosenTargetIds += picks
        }
        // The refinement below swaps target `i` for requirement `i`, which only lines up while every
        // requirement holds exactly one target; a multi-target slot keeps its heuristic picks.
        if (chosenTargets.size != targetInfos.size) {
            return withSumGatedExilePayment(state, action, TargetSelection.applyTargets(baseAction, chosenTargets))
        }

        // Only paid for once a requirement actually has rival targets to simulate — every
        // requirement having at most one candidate is the common case, and digesting the whole
        // position for each affordable candidate to find that out is waste.
        val here by lazy(LazyThreadSafetyMode.NONE) { StateProgress.digest(state) }
        val targetCandidates =
            if (forceTargetRefinement) maxOf(budget.allowances.targetCandidates, RESCUE_TARGET_CANDIDATES)
            else budget.allowances.targetCandidates
        for (i in targetInfos.indices) {
            // A forced refinement ignores the clock: it runs only on the inert path, where the
            // alternative is dropping an ability that may well have had a productive target.
            if (budget.expired() && !forceTargetRefinement) break
            val info = targetInfos[i]
            val priorIds = chosenTargetIds.take(i).toSet()
            val ranked = worth(i, info.validTargets.filterNot { info.mustDifferFromEarlier && it in priorIds })
                .sortedByDescending(rankTarget)
            val candidates = if (targetPolarityFromEffect) {
                TargetSelection.simulationCandidates(state, ranked, targetCandidates, playerId, slotPolarities.getOrNull(i))
            } else {
                ranked.take(targetCandidates)
            }
            if (candidates.size <= 1) continue
            val best = candidates.maxByOrNull { candidate ->
                val trial = chosenTargets.toMutableList()
                trial[i] = TargetSelection.toChosenTarget(state, info, candidate, playerId)
                val result = simulator.simulate(state, TargetSelection.applyTargets(baseAction, trial))
                // A target that resolves back into the position we are standing in is not a target
                // choice, it is a no-op wearing one — Aphetto Alchemist untapping itself. Rank it
                // below every real option, so `chooseAction` only ever drops the whole ability as
                // inert when *no* target does anything. A target whose simulation never finished
                // ranks there too, for the same reason: we cannot say what it does.
                result.scoreOrRankLast { leaf ->
                    if (StateProgress.digest(leaf) == here) {
                        Double.NEGATIVE_INFINITY
                    } else {
                        val direct = evaluator.evaluate(leaf, leaf.projectedState, playerId)
                        if (recastBouncedPermanents) maxOf(direct, bestReplayOfBounced(state, leaf, playerId)) else direct
                    }
                }
            } ?: continue
            chosenTargets[i] = TargetSelection.toChosenTarget(state, info, best, playerId)
            chosenTargetIds[i] = best
        }
        // A lone optional slot ("up to one target creature") is a choice that includes *none*. When
        // the effect's side of the table can't be read, ask the simulation whether the best target
        // beats leaving the slot empty — and only drop it on a strict loss, so a target whose value
        // the evaluator can't see (a "can't block") still gets aimed.
        if (targetPolarityFromEffect && targetInfos.size == 1 && targetInfos[0].minTargets == 0 &&
            slotPolarities.getOrNull(0).let { it == null || it == TargetPolarity.UNKNOWN } &&
            !budget.expired()
        ) {
            val score = { targets: List<com.wingedsheep.engine.state.components.stack.ChosenTarget> ->
                simulator.simulate(state, TargetSelection.applyTargets(baseAction, targets))
                    .scoreOrRankLast { leaf -> evaluator.evaluate(leaf, leaf.projectedState, playerId) }
            }
            val skip = score(emptyList())
            if (skip != Double.NEGATIVE_INFINITY && skip > score(chosenTargets)) {
                return withSumGatedExilePayment(state, action, TargetSelection.applyTargets(baseAction, emptyList()))
            }
        }
        return withSumGatedExilePayment(
            state, action, TargetSelection.applyTargets(baseAction, chosenTargets)
        )
    }

    /**
     * The modes, and their targets, to cast a choose-N modal spell with ("choose two —"): every
     * selection of its available modes ([TargetSelection.modeSelections]) with each mode's targets
     * aimed by that mode's own effect, simulated, the best leaf kept. A mode slot whose side of the
     * table can't be read is tried on both sides. The heuristic fallback takes the first N modes in
     * printed order, which is how Ashling's Command came to make the opponent draw two.
     *
     * Bounded by [MAX_MODAL_SIMULATIONS] and the clock; null when nothing could be simulated, for
     * the caller to fall back on the heuristic pick.
     */
    private fun chooseModesBySimulation(
        state: GameState,
        action: LegalAction,
        baseAction: GameAction,
        playerId: EntityId,
        budget: DecisionBudget,
    ): GameAction? {
        val cast = baseAction as? CastSpell ?: return null
        val modal = action.modalEnumeration ?: return null
        val variantsByMode = mutableMapOf<Int, List<List<com.wingedsheep.engine.state.components.stack.ChosenTarget>>>()
        fun variants(mode: com.wingedsheep.engine.legalactions.ModalEnumerationMode) = variantsByMode.getOrPut(mode.index) {
            TargetSelection.modeTargetVariants(state, cast, mode, playerId, intents, perSide = targetPolarityFromEffect)
        }
        val trials = mutableListOf<CastSpell>()
        for (modes in TargetSelection.modeSelections(modal, MAX_MODAL_SIMULATIONS)) {
            val perMode = modes.map { variants(it) }
            if (perMode.any { it.isEmpty() }) continue
            // Every target combination when it fits, else each mode's heuristic pick only.
            val combinations = perMode.fold(1) { n, v -> n * v.size }
            val lists = if (combinations <= MAX_MODAL_SIMULATIONS) perMode else perMode.map { it.take(1) }
            lists.fold(listOf(emptyList<List<com.wingedsheep.engine.state.components.stack.ChosenTarget>>())) { acc, options ->
                acc.flatMap { prefix -> options.map { prefix + listOf(it) } }
            }.forEach { trials += TargetSelection.castWithModes(state, cast, modal, modes, it) }
        }
        var best: CastSpell? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (trial in trials.take(MAX_MODAL_SIMULATIONS)) {
            if (best != null && budget.expired()) break
            val score = simulator.simulate(state, trial)
                .scoreOrRankLast { leaf -> evaluator.evaluate(leaf, leaf.projectedState, playerId) }
            if (best == null || score > bestScore) {
                best = trial
                bestScore = score
            }
        }
        return best?.let { withSumGatedExilePayment(state, action, it) }
    }

    /** The cheap target pick — one heuristic choice per requirement, no simulation. */
    private fun heuristicTargets(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
    ): com.wingedsheep.engine.core.GameAction = withSumGatedExilePayment(
        state, action,
        TargetSelection.fillHeuristically(
            state, action.copy(action = withAutomaticPayments(state, action)), playerId,
            fillPartialRequirements = useMeaningfulFilter, intents = intents,
            polarityFromEffect = targetPolarityFromEffect,
        ),
    )

    /**
     * Materialize deterministic payment choices carried by [LegalAction.additionalCostInfo].
     *
     * The enumerator exposes a Blight path as a distinct legal action, but the processor can only
     * distinguish it from the alternative-mana path through `AdditionalCostPayment.blightTargets`.
     * Keeping that choice only in UI metadata made the built-in AI submit the wrong branch for both
     * spells and activated abilities. The first candidate is deterministic and already filtered by
     * projected controller/type/counter legality.
     */
    private fun withAutomaticPayments(state: GameState, action: LegalAction): GameAction {
        // Crew N / Saddle N: the enumerator offers the activation with no creatures named, and the
        // handler refuses an empty crew, so these were "illegal once materialized" and silently
        // dropped — the AI never crewed a Vehicle or saddled a Mount (2026-10-31 review, game
        // 556300fa: Surgehacker Mech sat uncrewed for six turns beside a 3/2 and a 1/1).
        when (val raw = action.action) {
            is CrewVehicle -> if (raw.crewCreatures.isEmpty()) {
                return tapForPowerSelection(state, action)?.let { raw.copy(crewCreatures = it) } ?: raw
            }
            is SaddleMount -> if (raw.saddleCreatures.isEmpty()) {
                return tapForPowerSelection(state, action)?.let { raw.copy(saddleCreatures = it) } ?: raw
            }
            else -> Unit
        }
        val gameAction = withAutomaticTapForGeneric(action, withAutomaticConvoke(state, action))
        val info = action.additionalCostInfo ?: return gameAction
        val existing = when (gameAction) {
            is CastSpell -> gameAction.additionalCostPayment
            is ActivateAbility -> gameAction.costPayment
            else -> null
        } ?: AdditionalCostPayment()
        val payerId = when (gameAction) {
            is CastSpell -> gameAction.playerId
            is ActivateAbility -> gameAction.playerId
            else -> null
        }
        val cheapestFirst = SelectionCostPayment.CandidateOrder(state, payerId, intents)
        val xValue = (gameAction as? CastSpell)?.xValue ?: 0
        val primary = SelectionCostPayment.plain(info, existing, cheapestFirst, xValue) ?: when (info.costType) {
            // Teamwork N (CR 702.194a): tap as *few* creatures as will clear the total-power
            // threshold, and among equally-few selections the *smallest* bodies — a board with a
            // 5/5 and a 1/1 paying teamwork 1 should turn the 1/1 sideways and keep the better
            // blocker up. So: the cheapest single creature that clears it on its own if there is
            // one, else greedy biggest-first to keep the count down. Creatures at 0 or negative
            // power never help a sum (and can only drag it down), so they are skipped.
            "TapForTotalPower" -> {
                val required = info.tapForPowerRequired
                val contributors = info.tapForPowerCreatures.filter { it.power > 0 }
                val cheapestSolo = contributors.filter { it.power >= required }.minByOrNull { it.power }
                if (cheapestSolo != null) {
                    existing.copy(variableCostPermanents = listOf(cheapestSolo.entityId))
                } else {
                    val chosen = mutableListOf<EntityId>()
                    var total = 0
                    for (creature in contributors.sortedByDescending { it.power }) {
                        if (total >= required) break
                        chosen += creature.entityId
                        total += creature.power
                    }
                    // Unreachable while the enumerator marks an unpayable teamwork variant
                    // unaffordable, but never submit a declaration we can't pay: fall back to the
                    // undeclared cast rather than an action the handler will reject.
                    if (total < required) {
                        return when (gameAction) {
                            is CastSpell -> gameAction.copy(declaredCostSlot = null)
                            else -> gameAction
                        }
                    }
                    existing.copy(variableCostPermanents = chosen)
                }
            }
            "Craft" -> existing.copy(exiledCards = craftMaterials(state, info))
            else -> return gameAction
        }
        // "Discard a card and sacrifice a creature": every further selection cost is paid too.
        val payment = info.alsoRequired.fold(primary) { acc, extra -> SelectionCostPayment.plain(extra, acc, cheapestFirst, xValue) ?: acc }
        return when (gameAction) {
            is CastSpell -> gameAction.copy(additionalCostPayment = payment)
            is ActivateAbility -> gameAction.copy(costPayment = payment)
            else -> gameAction
        }
    }

    /**
     * The creatures to tap for a Crew N or Saddle N activation: the selection whose total
     * contribution reaches N while giving up the least, or null when none reaches it.
     *
     * A creature is priced at its board value, discounted when it couldn't attack this turn anyway
     * (summoning sick, or barred from attacking) — tapping it gives up only its blocking. So a
     * fresh 2/2 crews before the 3/3 that could swing alongside the Vehicle, and two 1/1s crew a
     * crew-2 Vehicle before a 5/5 does. Contributors at zero power can't help and are skipped.
     * Small boards are searched exhaustively; a wide one is filled cheapest-per-power first and
     * then trimmed of any member the total no longer needs.
     */
    private fun tapForPowerSelection(state: GameState, action: LegalAction): List<EntityId>? {
        val required = action.tapForPowerRequired ?: return null
        val contributors = action.tapForPowerCreatures.orEmpty().filter { it.power > 0 }
        if (contributors.sumOf { it.power } < required) return null
        val projected = state.projectedState
        val costs = contributors.map { creature ->
            val card = state.getEntity(creature.entityId)?.get<CardComponent>()
            val value = card?.let {
                BoardPresence.permanentValue(state, projected, creature.entityId, it, intents)
            } ?: 0.0
            maxOf(0.0, value) * (if (creature.canAttack) 1.0 else IDLE_CREW_DISCOUNT) + CREW_MEMBER_COST
        }
        val n = contributors.size
        if (n <= EXHAUSTIVE_CREW_CANDIDATES) {
            var best = -1
            var bestCost = Double.MAX_VALUE
            for (mask in 1 until (1 shl n)) {
                var power = 0
                var cost = 0.0
                for (i in 0 until n) if (mask and (1 shl i) != 0) {
                    power += contributors[i].power
                    cost += costs[i]
                }
                if (power >= required && cost < bestCost) {
                    best = mask
                    bestCost = cost
                }
            }
            return (0 until n).filter { best and (1 shl it) != 0 }.map { contributors[it].entityId }
        }
        val order = (0 until n).sortedBy { costs[it] / contributors[it].power }
        val chosen = mutableListOf<Int>()
        var total = 0
        for (i in order) {
            if (total >= required) break
            chosen += i
            total += contributors[i].power
        }
        for (i in chosen.sortedByDescending { costs[it] }) {
            if (total - contributors[i].power >= required) {
                chosen -= i
                total -= contributors[i].power
            }
        }
        return chosen.map { contributors[it].entityId }
    }

    /**
     * The cheapest materials for a Craft activation (CR 702.167a): graveyard cards first, then the
     * permanents worth least on the board, [AdditionalCostData.craftMinCount] of them.
     *
     * The engine refuses a Craft activation with no materials named — it never auto-picks, because
     * which artifact goes is the activator's choice — so before this the AI's every Craft was
     * "illegal once materialized" and silently dropped. An LCI deck sat on Inverted Iceberg and
     * Unstable Glyphbridge for twenty-four turns with an empty hand and thirteen lands (2026-10-09
     * logs, game 11 T15–T39).
     *
     * A graveyard card is free to exile; a permanent is board value given up, so the cheapest one
     * by `BoardPresence` goes first — a Gnome token before a creature that is doing work.
     * Exactly the minimum, never more: the only crafts that reward extra materials read their total
     * power or mana value, and spending a body for that is a choice this pick does not try to make.
     * A per-slot craft (Throne of the Grim Captain) may need a matching the cheapest-first order
     * does not find; that one still reaches the processor, is refused, and is dropped as before.
     */
    private fun craftMaterials(
        state: GameState,
        info: com.wingedsheep.engine.legalactions.AdditionalCostData,
    ): List<EntityId> {
        val projected = state.projectedState
        val battlefield = state.getBattlefield().toSet()
        return info.validCraftMaterials
            .filter { state.getEntity(it) != null }
            .sortedBy { id ->
                if (id !in battlefield) return@sortedBy -1.0
                val card = state.getEntity(id)?.get<CardComponent>() ?: return@sortedBy 0.0
                BoardPresence.permanentValue(state, projected, id, card, intents)
            }
            .take(info.craftMinCount)
    }

    /** The entity a chosen target points at, whichever arm of the union it is. */
    private fun targetEntityId(target: ChosenTarget): EntityId = when (target) {
        is ChosenTarget.Player -> target.playerId
        is ChosenTarget.Permanent -> target.entityId
        is ChosenTarget.Card -> target.cardId
        is ChosenTarget.Spell -> target.spellEntityId
    }

    /**
     * Pay a **sum-gated graveyard exile** cast cost — collect evidence N and its filtered sibling —
     * once the targets are known.
     *
     * Every other additional cost is filled by [withAutomaticPayments] before targeting, which is
     * where the AI picks its targets from. This one can't be: Urgent Necropsy's threshold *is* the
     * summed mana value of the targets ("collect evidence X, where X is the total mana value of the
     * permanents this spell targets"), so it isn't determined until CR 601.2f, after the targets are
     * announced at 601.2c. `exileWeightPerTarget` is what the enumerator ships for exactly that, and
     * this runs on the finished action rather than the bare one.
     *
     * Cards are spent highest-mana-value-first, the same choice `CollectEvidenceResolver.autoSelect`
     * makes, so the AI's selection and the engine's fallback can't disagree about what a payment
     * looks like. When the graveyard can't cover what was targeted the *targets* give way, trimmed
     * from the end until the price is affordable (down to none, which prices at 0 and is always
     * payable): per the printed ruling an unreachable threshold means the caster "can't choose to
     * collect evidence at all", so such a cast would simply be rejected — trimming turns a rejected
     * action into a smaller legal one, and never into a worse one, since a target the AI drops was
     * one it could not have kept.
     */
    private fun withSumGatedExilePayment(
        state: GameState,
        action: LegalAction,
        gameAction: GameAction,
    ): GameAction {
        val cast = gameAction as? CastSpell ?: return gameAction
        val info = action.additionalCostInfo ?: return gameAction
        if (info.costType != "CollectEvidence" && info.costType != "ExileForTotal") return gameAction
        if (info.exileCardTypes.isNotEmpty()) return withCardTypeUnionExilePayment(state, info, cast)

        // Most expensive first: the fewest cards that clear the floor.
        val pool = info.validExileTargets
            .filter { state.getEntity(it) != null }
            .sortedByDescending { info.exileCardWeights[it] ?: 0 }
        val available = pool.sumOf { info.exileCardWeights[it] ?: 0 }

        var targets = cast.targets
        var required = info.exileMinTotalWeight + targets.sumOf {
            info.exileWeightPerTarget[targetEntityId(it)] ?: 0
        }
        while (required > available && targets.isNotEmpty()) {
            targets = targets.dropLast(1)
            required = info.exileMinTotalWeight + targets.sumOf {
                info.exileWeightPerTarget[targetEntityId(it)] ?: 0
            }
        }
        if (required > available) return gameAction // nothing left to trim; the engine will refuse

        val chosen = mutableListOf<EntityId>()
        var total = 0
        for (cardId in pool) {
            if (total >= required) break
            chosen += cardId
            total += info.exileCardWeights[cardId] ?: 0
        }
        return cast.copy(
            targets = targets,
            additionalCostPayment = (cast.additionalCostPayment ?: AdditionalCostPayment())
                .copy(exiledCards = chosen),
        )
    }

    /**
     * The union-measured form of [withSumGatedExilePayment] — "with four or more card types among
     * them" (Nethergoyf's escape). Delegates to the engine's own greedy cover so the AI and the
     * fallback pick can't drift.
     */
    private fun withCardTypeUnionExilePayment(
        state: GameState,
        info: com.wingedsheep.engine.legalactions.AdditionalCostData,
        cast: CastSpell,
    ): GameAction {
        val pool = info.validExileTargets.filter { state.getEntity(it) != null }
        val candidates = GraveyardTotalExileResolver.Candidates(
            cards = pool,
            weightById = info.exileCardWeights,
            typesById = pool.associateWith { info.exileCardTypes[it].orEmpty().toSet() },
        )
        val chosen = GraveyardTotalExileResolver.autoSelect(candidates, info.exileMinTotalWeight)
        if (chosen.isEmpty()) return cast // unreachable; the engine will refuse
        return cast.copy(
            additionalCostPayment = (cast.additionalCostPayment ?: AdditionalCostPayment())
                .copy(exiledCards = chosen),
        )
    }

    /**
     * Turn the Convoke candidates advertised by the legal-action enumerator into the payment the
     * cast handler consumes. Colored pips are satisfied first; remaining creatures pay only the
     * generic part of the cost, so the AI never submits an invalid overpayment.
     */
    private fun withAutomaticConvoke(state: GameState, action: LegalAction): GameAction {
        val cast = action.action as? CastSpell ?: return action.action
        val offered = action.convokeCreatures.orEmpty()
        val costString = action.manaCostString
        if (!action.hasConvoke || offered.isEmpty() || costString == null) return cast
        // Under [convokeLandsFirst] the cheapest bodies are first in line, so the greedy pass below
        // spends them before the creatures we would rather keep untapped.
        val creatures = if (convokeLandsFirst) {
            offered.sortedBy { CombatMath.creatureValue(state, state.projectedState, it.entityId) }
        } else {
            offered
        }

        val cost = ManaCost.parse(costString)
        val coloredNeeded = cost.symbols
            .filterIsInstance<ManaSymbol.Colored>()
            .groupingBy { it.color }
            .eachCount()
            .toMutableMap()
        var genericNeeded = cost.genericAmount
        val payments = linkedMapOf<EntityId, ConvokePayment>()
        val unused = creatures.toMutableList()

        for ((color, count) in coloredNeeded) {
            repeat(count) {
                val index = unused.indexOfFirst { color in it.colors }
                if (index >= 0) {
                    val creature = unused.removeAt(index)
                    payments[creature.entityId] = ConvokePayment(color)
                }
            }
        }
        while (genericNeeded > 0 && unused.isNotEmpty()) {
            val creature = unused.removeAt(0)
            payments[creature.entityId] = ConvokePayment()
            genericNeeded--
        }
        if (convokeLandsFirst) releaseConvokeForMana(state, cast.playerId, cost, payments)
        if (payments.isEmpty()) return cast

        val existing = cast.alternativePayment ?: AlternativePaymentChoice.NONE
        return cast.copy(
            alternativePayment = existing.copy(
                convokedCreatures = existing.convokedCreatures + payments
            )
        )
    }

    private val manaSolver: ManaSolver by lazy {
        ManaSolver(simulator.cardRegistry, PredicateEvaluator(simulator.cardRegistry))
    }

    /**
     * [convokeLandsFirst]: untap every convoking creature whose pip the rest of our mana can pay
     * instead, most valuable first. A creature tapped for convoke is a creature that does not
     * attack or block; a land tapped for the same pip costs nothing else on most turns. What is
     * left in [payments] is the shortfall mana genuinely cannot cover. Mutates [payments].
     */
    private fun releaseConvokeForMana(
        state: GameState,
        playerId: EntityId,
        cost: ManaCost,
        payments: LinkedHashMap<EntityId, ConvokePayment>,
    ) {
        val projected = state.projectedState
        val mostValuableFirst = payments.keys.sortedByDescending { CombatMath.creatureValue(state, projected, it) }
        for (creature in mostValuableFirst) {
            val without = LinkedHashMap(payments).apply { remove(creature) }
            if (manaSolver.canPay(state, playerId, costAfterConvoke(cost, without.values), excludeSources = without.keys)) {
                payments.remove(creature)
            }
        }
    }

    /** [cost] with each convoke payment's pip taken off: its color, or one generic. */
    private fun costAfterConvoke(cost: ManaCost, payments: Collection<ConvokePayment>): ManaCost {
        val symbols = cost.symbols.toMutableList()
        var generic = 0
        for (payment in payments) {
            val color = payment.color
            if (color == null) generic++ else symbols.remove(ManaSymbol.Colored(color))
        }
        return ManaCost(symbols).reduceGeneric(generic)
    }

    /**
     * Fill in a tap-for-generic payment (improvise CR 702.126, waterbend) the enumerator offered.
     *
     * The enumerator counts these taps toward affordability, so a cast that is only payable *with*
     * them is offered as affordable; submitting it with an empty payment would then be rejected at
     * the mana step. Filling it here keeps the AI's chosen action payable.
     *
     * Deliberately **artifacts only**, even though a waterbend cost also accepts creatures: an
     * artifact is rarely doing anything else this turn, whereas tapping a creature silently gives
     * up an attack or a block. That covers improvise exactly (CR 702.126a is artifacts-only) and
     * leaves waterbend no worse off than before.
     *
     * And only when the taps are actually *needed*. An improvise tap is optional, and filling an
     * optional one can lose the cast: a tapped artifact stops being a mana source but credits only
     * {1}, so tapping Arc Reactor ({T}: Add {C}{C}{C}) for improvise on a board of three lands
     * turns a payable {5} into an unpayable {4} — the AI's own action then hard-errors at the mana
     * step. `tapForGenericRequired == false` says mana alone covers it, so we leave the artifacts
     * up; `true` means the enumerator's affordability check already validated tapping *all* of
     * them, so filling to the cap is safe. Null is the waterbend paths, whose taps pay a cost that
     * is owed either way — unchanged behaviour there.
     */
    private fun withAutomaticTapForGeneric(action: LegalAction, gameAction: GameAction): GameAction {
        if (!action.hasTapForGeneric) return gameAction
        if (action.tapForGenericRequired == false) return gameAction
        val cast = gameAction as? CastSpell ?: return gameAction
        val artifacts = action.tapForGenericPermanents.orEmpty().filterNot { it.isCreature }
        if (artifacts.isEmpty()) return gameAction
        val costString = action.manaCostString ?: return gameAction

        // One tap per generic mana, and never more than the mechanic's own cap (waterbend's {N}).
        val genericInCost = ManaCost.parse(costString).genericAmount
        val cap = minOf(action.tapForGenericAmount ?: genericInCost, genericInCost)
        if (cap <= 0) return gameAction

        val existing = cast.alternativePayment ?: AlternativePaymentChoice.NONE
        return cast.copy(
            alternativePayment = existing.copy(
                tapForGenericPermanents = artifacts.take(cap).mapTo(linkedSetOf()) { it.entityId }
            )
        )
    }

    /**
     * When both a normal cast and a kicker/offspring variant of the same card are
     * affordable, drop the normal variant. The kicker variant is strictly better —
     * it does everything the normal cast does plus the kicker bonus (e.g., offspring
     * creates an additional token). Keeping both inflates the candidate list and
     * triggers unnecessary deep search (the two variants score close together,
     * tripping the "close call" heuristic).
     */
    private fun preferKickerVariants(actions: List<LegalAction>): List<LegalAction> {
        // "Strictly better" holds for the kicker's *bonus*, not for what the bonus is aimed at.
        // Tolarian Emissary's kicked ETB destroys target enchantment, and with only our own Aura
        // on the table the kicked cast blows it up — a target that is not chosen until the trigger
        // goes on the stack, so nothing at cast time can see it. Keeping the plain cast lets the
        // simulation compare the two; it costs one extra simulation per kicker card in hand.
        if (targetPolarityFromEffect) return actions
        // Collect cardIds that have an affordable CastWithKicker variant
        val kickedCardIds = mutableSetOf<EntityId>()
        for (action in actions) {
            if (action.actionType == "CastWithKicker") {
                val castSpell = action.action as? CastSpell ?: continue
                kickedCardIds.add(castSpell.cardId)
            }
        }
        if (kickedCardIds.isEmpty()) return actions

        // Remove the normal CastSpell variant for those cards
        return actions.filter { action ->
            if (action.actionType == "CastSpell") {
                val castSpell = action.action as? CastSpell ?: return@filter true
                castSpell.cardId !in kickedCardIds
            } else {
                true
            }
        }
    }

    /**
     * Expand an affordable X-cost action into one candidate [LegalAction] per concrete X value, so
     * the normal simulation-based scoring picks the best X instead of defaulting it away.
     *
     * Both halves matter, for different reasons:
     *
     * - **Activated abilities.** Submitting the enumerator's bare action runs it at `xValue = 0` —
     *   the Momir avatar would only ever look for a mana-value-0 creature and make nothing, so the
     *   AI would always pass it over. Only no-target abilities are expanded here; a targeted one
     *   keeps the engine's own choose-X decision path (see [bindsXWithoutTheEnginesHelp]). With no
     *   targets there is nothing to narrow, so the X values *are* the candidates, capped directly.
     * - **Spells.** There is no such decision path on the cast side: `CastSpell.xValue` left null is
     *   bound to 0 as the spell goes on the stack (CR 601.2b), so an un-expanded Day of Black Sun or
     *   Genesis Wave is cast for X=0 and does nothing. Targeted spells are expanded too, with their
     *   target lists narrowed to the chosen X — see [XCostSelection], which owns both the choice of
     *   which X values are worth a simulation and the narrowing that keeps the resulting action
     *   legal.
     *
     * An action with an additional cost we don't know how to pay passes through untouched. The
     * Momir avatar waits while a land drop is on offer — see [momirActivations].
     */
    private fun expandXCostAbilities(
        state: GameState,
        actions: List<LegalAction>,
        playerId: EntityId
    ): List<LegalAction> {
        val landDrops = actions.filter { it.action is PlayLand && it.affordable }
        // Simulated only when a Momir activation asks, and only under [libraryClock].
        val landDropAvailable = lazy(LazyThreadSafetyMode.NONE) {
            landDrops.any { !libraryClock || !landDropDrainsLibrary(state, it.action, playerId) }
        }
        return actions.flatMap { action -> expandXCost(state, action, playerId, landDropAvailable) }
    }

    /**
     * Whether playing [land] leaves our library closer to empty by a margin the evaluator's
     * `LibraryClock` prices — a landfall trigger that exiles or mills the library (Territorial
     * Bruntar over a Momir library of basics exiles all of it). A land the processor refuses
     * counts as draining: it is no land drop to wait for.
     */
    private fun landDropDrainsLibrary(state: GameState, land: GameAction, playerId: EntityId): Boolean {
        val result = simulator.simulate(state, land)
        if (result is SimulationResult.Illegal) return true
        val before = LibraryClock.libraryValue(state.getLibrary(playerId).size)
        return LibraryClock.libraryValue(result.state.getLibrary(playerId).size) < before
    }

    private fun expandXCost(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        landDropAvailable: Lazy<Boolean>,
    ): List<LegalAction> {
        val base = action.action
        val maxX = action.maxAffordableX
        val info = action.additionalCostInfo
        // "Sacrifice X creatures" (Eliminate the Competition) is paid once X is bound — the
        // cheapest X permanents, in [SelectionCostPayment.plain].
        val payableCost = info == null || info.costType == "DiscardCard" ||
            (info.costType == "SacrificeVariable" && info.sacrificeCountIsX)
        // The Momir avatar is expanded before the generic affordability guard below, because for it
        // "not worth activating" must mean *dropped*, not "fall through to the bare action". The
        // bare action carries `xValue = 0`, and with no mana available (`maxX == 0`) that is exactly
        // what the guard would let through — spending the once-each-turn activation and a card to
        // look for a mana-value-0 creature, a bucket that holds no castable card at all.
        if (isMomirAvatarActivation(state, action)) {
            return momirActivations(state, action, playerId, landDropAvailable)
        }
        if (!action.hasXCost || maxX == null || maxX < 1 || !payableCost) {
            return listOf(action)
        }
        if (!bindsXWithoutTheEnginesHelp(action)) return listOf(action)
        return when (base) {
            is ActivateAbility -> {
                val discard = chooseActivationDiscard(state, action, playerId)
                if (info?.costType == "DiscardCard" && info.discardCount > 0 && discard == null) {
                    return emptyList()
                }
                // A no-target ability has nothing to narrow, so the X values are all there is.
                val xCandidates =
                    XCostSelection.candidateXValues(state, action).take(XCostSelection.MAX_X_CANDIDATES)
                xCandidates.map { x ->
                    action.copy(action = base.copy(xValue = x, costPayment = discard ?: base.costPayment))
                }
            }
            // An empty expansion means the spell is uncastable to any purpose right now (every
            // affordable X leaves a mandatory target slot empty). Dropping it beats offering the
            // bare action, which would be submitted at X=0 and fizzle.
            is CastSpell -> XCostSelection.expandToX(state, action)
            else -> listOf(action)
        }
    }

    /**
     * Whether the AI has to choose this action's X itself.
     *
     * A targeted activated ability is the one shape that must *not* be pre-bound: submitted bare it
     * reaches the engine's own choose-X pause, which [DecisionResponder] answers by simulating each
     * value — strictly better than anything decided here, and it has to pick targets in the same
     * breath anyway. Everything else defaults to `xValue = 0` if the AI stays quiet
     * (`CastSpell.xValue ?: 0`), so staying quiet is not an option.
     */
    private fun bindsXWithoutTheEnginesHelp(action: LegalAction): Boolean =
        !(action.action is ActivateAbility && action.requiresTargets)

    /**
     * Every activation of the Momir avatar worth simulating this turn — possibly none.
     *
     * Returning an empty list is the point: unlike the generic X expansion, "the strategy doesn't
     * want to activate" must not fall back to the enumerator's bare action, whose `xValue` is 0.
     * X=0 is never a candidate here, so the AI can't spend its once-each-turn activation and a card
     * on a mana value with nothing castable in it.
     *
     * Also empty while [landDropAvailable] and a card would still be left to discard after the land:
     * the land is one more point of X for the same card, so it goes first. Under [libraryClock] a
     * land drop that would drain the library is not one to wait for — see [landDropDrainsLibrary].
     */
    private fun momirActivations(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
        landDropAvailable: Lazy<Boolean>,
    ): List<LegalAction> {
        val base = action.action as? ActivateAbility ?: return listOf(action)
        val info = action.additionalCostInfo
        // Shapes this strategy doesn't model (no {X}, or an additional cost we can't pay) fall back
        // to the untouched action rather than being silently dropped.
        if (!action.hasXCost || !(info == null || info.costType == "DiscardCard")) {
            return listOf(action)
        }
        val discard = chooseActivationDiscard(state, action, playerId)
        // No card to discard ⇒ the cost can't be paid at any X.
        if (info?.costType == "DiscardCard" && info.discardCount > 0 && discard == null) {
            return emptyList()
        }
        // Land drop first: the land is one more point of X for the same card. Only while another
        // card would be left to discard — playing the last card as a land forfeits the activation.
        if ((info?.validDiscardTargets?.size ?: 0) > (info?.discardCount ?: 0) && landDropAvailable.value) {
            return emptyList()
        }
        return momirXCandidates(state, action.maxAffordableX ?: 0, playerId).map { x ->
            action.copy(action = base.copy(xValue = x, costPayment = discard ?: base.costPayment))
        }
    }

    /**
     * Momir Basic is a resource-management format: the hand is nothing but lands, every activation
     * eats one, and the deck draws one card a turn — so the cheap early flips are the ones to skip
     * in order to still hold a card when the mana is there for a real threat.
     *
     * The published guidance agrees on both halves of that:
     *
     * - **Skip the first drops.** Start activating at four mana on the play, three on the draw
     *   (MTG Arena Zone's Momir guide; the Star City Games primer says the same in turn counts —
     *   the player on the play skips turns 1–3, the player on the draw skips turns 1–2).
     * - **Then top out around eight.** Mana values six through nine are the strong band, with seven
     *   and eight the recommended stopping points; nine gets swingy and ten-plus is only worth the
     *   extra turns of ramp if card draw showed up. So once eight is affordable, make an eight
     *   rather than pouring the surplus into a bigger, noisier flip.
     *
     * Below the target the whole available pool is used, which is just curving out.
     *
     * Sources: <https://mtgazone.com/momir-basic-midweek-magic-event-guide/>,
     * <https://articles.starcitygames.com/articles/the-momir-basic-primer/>.
     */
    private fun momirXCandidates(state: GameState, maxX: Int, playerId: EntityId): List<Int> {
        val wentFirst = state.turnOrder.firstOrNull() == playerId
        val firstStrategicX = if (wentFirst) MOMIR_FIRST_X_ON_THE_PLAY else MOMIR_FIRST_X_ON_THE_DRAW
        if (maxX < firstStrategicX) return emptyList()
        if (maxX >= MOMIR_TARGET_X) return listOf(MOMIR_TARGET_X)
        return listOf(maxX)
    }

    private fun isMomirAvatarActivation(state: GameState, action: LegalAction): Boolean {
        if (state.format !is Format.MomirBasic) return false
        val activation = action.action as? ActivateAbility ?: return false
        val card = state.getEntity(activation.sourceId)?.get<CardComponent>() ?: return false
        return card.name == MOMIR_AVATAR_NAME
    }

    /**
     * Choose which card(s) to discard for an activated ability whose additional cost is "discard a
     * card". Prefers a land as fodder (the safest discard, and in Momir Basic the whole hand is
     * basics); falls back to the first available card. Returns null when there is no discard cost.
     */
    private fun chooseActivationDiscard(
        state: GameState,
        action: LegalAction,
        playerId: EntityId
    ): com.wingedsheep.sdk.scripting.AdditionalCostPayment? {
        val info = action.additionalCostInfo ?: return null
        if (info.costType != "DiscardCard" || info.discardCount <= 0) return null
        val candidates = info.validDiscardTargets
        if (candidates.isEmpty()) return null
        val ranked = candidates.sortedByDescending { id ->
            if (state.getEntity(id)?.get<CardComponent>()?.isLand == true) 1 else 0
        }
        return com.wingedsheep.sdk.scripting.AdditionalCostPayment(
            discardedCards = ranked.take(info.discardCount)
        )
    }

    /** Resolve the card name from a legal action's underlying GameAction. */
    private fun resolveCardName(state: GameState, action: LegalAction): String? {
        val entityId = when (val gameAction = action.action) {
            is CastSpell -> gameAction.cardId
            is ActivateAbility -> gameAction.sourceId
            else -> return null
        }
        return state.getEntity(entityId)?.get<CardComponent>()?.name
    }

    private companion object {
        /** How many acted-from positions [remember] keeps. See it for why a short memory suffices. */
        const val POSITION_MEMORY = 32

        /**
         * How far a candidate must clear passing to be taken — rounding noise, not a margin. Leaf
         * scores are sums of a few dozen terms of order 1–100, so real differences are orders of
         * magnitude above this and floating-point summation-order noise orders of magnitude below.
         */
        const val SCORE_EPSILON = 1e-9

        /**
         * What looking at one card with a surveil or scry is worth to [fuelWalkerSpend], in evaluator
         * units — the selection the board score cannot see. Small on purpose: positive, so a −1
         * surveil beats letting the loyalty sit, and below the least a drawn card is worth (a land
         * in hand when mana-rich is 0.2 on `CardAdvantage`'s curve, a spell 0.8 and up), so a −3
         * draw outbids it whenever both are on offer.
         */
        const val SELECTION_VALUE = 0.15

        /**
         * The least a card drawn by [fuelWalkerSpend]'s activation counts for: twice a look, so a
         * draw outbids a surveil even when the card the leaf happened to draw — the real top card,
         * which the player cannot know — is one the hand curve prices at nothing.
         */
        const val DRAWN_CARD_FLOOR = 0.3

        /**
         * Targets simulated per requirement when rescuing a candidate the cheap pick made inert.
         * A tier below [com.wingedsheep.ai.engine.budget.BudgetTier.NORMAL] can cap candidates at
         * 1, so the rescue needs its own floor; this is the legacy cap.
         */
        const val RESCUE_TARGET_CANDIDATES = 8

        /**
         * Most mode-and-target combinations [chooseModesBySimulation] simulates for one choose-N
         * modal spell: choose two of four is six selections, each with a few target lists.
         */
        const val MAX_MODAL_SIMULATIONS = 24

        /**
         * Crew/Saddle payment ([tapForPowerSelection]): a creature that couldn't attack this turn
         * costs this fraction of its board value to tap; every member also costs a flat
         * [CREW_MEMBER_COST] so equal-value selections prefer fewer bodies.
         */
        const val IDLE_CREW_DISCOUNT = 0.5
        const val CREW_MEMBER_COST = 0.01

        /** Up to this many crew candidates, every subset is tried (2^n); beyond it, greedy. */
        const val EXHAUSTIVE_CREW_CANDIDATES = 12

        /** Where the avatar tops out — the recommended stopping point. See [momirXCandidates]. */
        const val MOMIR_TARGET_X = 8

        /** First activation on the play: turns 1–3 are skipped, so the first flip is a four. */
        const val MOMIR_FIRST_X_ON_THE_PLAY = 4

        /** First activation on the draw — one turn earlier, since the extra card pays for it. */
        const val MOMIR_FIRST_X_ON_THE_DRAW = 3

        const val MOMIR_AVATAR_NAME = "Momir Vig, Simic Visionary"
    }
}
