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
    // Layer effects carried by an enters-with rider (Ghost Vacuum's "each of them is a 1/1 Spirit in
    // addition to its other types"): set base power/toughness (Layer 7b) and add a creature subtype
    // (Layer 4). Both map to standing SDK effects with Duration.Permanent. Capability-only — the
    // EntersWithLayerEffect/PutEachExiledCardOntoTheBattlefield host shape is card-specific, so the
    // emitter declines -> SCAFFOLD. (A *plain-keyword* AddAbility under EntersWithLayerEffect is now
    // expressible as the EntersWithKeywords replacement — the INV/DOM kicker "enters with … and with
    // trample" cycle — but every corpus card with this tag also carries SetPT / activated-ability /
    // until-EOT riders the shape doesn't cover, so there is no calibrated card to render and the
    // emitter keeps declining.)
    effect("SetPT", "SetBasePowerToughness", "set base power/toughness via an enters-with layer effect (Ghost Vacuum)")
    effect("AddCreatureType", "AddCreatureType", "add a creature subtype in addition to other types (Ghost Vacuum)")
    // The mtgish IR routes every put-counter action through a `PutCounters` envelope whose nested
    // `_PutCountersAction` variants carry the real shape (the old top-level `PutACounterOfType…` /
    // `PutNumberCountersOfType…` actions are gone — `_PutCountersAction` is a tracked discriminator in
    // CAPABILITY_DISCRIMINATORS). Score the envelope as structural and map each variant we can express;
    // an unmapped variant (a distribute-among-any-number, duplicate-each-kind, …) keeps its card blocking.
    envelope("PutCounters", "put-counter envelope — the real action is the nested _PutCountersAction variant")
    // "Put a / N +1/+1 (or keyword) counter(s) on <permanent>" -> AddCounters / AddDynamicCounters.
    effects("ACounterOfTypeOnPermanent", "NumberCountersOfTypeOnPermanent", tag = "AddCounters", note = UNIVERSAL)
    // "Put up to N counters of a type on <permanent>" (Esper Terra's lore chapters) -> AddCountersUpTo,
    // the additive/player-chosen mirror of the RemoveAnyNumberOfCounters family. Capability-only: the
    // put-up-to-N shape is a value-selection prompt the emitter declines to render (-> SCAFFOLD), per
    // the module's "chosen values" policy.
    effect("UptoNumberCountersOfTypeOnPermanent", "AddCountersUpTo",
        "put up to N counters of a type on a permanent — player chooses 0..N (Esper Terra)")
    // "Put a / N counter(s) on each <filter>" — the mass form, ForEachInGroup(AddCounters) over the
    // recovered group filter or the just-created tokens (Bounding Felidar, Germination Practicum).
    composed("ACounterOfTypeOnEachPermanent", "ForEachInGroup(AddCounters) over a group filter",
        composes = listOf("AddCounters"))
    composed("NumberCountersOfTypeOnEachPermanent", "ForEachInGroup(AddCounters/AddDynamicCounters) over a group filter or created tokens",
        composes = listOf("AddCounters"))
    // "Double the number of <counter> counters on <permanent> / each <filter>" -> DoubleCounters
    // (Ornery Tumblewagg's saddled attack; Omnivorous Flytrap's each-of-those-creatures tail).
    effects("DoubleCountersOfTypeOnPermanent", "DoubleCountersOfTypeOnEachPermanent", tag = "DoubleCounters",
        note = "double the +1/+1 counters (Ornery Tumblewagg, Omnivorous Flytrap)")
    // "Double the number of each kind of counter on <permanent(s)>" -> DoubleAllCounters
    // (Zimone, Paradox Sculptor; Vorel of the Hull Clade).
    effects("DoubleAllCountersOnPermanent", "DoubleAllCountersOnEachPermanent", tag = "DoubleCounters",
        note = "double every kind of counter (Zimone, Paradox Sculptor)")
    // "Distribute N +1/+1 counters among one or two target creatures" — the counter analogue of
    // distributed damage; the `TargetedDistributed` envelope's `DistributeNumberAmongTargets`
    // distribution drives a DistributeDecision over the chosen targets, then this action places the
    // counters (Omnivorous Flytrap, Abzan Charm shape). The emitter declines whole-card rendering of
    // the distributed *triggered* shape (the distribution/double-on-each tail is card-specific), so
    // this is capability-only -> SCAFFOLD.
    effect("DistributedCounters", "DistributeCountersAmongTargets",
        "distribute N +1/+1 counters among one or two target creatures (Omnivorous Flytrap)")
    // "Put those counters on <permanent>" — a just-died permanent's counters move to the target
    // (Scolding Administrator's dies trigger). mtgish models it as DuplicateCountersOfPermanentOnPermanent
    // (source, dest); for a DYING source it lowers to MoveAllLastKnownCounters, which moves every counter
    // kind off the dead source (Essence Channeler shape), not just +1/+1. A living source scaffolds.
    effect("DuplicateCountersOfPermanentOnPermanent", "MoveAllLastKnownCounters",
        "move a dying permanent's counters to the target (Scolding Administrator)")
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

    // Airbend (TLA keyword action): "Exile target permanent. While it's exiled, its owner may cast it
    // for {2} rather than its mana cost." Composed wholesale by Effects.Airbend (no Keyword.AIRBEND) —
    // gather the chosen target(s), move them to exile, then grant the owner a may-play permission with
    // a fixed {2} alternative mana cost. Target-agnostic: the card's TargetRequirement supplies the
    // shape ("up to one", "any number of", "another", "you control"). The IR `AirbendPermanent` action
    // renders as a bare `Effects.Airbend()`; the surrounding Targeted envelope declares the target.
    composed("AirbendPermanent", "Effects.Airbend: GatherCards(ChosenTargets) + MoveCollection(EXILE) + GrantMayPlayFromExile(ownerControls, fixedAlternativeManaCost {2})",
        composes = listOf("GatherCards", "MoveCollection", "GrantMayPlayFromExile"))

    // Airbend the spell-on-stack form (Aang, Swift Savior — "airbend … target creature or spell"):
    // *exile* the spell from the stack to its owner's exile and grant the owner the same fixed-{2}
    // recast, via Effects.ExileTargetSpell(fixedAlternativeManaCost). This is NOT a counter — airbend
    // says "exile it", so it bypasses can't-be-countered and fires no "spell was countered" trigger
    // (the Aven Interrupter exileSpell primitive, reusing PlayWithFixedAlternativeManaCostComponent).
    // The full card pairs this with a cross-zone "creature or spell" target + a TargetIsSpellOnStack
    // branch; the emitter leaves the combined shape to scaffold, but the capability is supported, so
    // score it coverable.
    composed("AirbendSpell", "Effects.ExileTargetSpell(fixedAlternativeManaCost {2}) — exile the spell from the stack to owner's exile (not a counter), owner may recast for {2}",
        composes = listOf("ExileSpell", "GrantMayPlayFromExile"))

    effect("RegeneratePermanent", "Regenerate", UNIVERSAL)
    // "<permanent> becomes prepared" (Secrets of Strixhaven — Leech Collector's trigger). Maps to the
    // BecomePrepared effect; the enters-prepared flavour is the PREPARED keyword + PREPARE layout.
    effect("PreparePermanent", "BecomePrepared", "a PREPARE-layout permanent becomes prepared (Leech Collector)")
    // "<permanent> becomes unprepared" (Secrets of Strixhaven — Biblioplex Tomekeeper). The inverse of
    // BecomePrepared: maps to the Unprepare effect (strips prepared status + its exile prepare-spell copy).
    effect("UnpreparePermanent", "Unprepare", "a prepared permanent becomes unprepared (Biblioplex Tomekeeper)")
    // "Reveal target face-down permanent" (Hauntwoods Shrieker) — make the hidden card public (CR
    // 708.2). Informational only; the follow-up "if it's a creature card, you may turn it face up"
    // composes a TargetIsCreatureCard gate + MayEffect(TurnFaceUp).
    effect("RevealFaceDownPermanent", "RevealFaceDownPermanent", "reveal a face-down permanent (Hauntwoods Shrieker)")
    // "turn it face up" — the free, no-cost flip of a revealed creature card (Hauntwoods Shrieker),
    // distinct from paying a morph/manifest turn-up cost. Maps to the TurnFaceUp effect.
    effect("TurnPermanentFaceUp", "TurnFaceUp", "turn a face-down permanent face up for free (Hauntwoods Shrieker)")
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
