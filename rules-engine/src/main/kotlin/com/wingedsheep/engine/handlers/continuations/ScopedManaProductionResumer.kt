package com.wingedsheep.engine.handlers.continuations

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.handlers.effects.mana.ManaAbilityResolutionPipeline
import com.wingedsheep.engine.handlers.effects.mana.finishScopedManaProduction

class ScopedManaProductionResumer(services: EngineServices) : AutoResumerModule {
    private val pipeline = ManaAbilityResolutionPipeline(services.cardRegistry, services.conditionEvaluator,
        services.effectExecutorRegistry, services.predicateEvaluator, services.dynamicAmountEvaluator)

    override fun autoResumers(): List<AutoResumer<*>> = listOf(
        autoResumer(ScopedManaProductionContinuation::class) { state, frame, events, checkForMore ->
            val result = finishScopedManaProduction(state, frame, events, pipeline)
            if (result.outcome is Outcome.Paused) result else checkForMore(result.state, result.events)
        }
    )
}
