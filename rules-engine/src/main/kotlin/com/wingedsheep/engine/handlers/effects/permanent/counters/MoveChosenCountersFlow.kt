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
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId

/**
 * The prompt-per-counter-kind walk behind
 * [com.wingedsheep.sdk.scripting.effects.MoveChosenCountersToTargetEffect], shared by
 * [MoveChosenCountersToTargetExecutor] (which starts it) and
 * [com.wingedsheep.engine.handlers.continuations.MiscContinuationResumer] (which continues it after
 * each answer).
 *
 * The move-side twin of [RemoveAnyNumberOfCountersFlow]: each kind on the source gets a
 * `ChooseNumberDecision` bounded by the same budget-and-floor arithmetic
 * ([RemoveAnyNumberOfCountersFlow.kindBounds]), and a kind whose bounds coincide is moved without
 * asking. "Move a counter from target permanent you control onto a second target permanent"
 * (Nesting Grounds) is a budget and floor of one: a source carrying a single kind moves it
 * silently; with several kinds the player picks which one goes.
 */
object MoveChosenCountersFlow {

    /** Where the walk ended: everything applied, or a player owes an answer. */
    sealed interface Outcome {
        /** No prompt left; [state] has every forced move applied. [anyMoved] covers the whole walk. */
        data class Done(val state: GameState, val events: List<GameEvent>, val anyMoved: Boolean) : Outcome

        /** A decision is pending; [state] already carries it and the frame holding the rest of the walk. */
        data class Prompt(val state: GameState, val events: List<GameEvent>) : Outcome
    }

    /** The fixed facts of one walk, carried unchanged from prompt to prompt. */
    data class Move(
        val sourceId: EntityId,
        val destinationId: EntityId,
        val controllerId: EntityId,
        val sourceName: String,
        val destinationName: String,
        val drawCardOnMove: Boolean,
        val objectReferences: ObjectReferenceEnvironment = ObjectReferenceEnvironment()
    )

    /**
     * Walk [order] applying forced moves until a genuine choice is reached or the kinds run out.
     *
     * @param budget counters still movable in total, or null for no cap
     * @param floor counters that still *must* be moved in total
     */
    fun advance(
        state: GameState,
        move: Move,
        order: List<CounterType>,
        budget: Int?,
        floor: Int,
        anyMovedSoFar: Boolean,
        predicates: PredicateEvaluator,
        priorEvents: List<GameEvent> = emptyList()
    ): Outcome {
        var currentState = state
        var remaining = order
        var remainingBudget = budget
        var remainingFloor = floor
        var anyMoved = anyMovedSoFar
        val events = priorEvents.toMutableList()

        while (remaining.isNotEmpty()) {
            val kind = remaining.first()
            val rest = remaining.drop(1)
            val live = RemoveAnyNumberOfCountersFlow.countOf(currentState, move.sourceId, kind)
            if (live <= 0) {
                remaining = rest
                continue
            }

            val (minHere, maxHere) = RemoveAnyNumberOfCountersFlow.kindBounds(
                live = live,
                laterAvailable = rest.sumOf { RemoveAnyNumberOfCountersFlow.countOf(currentState, move.sourceId, it) },
                budget = remainingBudget,
                floor = remainingFloor
            ) ?: break

            if (minHere == maxHere) {
                // Nothing left to decide — apply it rather than asking a question with one answer.
                val (moved, moveEvents) = moveCounters(currentState, move, kind, minHere, predicates)
                currentState = moved
                events.addAll(moveEvents)
                if (moveEvents.isNotEmpty()) anyMoved = true
                remainingBudget = remainingBudget?.minus(minHere)
                remainingFloor = (remainingFloor - minHere).coerceAtLeast(0)
                remaining = rest
                continue
            }

            val decision = { decisionId: String -> ChooseNumberDecision(
                id = decisionId,
                playerId = move.controllerId,
                prompt = "Move how many ${kind.printed} counters from ${move.sourceName} onto ${move.destinationName}? ($minHere-$maxHere)",
                context = DecisionContext(
                    sourceId = move.sourceId,
                    sourceName = move.sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                minValue = minHere,
                maxValue = maxHere
            ) }
            val continuation = MoveChosenCountersToTargetContinuation(
                sourceId = move.sourceId,
                destinationId = move.destinationId,
                controllerId = move.controllerId,
                currentCounterType = kind,
                currentMaxAmount = maxHere,
                // The counts are informational — every later bound is recomputed from live counters.
                remainingCounterTypes = rest.map { it to RemoveAnyNumberOfCountersFlow.countOf(currentState, move.sourceId, it) },
                sourceName = move.sourceName,
                destinationName = move.destinationName,
                drawCardOnMove = move.drawCardOnMove,
                anyMovedSoFar = anyMoved,
                currentMinAmount = minHere,
                remainingBudget = remainingBudget,
                remainingFloor = remainingFloor,
                objectReferences = move.objectReferences
            )
            val pause = currentState.suspendForDecision(decision, continuation, events)
            return Outcome.Prompt(pause.state, pause.events)
        }

        return Outcome.Done(currentState, events, anyMoved)
    }

    /**
     * Move [count] of [kind] from the source onto the destination: removal first, then a placement
     * that honors counter-placement replacements (Hardened Scales). CR 122.5 — moving a counter
     * "puts" it onto the destination, so it's a placement by the controller of the moving effect.
     * Returns no events when nothing actually came off the source.
     */
    fun moveCounters(
        state: GameState,
        move: Move,
        kind: CounterType,
        count: Int,
        predicates: PredicateEvaluator
    ): Pair<GameState, List<GameEvent>> {
        val sourceCounters = state.getEntity(move.sourceId)?.get<CountersComponent>() ?: return state to emptyList()
        val removable = minOf(count, sourceCounters.getCount(kind))
        if (removable <= 0) return state to emptyList()

        var newState = state.updateEntity(move.sourceId) { it.with(sourceCounters.withRemoved(kind, removable)) }
        val events = mutableListOf<GameEvent>(CountersRemovedEvent(move.sourceId, kind, removable, move.sourceName))

        val placed = ReplacementEffectUtils.applyCounterPlacementModifiers(
            newState, move.destinationId, kind, removable,
            placerId = move.controllerId,
            predicateEvaluator = predicates
        )
        if (placed > 0) {
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
}
