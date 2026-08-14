package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
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
     *     checks whatsoever, used by lightweight call sites that construct a
     *     bare [DrawCardsExecutor] without an effect executor)
     * @param isDrawStep `true` when this is the active player's draw-step
     *     draw, `false` for spell/ability draws
     * @param emptyLibraryReason message on [DrawFailedEvent]
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
        emptyLibraryReason: String = "Empty library",
        context: EffectContext? = null
    ): EffectResult {
        var newState = state
        val drawnCards = mutableListOf<EntityId>()
        val perCardEvents = mutableListOf<GameEvent>()

        // CR 614.5 is scoped to one event and the events that replace it, and each iteration
        // below is a separate draw event — so every iteration starts from the chain this
        // instruction inherited, not from whatever a previous iteration's replacement left
        // behind. It matters in both directions: an announcement-level effect (or the effect
        // that spawned this nested instruction) must stay excluded for every card, while an
        // effect consumed replacing card 1 must be eligible again for card 2.
        val inheritedChain = state.activeReplacementChain

        var remaining = count
        while (remaining > 0) {
            newState = newState.copy(activeReplacementChain = inheritedChain)

            // 1. Check replacements. This runs *before* the primitive draw and before any
            //    empty-library check, and CR 614.11 requires exactly that ordering: effects
            //    that replace a card draw "are applied even if no cards could be drawn because
            //    there are no cards in the affected player's library". Hoisting an empty-library
            //    short-circuit above this would silently break every draw-replacement shield.
            if (dispatcher != null) {
                val dispatch = dispatcher.checkBeforeDraw(
                    state = newState,
                    playerId = playerId,
                    drawsLeftIncludingThis = remaining,
                    drawnCardsSoFar = drawnCards.toList(),
                    isDrawStep = isDrawStep,
                    context = context
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
                        remaining--
                        continue
                    }
                    is DrawReplacementDispatcher.DispatchResult.Modified -> {
                        // Only the announcement check (CR 121.2a) can modify a draw count,
                        // and it runs once, before this loop. Adjusting `remaining` here
                        // instead would not terminate: no card is drawn and nothing about
                        // the game state changes, so the same effect matches again on the
                        // next iteration and `remaining` only ever grows.
                        error("checkBeforeDraw must not modify the draw count")
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
            remaining--
        }

        return buildSuccessResult(newState, playerId, drawnCards, perCardEvents)
    }

    /**
     * Build a success result with a prepended [CardsDrawnEvent] aggregating
     * every card drawn in this loop invocation. Matches the historical
     * [DrawCardsExecutor] ordering, where the aggregate event comes first
     * and per-card side events (e.g., [CardRevealedFromDrawEvent])
     * come after.
     */
    private fun buildSuccessResult(
        state: GameState,
        playerId: EntityId,
        drawnCards: List<EntityId>,
        perCardEvents: List<GameEvent>
    ): EffectResult {
        val events = mutableListOf<GameEvent>()
        var newState = state.copy(activeReplacementChain = null)
        if (drawnCards.isNotEmpty()) {
            val cardNames = drawnCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            events.add(CardsDrawnEvent(playerId, drawnCards.size, drawnCards, cardNames))
            newState = newState.copy(
                lastCardDrawnThisTurnByPlayer = newState.lastCardDrawnThisTurnByPlayer + (playerId to drawnCards.last())
            )
        }
        events.addAll(perCardEvents)
        return EffectResult.success(newState, events)
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
        pauseResult: EffectResult
    ): EffectResult {
        val allEvents = mutableListOf<GameEvent>()
        var pausedState = pauseResult.state.copy(activeReplacementChain = null)
        if (drawnCards.isNotEmpty()) {
            val cardNames = drawnCards.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            allEvents.add(CardsDrawnEvent(playerId, drawnCards.size, drawnCards.toList(), cardNames))
            pausedState = pausedState.copy(
                lastCardDrawnThisTurnByPlayer = pausedState.lastCardDrawnThisTurnByPlayer + (playerId to drawnCards.last())
            )
        }
        allEvents.addAll(perCardEvents)
        allEvents.addAll(pauseResult.events)
        return EffectResult.paused(
            pausedState,
            pauseResult.pendingDecision!!,
            allEvents
        )
    }
}
