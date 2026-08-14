package com.wingedsheep.engine.mechanics.modal

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ChosenModesEverComponent
import com.wingedsheep.engine.state.components.battlefield.ChosenModesThisTurnComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ModalEffect

/**
 * Per-source memory of which modes a modal ability has already chosen — the state behind
 * [ModalEffect.excludePreviouslyChosenModes] ("choose one that hasn't been chosen", game-scoped,
 * Gandalf the Grey) and [ModalEffect.excludeModesChosenThisTurn] ("…this turn", turn-scoped,
 * Breeches / Eager Pillager).
 *
 * The memory is keyed to the source *object* (CR 700.4 object identity), so two copies of the same
 * permanent track their choices independently and a permanent that leaves and returns starts fresh.
 * The turn-scoped component is cleared each cleanup step by
 * [com.wingedsheep.engine.core.CleanupPhaseManager].
 *
 * Two pickers read and write through here so they can never disagree about what a source remembers:
 * the put-on-stack picker for modal *triggered* abilities
 * ([com.wingedsheep.engine.event.TriggerProcessor], CR 603.3c) and the resolution-time picker
 * ([com.wingedsheep.engine.handlers.effects.composite.ModalEffectExecutor]) that still serves modal
 * activated abilities and modals nested inside another effect.
 */
object ChosenModeMemory {

    /**
     * Mode indices of [modal] that [sourceId] must not be offered again. Empty when [modal] asks
     * for no memory at all, or when the source is gone.
     */
    fun excludedFor(state: GameState, sourceId: EntityId?, modal: ModalEffect): Set<Int> {
        if (!modal.excludePreviouslyChosenModes && !modal.excludeModesChosenThisTurn) return emptySet()
        val container = sourceId?.let { state.getEntity(it) } ?: return emptySet()
        val ever = if (modal.excludePreviouslyChosenModes) {
            container.get<ChosenModesEverComponent>()?.modeIndices ?: emptySet()
        } else emptySet()
        val thisTurn = if (modal.excludeModesChosenThisTurn) {
            container.get<ChosenModesThisTurnComponent>()?.modeIndices ?: emptySet()
        } else emptySet()
        return ever + thisTurn
    }

    /**
     * Record [modeIndex] against [sourceId] in whichever memories the caller asked for. A no-op
     * when neither flag is set or the source has already left the battlefield — the memory only
     * exists to narrow *that object's* later offers.
     */
    fun record(
        state: GameState,
        sourceId: EntityId?,
        modeIndex: Int,
        ever: Boolean,
        thisTurn: Boolean
    ): GameState {
        if (!ever && !thisTurn) return state
        if (sourceId == null || state.getEntity(sourceId) == null) return state
        return state.updateEntity(sourceId) { container ->
            var updated = container
            if (ever) {
                val existing = updated.get<ChosenModesEverComponent>() ?: ChosenModesEverComponent()
                updated = updated.with(existing.withChosen(modeIndex))
            }
            if (thisTurn) {
                val existing = updated.get<ChosenModesThisTurnComponent>() ?: ChosenModesThisTurnComponent()
                updated = updated.with(existing.withChosen(modeIndex))
            }
            updated
        }
    }
}
