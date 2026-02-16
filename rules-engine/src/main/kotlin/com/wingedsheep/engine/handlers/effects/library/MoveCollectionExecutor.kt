package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CardDestination
import com.wingedsheep.sdk.scripting.CardOrder
import com.wingedsheep.sdk.scripting.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.Player
import com.wingedsheep.sdk.scripting.ZonePlacement
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
            is CardDestination.ToZone -> moveToZone(state, context, cards, destination, effect.order)
        }
    }

    private fun moveToZone(
        state: GameState,
        context: EffectContext,
        cards: List<EntityId>,
        destination: CardDestination.ToZone,
        order: CardOrder
    ): ExecutionResult {
        val destPlayerId = resolvePlayer(destination.player, context, state)
            ?: return ExecutionResult.error(state, "Could not resolve destination player for MoveCollection")

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
                    // TODO: Add TappedComponent
                    newState.addToZone(destZoneKey, cardId)
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
            else -> context.controllerId
        }
    }
}
