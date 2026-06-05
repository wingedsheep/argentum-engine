package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.effects.DrawRevealDiscardUnlessEffect
import kotlin.reflect.KClass

/**
 * Executor for [DrawRevealDiscardUnlessEffect] — "Draw a card and reveal it. If it isn't
 * [filter], discard it." (Sindbad).
 *
 * Draws one card via the shared [DrawCardPrimitive] and emits the canonical `CardsDrawnEvent`
 * (which the primitive leaves to its driver) so "whenever you draw a card" triggers fire. It then
 * reveals the card to the opponent and evaluates it against the effect's filter using base state
 * (the card is in hand, a non-battlefield zone, so no projection is required). If it doesn't
 * match, the card is discarded via [ZoneTransitionService.discardCard] so the canonical
 * `CardsDiscardedEvent` fires (discard triggers, madness, client log) rather than a bare zone
 * change.
 */
class DrawRevealDiscardUnlessExecutor(
    cardRegistry: CardRegistry
) : EffectExecutor<DrawRevealDiscardUnlessEffect> {

    override val effectType: KClass<DrawRevealDiscardUnlessEffect> = DrawRevealDiscardUnlessEffect::class

    private val primitive = DrawCardPrimitive(cardRegistry)
    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: DrawRevealDiscardUnlessEffect,
        context: EffectContext
    ): EffectResult {
        val playerIds = context.resolvePlayerTargets(effect.target, state)
        if (playerIds.isEmpty()) {
            return EffectResult.error(state, "No valid player for draw")
        }

        var currentState = state
        val allEvents = mutableListOf<GameEvent>()
        val sourceName = context.sourceId?.let { currentState.getEntity(it)?.get<CardComponent>()?.name }
        for (playerId in playerIds) {
            val drawResult = primitive.drawOne(currentState, playerId, emptyLibraryReason = "Draw from empty library")
            currentState = drawResult.state

            val drawnCardId = drawResult.drawnCardId
            if (drawnCardId == null) {
                // Empty library — surface the primitive's events (DrawFailedEvent, Rule 704.5c
                // loss) and move on; nothing was drawn to reveal or discard.
                allEvents.addAll(drawResult.events)
                continue
            }

            // Emit the canonical aggregate CardsDrawnEvent (DrawCardPrimitive deliberately leaves
            // this to its driver) so draw triggers — "whenever you draw a card" — fire and the
            // client logs the draw. Ordering mirrors DrawLoop: aggregate event first, then the
            // primitive's per-card side events (e.g. CardRevealedFromDrawEvent).
            // Note: drawing via the primitive directly bypasses DrawLoop's draw-replacement
            // dispatch, so "if you would draw a card, instead ..." effects are not applied here.
            val drawnCard = currentState.getEntity(drawnCardId)?.get<CardComponent>()
            allEvents.add(CardsDrawnEvent(playerId, 1, listOf(drawnCardId), listOf(drawnCard?.name ?: "Card")))
            currentState = currentState.copy(
                lastCardDrawnThisTurnByPlayer = currentState.lastCardDrawnThisTurnByPlayer + (playerId to drawnCardId)
            )
            allEvents.addAll(drawResult.events)

            // "reveal it" — show the drawn card to the opponent whether it's kept or discarded.
            // revealToSelf = false: the drawing player already saw the card they drew.
            allEvents.add(
                CardsRevealedEvent(
                    revealingPlayerId = playerId,
                    cardIds = listOf(drawnCardId),
                    cardNames = listOf(drawnCard?.name ?: "Unknown"),
                    imageUris = listOf(drawnCard?.imageUri),
                    source = sourceName,
                    revealToSelf = false,
                )
            )

            val matches = predicateEvaluator.matches(
                currentState,
                currentState.projectedState,
                drawnCardId,
                effect.filter,
                PredicateContext.fromEffectContext(context),
            )
            if (!matches) {
                val discardResult = ZoneTransitionService.discardCard(currentState, playerId, drawnCardId)
                currentState = discardResult.state
                allEvents.addAll(discardResult.events)
            }
        }
        return EffectResult.success(currentState, allEvents)
    }
}
