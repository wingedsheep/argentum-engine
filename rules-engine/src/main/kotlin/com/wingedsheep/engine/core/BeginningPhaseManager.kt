package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.predicates.receivedCounterThisTurn
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.SagaComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ExertedComponent
import com.wingedsheep.engine.state.components.battlefield.HasDealtDamageComponent
import com.wingedsheep.engine.state.components.battlefield.PhasedOutComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.CardsInHandAtTurnStartComponent
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
import com.wingedsheep.sdk.scripting.UntapSelfDuringOtherUntapSteps
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

        // CR 502 — snapshot every player's hand size before anything else happens this turn.
        // "At the beginning of this turn" has to be captured here rather than read later: by the
        // upkeep, the very cards a card like Mindstorm Crown is measuring may already have moved.
        // Recorded for every player, not just the active one, so an opponent-scoped reading of the
        // same tracker is available for free.
        for (pid in newState.turnOrder) {
            val handSize = newState.getZone(ZoneKey(pid, Zone.HAND)).size
            newState = newState.updateEntity(pid) { container ->
                container.with(CardsInHandAtTurnStartComponent(count = handSize))
            }
        }

        // Phase in permanents that phased out under each active-team member's control. In a shared
        // team turn both teammates phase in together (CR 805.4); for a non-team game the active
        // team is just the active player. This happens during the untap step, before untapping
        // (Rule 702.26a).
        for (member in activeTeam) {
            newState = phaseInPermanents(newState, member, events)
        }

        // CR 502.2 / 731.2 — the second turn-based action of the untap step: check the previous
        // active side's spell counts and change the day/night designation if warranted. If it's day
        // and nobody on that side cast a spell, it becomes night; if it's night and any one of them
        // cast two or more, it becomes day; if it's neither, nothing happens (731.2c). No stack, no
        // priority. TurnManager.startTurn took the snapshot before resetting the counters. Any
        // daybound/nightbound transforms this designation change entails are
        // cascaded by DayNightService in the same event batch, and those events flow up through advanceStep
        // to PassPriorityHandler's detectTriggers so "whenever this transforms" abilities fire (CR 702.145b/e).
        run {
            val (afterDayNight, dayNightEvents) = com.wingedsheep.engine.mechanics.daynight.DayNightService
                .checkUntapStepDesignation(newState, cardRegistry)
            newState = afterDayNight
            events.addAll(dayNightEvents)
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

        // Filter out permanents that don't untap this step (e.g., Goblin Sharpshooter's
        // DOESNT_UNTAP, or the stronger CANT_BECOME_UNTAPPED from Blossombind).
        // Temporal Distortion's hourglass counters route through DOESNT_UNTAP via a
        // counter-keyed static ability (so the restriction is projection-scoped and
        // disappears if Temporal Distortion leaves play).
        //
        // Exerted permanents (CR 701.43a, ExertedComponent — a one-shot per-object marker, not a
        // continuous static ability) are filtered the same way. Unlike DOESNT_UNTAP, the marker is
        // cleared unconditionally below regardless of whether this filter actually skipped an
        // untap (2024-06-07 ruling: an exerted-but-already-untapped permanent's marker still
        // expires having done nothing).
        val permanentsAfterCantUntap = permanentsToUntap.filter { entityId ->
            !projected.doesntUntapDuringUntapStep(entityId) &&
                newState.getEntity(entityId)?.has<ExertedComponent>() != true
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
            val selfUntapIds = mutableListOf<EntityId>()

            for (permanentId in projectedForSeedborn.getBattlefieldControlledBy(playerId)) {
                val card = newState.getEntity(permanentId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                for (ability in cardDef.script.staticAbilities) {
                    when (ability) {
                        is UntapDuringOtherUntapSteps -> untapAll = true
                        is UntapFilteredDuringOtherUntapSteps -> filteredUntapFilters.add(ability.filter)
                        is UntapSelfDuringOtherUntapSteps -> selfUntapIds.add(permanentId)
                        else -> {}
                    }
                }
            }

            if (untapAll) {
                val tappedPermanents = newState.entities.filter { (entityId, container) ->
                    projectedForSeedborn.getController(entityId) == playerId &&
                        container.has<TappedComponent>() &&
                        !projectedForSeedborn.doesntUntapDuringUntapStep(entityId)
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
                            !projectedForSeedborn.doesntUntapDuringUntapStep(entityId) &&
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

            // Self-scoped untap (Bender's Waterskin — "Untap this artifact during each other
            // player's untap step"). Only the source permanent itself untaps. Guarded on
            // TappedComponent so it never double-processes a permanent the broad/filtered
            // branches above already untapped (avoids consuming a second stun counter).
            for (entityId in selfUntapIds) {
                val container = newState.getEntity(entityId) ?: continue
                if (!container.has<TappedComponent>()) continue
                if (projectedForSeedborn.doesntUntapDuringUntapStep(entityId)) continue
                val (afterUntap, untapEvents) = untapOrConsumeStun(newState, entityId)
                newState = afterUntap
                events.addAll(untapEvents)
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

        // Clear exert markers (CR 701.43a — "your next untap step") for every permanent the
        // active team controls, unconditionally: an exerted permanent that was already untapped
        // (or already had its untap replaced/skipped above) still has the marker expire here per
        // the 2024-06-07 ruling, having prevented nothing. Scoped to the active team, not every
        // permanent, since exert only ever refers to its controller's own next untap step.
        val exertedForActiveTeam = newState.entities.filter { (entityId, container) ->
            container.has<ExertedComponent>() && projectedAfterUntap.getController(entityId) in activeTeam
        }.keys
        for (entityId in exertedForActiveTeam) {
            newState = newState.updateEntity(entityId) { it.without<ExertedComponent>() }
        }

        // Wipe "put into a graveyard this turn" markers on every turn boundary so the
        // predicates (Abyssal Harvester — FDN; Samwise, Lobelia — LTR) match only cards
        // that arrived in a graveyard this turn, not last turn. Scans all entities (the
        // marker lives on graveyard cards, not battlefield permanents).
        val stampedThisTurn = newState.entities.filter { (_, container) ->
            container.has<com.wingedsheep.engine.state.components.identity.PutIntoGraveyardThisTurnComponent>()
        }.keys
        for (entityId in stampedThisTurn) {
            newState = newState.updateEntity(entityId) {
                it.without<com.wingedsheep.engine.state.components.identity.PutIntoGraveyardThisTurnComponent>()
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
        // Graveyard-only predicates; untap filters never see a card with the marker.
        StatePredicate.PutIntoGraveyardThisTurn -> false
        StatePredicate.PutIntoGraveyardFromBattlefieldThisTurn -> false
        // No granter context in untap filtering — granter-relative exclusion is resolution-time only.
        StatePredicate.IsGrantingPermanent -> false
        // Counter history is plain per-entity state, so answer it exactly rather than falling open.
        // In practice cleanup wiped the marker at the end of the previous turn, so this is false for
        // every permanent by the time the untap step runs on a normal turn.
        is StatePredicate.ReceivedCounterThisTurn ->
            receivedCounterThisTurn(container, predicate)
        // Damage history is likewise plain per-entity state, and both windows are answerable here
        // without the turn number. Every caller of this helper runs during an untap step — the first
        // step of the first phase of a turn (CR 500.1 / 501.1), in which no player receives priority
        // (CR 500.3) — and `turnNumber` has already been incremented by the time it runs, so nothing
        // can have dealt damage *this* turn yet: the per-turn window is exactly `false` for every
        // permanent. Falling into the "no constraint" group below would answer `true` instead, which
        // for an "each creature that dealt damage this turn" untap filter is the maximally wrong
        // answer (match everything rather than nothing). The lifetime window is just the marker.
        is StatePredicate.HasDealtDamage ->
            if (predicate.thisTurnOnly) false else container.has<HasDealtDamageComponent>()
        // Tap history, by the same argument as the per-turn damage window above: the untap step is
        // the first step of the turn, so no permanent has become tapped this turn yet and nothing can
        // have become tapped exactly once. Answered exactly rather than falling open, because an
        // "untap each creature that became tapped for the first time this turn" filter that matched
        // everything would untap the whole board.
        StatePredicate.BecameTappedOnlyOnceThisTurn -> false
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
        // Soulbond pairing (CR 702.95b) is plain per-entity state, so unlike the fail-open group
        // below it can be answered exactly here — an "untap each paired creature" filter must not
        // silently untap everything.
        StatePredicate.IsPaired ->
            container.has<com.wingedsheep.engine.state.components.battlefield.PairedComponent>()
        // Solved (CR 719.3b) is likewise plain per-entity state and survives the turn boundary,
        // so an "untap each solved Case" filter is answered exactly rather than falling open.
        StatePredicate.IsSolved ->
            container.has<com.wingedsheep.engine.state.components.battlefield.SolvedComponent>()
        is StatePredicate.Or -> predicate.predicates.any { matchesStatePredicateForUntap(it, container) }
        is StatePredicate.And -> predicate.predicates.all { matchesStatePredicateForUntap(it, container) }
        is StatePredicate.Not -> !matchesStatePredicateForUntap(predicate.predicate, container)
        // Relational battlefield predicates need the whole projected battlefield, which this
        // narrow untap helper deliberately does not receive. Fail closed rather than untapping an
        // unrelated permanent.
        is StatePredicate.HasLeastManaValueAmong -> false
        // Untap-during-other-untap-step filters only meaningfully restrict by counter type
        // and structural combinators. Tap / combat / face-down / damage-history / equipment
        // predicates would either be redundant at this point in the turn (e.g. IsTapped is
        // implied; combat is empty) or would require state we don't have here. Returning
        // true preserves the historical "no constraint" behavior, but the case is now
        // explicit so adding a new StatePredicate variant becomes a compile-time decision.
        StatePredicate.IsOnBattlefield,
        StatePredicate.IsTapped,
        StatePredicate.IsUntapped,
        StatePredicate.IsAttacking,
        StatePredicate.IsAttackingAlone,
        StatePredicate.IsAttackingAnOpponent,
        StatePredicate.IsAttackingYouOrYourPlaneswalkers,
        StatePredicate.IsBlocking,
        StatePredicate.IsBlocked,
        StatePredicate.IsUnblocked,
        StatePredicate.InSameBandAsSource,
        StatePredicate.IsBlockingSource,
        StatePredicate.CreatedBySource,
        StatePredicate.EnteredThisTurn,
        StatePredicate.WasDealtDamageThisTurn,
        StatePredicate.HasDealtCombatDamageToPlayer,
        StatePredicate.DealtCombatDamageToSourceControllerThisTurn,
        StatePredicate.ControllerDealtCombatDamageBySourceThisTurn,
        StatePredicate.AttackedThisTurn,
        StatePredicate.AttackedThisCombat,
        StatePredicate.BlockedThisCombat,
        StatePredicate.BlockedOrWasBlockedByLegendaryThisTurn,
        StatePredicate.IsFaceDown,
        StatePredicate.IsFaceUp,
        StatePredicate.HasMorphAbility,
        StatePredicate.HasDisguiseAbility,
        StatePredicate.IsRingBearer,
        StatePredicate.HasAnyCounter,
        StatePredicate.HasGreatestPower,
        StatePredicate.HasLeastPowerAmongAllCreatures,
        StatePredicate.HasLeastPower,
        StatePredicate.IsEquipped,
        StatePredicate.IsEnchanted,
        StatePredicate.IsModified,
        StatePredicate.IsSaddled,
        StatePredicate.IsSuspected,
        StatePredicate.HasLockedDoor,
        StatePredicate.CrewedOrSaddledSourceThisTurn,
        StatePredicate.CrewedOrSaddledBySourceThisTurn,
        StatePredicate.IsWarpExiled,
        StatePredicate.NotTargetedByAbilityFromSameNamedSource,
        StatePredicate.IsSource,
        StatePredicate.IsAttachedToBySource,
        StatePredicate.IsAttachedToSource,
        StatePredicate.ExiledWithSource,
        StatePredicate.WasCastForWarp -> true
        is StatePredicate.WasCastFromZone -> true
        is StatePredicate.AttachedToCardType -> true
        is StatePredicate.AttachedTo -> true
        is StatePredicate.IsEnchantedByAura -> true
    }
}
