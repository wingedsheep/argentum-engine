package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule
import com.wingedsheep.engine.registry.CardRegistry

/**
 * Module providing all library-related effect executors.
 */
class LibraryExecutors(
    private val cardRegistry: CardRegistry,
    private val targetFinder: TargetFinder? = null
) : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        ShuffleLibraryExecutor(),
        GrantMayPlayFromExileExecutor(),
        GrantPlayWithoutPayingCostExecutor(),
        GrantPlayWithAdditionalCostExecutor(),
        GrantFreeCastTargetFromExileExecutor(),
        GatherUntilMatchExecutor(),
        RevealCollectionExecutor(),
        ExileFromTopRepeatingExecutor(),
        ExileLibraryUntilManaValueExecutor(),
        GatherSubtypesExecutor(),
        ChooseCreatureTypePipelineExecutor(),
        ChooseOptionPipelineExecutor(),
        GatherCardsExecutor(),
        SelectFromCollectionExecutor(),
        SelectTargetPipelineExecutor(targetFinder = targetFinder ?: TargetFinder()),
        MoveCollectionExecutor(cardRegistry = cardRegistry, targetFinder = targetFinder),
        FilterCollectionExecutor(),
        PutOnTopOrBottomOfLibraryExecutor(),
        StoreNumberExecutor()
    )
}
