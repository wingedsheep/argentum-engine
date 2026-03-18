package com.wingedsheep.engine.mechanics.sba.zone

import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.mechanics.sba.StateBasedActionModule

class ZoneSbaModule : StateBasedActionModule {
    override fun checks(): List<StateBasedActionCheck> = listOf(
        TokensInWrongZonesCheck()
    )
}
