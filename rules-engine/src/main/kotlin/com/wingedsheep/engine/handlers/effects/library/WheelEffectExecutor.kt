package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.DrawFailedEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.LibraryShuffledEvent
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.WheelEffect
import kotlin.reflect.KClass

/**
 * Executor for WheelEffect.
 * "Each [affected] player shuffles their hand into their library, then draws that many cards."
 *
 * Used by cards like Winds of Change ("Each player shuffles the cards from their hand
 * into their library, then draws that many cards.")
 */
class WheelEffectExecutor : EffectExecutor<WheelEffect> {

    override val effectType: KClass<WheelEffect> = WheelEffect::class

    override fun execute(
        state: GameState,
        effect: WheelEffect,
        context: EffectContext
    ): ExecutionResult {
        val events = mutableListOf<GameEvent>()
        var newState = state

        // Determine which players are affected
        val affectedPlayers = when (effect.target) {
            is EffectTarget.Controller -> listOf(context.controllerId)
            is EffectTarget.EachPlayer -> {
                val players = mutableListOf(context.controllerId)
                context.opponentId?.let { players.add(it) }
                players
            }
            is EffectTarget.EachOpponent -> {
                context.opponentId?.let { listOf(it) } ?: emptyList()
            }
            else -> listOf(context.controllerId)
        }

        // For each affected player: count hand, shuffle hand into library, draw that many
        for (playerId in affectedPlayers) {
            val handZone = ZoneKey(playerId, ZoneType.HAND)
            val libraryZone = ZoneKey(playerId, ZoneType.LIBRARY)

            // Count cards in hand before shuffling
            val hand = newState.getZone(handZone)
            val handSize = hand.size

            if (handSize == 0) {
                // No cards to shuffle, no cards to draw
                continue
            }

            // Move all cards from hand to library
            for (cardId in hand) {
                val cardComponent = newState.getEntity(cardId)?.get<CardComponent>()
                newState = newState.removeFromZone(handZone, cardId)
                newState = newState.addToZone(libraryZone, cardId)

                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardComponent?.name ?: "Unknown",
                        fromZone = ZoneType.HAND,
                        toZone = ZoneType.LIBRARY,
                        ownerId = playerId
                    )
                )
            }

            // Shuffle the library
            val library = newState.getZone(libraryZone).shuffled()
            newState = newState.copy(zones = newState.zones + (libraryZone to library))
            events.add(LibraryShuffledEvent(playerId))

            // Draw that many cards
            val drawnCards = mutableListOf<EntityId>()
            repeat(handSize) {
                val currentLibrary = newState.getZone(libraryZone)
                if (currentLibrary.isEmpty()) {
                    // Failed to draw - game loss condition (Rule 704.5c)
                    newState = newState.updateEntity(playerId) { container ->
                        container.with(PlayerLostComponent(LossReason.EMPTY_LIBRARY))
                    }
                    events.add(DrawFailedEvent(playerId, "Empty library"))
                    return@repeat
                }

                val cardId = currentLibrary.first()
                drawnCards.add(cardId)

                newState = newState.removeFromZone(libraryZone, cardId)
                newState = newState.addToZone(handZone, cardId)
            }

            if (drawnCards.isNotEmpty()) {
                events.add(CardsDrawnEvent(playerId, drawnCards.size, drawnCards))
            }
        }

        return ExecutionResult.success(newState, events)
    }
}
