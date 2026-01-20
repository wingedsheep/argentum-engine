package com.wingedsheep.rulesengine.ecs.action

import com.wingedsheep.rulesengine.ability.AbilityCost
import com.wingedsheep.rulesengine.ability.AbilityId
import com.wingedsheep.rulesengine.ability.ActivatedAbility
import com.wingedsheep.rulesengine.ability.AddColorlessManaEffect
import com.wingedsheep.rulesengine.ability.AddManaEffect
import com.wingedsheep.rulesengine.ability.TimingRestriction
import com.wingedsheep.rulesengine.core.CounterType
import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.decision.DecisionResumer
import com.wingedsheep.rulesengine.ecs.ComponentContainer
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.combat.DamageEventProjector
import com.wingedsheep.rulesengine.ecs.combat.DamagePreventionEffect
import com.wingedsheep.rulesengine.ecs.combat.CombatDamageCalculator
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.components.PendingLegendRuleChoice
import com.wingedsheep.rulesengine.ecs.layers.ActiveContinuousEffect
import com.wingedsheep.rulesengine.ecs.layers.EffectDuration
import com.wingedsheep.rulesengine.ecs.layers.Modifier
import com.wingedsheep.rulesengine.ecs.stack.StackResolver
import com.wingedsheep.rulesengine.zone.ZoneType
import kotlinx.serialization.Serializable

/**
 * Handles execution of GameActions against GameState.
 *
 * All operations are pure functions - each action produces a new state
 * without modifying the original. Events are generated for game logging
 * and trigger detection.
 *
 * Example usage:
 * ```kotlin
 * val handler = GameActionHandler()
 * val result = handler.execute(state, DrawCard(playerId, 2))
 * when (result) {
 *     is GameActionResult.Success -> println("New state: ${result.state}")
 *     is GameActionResult.Failure -> println("Error: ${result.reason}")
 * }
 * ```
 */
class GameActionHandler {

    /**
     * Execute a single action and return the result.
     */
    fun execute(state: GameState, action: GameAction): GameActionResult {
        return try {
            val (newState, events) = executeAction(state, action)
            GameActionResult.Success(newState, action, events)
        } catch (e: IllegalStateException) {
            GameActionResult.Failure(state, action, e.message ?: "Unknown error")
        } catch (e: IllegalArgumentException) {
            GameActionResult.Failure(state, action, e.message ?: "Invalid argument")
        }
    }

    /**
     * Execute multiple actions in sequence.
     */
    fun executeAll(state: GameState, actions: List<GameAction>): GameActionResult {
        var currentState = state
        val allEvents = mutableListOf<GameActionEvent>()

        for (action in actions) {
            val result = execute(currentState, action)
            when (result) {
                is GameActionResult.Success -> {
                    currentState = result.state
                    allEvents.addAll(result.events)
                }
                is GameActionResult.Failure -> return result
            }
        }

        return if (actions.isNotEmpty()) {
            GameActionResult.Success(currentState, actions.last(), allEvents)
        } else {
            GameActionResult.Success(state, CheckStateBasedActions(), emptyList())
        }
    }

    private fun executeAction(state: GameState, action: GameAction): Pair<GameState, List<GameActionEvent>> {
        val events = mutableListOf<GameActionEvent>()

        val newState = when (action) {
            // Life actions
            is GainLife -> executeGainLife(state, action, events)
            is LoseLife -> executeLoseLife(state, action, events)
            is SetLife -> executeSetLife(state, action, events)
            is DealDamageToPlayer -> executeDealDamageToPlayer(state, action, events)
            is DealDamageToCreature -> executeDealDamageToCreature(state, action, events)

            // Mana actions
            is AddMana -> executeAddMana(state, action, events)
            is AddColorlessMana -> executeAddColorlessMana(state, action, events)
            is EmptyManaPool -> executeEmptyManaPool(state, action)
            is ActivateManaAbility -> executeActivateManaAbility(state, action, events)

            // Card drawing
            is DrawCard -> executeDrawCard(state, action, events)
            is DiscardCard -> executeDiscardCard(state, action, events)

            // Zone movement
            is MoveEntity -> executeMoveEntity(state, action, events)
            is PutOntoBattlefield -> executePutOntoBattlefield(state, action, events)
            is DestroyPermanent -> executeDestroyPermanent(state, action, events)
            is SacrificePermanent -> executeSacrificePermanent(state, action, events)
            is ExilePermanent -> executeExilePermanent(state, action, events)
            is ReturnToHand -> executeReturnToHand(state, action, events)

            // Tap/Untap
            is Tap -> executeTap(state, action, events)
            is Untap -> executeUntap(state, action, events)
            is UntapAll -> executeUntapAll(state, action, events)

            // Counters
            is AddCounters -> executeAddCounters(state, action)
            is RemoveCounters -> executeRemoveCounters(state, action)
            is AddPoisonCounters -> executeAddPoisonCounters(state, action)

            // Summoning sickness
            is RemoveSummoningSickness -> executeRemoveSummoningSickness(state, action)
            is RemoveAllSummoningSickness -> executeRemoveAllSummoningSickness(state, action)

            // Land actions
            is PlayLand -> executePlayLand(state, action, events)
            is ResetLandsPlayed -> executeResetLandsPlayed(state, action)

            // Library actions
            is ShuffleLibrary -> executeShuffleLibrary(state, action)

            // Combat actions
            is BeginCombat -> executeBeginCombat(state, action, events)
            is DeclareAttacker -> executeDeclareAttacker(state, action, events)
            is DeclareBlocker -> executeDeclareBlocker(state, action, events)
            is OrderBlockers -> executeOrderBlockers(state, action, events)
            is ResolveCombatDamage -> executeResolveCombatDamage(state, action, events)
            is EndCombat -> executeEndCombat(state, action, events)

            // Game flow
            is PassPriority -> executePassPriority(state, action)
            is EndGame -> executeEndGame(state, action, events)
            is PlayerLoses -> executePlayerLoses(state, action, events)
            is ResolveLegendRule -> executeResolveLegendRule(state, action, events)

            // Stack resolution
            is ResolveTopOfStack -> executeResolveTopOfStack(state, events)
            is CastSpell -> executeCastSpell(state, action, events)

            // Attachment actions
            is Attach -> executeAttach(state, action, events)
            is Detach -> executeDetach(state, action, events)

            // State-based actions
            is CheckStateBasedActions -> executeCheckStateBasedActions(state, events)
            is ClearDamage -> executeClearDamage(state, action)

            // Turn/Step actions
            is PerformCleanupStep -> executePerformCleanupStep(state, action, events)
            is ExpireEndOfCombatEffects -> executeExpireEndOfCombatEffects(state)
            is ExpireEffectsForPermanent -> executeExpireEffectsForPermanent(state, action)
            is ResolveCleanupDiscard -> executeResolveCleanupDiscard(state, action, events)

            // Decision submission
            is SubmitDecision -> executeSubmitDecision(state, action, events)
        }

        return newState to events
    }

