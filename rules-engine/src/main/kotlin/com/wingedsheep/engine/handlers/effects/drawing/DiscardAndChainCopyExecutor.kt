package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.DiscardAndChainCopyEffect
import com.wingedsheep.sdk.targeting.TargetPlayer
import java.util.UUID
import kotlin.reflect.KClass

/**
 * Executor for DiscardAndChainCopyEffect.
 *
 * Makes target player discard N cards, then offers that player a copy
 * of the spell that can target a new player. The copy itself has
 * the same effect, enabling recursive chaining.
 */
class DiscardAndChainCopyExecutor(
    private val targetFinder: TargetFinder = TargetFinder(),
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<DiscardAndChainCopyEffect> {

    override val effectType: KClass<DiscardAndChainCopyEffect> = DiscardAndChainCopyEffect::class

    override fun execute(
        state: GameState,
        effect: DiscardAndChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.success(state)

        val handZone = ZoneKey(targetPlayerId, Zone.HAND)
        val hand = state.getZone(handZone)

        // If hand has fewer cards than required, discard all and immediately offer chain copy
        if (hand.size <= effect.count) {
            val discardResult = discardCards(state, targetPlayerId, hand)
            if (!discardResult.isSuccess) return discardResult

            return offerChainCopy(
                discardResult.state,
                discardResult.events.toMutableList(),
                targetPlayerId,
                effect,
                context
            )
        }

        // Player must choose which cards to discard — pause with a continuation
        // that will handle both the discard AND the chain copy offer
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = targetPlayerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose ${effect.count} card${if (effect.count > 1) "s" else ""} to discard",
            options = hand,
            minSelections = effect.count,
            maxSelections = effect.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        val continuation = DiscardForChainContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = targetPlayerId,
            count = effect.count,
            spellName = effect.spellName,
            sourceId = context.sourceId
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    /**
     * After discarding, offer the target player the option to copy the spell.
     */
    private fun offerChainCopy(
        state: GameState,
        events: MutableList<GameEvent>,
        targetPlayerId: EntityId,
        effect: DiscardAndChainCopyEffect,
        context: EffectContext
    ): ExecutionResult {
        // Check if there are valid player targets for the chain copy
        val requirement = TargetPlayer()
        val legalTargets = targetFinder.findLegalTargets(
            state, requirement, targetPlayerId, context.sourceId
        )

        if (legalTargets.isEmpty()) {
            return ExecutionResult.success(state, events)
        }

        val decisionId = UUID.randomUUID().toString()
        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        } ?: effect.spellName

        val decision = YesNoDecision(
            id = decisionId,
            playerId = targetPlayerId,
            prompt = "Copy ${effect.spellName} and choose a new target?",
            context = DecisionContext(
                sourceId = context.sourceId,
                sourceName = sourceName,
                phase = DecisionPhase.RESOLUTION
            ),
            yesText = "Copy",
            noText = "Decline"
        )

        val continuation = DiscardChainCopyDecisionContinuation(
            decisionId = decisionId,
            targetPlayerId = targetPlayerId,
            count = effect.count,
            spellName = effect.spellName,
            sourceId = context.sourceId
        )

        val newState = state.withPendingDecision(decision).pushContinuation(continuation)

        return ExecutionResult.paused(
            newState,
            decision,
            events + listOf(
                DecisionRequestedEvent(
                    decisionId = decisionId,
                    playerId = targetPlayerId,
                    decisionType = "YES_NO",
                    prompt = decision.prompt
                )
            )
        )
    }

    private fun discardCards(
        state: GameState,
        playerId: EntityId,
        cardIds: List<EntityId>
    ): ExecutionResult {
        var newState = state
        val handZone = ZoneKey(playerId, Zone.HAND)
        val graveyardZone = ZoneKey(playerId, Zone.GRAVEYARD)

        for (cardId in cardIds) {
            newState = newState.removeFromZone(handZone, cardId)
            newState = newState.addToZone(graveyardZone, cardId)
        }

        return ExecutionResult.success(
            newState,
            listOf(CardsDiscardedEvent(playerId, cardIds))
        )
    }
}
