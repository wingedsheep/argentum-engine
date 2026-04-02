package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GiftGivenEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.GiftGivenEffect
import kotlin.reflect.KClass

/**
 * Executor for GiftGivenEffect.
 * Emits a GiftGivenEvent so that "whenever you give a gift" triggers can fire.
 * No game state change — event-only.
 */
class GiftGivenExecutor : EffectExecutor<GiftGivenEffect> {

    override val effectType: KClass<GiftGivenEffect> = GiftGivenEffect::class

    override fun execute(
        state: GameState,
        effect: GiftGivenEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        return ExecutionResult.success(
            state,
            listOf(
                GiftGivenEvent(
                    controllerId = context.controllerId,
                    sourceId = context.sourceId,
                    sourceName = sourceName
                )
            )
        )
    }
}
