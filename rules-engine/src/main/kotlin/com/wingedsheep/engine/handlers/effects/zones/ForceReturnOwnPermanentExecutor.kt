package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.ReturnToHandContinuation
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.BattlefieldFilterUtils
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ForceReturnOwnPermanentEffect
import kotlin.reflect.KClass

/**
 * Executor for ForceReturnOwnPermanentEffect.
 *
 * The controller selects a permanent they control matching the filter and returns
 * it to its owner's hand. If only one valid permanent exists, it's auto-selected.
 * If none exist, the effect does nothing.
 */
class ForceReturnOwnPermanentExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ForceReturnOwnPermanentEffect> {

    override val effectType: KClass<ForceReturnOwnPermanentEffect> = ForceReturnOwnPermanentEffect::class

    override fun execute(
        state: GameState,
        effect: ForceReturnOwnPermanentEffect,
        context: EffectContext
    ): ExecutionResult {
        val controllerId = context.controllerId
        val sourceId = context.sourceId
        val excludeId = if (effect.excludeSource) sourceId else null

        val validPermanents = BattlefieldFilterUtils.findMatchingOnBattlefield(
            state, effect.filter.youControl(), PredicateContext(controllerId = controllerId), excludeId
        )

        if (validPermanents.isEmpty()) {
            return ExecutionResult.success(state)
        }

        if (validPermanents.size == 1) {
            return returnPermanentToHand(state, validPermanents.first())
        }

        // Multiple choices — present selection decision
        val sourceName = sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "ability"
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = state,
            playerId = controllerId,
            sourceId = sourceId,
            sourceName = sourceName,
            prompt = "Choose a ${effect.filter.description} to return to hand",
            options = validPermanents,
            minSelections = 1,
            maxSelections = 1,
            ordered = false,
            phase = DecisionPhase.RESOLUTION,
            useTargetingUI = true
        )

        val continuation = ReturnToHandContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = controllerId,
            sourceId = sourceId
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events.toList()
        )
    }

    internal fun returnPermanentToHand(
        state: GameState,
        permanentId: EntityId
    ): ExecutionResult {
        val transitionResult = ZoneTransitionService.moveToZone(state, permanentId, Zone.HAND)
        return ExecutionResult.success(transitionResult.state, transitionResult.events)
    }
}
