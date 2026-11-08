package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.MoveChosenCountersToTargetContinuation
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.ObjectReferenceEnvironment
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId

/**
 * The prompt-per-(source, counter kind) walk behind
 * [com.wingedsheep.sdk.scripting.effects.MoveChosenCountersToTargetEffect], shared by
 * [MoveChosenCountersToTargetExecutor] (which starts it) and
 * [com.wingedsheep.engine.handlers.continuations.MiscContinuationResumer] (which continues it after
 * each answer).
 *
 * The move-side twin of [RemoveAnyNumberOfCountersFlow]: each step — one kind on one source — gets a
 * `ChooseNumberDecision` bounded by the same budget-and-floor arithmetic
 * ([RemoveAnyNumberOfCountersFlow.kindBounds]), and a step whose bounds coincide is taken without
 * asking. "Move a counter from target permanent you control onto a second target permanent"
 * (Nesting Grounds) is a budget and floor of one: a source carrying a single kind moves it
 * silently; with several kinds the player picks which one goes.
 *
 * Each answer takes its counters off its source at once, but nothing is put on the destination
 * until the walk ends: then each kind is placed in one go. The move is one event however many
 * sources and prompts it spans, so a placement replacement such as Hardened Scales ("if one or more
 * +1/+1 counters would be put on a creature …, that many plus one") applies once, not once per
 * source — Aetherborn Marauder gathering from three permanents gets one extra counter.
 */
object MoveChosenCountersFlow {

    /** Where the walk ended: everything applied, or a player owes an answer. */
    sealed interface Outcome {
        /** No prompt left; [state] has every move applied. [anyMoved] covers the whole walk. */
        data class Done(val state: GameState, val events: List<GameEvent>, val anyMoved: Boolean) : Outcome

        /** A decision is pending; [state] already carries it and the frame holding the rest of the walk. */
        data class Prompt(val state: GameState, val events: List<GameEvent>) : Outcome
    }

    /** The fixed facts of one walk, carried unchanged from prompt to prompt. */
    data class Move(
        val destinationId: EntityId,
        val controllerId: EntityId,
        val destinationName: String,
        val drawCardOnMove: Boolean,
        val objectReferences: ObjectReferenceEnvironment = ObjectReferenceEnvironment()
    )

    /** The counters taken off sources so far and not yet put on the destination, per kind. */
    typealias Pending = List<Pair<CounterType, Int>>

    /**
     * Walk [steps] (source, kind) taking forced moves until a genuine choice is reached or the steps
     * run out; on running out, place everything [pending] on the destination.
     *
     * @param budget counters still movable in total, or null for no cap
     * @param floor counters that still *must* be moved in total
     */
    fun advance(
        state: GameState,
        move: Move,
        steps: List<Pair<EntityId, CounterType>>,
        budget: Int?,
        floor: Int,
        pending: Pending,
        anyMovedSoFar: Boolean,
        predicates: PredicateEvaluator,
        priorEvents: List<GameEvent> = emptyList()
    ): Outcome {
        var currentState = state
        var remaining = steps
        var remainingBudget = budget
        var remainingFloor = floor
        var held = pending
        var anyMoved = anyMovedSoFar
        val events = priorEvents.toMutableList()

        while (remaining.isNotEmpty()) {
            val (sourceId, kind) = remaining.first()
            val rest = remaining.drop(1)
            val live = RemoveAnyNumberOfCountersFlow.countOf(currentState, sourceId, kind)
            if (live <= 0) {
                remaining = rest
                continue
            }

            val (minHere, maxHere) = RemoveAnyNumberOfCountersFlow.kindBounds(
                live = live,
                laterAvailable = rest.sumOf { (id, k) -> RemoveAnyNumberOfCountersFlow.countOf(currentState, id, k) },
                budget = remainingBudget,
                floor = remainingFloor
            ) ?: break

            if (minHere == maxHere) {
                // Nothing left to decide — take it rather than asking a question with one answer.
                val taken = take(currentState, move, sourceId, kind, minHere)
                currentState = taken.state
                events.addAll(taken.events)
                held = held.plusCount(kind, taken.count)
                if (taken.count > 0) anyMoved = true
                remainingBudget = remainingBudget?.minus(minHere)
                remainingFloor = (remainingFloor - minHere).coerceAtLeast(0)
                remaining = rest
                continue
            }

            val sourceName = nameOf(currentState, sourceId)
            val decision = { decisionId: String -> ChooseNumberDecision(
                id = decisionId,
                playerId = move.controllerId,
                prompt = "Move how many ${kind.printed} counters from $sourceName onto ${move.destinationName}? ($minHere-$maxHere)",
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                minValue = minHere,
                maxValue = maxHere
            ) }
            val continuation = MoveChosenCountersToTargetContinuation(
                sourceId = sourceId,
                destinationId = move.destinationId,
                controllerId = move.controllerId,
                currentCounterType = kind,
                currentMaxAmount = maxHere,
                remainingSteps = rest,
                destinationName = move.destinationName,
                drawCardOnMove = move.drawCardOnMove,
                anyMovedSoFar = anyMoved,
                currentMinAmount = minHere,
                remainingBudget = remainingBudget,
                remainingFloor = remainingFloor,
                pendingPlacements = held,
                objectReferences = move.objectReferences
            )
            val pause = currentState.suspendForDecision(decision, continuation, events)
            return Outcome.Prompt(pause.state, pause.events)
        }

        val (placed, placeEvents) = place(currentState, move, held, predicates)
        return Outcome.Done(placed, events + placeEvents, anyMoved)
    }

