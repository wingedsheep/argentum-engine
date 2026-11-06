package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.mechanics.sba.StateBasedActionModule

class PlayerSbaModule(
    private val zones: ZoneTransitionService,
    private val decisionHandler: com.wingedsheep.engine.handlers.DecisionHandler,
    /** See [PlayerLeavesGameProcessor.process]. */
    private val finishAbandonedResolution: (com.wingedsheep.engine.state.GameState) -> ExecutionResult,
) : StateBasedActionModule {
    override fun checks(): List<StateBasedActionCheck> = listOf(
        StartYourEnginesCheck(),
        AscendCitysBlessingCheck(),
        StoriedEnduringStoryCheck(),
        DungeonCompletionCheck(),
        PlayerLifeLossCheck(predicateEvaluator = zones.predicateEvaluator),
        EmptyLibraryDrawLossCheck(predicateEvaluator = zones.predicateEvaluator),
        CommanderDamageLossCheck(predicateEvaluator = zones.predicateEvaluator),
        PoisonLossCheck(predicateEvaluator = zones.predicateEvaluator),
        GameLossSourceChoiceCheck(zones, decisionHandler),
        TeamLossPropagationCheck(),
        PlayerLeavesGameCheck(zones, finishAbandonedResolution)
    )
}
