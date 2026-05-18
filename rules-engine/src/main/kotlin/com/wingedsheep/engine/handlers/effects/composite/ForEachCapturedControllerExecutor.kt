package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.EffectContinuation
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.ForEachPlayerContinuation
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachCapturedControllerEffect
import kotlin.reflect.KClass

/**
 * Executor for [ForEachCapturedControllerEffect].
 *
 * Cross-references three pipeline-stored lists, builds a per-controller tally, and
 * runs the sub-effects pipeline once per tallied controller (in turn order, starting
 * from the active player) with `context.controllerId` set to that player and
 * `storedNumbers[countVariable]` set to that player's count.
 *
 * Unlike [ForEachPlayerExecutor], the outer pipeline's `storedCollections` are
 * preserved per iteration so sub-effects can reference the same collections that
 * drove the tally.
 *
 * Continuation handling mirrors [ForEachPlayerExecutor] — sub-effect pauses re-enter
 * via [ForEachPlayerContinuation] over the remaining controllers.
 */
class ForEachCapturedControllerExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<ForEachCapturedControllerEffect> {

    override val effectType: KClass<ForEachCapturedControllerEffect> =
        ForEachCapturedControllerEffect::class

    override fun execute(
        state: GameState,
        effect: ForEachCapturedControllerEffect,
        context: EffectContext
    ): EffectResult {
        val deadCards = context.pipeline.storedCollections[effect.collection]
            ?: return EffectResult.error(state, "No collection named '${effect.collection}' in storedCollections")
        if (deadCards.isEmpty()) return EffectResult.success(state)

        val original = context.pipeline.storedCollections[effect.originalCollection]
            ?: return EffectResult.error(state, "No collection named '${effect.originalCollection}' in storedCollections")
        val controllers = context.pipeline.storedCollections[effect.controllerSnapshot]
            ?: return EffectResult.error(state, "No collection named '${effect.controllerSnapshot}' in storedCollections")
        if (controllers.size != original.size) {
            return EffectResult.error(
                state,
                "Controller snapshot '${effect.controllerSnapshot}' (${controllers.size}) is not " +
                    "parallel to originalCollection '${effect.originalCollection}' (${original.size})"
            )
        }

        val originalIndex: Map<EntityId, Int> = original.withIndex().associate { (i, id) -> id to i }
        val tallies = linkedMapOf<EntityId, Int>()
        for (deadId in deadCards) {
            val idx = originalIndex[deadId] ?: continue
            tallies.merge(controllers[idx], 1, Int::plus)
        }
        if (tallies.isEmpty()) return EffectResult.success(state)

        // Sort controllers by turn order, starting from the active player, for deterministic
        // ordering of any state-mutating sub-effects (damage, life loss, etc.).
        val turnOrder = state.turnOrder
        val activeIdx = state.activePlayerId?.let { turnOrder.indexOf(it) }?.takeIf { it >= 0 } ?: 0
        val ordered = (turnOrder.drop(activeIdx) + turnOrder.take(activeIdx))
            .filter { tallies.containsKey(it) }
            .map { it to tallies.getValue(it) }

        return processControllers(state, effect, ordered, context)
    }

    private fun processControllers(
        state: GameState,
        effect: ForEachCapturedControllerEffect,
        controllerTallies: List<Pair<EntityId, Int>>,
        outerContext: EffectContext
    ): EffectResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for ((index, entry) in controllerTallies.withIndex()) {
            val (playerId, count) = entry
            val remaining = controllerTallies.drop(index + 1).map { it.first }

            val perIterationContext = outerContext.copy(
                controllerId = playerId,
                pipeline = outerContext.pipeline.copy(
                    // Preserve outer storedCollections (unlike ForEachPlayerExecutor) so the
                    // sub-effects can still see the dead pile, original list, and snapshot.
                    storedNumbers = outerContext.pipeline.storedNumbers + (effect.countVariable to count)
                )
            )

            val stateForExecution = if (remaining.isNotEmpty()) {
                val continuation = ForEachPlayerContinuation(
                    decisionId = "pending",
                    remainingPlayers = remaining,
                    effects = effect.effects,
                    effectContext = outerContext
                )
                currentState.pushContinuation(continuation)
            } else {
                currentState
            }

            val result = executeSubEffects(stateForExecution, effect.effects, perIterationContext)

            if (result.isPaused) {
                return EffectResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            currentState = if (remaining.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)
        }

        return EffectResult.success(currentState, allEvents)
    }

    private fun executeSubEffects(
        state: GameState,
        effects: List<Effect>,
        context: EffectContext
    ): EffectResult {
        var currentState = state
        var currentContext = context
        val allEvents = mutableListOf<GameEvent>()

        for ((index, subEffect) in effects.withIndex()) {
            val remainingEffects = effects.drop(index + 1)

            val stateForExecution = if (remainingEffects.isNotEmpty()) {
                val continuation = EffectContinuation(
                    decisionId = "pending",
                    remainingEffects = remainingEffects,
                    effectContext = currentContext
                )
                currentState.pushContinuation(continuation)
            } else {
                currentState
            }

            val result = effectExecutor(stateForExecution, subEffect, currentContext)

            if (!result.isSuccess && !result.isPaused) {
                currentState = if (remainingEffects.isNotEmpty()) {
                    val (_, stateWithoutCont) = result.state.popContinuation()
                    stateWithoutCont
                } else {
                    result.state
                }
                allEvents.addAll(result.events)
                continue
            }

            if (result.isPaused) {
                return EffectResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            currentState = if (remainingEffects.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)

            if (result.updatedCollections.isNotEmpty() || result.updatedSubtypeGroups.isNotEmpty()) {
                currentContext = currentContext.copy(
                    pipeline = currentContext.pipeline.copy(
                        storedCollections = currentContext.pipeline.storedCollections + result.updatedCollections,
                        storedSubtypeGroups = currentContext.pipeline.storedSubtypeGroups + result.updatedSubtypeGroups
                    )
                )
            }
        }

        return EffectResult.success(currentState, allEvents)
    }
}
