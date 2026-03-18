package com.wingedsheep.engine.mechanics.sba.creature

import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.mechanics.sba.StateBasedActionModule

class CreatureSbaModule : StateBasedActionModule {
    override fun checks(): List<StateBasedActionCheck> = listOf(
        ZeroToughnessCheck(),
        LethalDamageCheck()
    )
}
