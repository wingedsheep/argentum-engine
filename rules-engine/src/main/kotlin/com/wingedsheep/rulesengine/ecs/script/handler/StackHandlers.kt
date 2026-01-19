package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.CounterSpellEffect
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.components.SpellOnStackComponent
import com.wingedsheep.rulesengine.ecs.event.ChosenTarget
import com.wingedsheep.rulesengine.ecs.script.EffectEvent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import kotlin.reflect.KClass

/**
 * Handler for CounterSpellEffect.
 *
 * Counters a target spell on the stack:
 * 1. Removes the spell from the stack
 * 2. Removes the SpellOnStackComponent
 * 3. Moves the spell card to its owner's graveyard
 *
 * The countered spell does NOT resolve - no effects happen.
 */
class CounterSpellHandler : BaseEffectHandler<CounterSpellEffect>() {
    override val effectClass: KClass<CounterSpellEffect> = CounterSpellEffect::class

    override fun execute(
        state: GameState,
        effect: CounterSpellEffect,
        context: ExecutionContext
    ): ExecutionResult {
        // Get the target spell from the context
        val target = context.targets.firstOrNull()
        if (target !is ChosenTarget.Spell) {
            return noOp(state)
        }

        val spellEntityId = target.spellEntityId

        // Verify the spell is actually on the stack
        if (spellEntityId !in state.getStack()) {
            return noOp(state)
        }

        // Get the card component to find the owner and name
        val container = state.getEntity(spellEntityId) ?: return noOp(state)
        val cardComponent = container.get<CardComponent>() ?: return noOp(state)
        val ownerId = cardComponent.ownerId
        val spellName = cardComponent.name

        var newState = state

        // Remove the spell from the stack
        // We need to remove this specific entity from the stack zone
        newState = newState.removeFromZone(spellEntityId, ZoneId.STACK)

        // Remove the SpellOnStackComponent
        newState = newState.updateEntity(spellEntityId) { c ->
            c.without<SpellOnStackComponent>()
        }

        // Move to owner's graveyard
        val graveyardZone = ZoneId.graveyard(ownerId)
        newState = newState.addToZone(spellEntityId, graveyardZone)

        return result(newState, EffectEvent.SpellCountered(spellEntityId, spellName, ownerId))
    }
}
