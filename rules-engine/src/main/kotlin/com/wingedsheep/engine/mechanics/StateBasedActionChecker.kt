package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.LossReason
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.ZoneType
import com.wingedsheep.sdk.model.EntityId

/**
 * Checks and applies state-based actions (Rule 704).
 *
 * State-based actions are game actions that happen automatically whenever
 * certain conditions are true. They don't use the stack.
 *
 * SBAs are checked:
 * - After a spell/ability resolves
 * - Before a player gets priority
 * - During cleanup step
 *
 * SBAs are checked repeatedly until none apply.
 */
class StateBasedActionChecker {

    private val stateProjector = StateProjector()

    /**
     * Check and apply all state-based actions until none apply.
     * Returns the new state and all events that occurred.
     */
    fun checkAndApply(state: GameState): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        // Keep checking until no SBAs apply
        var actionsApplied: Boolean
        do {
            val result = checkOnce(currentState)
            actionsApplied = result.events.isNotEmpty()
            currentState = result.newState
            allEvents.addAll(result.events)
        } while (actionsApplied)

        return ExecutionResult.success(currentState, allEvents)
    }

    /**
     * Check state-based actions once.
     */
    private fun checkOnce(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        // 704.5a - Player with 0 or less life loses
        val playerLossResults = checkPlayerLifeLoss(newState)
        newState = playerLossResults.newState
        events.addAll(playerLossResults.events)

        // 704.5b - Player with 10+ poison counters loses
        val poisonResults = checkPoisonLoss(newState)
        newState = poisonResults.newState
        events.addAll(poisonResults.events)

        // 704.5c - Player who attempted to draw from empty library loses
        // (Handled in DrawCardsExecutor which adds PlayerLostComponent)

        // 704.5f - Creature with toughness 0 or less goes to graveyard
        val zeroToughnessResults = checkZeroToughness(newState)
        newState = zeroToughnessResults.newState
        events.addAll(zeroToughnessResults.events)

        // 704.5g - Creature with lethal damage goes to graveyard
        val lethalDamageResults = checkLethalDamage(newState)
        newState = lethalDamageResults.newState
        events.addAll(lethalDamageResults.events)

        // 704.5h - Creature dealt damage by deathtouch source goes to graveyard
        // (Deathtouch tracking would need additional component)

        // 704.5i - Planeswalker with 0 loyalty goes to graveyard
        val planeswalkerResults = checkPlaneswalkerLoyalty(newState)
        newState = planeswalkerResults.newState
        events.addAll(planeswalkerResults.events)

        // 704.5j - If two or more legendary permanents with same name controlled by same player
        val legendResults = checkLegendRule(newState)
        newState = legendResults.newState
        events.addAll(legendResults.events)

        // 704.5m - +1/+1 and -1/-1 counters on same permanent annihilate
        val counterResults = checkCounterAnnihilation(newState)
        newState = counterResults.newState
        events.addAll(counterResults.events)

        // 704.5n - Aura not attached to legal permanent goes to graveyard
        val auraResults = checkUnattachedAuras(newState)
        newState = auraResults.newState
        events.addAll(auraResults.events)

        // 704.5p - Equipment/Fortification attached to illegal permanent becomes unattached
        // (Would need AttachedToComponent checking)

        // 704.5s - Token in non-battlefield zone ceases to exist
        val tokenResults = checkTokensInWrongZones(newState)
        newState = tokenResults.newState
        events.addAll(tokenResults.events)

        // Check for game end
        val gameEndResults = checkGameEnd(newState)
        newState = gameEndResults.newState
        events.addAll(gameEndResults.events)

        return ExecutionResult.success(newState, events)
    }

    /**
     * 704.5a - A player with 0 or less life loses the game.
     */
    private fun checkPlayerLifeLoss(state: GameState): ExecutionResult {
        // Skip if game is already over (prevents infinite loop in SBA checking)
        if (state.gameOver) {
            return ExecutionResult.success(state)
        }

        var newState = state
        val events = mutableListOf<GameEvent>()

        for (playerId in state.turnOrder) {
            val container = state.getEntity(playerId) ?: continue
            // Skip if player already marked as lost
            if (container.has<PlayerLostComponent>()) continue

            val lifeComponent = container.get<LifeTotalComponent>() ?: continue
            if (lifeComponent.life <= 0) {
                newState = newState.updateEntity(playerId) { c ->
                    c.with(PlayerLostComponent(LossReason.LIFE_ZERO))
                }
                events.add(PlayerLostEvent(playerId, GameEndReason.LIFE_ZERO))
            }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * 704.5b - A player with 10 or more poison counters loses the game.
     */
    private fun checkPoisonLoss(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (playerId in state.turnOrder) {
            val container = state.getEntity(playerId) ?: continue
            // Skip if player already marked as lost
            if (container.has<PlayerLostComponent>()) continue

            val counters = container.get<CountersComponent>() ?: continue
            val poisonCount = counters.getCount(CounterType.POISON)
            if (poisonCount >= 10) {
                newState = newState.updateEntity(playerId) { c ->
                    c.with(PlayerLostComponent(LossReason.POISON_COUNTERS))
                }
                events.add(PlayerLostEvent(playerId, GameEndReason.POISON_COUNTERS))
            }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * 704.5f - A creature with toughness 0 or less is put into its owner's graveyard.
     */
    private fun checkZeroToughness(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield().toList()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue

            val effectiveToughness = stateProjector.getProjectedToughness(state, entityId)

            if (effectiveToughness <= 0) {
                val result = putCreatureInGraveyard(newState, entityId, cardComponent, "zero toughness")
                newState = result.newState
                events.addAll(result.events)
            }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * 704.5g - A creature that's been dealt lethal damage is destroyed.
     */
    private fun checkLethalDamage(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield().toList()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val damageComponent = container.get<DamageComponent>() ?: continue

            if (!cardComponent.typeLine.isCreature) continue

            val effectiveToughness = stateProjector.getProjectedToughness(state, entityId)

            if (damageComponent.amount >= effectiveToughness) {
                val result = putCreatureInGraveyard(newState, entityId, cardComponent, "lethal damage")
                newState = result.newState
                events.addAll(result.events)
            }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * 704.5i - A planeswalker with 0 loyalty is put into its owner's graveyard.
     */
    private fun checkPlaneswalkerLoyalty(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield().toList()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Check if it's a planeswalker
            if (!cardComponent.typeLine.cardTypes.any { it.name == "PLANESWALKER" }) continue

            val counters = container.get<CountersComponent>()
            val loyalty = counters?.getCount(CounterType.LOYALTY) ?: 0

            if (loyalty <= 0) {
                val controllerId = container.get<ControllerComponent>()?.playerId
                    ?: cardComponent.ownerId
                    ?: continue
                val ownerId = cardComponent.ownerId ?: controllerId

                // Move to graveyard
                val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
                val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)

                newState = newState.removeFromZone(battlefieldZone, entityId)
                newState = newState.addToZone(graveyardZone, entityId)

                // Remove permanent components
                newState = newState.updateEntity(entityId) { c ->
                    c.without<ControllerComponent>()
                        .without<TappedComponent>()
                        .without<CountersComponent>()
                }

                events.add(
                    ZoneChangeEvent(
                        entityId,
                        cardComponent.name,
                        ZoneType.BATTLEFIELD,
                        ZoneType.GRAVEYARD,
                        ownerId
                    )
                )
            }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * 704.5j - Legend rule: If a player controls two or more legendary permanents
     * with the same name, that player chooses one and puts the rest into graveyard.
     */
    private fun checkLegendRule(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (playerId in state.turnOrder) {
            val battlefieldZone = ZoneKey(playerId, ZoneType.BATTLEFIELD)
            val permanents = state.getZone(battlefieldZone)

            // Group legendary permanents by name
            val legendaryByName = mutableMapOf<String, MutableList<EntityId>>()

            for (entityId in permanents) {
                val container = state.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                if (cardComponent.typeLine.isLegendary) {
                    legendaryByName.getOrPut(cardComponent.name) { mutableListOf() }.add(entityId)
                }
            }

            // For each name with duplicates, keep only one (first one for now - should be player choice)
            for ((_, entityIds) in legendaryByName) {
                if (entityIds.size > 1) {
                    // Keep first, sacrifice rest
                    for (i in 1 until entityIds.size) {
                        val entityId = entityIds[i]
                        val container = state.getEntity(entityId) ?: continue
                        val cardComponent = container.get<CardComponent>() ?: continue

                        val result = putCreatureInGraveyard(newState, entityId, cardComponent, "legend rule")
                        newState = result.newState
                        events.addAll(result.events)
                    }
                }
            }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * 704.5m - +1/+1 and -1/-1 counters on the same permanent annihilate in pairs.
     */
    private fun checkCounterAnnihilation(state: GameState): ExecutionResult {
        var newState = state

        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val counters = container.get<CountersComponent>() ?: continue

            val plusCounters = counters.getCount(CounterType.PLUS_ONE_PLUS_ONE)
            val minusCounters = counters.getCount(CounterType.MINUS_ONE_MINUS_ONE)

            if (plusCounters > 0 && minusCounters > 0) {
                val toRemove = minOf(plusCounters, minusCounters)
                val newCounters = counters
                    .withRemoved(CounterType.PLUS_ONE_PLUS_ONE, toRemove)
                    .withRemoved(CounterType.MINUS_ONE_MINUS_ONE, toRemove)

                newState = newState.updateEntity(entityId) { c ->
                    c.with(newCounters)
                }
            }
        }

        return ExecutionResult.success(newState)
    }

    /**
     * 704.5n - An Aura attached to an illegal object/player or not attached goes to graveyard.
     */
    private fun checkUnattachedAuras(state: GameState): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        for (entityId in state.getBattlefield().toList()) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            if (!cardComponent.typeLine.isAura) continue

            val attachedTo = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
            if (attachedTo == null) {
                // Aura not attached to anything - goes to graveyard
                val controllerId = container.get<ControllerComponent>()?.playerId
                    ?: cardComponent.ownerId
                    ?: continue
                val ownerId = cardComponent.ownerId ?: controllerId

                val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
                val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)

                newState = newState.removeFromZone(battlefieldZone, entityId)
                newState = newState.addToZone(graveyardZone, entityId)

                newState = newState.updateEntity(entityId) { c ->
                    c.without<ControllerComponent>()
                        .without<TappedComponent>()
                }

                events.add(
                    ZoneChangeEvent(
                        entityId,
                        cardComponent.name,
                        ZoneType.BATTLEFIELD,
                        ZoneType.GRAVEYARD,
                        ownerId
                    )
                )
            } else {
                // Check if attached target still exists on battlefield
                if (attachedTo.targetId !in state.getBattlefield()) {
                    val controllerId = container.get<ControllerComponent>()?.playerId
                        ?: cardComponent.ownerId
                        ?: continue
                    val ownerId = cardComponent.ownerId ?: controllerId

                    val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
                    val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)

                    newState = newState.removeFromZone(battlefieldZone, entityId)
                    newState = newState.addToZone(graveyardZone, entityId)

                    newState = newState.updateEntity(entityId) { c ->
                        c.without<ControllerComponent>()
                            .without<TappedComponent>()
                            .without<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                    }

                    events.add(
                        ZoneChangeEvent(
                            entityId,
                            cardComponent.name,
                            ZoneType.BATTLEFIELD,
                            ZoneType.GRAVEYARD,
                            ownerId
                        )
                    )
                }
            }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * 704.5s - Tokens in zones other than battlefield cease to exist.
     */
    private fun checkTokensInWrongZones(state: GameState): ExecutionResult {
        var newState = state

        // Check all entities for tokens not on battlefield
        val tokensToRemove = mutableListOf<Pair<EntityId, ZoneKey>>()

        for (playerId in state.turnOrder) {
            for (zoneType in listOf(ZoneType.HAND, ZoneType.GRAVEYARD, ZoneType.LIBRARY, ZoneType.EXILE)) {
                val zoneKey = ZoneKey(playerId, zoneType)
                for (entityId in state.getZone(zoneKey)) {
                    val container = state.getEntity(entityId) ?: continue
                    if (container.has<TokenComponent>()) {
                        tokensToRemove.add(entityId to zoneKey)
                    }
                }
            }
        }

        // Also check stack for token spells (shouldn't happen but just in case)
        for (entityId in state.stack) {
            val container = state.getEntity(entityId) ?: continue
            if (container.has<TokenComponent>()) {
                newState = newState.removeFromStack(entityId)
                newState = newState.withoutEntity(entityId)
            }
        }

        // Remove tokens from wrong zones
        for ((entityId, zoneKey) in tokensToRemove) {
            newState = newState.removeFromZone(zoneKey, entityId)
            newState = newState.withoutEntity(entityId)
        }

        return ExecutionResult.success(newState)
    }

    /**
     * Check if the game should end.
     */
    private fun checkGameEnd(state: GameState): ExecutionResult {
        // Skip if game is already over (prevents infinite loop in SBA checking)
        if (state.gameOver) {
            return ExecutionResult.success(state)
        }

        // Count players who haven't lost (no PlayerLostComponent)
        val activePlayers = state.turnOrder.filter { playerId ->
            val container = state.getEntity(playerId) ?: return@filter false
            !container.has<PlayerLostComponent>()
        }

        if (activePlayers.size == 1) {
            // One player remaining - they win
            val winner = activePlayers.first()
            // Determine the reason from the losing player
            val losingPlayer = state.turnOrder.find { it != winner }
            val lossComponent = losingPlayer?.let { state.getEntity(it)?.get<PlayerLostComponent>() }
            val reason = when (lossComponent?.reason) {
                LossReason.LIFE_ZERO -> GameEndReason.LIFE_ZERO
                LossReason.POISON_COUNTERS -> GameEndReason.POISON_COUNTERS
                LossReason.EMPTY_LIBRARY -> GameEndReason.DECK_EMPTY
                LossReason.CONCESSION -> GameEndReason.CONCESSION
                LossReason.CARD_EFFECT -> GameEndReason.CARD_EFFECT
                null -> GameEndReason.UNKNOWN
            }
            return ExecutionResult.success(
                state.copy(gameOver = true, winnerId = winner),
                listOf(GameEndedEvent(winner, reason))
            )
        } else if (activePlayers.isEmpty()) {
            // No players remaining - draw
            return ExecutionResult.success(
                state.copy(gameOver = true, winnerId = null),
                listOf(GameEndedEvent(null, GameEndReason.UNKNOWN))
            )
        }

        return ExecutionResult.success(state)
    }

    /**
     * Helper to move a creature to graveyard.
     */
    private fun putCreatureInGraveyard(
        state: GameState,
        entityId: EntityId,
        cardComponent: CardComponent,
        reason: String
    ): ExecutionResult {
        val container = state.getEntity(entityId) ?: return ExecutionResult.success(state)
        val controllerId = container.get<ControllerComponent>()?.playerId
            ?: cardComponent.ownerId
            ?: return ExecutionResult.success(state)

        val ownerId = cardComponent.ownerId ?: controllerId

        val battlefieldZone = ZoneKey(controllerId, ZoneType.BATTLEFIELD)
        val graveyardZone = ZoneKey(ownerId, ZoneType.GRAVEYARD)

        var newState = state.removeFromZone(battlefieldZone, entityId)
        newState = newState.addToZone(graveyardZone, entityId)

        // Remove permanent components
        newState = newState.updateEntity(entityId) { c ->
            c.without<ControllerComponent>()
                .without<TappedComponent>()
                .without<SummoningSicknessComponent>()
                .without<DamageComponent>()
                .without<CountersComponent>()
        }

        return ExecutionResult.success(
            newState,
            listOf(
                CreatureDestroyedEvent(entityId, cardComponent.name, reason),
                ZoneChangeEvent(
                    entityId,
                    cardComponent.name,
                    ZoneType.BATTLEFIELD,
                    ZoneType.GRAVEYARD,
                    ownerId
                )
            )
        )
    }
}
