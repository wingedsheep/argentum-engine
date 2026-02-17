package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all library-related effect executors.
 */
class LibraryExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        ShuffleLibraryExecutor(),
        ShuffleGraveyardIntoLibraryExecutor(),


        PutLandFromHandExecutor(),
        ChooseCreatureTypeRevealTopExecutor(),
        RevealUntilExecutor(),
        EachOpponentMayPutFromHandExecutor(),
        PutCreatureFromHandSharingTypeExecutor(),
        ChooseCreatureTypePipelineExecutor(),
        EachPlayerMayRevealCreaturesExecutor(),
        EachPlayerSearchesLibraryExecutor(),
        SearchTargetLibraryExileExecutor(),
        GatherCardsExecutor(),
        SelectFromCollectionExecutor(),
        MoveCollectionExecutor()
    )
}
