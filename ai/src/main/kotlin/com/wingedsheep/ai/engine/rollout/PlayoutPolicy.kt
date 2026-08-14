package com.wingedsheep.ai.engine.rollout

import com.wingedsheep.ai.engine.CombatAdvisor
import com.wingedsheep.ai.engine.CombatSeed
import com.wingedsheep.ai.engine.TargetSelection
import com.wingedsheep.ai.engine.XCostSelection
import com.wingedsheep.ai.engine.budget.BudgetTier
import com.wingedsheep.ai.engine.budget.DecisionBudget
import com.wingedsheep.ai.engine.budget.SearchAllowances
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.MeaningfulActionFilter
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.model.GameRng
import kotlin.math.exp

/**
 * How a player acts inside a playout.
 *
 * **The hard rule of this file: nothing here may simulate.** Not `Strategist`, not `CombatAdvisor`'s
 * local search, not `DecisionResponder`'s simulation paths, not the Strategist's simulation-refined
 * target pick. A playout that simulates is quadratic, and a quadratic playout is a slower static
 * evaluator with more moving parts. Every rule below is a read of the state or of the enumerated
 * actions.
 *
 * The second rule is that it must be **stochastic**. A deterministic policy makes all R playouts of
 * one candidate identical, which collapses R samples into one and leaves the rollout evaluator
 * measuring a single arbitrary line rather than a distribution over lines. The softmax at step 4 is
 * what makes the mechanism work at all.
 *
 * In order:
 *
 * 1. [MeaningfulActionFilter.canAutoPassWithoutEnumerating] → pass, without enumerating. Phase 0
 *    measured 76.2% of priority windows offering zero candidates while still paying ~0.40 ms to
 *    find that out; this decides 40% of them from the state alone, and a playout crosses ~20–30.
 * 2. Combat declarations → [CombatSeed] / `chooseBlockers(useSimulation = false)`, the heuristic
 *    halves of `CombatAdvisor` with the simulation halves cut off.
 * 3. Land drop → always, if legal. There is no board on which skipping a land drop is the play, and
 *    making it a softmax entry would only add noise.
 * 4. Otherwise → softmax over a zero-simulation priority score, with passing as one of the options.
 * 5. Targets → [TargetSelection]'s heuristic rank only.
 */
