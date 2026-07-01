package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
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
import com.wingedsheep.sdk.scripting.UntapLimitPerStep
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
        // CR 805.4 — in a shared team turn both teammates untap (and phase in / lose summoning
        // sickness) together. Without shared team turns (Team vs. Team — CR 808.4, non-team games)
        // only the active player untaps on their own turn.
        val activeTeam = state.sharedTurnTeam(activePlayer).toHashSet()

        val events = mutableListOf<GameEvent>()
        var newState = state

        // Phase in permanents that phased out under each active-team member's control. In a shared
        // team turn both teammates phase in together (CR 805.4); for a non-team game the active
        // team is just the active player. This happens during the untap step, before untapping
        // (Rule 702.26a).
        for (member in activeTeam) {
            newState = phaseInPermanents(newState, member, events)
        }

        // Check if the player has a SkipUntapComponent
        val skipUntap = newState.getEntity(activePlayer)?.get<SkipUntapComponent>()

        // Use projected state for controller checks (control-changing effects like Annex).
        // Recomputed from newState so just-phased-in permanents are visible.
        val projected = newState.projectedState

        // Find all tapped permanents controlled by the active team (CR 805.4)
        val permanentsToUntap = newState.entities.filter { (entityId, container) ->
            projected.getController(entityId) in activeTeam &&
                container.has<TappedComponent>()
        }.keys.filter { entityId ->
            // If there's a skip untap component, check if this permanent should be skipped
            if (skipUntap != null) {
                val cardComponent = newState.getEntity(entityId)?.get<CardComponent>()
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

        // Filter out permanents with CANT_UNTAP keyword (e.g., Goblin Sharpshooter).
        // Temporal Distortion's hourglass counters route through this same flag: it
        // grants DOESNT_UNTAP via a counter-keyed static ability (so the restriction
        // is projection-scoped and disappears if Temporal Distortion leaves play).
        val permanentsAfterCantUntap = permanentsToUntap.filter { entityId ->
            !projected.hasKeyword(entityId, AbilityFlag.DOESNT_UNTAP)
        }

        // Check if any permanents have MAY_NOT_UNTAP keyword (e.g., Everglove Courier)
        val mayNotUntapPermanents = permanentsAfterCantUntap.filter { entityId ->
            projected.hasKeyword(entityId, AbilityFlag.MAY_NOT_UNTAP)
        }

        // Untap-count restrictions (Damping Field — "can't untap more than one artifact"). A
        // global restriction: gather every active UntapLimitPerStep regardless of controller, and
        // for each work out which would-untap permanents match its filter. When more match than the
        // cap allows, the active player must keep the excess tapped (their choice which).
        val untapLimits = activeUntapLimits(newState).mapNotNull { (filter, max) ->
            val matching = permanentsAfterCantUntap.filter { entityId ->
                val container = newState.getEntity(entityId) ?: return@filter false
                matchesFilterForUntap(newState, projected, entityId, container, filter)
            }
            if (matching.size > max) UntapLimitChoice(matching, max) else null
        }
        val forcedKeepCount = untapLimits.sumOf { it.matchingPermanents.size - it.max }

        // Raise a single "keep tapped" decision when the player has any choice to make: optional
        // MAY_NOT_UNTAP permanents and/or a forced keep from an untap-count cap. The option pool is
        // the union of the optional permanents and every limit-constrained permanent.
        val choosablePermanents = (mayNotUntapPermanents + untapLimits.flatMap { it.matchingPermanents })
            .distinct()
        if (mayNotUntapPermanents.isNotEmpty() || forcedKeepCount > 0) {
            // Ask the player which permanents to keep tapped
            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = newState,
                playerId = activePlayer,
                sourceId = null,
                sourceName = null,
                prompt = "Select permanents to keep tapped",
                options = choosablePermanents,
                minSelections = forcedKeepCount,
                maxSelections = choosablePermanents.size,
                ordered = false,
                phase = DecisionPhase.STATE_BASED,
                useTargetingUI = true
            )

            val continuation = UntapChoiceContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                playerId = activePlayer,
                allPermanentsToUntap = permanentsAfterCantUntap,
                untapLimits = untapLimits
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                events + decisionResult.events
            )
        }

        // No MAY_NOT_UNTAP permanents — untap everything normally. Stun counters
        // replace each untap event per Rule 122.1d (handled by untapOrConsumeStun).
        for (entityId in permanentsAfterCantUntap) {
            val (afterUntap, untapEvents) = untapOrConsumeStun(newState, entityId, projected)
            newState = afterUntap
            events.addAll(untapEvents)
        }

        // Untap permanents for non-active players with UntapDuringOtherUntapSteps (e.g., Seedborn Muse)
        // or UntapFilteredDuringOtherUntapSteps (e.g., Ivorytusk Fortress)
        val projectedForSeedborn = newState.projectedState
        for (playerId in newState.turnOrder) {
            if (playerId in activeTeam) continue // active team already untapped above (CR 805.4)

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
                    // Another player's untap step (Seedborn Muse, etc.) — the
                    // "during your untap step" counter-removal replacement is not
                    // active here, so pass projected = null.
                    val (afterUntap, untapEvents) = untapOrConsumeStun(newState, entityId)
                    newState = afterUntap
                    events.addAll(untapEvents)
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
                        val (afterUntap, untapEvents) = untapOrConsumeStun(newState, entityId)
                        newState = afterUntap
                        events.addAll(untapEvents)
                        alreadyUntapped.add(entityId)
                    }
                }
            }
        }

        // Remove WhileSourceTapped floating effects whose source is no longer tapped
        newState = cleanupPhaseManager.cleanupWhileSourceTappedEffects(newState)

        // Remove summoning sickness from all creatures the active team controls (CR 805.4 — both
        // teammates' creatures lose summoning sickness on the team's turn; projected state).
        val projectedAfterUntap = newState.projectedState
        val creaturesToRefresh = newState.entities.filter { (entityId, container) ->
            projectedAfterUntap.getController(entityId) in activeTeam &&
                container.has<SummoningSicknessComponent>()
        }.keys

        for (entityId in creaturesToRefresh) {
            newState = newState.updateEntity(entityId) { it.without<SummoningSicknessComponent>() }
        }

        // Remove "entered this turn" tracking from all permanents
        val enteredThisTurn = newState.entities.filter { (_, container) ->
            container.has<EnteredThisTurnComponent>()
        }.keys
        for (entityId in enteredThisTurn) {
            newState = newState.updateEntity(entityId) { it.without<EnteredThisTurnComponent>() }
        }

        // Wipe "put into graveyard from battlefield this turn" markers on every turn
        // boundary so the predicate (Samwise, Lobelia — LTR) matches only cards that
        // arrived in a graveyard this turn, not last turn. Scans all entities (the
        // marker lives on graveyard cards, not battlefield permanents).
        val stampedThisTurn = newState.entities.filter { (_, container) ->
            container.has<com.wingedsheep.engine.state.components.identity.PutIntoGraveyardFromBattlefieldThisTurnMarker>()
        }.keys
        for (entityId in stampedThisTurn) {
            newState = newState.updateEntity(entityId) {
                it.without<com.wingedsheep.engine.state.components.identity.PutIntoGraveyardFromBattlefieldThisTurnMarker>()
            }
        }

        return ExecutionResult.success(newState, events)
    }

    /**
     * Phase in (Rule 702.26) every permanent that phased out under [activePlayer]'s
     * control. This is a turn-based action at the start of the untap step, before
     * untapping. Phased-in permanents keep their tapped state, counters, and
     * attachments; phasing is not a zone change, so no triggers fire.
     *
     * Phased-out attachments share the controller stamped at phase-out time, so they
     * are picked up by the same scan and phase in alongside their host.
     */
    private fun phaseInPermanents(
        state: GameState,
        activePlayer: EntityId,
        events: MutableList<GameEvent>
    ): GameState {
        val toPhaseIn = state.allBattlefieldEntities().filter { entityId ->
            val phased = state.getEntity(entityId)?.get<PhasedOutComponent>()
            // Permanents phased out "until source leaves" (Oubliette) don't phase in at untap —
            // they wait for the source's leaves-battlefield trigger.
            phased?.phasedOutByController == activePlayer && phased.phaseInOnSourceLeaves == null
        }

        var newState = state
        for (entityId in toPhaseIn) {
            val name = newState.getEntity(entityId)?.get<CardComponent>()?.name ?: "Permanent"
            newState = newState.updateEntity(entityId) { it.without<PhasedOutComponent>() }
            events.add(PhasedInEvent(entityId, name))
        }
        return newState
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
    /**
     * Collect the active untap-count caps (`UntapLimitPerStep`, e.g. Damping Field) as
     * `(filter, max)` pairs. The restriction is global, so every battlefield permanent's static
     * abilities are scanned regardless of controller. When two restrictions share a filter the
     * most restrictive (smallest [UntapLimitPerStep.max]) wins; distinct filters are kept separate.
     */
    private fun activeUntapLimits(
        state: GameState
    ): List<Pair<GameObjectFilter, Int>> {
        val byFilter = LinkedHashMap<GameObjectFilter, Int>()
        for (permanentId in state.getBattlefield()) {
            val card = state.getEntity(permanentId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                if (ability is UntapLimitPerStep) {
                    byFilter.merge(ability.filter, ability.max, ::minOf)
                }
            }
        }
        return byFilter.map { (filter, max) -> filter to max }
    }

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
                // Fail closed: an unhandled predicate (e.g. HasSubtype, IsLegendary,
                // HasColor) would silently match every entity if we fell through to
                // `true`, causing a filtered-untap ability to untap permanents it
                // shouldn't. Add explicit handling here when a new predicate is needed.
                else -> false
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
        // Graveyard-only predicate; untap filters never see a card with the marker.
        StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn -> false
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
        // Untap-during-other-untap-step filters only meaningfully restrict by counter type
        // and structural combinators. Tap / combat / face-down / damage-history / equipment
        // predicates would either be redundant at this point in the turn (e.g. IsTapped is
        // implied; combat is empty) or would require state we don't have here. Returning
        // true preserves the historical "no constraint" behavior, but the case is now
        // explicit so adding a new StatePredicate variant becomes a compile-time decision.
        StatePredicate.IsTapped,
        StatePredicate.IsUntapped,
        StatePredicate.IsAttacking,
        StatePredicate.IsBlocking,
        StatePredicate.IsBlocked,
        StatePredicate.IsUnblocked,
        StatePredicate.InSameBandAsSource,
        StatePredicate.IsBlockingSource,
        StatePredicate.CreatedBySource,
        StatePredicate.EnteredThisTurn,
        StatePredicate.WasDealtDamageThisTurn,
        StatePredicate.HasDealtDamage,
        StatePredicate.HasDealtCombatDamageToPlayer,
        StatePredicate.DealtCombatDamageToSourceControllerThisTurn,
        StatePredicate.AttackedThisTurn,
        StatePredicate.AttackedThisCombat,
        StatePredicate.BlockedThisCombat,
        StatePredicate.BlockedOrWasBlockedByLegendaryThisTurn,
        StatePredicate.IsFaceDown,
        StatePredicate.IsFaceUp,
        StatePredicate.HasMorphAbility,
        StatePredicate.IsRingBearer,
        StatePredicate.HasAnyCounter,
        StatePredicate.HasGreatestPower,
        StatePredicate.HasLeastPowerAmongAllCreatures,
        StatePredicate.HasLeastPower,
        StatePredicate.IsEquipped,
        StatePredicate.IsModified,
        StatePredicate.IsSaddled,
        StatePredicate.HasLockedDoor,
        StatePredicate.CrewedOrSaddledSourceThisTurn,
        StatePredicate.IsWarpExiled,
        StatePredicate.NotTargetedByAbilityFromSameNamedSource,
        StatePredicate.IsAttachedToBySource,
        StatePredicate.ExiledWithSource,
        StatePredicate.WasCastForWarp -> true
        is StatePredicate.AttachedToCardType -> true
        is StatePredicate.AttachedTo -> true
    }
}
