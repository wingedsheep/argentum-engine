package com.wingedsheep.tooling.coverage.bridge

/** Triggered-ability conditions and costs (accepted as [supported] pending a `Triggers.*`/`Costs.*`
 *  facade scan), plus the duration-scoped trigger/replacement creators (composed from primitives). */
internal fun BridgeBuilder.triggersCostsAndContinuous() {
    // Triggers — validated by a Triggers.* facade scan in a later phase.
    supported("WhenAPermanentEntersTheBattlefield", "trigger: ETB (Triggers.* scan validates in P1)")
    // Batched ETB — "whenever one or more permanents [matching a filter] enter" fires once per event
    // batch (Triggers.OneOrMorePermanentsEnter / OneOrMoreOpponentPermanentsEnter; controller scope
    // you-control by default or .opponentControls(); the matching batch members are exposed to the
    // payoff via the trigger.captured pipeline collection — Kambal, Profiteering Mayor).
    supported("WhenAnyNumberOfPermanentsEnterTheBattlefield", "trigger: one or more permanents enter, batched (Triggers.OneOrMorePermanentsEnter / OneOrMoreOpponentPermanentsEnter)")
    supported("WhenAPermanentDies", "trigger: dies")
    // "Whenever one or more <counter> counters are put on this creature, …" — the counters-placed
    // trigger (CountersPlacedEvent bound to SELF; Exemplar of Light, Pensive Professor). The emitter
    // renders only the SELF subject + a nameable counter type; a non-self subject or unnameable counter
    // declines -> SCAFFOLD.
    supported("WhenAnyNumberOfCountersOfTypeArePutOnAPermanent", "trigger: one or more counters of a type put on this permanent (CountersPlacedEvent, SELF)")
    supported("WhenACreatureAttacks", "trigger: attacks")
    // "Whenever this creature attacks a player, …" — the attacks-a-player trigger, gated on the declared
    // defender being a player (not a planeswalker or battle; CR 508.1 + Kaalia of the Vast's 2024-06-07
    // ruling). Maps to AttackPredicate.DefenderIsPlayer / Triggers.AttacksAnOpponent for the SELF subject
    // over a bare "a player" (Opponent / AnyPlayer) scope; the emitter renders that shape and declines a
    // non-self attacker or a constrained player scope (attacks-you / life-total / controls-N).
    supported("WhenACreatureAttacksAPlayer", "trigger: this creature attacks a player (Triggers.AttacksAnOpponent)")
    // "Whenever this creature attacks for the first time each turn, …" — SELF attack trigger gated on
    // the per-turn attacker set (AttackPredicate.FirstTimeEachTurn / Triggers.AttacksFirstTimeEachTurn,
    // Fear of Missing Out). Fires once on the first attack and not on a later combat phase the same turn.
    supported("WhenACreatureAttacksForTheFirstTimeEachTurn", "trigger: this creature attacks for the first time each turn (Triggers.AttacksFirstTimeEachTurn)")
    // "Whenever you attack with one or more creatures [matching a filter]" — the batched declare-attackers
    // trigger that fires once per combat when at least one attacker matches (Triggers.YouAttackWithFilter,
    // Jolene Plundering Pugilist's "with power 4 or greater"). You scope only; the emitter recovers the
    // attacker filter exactly or declines -> SCAFFOLD.
    supported("WhenAPlayerAttacksWithAnyNumberOfCreatures", "trigger: you attack with one or more creatures matching a filter (Triggers.YouAttackWithFilter)")
    supported("WhenACreatureBlocks", "trigger: blocks (Ydwen Efreet)")
    supported("WhenAPermanentBecomesTapped", "trigger: this permanent becomes tapped (Triggers.BecomesTapped — Wylie Duke, Atiin Hero)")
    supported("WhenACreatureDealsCombatDamageToAPlayer", "trigger: combat damage to player")
    // "Whenever you sacrifice a/another [filter] …" — the batched sacrifice trigger that fires when a
    // matching permanent you control is sacrificed (Triggers.YouSacrificeOneOrMore; the first matching
    // sacrificed permanent is bound as the triggering entity so "its mana value / power" reads from LKI —
    // Rakdos, the Muscle). You-scope; the emitter recovers the filter or declines -> SCAFFOLD.
    supported("WhenAPlayerSacrificesAPermanent", "trigger: you sacrifice one or more matching permanents (Triggers.YouSacrificeOneOrMore)")
    // "At the beginning of combat on your turn, …" (Triggers.BeginCombat — already scoped to your turn).
    // Oko, the Ringleader's combat-begin copy. You-turn scope only; the emitter renders BeginCombat when
    // the payoff is renderable (else SCAFFOLD).
    supported("AtTheBeginningOfCombatDuringAPlayersTurn", "trigger: beginning of combat on your turn (Triggers.BeginCombat)")
    supported("WhenAPlayerCastsASpell", "trigger: a player casts a spell (Triggers.YouCastSpell / AnyPlayerCastsSpell / OpponentCastsSpell + type filters)")
    supported("WhenAPlayerCastsTheirNthSpellInATurn", "trigger: you cast your Nth spell each turn (Triggers.NthSpellCast(N, Player.You) — Rodeo Pyromancers)")
    // Breeches, the Blastmaker stays a DELIBERATE DECLINE — its second-spell payoff (NthSpellCast above)
    // is a `MayCost(sacrifice an artifact)`-gated `FlipACoin_OnWinAndLose` that sets up two reflexive
    // (delayed) triggers — win: `CopySpellAndMayChooseNewTargets`, lose: deal that spell's mana value to
    // any target. The win/lose coin branch + copy-spell-with-new-targets cluster is exactly the
    // value-selection shape the module creator's note (mtgish-tooling/CLAUDE.md) says to leave BLOCKED
    // rather than risk a confidently-wrong render. FlipACoin_OnWinAndLose / CopySpellAndMayChooseNewTargets
    // stay unmapped on purpose, so Breeches stays BLOCKED; the hand-authored card + its scenario test
    // (BreechesTheBlastmakerTest) is the ground truth.
    //
    // ReflexiveTrigger itself is capability vocabulary — the "you may [pay a cost]. When you do, [reflexive
    // effect targeting]" idiom maps to ReflexiveTriggerEffect. The emitter renders ONLY the exact shape it
    // can reproduce (Boilerbilges Ripper: MayCost(sacrifice another creature or enchantment) +
    // If(CostWasPaid)[reflexive: this permanent deals N to any target]); any other reflexive shape (the
    // Breeches coin cluster) still scaffolds/blocks via its sibling tags.
    supported("ReflexiveTrigger", "reflexive 'when you do' trigger (ReflexiveTriggerEffect — Boilerbilges Ripper); other shapes scaffold")
    supported("AtTheBeginningOfAPlayersUpkeep", "trigger: upkeep (Triggers.YourUpkeep / EachUpkeep / EachOpponentUpkeep)")
    supported("AtTheBeginningOfAPlayersEndStep", "trigger: end step (Triggers.YourEndStep / EachEndStep)")
    supported("AtTheBeginningOfAPlayersSecondMainPhase", "trigger: your second (postcombat) main phase (Triggers.YourPostcombatMain — Survival ability word)")
    supported("WhenAPlayerGainsLife", "trigger: you gain life (Triggers.YouGainLife — Pest Mascot, Essence Channeler)")
    supported("WhenAPlayerGainsLifeForTheFirstTimeEachTurn", "trigger: you gain life for the first time each turn (Triggers.YouGainLifeFirstTimeEachTurn — Leech Collector)")
    supported("WhenAPlayerGainsControlOfAPermanentFromAPlayer", "trigger: an opponent gains control of a permanent from you (Triggers.OpponentGainsControlOfYourPermanent — Zidane, Tantalus Thief)")
    supported("WhenAPlayerTurnsAPermanentFaceUp", "trigger: you turn a permanent face up (Triggers.CreatureTurnedFaceUp — Growing Dread)")
    supported("WhenAPermanentIsTurnedFaceUp", "trigger: this or another permanent you control is turned face up (Triggers.CreatureTurnedFaceUp — Cryptid Inspector)")
    // OTJ Plot (CR 718) — "When this card becomes plotted, …" (Triggers.BecomesPlotted, Aloe Alchemist).
    supported("WhenACardBecomesPlotted", "trigger: this card becomes plotted (Triggers.BecomesPlotted)")
    supported("WhenAPermanentBecomesTheTargetOfASpellOrAbility", "trigger: becomes target (Triggers.BecomesTargetByOpponent / BecomesTarget / CreatureYouControlBecomesTargetByOpponent)")
    // OTJ Saddle (CR 702.171b) — "Whenever this creature becomes saddled for the first time each turn, …"
    // (Triggers.becomesSaddled(firstTimeEachTurn = true), Stubborn Burrowfiend).
    supported("WhenAPermanentBecomesSaddledForTheFirstTimeInATurn", "trigger: this permanent becomes saddled for the first time each turn (Triggers.becomesSaddled(firstTimeEachTurn = true))")
    // "Whenever this Equipment/Aura becomes attached to a permanent, …" (CR 603.2e) — the new
    // Triggers.becomesAttached. Capability-only: the renderable payoffs (Assimilation Aegis'
    // copy-of-linked-exile, Eriette's gain-control "for as long as that Aura is attached") carry the
    // attach-relative duration and exile linkage the emitter does NOT reconstruct, so it declines to
    // SCAFFOLD per the creator's note (chosen/inherited-value shapes). Hand-authored card is ground truth.
    supported("WhenAPermanentBecomesAttached", "trigger: an Aura/Equipment becomes attached (Triggers.becomesAttached) — capability only, emitter scaffolds")
    // Exploit payoff (CR 702.110b) — "when this creature exploits a creature, …". NOT an engine gap:
    // the shipped `card { exploit(onExploit, onExploitTargets) }` helper composes the whole mechanic
    // from primitives (EXPLOIT keyword + an ETB ReflexiveTriggerEffect whose action sacrifices a
    // creature and fires an observable EventPattern.ExploitedEvent), baking the self-payoff into the
    // reflexive so it survives self-sacrifice. Broadcast "whenever a creature you control exploits"
    // watchers use EventPattern.ExploitedEvent directly (Skull Skaab). Capability-only: fusing the
    // paired Exploit-keyword rule + this trigger back into one exploit() call (recovering owner /
    // each-opponent / targeted payoff shapes) is exactly the lossy render the fidelity policy declines,
    // so the emitter leaves it at SCAFFOLD (like WhenAPermanentBecomesAttached). The Exploit keyword
    // rule itself already scaffolds via `// STRUCTURE needs human wiring: Exploit`. See the Exploit
    // keyword + ExploitedEvent entries in card-sdk-language-reference.md; hand-authored cards
    // (VOW Diver Skaab, Graf Reaver, Mindleech Ghoul, Overcharged Amalgam, Repository Skaab,
    // Rot-Tide Gargantua) are ground truth.
    supported("WhenAPermanentExploitsAPermanent", "trigger: this creature exploits a creature (exploit payoff — card { exploit(onExploit, onExploitTargets) }) — capability only, emitter scaffolds")
    // Training payoff (CR 702.149c) — "whenever this creature trains, …". Like the exploit payoff above,
    // NOT an engine gap: the shipped `training()` helper adds the TRAINING keyword + the attack trigger
    // whose +1/+1 counter placement emits the parameterless `EventPattern.TrainedEvent` (fired only when
    // the counter actually lands), and this trigger keys on that event via `Triggers.trains()` (SELF
    // binding). Savior of Ollenbock's "whenever this creature trains, exile up to one …" is the payoff.
    // Capability-only: the emitter would have to fuse the paired Training-keyword rule + this trigger and
    // recover the exile-until-leaves / cross-zone-union payoff — exactly the lossy render the fidelity
    // policy declines — so it leaves the card at SCAFFOLD (like the exploit payoff). The hand-authored
    // Savior of Ollenbock card + its scenario test are ground truth. See the Triggers.trains() /
    // TrainedEvent entries in card-sdk-language-reference.md.
    supported("WhenAPermanentTrains", "trigger: this creature trains (training payoff — Triggers.trains(), TrainedEvent) — capability only, emitter scaffolds")
    // "When this permanent leaves the battlefield, …" — the self leaves-the-battlefield trigger
    // (Triggers.LeavesBattlefield). Savior of Ollenbock uses it to return every linked
    // ExileUntilLeaves card under its owner's control. Capability-only alongside the training payoff it
    // pairs with; the emitter scaffolds the whole exile-and-return loop.
    supported("WhenAPermanentLeavesTheBattlefield", "trigger: this permanent leaves the battlefield (Triggers.LeavesBattlefield)")
    // OTJ crime (CR 700.10) — "Whenever you commit a crime, …" (Triggers.YouCommitCrime, Marauding Sphinx).
    supported("WhenAPlayerCommitsACrime", "trigger: you commit a crime (Triggers.YouCommitCrime)")
    // "Whenever one or more cards leave your graveyard, …" — batching leave-graveyard trigger
    // (Triggers.CardsLeaveYourGraveyard — Owlin Historian, Attuned Hunter). You-scoped + unfiltered only.
    supported("WhenAnyNumberOfGraveyardCardsLeave", "trigger: one or more cards leave your graveyard (Triggers.CardsLeaveYourGraveyard)")
    // "When this Aura/permanent is put into a graveyard from the battlefield, …" — the self LTB-to-
    // graveyard trigger (Reach for the Sky's "draw a card"). Maps to Triggers.PutIntoGraveyardFromBattlefield;
    // only the SELF (ThisPermanent), any-player shape renders (anything else scaffolds).
    supported("WhenAPermanentIsPutIntoAPlayersGraveyard", "trigger: this permanent put into a graveyard from the battlefield (Triggers.PutIntoGraveyardFromBattlefield)")
    // "You may cast this card from your graveyard if [condition]. If you do, it enters with a +1/+1
    // counter." (Undead Sprinter, DSK) — a conditional self-cast-from-graveyard permission with a
    // cast-this-way counter rider. Maps to staticAbility { ability = MayCastSelfFromZones(Zone.GRAVEYARD,
    // condition) } + replacementEffect(EntersWithCounters(selfOnly = true, condition = WasCastFromGraveyard)).
    // The emitter renders ONLY the exact MayCastGraveyardCardWithEnterActions(self, [+1/+1]) body gated by a
    // nameable died-this-turn condition; any other body/condition scaffolds, so this is the gate.
    supported("FromGraveyardIf", "rule: conditional self-cast from graveyard + cast-this-way +1/+1 rider (MayCastSelfFromZones(condition) + EntersWithCounters(WasCastFromGraveyard))")

    // Intervening-if conditions (CR 603.4) gating a TriggerI, plus the Mount "while saddled" gate. The
    // emitter renders the recognised shapes to `triggerCondition = Conditions.*`; an unrenderable
    // condition still scaffolds, so these are accepted as supported vocabulary (the emitter is the gate).
    supported("PermanentPassesFilter", "condition: a permanent matches a filter (e.g. ThisPermanent IsSaddled -> Conditions.SourceIsSaddled)")
    // "if it's a creature card" after revealing a face-down permanent (Hauntwoods Shrieker). The
    // emitter renders the IsCardtype(Creature) shape to Conditions.TargetIsCreatureCard(0) (reads the
    // underlying card, not the face-down 2/2 projection); other revealed-card filters scaffold.
    supported("ACardWasRevealedThisWay", "condition: a revealed card matches a filter (IsCardtype Creature -> Conditions.TargetIsCreatureCard)")
    supported("PlayerPassesFilter", "condition: a player matches a filter (e.g. You HasntCastASpellThisTurn)")
    // "an opponent matches [filter]" — APlayerPassesFilter(Opponent, …). Used by Claim Jumper's
    // intervening-if "if an opponent controls more lands than you". The engine has
    // Conditions.OpponentControlsMoreLands; the emitter declines the surrounding repeatable-search loop,
    // so this is capability-only (-> SCAFFOLD).
    supported("APlayerPassesFilter", "condition: a player (e.g. an opponent) matches a filter")
    // "controls more [permanents] than [player]" -> Conditions.OpponentControlsMoreLands (for the
    // Land/You scope). Capability vocabulary; the emitter declines the loop body that uses it.
    supported("ControlsMorePermanentThanPlayer", "predicate: a player controls more [filter] than another (Opponent>You Lands -> Conditions.OpponentControlsMoreLands)")
    supported("IsSaddled", "predicate: this permanent is saddled (CR 702.171b)")
    // "during your turn" — IsPlayersTurn(You) -> Conditions.IsYourTurn, the gate on Overzealous Muscle's
    // "Whenever you commit a crime during your turn, …". Renders as a triggerCondition when it wraps a
    // trigger (the emitter unwraps the If-gated trigger); any non-You scope declines -> SCAFFOLD.
    supported("IsPlayersTurn", "condition: it's a given player's turn (You -> Conditions.IsYourTurn)")
    // "if a card left your graveyard this turn" — ACardLeftPlayersGraveyardThisTurn(AnyCard, You) ->
    // Conditions.CardsLeftGraveyardThisTurn(1) (Living History's attack TriggerI, Primary Research's
    // end-step TriggerI). Only the bare any-card + You scope renders; other shapes scaffold.
    supported("ACardLeftPlayersGraveyardThisTurn", "condition: a card left your graveyard this turn (Conditions.CardsLeftGraveyardThisTurn(1))")
    supported("HasntCastASpellThisTurn", "predicate: player hasn't cast a (filtered) spell this turn")
    // "you've cast another spell this turn" -> Conditions.YouCastSpellsThisTurn(atLeast = 2) at
    // resolution (the resolving spell is already recorded). The emitter renders the "Other ThisSpell"
    // shape; a non-You / unfiltered-other mismatch declines -> SCAFFOLD.
    supported("CastASpellThisTurn", "predicate: player has cast a (filtered) spell this turn (YouCastSpellsThisTurn)")
    // OTJ crime (CR Outlaws of Thunder Junction) — "you've committed a crime this turn" ->
    // Conditions.YouCommittedCrimeThisTurn, gating a cost reduction or a resolution-time effect.
    supported("CommitedACrimeThisTurn", "predicate: player has committed a crime this turn (YouCommittedCrimeThisTurn)")
    // Delirium (ability word) — "there are four or more card types among cards in your graveyard."
    // Both the static "as long as …" gate (NumCardTypesInGraveyardIs) and the activated-ability
    // "Activate only if …" gate (ThereAreNumberCardTypesInPlayersGraveyard) map to Conditions.Delirium(N)
    // (DISTINCT_TYPES aggregation over your graveyard). Spineseeker Centipede, Balustrade Wurm.
    supported("NumCardTypesInGraveyardIs", "predicate: N or more card types among cards in your graveyard (Conditions.Delirium)")
    supported("ThereAreNumberCardTypesInPlayersGraveyard", "condition: N or more card types in a player's graveyard (Conditions.Delirium)")
    // "if you do" / "if [the cost] was paid" — the IfYouDoEffect linkage after a MayCost(DiscardACard)
    // loot, rendered by the emitter as MayEffect(IfYouDoEffect(...)). Universal condition vocabulary.
    supported("CostWasPaid", "condition: the optional cost was paid (IfYouDoEffect linkage)")
    supported("WasCastFromTheirHand", "predicate: spell cast from the player's hand (fromZone = HAND)")
    // The negation — "a spell from anywhere other than your hand" (Kellan, the Kid). Backed by
    // SpellCastPredicate.CastFromZoneOtherThan(Zone.HAND). The trigger itself is coverable even
    // though Kellan's free-cast-or-play-land body declines to SCAFFOLD in the emitter.
    supported("WasntCastFromTheirHand", "predicate: spell cast from a zone other than the player's hand (CastFromZoneOtherThan(HAND))")
    // Source-relative Mount/Vehicle payoff filter: "a creature that crewed/saddled it this turn"
    // (Giant Beaver) -> GameObjectFilter.Creature.crewedOrSaddledSourceThisTurn().
    supported("SaddledPermanentThisTurn", "filter: a creature that saddled this permanent this turn (crewedOrSaddledSourceThisTurn)")
    supported("CrewedPermanentThisTurn", "filter: a creature that crewed this permanent this turn (crewedOrSaddledSourceThisTurn)")

    // Costs.
    supported("PayMana", "cost: pay mana (universal)")
    // A mana cost carrying {X} — the player-declared variable generic paid at cast/activation, whose
    // chosen value flows to resolution (DynamicAmount.XValue). Lantern Flare's cleave cost {X}{R}{W}
    // is the case in point: the engine's X-on-cleave support computes the affordable-X ceiling, threads
    // the chosen X through payment, and exposes it to the cleaved effect. Capability-only: the {X} /
    // cast-time-value area is exactly what the emitter declines to render exactly (creator's note in
    // mtgish-tooling/CLAUDE.md), so cards using it stay SCAFFOLD even though the capability is present.
    supported("PayManaAnyX", "cost: pay mana with {X} (player-declared, threads to resolution via DynamicAmount.XValue)")
    // Planeswalker loyalty cost (CR 606) — the +N / -N ability activation cost. The engine models it
    // via `loyaltyAbility(loyaltyChange) { }` with `startingLoyalty`. Oko, the Ringleader. The emitter
    // declines the whole loyalty-ability envelope (Activated) -> SCAFFOLD, so this is capability-only.
    supported("Loyalty", "cost: planeswalker loyalty +N/-N (loyaltyAbility(change) { })")
    supported("SacrificeAPermanent", "cost: sacrifice")
    supported("SacrificeNumberPermanents", "cost: sacrifice N")
    // "Pay N life" as an activation cost -> Costs.PayLife(n). The emitter renders fixed-integer amounts
    // (abilityCostDsl); non-integer amounts ({X}, life-total halves, …) are declined -> SCAFFOLD.
    supported("PayLife", "cost: pay life")
    // "As an additional cost to cast this spell, pay X life" (Vicious Rivalry) — a player-declared
    // variable additional cost. The engine models it via Costs.additional.PayXLife(); the declared X
    // flows to the spell's resolution X value. Capability-only: the emitter keeps it at SCAFFOLD because
    // it sits in the cast-time-X / extra-cost area the module declines to render exactly.
    supported("AdditionalCastingCostX", "cost: additional pay-X-life (Costs.additional.PayXLife())")
    // "As an additional cost to cast this spell, <cost>" — the generic additional-cost wrapper (the IR
    // `AdditionalCastingCost` node). Soaring Stoneglider's "exile two cards from your graveyard or pay
    // {1}{W}" is `AdditionalCastingCost(Or(ExileNumberGraveyardCards, PayMana))`. The engine models the
    // exile-or-pay shape via Costs.additional.ExileFromGraveyardOrPay(). Capability-only: the emitter
    // keeps additional-cost shapes at SCAFFOLD (the cast-time extra-cost area it declines to render).
    supported("AdditionalCastingCost", "cost: additional cost to cast (Costs.additional.*)")
    supported("ExileNumberGraveyardCards", "cost: exile N cards from your graveyard (Costs.additional.ExileFromGraveyardOrPay() / exile-from-graveyard)")
    // Waterbend {N} (Avatar: The Last Airbender, CR) — a generic-mana cost where each generic may be
    // paid by tapping an untapped artifact/creature you control. On activated abilities this maps to
    // `activatedAbility { cost = Costs.Mana("{N}"); hasWaterbend = true }`. The emitter renders the
    // fixed-generic shape; the X-carrying variants (WaterbendX / WaterbendCustomX) are left unsupported
    // -> blocked (the engine doesn't model an X waterbend cost yet).
    supported("Waterbend", "cost: waterbend {N} (tap artifacts/creatures, each pays {1} generic)")
    composed("DiscardACardOfType", "cost: discard filtered")
    // "Remove N counters of a type from among permanents you control" — the generic
    // remove-counters-from-among cost atom (e.g. Eladamri, Korève Domain).
    // Renders as Costs.additional.RemoveCounters(n, counterType, filter) for additional costs
    // or Costs.RemoveCounters(n, counterType, filter) for ability costs.
    // NOTE: entry is keyed by the _Cost discriminator value ("RemoveCounters"), not the
    // _RemoveCountersCost sub-variant — the tag extractor only reads CAPABILITY_DISCRIMINATORS
    // (_Cost, _Action, _Trigger, …), and _RemoveCountersCost is not a top-level discriminator.
    // The emitter still inspects the _RemoveCountersCost sub-field to decide which specific
    // shape it can render (NumberCountersOfTypeFromAmongPermanents vs ACounterOfTypeFromPermanent, …).
    supported("RemoveCounters",
        "cost: remove N counters (Costs.RemoveCounters / Costs.additional.RemoveCounters / Costs.pay.RemoveCounters)")

    // Duration-scoped continuous trigger / replacement creators.
    composed("CreateReplaceWouldDealDamageUntil", "PreventDamageShield / RedirectNextDamage", composes = listOf("PreventDamageShield"))
    // Duration-scoped PREVENTION twin of CreateReplaceWouldDealDamageUntil (mtgish prevention/replacement
    // split). "Prevent all (combat) damage that would be dealt … this turn" — Deep Wood, Leery Fogbeast,
    // Maze of Shadows. Same PreventDamageShield capability, minus the `PreventThatDamage` replacement payload.
    composed("CreatePreventDamageUntil", "PreventDamageShield (duration-scoped prevention)", composes = listOf("PreventDamageShield"))
    composed("CreateTriggerUntil", "CreateGlobalTriggeredAbility (duration)", composes = listOf("CreateGlobalTriggeredAbility"))
    composed("CreateFutureTrigger", "CreateDelayedTrigger", composes = listOf("CreateDelayedTrigger"))

    // "If a triggered ability of a [filter] you control triggers, that ability triggers an additional
    // time" (Annie Joins Up, Twinflame Travelers — CR 603.2d). A static doubler over a filtered group;
    // emitter renders staticAbility { ability = AdditionalSourceTriggers(sourceFilter = …) }.
    effect("AbilitiesTriggerAnAdditionalTime", "AdditionalSourceTriggers")

    // "[Each player] can't cast more than N spell(s) each turn" (High Noon / Yawgmoth's Agenda) — the
    // nested _PlayerEffect of a PlayerEffect / EachPlayerEffect static. Renders to
    // `RestrictSpellsCastPerTurn(maxPerTurn = N, eachPlayer = <scope>)`.
    effect("CantCastMoreThanNumberSpellsEachTurn", "RestrictSpellsCastPerTurn")

    // "Your opponents can't cast spells [during your turn]" / "… can't activate abilities of
    // <filter> [during your turn]" (Grand Abolisher, Voice of Victory) — the nested _PlayerEffects of
    // an `EachPlayerEffect{Opponent}` static, optionally gated by an `If(IsPlayersTurn(You))`. They
    // render to the who/which/when-parameterised `PlayersCantCastSpells` / `PlayersCantActivateAbilities`
    // statics (affected = Player.EachOpponent, condition = Conditions.IsYourTurn). The activate filter
    // is an `AbilityOfAPermanent(Or[IsCardtype …])` cardtype union.
    effect("CantCastSpells", "PlayersCantCastSpells")
    effect("CantActivateAbilities", "PlayersCantActivateAbilities")
    supported("AbilityOfAPermanent", "ability selector: activated abilities of permanents matching a filter")

    // "Players may play cards they exiled this way" (Memory Vessel) — the nested _PlayerEffect of a
    // CreateEachPlayerEffectUntil. Each affected player's effect grants a may-play window over the
    // cards that player exiled this way: `GrantMayPlayFromExile` over the per-player exile
    // collection. Capability-only — the emitter declines the per-player exile/may-play/restriction
    // activated-ability cluster -> SCAFFOLD.
    composed(
        "MayPlayExiledCards",
        "GrantMayPlayFromExile over the cards that player exiled this way",
        composes = listOf("GrantMayPlayFromExile"),
    )
    // "Players can't play cards from their hand" (Memory Vessel) — the nested _PlayerEffect of a
    // CreateEachPlayerEffectUntil. Renders to the hand-scoped player restriction
    // `CantPlayCardsFromHand` (blocks casting spells and playing lands from the hand zone only;
    // exile/graveyard may-play windows are unaffected).
    effect("CantPlayCardsFromHand", "CantPlayCardsFromHand")

    // Rest in Peace: "exile all graveyards" (ETB) + "if a card or token would be put into a graveyard
    // from anywhere, exile it instead" (static replacement). The first composes via the Gather -> Move
    // pipeline over Player.Each graveyards; the second is the RedirectZoneChange(EXILE) replacement.
    composed(
        "ExileEachPlayersGraveyard",
        "GatherCards(GRAVEYARD, Player.Each) -> MoveCollection -> EXILE",
        composes = listOf("GatherCards", "MoveCollection"),
    )
    effect("ReplaceWouldPutIntoGraveyard", "RedirectZoneChange")
    supported("WouldPutACardOrTokenInAPlayersGraveyardFromAnywhere", "replaceable event: any card/token to any graveyard from anywhere")
    supported("ExileItInstead", "replacement action: exile instead of going to the graveyard")

    // "If one or more tokens would be created under your control, those tokens plus a <token>
    // are created instead" (Peregrin Took, Worldwalker Helm, Quina, Qu Gourmet). Maps to the
    // CreateAdditionalToken replacement. Capability-only — the emitter declines (SCAFFOLD)
    // because the IR describes the added token inline (P/T/color/subtype) while the SDK
    // references a predefined token by name, a card-specific mapping we won't render lossily.
    composed(
        "ReplaceAnyNumberOfTokensWouldBeCreated",
        "CreateAdditionalToken (those tokens plus an extra named token)",
        composes = listOf("CreateAdditionalToken"),
    )

    // "You have no maximum hand size for the rest of the game" (Wisdom of Ages) — the nested
    // _PlayerEffect of a CreatePlayerEffect(You) resolution action. Renders to the one-shot
    // player-scoped `RemoveMaximumHandSize` effect (distinct from the battlefield-only
    // NoMaximumHandSize static used by Reliquary Tower / Thought Vessel).
    effect("HasNoMaximumHandSize", "RemoveMaximumHandSize")

    // "Spells your opponents cast from graveyards or from exile cost {2} more to cast" (Aven
    // Interrupter) — the nested _PlayerEffect of an EachPlayerEffect{Opponent} static. Renders to
    // `ModifySpellCost(target = SpellCostTarget.OpponentsCastFromZones(...), modification =
    // IncreaseGeneric(N))`. The spell-zone selectors below carry the zone set.
    effect("IncreaseSpellCost", "ModifySpellCost")
    supported("WasCastFromExile", "spell selector: cast from exile (SpellCostTarget.*CastFromZones zone = EXILE)")
    supported("WasCastFromAPlayersGraveyard", "spell selector: cast from a graveyard (SpellCostTarget.*CastFromZones zone = GRAVEYARD)")

    // "Spells you cast from your graveyard or from exile cost {N} less" (Doc Aurlock, Grizzled
    // Genius) — the nested _PlayerEffect of a controller-scoped PlayerEffect{You} static. Renders to
    // `ModifySpellCost(target = SpellCostTarget.YouCastFromZones(GRAVEYARD, EXILE), modification =
    // ReduceGeneric(N))`. The you-cast analogue of the IncreaseSpellCost/OpponentsCastFromZones
    // shape above; the spell-zone selectors are shared.
    effect("DecreaseSpellCost", "ModifySpellCost")
    // "Plotting cards from your hand costs {N} less" (Doc Aurlock) — a controller-scoped player
    // static over the Plot special action (CR 718). Renders to `ModifyPlotCost(target =
    // PlotCostTarget.YouPlotFromHand, modification = ReduceGeneric(N))`.
    effect("DecreasePlotFromHandCost", "ModifyPlotCost")

    // Fblthp, Lost on the Range (CR 718) — top-of-library plot + look. Nested _PlayerEffect /
    // _Rule capabilities of the controller-scoped statics.
    supported("MayLookAtTopCardOfLibraryAnyTime", "player static: look at the top card of your library any time (LookAtTopOfLibrary)")
    supported("MayPlotCardsFromTheTopOfTheirLibrary", "player static: plot cards from the top of your library (PlotFromTopOfLibrary)")
    supported("TopCardOfPlayersLibraryEffect", "rule: the top card of your library has plot, plot cost = its mana cost (PlotFromTopOfLibrary)")

    // Roxanne, Starfall Savant (CR 605) — "Whenever you tap an artifact token for mana, add one mana
    // of any type that artifact token produced." A triggered mana ability; renders to the
    // AdditionalManaOnSourceTap mirror static (color = null), the same shape as Lavaleaper's land mirror.
    supported("WhenAPlayerTapsAPermanentForMana", "trigger: tap a permanent for mana → AdditionalManaOnSourceTap mirror static")

    // Torpor Orb / Hushwing Gryff / Tocatli Honor Guard — "Creatures entering don't cause abilities to
    // trigger." A continuous static that suppresses CR 603.6 enters-the-battlefield triggers caused by a
    // matching permanent entering. Renders to the SuppressEntersTriggers static ability.
    supported("PermanentsEnteringTheBattlefieldDontCauseAbilitiesToTrigger", "rule: matching permanents entering don't cause abilities to trigger (SuppressEntersTriggers)")

    // Characteristic-defining power/toughness (CR 604.3, applied in layer 7a) — "~'s power is equal to
    // the number of [X]". The card DSL models each stat independently: `dynamicPower(amount, offset)` /
    // `dynamicToughness(amount, offset)`, with `dynamicStats(...)` composing both over one shared count
    // (the `*`/`*` cycle). Each stat is its own IR rule, so both are supported on their own — a
    // power-only CDA (Duelist of the Mind's `*`/3) and two different counts (Yavimaya Kavu) are as
    // renderable as the matched pair. Whether the *amount* itself is recoverable is the emitter's call;
    // an unreadable count still scaffolds.
    supported("CDA_Power", "rule: characteristic-defining power (dynamicPower / dynamicStats)")
    supported("CDA_Toughness", "rule: characteristic-defining toughness (dynamicToughness / dynamicStats)")
}
