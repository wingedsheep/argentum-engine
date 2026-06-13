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
    supported("WhenAPlayerCastsTheirNthSpellInATurn", "trigger: you cast your Nth spell each turn (Triggers.NthSpellCast(N, Player.You) — Rodeo Pyromancers)")
    supported("AtTheBeginningOfAPlayersUpkeep", "trigger: upkeep (Triggers.YourUpkeep / EachUpkeep / EachOpponentUpkeep)")
    supported("AtTheBeginningOfAPlayersEndStep", "trigger: end step (Triggers.YourEndStep / EachEndStep)")
    // OTJ Plot (CR 718) — "When this card becomes plotted, …" (Triggers.BecomesPlotted, Aloe Alchemist).
    supported("WhenACardBecomesPlotted", "trigger: this card becomes plotted (Triggers.BecomesPlotted)")
    supported("WhenAPermanentBecomesTheTargetOfASpellOrAbility", "trigger: becomes target (Triggers.BecomesTargetByOpponent / BecomesTarget / CreatureYouControlBecomesTargetByOpponent)")
    // OTJ Saddle (CR 702.171b) — "Whenever this creature becomes saddled for the first time each turn, …"
    // (Triggers.becomesSaddled(firstTimeEachTurn = true), Stubborn Burrowfiend).
    supported("WhenAPermanentBecomesSaddledForTheFirstTimeInATurn", "trigger: this permanent becomes saddled for the first time each turn (Triggers.becomesSaddled(firstTimeEachTurn = true))")
    // OTJ crime (CR 700.10) — "Whenever you commit a crime, …" (Triggers.YouCommitCrime, Marauding Sphinx).
    supported("WhenAPlayerCommitsACrime", "trigger: you commit a crime (Triggers.YouCommitCrime)")
    // "Whenever one or more cards leave your graveyard, …" — batching leave-graveyard trigger
    // (Triggers.CardsLeaveYourGraveyard — Owlin Historian, Attuned Hunter). You-scoped + unfiltered only.
    supported("WhenAnyNumberOfGraveyardCardsLeave", "trigger: one or more cards leave your graveyard (Triggers.CardsLeaveYourGraveyard)")
    // "When this Aura/permanent is put into a graveyard from the battlefield, …" — the self LTB-to-
    // graveyard trigger (Reach for the Sky's "draw a card"). Maps to Triggers.PutIntoGraveyardFromBattlefield;
    // only the SELF (ThisPermanent), any-player shape renders (anything else scaffolds).
    supported("WhenAPermanentIsPutIntoAPlayersGraveyard", "trigger: this permanent put into a graveyard from the battlefield (Triggers.PutIntoGraveyardFromBattlefield)")

    // Intervening-if conditions (CR 603.4) gating a TriggerI, plus the Mount "while saddled" gate. The
    // emitter renders the recognised shapes to `triggerCondition = Conditions.*`; an unrenderable
    // condition still scaffolds, so these are accepted as supported vocabulary (the emitter is the gate).
    supported("PermanentPassesFilter", "condition: a permanent matches a filter (e.g. ThisPermanent IsSaddled -> Conditions.SourceIsSaddled)")
    supported("PlayerPassesFilter", "condition: a player matches a filter (e.g. You HasntCastASpellThisTurn)")
    supported("IsSaddled", "predicate: this permanent is saddled (CR 702.171b)")
    // "during your turn" — IsPlayersTurn(You) -> Conditions.IsYourTurn, the gate on Overzealous Muscle's
    // "Whenever you commit a crime during your turn, …". Renders as a triggerCondition when it wraps a
    // trigger (the emitter unwraps the If-gated trigger); any non-You scope declines -> SCAFFOLD.
    supported("IsPlayersTurn", "condition: it's a given player's turn (You -> Conditions.IsYourTurn)")
    supported("HasntCastASpellThisTurn", "predicate: player hasn't cast a (filtered) spell this turn")
    // "you've cast another spell this turn" -> Conditions.YouCastSpellsThisTurn(atLeast = 2) at
    // resolution (the resolving spell is already recorded). The emitter renders the "Other ThisSpell"
    // shape; a non-You / unfiltered-other mismatch declines -> SCAFFOLD.
    supported("CastASpellThisTurn", "predicate: player has cast a (filtered) spell this turn (YouCastSpellsThisTurn)")
    // OTJ crime (CR Outlaws of Thunder Junction) — "you've committed a crime this turn" ->
    // Conditions.YouCommittedCrimeThisTurn, gating a cost reduction or a resolution-time effect.
    supported("CommitedACrimeThisTurn", "predicate: player has committed a crime this turn (YouCommittedCrimeThisTurn)")
    // "if you do" / "if [the cost] was paid" — the IfYouDoEffect linkage after a MayCost(DiscardACard)
    // loot, rendered by the emitter as MayEffect(IfYouDoEffect(...)). Universal condition vocabulary.
    supported("CostWasPaid", "condition: the optional cost was paid (IfYouDoEffect linkage)")
    supported("WasCastFromTheirHand", "predicate: spell cast from the player's hand (fromZone = HAND)")
    // Source-relative Mount/Vehicle payoff filter: "a creature that crewed/saddled it this turn"
    // (Giant Beaver) -> GameObjectFilter.Creature.crewedOrSaddledSourceThisTurn().
    supported("SaddledPermanentThisTurn", "filter: a creature that saddled this permanent this turn (crewedOrSaddledSourceThisTurn)")
    supported("CrewedPermanentThisTurn", "filter: a creature that crewed this permanent this turn (crewedOrSaddledSourceThisTurn)")

    // Costs.
    supported("PayMana", "cost: pay mana (universal)")
    supported("SacrificeAPermanent", "cost: sacrifice")
    supported("SacrificeNumberPermanents", "cost: sacrifice N")
    // "Pay N life" as an activation cost -> Costs.PayLife(n). The emitter renders fixed-integer amounts
    // (abilityCostDsl); non-integer amounts ({X}, life-total halves, …) are declined -> SCAFFOLD.
    supported("PayLife", "cost: pay life")
    // Waterbend {N} (Avatar: The Last Airbender, CR) — a generic-mana cost where each generic may be
    // paid by tapping an untapped artifact/creature you control. On activated abilities this maps to
    // `activatedAbility { cost = Costs.Mana("{N}"); hasWaterbend = true }`. The emitter renders the
    // fixed-generic shape; the X-carrying variants (WaterbendX / WaterbendCustomX) are left unsupported
    // -> blocked (the engine doesn't model an X waterbend cost yet).
    supported("Waterbend", "cost: waterbend {N} (tap artifacts/creatures, each pays {1} generic)")
    composed("DiscardACardOfType", "cost: discard filtered")

    // Duration-scoped continuous trigger / replacement creators.
    composed("CreateReplaceWouldDealDamageUntil", "PreventDamageShield / RedirectNextDamage", composes = listOf("PreventDamageShield"))
    // Duration-scoped PREVENTION twin of CreateReplaceWouldDealDamageUntil (mtgish prevention/replacement
    // split). "Prevent all (combat) damage that would be dealt … this turn" — Deep Wood, Leery Fogbeast,
    // Maze of Shadows. Same PreventDamageShield capability, minus the `PreventThatDamage` replacement payload.
    composed("CreatePreventDamageUntil", "PreventDamageShield (duration-scoped prevention)", composes = listOf("PreventDamageShield"))
    composed("CreateTriggerUntil", "CreateGlobalTriggeredAbility (duration)", composes = listOf("CreateGlobalTriggeredAbility"))
    composed("CreateFutureTrigger", "CreateDelayedTrigger", composes = listOf("CreateDelayedTrigger"))
}
