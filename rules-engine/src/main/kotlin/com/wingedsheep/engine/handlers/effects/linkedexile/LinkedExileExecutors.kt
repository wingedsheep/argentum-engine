package com.wingedsheep.engine.handlers.effects.linkedexile

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService

/**
 * Module providing linked-exile effect executors — effects that exile entities with
 * bookkeeping so a later trigger (e.g. Oblivion Ring's leaves-the-battlefield return)
 * can find the exiled object.
 */
class LinkedExileExecutors(private val zones: ZoneTransitionService) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        EmitChampionedEventExecutor(),
        ExileUntilLeavesExecutor(zones),
        MoveUntilSourceLeavesExecutor(zones),
        ExileWithAurasNotingCountersExecutor(zones),
        MarkExileOnDeathExecutor(),
        MarkExileControllerGraveyardOnDeathExecutor(),
        ReturnOneFromLinkedExileExecutor(zones),
        ReturnNotedExileTappedWithAurasExecutor(zones),
        RecordChosenLinkedExileExecutor(),
        LinkCollectionToPermanentExecutor()
    )
}
