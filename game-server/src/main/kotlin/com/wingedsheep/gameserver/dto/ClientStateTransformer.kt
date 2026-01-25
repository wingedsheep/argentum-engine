package com.wingedsheep.gameserver.dto

import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.*
import com.wingedsheep.engine.state.components.battlefield.*
import com.wingedsheep.engine.state.components.combat.*
import com.wingedsheep.engine.state.components.player.*
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.mechanics.layers.StateProjector

/**
 * Transforms internal game state into client-facing DTOs.
 *
 * This class:
 * - Masks hidden information (opponent's hand, libraries)
 * - Transforms internal components into explicit DTO fields
 * - Applies continuous effects to show "true" card state
 * - Prevents information leakage by only including relevant data
 */
class ClientStateTransformer {

    private val stateProjector = StateProjector()

    /**
     * Transform the game state for a specific player's view.
     *
     * @param state The full game state
     * @param viewingPlayerId The player who will see this state
     * @return Client-safe game state DTO
     */
    fun transform(
        state: GameState,
        viewingPlayerId: EntityId
    ): ClientGameState {
        // Project the game state to apply continuous effects (Rule 613)
        val projectedState = stateProjector.project(state)

        // Build visible cards map
        val cards = mutableMapOf<EntityId, ClientCard>()

        // Process all zones
        val zones = mutableListOf<ClientZone>()

        for ((zoneKey, entityIds) in state.zones) {
            val isVisible = isZoneVisibleTo(zoneKey, viewingPlayerId)

            zones.add(
                ClientZone(
                    zoneId = zoneKey,
                    cardIds = if (isVisible) entityIds else emptyList(),
                    size = entityIds.size,
                    isVisible = isVisible
                )
            )

            // Only include card details for visible zones
            if (isVisible) {
                for (entityId in entityIds) {
                    val clientCard = transformCard(state, entityId, zoneKey, projectedState)
                    if (clientCard != null) {
                        cards[entityId] = clientCard
                    }
                }
            }
        }

        // Add the stack as a zone (stack is stored separately in GameState)
        if (state.stack.isNotEmpty()) {
            val stackZoneKey = ZoneKey(viewingPlayerId, ZoneType.STACK)
            zones.add(
                ClientZone(
                    zoneId = stackZoneKey,
                    cardIds = state.stack,
                    size = state.stack.size,
                    isVisible = true
                )
            )

            // Include card details for stack items
            for (entityId in state.stack) {
                val clientCard = transformCard(state, entityId, stackZoneKey, projectedState)
                if (clientCard != null) {
                    cards[entityId] = clientCard
                }
            }
        }

        // Build player information
        val players = state.turnOrder.map { playerId ->
            transformPlayer(state, playerId, viewingPlayerId)
        }

        // Build combat state if in combat
        val combat = transformCombat(state)

        // Get active and priority players, defaulting to first player if not set
        val activePlayerId = state.activePlayerId ?: state.turnOrder.firstOrNull() ?: viewingPlayerId
        val priorityPlayerId = state.priorityPlayerId ?: activePlayerId

        return ClientGameState(
            viewingPlayerId = viewingPlayerId,
            cards = cards,
            zones = zones,
            players = players,
            currentPhase = state.phase,
            currentStep = state.step,
            activePlayerId = activePlayerId,
            priorityPlayerId = priorityPlayerId,
            turnNumber = state.turnNumber,
            isGameOver = state.gameOver,
            winnerId = state.winnerId,
            combat = combat
        )
    }

    /**
     * Check if a zone's contents should be visible to a player.
     */
    private fun isZoneVisibleTo(zoneKey: ZoneKey, viewingPlayerId: EntityId): Boolean {
        return when (zoneKey.zoneType) {
            ZoneType.LIBRARY -> false
            ZoneType.HAND -> zoneKey.ownerId == viewingPlayerId
            ZoneType.BATTLEFIELD,
            ZoneType.GRAVEYARD,
            ZoneType.STACK,
            ZoneType.EXILE,
            ZoneType.COMMAND -> true
        }
    }

