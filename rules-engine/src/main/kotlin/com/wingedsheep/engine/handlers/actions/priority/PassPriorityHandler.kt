package com.wingedsheep.engine.handlers.actions.priority

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PendingTriggersContinuation
import com.wingedsheep.engine.core.PriorityChangedEvent
import com.wingedsheep.engine.core.StepChangedEvent
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.core.ZoneChangeEvent
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.CreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.player.NonTokenCreaturesDiedThisTurnComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import kotlin.reflect.KClass

/**
 * Handler for the PassPriority action.
 *
 * When a player passes priority:
 * - If all players have passed, resolve top of stack or advance game
 * - Otherwise, pass to next player
 */
class PassPriorityHandler(
    private val turnManager: TurnManager,
    private val stackResolver: StackResolver,
    private val sbaChecker: StateBasedActionChecker,
    private val triggerDetector: TriggerDetector,
    private val triggerProcessor: TriggerProcessor
) : ActionHandler<PassPriority> {
    override val actionType: KClass<PassPriority> = PassPriority::class

    override fun validate(state: GameState, action: PassPriority): String? {
        if (state.priorityPlayerId != action.playerId) {
            return "You don't have priority"
        }
        // Cannot pass priority while there's a pending decision
        val pendingDecision = state.pendingDecision
        if (pendingDecision != null) {
            return "Cannot pass priority while there's a pending decision - please respond to: ${pendingDecision.prompt}"
        }
        // Cannot pass priority during combat declaration steps until the declaration is submitted.
        // During DECLARE_ATTACKERS, the active player must submit DeclareAttackers before passing.
        // During DECLARE_BLOCKERS, the defending player must submit DeclareBlockers before passing.
        if (state.step == Step.DECLARE_ATTACKERS && action.playerId == state.activePlayerId) {
            val attackersDeclared = state.getEntity(action.playerId)
                ?.get<AttackersDeclaredThisCombatComponent>() != null
            if (!attackersDeclared) {
                return "You must declare attackers before passing priority"
            }
        }
        if (state.step == Step.DECLARE_BLOCKERS && action.playerId != state.activePlayerId) {
            val blockersDeclared = state.getEntity(action.playerId)
                ?.get<BlockersDeclaredThisCombatComponent>() != null
            if (!blockersDeclared) {
                return "You must declare blockers before passing priority"
            }
        }
        return null
    }

    override fun execute(state: GameState, action: PassPriority): ExecutionResult {
        val newState = state.withPriorityPassed(action.playerId)

        // Check if all players passed
        if (newState.allPlayersPassed()) {
            return if (newState.stack.isNotEmpty()) {
                resolveTopOfStack(newState)
            } else {
                val advanceResult = advanceGame(newState)
                if (!advanceResult.isSuccess || advanceResult.events.isEmpty()) {
                    return advanceResult
                }
                // Track nontoken creature deaths from step advancement (e.g., combat damage)
                var currentState = trackNonTokenCreatureDeaths(advanceResult.newState, advanceResult.events)
                val triggers = triggerDetector.detectTriggers(currentState, advanceResult.events).toMutableList()

                // Also detect delayed triggers and phase/step triggers for the new step
                val stepChangedEvent = advanceResult.events.filterIsInstance<StepChangedEvent>().lastOrNull()
                if (stepChangedEvent != null) {
                    val (delayedTriggers, consumedIds) = triggerDetector.detectDelayedTriggers(currentState, stepChangedEvent.newStep)
                    if (consumedIds.isNotEmpty()) {
                        currentState = currentState.removeDelayedTriggers(consumedIds)
                    }
                    triggers.addAll(delayedTriggers)

                    // Detect phase/step triggers (e.g., "At the beginning of your upkeep")
                    val activePlayer = currentState.activePlayerId
                    if (activePlayer != null) {
                        val phaseStepTriggers = triggerDetector.detectPhaseStepTriggers(
                            currentState, stepChangedEvent.newStep, activePlayer
                        )
                        triggers.addAll(phaseStepTriggers)
                    }
                }

                if (triggers.isNotEmpty()) {
                    val triggerResult = triggerProcessor.processTriggers(currentState, triggers)
                    if (triggerResult.isPaused) {
                        return ExecutionResult.paused(
                            triggerResult.state,
                            triggerResult.pendingDecision!!,
                            advanceResult.events + triggerResult.events
                        )
                    }
                    return ExecutionResult.success(
                        triggerResult.newState.withPriority(state.activePlayerId),
                        advanceResult.events + triggerResult.events
                    )
                }
                ExecutionResult.success(currentState, advanceResult.events)
            }
        }

        // Pass to next player
        val nextPlayer = state.getNextPlayer(action.playerId)
        return ExecutionResult.success(
            newState.copy(priorityPlayerId = nextPlayer),
            listOf(PriorityChangedEvent(nextPlayer))
        )
    }

    private fun resolveTopOfStack(state: GameState): ExecutionResult {
        // Determine who controlled the top stack item (caster/activator) so priority
        // returns to them after resolution, per MTG rule 117.3c
        val topId = state.getTopOfStack()
        val topContainer = topId?.let { state.getEntity(it) }
        val stackItemController = topContainer?.let { container ->
            container.get<SpellOnStackComponent>()?.casterId
                ?: container.get<TriggeredAbilityOnStackComponent>()?.controllerId
                ?: container.get<ActivatedAbilityOnStackComponent>()?.controllerId
        } ?: state.activePlayerId

        val preResolutionStackSize = state.continuationStack.size
        val result = stackResolver.resolveTop(state)

        // If resolution paused mid-way (e.g., Broken Bond destroys a creature then asks
        // "may put a land from hand"), triggers from events emitted before the pause
        // would otherwise be lost — they never reach detectTriggers because the paused
        // branch below returns early. Detect them now and queue them as a
        // PendingTriggersContinuation beneath the frames this resolution pushed, so
        // they fire after the spell finishes resolving (via checkForMoreContinuations).
        if (result.isPaused) {
            val triggers = triggerDetector.detectTriggers(
                trackNonTokenCreatureDeaths(result.newState, result.events),
                result.events
            )
            if (triggers.isNotEmpty()) {
                val pendingTriggers = PendingTriggersContinuation(
                    decisionId = "resolution-deferred-triggers-${java.util.UUID.randomUUID()}",
                    remainingTriggers = triggers
                )
                val stack = result.newState.continuationStack
                val newStack = stack.subList(0, preResolutionStackSize) +
                    pendingTriggers +
                    stack.subList(preResolutionStackSize, stack.size)
                return ExecutionResult.paused(
                    result.newState.copy(continuationStack = newStack),
                    result.pendingDecision!!,
                    result.events
                )
            }
            return result
        }

        if (!result.isSuccess) {
            return result
        }

        // Track nontoken creature deaths from resolution events
        val trackedState = trackNonTokenCreatureDeaths(result.newState, result.events)

        // Detect triggers from resolution events BEFORE SBAs (so damage triggers
        // see the creature still on the battlefield, per MTG rules 603.10)
        val preSbaTriggers = triggerDetector.detectTriggers(trackedState, result.events)

        // Check state-based actions after resolution
        val sbaResult = sbaChecker.checkAndApply(trackedState)

        // If SBA needs player input (e.g., legend rule), return paused
        if (sbaResult.isPaused) {
            return ExecutionResult.paused(
                sbaResult.state,
                sbaResult.pendingDecision!!,
                result.events + sbaResult.events
            )
        }

        var combinedEvents = result.events + sbaResult.events

        if (sbaResult.newState.gameOver) {
            return ExecutionResult.success(sbaResult.newState, combinedEvents)
        }

        // Track nontoken creature deaths from SBA events
        val sbaTrackedState = trackNonTokenCreatureDeaths(sbaResult.newState, sbaResult.events)

        // Detect triggers from SBA events (e.g., death triggers) on post-SBA state
        val sbaTriggers = triggerDetector.detectTriggers(sbaTrackedState, sbaResult.events)
        val triggers = preSbaTriggers + sbaTriggers
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(sbaTrackedState, triggers)

            if (triggerResult.isPaused) {
                return ExecutionResult.paused(
                    triggerResult.state,
                    triggerResult.pendingDecision!!,
                    combinedEvents + triggerResult.events
                )
            }

            combinedEvents = combinedEvents + triggerResult.events
            return ExecutionResult.success(
                triggerResult.newState.withPriority(stackItemController),
                combinedEvents
            )
        }

        return ExecutionResult.success(
            sbaTrackedState.withPriority(stackItemController),
            combinedEvents
        )
    }

    private fun advanceGame(state: GameState): ExecutionResult {
        return turnManager.advanceStep(state)
    }

    /**
     * No-op: creature death tracking is now done inline in ZoneTransitionService.moveToZone()
     * so that subsequent effects in the same chain can see updated counts.
     * This method is kept for API compatibility with the call sites.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun trackNonTokenCreatureDeaths(state: GameState, events: List<GameEvent>): GameState {
        return state
    }

    companion object {
        fun create(services: EngineServices): PassPriorityHandler {
            return PassPriorityHandler(
                services.turnManager,
                services.stackResolver,
                services.sbaChecker,
                services.triggerDetector,
                services.triggerProcessor
            )
        }
    }
}
