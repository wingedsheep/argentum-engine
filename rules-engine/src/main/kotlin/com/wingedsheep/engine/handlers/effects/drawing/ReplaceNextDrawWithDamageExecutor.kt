package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.mechanics.layers.ActiveFloatingEffect
import com.wingedsheep.engine.mechanics.layers.FloatingEffectData
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.ReplaceNextDrawWithDamageEffect
import kotlin.reflect.KClass

/**
 * Executor for ReplaceNextDrawWithDamageEffect.
 * Creates a floating replacement effect shield that replaces the controller's
 * next card draw this turn with dealing damage to the chosen target.
 *
 * Used by Words of War: "{1}: The next time you would draw a card this turn,
 * this enchantment deals 2 damage to any target instead."
 */
class ReplaceNextDrawWithDamageExecutor : EffectExecutor<ReplaceNextDrawWithDamageEffect> {

    override val effectType: KClass<ReplaceNextDrawWithDamageEffect> =
        ReplaceNextDrawWithDamageEffect::class

    override fun execute(
        state: GameState,
        effect: ReplaceNextDrawWithDamageEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = with(EffectExecutorUtils) {
            context.targets.firstOrNull()?.toEntityId()
        } ?: return ExecutionResult.error(state, "No valid target for damage replacement")

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.ReplaceDrawWithDamage(effect.damageAmount, targetId),
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
