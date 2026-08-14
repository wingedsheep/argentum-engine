package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.scripting.effects.ExileSpellsOnStackEffect
import kotlin.reflect.KClass

/** Exiles the matching spells without countering them. */
class ExileSpellsOnStackExecutor(
    private val cardRegistry: CardRegistry,
) : EffectExecutor<ExileSpellsOnStackEffect> {
    override val effectType: KClass<ExileSpellsOnStackEffect> = ExileSpellsOnStackEffect::class

    override fun execute(
        state: GameState,
        effect: ExileSpellsOnStackEffect,
        context: EffectContext,
    ): EffectResult {
        val spellIds = state.stack.filter { entityId ->
            if (effect.excludeSource && entityId == context.sourceId) return@filter false
            val spell = state.getEntity(entityId)?.get<SpellOnStackComponent>() ?: return@filter false
            !effect.opponentsOnly || spell.casterId != context.controllerId
        }

        val resolver = StackResolver(cardRegistry = cardRegistry)
        var currentState = state
        val events = mutableListOf<GameEvent>()
        for (spellId in spellIds) {
            if (spellId !in currentState.stack) continue
            val result = resolver.exileSpell(currentState, spellId, makePlotted = false)
            if (result.error != null) continue
            currentState = result.state
            events.addAll(result.events)
        }
        return EffectResult.success(currentState, events)
    }
}
