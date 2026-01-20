package com.wingedsheep.gameserver.dto

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.layers.ModifierProvider
import com.wingedsheep.rulesengine.ecs.layers.ProjectionCache
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Transforms internal game state into client-facing DTOs.
 *
 * This class:
 * - Masks hidden information (opponent's hand, libraries)
 * - Transforms internal components into explicit DTO fields
 * - Applies continuous effects to show "true" card state
 * - Prevents information leakage by only including relevant data
 */
class ClientStateTransformer(
    private val modifierProvider: ModifierProvider? = null
) {
    /**
     * Transform the game state for a specific player's view.
     *
     * @param state The full game state
     * @param viewingPlayerId The player who will see this state
     * @param projectionCache Optional cache for layer projections
     * @return Client-safe game state DTO
     */
    fun transform(
        state: GameState,
        viewingPlayerId: EntityId,
        projectionCache: ProjectionCache? = null
    ): ClientGameState {
        // Build visible cards map
        val cards = mutableMapOf<EntityId, ClientCard>()

        // Process all zones
        val zones = mutableListOf<ClientZone>()

        for ((zoneId, entityIds) in state.zones) {
            val isVisible = isZoneVisibleTo(zoneId, viewingPlayerId)

            zones.add(
                ClientZone(
                    zoneId = zoneId,
                    cardIds = if (isVisible) entityIds else emptyList(),
                    size = entityIds.size,
                    isVisible = isVisible
                )
            )

            // Only include card details for visible zones
            if (isVisible) {
                for (entityId in entityIds) {
                    val clientCard = transformCard(state, entityId, zoneId, projectionCache)
                    if (clientCard != null) {
                        cards[entityId] = clientCard
                    }
                }
            }
        }

        // Build player information
        val players = state.getPlayerIds().map { playerId ->
            transformPlayer(state, playerId, viewingPlayerId)
        }

        // Build combat state if in combat
        val combat = transformCombat(state)

        return ClientGameState(
            viewingPlayerId = viewingPlayerId,
            cards = cards,
            zones = zones,
            players = players,
            currentPhase = state.currentPhase,
            currentStep = state.currentStep,
            activePlayerId = state.activePlayerId,
            priorityPlayerId = state.priorityPlayerId,
            turnNumber = state.turnNumber,
            isGameOver = state.isGameOver,
            winnerId = state.winner,
            combat = combat
        )
    }

    /**
     * Check if a zone's contents should be visible to a player.
     */
    private fun isZoneVisibleTo(zoneId: ZoneId, viewingPlayerId: EntityId): Boolean {
        return when (zoneId.type) {
            ZoneType.LIBRARY -> false
            ZoneType.HAND -> zoneId.ownerId == viewingPlayerId
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
        zoneId: ZoneId,
        projectionCache: ProjectionCache?
    ): ClientCard? {
        val container = state.getEntity(entityId) ?: return null
        val cardComponent = container.get<CardComponent>() ?: return null
        val definition = cardComponent.definition

        // Get controller
        val controllerId = container.get<ControllerComponent>()?.controllerId
            ?: cardComponent.ownerId

        // Get projected values for battlefield cards (continuous effects)
        val projectedView = if (zoneId.type == ZoneType.BATTLEFIELD && projectionCache != null) {
            projectionCache.getView(state, entityId, modifierProvider)
        } else {
            null
        }

        // Use projected values if available, otherwise use base definition
        val power = projectedView?.power ?: definition.creatureStats?.basePower
        val toughness = projectedView?.toughness ?: definition.creatureStats?.baseToughness
        val keywords = projectedView?.keywords ?: definition.keywords
        val colors = projectedView?.colors ?: definition.colors

        // Get state components
        val isTapped = container.has<TappedComponent>()
        val hasSummoningSickness = container.has<SummoningSicknessComponent>()
        val isTransformed = container.has<TransformedComponent>()

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
        val attackingTarget = when (val target = attackingComponent?.target) {
            is CombatTarget.Player -> target.playerId
            is CombatTarget.Planeswalker -> target.planeswalkerEntityId
            is CombatTarget.Battle -> target.battleEntityId
            null -> null
        }
        val blockingTarget = blockingComponent?.attackerId

        // Get attachments
        val attachedToComponent = container.get<AttachedToComponent>()
        val attachedTo = attachedToComponent?.targetId

        // Compute what's attached to this card
        val attachments = state.getBattlefield().filter { otherId ->
            state.getComponent<AttachedToComponent>(otherId)?.targetId == entityId
        }

        // Check if token
        val isToken = container.has<TokenComponent>()

        // Build type line string
        val typeLineParts = mutableListOf<String>()
        if (definition.typeLine.supertypes.isNotEmpty()) {
            typeLineParts.add(definition.typeLine.supertypes.joinToString(" ") { it.displayName })
        }
        typeLineParts.add(definition.typeLine.cardTypes.joinToString(" ") { it.displayName })
        val typeLine = if (definition.typeLine.subtypes.isNotEmpty()) {
            "${typeLineParts.joinToString(" ")} — ${definition.typeLine.subtypes.joinToString(" ") { it.value }}"
        } else {
            typeLineParts.joinToString(" ")
        }

        return ClientCard(
            id = entityId,
            name = definition.name,
            manaCost = definition.manaCost.toString(),
            manaValue = definition.cmc,
            typeLine = typeLine,
            cardTypes = definition.typeLine.cardTypes.map { it.name }.toSet(),
            subtypes = definition.typeLine.subtypes.map { it.value }.toSet(),
            colors = colors,
            oracleText = definition.oracleText,
            power = power,
            toughness = toughness,
            damage = damage,
            keywords = keywords,
            counters = counters,
            isTapped = isTapped,
            hasSummoningSickness = hasSummoningSickness,
            isTransformed = isTransformed,
            isAttacking = isAttacking,
            isBlocking = isBlocking,
            attackingTarget = attackingTarget,
            blockingTarget = blockingTarget,
            controllerId = controllerId,
            ownerId = cardComponent.ownerId,
            isToken = isToken,
            zone = zoneId,
            attachedTo = attachedTo,
            attachments = attachments,
            isFaceDown = false // TODO: Add face-down support for morph
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
        val playerComponent = state.getComponent<PlayerComponent>(playerId)

        // Only show mana pool to the player themselves
        val manaPool = if (playerId == viewingPlayerId) {
            val pool = state.getManaPool(playerId)
            ClientManaPool(
                white = pool.white,
                blue = pool.blue,
                black = pool.black,
                red = pool.red,
                green = pool.green,
                colorless = pool.colorless
            )
        } else {
            null
        }

        return ClientPlayer(
            playerId = playerId,
            name = playerComponent?.name ?: "Unknown",
            life = state.getLife(playerId),
            poisonCounters = state.getPoisonCounters(playerId),
            handSize = state.getHandSize(playerId),
            librarySize = state.getLibrarySize(playerId),
            graveyardSize = state.getGraveyardSize(playerId),
            landsPlayedThisTurn = state.getLandsPlayed(playerId),
            hasLost = state.hasLost(playerId),
            manaPool = manaPool
        )
    }

    /**
     * Transform combat state if in combat.
     */
    private fun transformCombat(state: GameState): ClientCombatState? {
        val combat = state.combat ?: return null

        val attackers = mutableListOf<ClientAttacker>()
        val blockers = mutableListOf<ClientBlocker>()

        // Find all attackers
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            val attackingComponent = container.get<AttackingComponent>()
            if (attackingComponent != null) {
                val blockedByComponent = container.get<BlockedByComponent>()
                attackers.add(
                    ClientAttacker(
                        creatureId = entityId,
                        creatureName = cardComponent.definition.name,
                        attackingTarget = when (val target = attackingComponent.target) {
                            is CombatTarget.Player -> ClientCombatTarget.Player(target.playerId)
                            is CombatTarget.Planeswalker -> ClientCombatTarget.Planeswalker(target.planeswalkerEntityId)
                            is CombatTarget.Battle -> ClientCombatTarget.Player(target.battleEntityId) // Treat as player for now
                        },
                        blockedBy = blockedByComponent?.blockerIds ?: emptyList()
                    )
                )
            }

            val blockingComponent = container.get<BlockingComponent>()
            if (blockingComponent != null) {
                blockers.add(
                    ClientBlocker(
                        creatureId = entityId,
                        creatureName = cardComponent.definition.name,
                        blockingAttacker = blockingComponent.attackerId
                    )
                )
            }
        }

        return ClientCombatState(
            attackingPlayerId = combat.attackingPlayer,
            defendingPlayerId = combat.defendingPlayer,
            attackers = attackers,
            blockers = blockers
        )
    }
}
