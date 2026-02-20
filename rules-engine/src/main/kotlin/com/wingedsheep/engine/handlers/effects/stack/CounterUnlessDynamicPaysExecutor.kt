package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.CounterUnlessPaysContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.scripting.effects.CounterUnlessDynamicPaysEffect
import kotlin.reflect.KClass

/**
 * Executor for CounterUnlessDynamicPaysEffect.
 * "Counter target spell unless its controller pays {X} for each [something]."
 *
 * Evaluates the DynamicAmount at resolution time to determine the total generic
 * mana cost, then follows the same logic as CounterUnlessPaysExecutor.
 */
class CounterUnlessDynamicPaysExecutor(
    private val amountEvaluator: DynamicAmountEvaluator
) : EffectExecutor<CounterUnlessDynamicPaysEffect> {

    override val effectType: KClass<CounterUnlessDynamicPaysEffect> = CounterUnlessDynamicPaysEffect::class

    private val decisionHandler = DecisionHandler()

    override fun execute(
        state: GameState,
        effect: CounterUnlessDynamicPaysEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetSpell = context.targets.firstOrNull()
        if (targetSpell !is ChosenTarget.Spell) {
            return ExecutionResult.error(state, "No valid spell target")
        }

        val spellEntity = state.getEntity(targetSpell.spellEntityId)
            ?: return ExecutionResult.error(state, "Spell not found on stack")

        val spellComponent = spellEntity.get<SpellOnStackComponent>()
            ?: return ExecutionResult.error(state, "Target is not a spell")

        val payingPlayerId = spellComponent.casterId

        // Evaluate the dynamic amount to get the total generic mana cost
        val totalGenericCost = amountEvaluator.evaluate(state, effect.amount, context)

        if (totalGenericCost <= 0) {
            // Cost is 0 or negative — spell is not countered (no payment needed)
            return ExecutionResult.success(state)
        }

        val manaCost = ManaCost(listOf(ManaSymbol.Generic(totalGenericCost)))

        // Check if the paying player has enough mana to pay
        val manaSolver = ManaSolver()
        if (!manaSolver.canPay(state, payingPlayerId, manaCost)) {
            // Can't pay → auto-counter
            return StackResolver().counterSpell(state, targetSpell.spellEntityId)
        }

        // Can pay → ask the spell's controller if they want to pay
        val decisionResult = decisionHandler.createYesNoDecision(
            state = state,
            playerId = payingPlayerId,
            sourceId = context.sourceId,
            sourceName = "Counter unless pays",
            prompt = "Pay $manaCost to prevent your spell from being countered?",
            yesText = "Pay $manaCost",
            noText = "Don't pay"
        )

        // Push continuation so we know what to do when they answer
        val continuation = CounterUnlessPaysContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            payingPlayerId = payingPlayerId,
            spellEntityId = targetSpell.spellEntityId,
            manaCost = manaCost,
            sourceId = context.sourceId,
            sourceName = "Counter unless pays"
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }
}
