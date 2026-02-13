package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.ChooseColorProtectionTargetContinuation
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.EffectExecutorUtils
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.ChooseColorAndGrantProtectionToTargetEffect
import kotlin.reflect.KClass

/**
 * Executor for ChooseColorAndGrantProtectionToTargetEffect.
 *
 * "{W}: This creature gains protection from the color of your choice until end of turn."
 *
 * This executor resolves the target, creates a color choice decision, and pushes a continuation.
 * When the player responds, the ContinuationHandler creates the floating effect on the target.
 */
class ChooseColorProtectionTargetExecutor(
    private val decisionHandler: DecisionHandler
) : EffectExecutor<ChooseColorAndGrantProtectionToTargetEffect> {

    override val effectType: KClass<ChooseColorAndGrantProtectionToTargetEffect> =
        ChooseColorAndGrantProtectionToTargetEffect::class

    override fun execute(
        state: GameState,
        effect: ChooseColorAndGrantProtectionToTargetEffect,
        context: EffectContext
    ): ExecutionResult {
        val targetEntityId = EffectExecutorUtils.resolveTarget(effect.target, context, state)
            ?: return ExecutionResult.error(state, "Could not resolve target for protection effect")

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
        val continuation = ChooseColorProtectionTargetContinuation(
            decisionId = decisionResult.pendingDecision!!.id,
            controllerId = context.controllerId,
            sourceId = context.sourceId,
            sourceName = sourceName,
            targetEntityId = targetEntityId,
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
