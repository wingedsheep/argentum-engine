package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.event.StateTriggerPoller
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.replacement.ReplacementRiders
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.EndTheTurnRequestedComponent
import com.wingedsheep.engine.state.components.player.MulliganStateComponent
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * The engine's single settle boundary. [ActionProcessor] runs it once after every accepted action.
 * It is the only code that turns events into triggered abilities.
 *
 * Each time a player would receive priority, the game first performs state-based actions, then
 * puts waiting triggered abilities on the stack, and repeats until neither happens. Only then does
 * the player receive priority (CR 117.5, 704.3). A triggered ability waits from the moment it
 * triggers until that point (CR 603.3), and those waiting abilities live in
 * [GameState.pendingTriggers]:
 *
 * 1. **Detect.** Triggers from the action's events, from a step it began (phase/step and delayed
 *    triggers), and from the state-trigger poll (CR 603.8) join the queue. Detection runs on the
 *    post-action state, before any state-based action, so an ability whose source an SBA is about
 *    to remove still triggers (CR 603.6a, 603.10).
 * 2. **Pause.** If the action ended on a question, the queue waits in the state. A paused action
 *    is not a moment anyone receives priority, and the question's answer is itself an action that
 *    settles.
 * 3. **Drain.** Perform state-based actions and queue the triggers they cause. Then put the whole
 *    queue on the stack in APNAP order (CR 603.3b). Repeat until nothing is left.
 *
 * Handlers only set who receives priority afterwards: an actor keeps it (CR 117.3c), and a
 * resolved stack object hands it on. Settling after it placed triggers only resets the passes,
 * because an object joined the stack (CR 117.4).
 *
 * Detection is single-pass: the events of putting abilities on the stack aren't scanned again. The
 * exception is CR 603.3b's second part, abilities that trigger on an attack-caused ability
 * triggering (Firebender Ascension). That wave is scoped by `causedByAttack`, which is what makes
 * it terminate.
 *
 * Because every action's and every SBA's events pass through here, turn history that no single
 * emitter owns is recorded alongside detection: [CounterHistory] credits counters removed from
 * permanents, whatever path removed them.
 */
