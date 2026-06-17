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
    // "Add N mana of any one color. Spend this mana only to cast Mount or Vehicle spells." (Intrepid
    // Stablemaster): AddManaWithModifiers with a repeat count, carrying a ManaRestriction (instant/sorcery
    // or a subtype-spend SubtypeSpellsOnly). Same mana family; the emitter renders only the fixed-count,
    // recoverable-restriction shape and scaffolds anything else.
    composed("AddManaRepeatedWithModifiers", UNIVERSAL, composes = listOf("AddMana", "AddColorlessMana", "AddManaOfChoice"))
    effect("AddColorlessMana", "AddColorlessMana", UNIVERSAL)

    // CreateTokens lowers to either the generic CreateToken (creature tokens) or a predefined-token
    // facade (Treasure -> CreatePredefinedToken); the capability scorer can't see which from the tag
    // alone, so name both — mirroring the AddMana family above.
    composed("CreateTokens", UNIVERSAL, composes = listOf("CreateToken", "CreatePredefinedToken"))
    // CreateTokensWithFlags is CreateTokens carrying enter-state flags (e.g. EntersTapped). The flag rides
    // on the same predefined/creature token facade (CreateTreasure(tapped = true), CreateToken(tapped = …)),
    // so it composes the same primitives — Goldvein Hydra's "tapped Treasure tokens equal to its power".
    composed("CreateTokensWithFlags", UNIVERSAL, composes = listOf("CreateToken", "CreatePredefinedToken"))
    // "becomes the creature type of your choice" — a ChooseACreatureType + an AddCreatureTypeVariable
    // layer effect collapse to one BecomeCreatureTypeEffect (Mistform cycle, Imagecrafter).
    effect("AddCreatureTypeVariable", "BecomeCreatureType", UNIVERSAL)
    effects("PutACounterOfTypeOnPermanent", "PutNumberCountersOfTypeOnPermanent", tag = "AddCounters", note = UNIVERSAL)
    // "put those counters on <permanent>" — the counters a just-died permanent had move to the target
    // (Scolding Administrator's dies trigger). Maps to MoveAllLastKnownCounters, which moves every
    // counter kind off the dying source (Essence Channeler shape), not just +1/+1.
    effect("PutFormerCountersOnPermanent", "MoveAllLastKnownCounters", "move a dying permanent's counters to the target (Scolding Administrator)")
    // "Put a counter on each <filter>" — the mass form, rendered as ForEachInGroup(AddCounters) over the
    // recovered group filter (Bounding Felidar's "each other creature you control").
    composed("PutACounterOfTypeOnEachPermanent", "ForEachInGroup(AddCounters) over a group filter",
        composes = listOf("AddCounters"))
    // "Until end of turn, if you would put one or more +1/+1 counters on a creature you control, put
    // that many plus N +1/+1 counters on it instead." (Prairie Dog) — the duration-/controller-scoped
    // analogue of Hardened Scales' static ModifyCounterPlacement, lowered to
    // Effects.GrantCounterPlacementModifier. The emitter renders only the fully-defaulted +1/+1
    // creature-you-control until-end-of-turn shape and scaffolds any deviation.
    effect("CreateReplaceWouldPutCountersUntil", "GrantCounterPlacementModifier",
        "temporary counter-placement modifier (Hardened Scales as an activated/duration-scoped effect)")

    // Earthbend N (TLA keyword action): target land becomes a 0/0 creature-land with haste, gets N
    // +1/+1 counters, and gains "when it dies or is exiled, return it to the battlefield tapped".
    // Composed wholesale by Effects.Earthbend (no Keyword.EARTHBEND) — animate + grant haste + add
    // counters + grant the return self-trigger (whose body is two zone-gated MoveToZone moves).
    composed("Earthbend", "Effects.Earthbend: AnimateLand + GrantKeyword(haste) + AddCounters + GrantTriggeredAbility(return)",
        composes = listOf("AnimateLand", "GrantKeyword", "AddCounters", "GrantTriggeredAbility", "MoveToZone"))

    effect("RegeneratePermanent", "Regenerate", UNIVERSAL)
    // "<permanent> becomes prepared" (Secrets of Strixhaven — Leech Collector's trigger). Maps to the
    // BecomePrepared effect; the enters-prepared flavour is the PREPARED keyword + PREPARE layout.
    effect("PreparePermanent", "BecomePrepared", "a PREPARE-layout permanent becomes prepared (Leech Collector)")
    // "attach it to target …" — an Equipment/Aura attaching ITSELF (the source) to a chosen permanent
    // (Thunder Lasso's ETB "attach it to target creature you control"). The engine idiom is
    // AttachEquipment, which always attaches the source. The emitter only renders the self-attach shape
    // (args[0] a self-ref) and declines anything else -> SCAFFOLD.
    effect("AttachPermanentToPermanent", "AttachEquipment", "self-attach an Equipment/Aura to a chosen permanent")
    effects("GainControlOfPermanent", "GainControlOfPermanentUntil", tag = "GainControl", note = UNIVERSAL)
    // "Exchange control of two target permanents" (Shifting Grift, Chromeshell Crab) — the two-target
    // ExchangeControlEffect, modeled as a pair of permanent Layer.CONTROL floating effects.
    effect("ExchangeControl", "ExchangeControl", "exchange control of two target permanents")
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
    // "enters with a +1/+1 counter" or a keyword counter (e.g. a lifelink counter, Dust Animus) ->
    // EntersWithCounters (default PlusOnePlusOne, or a Named CounterTypeFilter for keyword counters).
    composed("EntersWithACounter", "enters with one counter — +1/+1 or a keyword counter (EntersWithCounters replacement)")
    // "enters with N +1/+1 counters" — a fixed count renders EntersWithCounters(count = N), a dynamic
    // count (Stag Beetle) renders EntersWithDynamicCounters.
    composed("EntersWithNumberCounters", "enters with N +1/+1 counters (EntersWithCounters / EntersWithDynamicCounters)")

    // Prepare (Secrets of Strixhaven, CR 702.x). "This creature enters prepared." — the nested
    // `EntersPrepared` of the AsPermanentEnters envelope. The engine encodes "enters prepared" as the
    // PREPARED keyword on a CardLayout.PREPARE card (the stack resolver creates the exiled prepare-spell
    // copy on entry), so the emitter renders `keywords(Keyword.PREPARED)` + a `prepare("…") { spell { … } }`
    // block and skips this rule. Map the capability to the PREPARED keyword the golden carries.
    keyword("EntersPrepared", "PREPARED", "enters prepared (keywords(Keyword.PREPARED) on a PREPARE-layout card)")
    // "… becomes prepared" mid-game (Joined Researchers' end-step trigger) is registered above as
    // effect("PreparePermanent", "BecomePrepared", …); no PREPARED keyword in that case.
}
