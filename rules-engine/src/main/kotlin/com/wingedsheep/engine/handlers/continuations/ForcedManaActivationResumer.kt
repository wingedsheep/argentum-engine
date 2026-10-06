package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.ActivateManaAbilityChoiceContinuation
import com.wingedsheep.engine.core.EngineServices
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.handlers.effects.mana.ForcedManaActivation

/** Activates the mana ability chosen for an instructed activation, then resumes the resolution. */
class ForcedManaActivationResumer(private val services: EngineServices) : ContinuationResumerModule {
    override fun resumers(): List<ContinuationResumer<*>> = listOf(
        resumer(ActivateManaAbilityChoiceContinuation::class) { state, frame, response, checkForMore ->
            val index = (response as? OptionChosenResponse)?.optionIndex
                ?: return@resumer ExecutionResult.error(state, "Expected a mana ability choice")
            val action = frame.options.getOrNull(index)
                ?: return@resumer ExecutionResult.error(state, "Invalid mana ability choice: $index")
            if (!state.isCurrentObject(frame.source)) return@resumer checkForMore(state, emptyList())
            val result = ForcedManaActivation.activate(state, services.activateAbilityHandler, action)
            if (result.outcome is Outcome.Paused) result else checkForMore(result.state, result.events)
        }
    )
}
