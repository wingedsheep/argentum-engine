package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardPrimitive
import com.wingedsheep.engine.handlers.effects.drawing.DrawLoop
import com.wingedsheep.engine.handlers.effects.drawing.DrawReplacementDispatcher
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.SkipDrawStepComponent
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
    effectExecutor: ((GameState, Effect, EffectContext) -> EffectResult)?
) {

    private val primitive = DrawCardPrimitive(cardRegistry)
    private val dispatcher = DrawReplacementDispatcher(cardRegistry, effectExecutor)

    /**
     * Perform the draw step (active player draws a card).
     * Skips the draw on the first turn for the first player (standard rule).
     */
    fun performDrawStep(stateIn: GameState): ExecutionResult {
        val activePlayer = stateIn.activePlayerId
            ?: return ExecutionResult.error(stateIn, "No active player")

        // CR 800.4j: a turn whose active player has left the game continues without an
        // active player — there is no one to take the draw turn-based action, so it doesn't
        // happen. (Their library is gone too, so attempting it would be a spurious deck-out.)
        if (stateIn.getEntity(activePlayer)
                ?.has<com.wingedsheep.engine.state.components.player.PlayerLostComponent>() == true
        ) {
            return ExecutionResult.success(
                stateIn.withPriority(activePlayer),
                listOf(StepChangedEvent(Step.DRAW))
            )
        }

        // Snapshot the active player's cards-drawn-this-turn count as the draw step begins,
        // before the turn-based draw (CR 504.1). The first card drawn from here on is "the first
        // card they draw in this draw step" — the one exempted by Orcish Bowmasters. Captured for
        // every draw-step entry (including the skip/first-turn paths below) so a later in-step draw
        // is still identified correctly. See GameState.drawStepStartDrawCountByPlayer.
        val drawnSoFar = stateIn.getEntity(activePlayer)
            ?.get<com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent>()?.count ?: 0
        val state = stateIn.copy(
            drawStepStartDrawCountByPlayer = stateIn.drawStepStartDrawCountByPlayer + (activePlayer to drawnSoFar)
        )

        // CR 103.8a / 810.6: the player (or team) that goes first skips the draw step of its first
        // turn; CR 103.8c — no one skips in a game that began with 3+ players. The skip therefore
        // applies only to a genuine heads-up game: exactly two players (a 1v1 — each its own
        // singleton team), or a 2-team Two-Headed Giant pod where the two teams alternate shared
        // turns. A Team vs. Team pod (4/6/8 players taking individual turns) is a 3+-player game, so
        // no one skips — which is why this is gated on sharesTeamTurns, not just `teams.size == 2`.
        val isHeadsUp = state.activePlayers.size == 2 ||
            (state.format.sharesTeamTurns && state.teams.size == 2)
        val isFirstTurnFirstTeam = state.turnNumber == 1 &&
            activePlayer == state.turnOrder.first() &&
            isHeadsUp
        if (isFirstTurnFirstTeam) {
            return ExecutionResult.success(
                state.withPriority(activePlayer),
                listOf(StepChangedEvent(Step.DRAW))
            )
        }

        // CR 805.4b — each player on the active team draws during the team's draw step. Draw the
        // teammates first (a team draws in any order it likes, 805.6a), then the active player last
        // so its prompt-on-draw pause stays the final action of the step. Deck-out marks the loss in
        // the returned state, so accumulating each draw's state/events is enough. (Prompt-on-draw for
        // a teammate's own draw is a rare deferred nuance — teammate draws skip that prompt.) In a
        // non-team game — and in Team vs. Team, where each player draws only on their own turn
        // (CR 808.4) — there are no shared-turn teammates and this loop is a no-op.
        var s = state
        val teammateEvents = mutableListOf<GameEvent>()
        for (teammate in state.sharedTurnTeam(activePlayer)) {
            if (teammate == activePlayer) continue
            if (s.getEntity(teammate)?.has<SkipDrawStepComponent>() == true) {
                s = s.updateEntity(teammate) { it.without<SkipDrawStepComponent>() }
                continue
            }
            val teammateDrawCount = dispatcher.applyDrawAmountModifier(s, teammate, 1)
            if (teammateDrawCount <= 0) continue
            val r = drawCards(s, teammate, teammateDrawCount)
            if (r.isPaused) {
                return ExecutionResult.paused(r.newState, r.pendingDecision!!, teammateEvents + r.events)
            }
            s = r.newState
            teammateEvents.addAll(r.events)
        }

        // Skip the active player's draw if a "skip your next draw step" marker is present (e.g.,
        // Elfhame Sanctuary). Consume the marker so it only skips one draw step.
        if (s.getEntity(activePlayer)?.has<SkipDrawStepComponent>() == true) {
            val consumed = s.updateEntity(activePlayer) { it.without<SkipDrawStepComponent>() }
            return ExecutionResult.success(
                consumed.withPriority(activePlayer),
                teammateEvents + StepChangedEvent(Step.DRAW)
            )
        }

        // Apply ModifyDrawAmount replacements once at the draw-step's announcement
        // site (CR 121.2a) — e.g., Quantum Riddler's "draw that many cards plus one
        // instead" turns this step's draw of 1 into 2 while its restriction holds.
        val drawCount = dispatcher.applyDrawAmountModifier(s, activePlayer, 1)
        if (drawCount <= 0) {
            return ExecutionResult.success(
                s.withPriority(activePlayer),
                teammateEvents + StepChangedEvent(Step.DRAW)
            )
        }

        // Ask "prompt on draw" abilities (e.g., Words of Wind) once up-front.
        // The draw loop runs with skipPromptOnDraw=true to avoid asking again.
        val promptResult = checkPromptOnDraw(s, activePlayer, drawCount, isDrawStep = true)
        if (promptResult != null) {
            return if (teammateEvents.isEmpty()) promptResult
            else ExecutionResult.paused(promptResult.newState, promptResult.pendingDecision!!, teammateEvents + promptResult.events)
        }

        val drawResult = drawCards(s, activePlayer, drawCount)
        if (!drawResult.isSuccess) {
            return drawResult
        }

        val newState = drawResult.newState.withPriority(activePlayer)
        return ExecutionResult.success(newState, teammateEvents + drawResult.events + StepChangedEvent(Step.DRAW))
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
        ).toExecutionResult()
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
    )?.toExecutionResult()
}
