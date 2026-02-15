package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithBounceEffect
import kotlin.reflect.KClass

/**
 * Executor for ReplaceNextDrawWithBounceEffect.
 * Creates a floating replacement effect shield that replaces the controller's
 * next card draw this turn with each player returning a permanent to hand.
 *
 * Used by Words of Wind: "{1}: The next time you would draw a card this turn,
 * each player returns a permanent they control to its owner's hand instead."
 */
class ReplaceNextDrawWithBounceExecutor : EffectExecutor<ReplaceNextDrawWithBounceEffect> {

    override val effectType: KClass<ReplaceNextDrawWithBounceEffect> =
        ReplaceNextDrawWithBounceEffect::class

    override fun execute(
        state: GameState,
        effect: ReplaceNextDrawWithBounceEffect,
        context: EffectContext
    ): ExecutionResult {
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.ReplaceDrawWithBounce,
                affectedEntities = setOf(context.controllerId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name },
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        val newState = state.copy(
            floatingEffects = state.floatingEffects + floatingEffect
        )

        return ExecutionResult.success(newState)
    }
}
