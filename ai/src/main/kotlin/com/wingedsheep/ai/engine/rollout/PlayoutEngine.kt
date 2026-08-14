package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.ai.engine.evaluation.BoardEvaluator
import com.wingedsheep.engine.core.ActionProcessor
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import kotlin.math.abs

/**
 * A source of playouts: "play this state forward with this seed, tell me the win probability".
 *
 * The seam exists because [RolloutCandidateEvaluator] has two jobs that fail for different reasons —
 * *allocating* a budget across candidates and *running* a game forward — and only the second needs
 * an engine. Separating them lets the allocation logic (sequential halving, the common-random-number
 * grid, the tier gating) be asserted against a recording stub rather than inferred from win rates.
 */
fun interface Playouts {
    /**
     * @param baseline the raw evaluator score of the *decision's* root position. A playout that
     *   ends on a live board reports how far it moved from there, not where it landed — see
     *   [PlayoutEngine.run] for why the absolute score is unusable.
     */
    fun run(
        start: GameState,
        playerId: EntityId,
        seed: Long,
        horizonPlayerTurns: Int,
        baseline: Double,
    ): Double
}

/**
 * Plays one short, cheap game forward from a candidate's post-action state and reports how it went.
 *
 * The unit of work Phase 7 is built on: `RolloutCandidateEvaluator` runs several of these per
 * candidate and averages the results, replacing the single static evaluation the AI used to score a
 * leaf with.
 *
 * **It owns its own [ActionProcessor] and [LegalActionEnumerator], and never touches the
 * Strategist's `GameSimulator`.** That is not tidiness: `GameSimulator.isResolving` is mutable
 * instance state guarding recursion and `decisionResolver` is a mutable `var`, so a playout sharing
 * one would corrupt the guard of the very simulation that spawned it. `computeUndo = false` for the
 * same reason the simulator sets it — nothing here ever rewinds.
 *
 * The whole loop is a stripped-down `TableGameRunner`: no stuck detection (the horizon is the
 * clock), no illegal-action taxonomy (a rejected action falls back to a pass and the line ends),
 * no per-seat agents. Both players are driven by the same [PlayoutPolicy] — modelling the opponent
 * as "plays like us" is the standard rollout assumption and much cheaper than the alternative.
 */
class PlayoutEngine(
    cardRegistry: CardRegistry,
    private val evaluator: BoardEvaluator,
    private val policy: PlayoutPolicy,
    private val decisions: FastDecisionResponder,
    private val settings: RolloutSettings = RolloutSettings.DEFAULT,
    private val winProbabilityScale: Double = WinProbability.SCALE,
) : Playouts {
    private val processor = ActionProcessor(EngineServices(cardRegistry), computeUndo = false)
    private val enumerator = LegalActionEnumerator.create(cardRegistry)

    /**
     * Play [start] forward and return the win probability it reached, from [playerId]'s side.
     *
     * @param seed the whole source of randomness for this playout — both the engine's own
     *   `GameState.rng` (shuffles, coin flips) and the policy's softmax draws. Two calls with the
     *   same seed on the same state produce the same number, which is what makes the arena's
     *   "identical at 8 threads and at 1" assertion survive Phase 7 and what lets
     *   `RolloutCandidateEvaluator` pair its candidate comparisons.
     * @param horizonPlayerTurns how many player turns forward to stop at, counted on
     *   `GameState.turnNumber`.
     * @param baseline the raw score of the decision's root, subtracted before squashing. See
     *   [leafValue] — this is the difference between a working rollout evaluator and one that
     *   reports "certain loss" for every candidate.
     */
    override fun run(
        start: GameState,
        playerId: EntityId,
        seed: Long,
        horizonPlayerTurns: Int,
        baseline: Double,
    ): Double {
        // The engine's randomness and the policy's are separate streams off the same seed, so that
        // adding or removing a policy draw cannot perturb a shuffle and vice versa.
        val (policySeed, gameRng) = GameRng(seed).split()
        var state = start.copy(rng = gameRng)
        var rng = policySeed
        val stopTurn = start.turnNumber + horizonPlayerTurns
        var actions = 0

        while (actions < settings.maxActionsPerPlayout) {
            WinProbability.terminalValue(state, playerId)?.let { return it }

            // `turnNumber` first reaches `stopTurn` at that turn's untap step, so stopping here is
            // exactly "the end of the previous turn" — the crack-back has happened and been
            // resolved, and nothing is mid-combat or mid-resolution.
            if (state.turnNumber >= stopTurn) break

            settings.earlyCutoffMargin?.let { margin ->
                val p = leafValue(state, playerId, baseline)
                if (abs(p - WinProbability.DRAW) > margin) return p
            }

            val decision = state.pendingDecision
            if (decision != null) {
                val response = decisions.respond(state, decision, playerId)
                val result = processor.process(state, SubmitDecision(decision.playerId, response)).result
                if (result.error != null) break
                state = result.state
                actions++
                continue
            }

            val priorityPlayer = state.priorityPlayerId ?: break
            val (action, nextRng) = policy.decide(state, priorityPlayer, rng) {
                enumerator.enumerate(state, priorityPlayer, EnumerationMode.ACTIONS_ONLY)
            }
            rng = nextRng

            val result = processor.process(state, action).result
            val next = if (result.error != null) {
                // The policy proposed something the engine rejected. A playout has no business
                // recovering cleverly — pass, and if even that fails the line is over.
                val fallback = processor.process(state, PassPriority(priorityPlayer)).result
                if (fallback.error != null) break else fallback.state
            } else {
                result.state
            }
            // A no-op action would spin the loop to the cap without advancing the game.
            if (next === state) break
            state = next
            actions++
        }

        return WinProbability.terminalValue(state, playerId) ?: leafValue(state, playerId, baseline)
    }

    /**
     * The horizon leaf: how far the playout moved the static evaluation, squashed into probability
     * space.
     *
     * **The subtraction is not a refinement, it is the thing that makes this work at all.** The raw
     * evaluator is not calibrated to any absolute meaning: `ThreatAssessment` scores "we can never
     * kill them" with a 99-turn sentinel, so an ordinary position where one side has no creatures
     * comes out around −176 while a close board is single digits. Squashing that absolutely pins
     * every candidate to the same clamped extreme, and the whole search reports "certain loss" for
     * every line — measured, on the puzzle suite, before this subtraction existed.
     *
     * Because [baseline] is the *decision's* root and therefore identical for every candidate, the
     * arbitrary offset cancels while the differences between candidates — the only thing the
     * Strategist compares — survive at full resolution. That is also what lets [WinProbability.SCALE]
     * be calibrated against score *differences* (single digits) rather than absolute scores.
     */
    private fun leafValue(state: GameState, playerId: EntityId, baseline: Double): Double =
        WinProbability.squash(
            evaluator.evaluate(state, state.projectedState, playerId) - baseline,
            winProbabilityScale,
        )
}
