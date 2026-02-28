package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.effects.CounterAbilityEffect
import kotlin.reflect.KClass

/**
 * Executor for CounterAbilityEffect.
 * "Counter target activated or triggered ability."
 *
 * Removes the ability from the stack without it resolving.
 * Unlike spells, countered abilities don't go to any zone.
 */
class CounterAbilityExecutor : EffectExecutor<CounterAbilityEffect> {

    override val effectType: KClass<CounterAbilityEffect> = CounterAbilityEffect::class

    override fun execute(
        state: GameState,
        effect: CounterAbilityEffect,
        context: EffectContext
    ): ExecutionResult {
        val target = context.targets.firstOrNull()
        if (target !is ChosenTarget.Spell) {
            return ExecutionResult.error(state, "No valid ability target")
        }

        return StackResolver().counterAbility(state, target.spellEntityId)
    }
}
