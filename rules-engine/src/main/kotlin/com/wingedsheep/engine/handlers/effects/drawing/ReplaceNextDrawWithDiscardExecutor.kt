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
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithDiscardEffect
import kotlin.reflect.KClass

/**
 * Executor for ReplaceNextDrawWithDiscardEffect.
 * Creates a floating replacement effect shield that replaces the controller's
 * next card draw this turn with each opponent discarding a card.
 *
 * Used by Words of Waste: "{1}: The next time you would draw a card this turn,
 * each opponent discards a card instead."
 */
class ReplaceNextDrawWithDiscardExecutor : EffectExecutor<ReplaceNextDrawWithDiscardEffect> {

    override val effectType: KClass<ReplaceNextDrawWithDiscardEffect> =
        ReplaceNextDrawWithDiscardEffect::class

    override fun execute(
        state: GameState,
        effect: ReplaceNextDrawWithDiscardEffect,
        context: EffectContext
    ): ExecutionResult {
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.ReplaceDrawWithDiscard,
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
