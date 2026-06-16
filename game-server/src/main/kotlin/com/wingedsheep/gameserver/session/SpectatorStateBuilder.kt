package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
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
import com.wingedsheep.engine.view.ClientCommanderDamage
import com.wingedsheep.engine.view.ClientGameState
import com.wingedsheep.engine.view.ClientStateTransformer
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.core.Format
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.Zone

class SpectatorStateBuilder(
    private val cardRegistry: CardRegistry,
    private val stateTransformer: ClientStateTransformer
) {
    /**
     * Build a spectator view of an N-player game. [players] is every seated player in turn order;
     * [seatRoster] is the lightweight seat list echoed to the client. The heavy per-player board
     * state rides in the embedded [ClientGameState] (already N-player); the [SpectatorStateUpdate]'s
     * `player1`/`player2` legacy fields are the first two seats, kept for the current spectator
     * board, replay viewer, and the external `tournament-newspaper` tooling.
     */
    fun buildState(
        state: GameState,
        players: List<PlayerSession>,
        seatRoster: List<ServerMessage.PlayerSeatInfo>,
        sessionId: String
    ): ServerMessage.SpectatorStateUpdate {
        require(players.size >= 2) { "Spectator state needs at least 2 seated players" }
        val p1 = players[0]
        val p2 = players[1]

        // Build full ClientGameState with every player's hand masked for GameBoard reuse
        val spectatorClientState = buildClientGameState(state, players.map { it.playerId })

        // Build decision status if there's a pending decision
        val decisionStatus = state.pendingDecision?.let { decision ->
            val decidingPlayer = players.firstOrNull { it.playerId == decision.playerId } ?: p1
            createDecisionStatus(decision, decidingPlayer.playerName)
        }

        return ServerMessage.SpectatorStateUpdate(
            gameSessionId = sessionId,
            gameState = spectatorClientState,
            players = seatRoster,
            player1Id = p1.playerId.value,
            player2Id = p2.playerId.value,
            player1Name = p1.playerName,
            player2Name = p2.playerName,
            // Legacy fields for backward compatibility (first two seats)
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
            is BatchYesNoDecision -> "Making a choice"
            is ChooseModeDecision -> "Choosing mode"
            is ChooseColorDecision -> "Choosing a color"
            is ChooseNumberDecision -> "Choosing a number"
            is DistributeDecision -> "Distributing"
            is OrderObjectsDecision -> "Ordering blockers"
            is SplitPilesDecision -> "Splitting piles"
            is SearchLibraryDecision -> "Searching library"
            is ReorderLibraryDecision -> "Reordering cards"
            is AssignDamageDecision -> "Assigning damage"
            is CombatResolutionDecision -> "Assigning combat damage"
            is ChooseOptionDecision -> "Making a choice"
            is ChooseReplacementDecision -> "Changing text"
            is BudgetModalDecision -> "Choosing modes"
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
        playerIds: List<EntityId>
    ): ClientGameState {
        // Use the first seat's perspective as the "viewing player" for the transform,
        // then mask every player's hand (spectators can't see any hand contents)
        val baseState = stateTransformer.transform(state, playerIds.first(), isSpectator = true)

        // Filter out hand cards from the cards map (spectators can't see any player's hand)
        val allHandCards = playerIds.flatMap { state.getHand(it) }.toSet()
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
        val poisonCounters = playerEntity?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0
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
            poisonCounters = poisonCounters,
            handSize = hand.size,
            librarySize = library.size,
            battlefield = battlefield.mapNotNull { cardId -> buildCardInfo(state, cardId) },
            graveyard = graveyard.mapNotNull { cardId -> buildCardInfo(state, cardId) },
            stack = stack.mapNotNull { cardId -> buildCardInfo(state, cardId) },
            commanderDamage = buildCommanderDamage(state, playerId)
        )
    }

    /**
     * Per-commander damage tallies against [defenderId]. Empty outside `Format.Commander`. Mirrors
     * the player-facing transformer so the spectator badge renders identically.
     */
    private fun buildCommanderDamage(state: GameState, defenderId: EntityId): List<ClientCommanderDamage> {
        val format = state.format as? Format.Commander ?: return emptyList()
        if (state.commanderDamage.isEmpty()) return emptyList()
        return state.commanderDamage
            .asSequence()
            .filter { it.defendingPlayerId == defenderId && it.amount > 0 }
            .mapNotNull { entry ->
                val container = state.getEntity(entry.commanderId) ?: return@mapNotNull null
                val card = container.get<CardComponent>() ?: return@mapNotNull null
                val controllerId = container.get<ControllerComponent>()?.playerId
                    ?: card.ownerId
                    ?: return@mapNotNull null
                ClientCommanderDamage(
                    commanderId = entry.commanderId,
                    commanderName = card.name,
                    controllerId = controllerId,
                    amount = entry.amount,
                    threshold = format.commanderDamageThreshold,
                    imageUri = card.imageUri,
                )
            }
            .sortedByDescending { it.amount }
            .toList()
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

        val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
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
