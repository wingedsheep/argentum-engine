package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ChooseColorProtectionContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.ChooseColorAndGrantProtectionToGroupEffect
import kotlin.reflect.KClass

/**
 * Executor for ChooseColorAndGrantProtectionToGroupEffect.
 *
 * "Choose a color. Creatures you control gain protection from the chosen color until end of turn."
 *
 * This executor creates a color choice decision and pushes a continuation.
 * When the player responds, the ContinuationHandler creates the floating effects.
 */
class ChooseColorProtectionExecutor(
    private val decisionHandler: DecisionHandler
) : EffectExecutor<ChooseColorAndGrantProtectionToGroupEffect> {

    override val effectType: KClass<ChooseColorAndGrantProtectionToGroupEffect> =
        ChooseColorAndGrantProtectionToGroupEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseColorAndGrantProtectionToGroupEffect,
        context: EffectContext
    ): ExecutionResult {
        val sourceName = context.sourceId?.let { state.getEntity(it)?.get<CardComponent>()?.name } ?: "Unknown"

        // Create the color choice decision
        val decisionResult = decisionHandler.createColorDecision(
            state = state,
            playerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            prompt = "Choose a color for protection",
            phase = DecisionPhase.RESOLUTION
        )

        // Push continuation to handle the response
        val continuation = ChooseColorProtectionContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            filter = effect.filter,
            duration = effect.duration
        )

        val stateWithContinuation = decisionResult.state.pushContinuation(continuation)

        return ExecutionResult.paused(
            stateWithContinuation,
            decisionResult.pendingDecision,
            decisionResult.events
        )
    }
}
