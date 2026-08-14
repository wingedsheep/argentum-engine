# Marvel Super Heroes (MSH) — Missing Mechanics

Cards from MSH that **cannot** be implemented with the current engine/SDK, grouped by the
missing mechanic they need. Each mechanic is `add-feature` territory (a new SDK primitive,
keyword, or engine capability) — not pure card authoring.

Scope: the 276 booster cards (collector numbers 1–276). Triaged against the SDK on 2026-08-04,
updated 2026-08-07 after **power-up** and the **per-turn effect budget** shipped, 2026-08-08
after **teamwork** shipped in full, and 2026-08-10 after **copy-with-exceptions**,
**ward with a non-listed cost**, the **ability-source predicate on stack targets** and **improvise**
shipped. **23 of the 276 are blocked**; every other card is buildable from existing primitives.

Supported today and *not* a blocker despite looking like one: **power-up** (see the first section
below — the keyword, its once-only limit and its pip-wise cost reduction all ship), **harness / ∞ abilities**
(`CounterType.HARNESS` already ships, backing the SPM Infinity Stones — see
`definitions/spm/cards/TheSoulStone.kt`), the **Hero** and **Villain** creature types
(`Subtype.HERO`, `Subtype.VILLAIN`), the **Plan** enchantment *subtype* (`Subtype` is a free-form
`@JvmInline value class`, so no registration is needed), **sneak**, **connive**, **saga**, **crew**,
**landcycling**, **finality/stun counters**, and **registering a new passive named counter** (that is
documented routine card work — see `docs/card-sdk-language-reference.md` §16).

---

## Power-up — SHIPPED ✅ (20 of 24 cards unblocked)

> Power-up — {4}{W}: Put two +1/+1 counters on this creature. *(Activate each power-up ability
> only once. Reduce the cost by its mana cost if it entered this turn.)*

The set's marquee mechanic, implemented 2026-08-07 against **CR 702.193** (the rules text ships the
keyword; don't work from the reminder text alone). Authoring is `isPowerUp = true` in an
`activatedAbility { }` block — the same shape as `isExhaust`, deliberately, since both mean
"activate only once". See `docs/card-sdk-language-reference.md` → *Power-up*.

What that flag does:

1. **"Activate only once"** — the DSL auto-adds `ActivationRestriction.Once`, a per-object lifetime
   limit. Per CR 400.7 a re-entering permanent is a new object and may power up again; the existing
   `AbilityActivatedEverComponent` tracker gives that for free.
2. **The prefix** — `ActivatedAbility.describeWithCost` renders `"Power-up — "`, and the enumerator
   rebuilds the label from the *effective* cost, so the menu shows the discounted cost on the turn
   the permanent entered.
3. **The cost reduction** — `ManaCost.subtract(other)` in `mtg-sdk/.../core/ManaCost.kt` implements
   the pip-wise reduction of CR 702.193b / 118.7 (generic reduces generic; colored and colorless
   reduce their own type with excess spilling to generic; hybrids cancel identical hybrids then
   either half; `{X}` inert on both sides). `CastPermissionUtils.applyPowerUpSelfReduction` applies
   it gated on `EnteredThisTurnComponent`, from inside `applyActivatedAbilityCostReduction` — so all
   three read sites (the enumerator and both `ActivateAbilityHandler` sites) stay in lockstep.
   `ManaCost.subtract` is also exactly what **offering** (CR 702.48c) needs, if that ever comes up.

`ReduceActivatedAbilityCost.powerUpOnly` shipped alongside, mirroring `exhaustOnly`, which unblocks
**Hulk, Gamma Goliath** [215] ("Power-up abilities of other creatures you control cost {3} less").

Tests: `ManaCostSubtractTest` (every printed MSH power-up cost/mana-cost pair, plus the CR 118.7
subrules the set doesn't exercise) and `PowerUpKeywordScenarioTest` (once-only, re-entry reset,
entered-this-turn gating, displayed-vs-paid lockstep, stacking with `powerUpOnly`).

**Now buildable as ordinary card work (20):** Brave Brawler [8] · Captain Marvel, Earth's Protector
[11] · Aerial Doombot [43] · Bold Biochemist [48] · Stature,
Size Shifter [76] · Ninja of the Hand [108] · Unliving Legionnaire [119] · Human Torch, Johnny Storm
[136] · Quicksilver, Brash Blur [148] · Volcanic Villain [159] · Hercules, Prince of Power [171] ·
Pet Avengers [178] · Serpent Specialist [186] · She-Hulk, Jade Defender [188] · White Tiger, Ava
Ayala [196] · Abomination, Terrifying Titan [198] · Hulk, Gamma Goliath [215] · Thanos, the Mad Titan
[233] · Ultron Drone [253] · Viv Vision, Teen Synthezoid [256].

Non-blocking notes for those: Stature's "can't be blocked if her power is 1 or less" is
`CantBeBlockedWhilePropertyAtMost` (power-only, `GroupFilter.source()`) — **not** a
`ConditionalStaticAbility` over a power comparison, which would read her printed power and never
switch off; Quicksilver's opening-hand clause is `mayBeginGameOnBattlefield()`; Thanos's odd/even
sweep is `.manaValueIsOdd()` / `.manaValueIsEven()` + a modal.

### Still blocked — 4 cards, each needing one more thing ⛔

- **Wonder Man, Hollywood Hero** [160] — "Each power-up ability of permanents you control can be
  activated an additional time" must *raise* the limit. `ActivationRestriction.Once` is a fixed
  `data object`; needs either `ActivationRestriction.MaxPerGame(count: DynamicAmount)` or a
  `GrantExtraPowerUpActivations(filter, amount)` static consulted where `Once` / `MaxPerTurn` are
  enforced. `IgnoreExhaustActivationLimit` / `ExhaustActivationWaiver` is the structural precedent,
  but it *waives* the limit rather than raising it by one.
- **Kang the Conqueror** [62] — "During that turn, power-up abilities can't be activated" needs a
  turn-scoped flag on `GameState`, read where granted `PreventActivatedAbilities` is read
  (`CastPermissionUtils.isActivationPrevented`) and gated on `ActivatedAbility.isPowerUp`, which now
  exists. The extra turn itself is fine (`Effects.TakeExtraTurn`).
