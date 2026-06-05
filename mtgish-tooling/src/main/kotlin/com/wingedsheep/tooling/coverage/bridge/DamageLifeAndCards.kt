package com.wingedsheep.tooling.coverage.bridge

/** Direct one-to-one effects: damage, life, card draw, and the common single-permanent verbs. */
internal fun BridgeBuilder.damageLifeAndCards() {
    effects("SpellDealsDamage", "PermanentDealsDamage", tag = "DealDamage")
    effect("SpellDealsDistributedDamage", "DividedDamage")

    effects("DrawNumberCards", "DrawACard", tag = "DrawCards")
    effect("DrawUptoNumberCards", "DrawUpTo")
    composed("GainLifeForEach", "GainLife + DynamicAmount", composes = listOf("GainLife"))
    effect("GainLife", "GainLife")
    effect("LoseLife", "LoseLife")

    effect("SacrificePermanent", "Sacrifice")
    effect("CounterSpell", "Counter")
    effect("TakeAnExtraTurn", "TakeExtraTurn")
    effect("LoseTheGame", "LoseGame")
    effect("Shuffle", "ShuffleLibrary")
    effect("RevealHand", "RevealHand")
    effect("LookAtPlayersHand", "LookAtTargetHand")

    effects("TapPermanent", "UntapPermanent", tag = "TapUntap")
    composed("TapEachPermanent", composes = listOf("TapUntap"))
    composed("UntapEachPermanent", composes = listOf("TapUntap"))

    effect("AdjustPT", "ModifyStats")
    effect("AddAbility", "GrantKeyword")
}
