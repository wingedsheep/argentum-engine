package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

/**
 * "Is this X worth the card?" — [RemovalPatience]'s question, asked of a spell whose size is the
 * mana spent on it.
 *
 * A one-ply evaluator scores the board right after the spell resolves, so an `{X}` spell cast for
 * X=1 is weighed against passing, not against the same card cast two turns later for X=3. The leaf
 * sees one damage dealt or one card drawn and nothing lost but a card it prices low, and the AI
 * fires the spell the first turn it can (2026-11-02 user report: Ajani's Anguish "cast as soon as it
 * can"). [RemovalPatience] cannot catch it: it sets its bar at the card's mana value, and an X
 * spell's mana value in hand counts X as zero.
 *
 * The discount is the X the card is giving up: how much larger X our mana reaches within
 * [LOOKAHEAD_LAND_DROPS] land drops — the lands we control plus the ones in hand we can still play —
 * priced at [Patience.FAIR_TRADE_VALUE_PER_MANA] per point, the evaluator's own rate for what one
 * mana buys. A spell already cast for everything it will reach takes nothing; one cast for a
 * fraction of it pays the difference, and the leaf has to show the small X winning by more than
 * that (killing the creature that matters now, a lethal burn) for the cast to go ahead.
 *
 * It declines where X is not a size:
 *
 *  - **X gates the target** ("mana value X or less", "power X"): X is a function of the target —
 *    a larger X buys nothing — so [com.wingedsheep.ai.engine.XCostSelection] already picks the
 *    smallest X that reaches it.
 *  - **A creature spell** — a Hydra's body is board presence the moment it lands, and the leaf
 *    prices that tempo on its own.
 *  - **Anything on the stack**: a response is cast for what it answers now, not for later.
 *
 * Patience ends where [Patience.factorFor] says it does — lethal on board, a full hand, the game
 * moving on — so a burn spell is never held while we are dying, and late in the game the AI spends
 * what it has.
 */
object XPatience {

    /**
     * How much to subtract from [action]'s leaf score for spending its card on an X smaller than
     * our mana is about to reach, in raw evaluator units. `0.0` when [action] has no X to judge.
     */
    fun discount(
        state: GameState,
        playerId: EntityId,
        action: LegalAction,
        boardPresenceWeight: Double,
    ): Double {
        val cast = action.action as? CastSpell ?: return 0.0
        if (!action.hasXCost) return 0.0
        val x = cast.xValue ?: return 0.0
        if (state.stack.isNotEmpty()) return 0.0
        if (xGatesTargets(action)) return 0.0
        val card = state.getEntity(cast.cardId)?.get<CardComponent>() ?: return 0.0
        if (card.isCreature || !card.manaCost.hasX) return 0.0

        val projected = state.projectedState
        val patience = Patience.factorFor(state, projected, playerId)
        if (patience <= 0.0) return 0.0

        val lands = projected.getBattlefieldControlledBy(playerId).count { projected.hasType(it, "LAND") }
        val landsInHand = state.getZone(playerId, Zone.HAND).count { id ->
            id != cast.cardId && state.getEntity(id)?.get<CardComponent>()?.isLand == true
        }
        val reachableMana = lands + minOf(landsInHand, LOOKAHEAD_LAND_DROPS)
        // In hand an X spell's mana value counts X as zero, so it is exactly the fixed part.
        val reachableX = (reachableMana - card.manaValue) / card.manaCost.xCount.coerceAtLeast(1)
        val givenUp = (reachableX - x).coerceAtLeast(0)
        if (givenUp == 0) return 0.0
        return boardPresenceWeight * patience * Patience.FAIR_TRADE_VALUE_PER_MANA * givenUp
    }

    /** Whether some requirement's legal targets depend on X — see the class KDoc. */
    private fun xGatesTargets(action: LegalAction): Boolean =
        action.xConstrainsTargetManaValue || action.xConstrainsTargetManaValueExactly ||
            action.xConstrainsTargetPower ||
            action.targetRequirements.orEmpty().any {
                it.xConstrainsManaValue || it.xConstrainsManaValueExactly || it.xConstrainsPower
            }

    /**
     * How many land drops ahead the reachable X looks. Two, so that the turn-two X=1 cast is
     * measured against the X=3 it would be on turn four rather than only the X=2 of next turn — a
     * one-turn horizon prices every cast at a one-point loss as long as lands keep coming, which
     * is too small to outvote anything. Further than that is a bet on draws the AI has not made.
     */
    const val LOOKAHEAD_LAND_DROPS = 2
}
