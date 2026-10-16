package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * An object's mana value as CR 202.3 reads it in the zone it is in right now.
 *
 * [CardComponent.manaValue] is the zone-independent reading, which treats `{X}` as 0. That is right
 * everywhere except the stack: CR 202.3e makes X the number chosen for it while the object is a
 * spell on the stack, so a Fireball cast with X=4 is a mana-value-5 spell to Electrosiphon's
 * energy, Reject Imperfection's "3 or less" and Counterbalance's reveal match. Once the card leaves
 * the stack its X is 0 again, which is why a "that spell's mana value" read that must outlive the
 * counter snapshots the number first (`Effects.StoreNumber`).
 *
 * Face-down status stays with the caller: the predicate and amount paths read it from projection
 * (CR 708.2 — no mana cost, mana value 0) before they ever reach the card's cost.
 */
object ManaValues {

    /** [entityId]'s mana value, or 0 when it has no card (an ability on the stack, a vanished id). */
    fun of(state: GameState, entityId: EntityId): Int {
        val card = state.getEntity(entityId)?.get<CardComponent>() ?: return 0
        return of(state, entityId, card)
    }

    /** [card]'s mana value as the object [entityId] — the caller already holds its card component. */
    fun of(state: GameState, entityId: EntityId, card: CardComponent): Int =
        card.manaValue + chosenX(state, entityId, card)

    /**
     * What `{X}` adds on the stack: the announced X once per `{X}` in the mana cost ({X}{X}{R} with
     * X=3 adds 6). A nonmodal transformed spell's mana value is its front face's
     * ([CardComponent.manaValueOverride], CR 202.3b) and carries no X of its own.
     */
    private fun chosenX(state: GameState, entityId: EntityId, card: CardComponent): Int {
        if (card.manaValueOverride != null || !card.manaCost.hasX) return 0
        if (entityId !in state.stack) return 0
        val x = state.getEntity(entityId)?.get<SpellOnStackComponent>()?.xValue ?: return 0
        return x * card.manaCost.xCount
    }
}
