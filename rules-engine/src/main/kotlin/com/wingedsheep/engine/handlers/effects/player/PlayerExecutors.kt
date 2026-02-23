package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all player-related effect executors.
 */
class PlayerExecutors(
    private val decisionHandler: DecisionHandler = DecisionHandler()
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        AddCombatPhaseExecutor(),
        CreateGlobalTriggeredAbilityUntilEndOfTurnExecutor(),
        GrantShroudExecutor(),
        PlayAdditionalLandsExecutor(),
        PreventLandPlaysThisTurnExecutor(),
        SecretBidExecutor(decisionHandler),
        SkipCombatPhasesExecutor(),
        SkipUntapExecutor(),
        TakeExtraTurnExecutor()
    )
}