    // =========================================================================
    // Life Actions
    // =========================================================================

    private fun executeGainLife(
        state: GameState,
        action: GainLife,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val lifeComponent = container.get<LifeComponent>() ?: throw IllegalStateException("Player has no life component")

        val oldLife = lifeComponent.life
        val newLife = oldLife + action.amount
        events.add(GameActionEvent.LifeChanged(action.playerId, oldLife, newLife))

        return state.updateEntity(action.playerId) { c ->
            c.with(lifeComponent.gainLife(action.amount))
        }
    }

    private fun executeLoseLife(
        state: GameState,
        action: LoseLife,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val lifeComponent = container.get<LifeComponent>() ?: throw IllegalStateException("Player has no life component")

        val oldLife = lifeComponent.life
        val newLife = oldLife - action.amount
        events.add(GameActionEvent.LifeChanged(action.playerId, oldLife, newLife))

        return state.updateEntity(action.playerId) { c ->
            c.with(lifeComponent.loseLife(action.amount))
        }
    }

    private fun executeSetLife(
        state: GameState,
        action: SetLife,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val lifeComponent = container.get<LifeComponent>() ?: throw IllegalStateException("Player has no life component")

        val oldLife = lifeComponent.life
        events.add(GameActionEvent.LifeChanged(action.playerId, oldLife, action.amount))

        return state.updateEntity(action.playerId) { c ->
            c.with(lifeComponent.setLife(action.amount))
        }
    }

    private fun executeDealDamageToPlayer(
        state: GameState,
        action: DealDamageToPlayer,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.targetPlayerId) ?: throw IllegalStateException("Target player not found")
        val lifeComponent = container.get<LifeComponent>() ?: throw IllegalStateException("Player has no life component")

        val oldLife = lifeComponent.life
        val newLife = oldLife - action.amount
        events.add(GameActionEvent.DamageDealtToPlayer(action.sourceEntityId, action.targetPlayerId, action.amount))
        events.add(GameActionEvent.LifeChanged(action.targetPlayerId, oldLife, newLife))

