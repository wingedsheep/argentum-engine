package com.wingedsheep.tooling.coverage.bridge

/** Mana, counters, control, and combat/untap-state effects. Mostly the "universal" verbs that Portal
 *  never exercised but every later set does — each line lifts recall on every set at once. */
internal fun BridgeBuilder.manaCountersAndState() {
    // AddMana's exact Effect depends on the produced symbol — colorless ({C}) serialises as
    // AddColorlessMana, "any color" as AddManaOfChoice, a fixed colour as AddMana — and the capability
    // scorer can't see the symbol arg, so name the whole mana family the action can lower to.
    composed("AddMana", UNIVERSAL, composes = listOf("AddMana", "AddColorlessMana", "AddManaOfChoice"))
    composed("AddManaRepeated", UNIVERSAL, composes = listOf("AddMana", "AddColorlessMana", "AddManaOfChoice"))
    // "Add {U}/{C}/any color. Spend this mana only to cast an instant or sorcery spell."
    // (Hydro-Channeler, Vodalian Arcanist): same mana family as AddMana, carrying a
    // ManaRestriction.InstantOrSorceryOnly — the emitter renders only that one CanOnlySpendOnSpells
    // shape and scaffolds any other modifier.
    composed("AddManaWithModifiers", UNIVERSAL, composes = listOf("AddMana", "AddColorlessMana", "AddManaOfChoice"))
    effect("AddColorlessMana", "AddColorlessMana", UNIVERSAL)

    // CreateTokens lowers to either the generic CreateToken (creature tokens) or a predefined-token
    // facade (Treasure -> CreatePredefinedToken); the capability scorer can't see which from the tag
    // alone, so name both — mirroring the AddMana family above.
    composed("CreateTokens", UNIVERSAL, composes = listOf("CreateToken", "CreatePredefinedToken"))
    // "becomes the creature type of your choice" — a ChooseACreatureType + an AddCreatureTypeVariable
    // layer effect collapse to one BecomeCreatureTypeEffect (Mistform cycle, Imagecrafter).
    effect("AddCreatureTypeVariable", "BecomeCreatureType", UNIVERSAL)
    effects("PutACounterOfTypeOnPermanent", "PutNumberCountersOfTypeOnPermanent", tag = "AddCounters", note = UNIVERSAL)

    // Earthbend N (TLA keyword action): target land becomes a 0/0 creature-land with haste, gets N
    // +1/+1 counters, and gains "when it dies or is exiled, return it to the battlefield tapped".
    // Composed wholesale by Effects.Earthbend (no Keyword.EARTHBEND) — animate + grant haste + add
    // counters + grant the return self-trigger (whose body is two zone-gated MoveToZone moves).
    composed("Earthbend", "Effects.Earthbend: AnimateLand + GrantKeyword(haste) + AddCounters + GrantTriggeredAbility(return)",
        composes = listOf("AnimateLand", "GrantKeyword", "AddCounters", "GrantTriggeredAbility", "MoveToZone"))

    effect("RegeneratePermanent", "Regenerate", UNIVERSAL)
    effects("GainControlOfPermanent", "GainControlOfPermanentUntil", tag = "GainControl", note = UNIVERSAL)
    effect("RemoveCreatureFromCombat", "RemoveFromCombat", UNIVERSAL)
    // Ydwen Efreet's "remove from combat and creatures it solely blocked become unblocked" — the same
    // RemoveFromCombat effect (the emitter sets its unblockSoleBlockedAttackers flag).
    effect("RemoveCreatureFromCombatAndUnblockBlockers", "RemoveFromCombat", UNIVERSAL)
    // "Flip a coin. If you lose the flip, …" — a generic control-flow effect (Ydwen Efreet); its nested
    // on-lose actions are resolved by the recursive tree walk.
    effect("FlipACoin_OnLose", "FlipCoin", "flip a coin, on-lose actions nested")
    // Goad (CR 701.15): targeted GoadCreature renders; mass GoadEachCreature carries the same
    // capability but its group filter often scaffolds in the emitter.
    effects("GoadCreature", "GoadEachCreature", tag = "Goad", note = UNIVERSAL)

    effect("EachPermanentDoesntUntapDuringControllersNextUntap", "SkipUntap",
        "Exhaustion: target player's creatures+lands don't untap next untap step")
    effect("SkipAllCombatPhasesTheirNextTurn", "SkipCombatPhases",
        "False Peace: target skips all combat phases of their next turn")

    // "As ~ enters the battlefield" replacement actions (nested under the AsPermanentEnters envelope).
    // The engine realises each via a dedicated ReplacementEffect — EntersTapped, and a +1/+1 counter via
    // EntersWithCounters. Those SerialNames live in scripting/ReplacementEffect.kt (outside the scanned
    // effects/ dir, so not registry-validated), hence `composed` with the realising effect noted. Counter
    // variants beyond a single fixed +1/+1 (EntersWithNumberCounters, EntersWithACounterOfChoice, …) stay
    // unmapped so they keep blocking until their scoping/rendering is verified.
    composed("EntersTapped", "enters tapped (EntersTapped replacement)")
    composed("EntersWithACounter", "enters with one +1/+1 counter (EntersWithCounters replacement)")
}