class Settler(
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor,
    private val sbaChecker: StateBasedActionChecker,
    private val stateTriggerPoller: StateTriggerPoller,
    private val turnManager: TurnManager,
    /**
     * Runs a prevention effect's owed result (see [ReplacementRiders]). Combat damage isn't an
     * effect, so the results its prevention owes are run here, before detection and SBAs.
     */
    private val effectExecutor: ((GameState, Effect, EffectContext) -> EffectResult)? = null
) {

    fun settle(executed: ExecutionResult): ExecutionResult {
        if (executed.outcome is Outcome.Rejected) return executed
        if (executed.state.gameOver) return executed.copy(state = executed.state.withoutPendingTriggers())
        // Nothing has happened yet before the first turn begins: mulligans draw and shuffle, but
        // no one receives priority, and no permanent can have triggered.
        if (mulligansInProgress(executed.state)) return executed

        val result = runReplacementRiders(endTheTurnIfRequested(executed))
        if (result.outcome is Outcome.Rejected) return result
        val state = result.state
        // A lone priority pass changes nothing on the board, so there is nothing to detect,
        // check, or put on the stack. Skip the full SBA and poll sweep on the engine's most
        // frequent action.
        if (state.pendingTriggers.isEmpty() && result.events.all { it is PriorityChangedEvent }) return result

        // Once the triggers are queued, a Siege's defeat trigger spares it on its own (CR 704.5v);
        // the pre-detection marker combat damage armed is spent.
        val detected = com.wingedsheep.engine.mechanics.battle.Battles.disarmDefeatTriggers(
            detect(state, triggeringEvents(result.events))
        )
        if (detected.pendingDecision != null) {
            return ExecutionResult.propagatePause(detected, result.events)
        }
        return drain(detected, result.events)
    }

    /**
     * A player is still deciding on a mulligan. Only a player who has a mulligan record and hasn't
     * kept counts. A game set up past the opening hands, such as a test scenario, has no records.
     */
    private fun mulligansInProgress(state: GameState): Boolean = state.turnOrder.any { playerId ->
        state.getEntity(playerId)?.get<MulliganStateComponent>()?.hasKept == false
    }

    /**
     * An "end the turn" effect (CR 724.1) marks the active player while its spell or ability
     * resolves, and the expedited process runs once that resolution is over. Doing it here covers a
     * resolution that finished with no question asked and one that finished with the answer to a
     * question.
     */
    private fun endTheTurnIfRequested(result: ExecutionResult): ExecutionResult {
        val state = result.state
        if (state.pendingDecision != null) return result
        val activePlayer = state.activePlayerId ?: return result
        if (state.getEntity(activePlayer)?.has<EndTheTurnRequestedComponent>() != true) return result
        val ended = turnManager.performEndTheTurn(state)
        return ended.copy(events = result.events + ended.events)
    }

    private fun runReplacementRiders(result: ExecutionResult): ExecutionResult {
        val executor = effectExecutor ?: return result
        if (result.state.pendingReplacementRiders.isEmpty() || result.outcome !is Outcome.Done) return result
        val drained = ReplacementRiders.drain(result.state, executor)
        return ExecutionResult(drained.state, result.events + drained.events, drained.outcome)
    }

    /**
     * CR 724.1a: when an effect ends the turn, abilities that triggered before that process began
     * cease to exist. Only events after the last [TurnEndedByEffectEvent] can trigger anything.
     */
    private fun triggeringEvents(events: List<GameEvent>): List<GameEvent> {
        val lastTurnEnd = events.indexOfLast { it is TurnEndedByEffectEvent }
        return if (lastTurnEnd < 0) events else events.subList(lastTurnEnd + 1, events.size)
    }

    /** Queue every trigger [events] caused, including those of a step they began. */
    private fun detect(state: GameState, events: List<GameEvent>): GameState {
        var working = ControlHistory.record(CounterHistory.recordRemovals(state, events), events)
        val triggers = triggerDetector.detectTriggers(working, events).toMutableList()

        val stepChanged = events.filterIsInstance<StepChangedEvent>().lastOrNull()
        if (stepChanged != null) {
            val (delayed, consumedIds) = triggerDetector.detectDelayedTriggers(working, stepChanged.newStep)
            if (consumedIds.isNotEmpty()) working = working.removeDelayedTriggers(consumedIds)
            triggers += delayed
            working.activePlayerId?.let { activePlayer ->
                triggers += triggerDetector.detectPhaseStepTriggers(working, stepChanged.newStep, activePlayer)
            }
        }
        return working.enqueue(triggers)
    }

    private fun drain(start: GameState, priorEvents: List<GameEvent>): ExecutionResult {
        var state = start
        val events = priorEvents.toMutableList()
        var placedAny = false

        while (true) {
            val sba = sbaChecker.checkAndApply(
                state,
                state.pendingTriggers.mapNotNull { it.objectReferences.origin }.toSet()
            )
            events += sba.events
            state = sba.state
            // SBA-caused triggers join the queue whether or not the SBA stopped for a choice (the
            // legend rule); the ones already waiting stay put until the choice is answered.
            state = ControlHistory.record(CounterHistory.recordRemovals(state, sba.events), sba.events)
            state = state.enqueue(triggerDetector.detectTriggers(state, sba.events))
            if (sba.pendingDecision != null) return ExecutionResult.propagatePause(state, events)
            if (state.gameOver) return ExecutionResult.success(state.withoutPendingTriggers(), events)

            val poll = stateTriggerPoller.poll(state)
            state = poll.newState.enqueue(poll.pendingTriggers)

            if (state.pendingTriggers.isEmpty()) break

            val waiting = apnapOrder(state, state.pendingTriggers)
            val placed = triggerProcessor.processTriggers(state.withoutPendingTriggers(), waiting)
            events += placed.events
            if (placed.outcome is Outcome.Rejected) return ExecutionResult(placed.state, events, placed.outcome)
            placedAny = true
            state = placed.state

            // CR 603.3b, second part: abilities that trigger on an attack-caused ability
            // triggering go on the stack after the first wave.
            val attackCaused = placed.events.filter { it is AbilityTriggeredEvent && it.causedByAttack }
            if (attackCaused.isNotEmpty()) {
                state = state.enqueue(triggerDetector.detectTriggers(state, attackCaused))
            }
            if (placed.pendingDecision != null) return ExecutionResult.propagatePause(state, events)
        }

        if (placedAny) state = state.withPriority(state.priorityPlayerId)
        return ExecutionResult.success(state, events)
    }

    /**
     * Stable APNAP sort (CR 603.3b). Each detection pass already returns its triggers in APNAP
     * order. The queue can hold several passes, e.g. one parked across a pause and one from the
     * answer, and a stable sort keeps each controller's own triggers in detection order.
     */
    private fun apnapOrder(state: GameState, triggers: List<PendingTrigger>): List<PendingTrigger> {
        val activeIndex = state.turnOrder.indexOf(state.activePlayerId).coerceAtLeast(0)
        val playerOrder = state.turnOrder.drop(activeIndex) + state.turnOrder.take(activeIndex)
        return triggers.sortedBy { trigger ->
            playerOrder.indexOf(trigger.controllerId).let { if (it < 0) playerOrder.size else it }
        }
    }

    private fun GameState.enqueue(triggers: List<PendingTrigger>): GameState =
        if (triggers.isEmpty()) this else copy(pendingTriggers = pendingTriggers + triggers)

    private fun GameState.withoutPendingTriggers(): GameState =
        if (pendingTriggers.isEmpty()) this else copy(pendingTriggers = emptyList())
}
