package com.wingedsheep.tooling.coverage.bridge

/** Direct one-to-one effects: damage, life, card draw, and the common single-permanent verbs. */
internal fun BridgeBuilder.damageLifeAndCards() {
    effects(
        "SpellDealsDamage", "PermanentDealsDamage",
        // "this spell deals N damage … that can't be prevented" (Pinpoint Avalanche) and "have <permanent>
        // deal N damage to <recipient>" (Skirk Commando, Snapping Thragg, Aether Charge) — both realise as
        // a DealDamageEffect, so they carry the same DealDamage capability as the plain spell/permanent forms.
        "SpellDealsDamageCantBePrevented", "HavePermanentDealDamage",
        // "it deals N damage to <recipient>" where "it" is a card face up in exile (Longhorn
        // Sharpshooter's plot trigger) — same DealDamageEffect, attributed to the ability source.
        "ExiledCardDealsDamage",
        tag = "DealDamage",
    )
    effect("SpellDealsDistributedDamage", "DividedDamage")
    // "<creature> fights <creature>" (Savage Punch, Dragonclaw Strike, Chelonian Tackle) -> FightEffect.
    effect("Fight", "Fight")
    // "If that creature or planeswalker would die this turn, exile it instead." (Scorching Dragonfire) —
    // a death-replacement marker (CR 614) on the just-damaged target -> MarkExileOnDeathEffect. The
    // emitter renders ONLY the exact shape (a "permanent would die" replaceable event on a bound target,
    // a lone ExileItInstead replacement, until end of turn); other shapes scaffold.
    effect("CreateReplaceWouldPutIntoGraveyardUntil", "MarkExileOnDeath")

    // "where X is …" — mtgish binds the value with a CreateValueX action, then a later action spends
    // `ValueX`. Argentum has no separate "set X" step for a one-shot spell: the computed DynamicAmount
    // is inlined into the spending effect (the emitter folds CreateValueX + SpellDealsDamage(ValueX)
    // into one DealDamage with the dynamic amount). So this carries the DealDamage capability — Thunder
    // Salvo's "deals X damage … where X is 2 plus the number of other spells you've cast this turn".
    composed("CreateValueX", "DynamicAmount inlined into the spending effect", composes = listOf("DealDamage"))

    effects("DrawNumberCards", "DrawACard", tag = "DrawCards")
    effect("DrawUptoNumberCards", "DrawUpTo")
    // "The next time you would draw a card this turn, [do X] instead" (the Onslaught Words cycle); the
    // replacement action's own capability is surfaced via the `_ReplacementActionWouldDraw` discriminator.
    effect("CreateFutureReplaceWouldDraw", "ReplaceNextDrawWith")
    // "Prevent the next N damage that would be dealt to <recipient> this turn" — the damage twin of the
    // draw replacement above (Daru Healer, Samite Healer, the Circles' "to you" mode, Healing Salve, …).
    // The common prevent shape is one PreventDamageEffect; redirect / gain-life / distributed replacement
    // variants compose further primitives, so this is `composed` (mirrors `CreateReplaceWouldDealDamageUntil`).
    composed("CreateFutureReplaceWouldDealDamage", "PreventDamageShield (prevent next N damage to recipient)",
        composes = listOf("PreventDamageShield"))
    // Damage PREVENTION, split out of the would-deal-damage REPLACEMENT family by the mtgish creator:
    // CreateFuturePreventDamage / CreatePreventDamageUntil / PreventDamage are the first-class prevention
    // twins of CreateFutureReplaceWouldDealDamage / CreateReplaceWouldDealDamageUntil / ReplaceWouldDealDamage.
    // Prevention no longer carries a `PreventThatDamage` replacement-action payload (the node IS the
    // prevention), but it maps to the same PreventDamageShield engine capability, so the *verdict* is
    // identical to the pre-split replacement entries — only the discriminator changes.
    //   - CreateFuturePreventDamage → "Prevent the next N damage that would be dealt to <recipient> this turn"
    //   - PreventDamage             → immediate/static "prevent [all] damage that would be dealt to <recipient>"
    // (CreatePreventDamageUntil is registered with the other duration-scoped creators in TriggersCostsAndContinuous.)
    composed("CreateFuturePreventDamage", "PreventDamageShield (prevent next N damage to recipient)",
        composes = listOf("PreventDamageShield"))
    composed("PreventDamage", "PreventDamageShield (prevent damage to recipient)",
        composes = listOf("PreventDamageShield"))
    composed("GainLifeForEach", "GainLife + DynamicAmount", composes = listOf("GainLife"))
    effect("GainLife", "GainLife")
    effect("LoseLife", "LoseLife")

    effect("SacrificePermanent", "Sacrifice")
    // "chooses a permanent they control of each permanent type" (Liliana, Dreadhorde General's −9) —
    // the ChooseOnePerCategory pipeline step over Filters.PermanentTypes. The IR follows this action
    // with `SacrificeEachPermanent(ExceptFor(ThePermanentsChosenThisWay))`, which is the
    // exclude → sacrifice tail of the same pipeline; that trio is *deliberately left unmapped*,
    // because those three tags appear on 100+ cards in shapes this step does not cover and mapping
    // them here would score all of them as coverable. Recovering the pair (which would also unlock
    // Cataclysm / Divine Reckoning / Tragic Arrogance) is its own emitter pass.
    effect("ChooseAPermanentOfEachPermanentTypeAvailable", "ChooseOnePerCategory",
        "one pick per permanent type — chooseOnePerCategory(pool, Filters.PermanentTypes)")
    effect("CounterSpell", "Counter")
    // "Counter target spell, activated ability, or triggered ability" (Overcharged Amalgam's exploit
    // payoff, Teferi's Response). Same CounterEffect as CounterSpell, but its target dispatches at
    // resolution on whatever stack-object kind was chosen -> Effects.CounterSpellOrAbility().
    effect("CounterSpellOrAbility", "Counter")
    // Copy / retarget a spell or ability on the stack (Return the Favor). The unified copy effect
    // dispatches at resolution on the chosen stack-object kind (spell / activated / triggered ability).
    effect("CopySpellOrAbilityAndMayChooseNewTargets", "CopyTargetSpellOrAbility")
    // "Copy that spell" — bare CopySpell(Trigger_ThatSpell) on a cast trigger (Double Down). Renders to
    // CopyTargetSpell(EffectTarget.TriggeringEntity); only the triggering-spell subject is emitted (the
    // "may choose new targets" variant stays a deliberate decline above — Breeches).
    effect("CopySpell", "CopyTargetSpell")
    effect("ChangeTargetsOfSpellOrAbility", "ChangeTarget")
    effect("TakeAnExtraTurn", "TakeExtraTurn")
    // Extra phases (CR 500.8). The combat-only shape is the atom `Effects.AddCombatPhase`; the
    // combat+main shape composes it with `Effects.AddMainPhase` (Aggravated Assault).
    effect("ThereIsAnAdditionalCombatPhase", "AddCombatPhase")
    composed(
        "ThereIsAnAdditionalCombatPhaseAndAnAdditionalMainPhase",
        "Composite(Effects.AddCombatPhase + Effects.AddMainPhase)",
        composes = listOf("AddCombatPhase", "AddMainPhase")
    )
    effect("LoseTheGame", "LoseGame")
    // A turn-scoped game-wide continuous effect. Only "Damage can't be prevented this turn"
    // (DamageCantBePrevented + UntilEndOfTurn) renders -> Effects.DamageCantBePreventedThisTurn()
    // (Impractical Joke); other game effects scaffold in the emitter.
    effect("CreateGameEffect", "DamageCantBePreventedThisTurn")
    effect("Shuffle", "ShuffleLibrary")
    // Investigate (CR 701.36) — create a Clue token. `Effects.Investigate(n)` is a NAMED FACADE, not
    // its own Effect type: it returns `CreatePredefinedTokenEffect("Clue", n)`, so the leaf SerialName
    // is `CreatePredefinedToken`. The entry used to name a SerialName "Investigate" that has never
    // existed, which the registry validation correctly reported as a MISSING gap — blocking every
    // investigate card in the corpus rather than scoring the capability we do have.
    effect("Investigate", "CreatePredefinedToken", "investigate -> Effects.Investigate(n) = a Clue predefined token (CR 701.36)")
    effect("RevealHand", "RevealHand")
    effect("LookAtPlayersHand", "LookAtTargetHand")

    effects("TapPermanent", "UntapPermanent", tag = "TapUntap")
    composed("TapEachPermanent", composes = listOf("TapUntap"))
    composed("UntapEachPermanent", composes = listOf("TapUntap"))
    // "tap or untap <permanent>" — a resolution-time ModalEffect.chooseOne of Tap/Untap on the
    // same chosen permanent (Elite Interceptor's Rejoinder, Bounding Krasis, …).
    composed("TapOrUntapPermanent", composes = listOf("TapUntap"))

    // Plain ±P/T and the two dynamic-amount variants (AdjustPTX uses a ±X modifier over a game number —
    // Wirewood Pride / Feeding Frenzy / Tribal Unity; AdjustPTForEach scales a fixed base by a count —
    // Goblin Piledriver / Shaleskin Bruiser) all realise as a ModifyStatsEffect.
    effects("AdjustPT", "AdjustPTX", "AdjustPTForEach", tag = "ModifyStats")
    effect("AddAbility", "GrantKeyword")
    // "gains all activated abilities of [that permanent]" — the layer effect that copies another
    // object's activated abilities wholesale rather than granting an authored one. Quicksilver
    // Elemental, Grell Philosopher, Havengul Lich. Renders to GainAllActivatedAbilitiesOf, which
    // snapshots the donor's abilities as the ability resolves. Capability-only: each card wraps it in
    // its own activated/triggered envelope, so the emitter declines -> SCAFFOLD.
    effect("AddAbilityFromPermanentHasable", "GainAllActivatedAbilitiesOf",
        "gain all activated abilities of another object (Quicksilver Elemental)")

    // "becomes a copy of [target permanent]" (CR 707) — the _LayerEffect inside a copy continuous
    // effect. Oko, the Ringleader's combat-begin "Oko becomes a copy of up to one target creature you
    // control". Renders to EachPermanentBecomesCopyOfTarget (affected = the copying permanent). The
    // emitter declines the planeswalker/loyalty + optional-target + "except hexproof" cluster -> SCAFFOLD.
    effect("IsACopyOfPermanent", "EachPermanentBecomesCopyOfTarget")
    // "becomes a copy of [a card in exile]" (CR 707) — the copy source sits outside the battlefield.
    // Lazav, Familiar Stranger: "you may have Lazav become a copy of that [exiled] card." Renders to
    // EachPermanentBecomesCopyOfTarget(sourceFromAnyZone = true). Capability-only; the surrounding
    // crime trigger + may-exile-from-a-graveyard pipeline is card-specific -> SCAFFOLD.
    effect("IsACopyOfExiledCard", "EachPermanentBecomesCopyOfTarget")

    // "exile that spell instead of putting it into your graveyard as it resolves; it becomes plotted"
    // (CR 718) — the _ResolveAction inside a CreateSpellEffect/AsResolves on a self-cast spell. Lilah,
    // Undefeated Slickshot. Renders to MarkSpellPlotOnResolve; the surrounding cast-trigger envelope is
    // card-specific -> SCAFFOLD.
    effect("ExileResolvingSpellAndPlotIt", "MarkSpellPlotOnResolve")
}
