package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.GraveyardEntryTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.ReturnCreaturesPutInGraveyardThisTurnEffect
import com.wingedsheep.sdk.scripting.references.Player
import kotlin.reflect.KClass

/**
 * Executor for ReturnCreaturesPutInGraveyardThisTurnEffect.
 *
 * Finds all creature cards in the resolved player's graveyard that have a
 * GraveyardEntryTurnComponent matching the current turn number, and returns
 * them to their owner's hand.
 */
class ReturnCreaturesPutInGraveyardThisTurnExecutor : EffectExecutor<ReturnCreaturesPutInGraveyardThisTurnEffect> {

    override val effectType: KClass<ReturnCreaturesPutInGraveyardThisTurnEffect> =
        ReturnCreaturesPutInGraveyardThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnCreaturesPutInGraveyardThisTurnEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = resolvePlayer(effect.player, context) ?: return ExecutionResult.success(state)
        val graveyardKey = ZoneKey(playerId, Zone.GRAVEYARD)
        val graveyardIds = state.getZone(graveyardKey)

        // Find creature cards put into graveyard this turn
        val creaturesToReturn = graveyardIds.filter { entityId ->
            val container = state.getEntity(entityId) ?: return@filter false
            val cardComponent = container.get<CardComponent>() ?: return@filter false
            val entryTurn = container.get<GraveyardEntryTurnComponent>() ?: return@filter false
            cardComponent.typeLine.isCreature && entryTurn.turnNumber == state.turnNumber
        }

        if (creaturesToReturn.isEmpty()) {
            return ExecutionResult.success(state)
        }

        var newState = state
        val events = mutableListOf<com.wingedsheep.engine.core.GameEvent>()

        for (entityId in creaturesToReturn) {
            val cardComponent = newState.getEntity(entityId)?.get<CardComponent>() ?: continue
            val ownerId = cardComponent.ownerId ?: playerId
            val handKey = ZoneKey(ownerId, Zone.HAND)

            newState = newState.removeFromZone(graveyardKey, entityId)
            newState = newState.addToZone(handKey, entityId)

            events.add(
                ZoneChangeEvent(
                    entityId = entityId,
                    entityName = cardComponent.name,
                    fromZone = Zone.GRAVEYARD,
                    toZone = Zone.HAND,
                    ownerId = ownerId
                )
            )
        }

        return ExecutionResult.success(newState, events)
    }

    private fun resolvePlayer(player: Player, context: EffectContext) = when (player) {
        is Player.You -> context.controllerId
        is Player.Opponent -> context.opponentId
        is Player.TargetOpponent -> context.opponentId
        is Player.TargetPlayer -> context.opponentId
        is Player.TriggeringPlayer -> context.triggeringEntityId
        else -> context.controllerId
    }
}
