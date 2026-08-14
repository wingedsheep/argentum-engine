package com.wingedsheep.engine.mechanics.sba.permanent

import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.mechanics.sba.StateBasedActionCheck
import com.wingedsheep.engine.mechanics.sba.StateBasedActionModule
import com.wingedsheep.engine.registry.CardRegistry

class PermanentSbaModule(
    private val decisionHandler: DecisionHandler,
    private val cardRegistry: CardRegistry
) : StateBasedActionModule {
    override fun checks(): List<StateBasedActionCheck> = listOf(
        DayNightCheck(cardRegistry),
        EndedDurationExpiryCheck(),
        AttachedCopyExpiryCheck(),
        PlaneswalkerLoyaltyCheck(),
        BattleDefenseCheck(),
        BattleProtectorCheck(),
        LegendRuleCheck(decisionHandler, cardRegistry),
        CounterAnnihilationCheck(),
        UnattachedAurasCheck(cardRegistry),
        SoulbondPairingCheck(),
        SagaSacrificeCheck(cardRegistry),
        CommanderZoneChoiceCheck(decisionHandler),
    )
}
