package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all permanent-related effect executors.
 */
class PermanentExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        TapUntapExecutor(),
        TapAllCreaturesExecutor(),
        TapTargetCreaturesExecutor(),
        ModifyStatsExecutor(),
        ModifyStatsForGroupExecutor(),
        GrantKeywordUntilEndOfTurnExecutor(),
        AddCountersExecutor(),
        RemoveCountersExecutor()
    )
}
