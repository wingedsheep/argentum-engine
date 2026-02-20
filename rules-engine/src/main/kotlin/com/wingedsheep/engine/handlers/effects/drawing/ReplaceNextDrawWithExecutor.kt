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
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithEffect
import kotlin.reflect.KClass

/**
 * Executor for [ReplaceNextDrawWithEffect].
 *
 * Creates a floating replacement-effect shield that intercepts the controller's
 * next card draw this turn, replacing it with the stored [ReplaceNextDrawWithEffect.replacementEffect].
 *
 * The replacement effect is stored directly in the shield as a generic [SerializableModification.ReplaceDrawWithEffect],
 * which is consumed at draw time by delegating to the effect execution pipeline.
 */
class ReplaceNextDrawWithExecutor : EffectExecutor<ReplaceNextDrawWithEffect> {

    override val effectType: KClass<ReplaceNextDrawWithEffect> = ReplaceNextDrawWithEffect::class

    override fun execute(
        state: GameState,
        effect: ReplaceNextDrawWithEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val modification = SerializableModification.ReplaceDrawWithEffect(
            replacementEffect = effect.replacementEffect,
            targets = context.targets,
            sourceId = context.sourceId,
            sourceName = sourceName
        )

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = modification,
                affectedEntities = setOf(context.controllerId)
            ),
            duration = Duration.EndOfTurn,
            sourceId = context.sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            timestamp = System.currentTimeMillis()
        )

        return ExecutionResult.success(state.copy(floatingEffects = state.floatingEffects + floatingEffect))
    }
}
