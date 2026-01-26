package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.movePermanentToZone
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolveTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.scripting.ReturnToHandEffect
import kotlin.reflect.KClass

/**
 * Executor for ReturnToHandEffect.
 * "Return target creature to its owner's hand"
 *
 * Handles returning cards from:
 * - Battlefield (normal bounce effect)
 * - Graveyard (death triggers like Endless Cockroaches)
 */
class ReturnToHandExecutor : EffectExecutor<ReturnToHandEffect> {

    override val effectType: KClass<ReturnToHandEffect> = ReturnToHandEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnToHandEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetId = resolveTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid target for return")

        val container = state.getEntity(targetId)
            ?: return ExecutionResult.error(state, "Entity not found")

        val cardComponent = container.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Not a card")

        val ownerId = cardComponent.ownerId
            ?: container.get<ControllerComponent>()?.playerId
            ?: return ExecutionResult.error(state, "Cannot determine owner")

        // Check if card is in graveyard (e.g., death trigger)
        val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)
        if (targetId in state.getZone(graveyardZone)) {
            return moveFromGraveyardToHand(state, targetId, cardComponent, ownerId)
        }

        // Otherwise, use standard battlefield-to-hand logic
        return movePermanentToZone(state, targetId, ZoneType.HAND)
    }

    /**
     * Move a card from graveyard to hand (for death triggers like Endless Cockroaches).
     */
    private fun moveFromGraveyardToHand(
        state: GameState,
        cardId: com.wingedsheep.sdk.model.EntityId,
        cardComponent: CardComponent,
        ownerId: com.wingedsheep.sdk.model.EntityId
    ): ExecutionResult {
        val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)
        val handZone = ZoneKey(ownerId, ZoneType.HAND)

        var newState = state.removeFromZone(graveyardZone, cardId)
        newState = newState.addToZone(handZone, cardId)

        return ExecutionResult.success(
            newState,
            listOf(
                ZoneChangeEvent(
                    cardId,
                    cardComponent.name,
                    ZoneType.GRAVEYARD,
                    ZoneType.HAND,
                    ownerId
                )
            )
        )
    }
}
