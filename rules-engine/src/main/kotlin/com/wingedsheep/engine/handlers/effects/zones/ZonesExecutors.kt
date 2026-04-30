package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Module providing zone-transition effect executors — effects that physically move
 * entities between zones (battlefield, graveyard, exile, hand, library) without
 * adding or removing link bookkeeping.
 */
class ZonesExecutors(
    private val cardRegistry: CardRegistry
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        MoveToZoneEffectExecutor(cardRegistry),
        ExileAndGrantOwnerPlayPermissionExecutor(),
        WarpExileExecutor(),
        ForceExileMultiZoneExecutor(),
        ForceReturnOwnPermanentExecutor(),
        ForceSacrificeExecutor(),
        SacrificeExecutor(),
        SacrificeSelfExecutor(),
        SacrificeTargetExecutor(),
        ReturnCreaturesPutInGraveyardThisTurnExecutor(),
        ReturnSelfToBattlefieldAttachedExecutor(cardRegistry),
        ExileOpponentsGraveyardsExecutor(),
        DestroyAllEquipmentOnTargetExecutor()
    )
}
