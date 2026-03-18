package com.wingedsheep.engine.mechanics.sba.game

import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.mechanics.sba.StateBasedActionModule

class GameSbaModule : StateBasedActionModule {
    override fun checks(): List<StateBasedActionCheck> = listOf(
        GameEndCheck()
    )
}
