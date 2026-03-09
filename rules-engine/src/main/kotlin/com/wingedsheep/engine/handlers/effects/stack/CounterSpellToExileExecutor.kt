package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.scripting.effects.CounterSpellToExileEffect
import kotlin.reflect.KClass

/**
 * Executor for CounterSpellToExileEffect.
 * "Counter target spell. Exile it instead of putting it into its owner's graveyard.
 *  You may cast that card without paying its mana cost for as long as it remains exiled."
 */
class CounterSpellToExileExecutor(
    private val cardRegistry: CardRegistry? = null
) : EffectExecutor<CounterSpellToExileEffect> {

    override val effectType: KClass<CounterSpellToExileEffect> = CounterSpellToExileEffect::class

    override fun execute(
        state: GameState,
        effect: CounterSpellToExileEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetSpell = context.targets.firstOrNull()
        if (targetSpell !is ChosenTarget.Spell) {
            return ExecutionResult.error(state, "No valid spell target")
        }

        return StackResolver(cardRegistry = cardRegistry).counterSpellToExile(
            state = state,
            spellId = targetSpell.spellEntityId,
            grantFreeCast = effect.grantFreeCast,
            controllerId = context.controllerId
        )
    }
}
