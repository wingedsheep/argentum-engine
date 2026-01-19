package com.wingedsheep.rulesengine.ecs.script.handler

import com.wingedsheep.rulesengine.ability.DiscardCardsEffect
import com.wingedsheep.rulesengine.ability.DrawCardsEffect
import com.wingedsheep.rulesengine.ability.EffectTarget
import com.wingedsheep.rulesengine.ability.ReturnFromGraveyardEffect
import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ability.WheelEffect
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

/**
 * Handler for ReturnFromGraveyardEffect.
 * Returns a card matching the filter from the controller's graveyard to a destination.
 * For now, auto-selects the first matching card. Full implementation would present a choice.
 */
class ReturnFromGraveyardHandler : BaseEffectHandler<ReturnFromGraveyardEffect>() {
    override val effectClass: KClass<ReturnFromGraveyardEffect> = ReturnFromGraveyardEffect::class

    override fun execute(
        state: EcsGameState,
        effect: ReturnFromGraveyardEffect,
        context: ExecutionContext
    ): ExecutionResult {
        val playerId = context.controllerId
        val graveyardZone = ZoneId(ZoneType.GRAVEYARD, playerId)
        val graveyard = state.getZone(graveyardZone)

        // Find first card matching filter
        val matchingCard = graveyard.firstOrNull { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            cardComponent != null && SearchLibraryHandler.matchesFilter(cardComponent.definition, effect.filter)
        }

        if (matchingCard == null) {
            // No matching card - effect does nothing
            return result(state)
        }

        val cardName = state.getEntity(matchingCard)?.get<CardComponent>()?.definition?.name ?: "Unknown"

        // Remove from graveyard
        var currentState = state.removeFromZone(matchingCard, graveyardZone)

        // Add to destination
        val destZoneId = when (effect.destination) {
            SearchDestination.HAND -> ZoneId(ZoneType.HAND, playerId)
            SearchDestination.BATTLEFIELD -> ZoneId.BATTLEFIELD
            SearchDestination.GRAVEYARD -> ZoneId(ZoneType.GRAVEYARD, playerId)
            SearchDestination.TOP_OF_LIBRARY -> ZoneId(ZoneType.LIBRARY, playerId)
        }

        currentState = if (effect.destination == SearchDestination.TOP_OF_LIBRARY) {
            currentState.addToZoneAt(matchingCard, destZoneId, 0)
        } else {
            currentState.addToZone(matchingCard, destZoneId)
        }

        return result(
            currentState,
            EcsEvent.CardMovedToZone(matchingCard, cardName, effect.destination.description)
        )
    }
}

/**
 * Handler for WheelEffect.
 * Each affected player shuffles their hand into their library, then draws that many cards.
 */
class WheelHandler : BaseEffectHandler<WheelEffect>() {
    override val effectClass: KClass<WheelEffect> = WheelEffect::class

    override fun execute(
        state: EcsGameState,
        effect: WheelEffect,
        context: ExecutionContext
    ): ExecutionResult {
        var currentState = state
        val events = mutableListOf<EcsEvent>()

        val players = when (effect.target) {
            EffectTarget.Controller -> listOf(context.controllerId)
            EffectTarget.Opponent -> listOf(getOpponent(context.controllerId, state))
            EffectTarget.EachPlayer -> state.getPlayerIds()
            else -> state.getPlayerIds()
        }

        for (playerId in players) {
            val handZone = ZoneId(ZoneType.HAND, playerId)
            val libraryZone = ZoneId(ZoneType.LIBRARY, playerId)

            val hand = currentState.getZone(handZone)
            val handSize = hand.size

            // Shuffle hand into library
            for (cardId in hand.toList()) {
                currentState = currentState
                    .removeFromZone(cardId, handZone)
                    .addToZone(cardId, libraryZone)
            }

            // Shuffle the library
            currentState = currentState.shuffleZone(libraryZone)

            // Draw that many cards
            repeat(handSize) {
                val library = currentState.getZone(libraryZone)
                if (library.isNotEmpty()) {
                    val cardId = library.first()
                    val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
                    currentState = currentState
                        .removeFromZone(cardId, libraryZone)
                        .addToZone(cardId, handZone)
                    events.add(EcsEvent.CardDrawn(playerId, cardId, cardName))
                }
            }

            events.add(EcsEvent.LibraryShuffled(playerId))
        }

        return ExecutionResult(currentState, events)
    }
}
