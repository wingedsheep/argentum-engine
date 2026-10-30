package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.event.StateTriggerPoller
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.replacement.ReplacementRiders
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.mechanics.TriggerOrdering
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.nameVisibleToAll
import com.wingedsheep.engine.state.components.player.EndTheTurnRequestedComponent
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.EventPattern

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
 * A player who orders their own triggers is asked for that order before the wave is placed
 * ([TriggerOrdering]); otherwise each player's abilities keep the order they triggered in.
 *
 * Putting a wave on the stack can stop for a choice (the order, a trigger's targets or modes). The rest of
 * that wave waits in [GameState.triggersBeingPlaced], and the drain after the answer places it
 * first, before any state-based action. Triggers that trigger in the meantime (ward, on the
 * answer's targets) join [GameState.pendingTriggers], the next wave, so they end up above it.
 *
 * Handlers only set who receives priority afterwards: an actor keeps it (CR 117.3c), and a
 * resolved stack object hands it on. Settling after it placed triggers only resets the passes,
 * because an object joined the stack (CR 117.4).
 *
 * Detection is single-pass: the events of putting abilities on the stack aren't scanned again. The
 * exception is CR 603.3b's second part, abilities that trigger on an attack- or entry-caused
 * ability or a Saga's final chapter ability triggering (Firebender Ascension, Strict Proctor,
 * Historian's Boon). That wave is scoped by
 * `triggerCause`, which is what makes it terminate: the meta-triggers themselves carry no cause.
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
    private val effectExecutor: ((GameState, Effect, EffectContext) -> EffectResult)? = null,
    /** Carries out a "restart the game" once the resolution that asked for it is over (CR 727). */
    private val gameRestarter: GameRestarter? = null,
    /** Starts a subgame once the effect that created it has run, and ends it when it's over (CR 729). */
    private val subgames: Subgames? = null
) {

    /**
     * Settle [executed] in its game. A subgame that is over hands its result to the main game it
     * was created from (CR 729.5), and the main game settles in turn: its own triggers and
     * state-based actions only see the events of its own game. The events returned are every
     * game's, in order, for the players to see.
     */
    fun settle(executed: ExecutionResult): ExecutionResult {
        val settled = settleThisGame(executed)
        if (settled.outcome is Outcome.Rejected || !settled.state.gameOver) return settled
        val ended = subgames?.takeIf { settled.state.mainGame != null }?.end(settled.state) ?: return settled
        val main = settle(ended)
        return main.copy(events = settled.events + main.events)
    }

    private fun settleThisGame(executed: ExecutionResult): ExecutionResult {
        if (executed.outcome is Outcome.Rejected) return executed
        if (executed.state.gameOver) return executed.copy(state = executed.state.withoutPendingTriggers())
        val current = restartIfRequested(executed)
        // Nothing has happened yet before the first turn begins: mulligans draw and shuffle, but
        // no one receives priority, and no permanent can have triggered. A restarted game is back
        // at that point, and nothing from the game it ended can trigger in the new one.
        if (mulligansInProgress(current.state)) return current

        val raw = runReplacementRiders(endTheTurnIfRequested(current))
        val result = com.wingedsheep.engine.mechanics.GraveyardOrdering.finish(raw)
        if (result.outcome is Outcome.Rejected) return result
        // An action that entered permanents outside an effect instruction (a resolving permanent
        // spell, a land play) is one event: a "the next time …" grant it spent is done. A paused
        // action may still be mid-event, so its grants wait for the answer to settle.
        val state = if (result.state.pendingDecision == null) {
            com.wingedsheep.engine.replacement.ActiveReplacements.purgeSpent(result.state)
        } else result.state
        // A lone priority pass changes nothing on the board, so there is nothing to detect,
        // check, or put on the stack. Skip the full SBA and poll sweep on the engine's most
        // frequent action.
        if (state.pendingTriggers.isEmpty() && state.triggersBeingPlaced.isEmpty() &&
            result.events.all { it is PriorityChangedEvent }) return result

        // Once the triggers are queued, a Siege's defeat trigger spares it on its own (CR 704.5v);
        // the pre-detection marker combat damage armed is spent.
        val detected = com.wingedsheep.engine.mechanics.battle.Battles.disarmDefeatTriggers(
            detect(state, triggeringEvents(result.events))
        )
        if (detected.pendingDecision != null) {
            return ExecutionResult.propagatePause(detected, result.events)
        }
        if (detected.pendingSubgame != null && subgames != null) return startSubgame(detected, result.events)
        return drain(detected, result.events)
    }

    /**
     * The resolving spell created a subgame (CR 729). The main game is discontinued mid-resolution,
     * so it performs no state-based action and puts no waiting trigger on the stack — those wait in
     * it until it resumes. The subgame starts and settles as a game of its own.
     */
    private fun startSubgame(main: GameState, mainEvents: List<GameEvent>): ExecutionResult {
        val started = subgames!!.start(main)
        val sub = settleThisGame(started)
        return sub.copy(events = mainEvents + sub.events)
    }

    /**
     * A player is still deciding on a mulligan. Only a player who has a mulligan record and hasn't
     * kept counts. A game set up past the opening hands, such as a test scenario, has no records.
     */
    private fun mulligansInProgress(state: GameState): Boolean =
        com.wingedsheep.engine.handlers.MulliganHandler.openingHandsPending(state)

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

    /**
     * A "restart the game" effect (CR 727) records its request while its ability resolves; the game
     * is restarted once that resolution is over, whether or not it stopped to ask a question first.
     */
    private fun restartIfRequested(result: ExecutionResult): ExecutionResult {
        val request = result.state.pendingRestart ?: return result
        val restarter = gameRestarter ?: return result
        if (result.state.pendingDecision != null) return result
        val restarted = restarter.restart(result.state, request)
        return restarted.copy(events = result.events + restarted.events)
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
        var working = ControlHistory.record(CounterHistory.record(state, events), events)
        val checkpoint = state.attackDeclarationCheckpoint()
        val declarationEvents = checkpoint?.events.orEmpty().toSet()
        val provisional = events.filter { it in declarationEvents }
        val triggers = if (provisional.isEmpty()) {
            triggerDetector.detectTriggers(working, events).toMutableList()
        } else {
            // No priority occurs before payment; keep these triggers queued but individually
            // reversible, without erasing triggers from later mana abilities on cancellation.
            (triggerDetector.detectTriggers(working, provisional).map {
                it.copy(attackDeclarationId = checkpoint!!.id)
            } + triggerDetector.detectTriggers(working, events.filter { it !in declarationEvents })).toMutableList()
        }

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
            // The rest of a wave whose placement stopped for a choice goes on the stack before
            // anything else happens: it was already ordered, and no player has received priority.
            val resuming = state.triggersBeingPlaced.isNotEmpty()
            if (!resuming) {
                val sba = sbaChecker.checkAndApply(
                    state,
                    state.pendingTriggers.mapNotNull { it.objectReferences.origin }.toSet()
                )
                events += sba.events
                state = sba.state
                // SBA-caused triggers join the queue whether or not the SBA stopped for a choice (the
                // legend rule); the ones already waiting stay put until the choice is answered.
                state = ControlHistory.record(CounterHistory.record(state, sba.events), sba.events)
                state = state.enqueue(triggerDetector.detectTriggers(state, sba.events))
                if (sba.outcome is Outcome.Rejected) return ExecutionResult(state, events, sba.outcome)
                if (sba.pendingDecision != null) return ExecutionResult.propagatePause(state, events)
                if (state.gameOver) return ExecutionResult.success(state.withoutPendingTriggers(), events)

                val poll = stateTriggerPoller.poll(state)
                state = poll.newState.enqueue(poll.pendingTriggers)

                if (state.pendingTriggers.isEmpty()) break
            }

            val waiting = if (resuming) state.triggersBeingPlaced
                else TriggerOrdering.suggested(apnapOrder(state, state.pendingTriggers))
            if (!resuming) {
                // A player who orders their own triggers (CR 603.3b) is asked before any of them
                // chooses targets; the wave waits, in APNAP order, until every answer is in.
                val ordering = TriggerOrdering.ask(state.copy(pendingTriggers = emptyList(), triggersBeingPlaced = waiting))
                if (ordering != null) return ExecutionResult.propagatePause(ordering.state, events + ordering.events)
            }
            val placementState =
                if (resuming) state.copy(triggersBeingPlaced = emptyList()) else state.copy(pendingTriggers = emptyList())
            val placed = triggerProcessor.processTriggers(placementState, waiting)
            events += placed.events
            if (placed.outcome is Outcome.Rejected) return ExecutionResult(placed.state, events, placed.outcome)
            placedAny = true
            state = placed.state

            // CR 603.3b, second part: abilities that trigger on an attack- or entry-caused ability,
            // or a final Saga chapter, triggering go on the stack after the first wave.
            val causeStamped = placed.events.filter { it is AbilityTriggeredEvent && it.triggerCause != null }
            if (causeStamped.isNotEmpty()) {
                state = state.enqueue(triggerDetector.detectTriggers(state, causeStamped))
            }
            if (placed.pendingDecision != null) return ExecutionResult.propagatePause(state, events)
            // An automatically removed state trigger immediately fires again. With no state
            // change or player choice, repeating this wave cannot reach priority (CR 104.4b).
            if (waiting.any { it.ability.trigger == EventPattern.StateConditionMetEvent } &&
                state == placementState && stateTriggerPoller.poll(state).pendingTriggers.isNotEmpty()
            ) {
                return ExecutionResult.success(
                    state.copy(gameOver = true, winnerId = null),
                    events + GameEndedEvent(null, GameEndReason.INFINITE_LOOP)
                )
            }
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

    /**
     * Queue [triggers], naming each by what every player may see of its source. A face-down
     * permanent's ward (CR 702.168a disguise, 701.58a cloak) is the one ability a hidden card can
     * trigger, and a trigger carrying the card's real name kept that name in the state on the
     * stack object — where nothing but the client masking stood between it and the opponent.
     */
    private fun GameState.enqueue(triggers: List<PendingTrigger>): GameState =
        if (triggers.isEmpty()) this else copy(pendingTriggers = pendingTriggers + triggers.map { trigger ->
            val publicName = nameVisibleToAll(this, trigger.sourceId, trigger.sourceName)
            if (publicName == trigger.sourceName) trigger else trigger.copy(sourceName = publicName)
        })

    private fun GameState.withoutPendingTriggers(): GameState =
        if (pendingTriggers.isEmpty() && triggersBeingPlaced.isEmpty()) this
        else copy(pendingTriggers = emptyList(), triggersBeingPlaced = emptyList())
}
