package com.wingedsheep.engine.handlers.effects.removal

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
import com.wingedsheep.sdk.scripting.RegenerateEffect
import kotlin.reflect.KClass

/**
 * Executor for RegenerateEffect.
 * "Regenerate [permanent]" creates a one-shot regeneration shield that expires at end of turn.
 * The next time the permanent would be destroyed this turn, instead:
 * tap it, remove all damage, and remove it from combat.
 */
class RegenerateExecutor : EffectExecutor<RegenerateEffect> {

    override val effectType: KClass<RegenerateEffect> = RegenerateEffect::class

    override fun execute(
        state: GameState,
        effect: RegenerateEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = EffectExecutorUtils.resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "Could not resolve target for RegenerateEffect")

        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.RegenerationShield,
                affectedEntities = setOf(targetId)
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
