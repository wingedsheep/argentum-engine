package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CardDestination
import com.wingedsheep.sdk.scripting.CardOrder
import com.wingedsheep.sdk.scripting.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.Player
import com.wingedsheep.sdk.scripting.ZonePlacement
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for MoveCollectionEffect.
 *
 * Reads cards from a named collection in [EffectContext.storedCollections]
 * and moves them to the specified destination zone.
 *
 * Cards are removed from their current zone (determined by looking at which zone
 * currently contains each card) and placed in the destination.
 */
class MoveCollectionExecutor : EffectExecutor<MoveCollectionEffect> {

    override val effectType: KClass<MoveCollectionEffect> = MoveCollectionEffect::class

    override fun execute(
        state: GameState,
        effect: MoveCollectionEffect,
        context: EffectContext
    ): ExecutionResult {
        val cards = context.storedCollections[effect.from]
            ?: return ExecutionResult.error(state, "No collection named '${effect.from}' in storedCollections")

        if (cards.isEmpty()) {
            return ExecutionResult.success(state)
        }

        val destination = effect.destination
        return when (destination) {
            is CardDestination.ToZone -> moveToZone(state, context, cards, destination, effect.order, effect.revealed)
        }
    }

    private fun moveToZone(
        state: GameState,
        context: EffectContext,
        cards: List<EntityId>,
        destination: CardDestination.ToZone,
        order: CardOrder,
        revealed: Boolean = false
    ): ExecutionResult {
        val destPlayerId = resolvePlayer(destination.player, context, state)
            ?: return ExecutionResult.error(state, "Could not resolve destination player for MoveCollection")

        val destZone = destination.zone

        // ControllerChooses ordering: pause for player to see/reorder cards going to library
        if (order == CardOrder.ControllerChooses && destZone == Zone.LIBRARY) {
            val isBottom = destination.placement == ZonePlacement.Bottom
            // For top placement: always pause (even for 1 card, so player can see it)
            // For bottom placement: only pause when there are multiple cards to order
            if (!isBottom || cards.size > 1) {
                return pauseForOrderDecision(state, context, cards, destZone, destPlayerId, destination.placement)
            }
        }

        return moveCardsToZone(state, context, cards, destination, destPlayerId, revealed)
    }

