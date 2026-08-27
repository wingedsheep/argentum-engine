package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.advisor.CardAdvisorRegistry
import com.wingedsheep.ai.engine.advisor.CastContext
import com.wingedsheep.ai.engine.budget.BudgetPolicy
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.budget.LegacyBudgetPolicy
import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.ai.engine.evaluation.EvaluationWeights
import com.wingedsheep.ai.engine.knowledge.HoldPolicy
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
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
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.MeaningfulActionFilter
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import com.wingedsheep.sdk.scripting.ConvokePayment

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
        boardPresenceWeight = boardPresenceWeight,
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
        val affordable = expandXCostAbilities(state, preferKickerVariants(candidatesFrom(legalActions)), playerId)

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
        if (pass != null) {
            leaves += pass
            leafStates += simulator.simulate(evaluationState, pass.action).state
        }
        for (action in affordable) {
            searched++
            val (materialized, simulation) = materialize(evaluationState, action, playerId, budget, here)
            val usable = when {
                // LegalAction affordability is necessarily a preview for costs such as convoke and
                // modal/additional payments. If materializing the concrete action cannot pass the
                // authoritative processor, it is not a candidate the AI may submit.
                simulation is SimulationResult.Illegal -> false
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
                leaves += action.copy(action = materialized)
                leafStates += simulation.state
            } else {
                val illegal = simulation is SimulationResult.Illegal
                dropped?.add(
                    droppedOption(
                        evaluationState, action, materialized,
                        note = if (illegal) {
                            "dropped — illegal once materialized"
                        } else {
                            "dropped — leads back to a position already acted from"
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

        // ── Pass 2: score every leaf at once ──
        val leafScores = candidateEvaluator.scoreAll(evaluationState, leafStates, playerId, budget)
        val passScore = if (pass != null) {
            leafScores.first()
        } else {
            // No pass on offer: the "do nothing" reference is the current position itself.
            candidateEvaluator.score(evaluationState, evaluationState, playerId, budget)
        }

        // ── Pass 3: per-card timing and advisor adjustments, in raw evaluator units ──
        val firstCandidate = if (pass != null) 1 else 0
        val adjusted = (firstCandidate until leaves.size).map { i ->
            Triple(leaves[i], leafScores[i], adjustScore(evaluationState, leaves[i], playerId, leafScores[i], passScore))
        }
        val scored = adjusted.map { (action, _, adjustment) -> action to adjustment.score }

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

        val best = scored.maxByOrNull { it.second }
        val takeAction = best != null && best.second > adjustedPassScore
        val chosen = if (takeAction) {
            remember(here)
            // Fill in targets on the returned action so the processor can execute it.
            // The committed target is chosen by simulation (not just the heuristic) so the
            // AI sees the real resolved board, including effects already on the stack.
            best.first
        } else {
            pass ?: legalActions.first()
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
        if (simulation is SimulationResult.Illegal || simulation is SimulationResult.NeedsDecision) {
            return materialized to simulation
        }
        if (StateProgress.digest(simulation.state) != here) return materialized to simulation

        val refined = chooseCommittedTargets(state, action, playerId, budget, forceTargetRefinement = true)
        if (refined == materialized) return materialized to simulation
        return refined to simulator.simulate(state, refined)
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
        leafScore: Double,
        passScore: Double,
    ): AdjustedScore {
        val cardName = resolveCardName(state, action) ?: return AdjustedScore(leafScore)

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
        )
        if (timing is TimingVerdict.NoWindow) {
            // The card does nothing here, so nothing the simulation reports should make it beat
            // passing. See [TimingVerdict.NoWindow] for why this is a floor and not a penalty.
            return AdjustedScore(passScore - 1.0, "hold policy: wrong window — floored below passing")
        }
        val timingDelta = (timing as? TimingVerdict.Adjust)?.delta ?: 0.0
        val timingReason = (timing as? TimingVerdict.Adjust)?.reason ?: "timing"
        val timingNote =
            if (timingDelta != 0.0) "hold policy $timingReason %+.2f".format(timingDelta) else null

        // Check for card-specific advisor override. Timing is applied outside it, so a per-card
        // advisor still sees the pure board score as its `defaultScore` and a card with both
        // keeps both.
        val advisor = advisorRegistry.getAdvisor(cardName)
            ?: return AdjustedScore(leafScore + timingDelta, timingNote)
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
        )
    }

    /**
     * A leaf score after per-card adjustment, plus what did the adjusting.
     *
     * The [note] exists for the local testing mode: a candidate the AI passed over despite a strong
     * board score is only explicable if the panel can say *which* policy floored it.
     */
    private data class AdjustedScore(val score: Double, val note: String? = null)

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
        val baseAction = withAutomaticPayments(action)
        if (TargetSelection.targetsAlreadyFilled(baseAction) != false) {
            return withSumGatedExilePayment(state, action, baseAction)
        }
        if (!budget.allowances.refineTargetsBySimulation && !forceTargetRefinement) {
            return heuristicTargets(state, action, playerId)
        }
        val targetInfos = TargetSelection.fillableRequirements(action, useMeaningfulFilter)
            ?: return heuristicTargets(state, action, playerId)

        // Heuristic baseline for every requirement, then refine each one by simulation.
        val chosenTargets = mutableListOf<com.wingedsheep.engine.state.components.stack.ChosenTarget>()
        val chosenIds = mutableSetOf<EntityId>()
        val chosenTargetIds = mutableListOf<EntityId>()
        for (info in targetInfos) {
            val available = if (info.mustDifferFromEarlier) {
                info.validTargets.filterNot(chosenIds::contains)
            } else {
                info.validTargets
            }
            val selectedId = available.maxByOrNull { TargetSelection.rank(state, it, playerId, intents) }
                ?: return heuristicTargets(state, action, playerId)
            chosenTargets += TargetSelection.toChosenTarget(state, info, selectedId, playerId)
            chosenIds += selectedId
            chosenTargetIds += selectedId
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
            val candidates = info.validTargets
                .filterNot { info.mustDifferFromEarlier && it in priorIds }
                .sortedByDescending { TargetSelection.rank(state, it, playerId, intents) }
                .take(targetCandidates)
            if (candidates.size <= 1) continue
            val best = candidates.maxByOrNull { candidate ->
                val trial = chosenTargets.toMutableList()
                trial[i] = TargetSelection.toChosenTarget(state, info, candidate, playerId)
                val result = simulator.simulate(state, TargetSelection.applyTargets(baseAction, trial))
                // A target that resolves back into the position we are standing in is not a target
                // choice, it is a no-op wearing one — Aphetto Alchemist untapping itself. Rank it
                // below every real option, so `chooseAction` only ever drops the whole ability as
                // inert when *no* target does anything.
                if (StateProgress.digest(result.state) == here) {
                    Double.NEGATIVE_INFINITY
                } else {
                    evaluator.evaluate(result.state, result.state.projectedState, playerId)
                }
            } ?: continue
            chosenTargets[i] = TargetSelection.toChosenTarget(state, info, best, playerId)
            chosenTargetIds[i] = best
        }
        return withSumGatedExilePayment(
            state, action, TargetSelection.applyTargets(baseAction, chosenTargets)
        )
    }

    /** The cheap target pick — one heuristic choice per requirement, no simulation. */
    private fun heuristicTargets(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
    ): com.wingedsheep.engine.core.GameAction = withSumGatedExilePayment(
        state, action,
        TargetSelection.fillHeuristically(
            state, action.copy(action = withAutomaticPayments(action)), playerId,
            fillPartialRequirements = useMeaningfulFilter, intents = intents
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
    private fun withAutomaticPayments(action: LegalAction): GameAction {
        val gameAction = withAutomaticTapForGeneric(action, withAutomaticConvoke(action))
        val info = action.additionalCostInfo ?: return gameAction
        val existing = when (gameAction) {
            is CastSpell -> gameAction.additionalCostPayment
            is ActivateAbility -> gameAction.costPayment
            else -> null
        } ?: AdditionalCostPayment()
        val payment = when (info.costType) {
            "Blight" -> existing.copy(blightTargets = info.validBlightTargets.take(1))
            "Behold" -> existing.copy(beheldCards = info.validBeholdTargets.take(info.beholdCount))
            "TapPermanents" -> existing.copy(tappedPermanents = info.validTapTargets.take(info.tapCount))
            "DiscardCard" -> existing.copy(discardedCards = info.validDiscardTargets.take(info.discardCount))
            "SacrificePermanent" -> existing.copy(
                sacrificedPermanents = info.validSacrificeTargets.take(info.sacrificeCount)
            )
            "BouncePermanent" -> existing.copy(bouncedPermanents = info.validBounceTargets.take(info.bounceCount))
            "ExileFromGraveyard" -> existing.copy(exiledCards = info.validExileTargets.take(info.exileMinCount))
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
            else -> return gameAction
        }
        return when (gameAction) {
            is CastSpell -> gameAction.copy(additionalCostPayment = payment)
            is ActivateAbility -> gameAction.copy(costPayment = payment)
            else -> gameAction
        }
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
     * Turn the Convoke candidates advertised by the legal-action enumerator into the payment the
     * cast handler consumes. Colored pips are satisfied first; remaining creatures pay only the
     * generic part of the cost, so the AI never submits an invalid overpayment.
     */
    private fun withAutomaticConvoke(action: LegalAction): GameAction {
        val cast = action.action as? CastSpell ?: return action.action
        val creatures = action.convokeCreatures.orEmpty()
        val costString = action.manaCostString
        if (!action.hasConvoke || creatures.isEmpty() || costString == null) return cast

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
        if (payments.isEmpty()) return cast

        val existing = cast.alternativePayment ?: AlternativePaymentChoice.NONE
        return cast.copy(
            alternativePayment = existing.copy(
                convokedCreatures = existing.convokedCreatures + payments
            )
        )
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
     * An action with an additional cost we don't know how to pay passes through untouched.
     */
    private fun expandXCostAbilities(
        state: GameState,
        actions: List<LegalAction>,
        playerId: EntityId
    ): List<LegalAction> = actions.flatMap { action ->
        val base = action.action
        val maxX = action.maxAffordableX
        val info = action.additionalCostInfo
        val payableCost = info == null || info.costType == "DiscardCard"
        // The Momir avatar is expanded before the generic affordability guard below, because for it
        // "not worth activating" must mean *dropped*, not "fall through to the bare action". The
        // bare action carries `xValue = 0`, and with no mana available (`maxX == 0`) that is exactly
        // what the guard would let through — spending the once-each-turn activation and a card to
        // look for a mana-value-0 creature, a bucket that holds no castable card at all.
        if (isMomirAvatarActivation(state, action)) {
            return@flatMap momirActivations(state, action, playerId)
        }
        if (!action.hasXCost || maxX == null || maxX < 1 || !payableCost) {
            return@flatMap listOf(action)
        }
        if (!bindsXWithoutTheEnginesHelp(action)) return@flatMap listOf(action)
        when (base) {
            is ActivateAbility -> {
                val discard = chooseActivationDiscard(state, action, playerId)
                if (info?.costType == "DiscardCard" && info.discardCount > 0 && discard == null) {
                    return@flatMap emptyList()
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
     */
    private fun momirActivations(
        state: GameState,
        action: LegalAction,
        playerId: EntityId,
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
         * Targets simulated per requirement when rescuing a candidate the cheap pick made inert.
         * A tier below [com.wingedsheep.ai.engine.budget.BudgetTier.NORMAL] can cap candidates at
         * 1, so the rescue needs its own floor; this is the legacy cap.
         */
        const val RESCUE_TARGET_CANDIDATES = 8

        /** Where the avatar tops out — the recommended stopping point. See [momirXCandidates]. */
        const val MOMIR_TARGET_X = 8

        /** First activation on the play: turns 1–3 are skipped, so the first flip is a four. */
        const val MOMIR_FIRST_X_ON_THE_PLAY = 4

        /** First activation on the draw — one turn earlier, since the extra card pays for it. */
        const val MOMIR_FIRST_X_ON_THE_DRAW = 3

        const val MOMIR_AVATAR_NAME = "Momir Vig, Simic Visionary"
    }
}
