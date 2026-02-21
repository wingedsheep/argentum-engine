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
import com.wingedsheep.sdk.scripting.effects.PreventCombatDamageFromEffect
import kotlin.reflect.KClass

/**
 * Executor for PreventCombatDamageFromEffect.
 * "Prevent all combat damage [matching creatures] would deal this turn."
 *
 * Creates a floating effect with the GroupFilter stored so that creature type
 * is checked at damage time (per ruling: type checked when damage would be dealt).
 */
class PreventCombatDamageFromExecutor : EffectExecutor<PreventCombatDamageFromEffect> {

    override val effectType: KClass<PreventCombatDamageFromEffect> =
        PreventCombatDamageFromEffect::class

    override fun execute(
        state: GameState,
        effect: PreventCombatDamageFromEffect,
        context: EffectContext
    ): ExecutionResult {
        val floatingEffect = ActiveFloatingEffect(
            id = EntityId.generate(),
            effect = FloatingEffectData(
                layer = Layer.ABILITY,
                sublayer = null,
                modification = SerializableModification.PreventCombatDamageFromGroup(
                    filter = effect.source.baseFilter
                ),
                affectedEntities = emptySet()
            ),
            duration = effect.duration,
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