    /**
     * Pause execution and present a ReorderLibraryDecision so the player
     * can choose the order of multiple cards going to the top of a library.
     */
    private fun pauseForOrderDecision(
        state: GameState,
        context: EffectContext,
        cards: List<EntityId>,
        destZone: Zone,
        destPlayerId: EntityId,
        placement: ZonePlacement = ZonePlacement.Top
    ): ExecutionResult {
        val playerId = context.controllerId

        // Build card info map for the UI
        val cardInfoMap = cards.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null
            )
        }

        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val promptText = when (placement) {
            ZonePlacement.Bottom -> "Put the revealed cards on the bottom of your library in any order."
            else -> if (cards.size == 1) "Look at the top card of your library."
                else "Look at the top ${cards.size} cards of your library. Put them back in any order."
        }

        val decision = ReorderLibraryDecision(
            id = decisionId,
            playerId = playerId,
            prompt = promptText,
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            cards = cards,
            cardInfo = cardInfoMap
        )

        val continuation = MoveCollectionOrderContinuation(
            decisionId = decisionId,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            cards = cards,
            destinationZone = destZone,
            destinationPlayerId = destPlayerId,
            placement = placement
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = playerId,
                    decisionType = "REORDER_LIBRARY",
                    prompt = decision.prompt
                )
            )
        )
    }

    /**
     * Actually move the cards to the destination zone. Used both for direct moves
     * and after the player has chosen an order via MoveCollectionOrderContinuation.
     */
    internal fun moveCardsToZone(
        state: GameState,
        context: EffectContext,
        cards: List<EntityId>,
        destination: CardDestination.ToZone,
        destPlayerId: EntityId,
        revealed: Boolean = false
    ): ExecutionResult {
        val destZone = destination.zone
        val events = mutableListOf<GameEvent>()
        var newState = state

        // Determine where each card currently lives (for removal)
        for (cardId in cards) {
            val ownerId = newState.getEntity(cardId)?.get<OwnerComponent>()?.playerId ?: destPlayerId
            val cardName = newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"

            // Find and remove from current zone
            val fromZone = findCurrentZone(newState, cardId, ownerId)
            if (fromZone != null) {
                newState = newState.removeFromZone(ZoneKey(ownerId, fromZone), cardId)
            }

            // Add to destination zone based on placement
            val destZoneKey = ZoneKey(destPlayerId, destZone)
            newState = when (destination.placement) {
                ZonePlacement.Top, ZonePlacement.Default -> {
                    if (destZone == Zone.LIBRARY) {
                        // Prepend to library (top)
                        val currentLibrary = newState.getZone(destZoneKey)
                        newState.copy(zones = newState.zones + (destZoneKey to listOf(cardId) + currentLibrary))
                    } else {
                        newState.addToZone(destZoneKey, cardId)
                    }
                }
                ZonePlacement.Bottom -> {
                    if (destZone == Zone.LIBRARY) {
                        // Append to library (bottom)
                        val currentLibrary = newState.getZone(destZoneKey)
                        newState.copy(zones = newState.zones + (destZoneKey to currentLibrary + cardId))
                    } else {
                        newState.addToZone(destZoneKey, cardId)
                    }
                }
                ZonePlacement.Shuffled -> {
                    newState.addToZone(destZoneKey, cardId)
                    // Shuffle will happen after all cards are added
                }
                ZonePlacement.Tapped -> {
                    newState.addToZone(destZoneKey, cardId)
                }
            }

            // Apply battlefield-specific components
            if (destZone == Zone.BATTLEFIELD) {
                val container = newState.getEntity(cardId)
                if (container != null) {
                    var newContainer = container.with(ControllerComponent(destPlayerId))

                    // Creatures enter with summoning sickness
                    val cardComp = container.get<CardComponent>()
                    if (cardComp?.typeLine?.isCreature == true) {
                        newContainer = newContainer.with(SummoningSicknessComponent)
                    }

                    // Apply tapped status if entering tapped
                    if (destination.placement == ZonePlacement.Tapped) {
                        newContainer = newContainer.with(TappedComponent)
                    }

                    newState = newState.copy(entities = newState.entities + (cardId to newContainer))
                }
            }

            if (fromZone != null) {
                events.add(
                    ZoneChangeEvent(
                        entityId = cardId,
                        entityName = cardName,
                        fromZone = fromZone,
                        toZone = destZone,
                        ownerId = ownerId
                    )
                )
            }
        }

        // Handle shuffled placement
        if (destination.placement == ZonePlacement.Shuffled && destZone == Zone.LIBRARY) {
            val destZoneKey = ZoneKey(destPlayerId, Zone.LIBRARY)
            val library = newState.getZone(destZoneKey)
            newState = newState.copy(zones = newState.zones + (destZoneKey to library.shuffled()))
            events.add(LibraryShuffledEvent(destPlayerId))
        }

        // Emit reveal event if configured
        if (revealed && cards.isNotEmpty()) {
            val cardNames = cards.map { cardId ->
                newState.getEntity(cardId)?.get<CardComponent>()?.name ?: "Unknown"
            }
            val imageUris = cards.map { cardId ->
                newState.getEntity(cardId)?.get<CardComponent>()?.imageUri
            }
            val sourceName = context.sourceId?.let { sourceId ->
                newState.getEntity(sourceId)?.get<CardComponent>()?.name
            }
            events.add(
                CardsRevealedEvent(
                    revealingPlayerId = context.controllerId,
                    cardIds = cards,
                    cardNames = cardNames,
                    imageUris = imageUris,
                    source = sourceName
                )
            )
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Find which zone a card currently lives in.
     */
    private fun findCurrentZone(state: GameState, cardId: EntityId, ownerId: EntityId): Zone? {
        for (zone in Zone.entries) {
            val zoneKey = ZoneKey(ownerId, zone)
            if (cardId in state.getZone(zoneKey)) {
                return zone
            }
        }
        return null
    }

    private fun resolvePlayer(player: Player, context: EffectContext, state: GameState): EntityId? {
        return when (player) {
            is Player.You -> context.controllerId
            is Player.Opponent -> context.opponentId
            is Player.TargetOpponent -> context.opponentId
            is Player.TargetPlayer -> context.opponentId
            is Player.ContextPlayer -> context.targets.getOrNull(player.index)?.let { EffectExecutorUtils.run { it.toEntityId() } }
            else -> context.controllerId
        }
    }
}
