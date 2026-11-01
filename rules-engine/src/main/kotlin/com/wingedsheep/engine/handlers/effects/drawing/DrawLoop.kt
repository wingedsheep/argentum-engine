package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.DrawnThisTurnComponent
import com.wingedsheep.sdk.core.Zone
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
        context: EffectContext? = null,
        /** The card whose cycling ability causes these draws (`EventPattern.DrawEvent.cyclingOf`). */
        cycledCardId: EntityId? = null
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

            // 0. A per-turn draw cap (CR 121.2b, Narset) applies to each individual draw: the
            //    instruction is carried out as far as it can be and the rest are not drawn. A
            //    draw the player can't make isn't an event, so it is checked before replacements
            //    (Narset's ruling: they can't replace a disallowed draw) and before the empty-
            //    library check (nothing was attempted). Nothing below can lift the cap mid-loop,
            //    so the remaining draws all stop here.
            if (primitive.drawProhibited(newState, playerId)) break

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
                    context = context,
                    cycledCardId = cycledCardId
                )
                when (dispatch) {
                    is DrawReplacementDispatcher.DispatchResult.Paused -> {
                        return buildPausedResult(
                            newState, playerId, drawnCards, perCardEvents, dispatch.result, cycledCardId
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
                return buildSuccessResult(newState, playerId, drawnCards, perCardEvents, cycledCardId)
            }

            drawnCards.add(drawOneResult.drawnCardId!!)
            remaining--

            // 3. "As you draw it" questions about the first card of the turn — optional reveals
            //    (God-Eternal Kefnet), then miracle (CR 702.94a: "you may reveal this card from your
            //    hand as you draw it") — asked now, before the next card is drawn, with the rest of
            //    the instruction waiting beneath them.
            val miracleCost = drawOneResult.miracle?.cost
            val revealSources = drawOneResult.optionalRevealSources
            if (miracleCost != null || revealSources.isNotEmpty()) {
                val withTail = newState.copy(activeReplacementChain = null).pushContinuation(
                    DrawReplacementRemainingDrawsContinuation(
                        drawingPlayerId = playerId,
                        remainingDraws = remaining,
                        isDrawStep = isDrawStep,
                        announcementApplied = true,
                        cycledCardId = cycledCardId
                    )
                )
                val pause = EffectResult.from(
                    askFirstDrawQuestion(withTail, playerId, drawOneResult.drawnCardId, revealSources, miracleCost)!!
                )
                return buildPausedResult(newState, playerId, drawnCards, perCardEvents, pause, cycledCardId)
            }
        }

        return buildSuccessResult(newState, playerId, drawnCards, perCardEvents, cycledCardId)
    }

    /**
     * Ask the next "as you draw it" question about [cardId], the first card [playerId] drew this
     * turn: the first of [revealSources]' optional reveals, else the miracle reveal when
     * [miracleCost] is set. Null when nothing is left to ask. The caller has already pushed the
     * [DrawReplacementRemainingDrawsContinuation] carrying the rest of the instruction — an empty
     * tail too, which in the draw step hands the drawing player priority once the answers are in.
     *
     * The miracle question is a [PrivateQuestion]: it exists only because the card has miracle, so
     * it reaches no other seat. An optional reveal's question is public — its source is on the
     * battlefield — and names the drawn card only in the asked player's prompt.
     */
    fun askFirstDrawQuestion(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        revealSources: List<EntityId>,
        miracleCost: com.wingedsheep.sdk.core.ManaCost?,
        events: List<GameEvent> = emptyList()
    ): ExecutionResult? {
        val cardName = state.getEntity(cardId)?.get<CardComponent>()?.name ?: "this card"
        val sourceId = revealSources.firstOrNull()
        if (sourceId != null) {
            val sourceName = state.getEntity(sourceId)?.get<CardComponent>()?.name ?: "this permanent"
            return state.suspendForDecision(
                question = { id ->
                    YesNoDecision(
                        id = id,
                        playerId = playerId,
                        prompt = "Reveal $cardName with $sourceName?",
                        context = DecisionContext(
                            sourceId = sourceId,
                            sourceName = sourceName,
                            phase = DecisionPhase.RESOLUTION
                        ),
                        yesText = "Reveal",
                        noText = "Keep hidden",
                        hint = "$sourceName lets you reveal the first card you draw each turn as you draw it."
                    )
                },
                answer = OptionalFirstDrawRevealContinuation(
                    playerId, cardId, sourceId, revealSources.drop(1), miracleCost
                ),
                events = events
            )
        }
        if (miracleCost == null) return null
        return state.suspendForDecision(
            question = { id ->
                YesNoDecision(
                    id = id,
                    playerId = playerId,
                    prompt = "Reveal $cardName for its miracle?",
                    context = DecisionContext(
                        sourceId = cardId,
                        sourceName = cardName,
                        phase = DecisionPhase.RESOLUTION
                    ),
                    yesText = "Reveal",
                    noText = "Keep hidden",
                    hint = "Miracle $miracleCost — reveal it as the first card you drew this turn, and a " +
                        "trigger lets you cast it for $miracleCost as it resolves."
                )
            },
            answer = MiracleRevealContinuation(playerId, cardId, miracleCost),
            events = events
        )
    }

    /**
     * Pin each drawn card's [DrawnThisTurnComponent] to the object it is now in its hand
     * (`StatePredicate.DrawnThisTurn`). A card no longer in a hand by now — moved on by a pause's
     * own effects — is skipped, since it already stopped being the drawn object.
     */
    private fun stampDrawnThisTurn(state: GameState, drawnCards: List<EntityId>): GameState =
        drawnCards.fold(state) { acc, cardId ->
            val ref = acc.objectRef(cardId)
            if (ref == null || acc.logicalZone(cardId)?.zoneType != Zone.HAND) acc
            else acc.updateEntity(cardId) { it.with(DrawnThisTurnComponent(ref.generation)) }
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
        perCardEvents: List<GameEvent>,
        cycledCardId: EntityId?
    ): EffectResult {
        val events = mutableListOf<GameEvent>()
        var newState = state.copy(activeReplacementChain = null)
        if (drawnCards.isNotEmpty()) {
            val cardNames = drawnCards.map { newState.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            events.add(CardsDrawnEvent(playerId, drawnCards.size, drawnCards, cardNames, setOf(state.actorFor(playerId)) - playerId, cycledCardId))
            newState = stampDrawnThisTurn(newState.copy(
                lastCardDrawnThisTurnByPlayer = newState.lastCardDrawnThisTurnByPlayer + (playerId to drawnCards.last())
            ), drawnCards)
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
        pauseResult: EffectResult,
        cycledCardId: EntityId?
    ): EffectResult {
        val allEvents = mutableListOf<GameEvent>()
        var pausedState = pauseResult.state.copy(activeReplacementChain = null)
        if (drawnCards.isNotEmpty()) {
            val cardNames = drawnCards.map { state.getEntity(it)?.get<CardComponent>()?.name ?: "Card" }
            allEvents.add(CardsDrawnEvent(playerId, drawnCards.size, drawnCards.toList(), cardNames, setOf(state.actorFor(playerId)) - playerId, cycledCardId))
            pausedState = stampDrawnThisTurn(pausedState.copy(
                lastCardDrawnThisTurnByPlayer = pausedState.lastCardDrawnThisTurnByPlayer + (playerId to drawnCards.last())
            ), drawnCards)
        }
        allEvents.addAll(perCardEvents)
        allEvents.addAll(pauseResult.events)
        return EffectResult.propagatePause(
            pausedState,
            allEvents
        )
    }
}
