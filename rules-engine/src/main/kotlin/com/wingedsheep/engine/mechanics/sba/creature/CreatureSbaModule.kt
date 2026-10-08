package com.wingedsheep.engine.mechanics.sba.creature

import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.mechanics.sba.StateBasedActionModule

class CreatureSbaModule(private val zones: ZoneTransitionService) : StateBasedActionModule {
    override fun checks(): List<StateBasedActionCheck> = listOf(
        ControlChangedRemovesFromCombatCheck(),
        StoppedBeingCreatureRemovesFromCombatCheck(),
        ZeroToughnessCheck(zones),
        LethalDamageCheck(zones)
    )
}
