package com.wingedsheep.engine.handlers.effects.removal

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.scripting.effects.Effect

/**
 * Module providing all removal-related effect executors.
 *
 * Uses deferred initialization to inject the parent registry's execute function
 * into PayOrSufferExecutor, which needs it for executing arbitrary suffer effects.
 */
class RemovalExecutors(
    private val cardRegistry: com.wingedsheep.engine.registry.CardRegistry
) : ExecutorModule {
    private lateinit var effectExecutor: (GameState, Effect, EffectContext) -> ExecutionResult

    private val payOrSufferExecutor by lazy { PayOrSufferExecutor(cardRegistry = cardRegistry, executeEffect = effectExecutor) }

    /**
     * Initialize the module with the parent registry's execute function.
     * Must be called before executors() is accessed.
     */
    fun initialize(executor: (GameState, Effect, EffectContext) -> ExecutionResult) {
        this.effectExecutor = executor
    }

    override fun executors(): List<EffectExecutor<*>> = listOf(
        AnyPlayerMayPayExecutor(),
        CantBeRegeneratedExecutor(),
        DestroyAllEquipmentOnTargetExecutor(),
        ExileUntilLeavesExecutor(),
        MarkExileOnDeathExecutor(),
        MarkExileControllerGraveyardOnDeathExecutor(),
        ForceExileMultiZoneExecutor(),
        ForceSacrificeExecutor(),
        ForceReturnOwnPermanentExecutor(),
        EachPlayerChoosesCreatureTypeExecutor(),
        payOrSufferExecutor,
        RegenerateExecutor(),

        ExileOpponentsGraveyardsExecutor(),
        ReturnCreaturesPutInGraveyardThisTurnExecutor(),
        ReturnOneFromLinkedExileExecutor(),
        SacrificeExecutor(),
        SacrificeSelfExecutor(),
        SacrificeTargetExecutor(),
        MoveToZoneEffectExecutor(cardRegistry),
        PutOnTopOrBottomOfLibraryExecutor(),
        ReturnSelfToBattlefieldAttachedExecutor(cardRegistry),
        WarpExileExecutor()
    )
}
