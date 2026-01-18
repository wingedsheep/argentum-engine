package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.game.GameState
import com.wingedsheep.rulesengine.player.ManaPool
import com.wingedsheep.rulesengine.player.Player
import com.wingedsheep.rulesengine.player.PlayerId
import com.wingedsheep.rulesengine.zone.Zone
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Converts between old [GameState] and new [EcsGameState].
 *
 * Used during the migration period to maintain backward compatibility.
 * Existing code can continue to use GameState while new code uses EcsGameState.
 *
 * Key guarantees:
 * - Round-trip conversion preserves all game state: `fromEcs(toEcs(state)) == state`
 * - Conversion is deterministic
 * - All components are properly serialized/deserialized
 *
 * Example usage:
 * ```kotlin
 * // Convert to ECS for new systems
 * val ecsState = StateConverter.toEcs(oldState)
 *
 * // Convert back for legacy systems
 * val oldState = StateConverter.fromEcs(ecsState)
 * ```
 */
object StateConverter {

    // ==========================================================================
    // GameState -> EcsGameState
    // ==========================================================================

    /**
     * Convert old GameState to new EcsGameState.
     */
    fun toEcs(state: GameState): EcsGameState {
        val entities = mutableMapOf<EntityId, ComponentContainer>()
        val zones = mutableMapOf<ZoneId, List<EntityId>>()

        // Convert players
        for ((playerId, player) in state.players) {
            val entityId = EntityId.fromPlayerId(playerId)
            entities[entityId] = playerToComponents(player)

            // Convert player zones
            zones[ZoneId.library(entityId)] = convertZone(player.library, entities)
            zones[ZoneId.hand(entityId)] = convertZone(player.hand, entities)
            zones[ZoneId.graveyard(entityId)] = convertZone(player.graveyard, entities)
        }

        // Convert shared zones
        zones[ZoneId.BATTLEFIELD] = convertZone(state.battlefield, entities)
        zones[ZoneId.STACK] = convertZone(state.stack, entities)
        zones[ZoneId.EXILE] = convertZone(state.exile, entities)

        return EcsGameState(
            entities = entities,
            zones = zones,
            turnState = state.turnState,
            combat = state.combat,
            isGameOver = state.isGameOver,
            winner = state.winner?.let { EntityId.fromPlayerId(it) },
            pendingTriggers = state.pendingTriggers,
            triggersOnStack = state.triggersOnStack
        )
    }

    /**
     * Convert a Player to a ComponentContainer.
     */
    private fun playerToComponents(player: Player): ComponentContainer {
        var container = ComponentContainer.of(
            PlayerComponent(player.name),
            LifeComponent(player.life),
            ManaPoolComponent(player.manaPool),
            PoisonComponent(player.poisonCounters),
            LandsPlayedComponent(player.landsPlayedThisTurn, player.maxLandsPerTurn)
        )

        // Add win/lose components if applicable
        if (player.hasLost) {
            container = container.with(LostGameComponent(""))
        }
        if (player.hasWon) {
            container = container.with(WonGameComponent)
        }

        return container
    }

    /**
     * Convert a Zone to a list of EntityIds, adding card entities to the map.
     */
    private fun convertZone(
        zone: Zone,
        entities: MutableMap<EntityId, ComponentContainer>
    ): List<EntityId> {
        return zone.cards.map { card ->
            val entityId = EntityId.fromCardId(card.id)
            entities[entityId] = cardToComponents(card)
            entityId
        }
    }

    /**
     * Convert a CardInstance to a ComponentContainer.
     */
    private fun cardToComponents(card: CardInstance): ComponentContainer {
        var container = ComponentContainer.of(
            CardComponent(card.definition, EntityId.of(card.ownerId)),
            ControllerComponent(EntityId.of(card.controllerId))
        )

        // Add state components
        if (card.isTapped) {
            container = container.with(TappedComponent)
        }
        if (card.summoningSickness) {
            container = container.with(SummoningSicknessComponent)
        }
        if (card.damageMarked > 0) {
            container = container.with(DamageComponent(card.damageMarked))
        }
        if (card.counters.isNotEmpty()) {
            container = container.with(CountersComponent(card.counters))
        }
        if (card.mustBeBlocked) {
            container = container.with(MustBeBlockedComponent)
        }

        // Add P/T for creatures
        card.definition.creatureStats?.let { stats ->
            container = container.with(
                PTComponent(
                    basePower = stats.basePower,
                    baseToughness = stats.baseToughness,
                    powerModifier = card.powerModifier,
                    toughnessModifier = card.toughnessModifier
                )
            )
        }

        // Add keywords component if there are modifications
        if (card.additionalKeywords.isNotEmpty() ||
            card.removedKeywords.isNotEmpty() ||
            card.temporaryKeywords.isNotEmpty()
        ) {
            container = container.with(
                KeywordsComponent(
                    added = card.additionalKeywords,
                    removed = card.removedKeywords,
                    temporary = card.temporaryKeywords
                )
            )
        }

        // Add attachment if attached
        card.attachedTo?.let { targetId ->
            container = container.with(AttachedToComponent(EntityId.fromCardId(targetId)))
        }

        return container
    }

