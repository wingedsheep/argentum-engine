package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEndReason
import com.wingedsheep.engine.core.PlayerLostEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.GrantsCantLoseGameComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.scripting.effects.LoseGameEffect
import kotlin.reflect.KClass

/**
 * Executor for LoseGameEffect.
 * Target player loses the game immediately (e.g., Phage the Untouchable).
 */
class LoseGameExecutor : EffectExecutor<LoseGameEffect> {

    override val effectType: KClass<LoseGameEffect> = LoseGameEffect::class

    override fun execute(
        state: GameState,
        effect: LoseGameEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = EffectExecutorUtils.resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No target player for LoseGameEffect")

        // Check if player has already lost
        val container = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Target player not found")
        if (container.has<PlayerLostComponent>()) {
            return ExecutionResult.success(state)
        }

        // Check if player can't lose the game
        val cantLose = state.getBattlefield().any { entityId ->
            val c = state.getEntity(entityId) ?: return@any false
            c.has<GrantsCantLoseGameComponent>() &&
                c.get<ControllerComponent>()?.playerId == targetId
        }
        if (cantLose) {
            return ExecutionResult.success(state)
        }

        val newState = state.updateEntity(targetId) { c ->
            c.with(PlayerLostComponent(LossReason.CARD_EFFECT))
        }

        val events = listOf(
            PlayerLostEvent(targetId, GameEndReason.CARD_EFFECT, effect.message)
        )

        return ExecutionResult.success(newState, events)
    }
}
