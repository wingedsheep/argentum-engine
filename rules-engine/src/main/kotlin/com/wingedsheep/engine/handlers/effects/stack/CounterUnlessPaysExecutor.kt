package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.CounterUnlessPaysContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.sdk.scripting.CounterUnlessPaysEffect
import kotlin.reflect.KClass

/**
 * Executor for CounterUnlessPaysEffect.
 * "Counter target spell unless its controller pays {cost}."
 *
 * Logic:
 * 1. Get the target spell from context
 * 2. Find the spell's controller (casterId)
 * 3. Check if that player can pay the cost from their mana pool
 * 4. If they can't pay → auto-counter (no decision needed)
 * 5. If they can pay → present a YesNo decision, push CounterUnlessPaysContinuation
 */
class CounterUnlessPaysExecutor : EffectExecutor<CounterUnlessPaysEffect> {

    override val effectType: KClass<CounterUnlessPaysEffect> = CounterUnlessPaysEffect::class

    private val decisionHandler = DecisionHandler()

    override fun execute(
        state: GameState,
        effect: CounterUnlessPaysEffect,
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

        // Check if the paying player has enough mana to pay
        // Uses ManaSolver to consider both floating mana pool AND untapped mana sources (lands, etc.)
        val manaSolver = ManaSolver()
        if (!manaSolver.canPay(state, payingPlayerId, effect.cost)) {
            // Can't pay → auto-counter
            return StackResolver().counterSpell(state, targetSpell.spellEntityId)
        }

        // Can pay → ask the spell's controller if they want to pay
        val decisionResult = decisionHandler.createYesNoDecision(
            state = state,
            playerId = payingPlayerId,
            sourceId = context.sourceId,
            sourceName = "Counter unless pays",
            prompt = "Pay ${effect.cost} to prevent your spell from being countered?",
            yesText = "Pay ${effect.cost}",
            noText = "Don't pay"
        )

        // Push continuation so we know what to do when they answer
        val continuation = CounterUnlessPaysContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            payingPlayerId = payingPlayerId,
            spellEntityId = targetSpell.spellEntityId,
            manaCost = effect.cost,
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
