package com.wingedsheep.engine.handlers.effects.information

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.HandLookedAtEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.sdk.scripting.LookAtTargetHandEffect
import kotlin.reflect.KClass

/**
 * Executor for LookAtTargetHandEffect.
 * "Look at target player's hand"
 *
 * This effect allows a player to see the cards in another player's hand.
 * Cards that are looked at remain revealed to the viewing player for the
 * rest of the game (tracked via RevealedToComponent).
 */
class LookAtTargetHandExecutor : EffectExecutor<LookAtTargetHandEffect> {

    override val effectType: KClass<LookAtTargetHandEffect> = LookAtTargetHandEffect::class

    override fun execute(
        state: GameState,
        effect: LookAtTargetHandEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetPlayerId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target player for look at hand")

        val viewingPlayerId = context.controllerId

        // Get all cards in the target player's hand
        val handCards = state.getHand(targetPlayerId)

        if (handCards.isEmpty()) {
            // Empty hand - nothing to reveal, but still emit event
            return ExecutionResult.success(
                state,
                listOf(HandLookedAtEvent(viewingPlayerId, targetPlayerId, emptyList()))
            )
        }

        // Mark each card as revealed to the viewing player
        var newState = state
        for (cardId in handCards) {
            newState = newState.updateEntity(cardId) { container ->
                val existing = container.get<RevealedToComponent>()
                if (existing != null) {
                    container.with(existing.withPlayer(viewingPlayerId))
                } else {
                    container.with(RevealedToComponent.to(viewingPlayerId))
                }
            }
        }

        return ExecutionResult.success(
            newState,
            listOf(HandLookedAtEvent(viewingPlayerId, targetPlayerId, handCards))
        )
    }
}
