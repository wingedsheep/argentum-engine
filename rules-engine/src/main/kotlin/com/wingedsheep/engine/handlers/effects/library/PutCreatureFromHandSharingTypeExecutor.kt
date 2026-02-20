package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.PutCreatureFromHandSharingTypeWithTappedEffect
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for PutCreatureFromHandSharingTypeWithTappedEffect.
 *
 * Finds creature cards in the controller's hand that share a creature type
 * with each creature tapped as part of the cost, then presents them as choices.
 * Uses PutFromHandContinuation to handle the actual card movement.
 */
class PutCreatureFromHandSharingTypeExecutor : EffectExecutor<PutCreatureFromHandSharingTypeWithTappedEffect> {

    override val effectType: KClass<PutCreatureFromHandSharingTypeWithTappedEffect> =
        PutCreatureFromHandSharingTypeWithTappedEffect::class

    override fun execute(
        state: GameState,
        effect: PutCreatureFromHandSharingTypeWithTappedEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val tappedPermanents = context.tappedPermanents

        if (tappedPermanents.isEmpty()) {
            return ExecutionResult.success(state)
        }

        // Get the subtypes of each tapped creature
        val tappedCreatureSubtypes = tappedPermanents.map { entityId ->
            val card = state.getEntity(entityId)?.get<CardComponent>()
            card?.typeLine?.subtypes?.map { it.value }?.toSet() ?: emptySet()
        }

        // Find creature cards in hand that share a type with EACH tapped creature
        val handZone = ZoneKey(controllerId, Zone.HAND)
        val hand = state.getZone(handZone)

        val validCreatures = hand.filter { cardId ->
            val card = state.getEntity(cardId)?.get<CardComponent>() ?: return@filter false
            if (!card.typeLine.isCreature) return@filter false

            val cardSubtypes = card.typeLine.subtypes.map { it.value }.toSet()

            // Must share at least one creature type with EACH tapped creature
            tappedCreatureSubtypes.all { tappedTypes ->
                cardSubtypes.any { it in tappedTypes }
            }
        }

        // No valid creatures — effect does nothing
        if (validCreatures.isEmpty()) {
            return ExecutionResult.success(state)
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }

        // Create selection decision (min=0 since it's "you may")
        val decisionId = UUID.randomUUID().toString()
        val decision = SelectCardsDecision(
            id = decisionId,
            playerId = controllerId,
            prompt = "You may put a creature card from your hand onto the battlefield",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            options = validCreatures,
            minSelections = 0,
            maxSelections = 1
        )

        // Reuse PutFromHandContinuation — it handles moving from hand to battlefield
        val continuation = PutFromHandContinuation(
            decisionId = decisionId,
            playerId = controllerId,
            entersTapped = false,
            sourceId = context.sourceId,
            sourceName = sourceName
        )

        val newState = state
            .withPendingDecision(decision)
            .pushContinuation(continuation)

        return ExecutionResult.paused(newState, decision)
    }
}
