package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.MillEffect
import kotlin.reflect.KClass

/**
 * Executor for MillEffect.
 * "Mill X" - Put the top X cards of a player's library into their graveyard.
 */
class MillExecutor : EffectExecutor<MillEffect> {

    override val effectType: KClass<MillEffect> = MillEffect::class

    override fun execute(
        state: GameState,
        effect: MillEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for mill")

        var newState = state
        val milledCards = mutableListOf<EntityId>()

        val libraryZone = ZoneKey(targetId, ZoneType.LIBRARY)
        val graveyardZone = ZoneKey(targetId, ZoneType.GRAVEYARD)

        repeat(effect.count) {
            val library = newState.getZone(libraryZone)
            if (library.isEmpty()) return@repeat

            val cardId = library.first()
            milledCards.add(cardId)

            newState = newState.removeFromZone(libraryZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        return ExecutionResult.success(newState)
    }
}
