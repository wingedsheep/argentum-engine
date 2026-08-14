package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.ai.engine.evaluation.ThreatAssessment
import com.wingedsheep.engine.core.MaximumHandSize
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * When holding a card is still free — the part [RemovalPatience] and [CounterPatience] share.
 *
 * Both ask a version of "is what I am pointing this at worth the card?", and they differ only in
 * the bar: [RemovalPatience] measures a creature against the removal's own mana value,
 * [CounterPatience] measures a spell against what the caster can still deploy this turn. Everything
 * about *when the question stops applying* is common, and it is the half that keeps patience from
 * turning a card into a brick.
 *
 * Patience that never ends is just a dead card, so it ends three ways:
 *
 *  1. **We are dying.** [ThreatAssessment.lethalOnBoardAgainst] is a hard veto, not a discount: on a
 *     turn where doing nothing loses the game there is no bar at all. The evaluator would very
 *     probably have outvoted a discount here anyway — that is an argument about magnitudes, which
 *     holds only until someone refits the weights, and this is a rule, which holds always. It is
 *     the same predicate the evaluator's own `−10.0` lethal term uses, so there is one definition of
 *     "they have lethal" rather than two that can drift.
 *  2. **The hand is full.** At [MaximumHandSize.DEFAULT] cards the next draw is a discard, so
 *     holding stops being free and the bar goes to zero outright. `>=` rather than `>`: at exactly
 *     the maximum, this turn's cleanup discards nothing but the next draw step puts the hand over,
 *     so the card being weighed is already the one that will be pitched.
 *  3. **The game moves on.** Patience is a bet that something better is coming, and the bet gets
 *     worse every turn — see [byTurn]. By [SPENT_BY_TURN] the AI simply spends the card.
 *
 * A fourth release is not stated here because it is automatic: short of lethal, a discount is only a
 * nudge in the evaluator's own units, so a creature that is racing us or a spell that wins the game
 * is priced by `ThreatAssessment` and `LifeDifferential` on a scale patience cannot reach.
 *
 * [MaximumHandSize.DEFAULT] rather than `MaximumHandSize.effective`, which needs a `CardRegistry`
 * and two evaluators this has no reason to carry. The cost of the simplification is that a
 * Reliquary Tower makes the AI spend a card it could have kept — the same constant, and the same
 * trade, that `CardAdvantage.cardValue`'s "past 7 cards, you're discarding anyway" branch already
 * makes one file over.
 */
internal object Patience {

    /**
     * How much of a patience bar still stands for [playerId] right now — `1.0` early, decaying to
     * `0.0`, and `0.0` outright whenever one of the releases has fired.
     */
    fun factorFor(state: GameState, projected: ProjectedState, playerId: EntityId): Double {
        if (ThreatAssessment.lethalOnBoardAgainst(state, projected, playerId)) return 0.0
        if (state.getZone(playerId, Zone.HAND).size >= MaximumHandSize.DEFAULT) return 0.0
        return byTurn(state.turnNumber)
    }

    /**
     * How much of the bar still stands on turn [turnNumber], from `1.0` down to `0.0`.
     *
     * Early the bet is a good one — the opponent's best card is still in their deck, and a card held
     * is a card aimed. Late it is a bad one twice over: there are fewer draws left to improve on
     * what is already down, and the mana that would have cast it has been idling for turns. So the
     * bar decays rather than switching off.
     *
     * [GameState.turnNumber] counts **turns, not rounds** (`TurnManager` increments it on every turn
     * change), so these read as roughly "through round three" and "by round seven".
     */
    fun byTurn(turnNumber: Int): Double = when {
        turnNumber <= FULL_THROUGH_TURN -> 1.0
        turnNumber >= SPENT_BY_TURN -> 0.0
        else -> (SPENT_BY_TURN - turnNumber).toDouble() / (SPENT_BY_TURN - FULL_THROUGH_TURN)
    }

    /**
     * Board value of the permanent one mana should expect to answer or buy.
     *
     * Read straight off `BoardPresence.creatureValue`'s own scale rather than chosen: a vanilla
     * creature at its rate on the curve prices out at about this per mana — Grizzly Bears (2 mana
     * 2/2) 2.8, Hill Giant (4 mana 3/3) 4.2, Craw Wurm (6 mana 6/4) 7.6, Air Elemental (5 mana 4/4
     * flier) 8.3. So `mana × 1.4` is "a permanent that size", stated in the units the evaluator
     * already speaks — which is what lets [RemovalPatience] scale its bar with the removal's own
     * cost and [CounterPatience] scale its bar with the mana the opponent has left.
     */
    const val FAIR_TRADE_VALUE_PER_MANA = 1.4

    /** Through this turn the bet on something better is a good one, and the bar is at full height. */
    const val FULL_THROUGH_TURN = 6

    /** From this turn on there is no bar at all — see [byTurn]. */
    const val SPENT_BY_TURN = 14
}
