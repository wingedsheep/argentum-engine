package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Single source of truth for "can this card be emerge-cast, which creatures can pay for it, and
 * what does it actually cost?" — used by [com.wingedsheep.engine.legalactions.enumerators.EmergeCastEnumerator]
 * and by the cast handler's validate/execute paths.
 *
 * Emerge (CR 702.119a) is two static abilities that function while the spell is on the stack:
 * "You may cast this spell by paying [cost] and sacrificing a creature rather than paying its mana
 * cost", and "if you chose to pay this spell's emerge cost, its total cost is reduced by an amount
 * of **generic** mana equal to the sacrificed creature's mana value."
 *
 * Three consequences shape every read site:
 *
 *  - The reduction is generic-only. A creature whose mana value exceeds the generic portion of the
 *    emerge cost doesn't reduce the colored pips and the excess is simply wasted — so affordability
 *    has to be recomputed *per candidate creature*, not once for the spell.
 *  - The creature is chosen as you choose to pay the emerge cost (CR 601.2b) but sacrificed as you
 *    pay the total cost (CR 601.2h), i.e. *after* mana abilities are activated. It is therefore
 *    still available to be tapped for mana toward its own emerge cost, and the handler sacrifices
 *    it only once the mana payment has gone through.
 *  - Emerge grants no timing permission of its own — the spell is cast at its normal timing, which
 *    for Elder Deep-Fiend means flash.
 *
 * Like [DisturbCasts] there is no runtime-grant source: no card grants emerge to another, so a
 * printed keyword is the only input.
 */
object EmergeCasts {

    /** The printed emerge keyword on [cardDef], or null when it has none. */
    fun printedEmerge(cardDef: CardDefinition?): KeywordAbility.Emerge? =
        cardDef?.keywordAbilities?.filterIsInstance<KeywordAbility.Emerge>()?.firstOrNull()

    /**
     * Creatures [playerId] controls that could be sacrificed to pay an emerge cost (CR 702.119a —
     * "sacrificing a creature", with no further restriction; tapped and summoning-sick creatures
     * qualify). Read through projected state so animated lands and type-changing effects count.
     */
    fun sacrificeCandidates(state: GameState, playerId: EntityId): List<EntityId> {
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).filter { projected.isCreature(it) }
    }

    /**
     * The mana value the emerge reduction is measured against (CR 702.119a). A permanent's mana
     * value comes from its own mana cost, so a token or a card with no mana cost contributes 0.
     */
    fun manaValueOf(state: GameState, permanentId: EntityId): Int =
        state.getEntity(permanentId)?.get<CardComponent>()?.manaValue ?: 0

    /**
     * [cost] with the sacrificed creature's mana value taken off its **generic** portion
     * (CR 702.119a). Passing a null creature (no selection made yet) leaves the cost untouched.
     */
    fun reduceForSacrifice(cost: ManaCost, state: GameState, sacrificedId: EntityId?): ManaCost {
        val manaValue = sacrificedId?.let { manaValueOf(state, it) } ?: 0
        return if (manaValue > 0) cost.reduceGeneric(manaValue) else cost
    }
}
