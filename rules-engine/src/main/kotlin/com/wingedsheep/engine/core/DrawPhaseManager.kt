package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardsExecutor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.replacement.ReplacementEffectProcessor
import com.wingedsheep.engine.handlers.effects.drawing.DrawCardPrimitive
import com.wingedsheep.engine.handlers.effects.drawing.DrawReplacementDispatcher
import com.wingedsheep.engine.handlers.effects.drawing.DrawLoop
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CardsDrawnThisTurnComponent
import com.wingedsheep.engine.state.components.player.PlayerLostComponent
import com.wingedsheep.engine.state.components.player.SkipDrawStepComponent
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.TurnPart
import com.wingedsheep.sdk.scripting.SkipStepOrPhase
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Handles draw step execution.
 *
 * Delegates all draw mechanics to [DrawCardsExecutor], which shares the same
 * primitives ([DrawCardPrimitive], [DrawReplacementDispatcher], [DrawLoop])
 * with spell/ability draws.
 *
 * This manager's only job is the draw-step setup: first-turn skip, teammate
 * draws, skip-draw-step markers, and the draw-count announcement.
 */
class DrawPhaseManager(
    private val cardRegistry: CardRegistry,
    @Suppress("unused") private val decisionHandler: DecisionHandler,
    effectExecutor: ((GameState, Effect, EffectContext) -> EffectResult)?,
    replacementProcessor: ReplacementEffectProcessor,
    private val predicateEvaluator: PredicateEvaluator,
    private val amountEvaluator: DynamicAmountEvaluator = predicateEvaluator.amounts
) {
    private val drawExecutor = DrawCardsExecutor(
        cardRegistry = cardRegistry,
        effectExecutor = effectExecutor,
        replacementProcessor = replacementProcessor,
        amountEvaluator = amountEvaluator
    )

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
                ?.has<PlayerLostComponent>() == true
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
            ?.get<CardsDrawnThisTurnComponent>()?.count ?: 0
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
        // teammates first (a team draws in any order it likes, 805.6a), then the active player last.
        // In a non-team game — and in Team vs. Team, where each player draws only on their own turn
        // (CR 808.4) — there are no shared-turn teammates and this loop is a no-op.
        var s = state
        val teammateEvents = mutableListOf<GameEvent>()
        for (teammate in state.sharedTurnTeam(activePlayer)) {
            if (teammate == activePlayer) continue
            if (s.getEntity(teammate)?.has<SkipDrawStepComponent>() == true) {
                s = s.updateEntity(teammate) { it.without<SkipDrawStepComponent>() }
                continue
            }
            if (skipsDrawStep(s, teammate)) continue
            val r = drawCards(s, teammate, 1)
            if (r.outcome is Outcome.Paused) {
                return ExecutionResult.propagatePause(r.newState, teammateEvents + r.events)
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

        // A standing "Skip your draw step" static (Colfenor's Plans) — unlike the marker above it
        // is not consumed, so it applies every turn for as long as the permanent is around.
        if (skipsDrawStep(s, activePlayer)) {
            return ExecutionResult.success(
                s.withPriority(activePlayer),
                teammateEvents + StepChangedEvent(Step.DRAW)
            )
        }

        val drawResult = drawCards(s, activePlayer, 1)
        if (drawResult.outcome !is Outcome.Done) {
            return drawResult
        }

        val newState = drawResult.newState.withPriority(activePlayer)
        return ExecutionResult.success(newState, teammateEvents + drawResult.events + StepChangedEvent(Step.DRAW))
    }

    /**
     * Whether a standing [SkipStepOrPhase] for the draw step applies to [playerId] ("Skip your
     * draw step" — Colfenor's Plans). A turn-based scan rather than a continuous-projection value,
     * because a skipped draw step is a turn-based action, not a characteristic; the same reader as
     * the untap step's ([hasStandingSkip]), so a Room face's static counts only while its door is
     * unlocked (CR 709.5) and a permanent that lost its abilities no longer skips anything.
     */
    private fun skipsDrawStep(state: GameState, playerId: EntityId): Boolean =
        hasStandingSkip(state, cardRegistry, predicateEvaluator, playerId, TurnPart.DRAW_STEP)

    /**
     * Draw [count] cards for [playerId] as part of the draw step.
     *
     * Delegates to [DrawCardsExecutor.executeDraws] so the draw-step path and
     * the spell/ability path share the same primitives.
     *
     * @param announce `false` when [count] is the tail of a draw that was already
     *     announced and then paused — see [DrawCardsExecutor.executeDraws].
     */
    fun drawCards(
        state: GameState,
        playerId: EntityId,
        count: Int,
        announce: Boolean = true
    ): ExecutionResult {
        return drawExecutor.executeDraws(
            state = state,
            playerId = playerId,
            count = count,
            isDrawStep = true,
            emptyLibraryReason = "Library is empty",
            announce = announce
        ).toExecutionResult()
    }
}
