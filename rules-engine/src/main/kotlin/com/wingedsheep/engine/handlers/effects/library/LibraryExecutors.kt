package com.wingedsheep.engine.handlers.effects.library

import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.handlers.effects.ExecutorModule

/**
 * Module providing all library-related effect executors.
 */
class LibraryExecutors : ExecutorModule {
    override fun executors(): List<EffectExecutor<*>> = listOf(
        ScryExecutor(),
        MillExecutor(),
        ShuffleLibraryExecutor(),
        ShuffleGraveyardIntoLibraryExecutor(),
        SearchLibraryExecutor(),
        LookAtTopAndReorderExecutor(),
        LookAtOpponentLibraryExecutor(),
        LookAtTopCardsExecutor(),
        RevealAndOpponentChoosesExecutor(),
        WheelEffectExecutor(),
        PutLandFromHandExecutor(),
        ChooseCreatureTypeRevealTopExecutor(),
        RevealUntilNonlandDealDamageExecutor(),
        RevealUntilNonlandModifyStatsExecutor()
    )
}
