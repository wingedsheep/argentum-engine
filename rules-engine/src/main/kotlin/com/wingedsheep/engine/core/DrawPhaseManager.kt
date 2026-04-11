package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardPrimitive
import com.wingedsheep.engine.handlers.effects.drawing.DrawLoop
import com.wingedsheep.engine.handlers.effects.drawing.DrawReplacementDispatcher
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Handles draw step execution.
 *
 * The bulk of "what happens when a player draws" lives in the shared
 * drawing primitives under `handlers/effects/drawing/`:
 *  - [DrawCardPrimitive] — physical single-card move library→hand, empty-library
 *    loss, `CardsDrawnThisTurnComponent`, reveal-first-draw
 *  - [DrawReplacementDispatcher] — shield consumer, static draw replacement,
 *    prompt-on-draw
 *  - [DrawLoop] — the actual for-loop over draws
 *
 * This manager's job is to (a) apply the first-turn-first-player draw-skip
 * rule, (b) ask the prompt-on-draw question once up-front, and (c) invoke the
 * shared loop. Spell/ability draws via [com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor]
 * go through exactly the same primitives.
 */
class DrawPhaseManager(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry,
    @Suppress("unused") private val decisionHandler: DecisionHandler,
    effectExecutor: ((GameState, Effect, EffectContext) -> ExecutionResult)?
) {

    private val primitive = DrawCardPrimitive(cardRegistry)
    private val dispatcher = DrawReplacementDispatcher(cardRegistry, effectExecutor)

    /**
     * Perform the draw step (active player draws a card).
     * Skips the draw on the first turn for the first player (standard rule).
     */
    fun performDrawStep(state: GameState): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        val isFirstTurnFirstPlayer = state.turnNumber == 1 && activePlayer == state.turnOrder.first()
        if (isFirstTurnFirstPlayer) {
            return ExecutionResult.success(
                state.withPriority(activePlayer),
                listOf(StepChangedEvent(Step.DRAW))
            )
        }

        // Ask "prompt on draw" abilities (e.g., Words of Wind) once up-front.
        // The draw loop runs with skipPromptOnDraw=true to avoid asking again.
        val promptResult = checkPromptOnDraw(state, activePlayer, 1, isDrawStep = true)
        if (promptResult != null) {
            return promptResult
        }

        val drawResult = drawCards(state, activePlayer, 1)
        if (!drawResult.isSuccess) {
            return drawResult
        }

        val newState = drawResult.newState.withPriority(activePlayer)
        return ExecutionResult.success(newState, drawResult.events + StepChangedEvent(Step.DRAW))
    }

    /**
     * Draw [count] cards for [playerId] as part of the draw step (or the draw
     * following a replacement activation).
     *
     * The draw-step path runs the dispatcher with `skipPromptOnDraw = true`
     * because [performDrawStep] asks that question once up-front; the shared
     * loop does still check shield consumers and static draw replacements
     * (unless [skipPrompts] is set, in which case static is also skipped).
     */
    fun drawCards(state: GameState, playerId: EntityId, count: Int, skipPrompts: Boolean = false): ExecutionResult {
        return DrawLoop.run(
            state = state,
            playerId = playerId,
            count = count,
            primitive = primitive,
            dispatcher = dispatcher,
            isDrawStep = true,
            skipStaticReplacement = skipPrompts,
            skipPromptOnDraw = true,
            emptyLibraryReason = "Library is empty"
        )
    }

    /**
     * Ask the prompt-on-draw question for [playerId]. Exposed so the draw-step
     * continuation resumer can re-check for other unprompted abilities after
     * a player declines one.
     */
    internal fun checkPromptOnDraw(
        state: GameState,
        playerId: EntityId,
        drawCount: Int,
        isDrawStep: Boolean,
        declinedSourceIds: List<EntityId> = emptyList()
    ): ExecutionResult? = dispatcher.checkPromptOnDraw(
        state = state,
        playerId = playerId,
        drawCount = drawCount,
        drawnCardsSoFar = emptyList(),
        isDrawStep = isDrawStep,
        declinedSourceIds = declinedSourceIds
    )
}
