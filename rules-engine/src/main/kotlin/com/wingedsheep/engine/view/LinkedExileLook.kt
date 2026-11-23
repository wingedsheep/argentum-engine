package com.wingedsheep.engine.view

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.controlledLiveStaticAbilities
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.MayLookAtLinkedExile

/**
 * Who [MayLookAtLinkedExile] lets look at what — "You may look at cards exiled with this creature"
 * (Bane Alley Broker): the controller of each permanent with the live static, at the cards in that
 * permanent's linked-exile pile.
 *
 * Shared by [Visibility] (the current controller may look right now) and
 * [com.wingedsheep.engine.mechanics.sba.permanent.LinkedExileLookLatchCheck] (once allowed to look,
 * a player keeps the look for as long as the card stays exiled), so the two never disagree about
 * who holds the static.
 */
object LinkedExileLook {
    /** The cards [playerId] may look at through a [MayLookAtLinkedExile] they control. */
    fun cardsVisibleTo(state: GameState, playerId: EntityId, cardRegistry: CardRegistry): Set<EntityId> {
        var cards: MutableSet<EntityId>? = null
        val granting by lazy { grantingPermanents(state, playerId, cardRegistry) }
        for (permanentId in state.controlledBattlefield(playerId)) {
            // The pile check first: it is a component read, and almost no permanent has a pile, so
            // the per-pass cost stays one lookup per permanent.
            val pile = state.getEntity(permanentId)?.get<LinkedExileComponent>()?.exiledIds
            if (pile.isNullOrEmpty()) continue
            if (permanentId !in granting) continue
            (cards ?: HashSet<EntityId>().also { cards = it }).addAll(pile)
        }
        return cards ?: emptySet()
    }

    private fun grantingPermanents(state: GameState, playerId: EntityId, cardRegistry: CardRegistry): Set<EntityId> =
        state.controlledLiveStaticAbilities(playerId, cardRegistry)
            .filter { (_, ability) -> ability is MayLookAtLinkedExile }
            .mapTo(HashSet()) { (permanentId, _) -> permanentId }
}
