package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.CounterTriggeringSpellEffect
import kotlin.reflect.KClass

/**
 * Executor for CounterTriggeringSpellEffect.
 * "Counter that spell" — counters the spell that triggered this ability,
 * using context.triggeringEntityId rather than a chosen target.
 */
class CounterTriggeringSpellExecutor : EffectExecutor<CounterTriggeringSpellEffect> {

    override val effectType: KClass<CounterTriggeringSpellEffect> = CounterTriggeringSpellEffect::class

    override fun execute(
        state: GameState,
        effect: CounterTriggeringSpellEffect,
        context: EffectContext
    ): ExecutionResult {
        val spellEntityId = context.triggeringEntityId
            ?: return ExecutionResult.error(state, "No triggering spell to counter")

        // Verify the spell is still on the stack
        if (!state.stack.contains(spellEntityId)) {
            return ExecutionResult.success(state)
        }

        return StackResolver().counterSpell(state, spellEntityId)
    }
}
