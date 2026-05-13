package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ConniveContinuation
import com.wingedsheep.engine.core.CountersAddedEvent
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.ConniveEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for ConniveEffect.
 * Draw a card, then discard a card. If the discarded card is not a land,
 * put a +1/+1 counter on the target permanent.
 */
class ConniveEffectHandler(
    private val drawExecute: (GameState, Effect, EffectContext) -> EffectResult,
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<ConniveEffect> {

    override val effectType: KClass<ConniveEffect> = ConniveEffect::class

    override fun execute(
        state: GameState,
        effect: ConniveEffect,
        context: EffectContext
    ): EffectResult {
        val targetCreatureId = context.resolveTarget(effect.target, state)
            ?: return EffectResult.error(state, "No valid target for Connive")

        val drawResult = drawExecute(state, DrawCardsEffect(1, EffectTarget.Controller), context)
        if (drawResult.pendingDecision != null) {
            return drawResult
        }

        val stateAfterDraw = drawResult.state
        val drawEvents = drawResult.events
        val playerId = context.controllerId
        val hand = stateAfterDraw.getHand(playerId)

        if (hand.isEmpty()) {
            return EffectResult.success(stateAfterDraw, drawEvents)
        }

        if (hand.size == 1) {
            return autoDiscard(stateAfterDraw, drawEvents, playerId, hand.first(), targetCreatureId)
        }

        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name }
        val decisionResult = decisionHandler.createCardSelectionDecision(
            state = stateAfterDraw,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose a card to discard",
            options = hand,
            minSelections = 1,
            maxSelections = 1,
            ordered = false,
            phase = DecisionPhase.RESOLUTION
        )

        val continuation = ConniveContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            controllerId = playerId,
            targetCreatureId = targetCreatureId
        )
        val stateWithCont = decisionResult.state.pushContinuation(continuation)

        return EffectResult.paused(
            stateWithCont,
            decisionResult.pendingDecision,
            drawEvents + decisionResult.events
        )
    }

    private fun autoDiscard(
        state: GameState,
        priorEvents: List<GameEvent>,
        playerId: EntityId,
        cardId: EntityId,
        targetCreatureId: EntityId
    ): EffectResult {
        val isLand = state.getEntity(cardId)?.get<CardComponent>()?.isLand == true
        val discardResult = ZoneTransitionService.discardCard(state, playerId, cardId)
        var newState = discardResult.state
        val events = (priorEvents + discardResult.events).toMutableList()

        if (!isLand) {
            val (counterState, counterEvents) = addPlusOnePlusOne(newState, targetCreatureId)
            newState = counterState
            events.addAll(counterEvents)
        }

        return EffectResult.success(newState, events)
    }

    companion object {
        fun addPlusOnePlusOne(state: GameState, targetId: EntityId): Pair<GameState, List<GameEvent>> {
            val existing = state.getEntity(targetId)?.get<CountersComponent>() ?: CountersComponent()
            val newState = state.updateEntity(targetId) { container ->
                container.with(existing.withAdded(CounterType.PLUS_ONE_PLUS_ONE, 1))
            }
            val name = state.getEntity(targetId)?.get<CardComponent>()?.name ?: ""
            return newState to listOf(CountersAddedEvent(targetId, Counters.PLUS_ONE_PLUS_ONE, 1, name))
        }
    }
}
