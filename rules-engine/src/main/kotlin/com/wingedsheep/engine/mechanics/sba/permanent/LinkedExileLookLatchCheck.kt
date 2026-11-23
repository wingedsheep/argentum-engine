package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.mechanics.sba.SbaOrder
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MayLookAtInExileComponent
import com.wingedsheep.engine.view.LinkedExileLook
import com.wingedsheep.sdk.core.Zone

/**
 * Latches the look a [com.wingedsheep.sdk.scripting.MayLookAtLinkedExile] gives. Not a CR 704
 * state-based action — bookkeeping that rides the same "before a player gets priority" pass, like
 * [EndedDurationExpiryCheck]'s one-way duration latch.
 *
 * Bane Alley Broker's ruling: "Once you are allowed to look at a face-down card in exile, you are
 * allowed to look at that card as long as it's exiled. If you no longer control Bane Alley Broker
 * when its last ability resolves, you can continue to look at the relevant cards in exile." The
 * static alone ([LinkedExileLook], read live by `Visibility`) only covers the current controller,
 * so every face-down exiled card in such a pile gets a [MayLookAtInExileComponent] naming that
 * controller. The stamp lives on the card and is dropped when it leaves exile, which is exactly
 * "as long as it's exiled".
 *
 * Emits no event — it changes nothing any trigger or animation can see, and an event would make the
 * SBA loop re-run for nothing. Returns the state untouched when nothing new is latched.
 */
class LinkedExileLookLatchCheck(private val cardRegistry: CardRegistry) : StateBasedActionCheck {
    override val name = "Linked-exile look latch"
    override val order = SbaOrder.LINKED_EXILE_LOOK_LATCH

    override fun check(state: GameState): ExecutionResult {
        var newState = state
        for (playerId in state.turnOrder) {
            for (cardId in LinkedExileLook.cardsVisibleTo(state, playerId, cardRegistry)) {
                val card = newState.getEntity(cardId) ?: continue
                if (!card.has<FaceDownComponent>()) continue
                val existing = card.get<MayLookAtInExileComponent>()
                if (existing?.mayLook(playerId) == true) continue
                if (newState.turnOrder.none { cardId in newState.getZone(ZoneKey(it, Zone.EXILE)) }) continue
                newState = newState.updateEntity(cardId) {
                    it.with(existing?.withPlayer(playerId) ?: MayLookAtInExileComponent.to(playerId))
                }
            }
        }
        return ExecutionResult.success(newState, emptyList())
    }
}
