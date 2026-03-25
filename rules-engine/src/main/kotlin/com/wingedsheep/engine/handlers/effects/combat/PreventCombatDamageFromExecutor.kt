package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
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
        val newState = state.addFloatingEffect(
            layer = Layer.ABILITY,
            modification = SerializableModification.PreventCombatDamageFromGroup(
                filter = effect.source.baseFilter
            ),
            affectedEntities = emptySet(),
            duration = effect.duration,
            context = context
        )

        return ExecutionResult.success(newState)
    }
}