    // ==========================================================================
    // EcsGameState -> GameState
    // ==========================================================================

    /**
     * Convert new EcsGameState back to old GameState.
     */
    fun fromEcs(state: EcsGameState): GameState {
        // Convert players
        val players = state.getPlayerIds().associate { playerId ->
            val oldPlayerId = playerId.toPlayerId()
            oldPlayerId to componentsToPlayer(state, playerId)
        }

        return GameState(
            players = players,
            battlefield = ecsZoneToZone(state, ZoneId.BATTLEFIELD),
            stack = ecsZoneToZone(state, ZoneId.STACK),
            exile = ecsZoneToZone(state, ZoneId.EXILE),
            turnState = state.turnState,
            combat = state.combat,
            isGameOver = state.isGameOver,
            winner = state.winner?.toPlayerId(),
            pendingTriggers = state.pendingTriggers,
            triggersOnStack = state.triggersOnStack
        )
    }

    /**
     * Convert player entity components back to a Player.
     */
    private fun componentsToPlayer(state: EcsGameState, playerId: EntityId): Player {
        val name = state.getComponent<PlayerComponent>(playerId)?.name ?: ""
        val life = state.getComponent<LifeComponent>(playerId)?.life ?: 20
        val manaPool = state.getComponent<ManaPoolComponent>(playerId)?.pool ?: ManaPool.EMPTY
        val poison = state.getComponent<PoisonComponent>(playerId)?.counters ?: 0
        val lands = state.getComponent<LandsPlayedComponent>(playerId)
        val hasLost = state.hasComponent<LostGameComponent>(playerId)
        val hasWon = state.hasComponent<WonGameComponent>(playerId)

        val oldPlayerId = playerId.toPlayerId()

        return Player(
            id = oldPlayerId,
            name = name,
            life = life,
            manaPool = manaPool,
            poisonCounters = poison,
            library = ecsZoneToZone(state, ZoneId.library(playerId)),
            hand = ecsZoneToZone(state, ZoneId.hand(playerId)),
            graveyard = ecsZoneToZone(state, ZoneId.graveyard(playerId)),
            hasLost = hasLost,
            hasWon = hasWon,
            landsPlayedThisTurn = lands?.count ?: 0,
            maxLandsPerTurn = lands?.maximum ?: 1
        )
    }

    /**
     * Convert an ECS zone to an old Zone.
     */
    private fun ecsZoneToZone(state: EcsGameState, zoneId: ZoneId): Zone {
        val cards = state.getZone(zoneId).mapNotNull { entityId ->
            componentsToCard(state, entityId)
        }
        return Zone(zoneId.type, cards, zoneId.ownerId?.value)
    }

    /**
     * Convert card entity components back to a CardInstance.
     */
    private fun componentsToCard(state: EcsGameState, entityId: EntityId): CardInstance? {
        val cardComponent = state.getComponent<CardComponent>(entityId) ?: return null
        val controller = state.getComponent<ControllerComponent>(entityId)
        val pt = state.getComponent<PTComponent>(entityId)
        val keywords = state.getComponent<KeywordsComponent>(entityId)
        val counters = state.getComponent<CountersComponent>(entityId)
        val damage = state.getComponent<DamageComponent>(entityId)
        val attachment = state.getComponent<AttachedToComponent>(entityId)

        return CardInstance(
            id = CardId(entityId.value),
            definition = cardComponent.definition,
            ownerId = cardComponent.ownerId.value,
            controllerId = controller?.controllerId?.value ?: cardComponent.ownerId.value,
            isTapped = state.hasComponent<TappedComponent>(entityId),
            summoningSickness = state.hasComponent<SummoningSicknessComponent>(entityId),
            counters = counters?.counters ?: emptyMap(),
            damageMarked = damage?.amount ?: 0,
            powerModifier = pt?.powerModifier ?: 0,
            toughnessModifier = pt?.toughnessModifier ?: 0,
            additionalKeywords = keywords?.added ?: emptySet(),
            removedKeywords = keywords?.removed ?: emptySet(),
            temporaryKeywords = keywords?.temporary ?: emptySet(),
            attachedTo = attachment?.targetId?.toCardId(),
            mustBeBlocked = state.hasComponent<MustBeBlockedComponent>(entityId)
        )
    }
}
