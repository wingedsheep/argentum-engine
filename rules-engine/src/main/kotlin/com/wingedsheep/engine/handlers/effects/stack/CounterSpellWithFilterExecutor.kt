package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.CounterSpellWithFilterEffect
import com.wingedsheep.sdk.scripting.SpellFilter
import kotlin.reflect.KClass

/**
 * Executor for CounterSpellWithFilterEffect.
 * "Counter target [filter] spell"
 */
class CounterSpellWithFilterExecutor : EffectExecutor<CounterSpellWithFilterEffect> {

    override val effectType: KClass<CounterSpellWithFilterEffect> = CounterSpellWithFilterEffect::class

    override fun execute(
        state: GameState,
        effect: CounterSpellWithFilterEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetSpell = context.targets.firstOrNull()
        if (targetSpell !is ChosenTarget.Spell) {
            return ExecutionResult.error(state, "No valid spell target")
        }

        val spellEntity = state.getEntity(targetSpell.spellEntityId)
            ?: return ExecutionResult.error(state, "Spell not found on stack")

        val cardComponent = spellEntity.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Spell has no card component")

        if (!matchesFilter(cardComponent, effect.filter)) {
            return ExecutionResult.error(state, "Spell does not match filter: ${effect.filter.description}")
        }

        return StackResolver().counterSpell(state, targetSpell.spellEntityId)
    }

    private fun matchesFilter(card: CardComponent, filter: SpellFilter): Boolean {
        return when (filter) {
            is SpellFilter.AnySpell -> true
            is SpellFilter.CreatureSpell -> card.typeLine.isCreature
            is SpellFilter.NonCreatureSpell -> !card.typeLine.isCreature
            is SpellFilter.SorcerySpell -> card.typeLine.isSorcery
            is SpellFilter.InstantSpell -> card.typeLine.isInstant
            is SpellFilter.CreatureOrSorcery -> card.typeLine.isCreature || card.typeLine.isSorcery
        }
    }
}
