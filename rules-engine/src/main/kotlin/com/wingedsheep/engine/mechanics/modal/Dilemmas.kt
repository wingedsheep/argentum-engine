package com.wingedsheep.engine.mechanics.modal

import com.wingedsheep.engine.core.DilemmaFacedEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.DilemmasFacedThisTurnComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * "You face a dilemma as you choose one or more modes for a spell or ability" (Star Trek). The one
 * place a dilemma is recorded: it advances the chooser's [DilemmasFacedThisTurnComponent] and
 * returns the [DilemmaFacedEvent] that "whenever you face a dilemma" triggers watch, so the count
 * and the trigger can never disagree.
 *
 * Called from every point a player *chooses* modes: casting a modal spell
 * ([com.wingedsheep.engine.mechanics.stack.SpellCaster]), activating a modal ability
 * ([com.wingedsheep.engine.handlers.actions.ability.ActivateAbilityHandler]), putting a modal
 * triggered ability on the stack ([com.wingedsheep.engine.event.TriggerProcessor]) and a mode
 * choice made while an ability resolves
 * ([com.wingedsheep.engine.handlers.continuations.ModalAndCloneContinuationResumer]). A spell copy
 * inherits its modes rather than choosing them, so the copy paths never call it.
 */
object Dilemmas {

    /** Record one dilemma for [playerId]; a no-op when no mode was chosen. */
    fun face(
        state: GameState,
        playerId: EntityId,
        sourceName: String,
        modesChosen: Int = 1
    ): Pair<GameState, List<DilemmaFacedEvent>> {
        if (modesChosen <= 0 || state.getEntity(playerId) == null) return state to emptyList()
        val updated = state.updateEntity(playerId) { container ->
            val tracker = container.get<DilemmasFacedThisTurnComponent>() ?: DilemmasFacedThisTurnComponent()
            container.with(tracker.copy(count = tracker.count + 1))
        }
        return updated to listOf(DilemmaFacedEvent(playerId, sourceName))
    }
}
