package com.wingedsheep.engine.mechanics.stack

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.PlayWithoutPayingCostComponent
import com.wingedsheep.engine.state.components.identity.PlottedComponent
import com.wingedsheep.engine.state.components.stack.*
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.SourcePlottedOnPriorTurn
import com.wingedsheep.sdk.scripting.targets.*

/**
 * Zone-move tails shared by a spell's resolution, fizzle and counter paths: shuffling a card into
 * its owner's library, and making a card that was just exiled from the stack *plotted*.
 */
internal object SpellZoneMoves {
    /**
     * Shuffle [ownerId]'s library after an Omen spell has been added to it on resolution
     * (Tarkir: Dragonstorm — "then shuffle this card into its owner's library"). Mirrors
     * [com.wingedsheep.engine.handlers.effects.library.ShuffleLibraryExecutor]: clears any
     * known top-of-library positions before shuffling, then advances the deterministic RNG.
     * The caller is responsible for emitting the [LibraryShuffledEvent].
     */
    fun shuffleOwnerLibrary(state: GameState, ownerId: EntityId): GameState {
        val libraryZone = ZoneKey(ownerId, Zone.LIBRARY)
        val cleared = com.wingedsheep.engine.handlers.effects.library.LibraryRevealUtils
            .clearLibraryReveals(state, ownerId)
        val (library, advanced) = cleared.nextRandom { shuffle(cleared.getZone(libraryZone)) }
        return advanced.copy(zones = advanced.zones + (libraryZone to library))
    }

    /**
     * Make a card that already sits in [ownerId]'s exile *plotted* (CR 718): tag it with
     * [PlottedComponent] + [PlayWithoutPayingCostComponent], grant a permanent may-play
     * permission gated on [SourcePlottedOnPriorTurn] (a plotted card can't be cast the turn it
     * was plotted), and emit [CardPlottedEvent]. Shared by [ExileTargetSpellEffect]'s
     * `makePlotted` path and the [AfterResolveDestinationComponent] `ExiledSpellRider.BECOMES_PLOTTED` self-cast path
     * (Lilah, Undefeated Slickshot).
     */
    fun applyPlottedToExiledCard(
        state: GameState,
        cardId: EntityId,
        ownerId: EntityId,
        cardName: String,
        events: MutableList<GameEvent>,
    ): GameState {
        val turnPlotted = state.turnNumber
        var newState = state.updateEntity(cardId) { c ->
            c.with(PlottedComponent(controllerId = ownerId, turnPlotted = turnPlotted))
                .with(PlayWithoutPayingCostComponent(controllerId = ownerId, permanent = true))
        }
        val (permId, stateWithPerm) = newState.newEntity()
        newState = stateWithPerm.addMayPlayPermission(
            MayPlayPermission(
                id = permId,
                cardIds = setOf(cardId),
                controllerId = ownerId,
                sourceId = cardId,
                condition = SourcePlottedOnPriorTurn,
                permanent = true,
                timestamp = newState.timestamp,
            )
        )
        events.add(CardPlottedEvent(ownerId, cardId, cardName))
        return newState
    }
}
