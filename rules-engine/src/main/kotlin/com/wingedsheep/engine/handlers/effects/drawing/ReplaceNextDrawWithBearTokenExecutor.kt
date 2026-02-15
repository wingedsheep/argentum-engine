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
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithBearTokenEffect
import kotlin.reflect.KClass

/**
 * Executor for ReplaceNextDrawWithBearTokenEffect.
 * Creates a floating replacement effect shield that replaces the controller's
 * next card draw this turn with creating a 2/2 green Bear creature token.
 *
 * Used by Words of Wilding: "{1}: The next time you would draw a card this turn,
 * create a 2/2 green Bear creature token instead."
 */
class ReplaceNextDrawWithBearTokenExecutor : EffectExecutor<ReplaceNextDrawWithBearTokenEffect> {

    override val effectType: KClass<ReplaceNextDrawWithBearTokenEffect> =
        ReplaceNextDrawWithBearTokenEffect::class

    override fun execute(
        state: GameState,
        effect: ReplaceNextDrawWithBearTokenEffect,
        context: EffectContext
    ): ExecutionResult {
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.ReplaceDrawWithBearToken,
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
