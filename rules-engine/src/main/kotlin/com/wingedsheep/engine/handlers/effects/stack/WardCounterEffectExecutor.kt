package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.CounterUnlessPaysContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.WardCounterEffect
import kotlin.reflect.KClass

/**
 * Executor for WardCounterEffect.
 *
 * When a ward trigger resolves, this executor:
 * 1. Finds the spell/ability that targeted the warded permanent (via targetingSourceEntityId)
 * 2. Checks if it's still on the stack
 * 3. Offers the spell/ability's controller the choice to pay the ward cost
 * 4. If they can't or won't pay, counters the spell/ability
 */
class WardCounterEffectExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<WardCounterEffect> {
    override val effectType: KClass<WardCounterEffect> = WardCounterEffect::class

    override fun execute(
        state: GameState,
        effect: WardCounterEffect,
        context: EffectContext
    ): ExecutionResult {
        val spellEntityId = context.targetingSourceEntityId
            ?: return ExecutionResult.success(state) // No targeting source — do nothing

        // Check if the spell/ability is still on the stack
        if (!state.stack.contains(spellEntityId)) {
            return ExecutionResult.success(state) // Already left the stack — do nothing
        }

        val container = state.getEntity(spellEntityId)
            ?: return ExecutionResult.success(state)

        // Find the controller of the spell/ability
        val payingPlayerId = container.get<SpellOnStackComponent>()?.casterId
            ?: container.get<ActivatedAbilityOnStackComponent>()?.controllerId
            ?: container.get<TriggeredAbilityOnStackComponent>()?.controllerId
            ?: return ExecutionResult.success(state)

        val manaCost = ManaCost.parse(effect.manaCost)

        // Check if the player can pay
        val manaSolver = ManaSolver(cardRegistry)
        if (!manaSolver.canPay(state, payingPlayerId, manaCost)) {
            // Can't pay — counter immediately
            return counterSpellOrAbility(state, spellEntityId, container)
        }

        // Offer the choice to pay
        val decisionHandler = DecisionHandler()
        val decisionResult = decisionHandler.createYesNoDecision(
            state = state,
            playerId = payingPlayerId,
            sourceId = context.sourceId,
            sourceName = "Ward",
            prompt = "Pay $manaCost to prevent your spell from being countered by ward?",
            yesText = "Pay $manaCost",
            noText = "Don't pay"
        )

        val continuation = CounterUnlessPaysContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            payingPlayerId = payingPlayerId,
            spellEntityId = spellEntityId,
            manaCost = manaCost,
            sourceId = context.sourceId,
            sourceName = "Ward",
            controllerId = context.controllerId
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    private fun counterSpellOrAbility(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer
    ): ExecutionResult {
        val resolver = StackResolver(cardRegistry = cardRegistry)
        return if (container.has<SpellOnStackComponent>()) {
            resolver.counterSpell(state, entityId)
        } else {
            resolver.counterAbility(state, entityId)
        }
    }
}
