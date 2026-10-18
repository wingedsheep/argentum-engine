package com.wingedsheep.engine.handlers.effects.zones

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Module providing zone-transition effect executors — effects that physically move
 * entities between zones (battlefield, graveyard, exile, hand, library) without
 * adding or removing link bookkeeping.
 */
class ZonesExecutors(
    /**
     * The registry's re-entrant entry point, so [MoveToZoneEffectExecutor] can run an entering
     * permanent's OnEnterRunEffect replacement ("as this enters, …") when an effect puts a card
     * onto the battlefield.
     */
    private val recursion: (GameState, Effect, EffectContext) -> EffectResult,
    private val zones: ZoneTransitionService,
    private val cardRegistry: CardRegistry,
    private val targetFinder: TargetFinder
) : ExecutorModule {


    override fun executors(): List<EffectExecutor<*>> = listOf(
        MoveToZoneEffectExecutor(zones, cardRegistry, targetFinder, recursion),
        ExileAndGrantOwnerPlayPermissionExecutor(zones),
        WarpExileExecutor(zones),
        MoveTrackedBattlefieldObjectExecutor(zones),
        ForceExileMultiZoneExecutor(zones),
        ForceSacrificeExecutor(zones, dynamicAmountEvaluator = zones.predicateEvaluator.amounts),
        SacrificeExecutor(zones),
        SacrificeSelfExecutor(zones),
        SacrificeTargetExecutor(zones),
        EmitExploitedEventExecutor(),
        ReturnCreaturesPutInGraveyardThisTurnExecutor(),
        ReturnSameNamedFromGraveyardExecutor(zones),
        ReturnSelfToBattlefieldAttachedExecutor(cardRegistry),
        PutOntoBattlefieldAsAuraExecutor(zones, cardRegistry),
        PutOntoBattlefieldAsNonAuraExecutor(zones, cardRegistry),
        PutOntoBattlefieldAttachedToChosenExecutor(zones, cardRegistry, targetFinder),
        ExileOpponentsGraveyardsExecutor(),
        DestroyAllEquipmentOnTargetExecutor(zones),
        ChangeOwnershipExecutor()
    )
}
