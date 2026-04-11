package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing linked-exile effect executors — effects that exile entities with
 * bookkeeping so a later trigger (e.g. Oblivion Ring's leaves-the-battlefield return)
 * can find the exiled object.
 */
class LinkedExileExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        ExileUntilLeavesExecutor(),
        MarkExileOnDeathExecutor(),
        MarkExileControllerGraveyardOnDeathExecutor(),
        ReturnOneFromLinkedExileExecutor()
    )
}
