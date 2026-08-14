package com.wingedsheep.engine.handlers.effects.permanent.counters

import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.DecisionRequestedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.RemoveAnyNumberOfCountersContinuation
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.model.EntityId
import java.util.UUID

/**
 * The prompt-per-counter-kind walk behind
 * [com.wingedsheep.sdk.scripting.effects.RemoveAnyNumberOfCountersEffect], shared by
 * [RemoveAnyNumberOfCountersExecutor] (which starts it) and
 * [com.wingedsheep.engine.handlers.continuations.MiscContinuationResumer] (which continues it after
 * each answer) so the budget and floor arithmetic lives in exactly one place.
 *
 * Each kind currently on the target gets a `ChooseNumberDecision` in turn, bounded by:
 *  - **maximum** — that kind's live count, further clamped to the remaining `maxTotal` budget.
 *  - **minimum** — whatever of the remaining `minTotal` floor the *later* kinds can't cover. While
 *    the floor is still reachable from what's left the player chooses freely (0 is legal); on the
 *    last kind that can pay it the minimum rises to the shortfall. This is what makes "remove a
 *    counter" mandatory-once-resolved without dictating which kind comes off.
 *
 * A kind whose minimum and maximum coincide has no decision left to make, so it is applied
 * silently rather than asked — "remove a counter" from a permanent carrying only one kind never
 * raises a prompt at all.
 */
object RemoveAnyNumberOfCountersFlow {

    /** Where the walk ended: either everything is applied, or a player owes us an answer. */
    sealed interface Outcome {
        /** No prompt left to raise; [state] has every forced removal applied. */
        data class Done(val state: GameState, val events: List<GameEvent>) : Outcome

        /** [decision] is pending; [state] already carries it and the frame holding the rest of the walk. */
        data class Prompt(
            val state: GameState,
            val decision: ChooseNumberDecision,
            val events: List<GameEvent>
        ) : Outcome
    }

    /**
     * Walk [order] (counter kinds, in prompt order) applying forced removals until a genuine choice
     * is reached or the kinds run out.
     *
     * @param budget counters still removable in total, or null for no cap
     * @param floor counters that still *must* be removed in total
     */
    fun advance(
        state: GameState,
        targetId: EntityId,
        controllerId: EntityId,
        targetName: String,
        sourceId: EntityId?,
        sourceName: String?,
        order: List<String>,
        budget: Int?,
        floor: Int,
        priorEvents: List<GameEvent> = emptyList()
    ): Outcome {
        var currentState = state
        var remaining = order
        var remainingBudget = budget
        var remainingFloor = floor
        val events = priorEvents.toMutableList()

        while (remaining.isNotEmpty()) {
            if (remainingBudget != null && remainingBudget <= 0) break

            val kind = remaining.first()
            val rest = remaining.drop(1)
            val live = countOf(currentState, targetId, kind)
            if (live <= 0) {
                remaining = rest
                continue
            }

            val maxHere = remainingBudget?.let { minOf(live, it) } ?: live
            if (maxHere <= 0) break

            // What the kinds after this one could still supply toward the floor. Clamped to the
            // budget, since the budget bounds the total however it's spread across kinds.
            val othersRaw = rest.sumOf { countOf(currentState, targetId, it) }
            val others = remainingBudget?.let { minOf(othersRaw, it) } ?: othersRaw
            val minHere = (remainingFloor - others).coerceIn(0, maxHere)

            if (minHere == maxHere) {
                // Nothing left to decide — apply it rather than asking a question with one answer.
                val (applied, event) = removeCounters(currentState, targetId, kind, minHere, targetName)
                currentState = applied
                event?.let { events.add(it) }
                remainingBudget = remainingBudget?.minus(minHere)
                remainingFloor = (remainingFloor - minHere).coerceAtLeast(0)
                remaining = rest
                continue
            }

            val decisionId = UUID.randomUUID().toString()
            val decision = ChooseNumberDecision(
                id = decisionId,
                playerId = controllerId,
                prompt = "Remove how many $kind counters from $targetName? ($minHere-$maxHere)",
                context = DecisionContext(
                    sourceId = sourceId,
                    sourceName = sourceName,
                    phase = DecisionPhase.RESOLUTION
                ),
                minValue = minHere,
                maxValue = maxHere
            )
            val continuation = RemoveAnyNumberOfCountersContinuation(
                decisionId = decisionId,
                targetId = targetId,
                controllerId = controllerId,
                currentCounterType = kind,
                currentMinAmount = minHere,
                currentMaxAmount = maxHere,
                remainingCounterTypes = rest,
                targetName = targetName,
                sourceId = sourceId,
                sourceName = sourceName,
                remainingBudget = remainingBudget,
                remainingFloor = remainingFloor
            )
            events.add(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = controllerId,
                    decisionType = "CHOOSE_NUMBER",
                    prompt = decision.prompt
                )
            )
            return Outcome.Prompt(
                state = currentState.withPendingDecision(decision).pushContinuation(continuation),
                decision = decision,
                events = events
            )
        }

        return Outcome.Done(currentState, events)
    }

    /** Live count of [kind] on [targetId]; 0 when the entity is gone or tracks no counters. */
    fun countOf(state: GameState, targetId: EntityId, kind: String): Int =
        state.getEntity(targetId)
            ?.get<CountersComponent>()
            ?.getCount(resolveCounterType(kind))
            ?: 0

    /** Apply a removal, paired with the event to emit (null when [count] is 0). */
    fun removeCounters(
        state: GameState,
        targetId: EntityId,
        kind: String,
        count: Int,
        targetName: String
    ): Pair<GameState, CountersRemovedEvent?> {
        if (count <= 0) return state to null
        val counterType = resolveCounterType(kind)
        val current = state.getEntity(targetId)?.get<CountersComponent>() ?: return state to null
        val updated = state.updateEntity(targetId) { container ->
            container.with(current.withRemoved(counterType, count))
        }
        return updated to CountersRemovedEvent(targetId, kind, count, targetName)
    }
}
