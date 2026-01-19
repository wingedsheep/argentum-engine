package com.wingedsheep.rulesengine.ecs.action

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.AddColorlessManaEffect
import com.wingedsheep.rulesengine.ability.AddManaEffect
import com.wingedsheep.rulesengine.card.CounterType
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.combat.DamageEventProjector
import com.wingedsheep.rulesengine.ecs.combat.DamagePreventionEffect
import com.wingedsheep.rulesengine.ecs.combat.EcsCombatDamageCalculator
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.components.PendingLegendRuleChoice
import com.wingedsheep.rulesengine.ecs.stack.EcsStackResolver
import com.wingedsheep.rulesengine.zone.ZoneType

/**
 * Handles execution of EcsActions against EcsGameState.
 *
 * All operations are pure functions - each action produces a new state
 * without modifying the original. Events are generated for game logging
 * and trigger detection.
 *
 * Example usage:
 * ```kotlin
 * val handler = EcsActionHandler()
 * val result = handler.execute(state, EcsDrawCard(playerId, 2))
 * when (result) {
 *     is EcsActionResult.Success -> println("New state: ${result.state}")
 *     is EcsActionResult.Failure -> println("Error: ${result.reason}")
 * }
 * ```
 */
class EcsActionHandler {

    /**
     * Execute a single action and return the result.
     */
    fun execute(state: EcsGameState, action: EcsAction): EcsActionResult {
        return try {
            val (newState, events) = executeAction(state, action)
            EcsActionResult.Success(newState, action, events)
        } catch (e: IllegalStateException) {
            EcsActionResult.Failure(state, action, e.message ?: "Unknown error")
        } catch (e: IllegalArgumentException) {
            EcsActionResult.Failure(state, action, e.message ?: "Invalid argument")
        }
    }

    /**
     * Execute multiple actions in sequence.
     */
    fun executeAll(state: EcsGameState, actions: List<EcsAction>): EcsActionResult {
        var currentState = state
        val allEvents = mutableListOf<EcsActionEvent>()

        for (action in actions) {
            val result = execute(currentState, action)
            when (result) {
                is EcsActionResult.Success -> {
                    currentState = result.state
                    allEvents.addAll(result.events)
                }
                is EcsActionResult.Failure -> return result
            }
        }

        return if (actions.isNotEmpty()) {
            EcsActionResult.Success(currentState, actions.last(), allEvents)
        } else {
            EcsActionResult.Success(state, EcsCheckStateBasedActions(), emptyList())
        }
    }

    private fun executeAction(state: EcsGameState, action: EcsAction): Pair<EcsGameState, List<EcsActionEvent>> {
        val events = mutableListOf<EcsActionEvent>()

        val newState = when (action) {
            // Life actions
            is EcsGainLife -> executeGainLife(state, action, events)
            is EcsLoseLife -> executeLoseLife(state, action, events)
            is EcsSetLife -> executeSetLife(state, action, events)
            is EcsDealDamageToPlayer -> executeDealDamageToPlayer(state, action, events)
            is EcsDealDamageToCreature -> executeDealDamageToCreature(state, action, events)

            // Mana actions
            is EcsAddMana -> executeAddMana(state, action, events)
            is EcsAddColorlessMana -> executeAddColorlessMana(state, action, events)
            is EcsEmptyManaPool -> executeEmptyManaPool(state, action)
            is EcsActivateManaAbility -> executeActivateManaAbility(state, action, events)

            // Card drawing
            is EcsDrawCard -> executeDrawCard(state, action, events)
            is EcsDiscardCard -> executeDiscardCard(state, action, events)

            // Zone movement
            is EcsMoveEntity -> executeMoveEntity(state, action, events)
            is EcsPutOntoBattlefield -> executePutOntoBattlefield(state, action, events)
            is EcsDestroyPermanent -> executeDestroyPermanent(state, action, events)
            is EcsSacrificePermanent -> executeSacrificePermanent(state, action, events)
            is EcsExilePermanent -> executeExilePermanent(state, action, events)
            is EcsReturnToHand -> executeReturnToHand(state, action, events)

            // Tap/Untap
            is EcsTap -> executeTap(state, action, events)
            is EcsUntap -> executeUntap(state, action, events)
            is EcsUntapAll -> executeUntapAll(state, action, events)

            // Counters
            is EcsAddCounters -> executeAddCounters(state, action)
            is EcsRemoveCounters -> executeRemoveCounters(state, action)
            is EcsAddPoisonCounters -> executeAddPoisonCounters(state, action)

            // Summoning sickness
            is EcsRemoveSummoningSickness -> executeRemoveSummoningSickness(state, action)
            is EcsRemoveAllSummoningSickness -> executeRemoveAllSummoningSickness(state, action)

            // Land actions
            is EcsPlayLand -> executePlayLand(state, action, events)
            is EcsResetLandsPlayed -> executeResetLandsPlayed(state, action)

            // Library actions
            is EcsShuffleLibrary -> executeShuffleLibrary(state, action)

            // Combat actions
            is EcsBeginCombat -> executeBeginCombat(state, action, events)
            is EcsDeclareAttacker -> executeDeclareAttacker(state, action, events)
            is EcsDeclareBlocker -> executeDeclareBlocker(state, action, events)
            is EcsOrderBlockers -> executeOrderBlockers(state, action, events)
            is EcsResolveCombatDamage -> executeResolveCombatDamage(state, action, events)
            is EcsEndCombat -> executeEndCombat(state, action, events)

            // Game flow
            is EcsPassPriority -> executePassPriority(state, action)
            is EcsEndGame -> executeEndGame(state, action, events)
            is EcsPlayerLoses -> executePlayerLoses(state, action, events)
            is EcsResolveLegendRule -> executeResolveLegendRule(state, action, events)

            // Stack resolution
            is EcsResolveTopOfStack -> executeResolveTopOfStack(state, events)
            is EcsCastSpell -> executeCastSpell(state, action, events)

            // Attachment actions
            is EcsAttach -> executeAttach(state, action, events)
            is EcsDetach -> executeDetach(state, action, events)

            // State-based actions
            is EcsCheckStateBasedActions -> executeCheckStateBasedActions(state, events)
            is EcsClearDamage -> executeClearDamage(state, action)
        }

        return newState to events
    }

    // =========================================================================
    // Life Actions
    // =========================================================================

    private fun executeGainLife(
        state: EcsGameState,
        action: EcsGainLife,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val lifeComponent = container.get<LifeComponent>() ?: throw IllegalStateException("Player has no life component")

        val oldLife = lifeComponent.life
        val newLife = oldLife + action.amount
        events.add(EcsActionEvent.LifeChanged(action.playerId, oldLife, newLife))

        return state.updateEntity(action.playerId) { c ->
            c.with(lifeComponent.gainLife(action.amount))
        }
    }