    /** What one step took off its source: the state after, its removal event, and the count. */
    data class Taken(val state: GameState, val events: List<GameEvent>, val count: Int)

    /**
     * Take [count] of [kind] off [sourceId] toward the destination. CR 122.5: a counter the
     * destination can't have isn't removed from the source either, so nothing is taken then.
     */
    fun take(state: GameState, move: Move, sourceId: EntityId, kind: CounterType, count: Int): Taken {
        if (count <= 0) return Taken(state, emptyList(), 0)
        if (!state.projectedState.canReceiveCounters(move.destinationId, kind)) return Taken(state, emptyList(), 0)
        val sourceCounters = state.getEntity(sourceId)?.get<CountersComponent>() ?: return Taken(state, emptyList(), 0)
        val removable = minOf(count, sourceCounters.getCount(kind))
        if (removable <= 0) return Taken(state, emptyList(), 0)
        val newState = state.updateEntity(sourceId) { it.with(sourceCounters.withRemoved(kind, removable)) }
        val event = CountersRemovedEvent(sourceId, kind, removable, nameOf(state, sourceId), removedBy = move.controllerId)
        return Taken(newState, listOf(event), removable)
    }

    /** Add [count] of [kind] to the held counters. */
    fun Pending.plusCount(kind: CounterType, count: Int): Pending = when {
        count <= 0 -> this
        any { it.first == kind } -> map { if (it.first == kind) kind to it.second + count else it }
        else -> this + (kind to count)
    }

    /**
     * Put every held kind on the destination, each as one placement that honors counter-placement
     * replacements (Hardened Scales). Moving a counter "puts" it onto the destination, so it's a
     * placement by the controller of the moving effect.
     */
    private fun place(
        state: GameState,
        move: Move,
        held: Pending,
        predicates: PredicateEvaluator
    ): Pair<GameState, List<GameEvent>> {
        var newState = state
        val events = mutableListOf<GameEvent>()
        for ((kind, count) in held) {
            val placed = ReplacementEffectUtils.applyCounterPlacementModifiers(
                newState, move.destinationId, kind, count,
                placerId = move.controllerId,
                predicateEvaluator = predicates, byEffect = true
            )
            if (placed <= 0) continue
            val destCounters = newState.getEntity(move.destinationId)?.get<CountersComponent>() ?: CountersComponent()
            newState = newState.updateEntity(move.destinationId) { it.with(destCounters.withAdded(kind, placed)) }
            val (afterMark, firstThisTurn, firstOfTypeThisTurn) =
                DamageUtils.recordCounterPlacement(newState, move.destinationId, kind, placerId = move.controllerId)
            newState = afterMark
            events.add(
                CountersAddedEvent(
                    move.destinationId, kind, placed, move.destinationName,
                    firstThisTurn, firstOfTypeThisTurn = firstOfTypeThisTurn,
                    placedBy = move.controllerId
                )
            )
        }
        return newState to events
    }

    private fun nameOf(state: GameState, entityId: EntityId): String =
        state.getEntity(entityId)?.get<CardComponent>()?.name ?: ""
}
