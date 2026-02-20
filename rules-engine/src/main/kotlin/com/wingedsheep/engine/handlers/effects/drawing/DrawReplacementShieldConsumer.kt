package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.DrawReplacementRemainingDrawsContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Consumes a unified draw-replacement shield ([SerializableModification.ReplaceDrawWithEffect])
 * and executes the stored replacement effect via the effect pipeline.
 *
 * Used by both [DrawCardsExecutor] (spell/ability draws) and [TurnManager][com.wingedsheep.engine.core.TurnManager]
 * (draw step draws) to replace individual card draws with alternative effects.
 */
class DrawReplacementShieldConsumer(
    private val effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult
) {
    /**
     * Result of consuming a shield.
     */
    sealed interface ConsumeResult {
        /** The replacement effect completed synchronously. */
        data class Synchronous(val state: GameState, val events: List<GameEvent>) : ConsumeResult

        /** The replacement effect paused for a decision (e.g., bounce selection, discard choice). */
        data class Paused(val result: ExecutionResult) : ConsumeResult
    }

    /**
     * Attempt to consume a [SerializableModification.ReplaceDrawWithEffect] shield for [playerId].
     *
     * @param state The current game state
     * @param playerId The player about to draw
     * @param remainingDraws Number of draws left *after* this one (0 if this is the last)
     * @param drawnCardsSoFar Cards already drawn before this point (for CardsDrawnEvent)
     * @param eventsSoFar Events accumulated before this point
     * @param isDrawStep Whether this is from the draw step (vs spell/ability draws)
     * @return A [ConsumeResult] if a shield was consumed, or `null` if no shield exists
     */
    fun consumeShield(
        state: GameState,
        playerId: EntityId,
        remainingDraws: Int,
        drawnCardsSoFar: List<EntityId>,
        eventsSoFar: List<GameEvent>,
        isDrawStep: Boolean
    ): ConsumeResult? {
        val shieldIndex = state.floatingEffects.indexOfFirst { effect ->
            effect.effect.modification is SerializableModification.ReplaceDrawWithEffect &&
                playerId in effect.effect.affectedEntities
        }
        if (shieldIndex == -1) return null

        val shield = state.floatingEffects[shieldIndex]
        val mod = shield.effect.modification as SerializableModification.ReplaceDrawWithEffect

        // Remove the consumed shield
        val updatedEffects = state.floatingEffects.toMutableList()
        updatedEffects.removeAt(shieldIndex)
        var newState = state.copy(floatingEffects = updatedEffects)

        // If there are remaining draws, push a continuation so they resume after the pipeline
        if (remainingDraws > 0) {
            val remainingDrawsContinuation = DrawReplacementRemainingDrawsContinuation(
                drawingPlayerId = playerId,
                remainingDraws = remainingDraws,
                isDrawStep = isDrawStep
            )
            newState = newState.pushContinuation(remainingDrawsContinuation)
        }

        // Build context from the stored shield data
        val context = EffectContext(
            controllerId = playerId,
            sourceId = mod.sourceId,
            opponentId = newState.turnOrder.firstOrNull { it != playerId },
            targets = mod.targets
        )

        // Execute the stored replacement effect via the pipeline
        val pipelineResult = effectExecutor(newState, mod.replacementEffect, context)

        if (pipelineResult.isPaused) {
            // Pipeline needs a decision — remaining-draws continuation is underneath
            return ConsumeResult.Paused(pipelineResult)
        }

        // Pipeline completed synchronously
        var resultState = pipelineResult.state
        val resultEvents = pipelineResult.events

        // Pop the remaining-draws continuation if we pushed one (pipeline didn't use it)
        if (remainingDraws > 0) {
            val (popped, stateAfterPop) = resultState.popContinuation()
            if (popped is DrawReplacementRemainingDrawsContinuation) {
                resultState = stateAfterPop
            }
            // If it's something else, leave the state as-is (shouldn't happen)
        }

        return ConsumeResult.Synchronous(resultState, resultEvents)
    }
}
