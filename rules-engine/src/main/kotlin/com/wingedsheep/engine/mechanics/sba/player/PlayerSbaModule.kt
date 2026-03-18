package com.wingedsheep.engine.mechanics.sba.player

import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.mechanics.sba.StateBasedActionModule

class PlayerSbaModule : StateBasedActionModule {
    override fun checks(): List<StateBasedActionCheck> = listOf(
        PlayerLifeLossCheck(),
        PoisonLossCheck()
    )
}
