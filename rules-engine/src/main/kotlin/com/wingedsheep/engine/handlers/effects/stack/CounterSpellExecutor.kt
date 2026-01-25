package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.CounterSpellEffect

/**
 * Executor for CounterSpellEffect.
 * "Counter target spell"
 */
class CounterSpellExecutor : EffectExecutor<CounterSpellEffect> {

    override fun execute(
        state: GameState,
        effect: CounterSpellEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetSpell = context.targets.firstOrNull()
        if (targetSpell !is ChosenTarget.Spell) {
            return ExecutionResult.error(state, "No valid spell target")
        }

        return StackResolver().counterSpell(state, targetSpell.spellEntityId)
    }
}
