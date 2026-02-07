package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils.resolvePlayerTarget
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.BlackmailEffect
import kotlin.reflect.KClass

/**
 * Executor for BlackmailEffect.
 *
 * "Target player reveals three cards from their hand and you choose one of them.
 * That player discards that card."
 *
 * Two-step decision process:
 * 1. If target player has more than [revealCount] cards, they choose which to reveal.
 *    Otherwise, all cards are auto-revealed.
 * 2. Controller chooses one of the revealed cards for the target to discard.
 */
class BlackmailExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<BlackmailEffect> {

    override val effectType: KClass<BlackmailEffect> = BlackmailEffect::class

    override fun execute(
        state: GameState,
        effect: BlackmailEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetPlayerId = resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "No valid player for Blackmail")

        val handZone = ZoneKey(targetPlayerId, Zone.HAND)
        val hand = state.getZone(handZone)

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        // If hand is empty, nothing happens
        if (hand.isEmpty()) {
            return ExecutionResult.success(state.tick())
        }

        // If hand has revealCount or fewer cards, all are revealed - skip to controller choosing
        if (hand.size <= effect.revealCount) {
            return askControllerToChoose(state, context.controllerId, targetPlayerId, context.sourceId, sourceName, hand)
        }

        // Target player must choose which cards to reveal
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = targetPlayerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose ${effect.revealCount} cards to reveal",
            options = hand,
            minSelections = effect.revealCount,
            maxSelections = effect.revealCount,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        val continuation = BlackmailRevealContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            controllerId = context.controllerId,
            targetPlayerId = targetPlayerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            revealedCards = emptyList() // Will be filled from the response
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    companion object {
        /**
         * Create the decision for the controller to choose which revealed card the target discards.
         * Used both by the executor (when hand ≤ revealCount) and by the continuation handler.
         */
        fun askControllerToChoose(
            state: GameState,
            controllerId: com.wingedsheep.sdk.model.EntityId,
            targetPlayerId: com.wingedsheep.sdk.model.EntityId,
            sourceId: com.wingedsheep.sdk.model.EntityId?,
            sourceName: String?,
            revealedCards: List<com.wingedsheep.sdk.model.EntityId>
        ): ExecutionResult {
            val decisionHandler = DecisionHandler()

            val decisionResult = decisionHandler.createCardSelectionDecision(
                state = state,
                playerId = controllerId,
                sourceId = sourceId,
                sourceName = sourceName,
                prompt = "Choose a card for the target player to discard",
                options = revealedCards,
                minSelections = 1,
                maxSelections = 1,
                ordered = false,
                phase = DecisionPhase.RESOLUTION
            )

            val continuation = BlackmailChooseContinuation(
                decisionId = decisionResult.pendingDecision!!.id,
                targetPlayerId = targetPlayerId,
                sourceId = sourceId,
                sourceName = sourceName
            )

            val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

            return ExecutionResult.paused(
                stateWithContinuation,
                decisionResult.pendingDecision,
                decisionResult.events
            )
        }
    }
}