class PlayoutPolicy(
    private val combatAdvisor: CombatAdvisor,
    private val intents: IntentCatalog = IntentCatalog.NONE,
    private val settings: RolloutSettings = RolloutSettings.DEFAULT,
) {
    /** Combat inside a playout gets the seed and nothing else — see [DecisionBudget] tiers. */
    private val noSearch =
        DecisionBudget(BudgetTier.TRIVIAL, SearchAllowances.TRIVIAL_ALLOWANCES, millis = 0)

    /**
     * The action [playerId] takes at this priority window, and the advanced generator.
     *
     * @param legalActions the enumerated actions, or null when the caller has not enumerated yet —
     *   which is the point: [decide] returns `PassPriority` without ever asking for them when the
     *   state alone settles the window.
     */
    fun decide(
        state: GameState,
        playerId: EntityId,
        rng: GameRng,
        enumerate: () -> List<LegalAction>,
    ): Pair<GameAction, GameRng> {
        if (MeaningfulActionFilter.canAutoPassWithoutEnumerating(state, playerId)) {
            return PassPriority(playerId) to rng
        }

        val legalActions = enumerate()

        // Combat declaration is never optional — the enumerator offers exactly one of these and the
        // engine will not move on until it is answered.
        legalActions.firstOrNull { it.actionType == "DeclareAttackers" }?.let { action ->
            val seed = CombatSeed.attackers(state, state.projectedState, action, playerId)
            return DeclareAttackers(playerId, seed.attackers) to rng
        }
        legalActions.firstOrNull { it.actionType == "DeclareBlockers" }?.let { action ->
            return combatAdvisor.chooseBlockers(
                state, action, playerId, useSimulation = false, budget = noSearch
            ) to rng
        }

        val candidates = MeaningfulActionFilter.filterMeaningful(legalActions)
            .filter { it.affordable && !it.isManaAbility }
        if (candidates.isEmpty()) return PassPriority(playerId) to rng

        // Lands are free and never wrong.
        candidates.firstOrNull { it.actionType == "PlayLand" }?.let { return it.action to rng }

        // Passing competes with the candidates rather than being decided separately: on our own
        // main phase a creature or a removal spell outscores it, on the opponent's turn most
        // instants do not, and the softmax is what makes "usually act, sometimes hold" a
        // distribution instead of a rule.
        val scores = candidates.map { priorityScore(state, it, playerId) } + PASS_SCORE
        val (index, nextRng) = sample(scores, settings.temperature, rng)
        if (index == candidates.size) return PassPriority(playerId) to nextRng

        // X first, then targets: an X-cost spell's legal targets can depend on the X chosen
        // ("mana value X or less"), so filling targets against the enumerator's permissive list
        // and binding X afterwards would pick targets the chosen X doesn't reach. Costs no
        // simulation — XCostSelection reads the board, it does not play it forward — and it is
        // *sampled* rather than maximized, because always taking the largest affordable X would
        // make every playout of this line identical, which is the second rule above.
        val (chosen, xRng) = XCostSelection.sampleX(state, candidates[index], nextRng)
        return TargetSelection.fillHeuristically(
            state, chosen, playerId, fillPartialRequirements = true, intents = intents
        ) to xRng
    }

    /**
     * How much the policy wants to take this action, before any temperature is applied.
     *
     * A prior, not an evaluation: it reads the card, never the board. Phase 6's [IntentCatalog] is
     * what makes it more than "mana value ascending" — a removal spell and a same-cost do-nothing
     * enchantment are the same number without it, which is exactly the blindness Phase 6 fixed for
     * the real evaluator.
     */
    private fun priorityScore(state: GameState, action: LegalAction, playerId: EntityId): Double {
        val cast = action.action as? CastSpell
        val cardName = cast?.let { state.getEntity(it.cardId)?.get<CardComponent>()?.name }
        val intent = cardName?.let { intents.forName(it) }

        var score = when (action.actionType) {
            "PlayLand" -> LAND_SCORE
            "CastSpell", "CastWithKicker", "CastWithSplice" -> BASE_CAST_SCORE
            else -> BASE_ACTIVATION_SCORE
        }

        val card = cast?.let { state.getEntity(it.cardId)?.get<CardComponent>() }
        if (card?.isCreature == true) score += CREATURE_BONUS
        // Bigger spells are usually the ones worth the turn. Deliberately shallow — this is a
        // playout policy, and a good one here buys less than one extra playout would.
        score += (card?.manaValue ?: 0) * MANA_VALUE_WEIGHT

        if (intent != null) {
            if (IntentTag.REMOVAL in intent || IntentTag.EXILE_REMOVAL in intent) score += REMOVAL_BONUS
            if (IntentTag.SWEEPER in intent) score += SWEEPER_BONUS
            if (IntentTag.DRAW in intent) score += DRAW_BONUS
            if (IntentTag.ANTHEM in intent) score += ANTHEM_BONUS
            if (IntentTag.TOKEN_MAKER in intent) score += TOKEN_BONUS
            if (IntentTag.RAMP in intent) score += RAMP_BONUS
            // A counterspell cast proactively counters nothing. It is still a legal action the
            // enumerator offers on an empty stack, and a playout that fires them off at random
            // systematically undervalues holding one.
            if (IntentTag.COUNTERSPELL in intent && state.stack.isEmpty()) score += DEAD_CARD_PENALTY
        }
        return score
    }

    /**
     * Sample an index from [scores] by softmax at [temperature], returning it with the advanced
     * generator.
     *
     * Shifted by the max before exponentiating — the standard guard, and load-bearing here because
     * `DEAD_CARD_PENALTY` and a large mana value can put several points between the extremes.
     */
    internal fun sample(scores: List<Double>, temperature: Double, rng: GameRng): Pair<Int, GameRng> {
        if (scores.size == 1) return 0 to rng
        val max = scores.max()
        val weights = scores.map { exp((it - max) / temperature) }
        val total = weights.sum()
        if (!total.isFinite() || total <= 0.0) return rng.nextInt(scores.size)
        val (roll, nextRng) = rng.nextDouble()
        var cumulative = 0.0
        val threshold = roll * total
        for (i in weights.indices) {
            cumulative += weights[i]
            if (cumulative >= threshold) return i to nextRng
        }
        return weights.lastIndex to nextRng
    }

    private companion object {
        /**
         * The score passing competes at. Above [BASE_CAST_SCORE] so a nondescript spell is roughly
         * a coin flip against holding, and well below a creature or a removal spell.
         */
        const val PASS_SCORE = 0.9

        const val LAND_SCORE = 2.0
        const val BASE_CAST_SCORE = 0.8
        const val BASE_ACTIVATION_SCORE = 0.4
        const val CREATURE_BONUS = 0.6
        const val MANA_VALUE_WEIGHT = 0.15
        const val REMOVAL_BONUS = 0.8
        const val SWEEPER_BONUS = 0.4
        const val DRAW_BONUS = 0.5
        const val ANTHEM_BONUS = 0.5
        const val TOKEN_BONUS = 0.4
        const val RAMP_BONUS = 0.3
        const val DEAD_CARD_PENALTY = -1.5
    }
}
