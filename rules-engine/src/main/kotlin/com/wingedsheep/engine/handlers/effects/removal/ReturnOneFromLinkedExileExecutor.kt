package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ReturnOneFromLinkedExileEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for ReturnOneFromLinkedExileEffect.
 *
 * At the beginning of each player's upkeep, the active player returns one of their
 * owned exiled cards (from the source's LinkedExileComponent) to the battlefield.
 *
 * If there are multiple eligible cards, creates a SelectCardsDecision for the player.
 * If exactly one, auto-returns it. If none for this player, does nothing.
 * If no cards remain in the linked exile at all, removes the global triggered ability.
 */
class ReturnOneFromLinkedExileExecutor : EffectExecutor<ReturnOneFromLinkedExileEffect> {

    override val effectType: KClass<ReturnOneFromLinkedExileEffect> = ReturnOneFromLinkedExileEffect::class

    override fun execute(
        state: GameState,
        effect: ReturnOneFromLinkedExileEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.success(state)

        // Read the LinkedExileComponent from the source entity
        val sourceContainer = state.getEntity(sourceId)
            ?: return ExecutionResult.success(state)
        val linkedExile = sourceContainer.get<LinkedExileComponent>()
            ?: return ExecutionResult.success(state)

        // Find which linked cards are still in exile
        val allLinkedCards = linkedExile.exiledIds.filter { entityId ->
            state.zones.any { (zone, cards) -> zone.zoneType == Zone.EXILE && entityId in cards }
        }

        // If no cards remain in exile, remove the global triggered ability
        if (allLinkedCards.isEmpty()) {
            val newState = removeGlobalAbilityForSource(state, sourceId)
            return ExecutionResult.success(newState)
        }

        // The active player (whose upkeep it is) is the triggering entity
        val activePlayerId = context.triggeringEntityId
            ?: return ExecutionResult.success(state)

        // Find cards owned by the active player
        val playerCards = allLinkedCards.filter { entityId ->
            val container = state.getEntity(entityId)
            val ownerId = container?.get<OwnerComponent>()?.playerId
                ?: container?.get<CardComponent>()?.ownerId
            ownerId == activePlayerId
        }

        if (playerCards.isEmpty()) {
            return ExecutionResult.success(state)
        }

        if (playerCards.size == 1) {
            return returnCardToBattlefield(state, playerCards.first(), sourceId)
        }

        // Multiple eligible cards — create a decision
        val decisionId = UUID.randomUUID().toString()

        val cardInfoMap = playerCards.associateWith { cardId ->
            val container = state.getEntity(cardId)
            val cardComponent = container?.get<CardComponent>()
            SearchCardInfo(
                name = cardComponent?.name ?: "Unknown",
                manaCost = cardComponent?.manaCost?.toString() ?: "",
                typeLine = cardComponent?.typeLine?.toString() ?: "",
                imageUri = null
            )
        }

        val sourceName = sourceContainer.get<CardComponent>()?.name ?: "Unknown"

        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = activePlayerId,
            prompt = "Choose a card to return to the battlefield",
            context = DecisionContext(
                sourceId = sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = playerCards,
            minSelections = 1,
            maxSelections = 1,
            ordered = false,
            cardInfo = cardInfoMap
        )

        val continuation = ReturnFromLinkedExileContinuation(
            decisionId = decisionId,
            playerId = activePlayerId,
            sourceId = sourceId,
            eligibleCards = playerCards
        )

        val stateWithDecision = state.withPendingDecision(decision)
        val stateWithContinuation = stateWithDecision.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decision,
            listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = activePlayerId,
                    decisionType = "SELECT_CARDS",
                    prompt = decision.prompt
                )
            )
        )
    }

    companion object {
        fun returnCardToBattlefield(
            state: GameState,
            cardId: EntityId,
            sourceId: EntityId
        ): ExecutionResult {
            val container = state.getEntity(cardId)
                ?: return ExecutionResult.success(state)
            val cardComponent = container.get<CardComponent>()
                ?: return ExecutionResult.success(state)

            val ownerId = container.get<OwnerComponent>()?.playerId
                ?: cardComponent.ownerId
                ?: return ExecutionResult.success(state)

            val currentZone = state.zones.entries.find { (_, cards) -> cardId in cards }?.key
                ?: return ExecutionResult.success(state)

            val battlefieldZone = ZoneKey(ownerId, Zone.BATTLEFIELD)

            var newState = state.removeFromZone(currentZone, cardId)
            newState = newState.addToZone(battlefieldZone, cardId)

            val events = listOf(
                ZoneChangeEvent(
                    entityId = cardId,
                    entityName = cardComponent.name,
                    fromZone = Zone.EXILE,
                    toZone = Zone.BATTLEFIELD,
                    ownerId = ownerId
                )
            )

            // Check if any linked cards remain in exile; if not, remove the global ability
            val sourceContainer = newState.getEntity(sourceId)
            val linkedExile = sourceContainer?.get<LinkedExileComponent>()
            if (linkedExile != null) {
                val remaining = linkedExile.exiledIds.filter { entityId ->
                    newState.zones.any { (zone, cards) -> zone.zoneType == Zone.EXILE && entityId in cards }
                }
                if (remaining.isEmpty()) {
                    newState = removeGlobalAbilityForSource(newState, sourceId)
                }
            }

            return ExecutionResult.success(newState, events)
        }

        fun removeGlobalAbilityForSource(state: GameState, sourceId: EntityId): GameState {
            val filtered = state.globalGrantedTriggeredAbilities.filter { global ->
                global.sourceId != sourceId ||
                    global.ability.effect !is ReturnOneFromLinkedExileEffect
            }
            return state.copy(globalGrantedTriggeredAbilities = filtered)
        }
    }
}
