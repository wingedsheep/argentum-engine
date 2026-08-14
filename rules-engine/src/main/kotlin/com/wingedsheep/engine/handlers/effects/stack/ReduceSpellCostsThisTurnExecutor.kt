package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.TurnSpellCostReduction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.ReduceSpellCostsThisTurnEffect
import kotlin.reflect.KClass

/**
 * Executor for [ReduceSpellCostsThisTurnEffect].
 *
 * Evaluates the discount **once, here**, and records a [TurnSpellCostReduction] on the game state.
 * The cost calculator applies it to every matching spell the controller casts for the rest of the
 * turn; [com.wingedsheep.engine.core.TurnManager.startTurn] clears it at the turn boundary.
 *
 * Resolving the amount now rather than per cast is what the Scion cycle's rulings require — "the
 * value of X is determined only once, at the time the ability resolves". Mirrors
 * [GrantNextSpellAffinityExecutor], which installs the one-shot variant of the same idea.
 *
 * A resolved amount of 0 or less installs nothing: the discount would be a no-op, and skipping it
 * keeps the state (and the client's cost display) free of dead entries.
 */
class ReduceSpellCostsThisTurnExecutor(
    private val amountEvaluator: DynamicAmountEvaluator = DynamicAmountEvaluator()
) : EffectExecutor<ReduceSpellCostsThisTurnEffect> {

    override val effectType: KClass<ReduceSpellCostsThisTurnEffect> = ReduceSpellCostsThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: ReduceSpellCostsThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val amount = amountEvaluator.evaluate(state, effect.amount, context)
        if (amount <= 0) return EffectResult.success(state)

        val (effectiveState, sourceId) = if (context.sourceId != null) {
            state to context.sourceId
        } else {
            val (id, s) = state.newEntity()
            s to id
        }
        val sourceName = effectiveState.getEntity(sourceId)?.get<CardComponent>()?.name ?: "Unknown"

        val reduction = TurnSpellCostReduction(
            controllerId = context.controllerId,
            spellFilter = effect.spellFilter,
            amount = amount,
            sourceId = sourceId,
            sourceName = sourceName,
        )
        return EffectResult.success(
            effectiveState.copy(
                turnSpellCostReductions = effectiveState.turnSpellCostReductions + reduction
            )
        )
    }
}
