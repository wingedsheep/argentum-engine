package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId

/**
 * Shared loop that both [DrawCardsExecutor] (spell/ability draws) and
 * [com.wingedsheep.engine.core.DrawPhaseManager] (draw-step draws) call into.
 *
 * The loop's single responsibility is to sequence the following per iteration:
 *  1. Ask [DrawReplacementDispatcher.checkBeforeDraw] whether anything
 *     intercepts this draw.
 *  2. If nothing intercepts, call [DrawCardPrimitive.drawOne] to physically
 *     move a card from library to hand.
 *  3. When a pause happens mid-loop, flush a [CardsDrawnEvent] aggregating the
 *     cards already drawn so far, so downstream observers see a consistent
 *     snapshot.
 *
 * The aggregation of drawn cards into a single [CardsDrawnEvent] is the
 * historical contract for draw effects — preserved here so that existing tests
 * and UI code that look for `filterIsInstance<CardsDrawnEvent>()` keep working
 * byte-for-byte.
 */
object DrawLoop {

    /**
     * Run a sequence of [count] draws for [playerId].
     *
     * @param primitive physical single-card draw
     * @param dispatcher replacement-effect dispatcher (null = no replacement
     *     checks whatsoever, used by lightweight call sites like
     *     [com.wingedsheep.engine.handlers.effects.drawing.ReadTheRunesExecutor]
     *     that construct a bare [DrawCardsExecutor] without an effect executor)
     * @param isDrawStep `true` when this is the active player's draw-step
     *     draw, `false` for spell/ability draws
     * @param skipStaticReplacement skip the Parallel Thoughts-style static
     *     replacement check. Historical `skipPrompts = true` sets this when
     *     resuming after a decision already asked the question.
     * @param skipPromptOnDraw skip the prompt-on-draw check. Always `true` for
     *     the draw-step path (it asks once up-front in `performDrawStep`), and
     *     set to `true` by spell/ability resume paths that handled prompts.
     * @param emptyLibraryReason message on [com.wingedsheep.engine.core.DrawFailedEvent]
     *     when the library runs out mid-loop. Draw-step callers pass
     *     `"Library is empty"`; spell/ability callers pass `"Empty library"`.
     */
    fun run(
        state: GameState,
        playerId: EntityId,
        count: Int,
        primitive: DrawCardPrimitive,
        dispatcher: DrawReplacementDispatcher?,
        isDrawStep: Boolean,
        skipStaticReplacement: Boolean = false,
        skipPromptOnDraw: Boolean = false,
        emptyLibraryReason: String = "Empty library"
    ): ExecutionResult {
        var newState = state
        val drawnCards = mutableListOf<EntityId>()
        val perCardEvents = mutableListOf<GameEvent>()

        for (i in 0 until count) {
            val drawsLeftIncludingThis = count - i

            // 1. Check replacements.
            if (dispatcher != null) {
                val dispatch = dispatcher.checkBeforeDraw(
                    state = newState,
                    playerId = playerId,
                    drawsLeftIncludingThis = drawsLeftIncludingThis,
                    drawnCardsSoFar = drawnCards.toList(),
                    isDrawStep = isDrawStep,
                    skipStaticReplacement = skipStaticReplacement,
                    skipPromptOnDraw = skipPromptOnDraw
                )
                when (dispatch) {
                    is DrawReplacementDispatcher.DispatchResult.Paused -> {
                        return buildPausedResult(
                            newState, playerId, drawnCards, perCardEvents, dispatch.result
                        )
                    }
                    is DrawReplacementDispatcher.DispatchResult.Replaced -> {
                        newState = dispatch.state
                        perCardEvents.addAll(dispatch.events)
                        continue
                    }
                    is DrawReplacementDispatcher.DispatchResult.None -> {
                        // fall through to primitive draw
                    }
                }
            }

            // 2. Primitive single-card draw.
            val drawOneResult = primitive.drawOne(newState, playerId, emptyLibraryReason)
            newState = drawOneResult.state
            perCardEvents.addAll(drawOneResult.events)

            if (drawOneResult.failed) {
                // Empty library — flush aggregate event for prior draws and stop here.
                return buildSuccessResult(newState, playerId, drawnCards, perCardEvents)
            }

            drawnCards.add(drawOneResult.drawnCardId!!)
        }

        return buildSuccessResult(newState, playerId, drawnCards, perCardEvents)
    }

    /**
     * Build a success result with a prepended [CardsDrawnEvent] aggregating
     * every card drawn in this loop invocation. Matches the historical
     * [DrawCardsExecutor] ordering, where the aggregate event comes first
     * and per-card side events (e.g., [com.wingedsheep.engine.core.CardRevealedFromDrawEvent])
     * come after.
     */
    private fun buildSuccessResult(
        state: GameState,
        playerId: EntityId,
        drawnCards: List<EntityId>,
        perCardEvents: List<GameEvent>
    ): ExecutionResult {
        val events = mutableListOf<GameEvent>()
        if (drawnCards.isNotEmpty()) {
            val cardNames = drawnCards.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            events.add(CardsDrawnEvent(playerId, drawnCards.size, drawnCards, cardNames))
        }
        events.addAll(perCardEvents)
        return ExecutionResult.success(state, events)
    }

    /**
     * Build a paused result that flushes a [CardsDrawnEvent] for any cards
     * drawn before the pause, then appends the pause's own events. This is
     * what lets a multi-draw effect that pauses mid-loop still surface the
     * partial draw to downstream observers without losing the event.
     */
    private fun buildPausedResult(
        state: GameState,
        playerId: EntityId,
        drawnCards: List<EntityId>,
        perCardEvents: List<GameEvent>,
        pauseResult: ExecutionResult
    ): ExecutionResult {
        val allEvents = mutableListOf<GameEvent>()
        if (drawnCards.isNotEmpty()) {
            val cardNames = drawnCards.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            allEvents.add(CardsDrawnEvent(playerId, drawnCards.size, drawnCards.toList(), cardNames))
        }
        allEvents.addAll(perCardEvents)
        allEvents.addAll(pauseResult.events)
        return ExecutionResult.paused(
            pauseResult.state,
            pauseResult.pendingDecision!!,
            allEvents
        )
    }
}
