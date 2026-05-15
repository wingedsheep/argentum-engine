package com.wingedsheep.engine.handlers.effects.mana

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.EntityId

/**
 * Helpers for tracking mana produced by Treasure permanents.
 *
 * The [ManaPoolComponent.treasureMana] counter records how many mana units in
 * the pool came from a permanent with the Treasure subtype. When mana is spent
 * for a spell, [com.wingedsheep.engine.handlers.actions.spell.CastPaymentProcessor]
 * decrements the counter proportional to what was taken from the pool and flags
 * the spell as "paid with Treasure mana" when any was consumed.
 *
 * Powers Alchemist's Talent level 3 ("if mana from a Treasure was spent to
 * cast it"). The set of treasure-mana-producing executors that call into here
 * is: [AddManaExecutor], [AddColorlessManaExecutor], [AddAnyColorManaExecutor]
 * (both the immediate and the post-color-choice resumer paths).
 */
object TreasureManaTracker {

    /**
     * Whether the (recently-) producing source is a Treasure permanent. The
     * source may already be in the graveyard (Treasure's `{T}, Sacrifice this`
     * pays the cost before the mana effect resolves), but the entity persists
     * with its base [CardComponent.typeLine] intact.
     */
    fun isTreasureSource(state: GameState, sourceId: EntityId?): Boolean {
        if (sourceId == null) return false
        val card = state.getEntity(sourceId)?.get<CardComponent>() ?: return false
        return card.typeLine.hasSubtype(Subtype.TREASURE)
    }

    /**
     * Increment the producing player's `treasureMana` counter when [sourceId]
     * is a Treasure.
     */
    fun tagAddedMana(state: GameState, playerId: EntityId, sourceId: EntityId?, amount: Int): GameState {
        if (amount <= 0 || !isTreasureSource(state, sourceId)) return state
        return state.updateEntity(playerId) { container ->
            val pool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
            container.with(pool.copy(treasureMana = pool.treasureMana + amount))
        }
    }
}
