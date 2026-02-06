package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.PutLandFromHandOntoBattlefieldEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for PutLandFromHandOntoBattlefieldEffect.
 * "You may put a basic land card from your hand onto the battlefield tapped."
 *
 * Filters the player's hand for cards matching the filter, then creates a
 * SelectCardsDecision (with min=0 for optional) for the player to choose.
 */
class PutLandFromHandExecutor : EffectExecutor<PutLandFromHandOntoBattlefieldEffect> {

    override val effectType: KClass<PutLandFromHandOntoBattlefieldEffect> = PutLandFromHandOntoBattlefieldEffect::class

    private val predicateEvaluator = PredicateEvaluator()

    override fun execute(
        state: GameState,
        effect: PutLandFromHandOntoBattlefieldEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val handZone = ZoneKey(controllerId, Zone.HAND)
        val hand = state.getZone(handZone)

        // Filter hand for matching cards
        val predicateContext = PredicateContext.fromEffectContext(context)
        val matchingCards = hand.filter { cardId ->
            predicateEvaluator.matches(state, cardId, effect.filter, predicateContext)
        }

        // No matching cards in hand — effect does nothing
        if (matchingCards.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Get source name for the decision context
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Create the decision for card selection (min=0 since this is typically optional via MayEffect)
        val decisionId = UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "Choose a ${effect.filter.description} card from your hand to put onto the battlefield",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = matchingCards,
            minSelections = 0,
            maxSelections = 1
        )

        // Push continuation
        val continuation = PutFromHandContinuation(
            decisionId = decisionId,
            playerId = controllerId,
            entersTapped = effect.entersTapped,
            sourceId = context.sourceId,
            sourceName = sourceName
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)

        return ExecutionResult.paused(newState, decision)
    }
}
