package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.ShuffleGraveyardIntoLibraryEffect
import kotlin.reflect.KClass

/**
 * Executor for ShuffleGraveyardIntoLibraryEffect.
 * "Target player shuffles their graveyard into their library."
 */
class ShuffleGraveyardIntoLibraryExecutor : EffectExecutor<ShuffleGraveyardIntoLibraryEffect> {

    override val effectType: KClass<ShuffleGraveyardIntoLibraryEffect> = ShuffleGraveyardIntoLibraryEffect::class

    override fun execute(
        state: GameState,
        effect: ShuffleGraveyardIntoLibraryEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for shuffle graveyard into library")

        val graveyardZone = ZoneKey(targetId, Zone.GRAVEYARD)
        val libraryZone = ZoneKey(targetId, Zone.LIBRARY)

        val graveyardCards = state.getZone(graveyardZone)
        if (graveyardCards.isEmpty()) {
            // Nothing to shuffle in, but still shuffle the library per the card's text
            val library = state.getZone(libraryZone).shuffled()
            val newZones = state.zones + (libraryZone to library)
            return ExecutionResult.success(
                state.copy(zones = newZones),
                listOf(LibraryShuffledEvent(targetId))
            )
        }

        // Move all graveyard cards into the library, then shuffle
        val library = state.getZone(libraryZone)
        val newLibrary = (library + graveyardCards).shuffled()
        val newZones = state.zones + (graveyardZone to emptyList()) + (libraryZone to newLibrary)

        return ExecutionResult.success(
            state.copy(zones = newZones),
            listOf(LibraryShuffledEvent(targetId))
        )
    }
}
