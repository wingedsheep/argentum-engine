package com.wingedsheep.engine.handlers.actions.priority

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.PriorityChangedEvent
import com.wingedsheep.engine.core.StepChangedEvent
import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.event.TriggerDetector
import com.wingedsheep.engine.event.TriggerProcessor
import com.wingedsheep.engine.handlers.actions.ActionContext
import com.wingedsheep.engine.handlers.actions.ActionHandler
import com.wingedsheep.engine.mechanics.StateBasedActionChecker
import com.wingedsheep.engine.mechanics.stack.StackResolver
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.combat.BlockersDeclaredThisCombatComponent
import com.wingedsheep.engine.state.components.stack.ActivatedAbilityOnStackComponent
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.state.components.stack.TriggeredAbilityOnStackComponent
import com.wingedsheep.sdk.core.Step
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
                // Detect triggers from step transition events
                var currentState = advanceResult.newState
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

        val result = stackResolver.resolveTop(state)

        if (!result.isSuccess) {
            return result
        }

        // Detect triggers from resolution events BEFORE SBAs (so damage triggers
        // see the creature still on the battlefield, per MTG rules 603.10)
        val preSbaTriggers = triggerDetector.detectTriggers(result.newState, result.events)

        // Check state-based actions after resolution
        val sbaResult = sbaChecker.checkAndApply(result.newState)
        var combinedEvents = result.events + sbaResult.events

        if (sbaResult.newState.gameOver) {
            return ExecutionResult.success(sbaResult.newState, combinedEvents)
        }

        // Detect triggers from SBA events (e.g., death triggers) on post-SBA state
        val sbaTriggers = triggerDetector.detectTriggers(sbaResult.newState, sbaResult.events)
        val triggers = preSbaTriggers + sbaTriggers
        if (triggers.isNotEmpty()) {
            val triggerResult = triggerProcessor.processTriggers(sbaResult.newState, triggers)

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
            sbaResult.newState.withPriority(stackItemController),
            combinedEvents
        )
    }

    private fun advanceGame(state: GameState): ExecutionResult {
        return turnManager.advanceStep(state)
    }

    companion object {
        fun create(context: ActionContext): PassPriorityHandler {
            return PassPriorityHandler(
                context.turnManager,
                context.stackResolver,
                context.sbaChecker,
                context.triggerDetector,
                context.triggerProcessor
            )
        }
    }
}
