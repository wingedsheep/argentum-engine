package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.MarkedForSacrificeAtEndOfCombatComponent
import com.wingedsheep.sdk.scripting.effects.SacrificeAtEndOfCombatEffect
import kotlin.reflect.KClass

/**
 * Executor for SacrificeAtEndOfCombatEffect.
 * Marks a permanent for sacrifice at end of combat by adding
 * [MarkedForSacrificeAtEndOfCombatComponent].
 *
 * The actual sacrifice is processed by [TurnManager] when the END_COMBAT step begins.
 */
class SacrificeAtEndOfCombatExecutor : EffectExecutor<SacrificeAtEndOfCombatEffect> {

    override val effectType: KClass<SacrificeAtEndOfCombatEffect> = SacrificeAtEndOfCombatEffect::class

    override fun execute(
        state: GameState,
        effect: SacrificeAtEndOfCombatEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        // Verify the target is still on the battlefield
        if (!state.getBattlefield().contains(targetId)) {
            return ExecutionResult.success(state)
        }

        val newState = state.updateEntity(targetId) { container ->
            container.with(MarkedForSacrificeAtEndOfCombatComponent)
        }

        return ExecutionResult.success(newState)
    }
}