        return state.updateEntity(action.targetPlayerId) { c ->
            c.with(lifeComponent.loseLife(action.amount))
        }
    }

    private fun executeDealDamageToCreature(
        state: GameState,
        action: DealDamageToCreature,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.targetEntityId) ?: throw IllegalStateException("Target creature not found")
        val damageComponent = container.get<DamageComponent>() ?: DamageComponent(0)

        events.add(GameActionEvent.DamageDealtToCreature(action.sourceEntityId, action.targetEntityId, action.amount))

        return state.updateEntity(action.targetEntityId) { c ->
            c.with(damageComponent.addDamage(action.amount))
        }
    }

    // =========================================================================
    // Mana Actions
    // =========================================================================

    private fun executeAddMana(
        state: GameState,
        action: AddMana,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()

        events.add(GameActionEvent.ManaAdded(action.playerId, action.color.displayName, action.amount))

        return state.updateEntity(action.playerId) { c ->
            c.with(manaPool.add(action.color, action.amount))
        }
    }

    private fun executeAddColorlessMana(
        state: GameState,
        action: AddColorlessMana,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()

        events.add(GameActionEvent.ManaAdded(action.playerId, "Colorless", action.amount))

        return state.updateEntity(action.playerId) { c ->
            c.with(manaPool.addColorless(action.amount))
        }
    }

    private fun executeEmptyManaPool(state: GameState, action: EmptyManaPool): GameState {
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
        state: GameState,
        action: ActivateManaAbility,
        events: MutableList<GameActionEvent>
    ): GameState {
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

        // Get the ability - either from AbilitiesComponent or infer from basic lands
        val ability = getActivatableManaAbility(container, action.abilityIndex)
            ?: throw IllegalStateException("Mana ability not found at index ${action.abilityIndex}")

        var currentState = state

        // Pay the cost (tap for basic lands)
        currentState = payAbilityCost(currentState, sourceId, ability.cost, action.playerId, events)

        // Execute the effect (add mana)
        currentState = executeManaEffect(currentState, action.playerId, ability.effect, events)

        return currentState
    }

    /**
     * Get a mana ability from an entity, either from AbilitiesComponent or inferred from basic land types.
     */
    private fun getActivatableManaAbility(
        container: ComponentContainer,
        index: Int
    ): ActivatedAbility? {
        // First, check if the entity has explicit abilities defined
        val abilities = container.get<AbilitiesComponent>()
        if (abilities != null) {
            return abilities.getManaAbility(index)
        }

        // If no AbilitiesComponent, check if it's a basic land and infer the mana ability
        val cardComponent = container.get<CardComponent>() ?: return null
        val def = cardComponent.definition

        // Only handle basic lands with implicit mana abilities
        if (!def.isLand) return null
        val subtypes = def.typeLine.subtypes

        // For index 0, return the basic land's mana ability based on its subtype
        if (index == 0) {
            // Match basic land types to their mana colors
            return when {
                subtypes.any { it.value.equals("Plains", ignoreCase = true) } ->
                    createBasicLandManaAbility(com.wingedsheep.rulesengine.core.Color.WHITE)
                subtypes.any { it.value.equals("Island", ignoreCase = true) } ->
                    createBasicLandManaAbility(com.wingedsheep.rulesengine.core.Color.BLUE)
                subtypes.any { it.value.equals("Swamp", ignoreCase = true) } ->
                    createBasicLandManaAbility(com.wingedsheep.rulesengine.core.Color.BLACK)
                subtypes.any { it.value.equals("Mountain", ignoreCase = true) } ->
                    createBasicLandManaAbility(com.wingedsheep.rulesengine.core.Color.RED)
                subtypes.any { it.value.equals("Forest", ignoreCase = true) } ->
                    createBasicLandManaAbility(com.wingedsheep.rulesengine.core.Color.GREEN)
                else -> null
            }
        }

        return null
    }

    /**
     * Create a basic land mana ability (tap: add one mana of the specified color).
     */
    private fun createBasicLandManaAbility(color: com.wingedsheep.rulesengine.core.Color): ActivatedAbility {
        return ActivatedAbility(
            id = AbilityId.generate(),
            cost = AbilityCost.Tap,
            effect = AddManaEffect(color, 1),
            timingRestriction = TimingRestriction.INSTANT,
            isManaAbility = true
        )
    }

    /**
     * Pay the cost of an ability.
     */
    private fun payAbilityCost(
        state: GameState,
        sourceId: EntityId,
        cost: AbilityCost,
        playerId: EntityId,
        events: MutableList<GameActionEvent>
    ): GameState {
        return when (cost) {
            is AbilityCost.Tap -> {
                // Check if already tapped
                val container = state.getEntity(sourceId)!!
                if (container.get<TappedComponent>() != null) {
                    throw IllegalStateException("Permanent is already tapped")
                }
                val cardName = container.get<CardComponent>()?.name ?: "Unknown"
                events.add(GameActionEvent.PermanentTapped(sourceId, cardName))
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
                events.add(GameActionEvent.LifeChanged(playerId, lifeComp.life, newLife))
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
            is AbilityCost.RemoveCounter -> {
                // Remove a counter from the source permanent
                val container = state.getEntity(sourceId)!!
                val counters = container.get<CountersComponent>() ?: CountersComponent.EMPTY
                // Find a counter to remove - "any" means any type
                val counterTypeToRemove = if (cost.counterType == "any") {
                    // Find first available counter type
                    counters.counters.keys.firstOrNull()
                        ?: throw IllegalStateException("No counters to remove")
                } else {
                    // Specific counter type
                    CounterType.valueOf(cost.counterType.uppercase().replace("/", "_").replace("+", "PLUS_").replace("-", "MINUS_"))
                }
                if (counters.getCount(counterTypeToRemove) < cost.count) {
                    throw IllegalStateException("Not enough ${cost.counterType} counters to remove")
                }
                val cardName = container.get<CardComponent>()?.name ?: "Unknown"
                events.add(GameActionEvent.CounterRemoved(sourceId, cardName, counterTypeToRemove.name, cost.count))
                state.updateEntity(sourceId) { c ->
                    c.with(counters.remove(counterTypeToRemove, cost.count))
                }
            }
            is AbilityCost.Loyalty -> {
                // Add or remove loyalty counters from the planeswalker
                val container = state.getEntity(sourceId)!!
                val counters = container.get<CountersComponent>() ?: CountersComponent.EMPTY
                val currentLoyalty = counters.getCount(CounterType.LOYALTY)

                // For negative costs, check if we have enough loyalty
                if (cost.amount < 0 && currentLoyalty < -cost.amount) {
                    throw IllegalStateException("Not enough loyalty counters (have $currentLoyalty, need ${-cost.amount})")
                }

                val cardName = container.get<CardComponent>()?.name ?: "Unknown"
                val newLoyalty = currentLoyalty + cost.amount

                if (cost.amount > 0) {
                    events.add(GameActionEvent.CounterAdded(sourceId, cardName, "LOYALTY", cost.amount))
                } else {
                    events.add(GameActionEvent.CounterRemoved(sourceId, cardName, "LOYALTY", -cost.amount))
                }

                state.updateEntity(sourceId) { c ->
                    val newCounters = if (cost.amount > 0) {
                        counters.add(CounterType.LOYALTY, cost.amount)
                    } else {
                        counters.remove(CounterType.LOYALTY, -cost.amount)
                    }
                    c.with(newCounters)
                }
            }
            is AbilityCost.TapOtherCreature -> {
                // Tap another creature as a cost
                // The targetId must be provided (chosen by player/UI before activation)
                val targetId = cost.targetId
                    ?: throw IllegalStateException("TapOtherCreature cost requires a target creature to be selected")

                // Verify the target is a valid untapped creature we control
                val targetContainer = state.getEntity(targetId)
                    ?: throw IllegalStateException("Target creature not found")
                val targetController = targetContainer.get<ControllerComponent>()?.controllerId
                if (targetController != playerId) {
                    throw IllegalStateException("Target creature must be controlled by you")
                }
                if (targetContainer.get<TappedComponent>() != null) {
                    throw IllegalStateException("Target creature is already tapped")
                }
                if (targetId == sourceId) {
                    throw IllegalStateException("Must tap another creature, not the source")
                }

                val cardName = targetContainer.get<CardComponent>()?.name ?: "Unknown"
                events.add(GameActionEvent.PermanentTapped(targetId, cardName))
                state.updateEntity(targetId) { c -> c.with(TappedComponent) }
            }
            is AbilityCost.Blight -> {
                // Blight N: Put N -1/-1 counters on a creature you control
                // The targetId must be provided (chosen by player/UI before activation)
                val targetId = cost.targetId
                    ?: throw IllegalStateException("Blight cost requires a target creature to be selected")

                // Verify the target is a creature we control
                val targetContainer = state.getEntity(targetId)
                    ?: throw IllegalStateException("Target creature not found")
                val targetController = targetContainer.get<ControllerComponent>()?.controllerId
                if (targetController != playerId) {
                    throw IllegalStateException("Target creature must be controlled by you")
                }
                val cardComponent = targetContainer.get<CardComponent>()
                    ?: throw IllegalStateException("Target must be a card")
                if (!cardComponent.isCreature) {
                    throw IllegalStateException("Target must be a creature")
                }

                // Add -1/-1 counters to the target creature
                val counters = targetContainer.get<CountersComponent>() ?: CountersComponent.EMPTY
                val cardName = cardComponent.name
                events.add(GameActionEvent.CounterAdded(targetId, cardName, "MINUS_ONE_MINUS_ONE", cost.amount))
                state.updateEntity(targetId) { c ->
                    c.with(counters.add(CounterType.MINUS_ONE_MINUS_ONE, cost.amount))
                }
            }
            is AbilityCost.DiscardSelf -> {
                // Discard the source card as a cost (used for cycling abilities)
                // The source should be in hand and gets moved to graveyard
                val container = state.getEntity(sourceId)
                    ?: throw IllegalStateException("Source card not found")
                val cardComponent = container.get<CardComponent>()
                val cardName = cardComponent?.name ?: "Unknown"
                val ownerId = cardComponent?.ownerId ?: playerId

                // Find the zone the card is in (should be hand for cycling)
                val currentZone = state.findZone(sourceId)
                    ?: throw IllegalStateException("Source card not in any zone")

                events.add(GameActionEvent.CardDiscarded(playerId, sourceId, cardName))

                // Move from current zone to graveyard
                state.removeFromZone(sourceId, currentZone)
                    .addToZone(sourceId, ZoneId.graveyard(ownerId))
            }
        }
    }

    /**
     * Execute a mana-producing effect.
     */
    private fun executeManaEffect(
        state: GameState,
        playerId: EntityId,
        effect: com.wingedsheep.rulesengine.ability.Effect,
        events: MutableList<GameActionEvent>
    ): GameState {
        return when (effect) {
            is AddManaEffect -> {
                val container = state.getEntity(playerId) ?: throw IllegalStateException("Player not found")
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                events.add(GameActionEvent.ManaAdded(playerId, effect.color.displayName, effect.amount))
                state.updateEntity(playerId) { c ->
                    c.with(manaPool.add(effect.color, effect.amount))
                }
            }
            is AddColorlessManaEffect -> {
                val container = state.getEntity(playerId) ?: throw IllegalStateException("Player not found")
                val manaPool = container.get<ManaPoolComponent>() ?: ManaPoolComponent()
                events.add(GameActionEvent.ManaAdded(playerId, "Colorless", effect.amount))
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
        state: GameState,
        action: DrawCard,
        events: MutableList<GameActionEvent>
    ): GameState {
        var currentState = state
        val libraryZone = ZoneId.library(action.playerId)
        val handZone = ZoneId.hand(action.playerId)

        repeat(action.count) {
            val library = currentState.getZone(libraryZone)
            if (library.isEmpty()) {
                events.add(GameActionEvent.DrawFailed(action.playerId))
                // Mark player as lost due to drawing from empty library
                currentState = currentState.updateEntity(action.playerId) { c ->
                    c.with(LostGameComponent.decked())
                }
                return currentState
            }

            val cardId = library.first()
            val cardName = currentState.getEntity(cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"

            events.add(GameActionEvent.CardDrawn(action.playerId, cardId, cardName))

            currentState = currentState
                .removeFromZone(cardId, libraryZone)
                .addToZone(cardId, handZone)
        }

        return currentState
    }

    private fun executeDiscardCard(
        state: GameState,
        action: DiscardCard,
        events: MutableList<GameActionEvent>
    ): GameState {
        val handZone = ZoneId.hand(action.playerId)
        val graveyardZone = ZoneId.graveyard(action.playerId)

        if (!state.isInZone(action.cardId, handZone)) {
            throw IllegalStateException("Card not in hand")
        }

        val cardName = state.getEntity(action.cardId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(GameActionEvent.CardDiscarded(action.playerId, action.cardId, cardName))

        return state
            .removeFromZone(action.cardId, handZone)
            .addToZone(action.cardId, graveyardZone)
    }

    // =========================================================================
    // Zone Movement
    // =========================================================================

    private fun executeMoveEntity(
        state: GameState,
        action: MoveEntity,
        events: MutableList<GameActionEvent>
    ): GameState {
        val cardName = state.getEntity(action.entityId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(GameActionEvent.CardMoved(action.entityId, cardName, action.fromZone, action.toZone))

        var newState = state.removeFromZone(action.entityId, action.fromZone)
        newState = if (action.toTop) {
            newState.addToZone(action.entityId, action.toZone)
        } else {
            newState.addToZoneBottom(action.entityId, action.toZone)
        }

        return newState
    }

    private fun executePutOntoBattlefield(
        state: GameState,
        action: PutOntoBattlefield,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.entityId) ?: throw IllegalStateException("Entity not found")
        val cardComponent = container.get<CardComponent>() ?: throw IllegalStateException("Entity has no card component")

        val cardName = cardComponent.definition.name
        val currentZone = state.findZone(action.entityId)

        if (currentZone != null) {
            events.add(GameActionEvent.CardMoved(action.entityId, cardName, currentZone, ZoneId.BATTLEFIELD))
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

            // Add base components for projection
            val definition = cardComponent.definition
            updated = updated.with(BaseTypesComponent(definition.typeLine.cardTypes, definition.typeLine.subtypes))
            updated = updated.with(BaseColorsComponent(definition.colors))
            updated = updated.with(BaseKeywordsComponent(definition.keywords))
            definition.creatureStats?.let {
                updated = updated.with(BaseStatsComponent(it.basePower, it.baseToughness))
            }

            updated
        }

        // Emit PermanentEnteredBattlefield event for ETB trigger detection
        events.add(GameActionEvent.PermanentEnteredBattlefield(action.entityId, cardName, action.controllerId))

        return newState
    }

    private fun executeDestroyPermanent(
        state: GameState,
        action: DestroyPermanent,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.entityId) ?: throw IllegalStateException("Entity not found")
        val cardComponent = container.get<CardComponent>() ?: throw IllegalStateException("Entity has no card component")

        val cardName = cardComponent.definition.name
        val graveyardZone = ZoneId.graveyard(cardComponent.ownerId)

        events.add(GameActionEvent.PermanentDestroyed(action.entityId, cardName))
        if (cardComponent.definition.isCreature) {
            events.add(GameActionEvent.CreatureDied(action.entityId, cardName, cardComponent.ownerId))
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
        state: GameState,
        action: SacrificePermanent,
        events: MutableList<GameActionEvent>
    ): GameState {
        return executeDestroyPermanent(state, DestroyPermanent(action.entityId), events)
    }

    private fun executeExilePermanent(
        state: GameState,
        action: ExilePermanent,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.entityId) ?: throw IllegalStateException("Entity not found")
        val cardComponent = container.get<CardComponent>() ?: throw IllegalStateException("Entity has no card component")

        val cardName = cardComponent.definition.name
        val currentZone = state.findZone(action.entityId) ?: throw IllegalStateException("Entity not in any zone")

        events.add(GameActionEvent.CardExiled(action.entityId, cardName))

        return state
            .removeFromZone(action.entityId, currentZone)
            .addToZone(action.entityId, ZoneId.EXILE)
    }

    private fun executeReturnToHand(
        state: GameState,
        action: ReturnToHand,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.entityId) ?: throw IllegalStateException("Entity not found")
        val cardComponent = container.get<CardComponent>() ?: throw IllegalStateException("Entity has no card component")

        val cardName = cardComponent.definition.name
        val currentZone = state.findZone(action.entityId) ?: throw IllegalStateException("Entity not in any zone")
        val ownerHand = ZoneId.hand(cardComponent.ownerId)

        events.add(GameActionEvent.CardReturnedToHand(action.entityId, cardName))

        return state
            .removeFromZone(action.entityId, currentZone)
            .addToZone(action.entityId, ownerHand)
    }

    // =========================================================================
    // Tap/Untap
    // =========================================================================

    private fun executeTap(
        state: GameState,
        action: Tap,
        events: MutableList<GameActionEvent>
    ): GameState {
        val cardName = state.getEntity(action.entityId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(GameActionEvent.PermanentTapped(action.entityId, cardName))

        return state.updateEntity(action.entityId) { c -> c.with(TappedComponent) }
    }

    private fun executeUntap(
        state: GameState,
        action: Untap,
        events: MutableList<GameActionEvent>
    ): GameState {
        val cardName = state.getEntity(action.entityId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(GameActionEvent.PermanentUntapped(action.entityId, cardName))

        return state.updateEntity(action.entityId) { c -> c.without<TappedComponent>() }
    }

    private fun executeUntapAll(
        state: GameState,
        action: UntapAll,
        events: MutableList<GameActionEvent>
    ): GameState {
        var currentState = state

        for (entityId in state.getPermanentsControlledBy(action.controllerId)) {
            if (currentState.hasComponent<TappedComponent>(entityId)) {
                val cardName = currentState.getEntity(entityId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
                events.add(GameActionEvent.PermanentUntapped(entityId, cardName))
                currentState = currentState.updateEntity(entityId) { c -> c.without<TappedComponent>() }
            }
        }

        return currentState
    }

    // =========================================================================
    // Counters
    // =========================================================================

    private fun executeAddCounters(state: GameState, action: AddCounters): GameState {
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

    private fun executeRemoveCounters(state: GameState, action: RemoveCounters): GameState {
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

    private fun executeAddPoisonCounters(state: GameState, action: AddPoisonCounters): GameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val poison = container.get<PoisonComponent>() ?: PoisonComponent()

        return state.updateEntity(action.playerId) { c ->
            c.with(poison.add(action.amount))
        }
    }

    // =========================================================================
    // Summoning Sickness
    // =========================================================================

    private fun executeRemoveSummoningSickness(state: GameState, action: RemoveSummoningSickness): GameState {
        return state.updateEntity(action.entityId) { c -> c.without<SummoningSicknessComponent>() }
    }

    private fun executeRemoveAllSummoningSickness(state: GameState, action: RemoveAllSummoningSickness): GameState {
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
        state: GameState,
        action: PlayLand,
        events: MutableList<GameActionEvent>
    ): GameState {
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

        events.add(GameActionEvent.LandPlayed(action.playerId, action.cardId, cardComponent.definition.name))

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

    private fun executeResetLandsPlayed(state: GameState, action: ResetLandsPlayed): GameState {
        val container = state.getEntity(action.playerId) ?: throw IllegalStateException("Player not found")
        val landsPlayed = container.get<LandsPlayedComponent>() ?: return state

        return state.updateEntity(action.playerId) { c ->
            c.with(landsPlayed.reset())
        }
    }

    // =========================================================================
    // Library Actions
    // =========================================================================

    private fun executeShuffleLibrary(state: GameState, action: ShuffleLibrary): GameState {
        return state.shuffleZone(ZoneId.library(action.playerId))
    }

    // =========================================================================
    // Combat Actions
    // =========================================================================

    private fun executeBeginCombat(
        state: GameState,
        action: BeginCombat,
        events: MutableList<GameActionEvent>
    ): GameState {
        events.add(GameActionEvent.CombatStarted(action.attackingPlayerId, action.defendingPlayerId))
        return state.startCombat(action.defendingPlayerId)
    }

    private fun executeDeclareAttacker(
        state: GameState,
        action: DeclareAttacker,
        events: MutableList<GameActionEvent>
    ): GameState {
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

        val cardComponent = container.get<CardComponent>()
        val cardName = cardComponent?.definition?.name ?: "Unknown"
        events.add(GameActionEvent.AttackerDeclared(action.creatureId, cardName))

        // Check for Vigilance - creatures with vigilance don't tap when attacking
        val hasVigilance = cardComponent?.definition?.keywords?.contains(Keyword.VIGILANCE) ?: false

        // Add AttackingComponent to the creature (ECS pattern - attacker status lives on the entity)
        return state.updateEntity(action.creatureId) { c ->
            val withAttacking = c.with(AttackingComponent.attackingPlayer(combat.defendingPlayer))
            if (hasVigilance) withAttacking else withAttacking.with(TappedComponent)
        }
    }

    private fun executeDeclareBlocker(
        state: GameState,
        action: DeclareBlocker,
        events: MutableList<GameActionEvent>
    ): GameState {
        if (state.combat == null) throw IllegalStateException("Not in combat")
        val container = state.getEntity(action.blockerId) ?: throw IllegalStateException("Blocker not found")

        if (container.has<TappedComponent>()) {
            throw IllegalStateException("Creature is tapped and cannot block")
        }

        val cardName = container.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(GameActionEvent.BlockerDeclared(action.blockerId, action.attackerId, cardName))

        // Add BlockingComponent to the blocker (ECS pattern - blocker status lives on the entity)
        return state
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
        state: GameState,
        action: OrderBlockers,
        events: MutableList<GameActionEvent>
    ): GameState {
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

        events.add(GameActionEvent.BlockersOrdered(action.attackerId, action.orderedBlockerIds))

        // Update the BlockedByComponent with the new order
        return state.updateEntity(action.attackerId) { c ->
            c.with(currentBlockedBy.setOrder(action.orderedBlockerIds))
        }
    }

    private fun executeResolveCombatDamage(
        state: GameState,
        action: ResolveCombatDamage,
        events: MutableList<GameActionEvent>
    ): GameState {
        val combat = state.combat ?: throw IllegalStateException("Not in combat")

        // Calculate damage based on the step
        val damageStep = when (action.step) {
            CombatDamageStep.FIRST_STRIKE -> CombatDamageCalculator.DamageStep.FIRST_STRIKE
            CombatDamageStep.REGULAR -> CombatDamageCalculator.DamageStep.REGULAR
        }

        val damageResult = when (damageStep) {
            CombatDamageCalculator.DamageStep.FIRST_STRIKE ->
                CombatDamageCalculator.calculateFirstStrikeDamage(state)
            CombatDamageCalculator.DamageStep.REGULAR ->
                CombatDamageCalculator.calculateRegularDamage(state)
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
                is CombatDamageCalculator.PendingDamageEvent.ToPlayer -> {
                    val lifeComponent = newState.getComponent<LifeComponent>(event.targetPlayerId)
                    if (lifeComponent != null) {
                        val oldLife = lifeComponent.life
                        val newLife = oldLife - event.amount
                        events.add(GameActionEvent.DamageDealt(
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
                is CombatDamageCalculator.PendingDamageEvent.ToPlaneswalker -> {
                    // Remove loyalty counters from planeswalker
                    val counters = newState.getComponent<CountersComponent>(event.targetPlaneswalker)
                    if (counters != null) {
                        val currentLoyalty = counters.counters[CounterType.LOYALTY] ?: 0
                        val newLoyalty = (currentLoyalty - event.amount).coerceAtLeast(0)
                        events.add(GameActionEvent.DamageDealt(
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
                is CombatDamageCalculator.PendingDamageEvent.ToCreature -> {
                    val damageComponent = newState.getComponent<DamageComponent>(event.targetCreatureId)
                    val currentDamage = damageComponent?.amount ?: 0
                    val newDamage = currentDamage + event.amount
                    events.add(GameActionEvent.DamageDealt(
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
        if (damageStep == CombatDamageCalculator.DamageStep.FIRST_STRIKE) {
            newState = CombatDamageCalculator.markFirstStrikeDamageDealt(
                newState,
                damageResult.creaturesDealtDamage
            )
        }

        return newState
    }

    private fun executeEndCombat(
        state: GameState,
        action: EndCombat,
        events: MutableList<GameActionEvent>
    ): GameState {
        events.add(GameActionEvent.CombatEnded(action.playerId))

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

    private fun executePassPriority(state: GameState, action: PassPriority): GameState {
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
        state: GameState,
        action: EndGame,
        events: MutableList<GameActionEvent>
    ): GameState {
        events.add(GameActionEvent.GameEnded(action.winnerId))
        return state.endGame(action.winnerId)
    }

    private fun executePlayerLoses(
        state: GameState,
        action: PlayerLoses,
        events: MutableList<GameActionEvent>
    ): GameState {
        events.add(GameActionEvent.PlayerLost(action.playerId, action.reason))
        return state.updateEntity(action.playerId) { c ->
            c.with(LostGameComponent(action.reason))
        }
    }

    /**
     * Resolve a Legend Rule choice by keeping the chosen legendary
     * and putting all others with the same name into the graveyard.
     */
    private fun executeResolveLegendRule(
        state: GameState,
        action: ResolveLegendRule,
        events: MutableList<GameActionEvent>
    ): GameState {
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
                events.add(GameActionEvent.PermanentDestroyed(entityId, cardComp.name))
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

    private val stackResolver = StackResolver()

    private fun executeResolveTopOfStack(
        state: GameState,
        events: MutableList<GameActionEvent>
    ): GameState {
        val result = stackResolver.resolveTopOfStack(state)

        return when (result) {
            is StackResolver.ResolutionResult.Resolved -> {
                // Convert stack resolution events to action events
                for (stackEvent in result.events) {
                    events.addAll(convertStackEvent(stackEvent))
                }

                // Persist temporary modifiers as continuous effects
                val continuousEffects = result.temporaryModifiers.map { modifier ->
                    convertModifierToContinuousEffect(modifier, result.state.turnNumber)
                }

                result.state.addContinuousEffects(continuousEffects)
            }
            is StackResolver.ResolutionResult.Fizzled -> {
                for (stackEvent in result.events) {
                    events.addAll(convertStackEvent(stackEvent))
                }
                result.state
            }
            is StackResolver.ResolutionResult.EmptyStack -> {
                // Nothing to resolve
                state
            }
            is StackResolver.ResolutionResult.Error -> {
                throw IllegalStateException("Stack resolution error: ${result.message}")
            }
        }
    }

    /**
     * Convert a temporary Modifier to an ActiveContinuousEffect.
     *
     * By default, temporary modifiers from spells/abilities are "until end of turn"
     * effects. More specific durations would need to be encoded in the effect
     * definition or modifier itself.
     */
    private fun convertModifierToContinuousEffect(
        modifier: Modifier,
        currentTurn: Int
    ): ActiveContinuousEffect {
        // Look up the source name for a description
        // For now, use a generic description based on the modification type
        val description = when (val mod = modifier.modification) {
            is com.wingedsheep.rulesengine.ecs.layers.Modification.ModifyPT ->
                "+${mod.powerDelta}/+${mod.toughnessDelta} until end of turn"
            is com.wingedsheep.rulesengine.ecs.layers.Modification.AddKeyword ->
                "Gains ${mod.keyword.name.lowercase()} until end of turn"
            is com.wingedsheep.rulesengine.ecs.layers.Modification.SetPT ->
                "Becomes ${mod.power}/${mod.toughness} until end of turn"
            else -> "Continuous effect until end of turn"
        }

        return ActiveContinuousEffect.fromModifier(
            modifier = modifier,
            duration = EffectDuration.UntilEndOfTurn,
            description = description,
            createdOnTurn = currentTurn
        )
    }

    private fun executeCastSpell(
        state: GameState,
        action: CastSpell,
        events: MutableList<GameActionEvent>
    ): GameState {
        val container = state.getEntity(action.cardId)
            ?: throw IllegalStateException("Card not found: ${action.cardId}")
        val cardComponent = container.get<CardComponent>()
            ?: throw IllegalStateException("CardComponent missing")

        events.add(GameActionEvent.SpellCast(action.cardId, cardComponent.name, action.casterId))

        // Pay additional costs first (before moving spell to stack)
        var newState = payAdditionalCosts(state, action.casterId, action.additionalCostPayment, events)

        // Remove from source zone
        newState = newState.removeFromZone(action.cardId, action.fromZone)

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
     * Pay additional costs (sacrifice, discard, life, etc.) as part of casting a spell.
     */
    private fun payAdditionalCosts(
        state: GameState,
        playerId: EntityId,
        payment: com.wingedsheep.rulesengine.ability.AdditionalCostPayment,
        events: MutableList<GameActionEvent>
    ): GameState {
        var newState = state

        // Sacrifice permanents
        for (permanentId in payment.sacrificedPermanents) {
            val (resultState, resultEvents) = executeAction(newState, SacrificePermanent(permanentId, playerId))
            newState = resultState
            events.addAll(resultEvents)
        }

        // Discard cards
        for (cardId in payment.discardedCards) {
            val (resultState, resultEvents) = executeAction(newState, DiscardCard(playerId, cardId))
            newState = resultState
            events.addAll(resultEvents)
        }

        // Pay life
        if (payment.lifePaid > 0) {
            val (resultState, resultEvents) = executeAction(newState, LoseLife(playerId, payment.lifePaid))
            newState = resultState
            events.addAll(resultEvents)
        }

        // Exile cards
        for (cardId in payment.exiledCards) {
            val (resultState, resultEvents) = executeAction(newState, ExilePermanent(cardId))
            newState = resultState
            events.addAll(resultEvents)
        }

        // Tap permanents
        for (permanentId in payment.tappedPermanents) {
            val (resultState, resultEvents) = executeAction(newState, Tap(permanentId))
            newState = resultState
            events.addAll(resultEvents)
        }

        return newState
    }

    /**
     * Convert stack resolution events to action events.
     */
    private fun convertStackEvent(
        event: StackResolver.StackResolutionEvent
    ): List<GameActionEvent> {
        return when (event) {
            is StackResolver.StackResolutionEvent.SpellResolved ->
                listOf(GameActionEvent.SpellResolved(event.entityId, event.name))
            is StackResolver.StackResolutionEvent.SpellFizzled ->
                listOf(GameActionEvent.SpellFizzled(event.entityId, event.name, event.reason))
            is StackResolver.StackResolutionEvent.PermanentEnteredBattlefield ->
                listOf(GameActionEvent.PermanentEnteredBattlefield(event.entityId, event.name, event.controllerId))
            is StackResolver.StackResolutionEvent.SpellMovedToGraveyard ->
                listOf(GameActionEvent.CardMoved(event.entityId, event.name, ZoneId.STACK, ZoneId.graveyard(event.ownerId)))
            is StackResolver.StackResolutionEvent.AbilityResolved ->
                listOf(GameActionEvent.AbilityResolved(event.description, event.sourceId))
            is StackResolver.StackResolutionEvent.AbilityFizzled ->
                listOf(GameActionEvent.AbilityFizzled(event.description, event.sourceId, event.reason))
            is StackResolver.StackResolutionEvent.EffectEventWrapper ->
                emptyList() // Effect events are handled separately
        }
    }

    // =========================================================================
    // Attachment Actions
    // =========================================================================

    private fun executeAttach(
        state: GameState,
        action: Attach,
        events: MutableList<GameActionEvent>
    ): GameState {
        val attachmentName = state.getEntity(action.attachmentId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        val targetName = state.getEntity(action.targetId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(GameActionEvent.Attached(action.attachmentId, attachmentName, action.targetId, targetName))

        return state.updateEntity(action.attachmentId) { c ->
            c.with(AttachedToComponent(action.targetId))
        }
    }

    private fun executeDetach(
        state: GameState,
        action: Detach,
        events: MutableList<GameActionEvent>
    ): GameState {
        val attachmentName = state.getEntity(action.attachmentId)?.get<CardComponent>()?.definition?.name ?: "Unknown"
        events.add(GameActionEvent.Detached(action.attachmentId, attachmentName))

        return state.updateEntity(action.attachmentId) { c ->
            c.without<AttachedToComponent>()
        }
    }

    // =========================================================================
    // State-Based Actions
    // =========================================================================

    private fun executeCheckStateBasedActions(
        state: GameState,
        events: MutableList<GameActionEvent>
    ): GameState {
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
                    events.add(GameActionEvent.CreatureDied(entityId, cardComponent.definition.name, cardComponent.ownerId))

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
                    events.add(GameActionEvent.PlayerLost(playerId, "Life total reached 0 or less"))
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
                    events.add(GameActionEvent.PlayerLost(playerId, "10 or more poison counters"))
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
            events.add(GameActionEvent.GameEnded(activePlayers.first()))
            currentState = currentState.endGame(activePlayers.first())
        } else if (activePlayers.isEmpty()) {
            events.add(GameActionEvent.GameEnded(null))
            currentState = currentState.endGame(null)
        }

        return currentState
    }

    private fun executeClearDamage(state: GameState, action: ClearDamage): GameState {
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
     * the ResolveLegendRule action. No permanents are destroyed here.
     */
    private fun checkLegendRule(
        state: GameState,
        @Suppress("UNUSED_PARAMETER") events: MutableList<GameActionEvent>
    ): Pair<GameState, Boolean> {
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
        state: GameState,
        events: MutableList<GameActionEvent>
    ): Pair<GameState, Boolean> {
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
                events.add(GameActionEvent.PermanentDestroyed(entityId, cardComp.name))
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
                events.add(GameActionEvent.PermanentDestroyed(entityId, cardComp.name))
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
        state: GameState,
        @Suppress("UNUSED_PARAMETER") events: MutableList<GameActionEvent>
    ): Pair<GameState, Boolean> {
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
        state: GameState,
        @Suppress("UNUSED_PARAMETER") events: MutableList<GameActionEvent>
    ): Pair<GameState, Boolean> {
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

    // =========================================================================
    // Turn/Step Actions
    // =========================================================================

    /**
     * Perform the cleanup step at end of turn.
     *
     * Per Rule 514, the cleanup step:
     * 1. Active player discards down to maximum hand size (7 by default)
     * 2. All damage is removed from permanents
     * 3. All "until end of turn" and "this turn" effects end
     *
     * If the active player has more cards than their maximum hand size,
     * a PendingCleanupDiscard is created and must be resolved before
     * the turn can end.
     */
    private fun executePerformCleanupStep(
        state: GameState,
        action: PerformCleanupStep,
        @Suppress("UNUSED_PARAMETER") events: MutableList<GameActionEvent>
    ): GameState {
        var currentState = state

        // 1. Expire "until end of turn" continuous effects
        currentState = currentState.expireEndOfTurnEffects()

        // 2. Clear damage from all creatures
        for (entityId in currentState.getBattlefield()) {
            if (currentState.hasComponent<DamageComponent>(entityId)) {
                currentState = currentState.updateEntity(entityId) { c ->
                    c.without<DamageComponent>()
                }
            }
        }

        // 3. Check if active player needs to discard down to hand size
        // Default max hand size is 7 (would check for modifiers in a full implementation)
        val maxHandSize = 7
        val hand = currentState.getHand(action.playerId)
        val handSize = hand.size

        if (handSize > maxHandSize) {
            // Create a pending discard decision
            val discardCount = handSize - maxHandSize
            val pendingDiscard = com.wingedsheep.rulesengine.ecs.components.PendingCleanupDiscard(
                playerId = action.playerId,
                currentHandSize = handSize,
                maxHandSize = maxHandSize,
                discardCount = discardCount,
                cardsInHand = hand
            )
            currentState = currentState.addPendingCleanupDiscard(pendingDiscard)
        }

        return currentState
    }

    /**
     * Resolve a cleanup discard decision.
     *
     * The player has chosen which cards to discard to get down to their
     * maximum hand size.
     */
    private fun executeResolveCleanupDiscard(
        state: GameState,
        action: ResolveCleanupDiscard,
        events: MutableList<GameActionEvent>
    ): GameState {
        // Verify there's a pending discard for this player
        val pendingDiscard = state.getPendingCleanupDiscardForPlayer(action.playerId)
            ?: throw IllegalStateException("No pending cleanup discard for player ${action.playerId}")

        // Verify the correct number of cards is being discarded
        if (action.cardsToDiscard.size != pendingDiscard.discardCount) {
            throw IllegalArgumentException(
                "Must discard exactly ${pendingDiscard.discardCount} cards, but ${action.cardsToDiscard.size} were selected"
            )
        }

        // Verify all selected cards are in the player's hand
        val hand = state.getHand(action.playerId).toSet()
        for (cardId in action.cardsToDiscard) {
            if (cardId !in hand) {
                throw IllegalArgumentException("Card $cardId is not in player's hand")
            }
        }

        var currentState = state

        // Discard each card
        for (cardId in action.cardsToDiscard) {
            val cardComponent = currentState.getComponent<CardComponent>(cardId)
            val cardName = cardComponent?.name ?: "Unknown"

            events.add(GameActionEvent.CardDiscarded(action.playerId, cardId, cardName))

            // Move card from hand to graveyard
            val handZone = ZoneId.hand(action.playerId)
            val graveyardZone = ZoneId.graveyard(action.playerId)

            currentState = currentState
                .removeFromZone(cardId, handZone)
                .addToZone(cardId, graveyardZone)
        }

        // Remove the pending discard
        currentState = currentState.removePendingCleanupDiscardForPlayer(action.playerId)

        return currentState
    }

    /**
     * Expire "until end of combat" effects.
     * Called when combat ends.
     */
    private fun executeExpireEndOfCombatEffects(state: GameState): GameState {
        return state.expireEndOfCombatEffects()
    }

    /**
     * Expire effects that depend on a permanent that's leaving the battlefield.
     * Called when a permanent leaves to clean up WhileOnBattlefield and WhileAttached effects.
     */
    private fun executeExpireEffectsForPermanent(
        state: GameState,
        action: ExpireEffectsForPermanent
    ): GameState {
        return state.expireEffectsForLeavingPermanent(action.permanentId)
    }

    // =========================================================================
    // Decision Submission Actions
    // =========================================================================

    private val decisionResumer = DecisionResumer()

    /**
     * Execute a decision submission.
     *
     * This handles the player's response to a pending decision, using the
     * [DecisionResumer] to complete the effect based on the stored context.
     *
     * @throws IllegalStateException if there's no pending decision or context
     * @throws IllegalArgumentException if the response doesn't match the pending decision
     */
    private fun executeSubmitDecision(
        state: GameState,
        action: SubmitDecision,
        events: MutableList<GameActionEvent>
    ): GameState {
        // Verify there's a pending decision
        if (!state.isPausedForDecision) {
            throw IllegalStateException("Cannot submit decision: no pending decision in game state")
        }

        // Resume the effect with the player's response
        val result = decisionResumer.resume(state, action.response)

        // Convert EffectEvents to GameActionEvents
        // Note: In a full implementation, we'd have a proper event mapping
        // For now, we add a generic event to track the decision completion
        events.add(GameActionEvent.DecisionSubmitted(
            state.pendingDecision!!.playerId,
            state.pendingDecision!!.decisionId
        ))

        return result.state
    }
}

/**
 * Result of an action execution.
 */
sealed interface GameActionResult {
    data class Success(
        val state: GameState,
        val action: GameAction,
        val events: List<GameActionEvent> = emptyList()
    ) : GameActionResult

    data class Failure(
        val state: GameState,
        val action: GameAction,
        val reason: String
    ) : GameActionResult
}

/**
 * Events generated during action execution.
 */
@Serializable
sealed interface GameActionEvent {
    @Serializable
    data class LifeChanged(val playerId: EntityId, val oldLife: Int, val newLife: Int) : GameActionEvent
    @Serializable
    data class DamageDealtToPlayer(val sourceId: EntityId?, val targetId: EntityId, val amount: Int) : GameActionEvent
    @Serializable
    data class DamageDealtToCreature(val sourceId: EntityId?, val targetId: EntityId, val amount: Int) : GameActionEvent
    @Serializable
    data class ManaAdded(val playerId: EntityId, val color: String, val amount: Int) : GameActionEvent
    @Serializable
    data class CardDrawn(val playerId: EntityId, val cardId: EntityId, val cardName: String) : GameActionEvent
    @Serializable
    data class DrawFailed(val playerId: EntityId) : GameActionEvent
    @Serializable
    data class CardDiscarded(val playerId: EntityId, val cardId: EntityId, val cardName: String) : GameActionEvent
    @Serializable
    data class CardMoved(val entityId: EntityId, val cardName: String, val fromZone: ZoneId, val toZone: ZoneId) : GameActionEvent
    @Serializable
    data class PermanentDestroyed(val entityId: EntityId, val name: String) : GameActionEvent
    @Serializable
    data class CreatureDied(val entityId: EntityId, val name: String, val ownerId: EntityId) : GameActionEvent
    @Serializable
    data class CardExiled(val entityId: EntityId, val name: String) : GameActionEvent
    @Serializable
    data class CardReturnedToHand(val entityId: EntityId, val name: String) : GameActionEvent
    @Serializable
    data class PermanentTapped(val entityId: EntityId, val name: String) : GameActionEvent
    @Serializable
    data class PermanentUntapped(val entityId: EntityId, val name: String) : GameActionEvent
    @Serializable
    data class LandPlayed(val playerId: EntityId, val cardId: EntityId, val name: String) : GameActionEvent
    @Serializable
    data class CombatStarted(val attackingPlayerId: EntityId, val defendingPlayerId: EntityId) : GameActionEvent
    @Serializable
    data class AttackerDeclared(val creatureId: EntityId, val name: String) : GameActionEvent
    @Serializable
    data class BlockerDeclared(val blockerId: EntityId, val attackerId: EntityId, val name: String) : GameActionEvent
    @Serializable
    data class BlockersOrdered(val attackerId: EntityId, val orderedBlockerIds: List<EntityId>) : GameActionEvent
    @Serializable
    data class DamageDealt(val sourceId: EntityId, val targetId: EntityId, val amount: Int, val isCombatDamage: Boolean) : GameActionEvent
    @Serializable
    data class CombatEnded(val playerId: EntityId) : GameActionEvent
    @Serializable
    data class GameEnded(val winnerId: EntityId?) : GameActionEvent
    @Serializable
    data class PlayerLost(val playerId: EntityId, val reason: String) : GameActionEvent
    @Serializable
    data class Attached(val attachmentId: EntityId, val attachmentName: String, val targetId: EntityId, val targetName: String) : GameActionEvent
    @Serializable
    data class Detached(val attachmentId: EntityId, val name: String) : GameActionEvent

    // Stack resolution events
    @Serializable
    data class SpellCast(val entityId: EntityId, val name: String, val casterId: EntityId) : GameActionEvent
    @Serializable
    data class SpellResolved(val entityId: EntityId, val name: String) : GameActionEvent
    @Serializable
    data class SpellFizzled(val entityId: EntityId, val name: String, val reason: String) : GameActionEvent
    @Serializable
    data class PermanentEnteredBattlefield(val entityId: EntityId, val name: String, val controllerId: EntityId) : GameActionEvent
    @Serializable
    data class AbilityResolved(val description: String, val sourceId: EntityId) : GameActionEvent
    @Serializable
    data class AbilityFizzled(val description: String, val sourceId: EntityId, val reason: String) : GameActionEvent
    @Serializable
    data class CounterAdded(val entityId: EntityId, val name: String, val counterType: String, val count: Int) : GameActionEvent
    @Serializable
    data class CounterRemoved(val entityId: EntityId, val name: String, val counterType: String, val count: Int) : GameActionEvent

    // Decision events
    @Serializable
    data class DecisionSubmitted(val playerId: EntityId, val decisionId: String) : GameActionEvent
}