    /**
     * Transform an entity into a ClientCard DTO.
     */
    private fun transformCard(
        state: GameState,
        entityId: EntityId,
        zoneKey: ZoneKey,
        projectedState: ProjectedState
    ): ClientCard? {
        val container = state.getEntity(entityId) ?: return null
        val cardComponent = container.get<CardComponent>() ?: return null

        // Get controller (default to owner if not set)
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return null

        // Get owner
        val ownerId = cardComponent.ownerId ?: container.get<OwnerComponent>()?.playerId ?: controllerId

        // For battlefield permanents, use projected values from the layer system (Rule 613)
        // For cards in other zones, use base values
        val projectedValues = if (zoneKey.zoneType == ZoneType.BATTLEFIELD) {
            projectedState.getProjectedValues(entityId)
        } else {
            null
        }

        val power = projectedValues?.power ?: cardComponent.baseStats?.basePower
        val toughness = projectedValues?.toughness ?: cardComponent.baseStats?.baseToughness
        val keywords = projectedValues?.keywords?.mapNotNull {
            try { com.wingedsheep.sdk.core.Keyword.valueOf(it) } catch (_: Exception) { null }
        }?.toSet() ?: cardComponent.baseKeywords
        val colors = projectedValues?.colors?.mapNotNull {
            try { com.wingedsheep.sdk.core.Color.valueOf(it) } catch (_: Exception) { null }
        }?.toSet() ?: cardComponent.colors

        // Get state components
        val isTapped = container.has<TappedComponent>()
        val hasSummoningSickness = container.has<SummoningSicknessComponent>()
        val isFaceDown = container.has<FaceDownComponent>()

        // Get damage
        val damageComponent = container.get<DamageComponent>()
        val damage = damageComponent?.amount

        // Get counters
        val countersComponent = container.get<CountersComponent>()
        val counters = countersComponent?.counters ?: emptyMap()

        // Get combat state
        val attackingComponent = container.get<AttackingComponent>()
        val blockingComponent = container.get<BlockingComponent>()
        val isAttacking = attackingComponent != null
        val isBlocking = blockingComponent != null
        val attackingTarget = attackingComponent?.defenderId
        val blockingTarget = blockingComponent?.blockedAttackerIds?.firstOrNull()

        // Get attachments
        val attachedToComponent = container.get<AttachedToComponent>()
        val attachedTo = attachedToComponent?.targetId

        // Compute what's attached to this card
        val attachments = state.getBattlefield().filter { otherId ->
            state.getEntity(otherId)?.get<AttachedToComponent>()?.targetId == entityId
        }

        // Check if token
        val isToken = container.has<TokenComponent>()

        // Build type line string from TypeLine
        val typeLine = cardComponent.typeLine
        val typeLineParts = mutableListOf<String>()
        if (typeLine.supertypes.isNotEmpty()) {
            typeLineParts.add(typeLine.supertypes.joinToString(" ") { it.displayName })
        }
        typeLineParts.add(typeLine.cardTypes.joinToString(" ") { it.displayName })
        val typeLineString = if (typeLine.subtypes.isNotEmpty()) {
            "${typeLineParts.joinToString(" ")} — ${typeLine.subtypes.joinToString(" ") { it.value }}"
        } else {
            typeLineParts.joinToString(" ")
        }

        return ClientCard(
            id = entityId,
            name = cardComponent.name,
            manaCost = cardComponent.manaCost.toString(),
            manaValue = cardComponent.manaCost.cmc,
            typeLine = typeLineString,
            cardTypes = typeLine.cardTypes.map { it.name }.toSet(),
            subtypes = typeLine.subtypes.map { it.value }.toSet(),
            colors = colors,
            oracleText = cardComponent.oracleText,
            power = power,
            toughness = toughness,
            basePower = cardComponent.baseStats?.basePower,
            baseToughness = cardComponent.baseStats?.baseToughness,
            damage = damage,
            keywords = keywords,
            counters = counters,
            isTapped = isTapped,
            hasSummoningSickness = hasSummoningSickness,
            isTransformed = false, // TODO: Add transformed support
            isAttacking = isAttacking,
            isBlocking = isBlocking,
            attackingTarget = attackingTarget,
            blockingTarget = blockingTarget,
            controllerId = controllerId,
            ownerId = ownerId,
            isToken = isToken,
            zone = zoneKey,
            attachedTo = attachedTo,
            attachments = attachments,
            isFaceDown = isFaceDown
        )
    }

