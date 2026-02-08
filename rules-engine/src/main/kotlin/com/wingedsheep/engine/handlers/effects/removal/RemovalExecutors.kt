package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all removal-related effect executors.
 */
class RemovalExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        ChooseCreatureTypeReturnExecutor(),
        DestroyAllExecutor(),
        CantBeRegeneratedExecutor(),
        ExileUntilEndStepExecutor(),
        ForceSacrificeExecutor(),
        PayOrSufferExecutor(),
        RegenerateExecutor(),
        SacrificeExecutor(),
        SacrificeSelfExecutor(),
        MoveToZoneEffectExecutor()
    )
}
