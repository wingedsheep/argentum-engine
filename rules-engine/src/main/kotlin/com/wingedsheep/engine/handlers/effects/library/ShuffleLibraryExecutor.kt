package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
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
    ): EffectResult {
        val targetId = context.resolvePlayerTarget(effect.target)
            ?: return EffectResult.error(state, "No valid player for shuffle")

        val libraryZone = ZoneKey(targetId, Zone.LIBRARY)
        val library = state.getZone(libraryZone).shuffled()

        val newZones = state.zones + (libraryZone to library)
        return EffectResult.success(
            state.copy(zones = newZones),
            listOf(LibraryShuffledEvent(targetId))
        )
    }
}