    private fun executeLoseLife(
        state: EcsGameState,
        action: EcsLoseLife,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val lifeComponent = container.get<LifeComponent>() ?: throw IllegalStateException("Player has no life component")

        val oldLife = lifeComponent.life
        val newLife = oldLife - action.amount
        events.add(EcsActionEvent.LifeChanged(action.playerId, oldLife, newLife))

        return state.updateEntity(action.playerId) { c ->
            c.with(lifeComponent.loseLife(action.amount))
        }
    }

    private fun executeSetLife(
        state: EcsGameState,
        action: EcsSetLife,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val lifeComponent = container.get<LifeComponent>() ?: throw IllegalStateException("Player has no life component")

        val oldLife = lifeComponent.life
        events.add(EcsActionEvent.LifeChanged(action.playerId, oldLife, action.amount))

        return state.updateEntity(action.playerId) { c ->
            c.with(lifeComponent.setLife(action.amount))
        }
    }

    private fun executeDealDamageToPlayer(
        state: EcsGameState,
        action: EcsDealDamageToPlayer,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.targetPlayerId) ?: throw IllegalStateException("Target player not found")
        val lifeComponent = container.get<LifeComponent>() ?: throw IllegalStateException("Player has no life component")

        val oldLife = lifeComponent.life
        val newLife = oldLife - action.amount
        events.add(EcsActionEvent.DamageDealtToPlayer(action.sourceEntityId, action.targetPlayerId, action.amount))
        events.add(EcsActionEvent.LifeChanged(action.targetPlayerId, oldLife, newLife))

