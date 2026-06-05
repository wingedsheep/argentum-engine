package com.wingedsheep.tooling.coverage.bridge

/** Mana, counters, control, and combat/untap-state effects. Mostly the "universal" verbs that Portal
 *  never exercised but every later set does — each line lifts recall on every set at once. */
internal fun BridgeBuilder.manaCountersAndState() {
    effects("AddMana", "AddManaRepeated", tag = "AddMana", note = UNIVERSAL)
    effect("AddColorlessMana", "AddColorlessMana", UNIVERSAL)

    effect("CreateTokens", "CreateToken", UNIVERSAL)
    effects("PutACounterOfTypeOnPermanent", "PutNumberCountersOfTypeOnPermanent", tag = "AddCounters", note = UNIVERSAL)

    effect("RegeneratePermanent", "Regenerate", UNIVERSAL)
    effects("GainControlOfPermanent", "GainControlOfPermanentUntil", tag = "GainControl", note = UNIVERSAL)
    effect("RemoveCreatureFromCombat", "RemoveFromCombat", UNIVERSAL)

    effect("EachPermanentDoesntUntapDuringControllersNextUntap", "SkipUntap",
        "Exhaustion: target player's creatures+lands don't untap next untap step")
    effect("SkipAllCombatPhasesTheirNextTurn", "SkipCombatPhases",
        "False Peace: target skips all combat phases of their next turn")
}
