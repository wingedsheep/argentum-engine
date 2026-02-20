package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.MarkedForDestructionAtEndOfCombatComponent
import com.wingedsheep.sdk.scripting.effects.DestroyAtEndOfCombatEffect
import kotlin.reflect.KClass

/**
 * Executor for DestroyAtEndOfCombatEffect.
 * Marks a creature for destruction at end of combat by adding
 * [MarkedForDestructionAtEndOfCombatComponent].
 *
 * The actual destruction is processed by [TurnManager] when the END_COMBAT step begins.
 */
class DestroyAtEndOfCombatExecutor : EffectExecutor<DestroyAtEndOfCombatEffect> {

    override val effectType: KClass<DestroyAtEndOfCombatEffect> = DestroyAtEndOfCombatEffect::class

    override fun execute(
        state: GameState,
        effect: DestroyAtEndOfCombatEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        // Verify the target is still on the battlefield
        if (!state.getBattlefield().contains(targetId)) {
            return ExecutionResult.success(state)
        }

        val newState = state.updateEntity(targetId) { container ->
            container.with(MarkedForDestructionAtEndOfCombatComponent)
        }

        return ExecutionResult.success(newState)
    }
}
