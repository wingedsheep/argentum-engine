package com.wingedsheep.tooling.coverage.bridge

/** Triggered-ability conditions and costs (accepted as [supported] pending a `Triggers.*`/`Costs.*`
 *  facade scan), plus the duration-scoped trigger/replacement creators (composed from primitives). */
internal fun BridgeBuilder.triggersCostsAndContinuous() {
    // Triggers — validated by a Triggers.* facade scan in a later phase.
    supported("WhenAPermanentEntersTheBattlefield", "trigger: ETB (Triggers.* scan validates in P1)")
    supported("WhenACreatureOrPlaneswalkerDies", "trigger: dies")
    supported("WhenACreatureAttacks", "trigger: attacks")
    supported("WhenACreatureBlocks", "trigger: blocks (Ydwen Efreet)")
    supported("WhenACreatureDealsCombatDamageToAPlayer", "trigger: combat damage to player")
    supported("WhenAPlayerCastsASpell", "trigger: a player casts a spell (Triggers.YouCastSpell / AnyPlayerCastsSpell / OpponentCastsSpell + type filters)")
    supported("AtTheBeginningOfAPlayersEndStep", "trigger: end step (Triggers.YourEndStep / EachEndStep)")

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
