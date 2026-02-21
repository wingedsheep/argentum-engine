package com.wingedsheep.engine.handlers.effects.drawing

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.DrawUpToEffect
import kotlin.reflect.KClass

/**
 * Executor for DrawUpToEffect.
 *
 * "Draw up to N cards" — the player chooses how many (0 to maxCards).
 * Creates a ChooseNumberDecision and pushes a DrawUpToContinuation.
 * The continuation handler draws the chosen number of cards via DrawCardsEffect
 * through the registry (so draw replacement effects work correctly).
 *
 * If [DrawUpToEffect.storeNotDrawnAs] is set, stores the number of cards NOT drawn
 * (maxCards - chosen) as a named numeric variable in the effect context, allowing
 * subsequent pipeline effects to reference it via DynamicAmount.VariableReference.
 */
class DrawUpToExecutor(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : EffectExecutor<DrawUpToEffect> {

    override val effectType: KClass<DrawUpToEffect> = DrawUpToEffect::class

    override fun execute(
        state: GameState,
        effect: DrawUpToEffect,
        context: EffectContext
    ): ExecutionResult {
        val playerId = EffectExecutorUtils.resolvePlayerTarget(effect.target, context)
            ?: return ExecutionResult.error(state, "DrawUpTo: could not resolve player target")

        val sourceName = context.sourceId?.let { sourceId ->
            state.getEntity(sourceId)?.get<CardComponent>()?.name
        }

        val libraryZone = ZoneKey(playerId, Zone.LIBRARY)
        val librarySize = state.getZone(libraryZone).size
        val actualMax = effect.maxCards.coerceAtMost(librarySize)

        if (actualMax == 0) {
            // Can't draw any cards — store max not drawn and return
            val storeAs = effect.storeNotDrawnAs
            return if (storeAs != null) {
                injectStoredNumber(state, storeAs, effect.maxCards)
            } else {
                ExecutionResult.success(state)
            }
        }

        val decisionResult = decisionHandler.createNumberDecision(
            state = state,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose how many cards to draw (0-$actualMax)",
            minValue = 0,
            maxValue = actualMax,
            phase = DecisionPhase.RESOLUTION
        )

        val continuation = DrawUpToContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            playerId = playerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            maxCards = actualMax,
            originalMaxCards = effect.maxCards,
            storeNotDrawnAs = effect.storeNotDrawnAs
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
         * Inject a stored number into the next EffectContinuation on the stack,
         * following the same injection pattern as SelectFromCollectionContinuation.
         */
        fun injectStoredNumber(state: GameState, name: String, value: Int): ExecutionResult {
            val nextFrame = state.peekContinuation()
            val newState = if (nextFrame is EffectContinuation) {
                val (_, stateAfterPop) = state.popContinuation()
                stateAfterPop.pushContinuation(
                    nextFrame.copy(storedNumbers = nextFrame.storedNumbers + (name to value))
                )
            } else {
                state
            }
            return ExecutionResult.success(newState)
        }
    }
}
