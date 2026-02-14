package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all player-related effect executors.
 */
class PlayerExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        AddCombatPhaseExecutor(),
        CreateGlobalTriggeredAbilityUntilEndOfTurnExecutor(),
        PlayAdditionalLandsExecutor(),
        SkipCombatPhasesExecutor(),
        SkipUntapExecutor(),
        TakeExtraTurnExecutor()
    )
}
