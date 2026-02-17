package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.gameserver.dto.ClientGameState
import com.wingedsheep.gameserver.dto.ClientStateTransformer
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

class SpectatorStateBuilder(
    private val cardRegistry: CardRegistry,
    private val stateTransformer: ClientStateTransformer
) {
    fun buildState(
        state: GameState,
        p1: PlayerSession,
        p2: PlayerSession,
        sessionId: String
    ): ServerMessage.SpectatorStateUpdate {
        // Build full ClientGameState with both hands masked for GameBoard reuse
        val spectatorClientState = buildClientGameState(state, p1.playerId, p2.playerId)

        // Build decision status if there's a pending decision
        val decisionStatus = state.pendingDecision?.let { decision ->
            val decidingPlayer = if (decision.playerId == p1.playerId) p1 else p2
            createDecisionStatus(decision, decidingPlayer.playerName)
        }

        return ServerMessage.SpectatorStateUpdate(
            gameSessionId = sessionId,
            gameState = spectatorClientState,
            player1Id = p1.playerId.value,
            player2Id = p2.playerId.value,
            player1Name = p1.playerName,
            player2Name = p2.playerName,
            // Legacy fields for backward compatibility
            player1 = buildPlayerState(state, p1),
            player2 = buildPlayerState(state, p2),
            currentPhase = state.phase.name,
            activePlayerId = state.activePlayerId?.value,
            priorityPlayerId = state.priorityPlayerId?.value,
            combat = buildCombatState(state),
            decisionStatus = decisionStatus
        )
    }

    private fun createDecisionStatus(decision: PendingDecision, playerName: String): ServerMessage.SpectatorDecisionStatus {
        val displayText = when (decision) {
            is SelectCardsDecision -> "Selecting cards"
            is ChooseTargetsDecision -> "Choosing targets"
            is YesNoDecision -> "Making a choice"
            is ChooseModeDecision -> "Choosing mode"
            is ChooseColorDecision -> "Choosing a color"
            is ChooseNumberDecision -> "Choosing a number"
            is DistributeDecision -> "Distributing"
            is OrderObjectsDecision -> "Ordering blockers"
            is SplitPilesDecision -> "Splitting piles"
            is SearchLibraryDecision -> "Searching library"
            is ReorderLibraryDecision -> "Reordering cards"
            is AssignDamageDecision -> "Assigning damage"
            is ChooseOptionDecision -> "Making a choice"
            is SelectManaSourcesDecision -> "Selecting mana sources"
        }
        return ServerMessage.SpectatorDecisionStatus(
            playerName = playerName,
            playerId = decision.playerId.value,
            decisionType = decision::class.simpleName ?: "Unknown",
            displayText = displayText,
            sourceName = decision.context.sourceName
        )
    }

    private fun buildClientGameState(
        state: GameState,
        player1Id: EntityId,
        player2Id: EntityId
    ): ClientGameState {
        // Use player1's perspective as the "viewing player" for the transform,
        // then mask player1's hand as well
        val baseState = stateTransformer.transform(state, player1Id, isSpectator = true)

        // Filter out hand cards from the cards map (spectators can't see either player's hand)
        val player1Hand = state.getHand(player1Id).toSet()
        val player2Hand = state.getHand(player2Id).toSet()
        val allHandCards = player1Hand + player2Hand
        val visibleCards = baseState.cards.filterKeys { it !in allHandCards }

        // Update zones to hide hand contents but keep sizes
        val maskedZones = baseState.zones.map { zone ->
            if (zone.zoneId.zoneType == Zone.HAND) {
                // Keep size but hide card IDs for both hands
                zone.copy(
                    cardIds = emptyList(),
                    isVisible = false
                )
            } else {
                zone
            }
        }

        return baseState.copy(
            cards = visibleCards,
            zones = maskedZones
        )
    }

    private fun buildCombatState(state: GameState): ServerMessage.SpectatorCombatState? {
        // Only show combat state during combat phase
        if (state.step.phase != Phase.COMBAT) {
            return null
        }

        val attackers = mutableListOf<ServerMessage.SpectatorAttacker>()
        var attackingPlayerId: EntityId? = null
        var defendingPlayerId: EntityId? = null

        // Find all attackers on the battlefield
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val attackingComponent = container.get<AttackingComponent>() ?: continue

            // Track the attacking and defending players
            val controllerId = container.get<ControllerComponent>()?.playerId
            if (controllerId != null) {
                attackingPlayerId = controllerId
                defendingPlayerId = attackingComponent.defenderId
            }

            val blockedComponent = container.get<BlockedComponent>()
            attackers.add(
                ServerMessage.SpectatorAttacker(
                    creatureId = entityId.value,
                    blockedBy = blockedComponent?.blockerIds?.map { it.value } ?: emptyList()
                )
            )
        }

        if (attackers.isEmpty() || attackingPlayerId == null || defendingPlayerId == null) {
            return null
        }

        return ServerMessage.SpectatorCombatState(
            attackingPlayerId = attackingPlayerId.value,
            defendingPlayerId = defendingPlayerId.value,
            attackers = attackers
        )
    }

    private fun buildPlayerState(state: GameState, playerSession: PlayerSession): ServerMessage.SpectatorPlayerState {
        val playerId = playerSession.playerId
        val playerEntity = state.getEntity(playerId)

        val life = playerEntity?.get<LifeTotalComponent>()?.life ?: 20
        val hand = state.getZone(playerId, Zone.HAND)
        val library = state.getZone(playerId, Zone.LIBRARY)
        val battlefield = state.getZone(playerId, Zone.BATTLEFIELD)
        val graveyard = state.getZone(playerId, Zone.GRAVEYARD)

        // Stack is shared between players - get all stack items
        val stack = state.stack

        return ServerMessage.SpectatorPlayerState(
            playerId = playerId.value,
            playerName = playerSession.playerName,
            life = life,
            handSize = hand.size,
            librarySize = library.size,
            battlefield = battlefield.mapNotNull { cardId -> buildCardInfo(state, cardId) },
            graveyard = graveyard.mapNotNull { cardId -> buildCardInfo(state, cardId) },
            stack = stack.mapNotNull { cardId -> buildCardInfo(state, cardId) }
        )
    }

    private fun buildCardInfo(state: GameState, cardId: EntityId): ServerMessage.SpectatorCardInfo? {
        val card = state.getEntity(cardId) ?: return null
        val cardComponent = card.get<CardComponent>() ?: return null

        val isFaceDown = card.has<FaceDownComponent>()
        val tapped = card.has<TappedComponent>()
        val damage = card.get<DamageComponent>()?.amount ?: 0
        val isAttacking = card.has<AttackingComponent>()

        // Spectators should not see the identity of face-down cards
        if (isFaceDown) {
            return ServerMessage.SpectatorCardInfo(
                entityId = cardId.value,
                name = "Face-down creature",
                imageUri = null,
                isTapped = tapped,
                power = 2,
                toughness = 2,
                damage = damage,
                cardTypes = listOf("CREATURE"),
                isAttacking = isAttacking,
                targets = emptyList()
            )
        }

        val cardDef = cardRegistry.getCard(cardComponent.name)
        val cardTypes = cardComponent.typeLine.cardTypes.map { it.name }

        // Get targets for spells/abilities on the stack
        val targetsComponent = card.get<TargetsComponent>()
        val targets = targetsComponent?.targets?.mapNotNull { chosenTarget ->
            when (chosenTarget) {
                is ChosenTarget.Player -> ServerMessage.SpectatorTarget.Player(chosenTarget.playerId.value)
                is ChosenTarget.Permanent -> ServerMessage.SpectatorTarget.Permanent(chosenTarget.entityId.value)
                is ChosenTarget.Spell -> ServerMessage.SpectatorTarget.Spell(chosenTarget.spellEntityId.value)
                is ChosenTarget.Card -> null // Cards in zones not displayed as targets
            }
        } ?: emptyList()

        return ServerMessage.SpectatorCardInfo(
            entityId = cardId.value,
            name = cardComponent.name,
            imageUri = cardDef?.metadata?.imageUri,
            isTapped = tapped,
            power = cardComponent.baseStats?.basePower,
            toughness = cardComponent.baseStats?.baseToughness,
            damage = damage,
            cardTypes = cardTypes,
            isAttacking = isAttacking,
            targets = targets
        )
    }
}