    /**
     * Transform player information.
     */
    private fun transformPlayer(
        state: GameState,
        playerId: EntityId,
        viewingPlayerId: EntityId
    ): ClientPlayer {
        val container = state.getEntity(playerId)
        val playerComponent = container?.get<PlayerComponent>()
        val lifeTotalComponent = container?.get<LifeTotalComponent>()
        val landDropsComponent = container?.get<LandDropsComponent>()
        val manaPoolComponent = container?.get<ManaPoolComponent>()

        // Calculate zone sizes
        val handSize = state.getHand(playerId).size
        val librarySize = state.getLibrary(playerId).size
        val graveyardSize = state.getGraveyard(playerId).size

        // Determine lands played this turn
        val landsPlayed = if (landDropsComponent != null) {
            landDropsComponent.maxPerTurn - landDropsComponent.remaining
        } else {
            0
        }

        // Check if player has lost (they're not the winner and game is over)
        val hasLost = state.gameOver && state.winnerId != null && state.winnerId != playerId

        // Only show mana pool to the player themselves
        val manaPool = if (playerId == viewingPlayerId && manaPoolComponent != null) {
            ClientManaPool(
                white = manaPoolComponent.white,
                blue = manaPoolComponent.blue,
                black = manaPoolComponent.black,
                red = manaPoolComponent.red,
                green = manaPoolComponent.green,
                colorless = manaPoolComponent.colorless
            )
        } else {
            null
        }

        return ClientPlayer(
            playerId = playerId,
            name = playerComponent?.name ?: "Unknown",
            life = lifeTotalComponent?.life ?: 20,
            poisonCounters = 0, // TODO: Add poison counter component support
            handSize = handSize,
            librarySize = librarySize,
            graveyardSize = graveyardSize,
            landsPlayedThisTurn = landsPlayed,
            hasLost = hasLost,
            manaPool = manaPool
        )
    }

    /**
     * Transform combat state if in combat.
     */
    private fun transformCombat(state: GameState): ClientCombatState? {
        // Check if we're in a combat step
        if (state.step.phase != Phase.COMBAT) {
            return null
        }

        val attackers = mutableListOf<ClientAttacker>()
        val blockers = mutableListOf<ClientBlocker>()
        var attackingPlayerId: EntityId? = null
        var defendingPlayerId: EntityId? = null

        // Find all attackers and blockers
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            val attackingComponent = container.get<AttackingComponent>()
            if (attackingComponent != null) {
                val blockedComponent = container.get<BlockedComponent>()

                // Track the attacking and defending players
                val controllerId = container.get<ControllerComponent>()?.playerId
                if (controllerId != null) {
                    attackingPlayerId = controllerId
                    defendingPlayerId = attackingComponent.defenderId
                }

                attackers.add(
                    ClientAttacker(
                        creatureId = entityId,
                        creatureName = cardComponent.name,
                        attackingTarget = ClientCombatTarget.Player(attackingComponent.defenderId),
                        blockedBy = blockedComponent?.blockerIds ?: emptyList()
                    )
                )
            }

            val blockingComponent = container.get<BlockingComponent>()
            if (blockingComponent != null) {
                for (attackerId in blockingComponent.blockedAttackerIds) {
                    blockers.add(
                        ClientBlocker(
                            creatureId = entityId,
                            creatureName = cardComponent.name,
                            blockingAttacker = attackerId
                        )
                    )
                }
            }
        }

        // If no attackers, there's no combat state to show
        if (attackers.isEmpty()) {
            return null
        }

        return ClientCombatState(
            attackingPlayerId = attackingPlayerId ?: state.activePlayerId ?: return null,
            defendingPlayerId = defendingPlayerId ?: state.getOpponent(attackingPlayerId!!) ?: return null,
            attackers = attackers,
            blockers = blockers
        )
    }
}
