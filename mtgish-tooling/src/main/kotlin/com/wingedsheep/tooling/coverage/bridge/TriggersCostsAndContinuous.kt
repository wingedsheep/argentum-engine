package com.wingedsheep.tooling.coverage.bridge

/** Triggered-ability conditions and costs (accepted as [supported] pending a `Triggers.*`/`Costs.*`
 *  facade scan), plus the duration-scoped trigger/replacement creators (composed from primitives). */
internal fun BridgeBuilder.triggersCostsAndContinuous() {
    // Triggers — validated by a Triggers.* facade scan in a later phase.
    supported("WhenAPermanentEntersTheBattlefield", "trigger: ETB (Triggers.* scan validates in P1)")
    supported("WhenACreatureOrPlaneswalkerDies", "trigger: dies")
    supported("WhenACreatureAttacks", "trigger: attacks")
    supported("WhenACreatureDealsCombatDamageToAPlayer", "trigger: combat damage to player")

    // Costs.
    supported("PayMana", "cost: pay mana (universal)")
    supported("SacrificeAPermanent", "cost: sacrifice")
    supported("SacrificeNumberPermanents", "cost: sacrifice N")
    composed("DiscardACardOfType", "cost: discard filtered")

    // Duration-scoped continuous trigger / replacement creators.
    composed("CreateReplaceWouldDealDamageUntil", "PreventDamageShield / RedirectNextDamage", composes = listOf("PreventDamageShield"))
    composed("CreateTriggerUntil", "CreateGlobalTriggeredAbility (duration)", composes = listOf("CreateGlobalTriggeredAbility"))
    composed("CreateFutureTrigger", "CreateDelayedTrigger", composes = listOf("CreateDelayedTrigger"))
}
