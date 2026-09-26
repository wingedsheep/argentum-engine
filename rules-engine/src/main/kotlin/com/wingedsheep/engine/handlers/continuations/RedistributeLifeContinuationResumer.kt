package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.RedistributeLifeTotalsContinuation
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.effects.life.LifeRedistribution
import com.wingedsheep.engine.state.GameState

/**
 * Resumes [RedistributeLifeTotalsContinuation]: record the total the chooser picked for the
 * current player, then ask about the next player or apply the finished redistribution.
 */
class RedistributeLifeContinuationResumer(
    private val services: EngineServices
) : ContinuationResumerModule {

    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(RedistributeLifeTotalsContinuation::class, ::resume),
    )

    private fun resume(
        state: GameState,
        continuation: RedistributeLifeTotalsContinuation,
        response: DecisionResponse,
        checkForMore: CheckForMore
    ): ExecutionResult {
        if (response !is OptionChosenResponse) {
            return ExecutionResult.error(state, "Expected option choice for life redistribution")
        }
        val value = continuation.optionValues.getOrNull(response.optionIndex)
            ?: return ExecutionResult.error(state, "Invalid life total index: ${response.optionIndex}")
        return when (val step = LifeRedistribution.answer(state, continuation, value, services.predicateEvaluator)) {
            is LifeRedistribution.Step.Done -> checkForMore(step.state, step.events)
            is LifeRedistribution.Step.Ask -> state.suspendForDecision(step.question, step.continuation)
        }
    }
}
