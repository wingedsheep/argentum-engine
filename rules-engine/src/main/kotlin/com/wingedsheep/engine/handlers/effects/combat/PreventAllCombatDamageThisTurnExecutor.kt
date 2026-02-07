package com.wingedsheep.engine.handlers.effects.combat

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
import com.wingedsheep.sdk.scripting.PreventAllCombatDamageThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for PreventAllCombatDamageThisTurnEffect.
 * "Prevent all combat damage that would be dealt this turn."
 *
 * This creates a floating effect that prevents all combat damage for the rest of the turn.
 * The CombatManager checks for this effect before dealing any combat damage.
 */
class PreventAllCombatDamageThisTurnExecutor : EffectExecutor<PreventAllCombatDamageThisTurnEffect> {

    override val effectType: KClass<PreventAllCombatDamageThisTurnEffect> =
        PreventAllCombatDamageThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: PreventAllCombatDamageThisTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.PreventAllCombatDamage,
                affectedEntities = emptySet()
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
