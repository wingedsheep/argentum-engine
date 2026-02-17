package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all library-related effect executors.
 */
class LibraryExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        MillExecutor(),
        ShuffleLibraryExecutor(),
        ShuffleGraveyardIntoLibraryExecutor(),
        LookAtOpponentLibraryExecutor(),
        RevealAndOpponentChoosesExecutor(),
        WheelEffectExecutor(),
        PutLandFromHandExecutor(),
        ChooseCreatureTypeRevealTopExecutor(),
        RevealUntilNonlandDealDamageExecutor(),
        RevealUntilNonlandModifyStatsExecutor(),
        EachOpponentMayPutFromHandExecutor(),
        PutCreatureFromHandSharingTypeExecutor(),
        RevealUntilCreatureTypeExecutor(),
        EachPlayerMayRevealCreaturesExecutor(),
        EachPlayerSearchesLibraryExecutor(),
        SearchTargetLibraryExileExecutor(),
        RevealUntilNonlandDealDamageEachTargetExecutor(),
        GatherCardsExecutor(),
        SelectFromCollectionExecutor(),
        MoveCollectionExecutor()
    )
}