        return state.updateEntity(action.targetPlayerId) { c ->
            c.with(lifeComponent.loseLife(action.amount))
        }
    }

    private fun executeDealDamageToCreature(
        state: EcsGameState,
        action: EcsDealDamageToCreature,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.targetEntityId) ?: throw IllegalStateException("Target creature not found")
        val damageComponent = container.get<DamageComponent>() ?: DamageComponent(0)

        events.add(EcsActionEvent.DamageDealtToCreature(action.sourceEntityId, action.targetEntityId, action.amount))

        return state.updateEntity(action.targetEntityId) { c ->
            c.with(damageComponent.addDamage(action.amount))
        }
    }

    // =========================================================================
    // Mana Actions
    // =========================================================================

    private fun executeAddMana(
        state: EcsGameState,
        action: EcsAddMana,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()

        events.add(EcsActionEvent.ManaAdded(action.playerId, action.color.displayName, action.amount))

        return state.updateEntity(action.playerId) { c ->
            c.with(manaPool.add(action.color, action.amount))
        }
    }

    private fun executeAddColorlessMana(
        state: EcsGameState,
        action: EcsAddColorlessMana,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()

        events.add(EcsActionEvent.ManaAdded(action.playerId, "Colorless", action.amount))

        return state.updateEntity(action.playerId) { c ->
            c.with(manaPool.addColorless(action.amount))
        }
    }

    private fun executeEmptyManaPool(state: EcsGameState, action: EcsEmptyManaPool): EcsGameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val manaPool = container.get<ManaPoolComponent>() ?: return state

        return state.updateEntity(action.playerId) { c ->
            c.with(manaPool.empty())
        }
    }

    /**
     * Execute a mana ability activation.
     *
     * Mana abilities resolve immediately without using the stack (Rule 605).
     * This handles:
     * 1. Validating the ability exists and can be activated
     * 2. Paying the cost (typically tapping)
     * 3. Adding mana to the player's pool
     */
    private fun executeActivateManaAbility(
        state: EcsGameState,
        action: EcsActivateManaAbility,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val sourceId = action.sourceEntityId

        // Verify source is on the battlefield
        if (sourceId !in state.getBattlefield()) {
            throw IllegalStateException("Mana ability source must be on the battlefield")
        }

        val container = state.getEntity(sourceId)
            ?: throw IllegalStateException("Source entity not found")

        // Verify player controls the permanent
        val controller = container.get<ControllerComponent>()
            ?: throw IllegalStateException("Source has no controller")
        if (controller.controllerId != action.playerId) {
            throw IllegalStateException("Player does not control this permanent")
        }

        // Get the abilities component
        val abilities = container.get<AbilitiesComponent>()
            ?: throw IllegalStateException("Source has no abilities")

        // Get the specific mana ability
        val ability = abilities.getManaAbility(action.abilityIndex)
            ?: throw IllegalStateException("Mana ability not found at index ${action.abilityIndex}")

        if (!ability.isManaAbility) {
            throw IllegalStateException("Ability at index ${action.abilityIndex} is not a mana ability")
        }

        var currentState = state

        // Pay the cost
        currentState = payAbilityCost(currentState, sourceId, ability.cost, action.playerId, events)

        // Execute the effect (add mana)
        currentState = executeManaEffect(currentState, action.playerId, ability.effect, events)

        return currentState
    }

    /**
     * Pay the cost of an ability.
     */
    private fun payAbilityCost(
        state: EcsGameState,
        sourceId: EntityId,
        cost: AbilityCost,
        playerId: EntityId,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        return when (cost) {
            is AbilityCost.Tap -> {
                // Check if already tapped
                val container = state.getEntity(sourceId)!!
                if (container.get<TappedComponent>() != null) {
                    throw IllegalStateException("Permanent is already tapped")
                }
                val cardName = container.get<CardComponent>()?.name ?: "Unknown"
                events.add(EcsActionEvent.PermanentTapped(sourceId, cardName))
                state.updateEntity(sourceId) { c -> c.with(TappedComponent) }
            }
            is AbilityCost.Composite -> {
                // Pay all costs in the composite
                cost.costs.fold(state) { s, subCost ->
                    payAbilityCost(s, sourceId, subCost, playerId, events)
                }
            }
            is AbilityCost.Mana -> {
                // For mana abilities that cost mana (rare but possible)
                // This would deduct from the player's mana pool
                // For simplicity, basic lands don't have mana costs
                state
            }
            is AbilityCost.PayLife -> {
                val container = state.getEntity(playerId) ?: throw IllegalStateException("Player not found")
                val lifeComp = container.get<LifeComponent>() ?: throw IllegalStateException("Player has no life component")
                if (lifeComp.life < cost.amount) {
                    throw IllegalStateException("Not enough life to pay cost")
                }
                val newLife = lifeComp.life - cost.amount
                events.add(EcsActionEvent.LifeChanged(playerId, lifeComp.life, newLife))
                state.updateEntity(playerId) { c -> c.with(lifeComp.loseLife(cost.amount)) }
            }
            is AbilityCost.Sacrifice -> {
                // Would need to handle sacrifice - for now, not used by basic mana abilities
                throw IllegalStateException("Sacrifice cost not supported for mana abilities yet")
            }
            is AbilityCost.Discard -> {
                // Would need to handle discard - for now, not used by basic mana abilities
                throw IllegalStateException("Discard cost not supported for mana abilities yet")
            }
        }
    }

    /**
     * Execute a mana-producing effect.
     */
    private fun executeManaEffect(
        state: EcsGameState,
        playerId: EntityId,
        effect: com.wingedsheep.rulesengine.ability.Effect,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        return when (effect) {
            is AddManaEffect -> {
                val container = state.getEntity(playerId) ?: throw IllegalStateException("Player not found")
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                events.add(EcsActionEvent.ManaAdded(playerId, effect.color.displayName, effect.amount))
                state.updateEntity(playerId) { c ->
                    c.with(manaPool.add(effect.color, effect.amount))
                }
            }
            is AddColorlessManaEffect -> {
                val container = state.getEntity(playerId) ?: throw IllegalStateException("Player not found")
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                events.add(EcsActionEvent.ManaAdded(playerId, "Colorless", effect.amount))
                state.updateEntity(playerId) { c ->
                    c.with(manaPool.addColorless(effect.amount))
                }
            }
            else -> {
                throw IllegalStateException("Unsupported effect type for mana ability: ${effect::class.simpleName}")
            }
        }
    }

    // =========================================================================
    // Card Drawing
    // =========================================================================

    private fun executeDrawCard(
        state: EcsGameState,
        action: EcsDrawCard,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        var currentState = state
        val libraryZone = ZoneId.library(action.playerId)
        val handZone = ZoneId.hand(action.playerId)

        repeat(action.count) {
            val library = currentState.getZone(libraryZone)
            if (library.isEmpty()) {
                events.add(EcsActionEvent.DrawFailed(action.playerId))
                // Mark player as lost due to drawing from empty library
                currentState = currentState.updateEntity(action.playerId) { c ->
                    c.with(LostGameComponent.decked())
                }
                return currentState
            }

            val cardId = library.first()
            val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"

            events.add(EcsActionEvent.CardDrawn(action.playerId, cardId, cardName))

            currentState = currentState
                .removeFromZone(cardId, libraryZone)
                .addToZone(cardId, handZone)
        }

        return currentState
    }

    private fun executeDiscardCard(
        state: EcsGameState,
        action: EcsDiscardCard,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val handZone = ZoneId.hand(action.playerId)
        val graveyardZone = ZoneId.graveyard(action.playerId)

        if (!state.isInZone(action.cardId, handZone)) {
            throw IllegalStateException("Card not in hand")
        }

        val cardName = state.getEntity(action.cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(EcsActionEvent.CardDiscarded(action.playerId, action.cardId, cardName))

        return state
            .removeFromZone(action.cardId, handZone)
            .addToZone(action.cardId, graveyardZone)
    }

    // =========================================================================
    // Zone Movement
    // =========================================================================

    private fun executeMoveEntity(
        state: EcsGameState,
        action: EcsMoveEntity,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val cardName = state.getEntity(action.entityId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(EcsActionEvent.CardMoved(action.entityId, cardName, action.fromZone, action.toZone))

        var newState = state.removeFromZone(action.entityId, action.fromZone)
        newState = if (action.toTop) {
            newState.addToZone(action.entityId, action.toZone)
        } else {
            newState.addToZoneBottom(action.entityId, action.toZone)
        }

        return newState
    }

    private fun executePutOntoBattlefield(
        state: EcsGameState,
        action: EcsPutOntoBattlefield,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.entityId) ?: throw IllegalStateException("Entity not found")
        val cardComponent = container.get<CardComponent>() ?: throw IllegalStateException("Entity has no card component")

        val cardName = cardComponent.definition.name
        val currentZone = state.findZone(action.entityId)

        if (currentZone != null) {
            events.add(EcsActionEvent.CardMoved(action.entityId, cardName, currentZone, ZoneId.BATTLEFIELD))
        }

        // Remove from current zone and add to battlefield
        var newState = if (currentZone != null) {
            state.removeFromZone(action.entityId, currentZone)
        } else {
            state
        }

        newState = newState.addToZone(action.entityId, ZoneId.BATTLEFIELD)

        // Set controller
        newState = newState.updateEntity(action.entityId) { c ->
            var updated = c.with(ControllerComponent(action.controllerId))

            // Add tapped component if needed
            if (action.tapped) {
                updated = updated.with(TappedComponent)
            }

            // Add summoning sickness if creature
            if (cardComponent.definition.isCreature) {
                updated = updated.with(SummoningSicknessComponent)
            }

            updated
        }

        return newState
    }

    private fun executeDestroyPermanent(
        state: EcsGameState,
        action: EcsDestroyPermanent,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.entityId) ?: throw IllegalStateException("Entity not found")
        val cardComponent = container.get<CardComponent>() ?: throw IllegalStateException("Entity has no card component")

        val cardName = cardComponent.definition.name
        val graveyardZone = ZoneId.graveyard(cardComponent.ownerId)

        events.add(EcsActionEvent.PermanentDestroyed(action.entityId, cardName))
        if (cardComponent.definition.isCreature) {
            events.add(EcsActionEvent.CreatureDied(action.entityId, cardName, cardComponent.ownerId))
        }

        // Move to graveyard and clear damage
        return state
            .removeFromZone(action.entityId, ZoneId.BATTLEFIELD)
            .addToZone(action.entityId, graveyardZone)
            .updateEntity(action.entityId) { c ->
                c.without<DamageComponent>()
                    .without<TappedComponent>()
                    .without<SummoningSicknessComponent>()
            }
    }

    private fun executeSacrificePermanent(
        state: EcsGameState,
        action: EcsSacrificePermanent,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        return executeDestroyPermanent(state, EcsDestroyPermanent(action.entityId), events)
    }

    private fun executeExilePermanent(
        state: EcsGameState,
        action: EcsExilePermanent,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.entityId) ?: throw IllegalStateException("Entity not found")
        val cardComponent = container.get<CardComponent>() ?: throw IllegalStateException("Entity has no card component")

        val cardName = cardComponent.definition.name
        val currentZone = state.findZone(action.entityId) ?: throw IllegalStateException("Entity not in any zone")

        events.add(EcsActionEvent.CardExiled(action.entityId, cardName))

        return state
            .removeFromZone(action.entityId, currentZone)
            .addToZone(action.entityId, ZoneId.EXILE)
    }

    private fun executeReturnToHand(
        state: EcsGameState,
        action: EcsReturnToHand,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.entityId) ?: throw IllegalStateException("Entity not found")
        val cardComponent = container.get<CardComponent>() ?: throw IllegalStateException("Entity has no card component")

        val cardName = cardComponent.definition.name
        val currentZone = state.findZone(action.entityId) ?: throw IllegalStateException("Entity not in any zone")
        val ownerHand = ZoneId.hand(cardComponent.ownerId)

        events.add(EcsActionEvent.CardReturnedToHand(action.entityId, cardName))

        return state
            .removeFromZone(action.entityId, currentZone)
            .addToZone(action.entityId, ownerHand)
    }

    // =========================================================================
    // Tap/Untap
    // =========================================================================

    private fun executeTap(
        state: EcsGameState,
        action: EcsTap,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val cardName = state.getEntity(action.entityId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(EcsActionEvent.PermanentTapped(action.entityId, cardName))

        return state.updateEntity(action.entityId) { c -> c.with(TappedComponent) }
    }

    private fun executeUntap(
        state: EcsGameState,
        action: EcsUntap,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val cardName = state.getEntity(action.entityId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(EcsActionEvent.PermanentUntapped(action.entityId, cardName))

        return state.updateEntity(action.entityId) { c -> c.without<TappedComponent>() }
    }

    private fun executeUntapAll(
        state: EcsGameState,
        action: EcsUntapAll,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        var currentState = state

        for (entityId in state.getPermanentsControlledBy(action.controllerId)) {
            if (currentState.hasComponent<TappedComponent>(entityId)) {
                val cardName = currentState.getEntity(entityId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
                events.add(EcsActionEvent.PermanentUntapped(entityId, cardName))
                currentState = currentState.updateEntity(entityId) { c -> c.without<TappedComponent>() }
            }
        }

        return currentState
    }

    // =========================================================================
    // Counters
    // =========================================================================

    private fun executeAddCounters(state: EcsGameState, action: EcsAddCounters): EcsGameState {
        val counterType = try {
            CounterType.valueOf(
                action.counterType.uppercase()
                    .replace(" ", "_")
                    .replace("+1/+1", "PLUS_ONE_PLUS_ONE")
                    .replace("-1/-1", "MINUS_ONE_MINUS_ONE")
            )
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Unknown counter type: ${action.counterType}")
        }

        val container = state.getEntity(action.entityId) ?: throw IllegalStateException("Entity not found")
        val counters = container.get<CountersComponent>() ?: CountersComponent()

        return state.updateEntity(action.entityId) { c ->
            c.with(counters.add(counterType, action.amount))
        }
    }

    private fun executeRemoveCounters(state: EcsGameState, action: EcsRemoveCounters): EcsGameState {
        val counterType = try {
            CounterType.valueOf(
                action.counterType.uppercase()
                    .replace(" ", "_")
                    .replace("+1/+1", "PLUS_ONE_PLUS_ONE")
                    .replace("-1/-1", "MINUS_ONE_MINUS_ONE")
            )
        } catch (e: IllegalArgumentException) {
            throw IllegalStateException("Unknown counter type: ${action.counterType}")
        }

        val container = state.getEntity(action.entityId) ?: throw IllegalStateException("Entity not found")
        val counters = container.get<CountersComponent>() ?: return state

        return state.updateEntity(action.entityId) { c ->
            c.with(counters.remove(counterType, action.amount))
        }
    }

    private fun executeAddPoisonCounters(state: EcsGameState, action: EcsAddPoisonCounters): EcsGameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val poison = container.get<PoisonComponent>() ?: PoisonComponent()

        return state.updateEntity(action.playerId) { c ->
            c.with(poison.add(action.amount))
        }
    }

    // =========================================================================
    // Summoning Sickness
    // =========================================================================

    private fun executeRemoveSummoningSickness(state: EcsGameState, action: EcsRemoveSummoningSickness): EcsGameState {
        return state.updateEntity(action.entityId) { c -> c.without<SummoningSicknessComponent>() }
    }

    private fun executeRemoveAllSummoningSickness(state: EcsGameState, action: EcsRemoveAllSummoningSickness): EcsGameState {
        var currentState = state

        for (creatureId in state.getCreaturesControlledBy(action.controllerId)) {
            if (currentState.hasComponent<SummoningSicknessComponent>(creatureId)) {
                currentState = currentState.updateEntity(creatureId) { c ->
                    c.without<SummoningSicknessComponent>()
                }
            }
        }

        return currentState
    }

    // =========================================================================
    // Land Actions
    // =========================================================================

    private fun executePlayLand(
        state: EcsGameState,
        action: EcsPlayLand,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val playerContainer = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val landsPlayed = playerContainer.get<LandsPlayedComponent>() ?: LandsPlayedComponent()

        if (!landsPlayed.canPlayLand) {
            throw IllegalStateException("Cannot play another land this turn")
        }

        val cardContainer = state.getEntity(action.cardId) ?: throw IllegalStateException("Card not found")
        val cardComponent = cardContainer.get<CardComponent>() ?: throw IllegalStateException("Entity has no card component")

        if (!cardComponent.definition.isLand) {
            throw IllegalStateException("Card is not a land")
        }

        val handZone = ZoneId.hand(action.playerId)
        if (!state.isInZone(action.cardId, handZone)) {
            throw IllegalStateException("Card not in hand")
        }

        events.add(EcsActionEvent.LandPlayed(action.playerId, action.cardId, cardComponent.definition.name))

        return state
            .removeFromZone(action.cardId, handZone)
            .addToZone(action.cardId, ZoneId.BATTLEFIELD)
            .updateEntity(action.cardId) { c ->
                c.with(ControllerComponent(action.playerId))
            }
            .updateEntity(action.playerId) { c ->
                c.with(landsPlayed.playLand())
            }
    }

    private fun executeResetLandsPlayed(state: EcsGameState, action: EcsResetLandsPlayed): EcsGameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val landsPlayed = container.get<LandsPlayedComponent>() ?: return state

        return state.updateEntity(action.playerId) { c ->
            c.with(landsPlayed.reset())
        }
    }

    // =========================================================================
    // Library Actions
    // =========================================================================

    private fun executeShuffleLibrary(state: EcsGameState, action: EcsShuffleLibrary): EcsGameState {
        return state.shuffleZone(ZoneId.library(action.playerId))
    }

    // =========================================================================
    // Combat Actions
    // =========================================================================

    private fun executeBeginCombat(
        state: EcsGameState,
        action: EcsBeginCombat,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        events.add(EcsActionEvent.CombatStarted(action.attackingPlayerId, action.defendingPlayerId))
        return state.startCombat(action.defendingPlayerId)
    }

    private fun executeDeclareAttacker(
        state: EcsGameState,
        action: EcsDeclareAttacker,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val combat = state.combat ?: throw IllegalStateException("Not in combat")
        val container = state.getEntity(action.creatureId) ?: throw IllegalStateException("Creature not found")

        // Verify creature can attack
        if (!container.has<CardComponent>()) {
            throw IllegalStateException("Entity is not a card")
        }

        if (container.has<TappedComponent>()) {
            throw IllegalStateException("Creature is tapped")
        }

        if (container.has<SummoningSicknessComponent>()) {
            // Check for haste - would need to check keywords
            // For now, throw exception
            throw IllegalStateException("Creature has summoning sickness")
        }

        val cardName = container.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(EcsActionEvent.AttackerDeclared(action.creatureId, cardName))

        val newCombat = combat.addAttacker(action.creatureId)

        // Tap the attacker (unless has vigilance - would need keyword check)
        return state
            .copy(combat = newCombat)
            .updateEntity(action.creatureId) { c ->
                c.with(TappedComponent)
                    .with(AttackingComponent.attackingPlayer(combat.defendingPlayer))
            }
    }

    private fun executeDeclareBlocker(
        state: EcsGameState,
        action: EcsDeclareBlocker,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val combat = state.combat ?: throw IllegalStateException("Not in combat")
        val container = state.getEntity(action.blockerId) ?: throw IllegalStateException("Blocker not found")

        if (container.has<TappedComponent>()) {
            throw IllegalStateException("Creature is tapped and cannot block")
        }

        val cardName = container.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(EcsActionEvent.BlockerDeclared(action.blockerId, action.attackerId, cardName))

        val newCombat = combat.addBlocker(action.blockerId, action.attackerId)

        return state
            .copy(combat = newCombat)
            .updateEntity(action.blockerId) { c ->
                c.with(BlockingComponent(action.attackerId))
            }
            // Also add to the attacker's BlockedByComponent
            .updateEntity(action.attackerId) { c ->
                val existing = c.get<BlockedByComponent>()
                if (existing != null) {
                    c.with(existing.addBlocker(action.blockerId))
                } else {
                    c.with(BlockedByComponent(listOf(action.blockerId)))
                }
            }
    }

    private fun executeOrderBlockers(
        state: EcsGameState,
        action: EcsOrderBlockers,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val combat = state.combat ?: throw IllegalStateException("Not in combat")

        // Verify this is the attacking player
        if (combat.attackingPlayer != action.playerId) {
            throw IllegalStateException("Only the attacking player can order blockers")
        }

        // Verify the attacker is actually attacking
        val attackingComponent = state.getComponent<AttackingComponent>(action.attackerId)
            ?: throw IllegalStateException("Creature is not attacking")

        // Verify the blockers are actually blocking this attacker
        val currentBlockedBy = state.getComponent<BlockedByComponent>(action.attackerId)
        if (currentBlockedBy == null) {
            throw IllegalStateException("This attacker has no blockers")
        }

        // Verify the ordered blockers match the actual blockers
        if (action.orderedBlockerIds.toSet() != currentBlockedBy.blockerIds.toSet()) {
            throw IllegalStateException("Ordered blockers don't match actual blockers")
        }

        events.add(EcsActionEvent.BlockersOrdered(action.attackerId, action.orderedBlockerIds))

        // Update the BlockedByComponent with the new order
        return state.updateEntity(action.attackerId) { c ->
            c.with(currentBlockedBy.setOrder(action.orderedBlockerIds))
        }
    }

    private fun executeResolveCombatDamage(
        state: EcsGameState,
        action: EcsResolveCombatDamage,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val combat = state.combat ?: throw IllegalStateException("Not in combat")

        // Calculate damage based on the step
        val damageStep = when (action.step) {
            CombatDamageStep.FIRST_STRIKE -> EcsCombatDamageCalculator.DamageStep.FIRST_STRIKE
            CombatDamageStep.REGULAR -> EcsCombatDamageCalculator.DamageStep.REGULAR
        }

        val damageResult = when (damageStep) {
            EcsCombatDamageCalculator.DamageStep.FIRST_STRIKE ->
                EcsCombatDamageCalculator.calculateFirstStrikeDamage(state)
            EcsCombatDamageCalculator.DamageStep.REGULAR ->
                EcsCombatDamageCalculator.calculateRegularDamage(state)
        }

        // Apply prevention effects if any
        val preventionEffects = action.preventionEffectIds.mapNotNull { sourceId ->
            // For now, treat each sourceId as a "prevent all combat damage" effect
            // A more sophisticated implementation would look up the actual effect type
            DamagePreventionEffect.PreventAllCombatDamage(sourceId, "Prevention effect")
        }

        val projector = DamageEventProjector(state, preventionEffects)
        val projectedDamage = projector.project(damageResult.damageEvents)

        // Apply the damage
        var newState = state

        for (event in projectedDamage.finalEvents) {
            when (event) {
                is EcsCombatDamageCalculator.PendingDamageEvent.ToPlayer -> {
                    val lifeComponent = newState.getComponent<LifeComponent>(event.targetPlayerId)
                    if (lifeComponent != null) {
                        val oldLife = lifeComponent.life
                        val newLife = oldLife - event.amount
                        events.add(EcsActionEvent.DamageDealt(
                            event.sourceId,
                            event.targetPlayerId,
                            event.amount,
                            isCombatDamage = true
                        ))
                        newState = newState.updateEntity(event.targetPlayerId) { c ->
                            c.with(lifeComponent.copy(life = newLife))
                        }
                    }
                }
                is EcsCombatDamageCalculator.PendingDamageEvent.ToPlaneswalker -> {
                    // Remove loyalty counters from planeswalker
                    val counters = newState.getComponent<CountersComponent>(event.targetPlaneswalker)
                    if (counters != null) {
                        val currentLoyalty = counters.counters[CounterType.LOYALTY] ?: 0
                        val newLoyalty = (currentLoyalty - event.amount).coerceAtLeast(0)
                        events.add(EcsActionEvent.DamageDealt(
                            event.sourceId,
                            event.targetPlaneswalker,
                            event.amount,
                            isCombatDamage = true
                        ))
                        newState = newState.updateEntity(event.targetPlaneswalker) { c ->
                            c.with(counters.remove(CounterType.LOYALTY, event.amount))
                        }
                    }
                }
                is EcsCombatDamageCalculator.PendingDamageEvent.ToCreature -> {
                    val damageComponent = newState.getComponent<DamageComponent>(event.targetCreatureId)
                    val currentDamage = damageComponent?.amount ?: 0
                    val newDamage = currentDamage + event.amount
                    events.add(EcsActionEvent.DamageDealt(
                        event.sourceId,
                        event.targetCreatureId,
                        event.amount,
                        isCombatDamage = true
                    ))
                    newState = newState.updateEntity(event.targetCreatureId) { c ->
                        c.with(DamageComponent(newDamage))
                    }
                }
            }
        }

        // If this was first strike damage, mark creatures as having dealt first strike damage
        if (damageStep == EcsCombatDamageCalculator.DamageStep.FIRST_STRIKE) {
            newState = EcsCombatDamageCalculator.markFirstStrikeDamageDealt(
                newState,
                damageResult.creaturesDealtDamage
            )
        }

        return newState
    }

    private fun executeEndCombat(
        state: EcsGameState,
        action: EcsEndCombat,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        events.add(EcsActionEvent.CombatEnded(action.playerId))

        // Remove combat components from all creatures
        var newState = state
        for (entityId in state.getBattlefield()) {
            val container = newState.getEntity(entityId) ?: continue
            if (container.has<AttackingComponent>() || container.has<BlockingComponent>()) {
                newState = newState.updateEntity(entityId) { c ->
                    c.without<AttackingComponent>().without<BlockingComponent>()
                }
            }
        }

        return newState.endCombat()
    }

    // =========================================================================
    // Game Flow
    // =========================================================================

    private fun executePassPriority(state: EcsGameState, action: EcsPassPriority): EcsGameState {
        val turnState = state.turnState

        // Verify the correct player is passing
        if (turnState.priorityPlayer != action.playerId) {
            throw IllegalStateException("Player ${action.playerId} cannot pass priority - it's ${turnState.priorityPlayer}'s priority")
        }

        // Increment consecutive passes and move priority to next player
        val newTurnState = turnState
            .incrementConsecutivePasses()
            .passPriority()

        return state.copy(turnState = newTurnState)
    }

    private fun executeEndGame(
        state: EcsGameState,
        action: EcsEndGame,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        events.add(EcsActionEvent.GameEnded(action.winnerId))
        return state.endGame(action.winnerId)
    }

    private fun executePlayerLoses(
        state: EcsGameState,
        action: EcsPlayerLoses,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        events.add(EcsActionEvent.PlayerLost(action.playerId, action.reason))
        return state.updateEntity(action.playerId) { c ->
            c.with(LostGameComponent(action.reason))
        }
    }

    /**
     * Resolve a Legend Rule choice by keeping the chosen legendary
     * and putting all others with the same name into the graveyard.
     */
    private fun executeResolveLegendRule(
        state: EcsGameState,
        action: EcsResolveLegendRule,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        // Find the pending choice
        val pendingChoice = state.pendingLegendRuleChoices.find {
            it.controllerId == action.controllerId && it.legendaryName == action.legendaryName
        } ?: throw IllegalStateException("No pending Legend Rule choice for ${action.legendaryName}")

        // Validate the choice
        if (action.keepEntityId !in pendingChoice.duplicateIds) {
            throw IllegalStateException("${action.keepEntityId} is not one of the duplicate legendaries")
        }

        var currentState = state

        // Put all others into the graveyard
        val toSacrifice = pendingChoice.duplicateIds.filter { it != action.keepEntityId }
        for (entityId in toSacrifice) {
            val cardComp = currentState.getEntity(entityId)?.get<CardComponent>()
            if (cardComp != null && entityId in currentState.getBattlefield()) {
                val graveyardZone = ZoneId.graveyard(cardComp.ownerId)
                events.add(EcsActionEvent.PermanentDestroyed(entityId, cardComp.name))
                currentState = currentState
                    .removeFromZone(entityId, ZoneId.BATTLEFIELD)
                    .addToZone(entityId, graveyardZone)
            }
        }

        // Remove the pending choice
        currentState = currentState.removePendingLegendRuleChoice(pendingChoice)

        return currentState
    }

    // =========================================================================
    // Stack Resolution
    // =========================================================================

    private val stackResolver = EcsStackResolver()

    private fun executeResolveTopOfStack(
        state: EcsGameState,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val result = stackResolver.resolveTopOfStack(state)

        return when (result) {
            is EcsStackResolver.ResolutionResult.Resolved -> {
                // Convert stack resolution events to action events
                for (stackEvent in result.events) {
                    events.addAll(convertStackEvent(stackEvent))
                }
                result.state
            }
            is EcsStackResolver.ResolutionResult.Fizzled -> {
                for (stackEvent in result.events) {
                    events.addAll(convertStackEvent(stackEvent))
                }
                result.state
            }
            is EcsStackResolver.ResolutionResult.EmptyStack -> {
                // Nothing to resolve
                state
            }
            is EcsStackResolver.ResolutionResult.Error -> {
                throw IllegalStateException("Stack resolution error: ${result.message}")
            }
        }
    }

    private fun executeCastSpell(
        state: EcsGameState,
        action: EcsCastSpell,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val container = state.getEntity(action.cardId)
            ?: throw IllegalStateException("Card not found: ${action.cardId}")
        val cardComponent = container.get<CardComponent>()
            ?: throw IllegalStateException("CardComponent missing")

        events.add(EcsActionEvent.SpellCast(action.cardId, cardComponent.name, action.casterId))

        // Remove from source zone
        var newState = state.removeFromZone(action.cardId, action.fromZone)

        // Add to stack with SpellOnStackComponent
        newState = newState.updateEntity(action.cardId) { c ->
            c.with(
                SpellOnStackComponent(
                    casterId = action.casterId,
                    targets = action.targets,
                    xValue = action.xValue
                )
            )
        }

        // Add to stack zone
        newState = newState.addToStack(action.cardId)

        return newState
    }

    /**
     * Convert stack resolution events to action events.
     */
    private fun convertStackEvent(
        event: EcsStackResolver.StackResolutionEvent
    ): List<EcsActionEvent> {
        return when (event) {
            is EcsStackResolver.StackResolutionEvent.SpellResolved ->
                listOf(EcsActionEvent.SpellResolved(event.entityId, event.name))
            is EcsStackResolver.StackResolutionEvent.SpellFizzled ->
                listOf(EcsActionEvent.SpellFizzled(event.entityId, event.name, event.reason))
            is EcsStackResolver.StackResolutionEvent.PermanentEnteredBattlefield ->
                listOf(EcsActionEvent.PermanentEnteredBattlefield(event.entityId, event.name, event.controllerId))
            is EcsStackResolver.StackResolutionEvent.SpellMovedToGraveyard ->
                listOf(EcsActionEvent.CardMoved(event.entityId, event.name, ZoneId.STACK, ZoneId.graveyard(event.ownerId)))
            is EcsStackResolver.StackResolutionEvent.AbilityResolved ->
                listOf(EcsActionEvent.AbilityResolved(event.description, event.sourceId))
            is EcsStackResolver.StackResolutionEvent.AbilityFizzled ->
                listOf(EcsActionEvent.AbilityFizzled(event.description, event.sourceId, event.reason))
            is EcsStackResolver.StackResolutionEvent.EffectEvent ->
                emptyList() // Effect events are handled separately
        }
    }

    // =========================================================================
    // Attachment Actions
    // =========================================================================

    private fun executeAttach(
        state: EcsGameState,
        action: EcsAttach,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val attachmentName = state.getEntity(action.attachmentId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        val targetName = state.getEntity(action.targetId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(EcsActionEvent.Attached(action.attachmentId, attachmentName, action.targetId, targetName))

        return state.updateEntity(action.attachmentId) { c ->
            c.with(AttachedToComponent(action.targetId))
        }
    }

    private fun executeDetach(
        state: EcsGameState,
        action: EcsDetach,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        val attachmentName = state.getEntity(action.attachmentId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(EcsActionEvent.Detached(action.attachmentId, attachmentName))

        return state.updateEntity(action.attachmentId) { c ->
            c.without<AttachedToComponent>()
        }
    }

    // =========================================================================
    // State-Based Actions
    // =========================================================================

    private fun executeCheckStateBasedActions(
        state: EcsGameState,
        events: MutableList<EcsActionEvent>
    ): EcsGameState {
        var currentState = state
        var actionsPerformed: Boolean

        do {
            actionsPerformed = false

            // Check creatures with lethal damage
            for (entityId in currentState.getBattlefield()) {
                val container = currentState.getEntity(entityId) ?: continue
                val cardComponent = container.get<CardComponent>() ?: continue

                if (!cardComponent.definition.isCreature) continue

                val damage = container.get<DamageComponent>()?.amount ?: 0
                val toughness = cardComponent.definition.creatureStats?.baseToughness ?: 0
                // Note: This doesn't account for modifiers - in practice, use StateProjector

                // Creature dies if: toughness is 0 or less, OR damage >= toughness (with toughness > 0)
                if (toughness <= 0 || damage >= toughness) {
                    val graveyardZone = ZoneId.graveyard(cardComponent.ownerId)
                    events.add(EcsActionEvent.CreatureDied(entityId, cardComponent.definition.name, cardComponent.ownerId))

                    currentState = currentState
                        .removeFromZone(entityId, ZoneId.BATTLEFIELD)
                        .addToZone(entityId, graveyardZone)
                        .updateEntity(entityId) { c -> c.without<DamageComponent>() }

                    actionsPerformed = true
                }
            }

            // Check players with 0 or less life
            for (playerId in currentState.getPlayerIds()) {
                val container = currentState.getEntity(playerId) ?: continue
                val life = container.get<LifeComponent>() ?: continue
                val alreadyLost = container.has<LostGameComponent>()

                if (life.isAtZeroOrLess && !alreadyLost) {
                    events.add(EcsActionEvent.PlayerLost(playerId, "Life total reached 0 or less"))
                    currentState = currentState.updateEntity(playerId) { c ->
                        c.with(LostGameComponent.zeroLife())
                    }
                    actionsPerformed = true
                }
            }

            // Check players with 10+ poison counters
            for (playerId in currentState.getPlayerIds()) {
                val container = currentState.getEntity(playerId) ?: continue
                val poison = container.get<PoisonComponent>() ?: continue
                val alreadyLost = container.has<LostGameComponent>()

                if (poison.isLethal && !alreadyLost) {
                    events.add(EcsActionEvent.PlayerLost(playerId, "10 or more poison counters"))
                    currentState = currentState.updateEntity(playerId) { c ->
                        c.with(LostGameComponent.poison())
                    }
                    actionsPerformed = true
                }
            }

            // Legend Rule (704.5j): If player controls 2+ legendaries with same name, sacrifice extras
            val legendResult = checkLegendRule(currentState, events)
            currentState = legendResult.first
            if (legendResult.second) actionsPerformed = true

            // Aura Validity (704.5m, 704.5n): If aura's target is gone or illegal, put aura in graveyard
            val auraResult = checkAuraValidity(currentState, events)
            currentState = auraResult.first
            if (auraResult.second) actionsPerformed = true

            // Token Cessation (704.5d): Tokens not on battlefield cease to exist
            val tokenResult = checkTokenCessation(currentState, events)
            currentState = tokenResult.first
            if (tokenResult.second) actionsPerformed = true

            // Counter Cancellation (704.5q): +1/+1 and -1/-1 counters cancel each other
            val counterResult = checkCounterCancellation(currentState, events)
            currentState = counterResult.first
            if (counterResult.second) actionsPerformed = true

        } while (actionsPerformed)

        // Check for game end
        val activePlayers = currentState.getPlayerIds().filter { playerId ->
            !currentState.hasComponent<LostGameComponent>(playerId)
        }

        if (activePlayers.size == 1) {
            events.add(EcsActionEvent.GameEnded(activePlayers.first()))
            currentState = currentState.endGame(activePlayers.first())
        } else if (activePlayers.isEmpty()) {
            events.add(EcsActionEvent.GameEnded(null))
            currentState = currentState.endGame(null)
        }

        return currentState
    }

    private fun executeClearDamage(state: EcsGameState, action: EcsClearDamage): EcsGameState {
        if (action.entityId != null) {
            return state.updateEntity(action.entityId) { c -> c.without<DamageComponent>() }
        }

        // Clear damage from all creatures
        var currentState = state
        for (entityId in state.getBattlefield()) {
            if (currentState.hasComponent<DamageComponent>(entityId)) {
                currentState = currentState.updateEntity(entityId) { c -> c.without<DamageComponent>() }
            }
        }
        return currentState
    }

    // =========================================================================
    // State-Based Action Helpers
    // =========================================================================

    /**
     * Legend Rule (704.5j): If a player controls two or more legendary permanents
     * with the same name, that player chooses one of them and puts the rest
     * into their owners' graveyards.
     *
     * This creates pending choices that must be resolved by the player via
     * the EcsResolveLegendRule action. No permanents are destroyed here.
     */
    private fun checkLegendRule(
        state: EcsGameState,
        @Suppress("UNUSED_PARAMETER") events: MutableList<EcsActionEvent>
    ): Pair<EcsGameState, Boolean> {
        var currentState = state
        var choicesCreated = false

        // Group legendaries by controller and name
        val legendaries = state.getBattlefield()
            .mapNotNull { entityId ->
                val container = state.getEntity(entityId)
                val cardComp = container?.get<CardComponent>()
                val controllerComp = container?.get<ControllerComponent>()
                if (cardComp != null && controllerComp != null &&
                    cardComp.definition.typeLine.isLegendary) {
                    Triple(entityId, controllerComp.controllerId, cardComp.name)
                } else null
            }
            .groupBy { it.second to it.third }  // Group by (controller, name)

        for ((key, group) in legendaries) {
            val (controllerId, name) = key
            if (group.size > 1) {
                // Check if we already have a pending choice for this
                val existingChoice = currentState.pendingLegendRuleChoices.find {
                    it.controllerId == controllerId && it.legendaryName == name
                }

                if (existingChoice == null) {
                    // Create a pending choice for the player
                    val duplicateIds = group.map { it.first }
                    val pendingChoice = PendingLegendRuleChoice(
                        controllerId = controllerId,
                        legendaryName = name,
                        duplicateIds = duplicateIds
                    )
                    currentState = currentState.addPendingLegendRuleChoice(pendingChoice)
                    choicesCreated = true
                }
            }
        }

        // Note: We return false for actionsPerformed because no state was actually changed
        // (no permanents destroyed). The pending choice needs to be resolved separately.
        // However, we do need to signal that choices were created.
        return currentState to choicesCreated
    }

    /**
     * Aura Validity (704.5m, 704.5n): If an Aura is attached to an illegal object
     * or player, or is not attached to an object or player, that Aura is put
     * into its owner's graveyard.
     */
    private fun checkAuraValidity(
        state: EcsGameState,
        events: MutableList<EcsActionEvent>
    ): Pair<EcsGameState, Boolean> {
        var currentState = state
        var actionsPerformed = false

        for (entityId in state.getBattlefield()) {
            val container = currentState.getEntity(entityId) ?: continue
            val cardComp = container.get<CardComponent>() ?: continue

            if (!cardComp.isAura) continue

            val attachedTo = container.get<AttachedToComponent>()

            // Check if aura is attached to something
            if (attachedTo == null) {
                // Aura not attached - goes to graveyard
                val graveyardZone = ZoneId.graveyard(cardComp.ownerId)
                events.add(EcsActionEvent.PermanentDestroyed(entityId, cardComp.name))
                currentState = currentState
                    .removeFromZone(entityId, ZoneId.BATTLEFIELD)
                    .addToZone(entityId, graveyardZone)
                actionsPerformed = true
                continue
            }

            // Check if target still exists on battlefield
            val targetExists = attachedTo.targetId in currentState.getBattlefield()
            if (!targetExists) {
                val graveyardZone = ZoneId.graveyard(cardComp.ownerId)
                events.add(EcsActionEvent.PermanentDestroyed(entityId, cardComp.name))
                currentState = currentState
                    .removeFromZone(entityId, ZoneId.BATTLEFIELD)
                    .addToZone(entityId, graveyardZone)
                    .updateEntity(entityId) { c -> c.without<AttachedToComponent>() }
                actionsPerformed = true
            }
        }

        return currentState to actionsPerformed
    }

    /**
     * Token Cessation (704.5d): If a token is in a zone other than the battlefield,
     * it ceases to exist.
     */
    private fun checkTokenCessation(
        state: EcsGameState,
        @Suppress("UNUSED_PARAMETER") events: MutableList<EcsActionEvent>
    ): Pair<EcsGameState, Boolean> {
        var currentState = state
        var actionsPerformed = false

        // Find all tokens not on battlefield
        val battlefield = state.getBattlefield().toSet()
        val tokensNotOnBattlefield = state.entitiesWithComponent<TokenComponent>()
            .filter { it !in battlefield }

        for (entityId in tokensNotOnBattlefield) {
            // Find which zone the token is in and remove it
            val zone = currentState.findZone(entityId)
            if (zone != null) {
                currentState = currentState.removeFromZone(entityId, zone)
            }
            // Remove the entity entirely
            currentState = currentState.removeEntity(entityId)
            actionsPerformed = true
        }

        return currentState to actionsPerformed
    }

    /**
     * Counter Cancellation (704.5q): If a permanent has both a +1/+1 counter
     * and a -1/-1 counter on it, N +1/+1 and N -1/-1 counters are removed from it,
     * where N is the smaller of the number of +1/+1 and -1/-1 counters on it.
     */
    private fun checkCounterCancellation(
        state: EcsGameState,
        @Suppress("UNUSED_PARAMETER") events: MutableList<EcsActionEvent>
    ): Pair<EcsGameState, Boolean> {
        var currentState = state
        var actionsPerformed = false

        for (entityId in state.getBattlefield()) {
            val container = currentState.getEntity(entityId) ?: continue
            val counters = container.get<CountersComponent>() ?: continue

            val plusCounters = counters.getCount(CounterType.PLUS_ONE_PLUS_ONE)
            val minusCounters = counters.getCount(CounterType.MINUS_ONE_MINUS_ONE)

            if (plusCounters > 0 && minusCounters > 0) {
                val toRemove = minOf(plusCounters, minusCounters)
                val newCounters = counters
                    .remove(CounterType.PLUS_ONE_PLUS_ONE, toRemove)
                    .remove(CounterType.MINUS_ONE_MINUS_ONE, toRemove)

                currentState = currentState.updateEntity(entityId) { c ->
                    c.with(newCounters)
                }
                actionsPerformed = true
            }
        }

        return currentState to actionsPerformed
    }
}

/**
 * Result of an action execution.
 */
sealed interface EcsActionResult {
    data class Success(
        val state: EcsGameState,
        val action: EcsAction,
        val events: List<EcsActionEvent> = emptyList()
    ) : EcsActionResult

    data class Failure(
        val state: EcsGameState,
        val action: EcsAction,
        val reason: String
    ) : EcsActionResult
}

/**
 * Events generated during action execution.
 */
sealed interface EcsActionEvent {
    data class LifeChanged(val playerId: EntityId, val oldLife: Int, val newLife: Int) : EcsActionEvent
    data class DamageDealtToPlayer(val sourceId: EntityId?, val targetId: EntityId, val amount: Int) : EcsActionEvent
    data class DamageDealtToCreature(val sourceId: EntityId?, val targetId: EntityId, val amount: Int) : EcsActionEvent
    data class ManaAdded(val playerId: EntityId, val color: String, val amount: Int) : EcsActionEvent
    data class CardDrawn(val playerId: EntityId, val cardId: EntityId, val cardName: String) : EcsActionEvent
    data class DrawFailed(val playerId: EntityId) : EcsActionEvent
    data class CardDiscarded(val playerId: EntityId, val cardId: EntityId, val cardName: String) : EcsActionEvent
    data class CardMoved(val entityId: EntityId, val cardName: String, val fromZone: ZoneId, val toZone: ZoneId) : EcsActionEvent
    data class PermanentDestroyed(val entityId: EntityId, val name: String) : EcsActionEvent
    data class CreatureDied(val entityId: EntityId, val name: String, val ownerId: EntityId) : EcsActionEvent
    data class CardExiled(val entityId: EntityId, val name: String) : EcsActionEvent
    data class CardReturnedToHand(val entityId: EntityId, val name: String) : EcsActionEvent
    data class PermanentTapped(val entityId: EntityId, val name: String) : EcsActionEvent
    data class PermanentUntapped(val entityId: EntityId, val name: String) : EcsActionEvent
    data class LandPlayed(val playerId: EntityId, val cardId: EntityId, val name: String) : EcsActionEvent
    data class CombatStarted(val attackingPlayerId: EntityId, val defendingPlayerId: EntityId) : EcsActionEvent
    data class AttackerDeclared(val creatureId: EntityId, val name: String) : EcsActionEvent
    data class BlockerDeclared(val blockerId: EntityId, val attackerId: EntityId, val name: String) : EcsActionEvent
    data class BlockersOrdered(val attackerId: EntityId, val orderedBlockerIds: List<EntityId>) : EcsActionEvent
    data class DamageDealt(val sourceId: EntityId, val targetId: EntityId, val amount: Int, val isCombatDamage: Boolean) : EcsActionEvent
    data class CombatEnded(val playerId: EntityId) : EcsActionEvent
    data class GameEnded(val winnerId: EntityId?) : EcsActionEvent
    data class PlayerLost(val playerId: EntityId, val reason: String) : EcsActionEvent
    data class Attached(val attachmentId: EntityId, val attachmentName: String, val targetId: EntityId, val targetName: String) : EcsActionEvent
    data class Detached(val attachmentId: EntityId, val name: String) : EcsActionEvent

    // Stack resolution events
    data class SpellCast(val entityId: EntityId, val name: String, val casterId: EntityId) : EcsActionEvent
    data class SpellResolved(val entityId: EntityId, val name: String) : EcsActionEvent
    data class SpellFizzled(val entityId: EntityId, val name: String, val reason: String) : EcsActionEvent
    data class PermanentEnteredBattlefield(val entityId: EntityId, val name: String, val controllerId: EntityId) : EcsActionEvent
    data class AbilityResolved(val description: String, val sourceId: EntityId) : EcsActionEvent
    data class AbilityFizzled(val description: String, val sourceId: EntityId, val reason: String) : EcsActionEvent
}
