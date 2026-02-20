package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.effects.CounterSpellWithFilterEffect
import kotlin.reflect.KClass

/**
 * Executor for CounterSpellWithFilterEffect.
 * "Counter target [filter] spell"
 */
class CounterSpellWithFilterExecutor : EffectExecutor<CounterSpellWithFilterEffect> {

    override val effectType: KClass<CounterSpellWithFilterEffect> = CounterSpellWithFilterEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: CounterSpellWithFilterEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetSpell = context.targets.firstOrNull()
        if (targetSpell !is ChosenTarget.Spell) {
            return ExecutionResult.error(state, "No valid spell target")
        }

        state.getEntity(targetSpell.spellEntityId)
            ?: return ExecutionResult.error(state, "Spell not found on stack")

        val predicateContext = PredicateContext.fromEffectContext(context)
        if (!predicateEvaluator.matches(state, targetSpell.spellEntityId, effect.filter.baseFilter, predicateContext)) {
            return ExecutionResult.error(state, "Spell does not match filter: ${effect.filter.baseFilter.description}")
        }

        return StackResolver().counterSpell(state, targetSpell.spellEntityId)
    }
}
