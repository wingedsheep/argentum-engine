package com.wingedsheep.engine.handlers.effects.composite

import com.wingedsheep.engine.core.CardsRevealedEvent
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.MayRevealCardFromHandContinuation
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MayRevealCardFromHandEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for [MayRevealCardFromHandEffect] — the atomic "you may reveal a
 * [filter] card from your hand" choice.
 *
 * Flow:
 *  1. Compute eligible cards in the controller's hand matching `effect.filter`.
 *  2. If none, skip the prompt entirely and run `effect.otherwise`.
 *  3. Otherwise, present a `SelectCardsDecision` with `minSelections = 0,
 *     maxSelections = 1` listing the eligible cards. The player either picks
 *     one (reveals it) or submits an empty selection (declines).
 *  4. On reveal: emit `CardsRevealedEvent`; the inner choice has no further
 *     payoff.
 *  5. On decline: run `effect.otherwise`.
 *
 * @param effectExecutor sub-effect runner provided by the registry, used to
 *   chain into `effect.otherwise` (which itself may pause).
 */
class MayRevealCardFromHandEffectExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult,
    private val predicateEvaluator: PredicateEvaluator = PredicateEvaluator(),
) : EffectExecutor<MayRevealCardFromHandEffect> {

    override val effectType: KClass<MayRevealCardFromHandEffect> = MayRevealCardFromHandEffect::class

    override fun execute(
        state: GameState,
        effect: MayRevealCardFromHandEffect,
        context: EffectContext,
    ): EffectResult {
        val revealer = context.controllerId
        val handCards = state.getHand(revealer)
        val predicateContext = PredicateContext(
            controllerId = revealer,
            sourceId = context.sourceId,
            targetOpponentId = context.targets.firstNotNullOfOrNull {
                (it as? com.wingedsheep.engine.state.components.stack.ChosenTarget.Player)?.playerId
            },
        )
        val eligible = handCards.filter { cardId ->
            predicateEvaluator.matches(state, state.projectedState, cardId, effect.filter, predicateContext)
        }

        if (eligible.isEmpty()) {
            return runOtherwise(state, effect.otherwise, context)
        }

        val sourceName = context.sourceId
            ?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        val decisionId = UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = revealer,
            prompt = "You may reveal a ${effect.filter.description} card from your hand",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION,
            ),
            options = eligible,
            minSelections = 0,
            maxSelections = 1,
        )

        val continuation = MayRevealCardFromHandContinuation(
            decisionId = decisionId,
            revealerId = revealer,
            sourceId = context.sourceId,
            sourceName = sourceName,
            otherwise = effect.otherwise,
            effectContext = context,
        )

        val paused = state
            .pushContinuation(continuation)
            .withPendingDecision(decision)
        return EffectResult.paused(paused, decision)
    }

    private fun runOtherwise(
        state: GameState,
        otherwise: Effect?,
        context: EffectContext,
    ): EffectResult {
        if (otherwise == null) return EffectResult.success(state)
        return effectExecutor(state, otherwise, context)
    }

    companion object {
        /**
         * Emit the public reveal of [cardId] (revealed from [revealerId]'s hand) and
         * tag the card as revealed to the rest of the table. Shared between the
         * executor (no-decline auto-flow) and the continuation resumer.
         */
        fun emitReveal(
            state: GameState,
            revealerId: com.wingedsheep.sdk.model.EntityId,
            cardId: com.wingedsheep.sdk.model.EntityId,
            sourceName: String?,
        ): Pair<GameState, CardsRevealedEvent> {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>()
            val cardName = cardComponent?.name ?: "Unknown"
            val imageUri = cardComponent?.imageUri
            val newState = state.updateEntity(cardId) { container ->
                val existing = container
                    .get<com.wingedsheep.engine.state.components.identity.RevealedToComponent>()
                val merged = (existing?.playerIds ?: emptySet()) + state.turnOrder
                container.with(
                    com.wingedsheep.engine.state.components.identity.RevealedToComponent(merged)
                )
            }
            val event = CardsRevealedEvent(
                revealingPlayerId = revealerId,
                cardIds = listOf(cardId),
                cardNames = listOf(cardName),
                imageUris = listOf(imageUri),
                source = sourceName,
                revealToSelf = false,
                fromZone = Zone.HAND,
            )
            return newState to event
        }
    }
}
