package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.SkipUntapComponent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.UntapFilteredDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.predicates.StatePredicate

/**
 * Handles beginning phase logic: untap step, upkeep step, and saga lore counters.
 */
class BeginningPhaseManager(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    private val decisionHandler: DecisionHandler,
    private val cleanupPhaseManager: CleanupPhaseManager
) {

    /**
     * Perform the untap step.
     * - Untap all permanents controlled by the active player
     * - Respects SkipUntapComponent which prevents certain permanents from untapping
     * - No priority is given during untap step
     */
    fun performUntapStep(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        val events = mutableListOf<GameEvent>()
        var newState = state

        // Check if the player has a SkipUntapComponent
        val skipUntap = newState.getEntity(activePlayer)?.get<SkipUntapComponent>()

        // Use projected state for controller checks (control-changing effects like Annex)
        val projected = state.projectedState

        // Find all tapped permanents controlled by the active player
        val permanentsToUntap = state.entities.filter { (entityId, container) ->
            projected.getController(entityId) == activePlayer &&
                container.has<TappedComponent>()
        }.keys.filter { entityId ->
            // If there's a skip untap component, check if this permanent should be skipped
            if (skipUntap != null) {
                val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                val typeLine = cardComponent?.typeLine
                val isCreature = typeLine?.isCreature == true
                val isLand = typeLine?.isLand == true

                // Skip this permanent if it matches the skip criteria
                val shouldSkip = (skipUntap.affectsCreatures && isCreature) ||
                    (skipUntap.affectsLands && isLand)
                !shouldSkip
            } else {
                true
            }
        }

        // Remove the SkipUntapComponent after processing (it's been consumed)
        if (skipUntap != null) {
            newState = newState.updateEntity(activePlayer) { container ->
                container.without<SkipUntapComponent>()
            }
        }

        // Filter out permanents with CANT_UNTAP keyword (e.g., Goblin Sharpshooter)
        val permanentsAfterCantUntap = permanentsToUntap.filter { entityId ->
            !projected.hasKeyword(entityId, AbilityFlag.DOESNT_UNTAP)
        }

        // Check if any permanents have MAY_NOT_UNTAP keyword (e.g., Everglove Courier)
        val mayNotUntapPermanents = permanentsAfterCantUntap.filter { entityId ->
            projected.hasKeyword(entityId, AbilityFlag.MAY_NOT_UNTAP)
        }

        if (mayNotUntapPermanents.isNotEmpty()) {
            // Ask the player which permanents to keep tapped
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = activePlayer,
                sourceId = null,
                sourceName = null,
                prompt = "Select permanents to keep tapped",
                options = mayNotUntapPermanents,
                minSelections = 0,
                maxSelections = mayNotUntapPermanents.size,
                ordered = false,
                phase = DecisionPhase.STATE_BASED,
                useTargetingUI = true
            )

            val continuation = UntapChoiceContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                playerId = activePlayer,
                allPermanentsToUntap = permanentsAfterCantUntap
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                events + decisionResult.events
            )
        }

        // No MAY_NOT_UNTAP permanents - untap everything normally
        for (entityId in permanentsAfterCantUntap) {
            val cardName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
            newState = newState.updateEntity(entityId) { it.without<TappedComponent>() }
            events.add(UntappedEvent(entityId, cardName))
        }

        // Untap permanents for non-active players with UntapDuringOtherUntapSteps (e.g., Seedborn Muse)
        // or UntapFilteredDuringOtherUntapSteps (e.g., Ivorytusk Fortress)
        val projectedForSeedborn = newState.projectedState
        for (playerId in newState.turnOrder) {
            if (playerId == activePlayer) continue

            var untapAll = false
            val filteredUntapFilters = mutableListOf<GameObjectFilter>()

            for (permanentId in projectedForSeedborn.getBattlefieldControlledBy(playerId)) {
                val card = newState.getEntity(permanentId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                for (ability in cardDef.script.staticAbilities) {
                    when (ability) {
                        is UntapDuringOtherUntapSteps -> untapAll = true
                        is UntapFilteredDuringOtherUntapSteps -> filteredUntapFilters.add(ability.filter)
                        else -> {}
                    }
                }
            }

            if (untapAll) {
                val tappedPermanents = newState.entities.filter { (entityId, container) ->
                    projectedForSeedborn.getController(entityId) == playerId &&
                        container.has<TappedComponent>() &&
                        !projectedForSeedborn.hasKeyword(entityId, AbilityFlag.DOESNT_UNTAP)
                }.keys
                for (entityId in tappedPermanents) {
                    val cardName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
                    newState = newState.updateEntity(entityId) { it.without<TappedComponent>() }
                    events.add(UntappedEvent(entityId, cardName))
                }
            } else if (filteredUntapFilters.isNotEmpty()) {
                val alreadyUntapped = mutableSetOf<EntityId>()
                for (filter in filteredUntapFilters) {
                    val tappedPermanents = newState.entities.filter { (entityId, container) ->
                        entityId !in alreadyUntapped &&
                            projectedForSeedborn.getController(entityId) == playerId &&
                            container.has<TappedComponent>() &&
                            !projectedForSeedborn.hasKeyword(entityId, AbilityFlag.DOESNT_UNTAP) &&
                            matchesFilterForUntap(newState, projectedForSeedborn, entityId, container, filter)
                    }.keys
                    for (entityId in tappedPermanents) {
                        val cardName = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
                        newState = newState.updateEntity(entityId) { it.without<TappedComponent>() }
                        events.add(UntappedEvent(entityId, cardName))
                        alreadyUntapped.add(entityId)
                    }
                }
            }
        }

        // Remove WhileSourceTapped floating effects whose source is no longer tapped
        newState = cleanupPhaseManager.cleanupWhileSourceTappedEffects(newState)

        // Remove summoning sickness from all creatures the player controls (using projected state)
        val projectedAfterUntap = newState.projectedState
        val creaturesToRefresh = newState.entities.filter { (entityId, container) ->
            projectedAfterUntap.getController(entityId) == activePlayer &&
                container.has<SummoningSicknessComponent>()
        }.keys

        for (entityId in creaturesToRefresh) {
            newState = newState.updateEntity(entityId) { it.without<SummoningSicknessComponent>() }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Perform the upkeep step.
     * - Triggers "at the beginning of your upkeep" abilities
     * - Players receive priority
     */
    fun performUpkeepStep(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        // Give priority to active player
        val newState = state.withPriority(activePlayer)

        return ExecutionResult.success(
            newState,
            listOf(StepChangedEvent(Step.UPKEEP))
        )
    }

    /**
     * Add a lore counter to each Saga the active player controls (Rule 714.3c).
     * This is a turn-based action that happens at the beginning of precombat main phase.
     */
    fun addLoreCountersToSagas(state: GameState, activePlayer: EntityId): ExecutionResult {
        var newState = state
        val events = mutableListOf<GameEvent>()

        val battlefieldZone = ZoneKey(activePlayer, Zone.BATTLEFIELD)
        for (entityId in newState.getZone(battlefieldZone)) {
            val container = newState.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val sagaComponent = container.get<SagaComponent>() ?: continue

            // This entity is a Saga — add a lore counter and mark newly triggered chapters
            val counters = container.get<CountersComponent>() ?: CountersComponent()
            val newLoreCount = counters.getCount(CounterType.LORE) + 1

            // Determine which chapters this lore counter triggers
            val cardDef = cardRegistry.getCard(cardComponent.cardDefinitionId)
            var updatedSaga = sagaComponent
            if (cardDef != null) {
                for (chapter in cardDef.sagaChapters) {
                    if (newLoreCount >= chapter.chapter && chapter.chapter !in sagaComponent.triggeredChapters) {
                        updatedSaga = updatedSaga.withChapterTriggered(chapter.chapter)
                    }
                }
            }

            newState = newState.updateEntity(entityId) { c ->
                c.with(counters.withAdded(CounterType.LORE, 1))
                    .with(updatedSaga)
            }
            events.add(CountersAddedEvent(entityId, "LORE", 1, cardComponent.name))
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Check if an entity matches a GameObjectFilter for untap-during-other-untap-step abilities.
     * Uses projected state for type checks and base state for counters.
     */
    private fun matchesFilterForUntap(
        state: GameState,
        projected: ProjectedState,
        entityId: EntityId,
        container: ComponentContainer,
        filter: GameObjectFilter
    ): Boolean {
        // Check card type predicates
        for (predicate in filter.cardPredicates) {
            val matches = when (predicate) {
                CardPredicate.IsCreature -> projected.isCreature(entityId)
                CardPredicate.IsLand -> projected.hasType(entityId, "LAND")
                CardPredicate.IsArtifact -> projected.hasType(entityId, "ARTIFACT")
                CardPredicate.IsEnchantment -> projected.hasType(entityId, "ENCHANTMENT")
                else -> true // Other card predicates not relevant for untap filtering
            }
            if (!matches) return false
        }
        // Check state predicates (e.g., HasCounter)
        for (predicate in filter.statePredicates) {
            if (!matchesStatePredicateForUntap(predicate, container)) return false
        }
        return true
    }

    private fun matchesStatePredicateForUntap(
        predicate: StatePredicate,
        container: ComponentContainer
    ): Boolean = when (predicate) {
        is StatePredicate.HasCounter -> {
            val countersComponent = container.get<CountersComponent>()
            if (countersComponent == null) {
                false
            } else {
                val counterType = when (predicate.counterType) {
                    "+1/+1" -> CounterType.PLUS_ONE_PLUS_ONE
                    "-1/-1" -> CounterType.MINUS_ONE_MINUS_ONE
                    else -> null
                }
                counterType != null && countersComponent.getCount(counterType) > 0
            }
        }
        is StatePredicate.Or -> predicate.predicates.any { matchesStatePredicateForUntap(it, container) }
        is StatePredicate.And -> predicate.predicates.all { matchesStatePredicateForUntap(it, container) }
        is StatePredicate.Not -> !matchesStatePredicateForUntap(predicate.predicate, container)
        else -> true // Other state predicates not relevant for untap filtering
    }
}
