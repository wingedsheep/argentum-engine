package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.AnyPlayerMayPayEffect
import com.wingedsheep.sdk.scripting.costs.PayCost
import kotlin.reflect.KClass

/**
 * Executor for AnyPlayerMayPayEffect.
 *
 * Handles "any player may [cost]. If a player does, [consequence]."
 *
 * Each player in APNAP order gets the chance to pay the cost.
 * As soon as any player pays, the consequence is executed.
 * If no player pays, nothing happens.
 *
 * Example: Prowling Pangolin - "Any player may sacrifice two creatures.
 * If a player does, sacrifice Prowling Pangolin."
 */
class AnyPlayerMayPayExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<AnyPlayerMayPayEffect> {

    override val effectType: KClass<AnyPlayerMayPayEffect> = AnyPlayerMayPayEffect::class

    private val predicateEvaluator = PredicateEvaluator()
    private val stateProjector = StateProjector()

    override fun execute(
        state: GameState,
        effect: AnyPlayerMayPayEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceId = context.sourceId
            ?: return ExecutionResult.error(state, "No source for any player may pay effect")

        val sourceContainer = state.getEntity(sourceId)
            ?: return ExecutionResult.error(state, "Source entity not found")
        val sourceCard = sourceContainer.get<CardComponent>()
            ?: return ExecutionResult.error(state, "Source has no card component")

        // Get players in APNAP order
        val activePlayer = state.activePlayerId
            ?: return ExecutionResult.error(state, "No active player")
        val playerOrder = listOf(activePlayer) + state.turnOrder.filter { it != activePlayer }

        return askNextPlayer(state, effect, context, sourceId, sourceCard.name, playerOrder, 0)
    }

    /**
     * Ask the next player in APNAP order if they want to pay the cost.
     * Skips players who can't pay.
     */
    private fun askNextPlayer(
        state: GameState,
        effect: AnyPlayerMayPayEffect,
        context: EffectContext,
        sourceId: EntityId,
        sourceName: String,
        playerOrder: List<EntityId>,
        currentIndex: Int
    ): ExecutionResult {
        // Find the next player who can pay
        var index = currentIndex
        while (index < playerOrder.size) {
            val playerId = playerOrder[index]
            if (canPlayerPay(state, playerId, effect.cost, sourceId)) {
                break
            }
            index++
        }

        // No more players can pay - nothing happens
        if (index >= playerOrder.size) {
            return ExecutionResult.success(state)
        }

        val playerId = playerOrder[index]
        return when (val cost = effect.cost) {
            is PayCost.Sacrifice -> askPlayerToSacrifice(
                state, effect, context, cost, sourceId, sourceName,
                playerId, playerOrder, index
            )
            else -> ExecutionResult.error(state, "Unsupported cost type for AnyPlayerMayPay: ${cost::class.simpleName}")
        }
    }

    private fun canPlayerPay(
        state: GameState,
        playerId: EntityId,
        cost: PayCost,
        sourceId: EntityId
    ): Boolean {
        return when (cost) {
            is PayCost.Sacrifice -> {
                val validPermanents = findValidPermanentsOnBattlefield(state, playerId, cost.filter, sourceId)
                validPermanents.size >= cost.count
            }
            else -> false
        }
    }

    private fun askPlayerToSacrifice(
        state: GameState,
        effect: AnyPlayerMayPayEffect,
        context: EffectContext,
        cost: PayCost.Sacrifice,
        sourceId: EntityId,
        sourceName: String,
        playerId: EntityId,
        playerOrder: List<EntityId>,
        currentIndex: Int
    ): ExecutionResult {
        val validPermanents = findValidPermanentsOnBattlefield(state, playerId, cost.filter, sourceId)

        val prompt = "You may sacrifice ${cost.count} ${cost.filter.description}s to cause $sourceName to be sacrificed, or skip"

        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = playerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = prompt,
            options = validPermanents,
            minSelections = 0,
            maxSelections = cost.count,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true
        )

        val continuation = AnyPlayerMayPayContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            currentPlayerId = playerId,
            remainingPlayers = playerOrder.drop(currentIndex + 1),
            sourceId = sourceId,
            sourceName = sourceName,
            controllerId = context.controllerId,
            cost = effect.cost,
            consequence = effect.consequence,
            requiredCount = cost.count,
            filter = cost.filter
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }

    private fun findValidPermanentsOnBattlefield(
        state: GameState,
        playerId: EntityId,
        filter: com.wingedsheep.sdk.scripting.GameObjectFilter,
        sourceId: EntityId
    ): List<EntityId> {
        val battlefieldZone = ZoneKey(playerId, Zone.BATTLEFIELD)
        val battlefield = state.getZone(battlefieldZone)
        val context = PredicateContext(controllerId = playerId)
        val projected = stateProjector.project(state)

        return battlefield.filter { permanentId ->
            predicateEvaluator.matchesWithProjection(state, projected, permanentId, filter, context)
        }
    }
}
