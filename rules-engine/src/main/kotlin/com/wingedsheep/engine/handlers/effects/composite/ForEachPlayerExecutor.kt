package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Effect
import com.wingedsheep.sdk.scripting.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.Player
import kotlin.reflect.KClass

/**
 * Executor for ForEachPlayerEffect.
 *
 * Iterates over each player matching the player selector, executing the sub-effects
 * pipeline for each player individually. Each iteration gets a fresh context with
 * `controllerId` set to the current player (so `Player.You` resolves correctly)
 * and cleared storedCollections.
 *
 * Uses the same pre-push/pop continuation pattern as ForEachTargetExecutor.
 */
class ForEachPlayerExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) : EffectExecutor<ForEachPlayerEffect> {

    override val effectType: KClass<ForEachPlayerEffect> = ForEachPlayerEffect::class

    override fun execute(
        state: GameState,
        effect: ForEachPlayerEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerIds = resolvePlayers(effect.players, state, context)
        if (playerIds.isEmpty()) {
            return ExecutionResult.success(state)
        }

        return processPlayers(state, effect.effects, playerIds, context)
    }

    /**
     * Process players starting from the first one in the list.
     * Called both from the executor and from the continuation handler.
     */
    fun processPlayers(
        state: GameState,
        effects: List<Effect>,
        players: List<EntityId>,
        outerContext: EffectContext
    ): ExecutionResult {
        var currentState = state
        val allEvents = mutableListOf<GameEvent>()

        for ((index, playerId) in players.withIndex()) {
            val remainingPlayers = players.drop(index + 1)

            // Create a per-player context with controllerId set to the current player
            // and fresh storedCollections
            val perPlayerContext = outerContext.copy(
                controllerId = playerId,
                storedCollections = emptyMap()
            )

            // Pre-push a ForEachPlayerContinuation for remaining players
            val stateForExecution = if (remainingPlayers.isNotEmpty()) {
                val continuation = ForEachPlayerContinuation(
                    decisionId = "pending",
                    remainingPlayers = remainingPlayers,
                    effects = effects,
                    sourceId = outerContext.sourceId,
                    controllerId = outerContext.controllerId,
                    opponentId = outerContext.opponentId,
                    xValue = outerContext.xValue
                )
                currentState.pushContinuation(continuation)
            } else {
                currentState
            }

            // Execute the sub-effects pipeline for this player
            val result = executeSubEffects(stateForExecution, effects, perPlayerContext)

            if (result.isPaused) {
                // Sub-effect needs a decision. ForEachPlayerContinuation is underneath.
                return ExecutionResult.paused(
                    result.state,
                    result.pendingDecision!!,
                    allEvents + result.events
                )
            }

            // Pop the pre-pushed continuation (it wasn't needed)
            currentState = if (remainingPlayers.isNotEmpty()) {
                val (_, stateWithoutCont) = result.state.popContinuation()
                stateWithoutCont
            } else {
                result.state
            }
            allEvents.addAll(result.events)
        }

        return ExecutionResult.success(currentState, allEvents)
    }

    /**
     * Execute a list of sub-effects in sequence for a single player.
     * Same pattern as CompositeEffectExecutor / ForEachTargetExecutor.
     */
    private fun executeSubEffects(
        state: GameState,
        effects: List<Effect>,
        context: EffectContext
    ): ExecutionResult {
        var currentState = state
        var currentContext = context
        val allEvents = mutableListOf<GameEvent>()

        for ((index, subEffect) in effects.withIndex()) {
            val remainingEffects = effects.drop(index + 1)

            // Pre-push EffectContinuation for remaining sub-effects
            val stateForExecution = if (remainingEffects.isNotEmpty()) {
                val continuation = EffectContinuation(
                    decisionId = "pending",
                    remainingEffects = remainingEffects,
                    sourceId = currentContext.sourceId,
                    controllerId = currentContext.controllerId,
                    opponentId = currentContext.opponentId,
                    xValue = currentContext.xValue,
                    targets = currentContext.targets,
                    storedCollections = currentContext.storedCollections
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
                return ExecutionResult.paused(
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

            if (result.updatedCollections.isNotEmpty()) {
                currentContext = currentContext.copy(
                    storedCollections = currentContext.storedCollections + result.updatedCollections
                )
            }
        }

        return ExecutionResult.success(currentState, allEvents)
    }

    private fun resolvePlayers(player: Player, state: GameState, context: EffectContext): List<EntityId> {
        return when (player) {
            Player.Each -> state.turnOrder
            Player.ActivePlayerFirst -> {
                val activePlayer = state.activePlayerId ?: return state.turnOrder
                listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }
            }
            Player.You -> listOf(context.controllerId)
            Player.Opponent, Player.TargetOpponent, Player.EachOpponent -> {
                state.turnOrder.filter { it != context.controllerId }
            }
            else -> state.turnOrder
        }
    }
}
