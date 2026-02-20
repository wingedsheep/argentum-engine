package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import kotlin.reflect.KClass

/**
 * Executor for ShuffleLibraryEffect.
 * "Shuffle your library" or "Target player shuffles their library"
 */
class ShuffleLibraryExecutor : EffectExecutor<ShuffleLibraryEffect> {

    override val effectType: KClass<ShuffleLibraryEffect> = ShuffleLibraryEffect::class

    override fun execute(
        state: GameState,
        effect: ShuffleLibraryEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for shuffle")

        val libraryZone = ZoneKey(targetId, Zone.LIBRARY)
        val library = state.getZone(libraryZone).shuffled()

        val newZones = state.zones + (libraryZone to library)
        return ExecutionResult.success(
            state.copy(zones = newZones),
            listOf(LibraryShuffledEvent(targetId))
        )
    }
}