- **Nick Fury, Agent of S.H.I.E.L.D.** [25] — the power-up and the top-seven dig are both ordinary
  composition (Gather → Select → Move, Gishath's shape), but *"If it's a double-faced card, you may
  transform it"* has no faithful modelling: there is no "is a double-faced card" predicate anywhere
  in the SDK, so the optional transform can only be offered unconditionally — a prompt on a
  single-faced permanent, where the printed card offers none. Needed: a `StatePredicate.IsDoubleFaced`
  (the card component already knows its back face; it is the *predicate* and its `PredicateEvaluator`
  branch that are missing), then wrap the existing `MayEffect(ForEachInCollectionEffect(…,
  TransformEffect))` in a `ConditionalEffect` over it. Note the transform must stay *post-entry* —
  the printed order puts the card onto the battlefield first, so its ETB triggers fire on the front
  face and only then does it flip, which is not the same as entering transformed.
- **Loki Laufeyson** [143] — the power-up half is done; the *other* ability needs a delayed "when you
  next cast" trigger whose spell filter is source-relative
  (`CardPredicate.ManaValueAtMostDynamic(DynamicAmounts.sourcePower())` — the predicate exists, but
  nothing evaluates a source-relative dynamic filter inside delayed-trigger matching).

## Teamwork N — SHIPPED ✅ (all 13 cards implemented)

> Teamwork 4 *(As an additional cost to cast this spell, you may tap any number of creatures you
> control with total power 4 or more.)*

The set's second new mechanic, implemented 2026-08-08 against **CR 702.194**: an **optional**
additional cast cost plus a durable "was cast using teamwork" fact readable at resolution. Authoring
is `teamwork(n)` in the `card { }` block — the same one-line shape as `bargain()`, deliberately,
since both are optional additional costs riding one rail. See
`docs/card-sdk-language-reference.md` → *Teamwork N*.

What that helper does:

1. **The rail** — `KeywordAbility.OptionalAdditionalCost(additionalCost = …, displayPrefix =
   "Teamwork N", keyword = Keyword.TEAMWORK, declaredSlot = ChoiceSlot.TEAMWORK)`, so the enumerator
   offers a `CastWithKicker` variant labelled "(Teamwork N)", the ordinary additional-cost payment
   flow collects the taps, and the engine stamps the slot on the stack object and durably onto the
   permanent it becomes. `ChoiceSlot.TEAMWORK` keeps "cast using teamwork" a *different* fact from
   "kicked" and "bargained" (CR 702.194b).
2. **The payoff** — `Conditions.TeamworkWasPaid`, a facade over
   `CastChoiceMade(ChoiceSlot.TEAMWORK)` mirroring `WasBargained`. The "choose both instead" shape
   (the mode count is CR 700.2, branching on the CR 601.2b declaration — *not* CR 702.194c, which is
   about targets) is `modal(chooseCount = 2, minChooseCount = 1, dynamicChooseCount =
   DynamicAmount.Conditional(Conditions.TeamworkWasPaid, 2, 1))` — the existing modal type, with two
   wiring fixes it needed: the cast handler now evaluates `dynamicChooseCount` with *this cast's*
   `declaredCostSlot` (without it the teamwork branch could never be taken, since the durable
   cast-choices bag only exists after the spell resolves), and the declared cast is enumerated as a
   `CastSpellModal` variant so the client is offered the modes at all. Covered end to end by
   `TeamworkMechanicScenarioTest`'s "Teamwork Orders" cases.
   **CR 702.194c** — a teamwork-only clause that has its own target is chosen only on the declared
   cast — is the rail's `kickerTarget(...)` / `kickerEffect` slots: the enumerator prices the plain
   cast against `targetRequirements` and the declared cast against `kickerTargetRequirements`, so the
   plain cast is announced as though the clause weren't there. Covered by the "Teamwork Rally" cases.
3. **The cost atom** — the one real gap, now closed. `CostAtom.VariablePermanents` gained
   `PermanentCostAction.TAP`, `VariableCostMeasure.TOTAL_POWER` and a `minMeasure` floor (a threshold
   on the *measure*, not the count), reached through `Costs.additional.TapForTotalPower(n)`. The atom
   also became payable as a *spell* additional cost, not only an activated-ability cost, through
   `AdditionalCostPayment.variableCostPermanents`.
4. **The crew selection, mirrored** — `VariablePermanentsCost`
   (`rules-engine/.../mechanics/cost/`) is the shared answer to "which permanents can pay, and how
   much do the chosen ones measure" for every reader of `CostAtom.VariablePermanents`, modelled on
   `CrewEnumerator`: untapped only (CR 701.26a), controlled by the payer, matched and summed through
   **projected** state, and with no summoning-sickness check (CR 302.6 governs the `{T}` symbol, not
   a tap paid as a cost). The cast enumerators, the cast validator, the payer and the built-in AI all
   read it, so they can't disagree. Crew and saddle are *not* routed through it — they are a
   different activation shape and keep their own copy of the eligibility rule (their measure must
   stay separate, since crew-specific "crews as though its power were 2 greater" statics must not
   raise a teamwork total); folding their candidate selection in is an open follow-up.
   The candidate payload sent to the client is the crew/saddle `TapForPowerCreatureData`, under a
   `costType = "TapForTotalPower"` cost-payment phase.
5. **The tap cause** — the payer-side half, added 2026-08-08 for Agent Maria Hill. `TappedEvent` now
   carries a `TapReason` (`mtg-sdk/.../scripting/TapReason.kt`) alongside `tappedById`, matched by
   `EventPattern.TapEvent.reason` and authored as `Triggers.BecomesTappedForTeamwork`. A separate axis
   from attribution is what the card needs: a teamwork tap, an attack tap and a crew tap are all
   performed by the creature's own controller, so `tappedById` is identical across them. Only
   `TapReason.TEAMWORK` is classified — stamped by `CastSpellHandler` on the taps paying an additional
   cost declared under `ChoiceSlot.TEAMWORK` (`TapReason.forChoiceSlot`) — and every other tap site
   deliberately reports `UNSPECIFIED` rather than a guess. Both tap sites for the atom now go through
   one chokepoint, `VariablePermanentsCost.tapAll`, so the cast payer and the ability payer can't
   drift.

Open follow-ups, neither reachable by a printed card today: folding crew's candidate selection onto
`VariablePermanentsCost.candidates` (their measures must stay split), and modal casts from a
non-hand zone — `CastFromZoneEnumerator` has never emitted `CastSpellModal` at all, so a modal
teamwork spell given flashback or a graveyard-cast grant would be advertised without its modes.

Tests: `TeamworkMechanicScenarioTest` (multi-creature payment, single-creature payment, declining,
an unmet threshold, already-tapped and opponent-controlled creatures, projected power via a lord,
summoning sickness, the durable flag on a resolving permanent, teamwork-vs-kicked separation, the
advertised legal action, the modal "Teamwork Orders" cases, and the CR 702.194c "Teamwork Rally"
cases), `TapReasonScenarioTest` (the cause on each classified and unclassified tap site, a
cause-agnostic trigger still matching, serialization), plus one scenario test per implemented card.

**Implemented (13):** Agent Maria Hill [2] · Helicarrier Strike [15] · Murdock's Crusade [24] ·
Atlantis Attacks [46] · We Say Thee Nay! [82] · Cruel Alliance [92] · Too Evil to Stay Dead [118] ·
Widow's Bite [122] · HULK SMASH! [135] · Repulsor Blast [150] · Team Tactics [155] · Earth's
Mightiest Heroes [165] · Go Nuts! [168]. Murdock's Crusade, Atlantis Attacks, Widow's Bite,
HULK SMASH! and Go Nuts! are the "choose both instead" modal shape, all on `dynamicChooseCount` as
above.

Cruel Alliance and Too Evil to Stay Dead are a third shape the rail already covered: the teamwork
"instead" replaces the *target requirement*, not the effect's size, so they use the shared
optional-additional-cost `kickerTarget` / `kickerEffect` slots (Fight with Fire, Brave the Wilds) —
the teamwork cast announces a target the plain cast could not.

Agent Maria Hill is the only *payer-side* payoff in the set: her trigger is on the creature that was
tapped, not on the spell that was cast, which is why `Conditions.TeamworkWasPaid` cannot express it.

## Shield counters — 1 card ⛔

> Captain America enters with a shield counter on him. *(If he would be dealt damage or destroyed,
> remove a shield counter from him instead.)*

Not implemented at all. `docs/card-sdk-language-reference.md` §16 lists `shield` among printed
counter kinds, but there is no `CounterType.SHIELD`, no `Counters.SHIELD`, and no engine handling
anywhere. Needed: the enum constant + string constant in `mtg-sdk/.../core/CounterType.kt`, plus the
built-in replacement per CR 122.1e.

The **stun counter is the exact structural precedent** — a counter with an inherent rule wired at a
central chokepoint (`untapOrConsumeStun` in `rules-engine/core/UntapHelpers.kt`, invoked from
`BeginningPhaseManager`, `TapUntapExecutor`, and the sacrifice/pay resumer). Shield needs the same at
the damage-application site (`DamageUtils.dealDamageToTarget`) and the destroy path (destroy executor
plus the lethal-damage state-based action).

Blocked card: **Captain America, Super-Soldier** [9]. Its second clause is already fine —
`GrantHexproofToController` + `GrantKeyword(HEXPROOF, Heroes)` under a `ConditionalStaticAbility`
gated on `Conditions.SourceHasCounter`.

## Equip worthy — SHIPPED ✅ (1 card implemented)

> Equip worthy {1} *(A creature is worthy if it's a legendary non-Villain that's red and/or white.)*

Implemented 2026-08-10 as a **`quality` + `targetFilter` pair on the existing `equipAbility` facade**,
with the five existing cards that were hand-rolling the shape converged onto it — not as a second
authoring path beside them. (Four of those five print an actual "Equip [quality]" ability; Ghostfire
Blade is the exception — see below.)

1. **`equipAbility(cost, genericCostReduction, quality, targetFilter)`**
   (`mtg-sdk/.../dsl/CardBuilder.kt`). CR 702.6c is the rule: an equip ability may further restrict
   its targets ("Equip [quality]" / "Equip [quality] creature") and "may legally target only a
   creature that's controlled by the player activating the ability and that has the chosen quality".
   `quality` supplies the wording, in two places: `ActivatedAbility.equipQuality`, which makes
   `describeWithCost` render the ability as its printed line ("Equip worthy {1}") against the
   *effective* cost, so an equip discount rewrites the menu text a `descriptionOverride` would have
   frozen; and the target requirement's id/label ("worthy creature you control"), which is the
   targeting prompt and `LegalActionInfo.targetDescription`. `targetFilter` is the rules half.
   Blackblade Reforged, Bilbo's Ring, Dúnedain Blade, Ghostfire Blade and Pirate Hat were each
   hand-rolling this as a bare `activatedAbility { }` **without `isEquipAbility`**, so Forge Anew's
   free first equip, Eowyn's equip discount and instant-speed-equip permissions all silently skipped
   their restricted halves; converging them onto the facade fixes that. Non-mana equip costs
   ("Equip—Sacrifice a creature") still use the hand-rolled escape hatch — the facade parses a mana
   cost only. `EquipQualityVariantTest` (mtg-sets) pins both catalog-wide invariants: every
   `isEquipAbility` ability is sorcery-speed and targets a single creature you control.

   Of those five, only Blackblade Reforged, Bilbo's Ring, Dúnedain Blade and Pirate Hat print a
   CR 702.6c quality restriction. **Ghostfire Blade does not** — it prints one "Equip {3}" plus
   "This Equipment's equip ability costs {2} less to activate if it targets a colorless creature",
   and our extra {1} ability is a *model* of that reduction (behaviourally equivalent, and older
   than the facade's `genericCostReduction` rail, which is how it would be written today). It rides
   the same rail; it is not a printed "Equip [quality]" card.

   "Worthy" itself is **not** an SDK concept — a Scryfall search for the term returns exactly one
   card, so it is spelled out on Mjölnir from existing predicates
   (`.legendary().notSubtype(Subtype.VILLAIN).withAnyColor(RED, WHITE).youControl()`) and the card's
   own printed reminder text carries the definition. No keyword-display entry was added.

2. **`SourceFilter.EquippedCreature`** (`mtg-sdk/.../scripting/events/EventFilters.kt`) — the mirror
   was real and is one line plus one engine branch: `DamageUtils.damageSourceMatches` now handles
   `SourceFilter.EnchantedCreature, SourceFilter.EquippedCreature` in a shared branch exactly as
   `damageRecipientMatches` already did for the `RecipientFilter` pair. That single matcher is used by
   prevention, doubling and flat/dynamic damage modification alike, and by both the combat and
   noncombat damage paths, so "Double all damage equipped creature would deal" is
   `DoubleDamage(appliesTo = DamageEvent(source = SourceFilter.EquippedCreature))` with no further
   plumbing.

Card: **Mjölnir, Hammer of Thor** [146].

## Copy-with-exceptions: name, added types, longer durations — SHIPPED ✅ (all 3 cards implemented)

Implemented 2026-08-10 as **convergence between the two copy paths**, not as three riders bolted onto
one of them. The gap was that `EachPermanentBecomesCopyOfTargetEffect`
(`mtg-sdk/.../scripting/effects/CopyEffects.kt`) exposed only `addedKeywords` / `powerOverride` /
`toughnessOverride` / `retainActivatingAbility` as copy exceptions while the *token* sibling
`CreateTokenCopyOfTargetEffect` had a dozen more, and that the permanent executor honoured only
`Duration.Permanent` / `EndOfTurn` / `UntilNextEndStep`, silently degrading anything else.

What shipped:

1. **`CopyExceptions`** (`mtg-sdk/.../scripting/effects/CopyExceptions.kt`) — one serializable value
   type for the whole "except …" half of a copy effect (CR 707.9b): `nameOverride`, `addedKeywords`,
   `addedSupertypes` / `removedSupertypes`, `addedCardTypes` / `overrideCardTypes`, `addedSubtypes` /
   `overrideSubtypes`, `addedColors` / `overrideColors`, `powerOverride` / `toughnessOverride`,
   `noManaCost`. The add-vs-override split is CR 205.1a (a stated card type or subtype *replaces*)
   against CR 205.1b (the "in addition to its other types" / "still a [type]" clause *retains* the
   prior types) — which is exactly the difference between Absorbing Man and Taskmaster.
   `EachPermanentBecomesCopyOfTargetEffect.exceptions` carries it; `CreateTokenCopyOfTargetEffect`
   keeps its historical flat riders as the authoring surface (≈20 card call sites) but exposes them
   as a `copyExceptions` view onto the same type. `retainActivatingAbility` stayed on the effect — it
   isn't a characteristic.
2. **`CopyExceptionApplier`** (`rules-engine/.../handlers/effects/copy/`) — the single place the
   type-line/name/keyword/P-T/color arithmetic lives. Four copy paths call it — permanent-becomes-a-
   copy, both token-copy executors, and the `EntersAsCopy` clone path — so a new exception is written
   once and all of them get it. (The two `removeLegendary`-only paths, Helm of the Host's
   equipped-creature token and spell copies, have no arithmetic to share and stay outside.) It also
   fixes a latent bug on the permanent path: a P/T
   override used to be dropped when the copied object had no base stats at all, which is precisely
   Absorbing Man copying a land.
3. **`Duration.UntilYourNextTurn`** in the copy-revert path — `RevertCopyAtYourNextTurnComponent(playerId)`,
   a sibling to the two existing revert markers, expired in
   `CleanupPhaseManager.expireUntilYourNextTurnEffects` alongside every other "until your next turn"
   effect. The long duration is load-bearing for the two triggered mimics: the copy replaces the card
   component wholesale, so the permanent's own first-main-phase trigger is gone while the copy is up
   and back in time to fire again once it reverts.

Tests: `CopyExceptionsTest` (the mechanic — add vs override on each axis, the add-then-remove
supertype order, P/T conjured onto a copy source that had none, and a pin that the token effect's
flat riders map onto the same vocabulary), plus one scenario test per card covering the removal
direction against the legend rule, the additive typing, and the until-your-next-turn revert window
with its re-firing trigger.

**Implemented (3):** Shuri, Wakandan Inventor [75] · Absorbing Man [199] · Taskmaster, Mercenary
Mimic [232].

## Improvise (CR 702.126) — SHIPPED ✅ (2 of 2 cards unblocked)

Shipped 2026-08-10 against the verified rule text: CR 702.126a "For each generic mana in this spell's
total cost, you may tap an untapped artifact you control rather than pay that mana", 702.126b (neither
an additional nor an alternative cost; applies only *after* the total cost is determined), 702.126c
(multiple instances are redundant).

The triage above proposed a fourth parallel payment field (`improvisedArtifacts`) beside
`convokedCreatures`, `harmonizeCreature` and `waterbendPermanents`. That was **not** what shipped.
Improvise and waterbend are the same mechanism with different eligibility, so the two were converged
onto one rail instead:

- `AlternativePaymentChoice.waterbendPermanents` was renamed `tapForGenericPermanents` and is now the
  single carrier for "tap permanents you control, each paying {1} generic".
- The eligibility rule is a value, `TapForGeneric.IMPROVISE` (artifacts) / `TapForGeneric.WATERBEND`
  (artifacts or creatures), consumed by one `AlternativePaymentHandler.applyTapForGeneric` and one
  `CostEnumerationUtils.findTapForGenericPermanents` / `canAffordWithTapForGeneric`.
- `LegalAction` / the DTO / the client renamed to match, plus a `tapForGenericLabel` so the one HUD
  (`TapForGenericSelector`, `tapForGeneric` pipeline phase) names the mechanic being paid.

A further keyword of this shape is now one enum entry, not a new field + handler branch + UI.
Card-side authoring is just `keywords(Keyword.IMPROVISE)`.

Unblocked cards: **Ironheart, Clever Champion** [60] · **Arc Reactor** [243]. Ironheart's second line
("Noncreature spells you cast have improvise") needed no extra work —
`GrantKeywordToOwnSpells(Keyword.IMPROVISE, GameObjectFilter.Noncreature)` resolves through the same
`GrantedKeywordResolver` every cost keyword uses.

## Ability-source predicate on stack targets — SHIPPED ✅ (2 of 2 cards unblocked)

Shipped 2026-08-10. An ability on the stack is its own object with none of its source's
characteristics (CR 113.3b/c), so a type predicate applied to the ability entity is never true.
`CardPredicate.AbilitySourceMatches(subfilter)` redirects the match onto the ability's **source**
(CR 113.7) — `ActivatedAbilityOnStackComponent.sourceId` / `TriggeredAbilityOnStackComponent.sourceId`
— and evaluates the subfilter there, in the evaluator's stack branch alongside
`CardPredicate.TargetsMatching`. The source is read from the projection while it is on the
battlefield and from its printed characteristics once it has left, so a dead creature's dies trigger
is still "from a creature source" (CR 113.7a, and CR 608.2b for the resolution-time re-check) — the
same source resolution `CantBeTargetedBySourceTypeAbilities` uses.

Authoring: `Targets.ActivatedOrTriggeredAbilityYouControlFrom(GameObjectFilter.Creature)`, or the
`.abilitySourceMatches(...)` chain on `GameObjectFilter` / `TargetFilter`. See
`docs/card-sdk-language-reference.md`.

Shipped alongside, because the cards were unplayable without it: the legal-action enumerator
(`TargetEnumerationUtils.findValidSpellTargets`) filtered *every* stack target down to spells, so no
ability-targeting card — including the already-shipped Gogo, Master of Mimicry and Peter Parker's
Camera — was ever **offered** an ability as a legal target, even though `TargetFinder` accepted one
when the action was submitted. Both readers now go through the single
`StackObjectTargeting.permitsAbilities` seam.

**Unblocked (2):** Echo, Perceptive Prodigy [51] (creature source) · Scientist Supreme of A.I.M.
[225] (artifact source).

## Ward with a non-listed cost — SHIPPED ✅ (both cards implemented)

`WardCost` (`mtg-sdk/.../scripting/effects/StackEffects.kt`) had exactly six variants: `Mana`, `Life`,
`DynamicLife`, `Discard`, `Sacrifice`, `Composite`, with `Composite` being **AND** (Gisa's
"Ward—{2}, Pay 2 life"). Two variants were added 2026-08-10, each with its branch in
`WardCounterEffectExecutor` and description strings in `KeywordAbility.kt` /
`KeywordStaticAbilities.kt` / `StackEffects.kt`:

- **`WardCost.PlayerCounters(counterType, amount)`** — "Ward—Get five poison counters"
  (`KeywordAbility.wardPlayerCounters(Counters.POISON, 5)`). Counters placed on the *paying* player
  (CR 122.1), through the ordinary `AddCountersEffect` executor so replacement effects, the
  `CountersAddedEvent` and the ten-poison state-based action (CR 122.1f) all follow for free. It is
  the one ward cost with no affordability gate — a player can always get counters, so it always
  prompts and never counters for inability. → **The Serpent Society** [226]
- **`WardCost.Choice(options)`** — "Ward—Discard a card or pay {2}"
  (`KeywordAbility.wardChoice(...)`, or the named `wardDiscardOrPay("{2}")`), the OR sibling of
  `Composite`'s AND, modelled on `AdditionalCost.Choice` / `PayCost.Choice`. A `ChooseOptionDecision`
  lists only the options the payer can pay plus a trailing decline; picking one charges it through
  that cost's own ordinary flow, so the disjunction adds a picker and no payment logic.
  → **Titania, Rugged Rumbler** [235]

Two supporting pieces landed with them. `WardCost.clause` is the self-contained verb phrase for a
cost ("discard a card", "pay {2}"), as opposed to `description`'s object phrase — a disjunction has
to render each option with its own verb, and the three ward renderers keep supplying the verb for
every other shape, so existing oracle text is unchanged. `WardCounterEffectExecutor.canPayWardCost`
is now the single source of truth for "unpayable ward cost → counter without a prompt": every
per-cost handler consults it, and `Choice` uses it to filter the options it offers.

Titania carries the *same printed shape* on two rails — `Costs.additional.DiscardOrPay("{2}")` for
the additional cost (`AdditionalCost.OrPay`, whose mana leg folds into the spell's own cost at cast
time) and `KeywordAbility.wardDiscardOrPay("{2}")` for the ward (`WardCost.Choice`, paid standing
alone as the trigger resolves). The facades are deliberately named to match; the types stay separate
because the rails genuinely differ.

Tests: `WardPlayerCountersTest` and `WardCostChoiceTest` (engine-level, per mechanic), plus
`TheSerpentSocietyScenarioTest` and `TitaniaRuggedRumblerScenarioTest`.

The mtgish emitter learned the disjunction (`_Cost: Or` → `KeywordAbility.wardChoice(...)`, via a new
`wardCostExpr` leg renderer in `CardStructure.kt`); every leg must render faithfully or the whole
ward line still declines to SCAFFOLD. The IR has no player-counter cost tag, so
`WardCost.PlayerCounters` has nothing to map to and is not taught.

## Missing keyword counters: haste, menace — 1 card ⛔

Nine of the eleven keyword counters exist and are wired. `HASTE` and `MENACE` are absent. Three lines
each: a `CounterType` entry + a `Counters` const in `mtg-sdk/.../core/CounterType.kt`, and an entry in
`KEYWORD_COUNTER_MAP` (`rules-engine/.../mechanics/layers/StateProjector.kt`).

Blocked card: **Super-Adaptoid** [250]. Everything else on it composes today.

## One-off blockers

Each of these is a single card needing one specific addition.

### Batched counters-placed trigger — **Invisible Woman, Sue Storm** [17]
"Whenever you put one or more +1/+1 counters on one or more **other Heroes** you control" is a CR
603.2c batch: one payoff per simultaneous placement. `Triggers.countersPlacedOn(...)` fires once **per
receiving permanent**, so it over-fires on exactly the plays this set is built around (Phil Coulson's
tap ability, Origin of the Avengers III). Needed: a `batch` flag on `CountersPlacedEvent`
(`mtg-sdk/.../scripting/events/EventFilters.kt`) plus a `TriggerDetector.detectCounterPlacementBatchTriggers`.
Direct analogues already exist as `OneOrMoreBecomeTapped` (`TapEvent(batch = true)` →
`detectTapBatchTriggers`) and `OneOrMorePermanentsEnter`.

### Per-permanent "you put counters on it this turn" memory — **Kid Loki** [63]
No turn-scoped counter-placement memory exists. `StatePredicate` has `EnteredThisTurn`,
`AttackedThisTurn`, `HasCounter`, `IsModified` — nothing about *when* counters arrived. Needed: a
`CountersPutOnThisTurnComponent` (mirroring `CardsDrawnThisTurnComponent`) written at the
counter-placement chokepoint and cleared by `TurnManager.startTurn`, plus
`StatePredicate.HadCountersPutOnThisTurn(counterType, placedBy)` and an
`ObjectFilter.hadCountersPutOnThisTurn()` builder. The "*you've* put" attribution is free —
`CountersPlacedEvent.placedBy` already carries the placer per CR 122.6a.

### Replacing a keyword action (connive) — **Leader, Super-Genius** [64]
"If a creature you control would connive, instead you draw a card, then that creature connives."
Connive is a *composed effect* (`Patterns.Hand.connive` / `Effects.Connive`), not an event the
replacement system can see; `ReplacementEffect` has no keyword-action variants at all (its nearest
neighbours are `ModifyDrawAmount`, `ModifyMillAmount`, `ModifyExplore`). Needed: either a
`ConniveEvent` `EventPattern` + `ReplacementEffect.ModifyConnive(prefixEffect)` read by the connive
executor, or a `ConniveModifier` static consulted at the same point. The card's second ability is fine
today.

### "Becomes the target of an ability you control" — **Loki, God of Mischief** [65]
Two independent gaps in `EventPattern.BecomesTargetEvent`: (1) **player targets are never emitted** —
`StackResolver` documents that it emits the event for permanent and spell targets only; needs emission
plus a player-aware branch in `TriggerMatcher.matchesBecomesTargetTrigger`; (2) **no abilities-only
filter** — the event has `spellsOnly` but no inverse. `byYou` and `firstTimeEachTurn` already exist.

### Counting coloured mana symbols in one object's cost — **Namor the Sub-Mariner** [69]
`EntityNumericProperty` exposes `ManaValue`, `ColorCount`, `SubtypeCount` — no pip count;
`DynamicAmount.DevotionTo` counts pips only across a player's whole battlefield. Needed:
`EntityNumericProperty.ColoredManaSymbolCount(colors)` reusing `DevotionTo`'s hybrid/Phyrexian rules,
plus `CardPredicate.ManaCostContainsSymbol(color)` for the trigger's filter (`withColor(BLUE)` tests
the card's *colour*, which is not the same thing). Namor's dynamic power is fine.

### Aggregate-metric cost + capped free cast — **Baron Helmut Zemo** [87]
Three gaps stack. (1) "Exile any number of black cards from your graveyard with **fifteen or more black
mana symbols** among their mana costs" — costs select by count and filter, never by a summed per-card
metric; structurally the same selection crew uses for total power, over the missing
`ColoredManaSymbolCount` above. (2) `CastAnyNumberFromCollectionWithoutPayingCostEffect`
(`mtg-sdk/.../scripting/effects/LibraryEffects.kt`) has no `maxCasts` field, so "cast up to three of the
copies" can't be bounded. (3) Not a blocker: the boast window is already
`ActivationRestriction.OncePerTurn` + `Conditions.SourceAttackedThisTurn`; only a cosmetic
`Keyword.BOAST` is absent.

### Capped free cast — **Doom Reigns Supreme** [96]
The same missing `maxCasts` on `CastAnyNumberFromCollectionWithoutPayingCostEffect` ("cast up to two
spells from among the exiled cards"). Everything else on the card — including the plan-counter
threshold — composes today.

### Mana restricted to equip abilities — **Ronin, Shadow Stalker** [112]
"Spend this mana only to cast Equipment spells **or activate equip abilities**". `ManaRestriction` has
`SubtypeSpellsOnly(setOf("Equipment"))` for the spell half and `AbilityActivationOnly` for *any*
activated ability; `AnyOf(...)` of the two would wrongly pay for every activated ability in play.
Needed: `ManaRestriction.EquipActivationOnly` plus an `isEquipActivation` flag threaded onto
`SpellPaymentContext` (the `ActivatedAbility.isEquipAbility` marker it would read already exists).
Everything else on the card is available.

### "Until the end of your next turn" continuous duration — **Evil's Thrall** [128]
`Duration` has `EndOfTurn`, `UntilYourNextTurn` (which ends at the *beginning* of your next turn — a
full turn short), `UntilNextEndStep` and `UntilYourNextUpkeep`, but no "until the end of your next
turn". The turn-keyed window exists only for play permissions (`MayPlayExpiry.UntilEndOfNextTurn`), not
for continuous effects, so `GainControlEffect` cannot express the conditional half. Needed:
`Duration.UntilEndOfYourNextTurn` plus its expiry sweep in `CleanupPhaseManager`.

### Repeatable optional payment feeding a modal — **Hawkeye, Master Marksman** [130]
"You may pay {1} **up to three times**. When you do, choose up to that many." `MayPayManaEffect` /
`ReflexiveTriggerEffect` handle a *single* optional payment; `RepeatDynamicTimesEffect` repeats a fixed
count. Nothing loops an optional payment, stops on decline, and publishes the number of times paid.
Per-mode `additionalManaCost` on `ModalEffect` is not an alternative — it is honoured only by
`CastSpellEnumerator` / `CastSpellHandler` at cast time, never during a triggered ability's resolution.
Needed: `RepeatOptionalPaymentEffect(cost, maxTimes, storeCountAs)` whose stored count feeds
`ModalEffect.dynamicChooseCount` (`chooseUpToDynamic` already accepts any `DynamicAmount`).

### Targeted proliferate — **Powerful Broker** [179]
`ProliferateEffect` is a bare `data object` with no target and no cap
(`mtg-sdk/.../scripting/effects/CounterEffects.kt`); the executor picks its own permanents at
resolution, so it cannot express a *targeted* single-object proliferate (which is respondable and
respects hexproof). Needed: a `target: EffectTarget? = null` field plus a branch in
`ProliferateExecutor` that skips the selection continuation when a target is supplied, and a "target
permanent or player" `TargetRequirement`.

### "Activate abilities as though they had haste" — **Shang-Chi, Master of Kung Fu** [187]
No static, keyword, or engine hook for this anywhere. The summoning-sickness gate for `{T}` abilities
is checked inline against `SummoningSicknessComponent` + `hasHaste` in `ManaSolver` and the ability
enumerators. Needed: a `MayActivateAbilitiesAsThoughHasty(filter: GroupFilter)` static in
`MiscStaticAbilities.kt` read at each of those sites. `GrantKeyword(HASTE, …)` is **functionally
wrong** — it would also lift the attack restriction. The card's mana ability is already fine.

### One-shot "next spell you cast is free" — **World War Hulk** [197]
The pending-rider family in `mtg-sdk/.../scripting/effects/StackEffects.kt` has
`CopyNextSpellCastEffect`, `MakeNextSpellUncounterableEffect` and `GrantNextSpellAffinityEffect` — but
no free-cast sibling. Needed: `CastNextSpellFreeEffect(spellFilter)` + a `PendingNextSpellFreeCast`
state record mirroring `rules-engine/.../state/PendingNextSpellAffinity.kt` (held on `GameState`,
consumed in `CastSpellHandler`, honoured by `CostCalculator.hasFreeCastPermission`). The existing static
`MayCastWithoutPayingManaCost` is battlefield-resident with no one-shot consumption, so a Saga chapter
cannot use it — even with `oncePerTurn` it would keep granting the free cast on chapters II and III.

### Disjunctive activated-ability cost — **Bullseye, Death Dealer** [209]
"Sacrifice an artifact **or** discard a nonland card" as an activation cost. `AbilityCost` has no
`Choice`/`AnyOf` variant, and neither does `CostAtom`. The two disjunctive cost types that exist serve
other slots: `PayCost.Choice` (consumed only by resolution-time `PayOrSufferEffect`) and
`AdditionalCost.Choice` (spell casts only). Needed: `AbilityCost.Choice(options: List<AbilityCost>)`
plus the payer/enumerator branch that offers the affordable options. The card's ETB half **is**
expressible today via `ReflexiveTriggerEffect` over `Effects.ChooseAction` with `FeasibilityCheck`s.

### Per-permanent "first time it became tapped this turn" — **Captain America, Living Legend** [210]
`EventPattern.TapEvent` carries only `filter`, `batch`, `tapper`, and no per-entity tap history exists.
The `firstTimeEachTurn` gate exists on `LifeGainEvent`, `BecameSaddledEvent`, `CountersPlacedEvent` and
`BecomesTargetEvent`, each backed by an event-specific `firstThisTurn` flag computed in
`TriggerMatcher`. Needed: a `BecameTappedThisTurnComponent` (cleared in `CleanupPhaseManager`), a
`firstThisTurn` field on `TappedEvent`, a `firstTimeEachTurn` field on `TapEvent`, and the matching
`TriggerMatcher` branch. The "during your turn" half is already `Conditions.IsYourTurn`.

### Linked exile from hand + return to origin zone — **Cloak and Dagger, Entwined** [211]
`ExileUntilLeavesEffect` accepts battlefield permanents and graveyard cards only — every other zone is
explicitly ignored. The hand branch *can* be linked via a
`GatherCards → SelectFromCollection → MoveCollectionEffect(linkToSource = true)` pipeline, and the
either/or choice via `Effects.ChooseAction`. **The real gap is the return side**:
`ReturnLinkedExileToHand` and `ReturnLinkedExileUnderOwnersControl` both act on the whole linked pile,
and nothing returns each card to the zone it came from. Needed: a `ReturnLinkedExileToOriginZone` effect
(or an origin-zone field on `LinkedExileComponent`), plus lifting the hand-zone restriction on
`ExileUntilLeaves` so the card reads as one primitive.

### Two additions on one card — **Storm, Windrider** [230]
1. **"those creatures gain flying"** — `Triggers.youCastSpellTargeting(filter)` exists, but nothing
   names *the targets of the triggering spell*. `EffectTarget` has `TriggeringEntity`, `CardSource` has
   `ChosenTargets` (this effect's own targets) — neither reaches the spell's targets. Cleanest fix: have
   the `SpellCastPredicate.TargetsMatching` matcher record the matching target ids into
   `TriggerContext.capturedEntityIds` (already plumbed to `PipelineState.TRIGGER_CAPTURED_COLLECTION` by
   `StackResolver`), after which the card is a plain `ForEachInCollection(…, GrantKeyword(FLYING, …))` —
   no new effect type.
2. **"Creatures with flying can't attack you"** — the only defender-relative attack restriction is
   `CantBeAttackedWithout(requiredKeyword, attackerFilter)`, which is *inverted*; `CantAttack(filter)` is
   global. Needs either a nullable `requiredKeyword` or a new `CantBeAttackedBy(attackerFilter)` static
   plus its branch in the attack-legality check. (The "or block creatures you control" half is already
   `CantBeBlockedBy`.)

### Per-turn "dealt damage this turn" predicate — **Red Guardian, Super-Soldier** [34]
"Destroy target creature an opponent controls **that dealt damage this turn**" — a per-turn record of
damage *inflicted*. Three near-misses, none usable:
`StatePredicate.WasDealtDamageThisTurn` is the **passive** direction (damage received);
`StatePredicate.HasDealtDamage` is the right direction but wrong duration —
`HasDealtDamageComponent` is documented and implemented as persisting for the permanent's
battlefield lifetime, never cleared at end of turn
(`rules-engine/.../battlefield/BattlefieldComponents.kt:998-1004`, and absent from
`CleanupPhaseManager`'s per-turn strip list), so it would let Red Guardian kill anything that ever
dealt damage in any earlier turn; and the remaining damage-history predicates
(`HasDealtCombatDamageToPlayer`, `DealtCombatDamageToSourceControllerThisTurn`,
`ControllerDealtCombatDamageBySourceThisTurn`) are combat- and recipient-specific, missing noncombat
damage and damage to creatures. Needed: `StatePredicate.DealtDamageThisTurn` plus a per-turn
`DealtDamageThisTurnComponent` stamped in `DamageUtils` / `CombatDamageManager` alongside
`HasDealtDamageComponent`, cleared in `CleanupPhaseManager`, and wired into `PredicateEvaluator`,
`AffectsFilterResolver`, `TriggerMatcher`, `BeginningPhaseManager` and `Serialization`.

### "Do this only once each turn" (CR 603.2h) — not the trigger cap — SHIPPED ✅ (2 cards unblocked)
> Whenever a creature you control is dealt damage, you may have The Sensational She-Hulk deal that
> much damage to any target. **Do this only once each turn.**

Shipped as `TriggeredAbility.effectOncePerTurn`, distinct from the existing `oncePerTurn` **trigger**
cap. CR 603.2h: *"A triggered ability may have an instruction followed by 'Do this only once each
turn.' This ability triggers only if its source's controller has not yet taken the indicated action
that turn."* So while the action is untaken every matching event triggers — in a multi-block every
damaged creature puts its own instance on the stack and the controller declines down the line to the
one carrying the number they want — and once the action is taken the ability stops triggering for the
turn, with instances already on the stack doing nothing as they resolve (Nykthos Paragon / Riveteers
Ascendancy rulings). The "you may" is answered as each instance resolves (Legolas, Counter of Kills
ruling).

`TriggerProcessor` lowers the flag into `Gate.OnceEachTurn` gates around the consent gate: a
`spend = false` check outside it (a used-up instance resolves silently instead of raising a pointless
yes/no) and the spending gate inside it (declining costs nothing). `GatedEffectExecutor` checks and
spends atomically against a `TriggeredAbilityEffectAppliedThisTurnComponent` on the source, cleared in
cleanup. Capped abilities are excluded from the batched may-question (one shared yes/no would take
away the choice of *which* instance to use), and once the action is taken, later matching events that
turn are dropped silently — they never triggered. See `docs/card-sdk-language-reference.md` §8 →
*`effectOncePerTurn`*.

Do not confuse this with the other wording: "**This ability triggers** only once each turn"
(Crossbones [91], Moon Girl [223], Knight of Wundagore [175], Ant-Man [201]) *is* a trigger cap and is
correctly `oncePerTurn = true` today. `EffectOncePerTurnTest` pins both behaviours side by side.

**Unblocked and implemented (2):** Jennifer Walters // The Sensational She-Hulk [18] ·
Baron Strucker, HYDRA Overlord [88] ("Whenever another Villain you control enters, you may have it
connive. Do this only once each turn." — with a trigger cap, two Villains entering together would
only trigger for the first, so you could not pick which one connives).

**Also migrated — seven pre-existing cards outside MSH (7).** They print "Do this only once each
turn" but carried `oncePerTurn = true`, so declining burned the turn's only fire (the Legolas ruling
says the opposite in as many words, and the Gatherer rulings on five of the seven say "once you
*choose* to …, that ability won't trigger again that turn"). All seven now use `effectOncePerTurn`,
each with a scenario test that declines one instance and takes a later one:
`big/cards/AncientCornucopia.kt` · `tla/cards/EarthKingdomGeneral.kt` ·
`dsk/cards/IrreverentGremlin.kt` · `ltr/cards/LegolasCounterOfKills.kt` ·
`tla/cards/PlanetariumOfWanShiTong.kt` · `spm/cards/SpiderVerse.kt` · `eoe/cards/Terrasymbiosis.kt`.

Planetarium of Wan Shi Tong drove one generalization of the lowering: its effect is
`Composite(look at top card, May(cast it))`, and its ruling ties the turn's use to the *cast*, not
the look. `withEffectBudgetGate` therefore searches the **tail** of a `CompositeEffect` for the
consent gate — the payoff of a "do X, then you may Y" instruction is its last step — so the spending
gate lands inside the "may" rather than around the whole composite.

### Power-only dynamic CDA granted for a duration — **Ms. Marvel, Kamala Khan** [67]
> Embiggen Fist — Whenever you cast a spell that targets a creature you control, draw a card. Until
> end of turn, Ms. Marvel gains "**Ms. Marvel's base power is equal to the number of cards in your
> hand.**"

The granted clause is a **characteristic-defining ability**: base power must keep tracking hand size
for the rest of the turn. `Effects.SetBasePower` is a one-shot resolution-time *set* —
`SetBaseStatsEffect` documents its `power` as "evaluated at resolution time" and is deliberately
distinct from the projector's `SetPowerToughnessDynamic`, which is "re-evaluated per affected entity
at projection time". So her power froze at whatever the hand was when the trigger resolved.
Reproduced: hand 8 → power 11 (8 base + Giant Growth's 3); after a draw, hand 9 → power still 11.

**Implemented then removed from this branch.** The right shape is
`Effects.GrantStaticAbility(<power-only dynamic CDA>, EffectTarget.Self, Duration.EndOfTurn)` —
`GrantStaticAbility` already exists, but there is no power-only dynamic CDA to hand it:
`SetBasePowerToughnessDynamicStatic` (`mtg-sdk/.../scripting/StatsStaticAbilities.kt`) sets **both**
stats from one `DynamicAmount`, which would clobber her printed toughness of 4. Needed: a
`SetBasePowerDynamicStatic` mirroring the existing toughness-only `SetBaseToughnessForCreatureGroup`,
plus its `StaticAbilityHandler` branch onto the projector's existing `SetPowerToughnessDynamic` path.
Narrow and reusable — any "power is equal to X" grant wants it.

Her other two lines are fine today: `NoMaximumHandSize` and
`Triggers.youCastSpellTargeting(Creature.youControl())`.

### Cost reduction reading the source's own characteristic — **The Scarlet Witch** [151]
"Instant and sorcery spells you cast with mana value 4 or greater cost {X} less to cast, where X is
The Scarlet Witch's power." The obvious recipe —
`ReduceGenericBy(GreatestPropertyAmongPermanentsYouControl(Power, Any.named("The Scarlet Witch")))` —
compiles but **always reduces by 0**. `CostCalculator.greatestPropertyAmongMatching`
(`rules-engine/.../mechanics/mana/CostCalculator.kt:524`) matches battlefield permanents with a
stripped-down `matchesBattlefieldPredicate` (same file, ~line 768) that handles only
`IsCreature` / `IsArtifact` / `IsEnchantment` / `IsLand` / `IsPermanent` / `HasSubtype` and falls
through to `else -> false` for everything else, including `CardPredicate.NameEquals`. It ignores
`statePredicates` too, so `.sourceItself()` is equally inert. The card would read right and resolve
wrong. Either fix works:
1. A `CostReductionSource.SourceProperty(EntityNumericProperty.Power)` evaluated against
   `context.sourceId` — the exact-fidelity option; or
2. extend `CostCalculator.matchesBattlefieldPredicate` to handle `CardPredicate.NameEquals` (the full
   matcher one function over, ~line 994, already does), which makes the recipe work via the legend rule.

No existing `CostReductionSource` gets the semantics right — the subtype-based approximations
("greatest power among Warlocks/Heroes/Mutants you control") over-reduce whenever another such
creature is bigger.

### Condition-gated flash grant — **Captain Mar-Vell, Space-Born** [12]
"Cosmic Awareness — As long as an opponent has cast a spell this turn, you may cast spells as though
they had flash." **No new SDK vocabulary is needed** — `GrantFlashToSpellType` and
`Conditions.CompareAmounts(DynamicAmounts.spellsCastThisTurn(Player.EachOpponent), GTE, 1)` both
exist. The blocker is plumbing: both flash-permission scans match the raw ability type and never
unwrap the `ConditionalStaticAbility` that `staticAbility { condition = … }` produces —
`rules-engine/.../handlers/actions/spell/CastZoneResolver.kt:551` and
`rules-engine/.../legalactions/utils/CastPermissionUtils.kt:528` both do
`for (ability in def.script.staticAbilities) { if (ability is GrantFlashToSpellType) … }`. A gated
grant is therefore *silently inert* — it never grants flash at all, rather than granting it
conditionally. Fix: route both loops through the existing
`CastPermissionUtils.activeStaticAbility(...)` helper (`CastPermissionUtils.kt:418`), which already
performs exactly this unwrapping for equip and play-from-top permissions. Roughly a two-line change,
and it likely fixes other gated permissions that are inert today.

### Damage replacement that heals previously-marked damage — **Wolverine, Fierce Fighter** [240]
"If damage would be dealt to Wolverine, instead that damage is dealt, but all other damage already
dealt to him is healed." Two gaps: (a) no SDK effect removes marked damage on demand — the
`without<DamageComponent>()` helper is reachable only through `RegenerateEffect` /
`RemoveDamageShieldEffect`, so an `Effects.RemoveAllDamage(target)` one-shot is needed; (b) every damage
replacement in `ReplacementEffect.kt` *changes* the damage (`PreventDamage`, `RedirectDamage`,
`DoubleDamage`, `ModifyDamageAmount`, `CapDamage`, `SetMinimumDamage`, `ReplaceDamageWith*`) — none lets
the damage through unchanged while running a rider; that shape exists only for zone changes
(`OnEnterRunEffect`). A triggered ability is **not** a substitute: state-based actions would see the
accumulated total and kill Wolverine before the trigger resolved.

---

## Small non-blocking additions worth folding in

These do not block any card outright, but the triage surfaced them as cheap correctness or fidelity
wins in whichever PR next touches the area:

- `Subtype.SYNTHEZOID` is missing from the constant list and `ALL_CREATURE_TYPES`. No MSH booster card
  needs it (Viv Vision is typed `Robot Hero`), but Vision, Synthezoid Avenger will.
- A `notAttacking()` builder on `ObjectFilter`/`TargetFilter`. Spider-Man, To the Rescue [228] currently
  hand-rolls `StatePredicate.Not(StatePredicate.IsAttacking)`, and the existing
  `mir/cards/Alarum.kt` silently drops its nonattacking restriction.
- `ReplacementEffect.ModifyCounterPlacement` has no `placedByYou` flag (its sibling
  `DoubleCounterPlacement` does). Doc Samson [164] is therefore modelled as the Winding Constrictor "if
  counters would be put" wording rather than the printed "**If you would put** …".
- `Conditions.SourceReceivedCounterThisTurn` matches any counter kind, not a specific one. Beast [206]
  reads "one or more **+1/+1** counters"; a `counterType` parameter would make it exact.
- `Keyword.EXTORT` does not exist. The Kingpin of Crime [220] composes it exactly as
  `Triggers.YouCastSpell` + `MayPayManaEffect("{W/B}", DrainLife(1))`; promote it to a real keyword when
  a second extort card lands.
- `Keyword.BOAST` does not exist either; the activation window composes from
  `ActivationRestriction.OncePerTurn` + `Conditions.SourceAttackedThisTurn`.
