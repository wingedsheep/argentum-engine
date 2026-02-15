package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.SecretBidEffect
import kotlin.reflect.KClass

/**
 * Executor for SecretBidEffect (Menacing Ogre).
 *
 * Each player secretly chooses a number (0 or more). Then those numbers are revealed.
 * Each player with the highest number loses that much life. If the controller is one
 * of those players, put counters on the source creature.
 *
 * Implementation: players choose sequentially in APNAP order (the numbers aren't
 * visible to opponents since each decision is private to the deciding player).
 */
class SecretBidExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<SecretBidEffect> {

    override val effectType: KClass<SecretBidEffect> = SecretBidEffect::class

    override fun execute(
        state: GameState,
        effect: SecretBidEffect,
        context: EffectContext
    ): ExecutionResult {
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")

        val playerOrder = listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }

        return askPlayerToChoose(
            state = state,
            effect = effect,
            context = context,
            playerOrder = playerOrder,
            currentPlayerIndex = 0,
            chosenNumbers = emptyMap()
        )
    }

    private fun askPlayerToChoose(
        state: GameState,
        effect: SecretBidEffect,
        context: EffectContext,
        playerOrder: List<EntityId>,
        currentPlayerIndex: Int,
        chosenNumbers: Map<EntityId, Int>
    ): ExecutionResult {
        val playerId = playerOrder[currentPlayerIndex]

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val prompt = "Secretly choose a number (you will lose that much life if you have the highest bid)"

        val decisionResult = decisionHandler.createNumberDecision(
            state = state,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = prompt,
            minValue = 0,
            maxValue = 99,
            phase = DecisionPhase.RESOLUTION
        )

        val remainingPlayers = playerOrder.drop(currentPlayerIndex + 1)

        val continuation = SecretBidContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            sourceId = context.sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            currentPlayerId = playerId,
            remainingPlayers = remainingPlayers,
            chosenNumbers = chosenNumbers,
            counterType = effect.counterType,
            counterCount = effect.counterCount
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }
}
