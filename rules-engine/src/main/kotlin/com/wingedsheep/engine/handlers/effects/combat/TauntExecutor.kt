package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.MustAttackPlayerComponent
import com.wingedsheep.sdk.scripting.TauntEffect
import kotlin.reflect.KClass

/**
 * Executor for TauntEffect.
 * "During target player's next turn, creatures that player controls attack you if able."
 *
 * This adds a MustAttackPlayerComponent to the target player, which forces their
 * creatures to attack the Taunt caster during their next combat phase.
 *
 * The CombatManager validates this requirement during declare attackers.
 */
class TauntExecutor : EffectExecutor<TauntEffect> {

    override val effectType: KClass<TauntEffect> = TauntEffect::class

    override fun execute(
        state: GameState,
        effect: TauntEffect,
        context: EffectContext
    ): ExecutionResult {
        // Resolve the target player (whose creatures must attack)
        val targetPlayerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target player for Taunt effect")

        // The defender is the controller of Taunt (the caster)
        val defenderId = context.controllerId

        // Cannot taunt yourself
        if (targetPlayerId == defenderId) {
            return ExecutionResult.error(state, "Cannot target yourself with Taunt")
        }

        // Add MustAttackPlayerComponent to the target player
        val newState = state.updateEntity(targetPlayerId) { container ->
            container.with(
                MustAttackPlayerComponent(
                    defenderId = defenderId,
                    activeThisTurn = false  // Will be activated at start of their turn
                )
            )
        }

        return ExecutionResult.success(newState)
    }
}
