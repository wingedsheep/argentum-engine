package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.DiscardCardsEffect
import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.CardComponent
import com.wingedsheep.rulesengine.ecs.script.EcsEvent
import com.wingedsheep.rulesengine.ecs.script.ExecutionContext
import com.wingedsheep.rulesengine.ecs.script.ExecutionResult
import com.wingedsheep.rulesengine.zone.ZoneType
import kotlin.reflect.KClass

/**
 * Handler for DrawCardsEffect.
 */
class DrawCardsHandler : BaseEffectHandler<DrawCardsEffect>() {
    override val effectClass: KClass<DrawCardsEffect> = DrawCardsEffect::class

    override fun execute(
        state: EcsGameState,
        effect: DrawCardsEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context.controllerId, state)
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        repeat(effect.count) {
            val result = drawCard(currentState, targetPlayerId)
            currentState = result.state
            events.addAll(result.events)
        }

        return ExecutionResult(currentState, events)
    }

    private fun drawCard(state: EcsGameState, playerId: EntityId): ExecutionResult {
        val libraryZone = ZoneId(ZoneType.LIBRARY, playerId)
        val handZone = ZoneId(ZoneType.HAND, playerId)

        val library = state.getZone(libraryZone)
        if (library.isEmpty()) {
            return ExecutionResult(state, listOf(EcsEvent.DrawFailed(playerId)))
        }

        val cardId = library.first()
        val newState = state
            .removeFromZone(cardId, libraryZone)
            .addToZone(cardId, handZone)

        val cardName = state.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"

        return ExecutionResult(
            state = newState,
            events = listOf(EcsEvent.CardDrawn(playerId, cardId, cardName))
        )
    }
}

/**
 * Handler for DiscardCardsEffect.
 */
class DiscardCardsHandler : BaseEffectHandler<DiscardCardsEffect>() {
    override val effectClass: KClass<DiscardCardsEffect> = DiscardCardsEffect::class

    override fun execute(
        state: EcsGameState,
        effect: DiscardCardsEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context.controllerId, state)
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        val handZone = ZoneId(ZoneType.HAND, targetPlayerId)
        val graveyardZone = ZoneId(ZoneType.GRAVEYARD, targetPlayerId)
        val hand = currentState.getZone(handZone)

        repeat(minOf(effect.count, hand.size)) {
            val cardId = currentState.getZone(handZone).lastOrNull() ?: return@repeat
            val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"

            currentState = currentState
                .removeFromZone(cardId, handZone)
                .addToZone(cardId, graveyardZone)

            events.add(EcsEvent.CardDiscarded(targetPlayerId, cardId, cardName))
        }

        return ExecutionResult(currentState, events)
    }
}
