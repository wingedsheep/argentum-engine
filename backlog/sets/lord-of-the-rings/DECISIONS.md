# LTR "Implement all remaining cards" — Decisions Log

Branch: `ltr-all-cards`. Goal: implement every unchecked card in `cards.md`, one commit per
card, faithful to oracle text (`ltr_set.json`) and the Comprehensive Rules
(`MagicCompRules_20260417.pdf`). Engine gaps are listed in `TODO.md`; this log records the
concrete decisions made per card as they are implemented.

Workflow note: per the user, this is a single sweeping effort on one branch (`ltr-all-cards`),
**one commit per card** (not one-PR-per-gap as the original TODO suggested). When a card needs a
new engine primitive, the primitive + card + tests + SDK-reference update land together in that
card's commit (or a small group commit when several cards share one freshly built primitive).

## Order of attack

Roughly grouped so that cards sharing a freshly built engine gap land together. See per-card
entries below for the actual decisions.

---

## Per-card decisions

### Bill the Pony (White) — Gap 27, assign combat damage by toughness

- **Oracle:** ETB create two Food; "Sacrifice a Food: Until end of turn, target creature you control
  assigns combat damage equal to its toughness rather than its power."
- **Engine gap:** the existing `AssignDamageEqualToToughness` is a *static* ability read off a card's
  printed statics by `CombatDamageUtils`; there was no turn-scoped, granted form.
- **Decision:** add a new `AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS` and grant it via the
  existing `Effects.GrantKeyword(AbilityFlag, target, Duration.EndOfTurn)` floating-effect path
  (Layer.ABILITY → projected keywords). `CombatDamageUtils.assignsDamageAsToughness` now short-circuits
  to `true` when the creature carries that projected flag — **unconditional** (no `toughness > power`
  gate), matching Bill's wording. This composes with the whole existing GrantKeyword machinery rather
  than adding a bespoke effect/executor; mirrors the Gap-39-style "grant a combat flag" pattern.
- **Composition:** ETB = `Effects.CreateFood(2)`; cost = `Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food"))`;
  target = `Targets.CreatureYouControl`.
- **Touched:** `AbilityFlag.kt` (+flag), `CombatDamageUtils.kt` (+projected-flag check), client
  `enums.ts` (AbilityFlag mirror + display name), card + `BillThePonyScenarioTest`, SDK reference.

### Slip On the Ring (White) — Gap 14, flicker (NO engine change)

- **Oracle:** "Exile target creature you own, then return it to the battlefield under your control.
  The Ring tempts you."
- **Decision:** Gap 14 was already obsolete — the flicker composes from existing primitives. The
  in-set **Meneldor, Swift Savior** does the identical "exile creature you own, return under your
  control" via `Effects.Move(EXILE).then(Move(BATTLEFIELD))`. Because the target is restricted to a
  creature *you own*, returning it under its owner's control = under your control. Add the
  `Effects.TheRingTemptsYou()` rider. No new effect/executor; no dedicated scenario test (snapshot
  net + the proven primitive cover it).
- **Composition:** `TargetCreature(filter = TargetFilter(GameObjectFilter.Creature.ownedByYou()))`
  + `Effects.Composite(Move EXILE, Move BATTLEFIELD, TheRingTemptsYou)`.

### Dreadful as the Storm (Blue) — Gap 13, set base P/T (facade only)

- **Oracle:** "Target creature has base power and toughness 5/5 until end of turn. The Ring tempts you."
- **Decision:** Gap 13 was engine-landed already — `SetBasePowerToughnessEffect` and its registered
  `SetBasePowerToughnessExecutor` exist; only the DSL facade was missing. Added
  `Effects.SetBasePowerAndToughness(power, toughness, target, duration)` (mirrors `SetBasePower`).
  Card composes it (5/5, EndOfTurn) with `Effects.TheRingTemptsYou()`. This also partially unblocks
  Frodo, Sauron's Bane. Scenario test asserts base P/T 5/5.
- **Triage note:** the parallel Explore triage over-claimed composability — Elrond (needs an
  "Nth ability resolution this turn" condition), Dúnedain Rangers (needs a player-level "you control a
  Ring-bearer" condition), Frodo Baggins (needs a conditional "must be blocked" static), Ringwraiths
  (graveyard-functional trigger) all genuinely need new primitives. The TODO gap list is largely
  ACCURATE; only Gap 14 (flicker) and Gap 13 (set base P/T, facade) were obsolete. **Do not trust a
  COMPOSABLE verdict without grepping the named primitive.**

### Battle-Scarred Goblin (Red) — Gap 15 (deal damage to each blocker)

- **Oracle:** "Whenever this creature becomes blocked, it deals 1 damage to each creature blocking it."
- **Decision:** the group-damage primitive `Patterns.Group.dealDamageToAll(n, GroupFilter)` and the
  `Triggers.BecomesBlocked` trigger already existed; the only gap was a *source-relative* "blocking
  THIS creature" filter (the existing `IsBlocking` matches any blocker in combat, which would wrongly
  hit blockers of other attackers). Added `StatePredicate.IsBlockingSource` (mirrors the existing
  source-relative `InSameBandAsSource`): a blocker whose `BlockingComponent.blockedAttackerIds`
  contains `PredicateContext.sourceId`. Plus filter builder `GameObjectFilter.Creature.blockingSource()`.
  `ForEachInGroup`→`findMatchingOnBattlefield` already threads the effect's sourceId into the
  predicate context, so it resolves correctly. Card = `Triggers.BecomesBlocked` +
  `Patterns.Group.dealDamageToAll(1, GroupFilter(Creature.blockingSource()))`.
- **Touched (exhaustive-when fan-out):** `StatePredicate.kt` (+predicate), `ObjectFilter.kt`
  (+`blockingSource()`), `PredicateEvaluator.kt` (real eval), `AffectsFilterResolver.kt` (inert/false
  in projection), `BeginningPhaseManager.kt` + `TriggerMatcher.kt` (no-constraint lists), card +
  `BattleScarredGoblinScenarioTest` (proves only its blockers die), SDK reference.
- **Reusable for:** any "becomes blocked → affect each blocker" card.

### Witch-king, Bringer of Ruin (Black, Extra) — Gap 36 (least-power filter)

- **Oracle:** "Flying. Whenever Witch-king attacks, defending player sacrifices a creature with the
  least power among creatures they control."
- **Decision:** added `StatePredicate.HasLeastPower` + `GameObjectFilter.Creature.hasLeastPower()`,
  mirroring the existing `HasGreatestPower` (controller-relative min, ties qualify). Edict reuses
  `Effects.Sacrifice(filter, 1, target)`. "Defending player" modeled as `Player.EachOpponent` per the
  committed engine convention (Agate Blade Assassin) — correct for 2-player, non-targeted.
- **Touched (exhaustive-when fan-out):** `StatePredicate.kt`, `ObjectFilter.kt`, `PredicateEvaluator.kt`
  (min/≤ eval), `AffectsFilterResolver.kt` (+`hasLeastPowerInProjection`), `BeginningPhaseManager.kt`
  + `TriggerMatcher.kt` (no-constraint lists), card + scenario test (unique-min auto-sacrifice), SDK ref.
- **Reusable for:** any "least power" target/edict (also helps Witch-king of Angmar / Shadowfax power
  comparisons later).

### Trailblazer's Boots (Artifact, Extra) — Gap 24 (nonbasic landwalk)

- **Oracle:** "Equipped creature has nonbasic landwalk. Equip {2}."
- **Decision:** added `Keyword.NONBASIC_LANDWALK` + an evasion branch in
  `BlockEvasionRules.LandwalkRule` (`playerControlsNonbasicLand` = a controlled land with
  `typeLine.isLand && !isBasicLand`). Card grants it via the standard
  `GrantKeyword(Keyword.NONBASIC_LANDWALK, Filters.EquippedCreature)` static + `equipAbility("{2}")`.
  Client `enums.ts` Keyword enum + display name updated.
- **Touched:** `Keyword.kt`, `BlockEvasionRules.kt`, client `enums.ts`, card + scenario test (equips
  via the equip activated ability, asserts block illegal vs a nonbasic land and legal vs only basics),
  SDK reference.

### Voracious Fell Beast (Black) — Gap 16 (count of permanents sacrificed by an effect)

- **Oracle:** "Flying. When this creature enters, each opponent sacrifices a creature of their choice.
  Create a Food token for each creature sacrificed this way."
- **Decision:** added `DynamicAmount.PermanentsSacrificedThisWay` (facade
  `DynamicAmounts.permanentsSacrificedThisWay()`) → reads `EffectContext.sacrificedPermanents.size`.
  The Gap-17 sacrifice-snapshot work already injects sacrifices into the resolving context so a
  sibling rider can read them. Card = ETB `Effects.Sacrifice(Creature, 1, EachOpponent)` then
  `CreatePredefinedTokenEffect("Food", dynamicCount = permanentsSacrificedThisWay())`.
- **Touched:** `DynamicAmount.kt`, `DynamicAmountEvaluator.kt` (exhaustive when),
  `DynamicAmounts.kt` facade, card + scenario test (1 Food when opp sacrifices, 0 when no creature),
  SDK ref. Note: 2-player → at most 1 sacrifice from the edict (each opponent sacrifices one).

### Gollum's Bite + Gollum, Patient Plotter (Black) — Gap 11 OBSOLETE (no engine change)

- **Finding:** Gap 11 (activated abilities from the graveyard) is fully engine-landed —
  `ActivatedAbility.activateFromZone = Zone.GRAVEYARD`, `Costs.ExileSelf`, the dedicated
  `GraveyardAbilityEnumerator`, and sorcery-speed gating all exist, with template cards Bonebind
  Orator (exile-from-GY cost) and Undead Gladiator (sorcery-speed return-from-GY).
- **Gollum's Bite** (Instant): `spell { Effects.ModifyStats(-2,-2, target) }` + a graveyard
  `activatedAbility { cost = Composite(Mana("{3}{B}"), Costs.ExileSelf); activateFromZone = GRAVEYARD;
  timing = SorcerySpeed; effect = TheRingTemptsYou() }`.
- **Gollum, Patient Plotter** (Creature 3/1): `LeavesBattlefield → TheRingTemptsYou`, plus a graveyard
  `activatedAbility { cost = Composite(Mana("{B}"), Costs.Sacrifice(Creature)); activateFromZone =
  GRAVEYARD; timing = SorcerySpeed; effect = Move(Self, Zone.HAND) }`. Scenario test (GameTestDriver,
  mirroring UndeadGladiatorTest) proves return-from-GY by sacrifice + sorcery-speed restriction.
- No new SDK; one commit per card.

### Ringwraiths (Black, Extra) — Gap 21 OBSOLETE + Barad-dûr (Land) — Gap 25 OBSOLETE

- **Probe finding:** Gap 21 (graveyard TRIGGERED ability) is landed — `TriggeredAbility.activeZone` /
  DSL `triggerZone = Zone.GRAVEYARD` (templates Pyre Zombie, Persistent Marshstalker). Gap 25 ({X} in
  an activated-ability cost) is landed — X-cost activation continuation threads X into
  `DynamicAmount.XValue` (templates Atalya, Soul Burn). Gap 22 (restricted mana) is also landed
  (`ManaRestriction` + `ManaSpellRider`) — but NOT a "legendary spells only" variant (Great Hall /
  Delighted Halfling still need a new `ManaRestriction` for supertype=legendary). Gap 8 protection
  partially: `ProtectionScope.Everything`/`CardType` exist on permanents, but PLAYER protection
  (The One Ring) and dynamic chosen-card-type grant (Pippin) are missing.
- **Ringwraiths:** ETB `ModifyStats(-3,-3)` then `ConditionalEffect(TargetMatchesFilter(legendary,0),
  LoseLife(3, ControllerOf("target creature")))` — legendary check is mid-resolution so the targeted
  creature is still present (SBAs run after). Graveyard half: `Triggers.RingTemptsYou` with
  `triggerZone = Zone.GRAVEYARD` → `Move(Self, HAND)`. Scenario test covers legendary vs non-legendary.
- **Barad-dûr:** Legendary Land modeled on Mines of Moria — `EntersTapped(unlessCondition = Exists(You,
  BATTLEFIELD, Creature.legendary()))`, `{T}: AddMana(BLACK)`, and
  `{X}{X}{B},{T}: Effects.Amass(XValue, "Orc")` gated by
  `ActivationRestriction.OnlyIfCondition(Conditions.CreatureDiedThisTurn)`. No dedicated test (all
  pieces individually tested; snapshot covers registration).
- No new SDK for either; one commit per card.

### Great Hall of the Citadel + Delighted Halfling — Gap 22 partial (legendary-spells-only mana)

- **Decision:** Gap 22's mana-restriction framework (`ManaRestriction` + `ManaSpellRider`) existed but
  lacked a "legendary spells only" variant. Added `ManaRestriction.LegendarySpellsOnly` + a
  `SpellPaymentContext.isLegendary` field (populated from `cardComponent.typeLine.isLegendary` at all 5
  spell-cast context sites: 2 in CastSpellHandler, 3 in CastSpellEnumerator) + the matcher case in
  `ManaPool.isSatisfiedBy` (`!isAbilityActivation && isLegendary`).
- **Great Hall** (Land): `{T}: AddColorlessMana(1)`; `{1},{T}: AddManaInAnyCombination(2, restriction =
  LegendarySpellsOnly)`.
- **Delighted Halfling** (1/2): `{T}: AddColorlessMana(1)`; `{T}: AddManaOfChoiceEffect(AnyColor,
  Fixed(1), LegendarySpellsOnly, riders = {MakesSpellUncounterable})`.
- **Touched:** `ManaRestriction.kt`, `ManaPool.kt` (+field, +matcher), `CastSpellHandler.kt` +
  `CastSpellEnumerator.kt` (populate isLegendary), 2 cards + `ManaSpendRestrictionLegendaryTest`
  (matcher unit test: pays legendary, not non-legendary, not abilities), SDK ref. ManaRestriction is
  server-side only → no client change.

## Session checkpoint (2026-06-13) — resumption notes

This session landed 7 cards (each one commit, each fully tested + snapshot-blessed + SDK-ref
updated), taking LTR from 234→241/291: **You Cannot Pass!, Gwaihir the Windlord, Ringsight,
Call of the Ring, Gandalf's Sanction, Troll of Khazad-dûm, Phial of Galadriel.**

The remaining 44 each need a *non-trivial* engine feature (the easy/contained ones are now done).
Triaged difficulty for whoever resumes:

- **Needs a "controller of context target N" Player reference** (usable in group filters / edicts —
  one feature unblocks two cards): **Fear, Fire, Foes!** ("1 damage to each other creature with the
  same controller" as the target) and **Breaking of the Fellowship** ("…another target creature that
  player controls"). No `ControllerOfTarget` player ref exists today.
- **Needs spell-copy extensions:** **Display of Power** (multi-target copy via ForEachTargetEffect over
  the existing `CopyTargetSpell` + a brand-new "this spell can't be copied" static — no uncopiable
  concept exists yet), **Gandalf the Grey** (modal "choose one not yet chosen" + copy).
- **Needs a reveal-until-N-of-type engine** (place matched, rest to bottom random; dynamic N): **The
  Ring Goes South**, **Sméagol, Helpful Guide** (opponent variant, put under your control), **Radagast
  the Brown** (look at top X, reveal-if-no-shared-type), **Sauron, the Necromancer** (copy GY creature
  as a token). Each varies in placement; a parameterized reveal-until effect would unlock several.
- **Needs a card-type-choice decision + client UI:** **Pippin, Guard of the Citadel** (protection from
  chosen card type — `ProtectionScope.CardType` exists; the *choice* decision does not).
- **Needs counter-type-choice + keyword counters (Gap 7):** **Aragorn, Company Leader**.
- **Player protection from everything (Gap 8):** **The One Ring** (rest composes).
- **Big bespoke / standalone systems:** **Grond** (Vehicles/Crew), **King of the Oathbreakers**
  (phasing), **Shelob** / **Sauron, the Dark Lord** / **Saruman of Many Colors** / **Tom Bombadil** /
  **Sharkey** / **Goldberry** / **Bewitching Leechcraft** / **Sauron's Ransom** / **Faramir, Prince of
  Ithilien** (delayed "choose an opponent" trigger) / **One Ring to Rule Them All** (Saga, mill =
  Ring-bearer power) / **Lost Isle Calling** (extra-turn + last-known source counters) / **Hew the
  Entwood** / **Ent-Draught Basin** & **Grishnákh** (target filter referencing the paid X / amassed
  power, during targeting) / **Glamdring** (cast-without-paying gated on combat damage) / **Press the
  Enemy** / **Flame of Anor** (conditional modal count) / **Galadriel of Lothlórien** (scry → reveal
  top, land → bf tapped) / **Frodo, Sauron's Bane** (type/ability morph + win condition) / **Éowyn,
  Lady of Rohan** (equip-cost reduction + begin-combat modal) / **Witch-king of Angmar** ("dealt combat
  damage to you this turn" combat-history filter + discard→indestructible) / **Barrow-Blade**
  (blocks/blocked-by → strip abilities of the *other* creature) / **Shadowfax** (put-from-hand tapped &
  attacking, power comparator) / **Sting** (Gap 29 conditional keyword from combat subtype) / **Peregrin
  Took** (token-creation rider) / **Palantír of Orthanc** (opponent-makes-may + MV-sum mill) / **Fires
  of Mount Doom** (impulse-play + destroy attached equipment) / **The Balrog, Durin's Bane** (cost
  reduction per permanent sacrificed this turn + can't-be-blocked-except-by-legendary + death trigger) /
  **Gollum, Scheming Guide** (opponent guesses land/nonland).

## Verified gap-status of remaining cards (probed, NOT yet implemented)

These were checked against the real SDK this session. Each needs the **small** new primitive noted
(the rest composes), unless marked bigger. Use this to resume without re-probing:

- **Boromir, Warden of the Tower** — needs a *triggering-spell*-relative "no mana was spent to cast it"
  condition. `Conditions.NoManaSpentToCast` exists but is SOURCE-relative (reads the ability source's
  own cast record), so it can't gate on the opponent's triggering spell. `Effects.CounterTriggeringSpell()`
  and group-indestructible (`ForEachInGroup(GroupFilter.AllCreaturesYouControl, GrantKeyword(INDESTRUCTIBLE))`)
  both exist. ADD: a trigger-spell-relative no-mana-spent condition.
- **Frodo Baggins** — needs a `MustBeBlocked` STATIC ability (only `MustBlock` static + `MustBeBlockedEffect`
  effect exist) to wrap in `ConditionalStaticAbility(_, Conditions.SourceIsRingBearer)`. ETB-tempt-on-legendary
  composes.
- **Dúnedain Rangers** + **Frodo Baggins (tempt)** — need a Ring-bearer `GameObjectFilter`/predicate for a
  player-level "you (don't) control a Ring-bearer" condition. None exists (only source-level
  `SourceIsRingBearer`). ADD: a `RingBearer` filter predicate.
- **Barrow-Blade** — `Triggers.BlocksOrBecomesBlockedBy(filter)` + `Effects.RemoveAllAbilities(TriggeringEntity)`
  exist, but the facade hardcodes `binding = SELF`; needs an ATTACHED binding (for the equipped creature) and
  the detector must populate `triggeringEntity` = the partner under ATTACHED. ADD/verify: binding param +
  detector support.
- **Riders of the Mark** — Affinity(`KeywordAbility.AffinityForSubtype(Subtype.HUMAN)`), trample/haste,
  `SourceAttackedThisTurn`, ReturnToHand(Self) all exist; the subtlety is "create tokens = its toughness"
  AFTER bouncing itself (last-known toughness once it leaves). Needs a last-known-toughness capture (or a
  reflexive that reads it before the move).
- **Mount Doom** — needs "destroy all creatures except up to two chosen" (no destroy-all-except primitive).
- **The Grey Havens** — needs "add one mana of a color among cards in a zone matching a filter" (graveyard
  legendary-creature colors) mana ability.
- **Sting, the Glinting Dagger** — needs a static granting a keyword conditioned on "blocking or blocked by a
  creature matching a filter (Goblin/Orc)" (Gap 29).
- **Shadowfax** — needs "put a creature from hand onto the battlefield tapped AND attacking" (combat-assignment
  on a put-from-hand) + power comparator.
- **Bewitching Leechcraft** — needs a custom untap-replacement ("if it would untap, remove a +1/+1 counter
  instead; if you do, untap it").
- **Ent-Draught Basin** / **Grishnákh** — both blocked on the same gap: a TARGET filter referencing a dynamic
  value (the paid X / the amassed Army's power) DURING targeting. `{X}` in activated cost itself works.
- **Glamdring / Galadriel of Lothlórien / Press the Enemy / Flame of Anor** — `CastFromCollectionWithoutPayingCost`
  and `MayCastWithoutPayingManaCost` exist; the open question is the dynamic MV/zone filters and (Galadriel)
  a "reveal top, if land put onto battlefield tapped" pattern — verify next session.

Confirmed-OBSOLETE gaps this session: 11 (graveyard-activated), 13 (set base P/T facade), 14 (flicker),
21 (graveyard-triggered), 25 ({X} in activated cost), 22-partial (added LegendarySpellsOnly).

### Dúnedain Rangers + Frodo Baggins (Gap 18 — Ring-bearer player-state)

- **Dúnedain Rangers:** added `StatePredicate.IsRingBearer` (checks `RingBearerComponent` + controller
  == ownerId) + `GameObjectFilter.Creature.ringBearer()`. Landfall trigger with intervening-if
  `Conditions.YouControl(Creature.ringBearer(), negate = true)` → `Effects.TheRingTemptsYou()`.
  Exhaustive-when fan-out: PredicateEvaluator + AffectsFilterResolver (real check), BeginningPhaseManager
  + TriggerMatcher (no-constraint lists).
- **Frodo Baggins:** ETB-on-legendary tempt via `Triggers.entersBattlefield(Creature.legendary().youControl(),
  binding = ANY)`. "Must be blocked if able while your Ring-bearer" → new `MustBeBlocked` static ability
  (counterpart of `MustBeBlockedEffect`), wrapped in `ConditionalStaticAbility(_, SourceIsRingBearer)`.
  `BlockPhaseManager.attackersWithMustBeBlockedStatic` scans attackers' card statics (unwrapping
  ConditionalStaticAbility, evaluating the gate with the attacker as source) and merges into both
  `findMustBeBlockedAttackers` (allCreatures) and `findMustBeBlockedIfAbleAttackers`.
- Tests: GameTestDriver, designating the Ring-bearer by tempting via Birthday Escape. Frodo test proves
  declare-no-blockers is illegal while Frodo is the Ring-bearer (legal otherwise). Dúnedain test proves
  landfall tempts only when you control no Ring-bearer.
- ConditionInterface is a type-alias for `Condition`, so `Conditions.*` values pass into
  `ConditionalStaticAbility(condition = …)` directly.
- **Frodo debugging note:** the Ring's level-1 temptation already grants "can't be blocked by greater
  power", so a must-be-blocked test on a 1/1 Ring-bearer needs a power-≤1 blocker (Savannah Lions, not
  Grizzly Bears). Engine was correct; test was wrong. Also: test-JVM stderr doesn't reach gradle
  console — use file-based diagnostics.

### Boromir, Warden of the Tower (White) — counter free spells

- **Oracle:** "Vigilance. Whenever an opponent casts a spell, if no mana was spent to cast it, counter
  that spell. Sacrifice Boromir: Creatures you control gain indestructible until end of turn. The Ring
  tempts you."
- **Decision:** the existing `NoManaSpentToCast` is SOURCE-relative; added the triggering-spell
  counterpart `Conditions.TriggeringSpellCastWithoutPayingMana` (reads `context.triggeringEntityId`'s
  `CastRecordComponent`, mirror of `evaluateNoManaSpentToCast`). Trigger =
  `Triggers.OpponentCastsSpell` + that intervening-if + `Effects.CounterTriggeringSpell()`. Sac ability =
  `Costs.SacrificeSelf` → `ForEachInGroup(AllCreaturesYouControl, GrantKeyword(INDESTRUCTIBLE))` then
  `TheRingTemptsYou()`. Test: free Mox Ruby ({0}) is countered; paid Grizzly Bears resolves.

### The Grey Havens (Land) — Gap 22 (graveyard-derived mana)

- **Oracle:** "ETB scry 1. {T}: Add {C}. {T}: Add one mana of any color among legendary creature cards
  in your graveyard."
- **Decision:** added `ManaColorSet.AmongCardsInGraveyard(filter)` + resolver `amongCardsInGraveyard`
  (iterates `state.getGraveyard(controllerId)`, matches filter, collects each card's base colors) +
  facade `Effects.AddManaOfColorAmongGraveyard(filter)`. New ManaColorSet variant → also added the
  branch to `LandManaColorInspector`'s exhaustive when. ETB scry composes via `Patterns.Library.scry(1)`.
  Test: a mono-green legendary in GY yields green.

### Glorious Gale (Blue) — composable (no engine change)

- **Oracle:** "Counter target creature spell. If it was a legendary spell, the Ring tempts you."
- **Decision:** `spell { target("creature spell", Targets.CreatureSpell); effect =
  ConditionalEffect(TargetMatchesFilter(GameObjectFilter.Any.legendary(), 0), TheRingTemptsYou())
  .then(CounterSpell()) }`. The legendary check runs while the spell is still on the stack (legendary
  is intrinsic, so evaluating it before the counter is functionally identical to printed order).
  Test (ScenarioTestBase + castSpellTargetingStackSpell): legendary Naban → countered + temptCount 1;
  nonlegendary Grizzly Bears → countered + temptCount 0.

### Mount Doom (Land) — composable (no engine change)

- **Oracle:** "{T}, Pay 1 life: Add {B} or {R}. {1}{B}{R}, {T}: deal 1 to each opponent. {5}{B}{R}, {T},
  Sacrifice Mount Doom and a legendary artifact: Choose up to two creatures, then destroy the rest."
- **Decision:** all composable. Mana ability = `Composite(Tap, PayLife(1))` →
  `AddManaOfChoice(ManaColorSet.Specific({B,R}))`. Damage = `DealDamage(1, PlayerRef(EachOpponent))`
  (Erebor Flamesmith pattern). Wrath = the Duneblast pipeline with `ChooseUpTo(2)` + `storeRemainder`
  → `MoveCollection(Destroy)`; cost = `Composite(Mana, Tap, SacrificeSelf, Sacrifice(Artifact.legendary()))`,
  sorcery-speed. No new SDK; snapshot only.

### Riders of the Mark (Red, Extra) — composable (no engine change)

- **Oracle:** Affinity for Humans; trample, haste; "At your end step, if it attacked this turn, return
  it to hand. If you do, create 1/1 white Human Soldier tokens equal to its toughness."
- **Decision:** `keywordAbility(KeywordAbility.AffinityForSubtype(Subtype.HUMAN))` + trample/haste;
  `YourEndStep` trigger with intervening-if `Conditions.SourceAttackedThisTurn` →
  `ReturnToHand(Self).then(CreateToken(count = DynamicAmounts.sourceToughness(), 1/1 white Human Soldier))`.
  `sourceToughness()` reads last-known toughness post-bounce (same mechanism as Heartfire Hero's
  source-power-on-death). Test: attacking Riders bounces + makes 4 tokens; non-attacking stays.

### Rangers of Ithilien (Blue) — power-comparison target filter

- **Oracle:** "Vigilance. When this creature enters, gain control of up to one target creature with
  lesser power for as long as you control this creature. Then the Ring tempts you."
- **Decision:** added strict `CardPredicate.PowerLessThanEntity(reference)` +
  `ObjectFilter/TargetFilter.powerLessThanEntity()` (the engine had ≤ via `powerAtMostEntity`; "lesser"
  is strict <). Real eval in PredicateEvaluator; no-op `-> false` in the other exhaustive CardPredicate
  whens (PredicateEvaluator projection list, TriggerMatcher, AffectsFilterResolver, CostCalculator).
  Card = ETB `target(powerLessThanEntity(Source))` + `GainControl(_, WhileYouControlSource("Rangers of
  Ithilien"))` then `TheRingTemptsYou()`. Test: 3/3 Rangers takes a 2/2. Also helps Grishnákh / other
  power-comparison cards.

### Phial of Galadriel (Artifact) — Gap 38 (conditional draw + life-gain replacements)

- **Oracle:** "If you would draw a card while you have no cards in hand, draw two instead. If you
  would gain life while you have 5 or less life, gain twice that much instead. {T}: Add one mana of
  any color."
- **Decision:** both replacements compose. Draw half = existing `ModifyDrawAmount(modifier=1,
  restrictions=[CardsInHandAtMost(0)], appliesTo=DrawEvent(You))`. Life half needed a condition gate:
  added `restrictions: List<Condition>` to `ModifyLifeGain` (mirrors `ModifyDrawAmount.restrictions`),
  evaluated in `LifeGainModifiers.apply` via a per-recipient `EffectContext` + `ConditionEvaluator`
  (same pattern as `DrawReplacementDispatcher`). `restrictions=[LifeAtMost(5)]`. Mana ability =
  `AddManaOfChoiceEffect(AnyColor, 1)`. Empty `restrictions` → no behavior change (Leyline of Hope /
  Alhammarret's Archive unaffected).
- **Test:** draw with empty hand → 2; with a card in hand → 1 (not doubled); gain 3 at life 5 → +6;
  gain 3 at life 10 → +3.

### Troll of Khazad-dûm (Black) — Gap 23 (typecycling) + min-blocker static

- **Oracle:** "Can't be blocked except by three or more creatures. Swampcycling {1}."
- **Decision:** Gap 23 (typecycling) was already landed — `KeywordAbility.typecycling("Swamp", "{1}")`
  expresses swampcycling (searches for a card with the Swamp type). The block restriction needed a
  new static: added `CantBeBlockedByFewerThan(minBlockers, filter)` — a generalization of menace
  (the minBlockers = 2 case). Enforced in `BlockPhaseManager.validateMinBlockersRequirements`
  (mirrors `validateMenaceRequirements`): an attacker carrying it may be unblocked, but if blocked
  needs ≥ minBlockers blockers. Added to the `-> null` group in `StaticAbilityHandler` (enforced at
  declaration, not as a continuous projection). Card = `CantBeBlockedByFewerThan(3)` + swampcycling.
- **Test:** Troll blocked by 2 → illegal; by 3 → legal; unblocked → legal.

### Gandalf's Sanction (Multicolor) — Gap 34 (excess damage redirected to controller)

- **Oracle:** "Deals X damage to target creature, where X is the number of instant and sorcery cards
  in your graveyard. Excess damage is dealt to that creature's controller instead."
- **Decision:** added `DealDamageEffect.excessToController` + facade `Effects.DealDamageExcessToController`.
  The engine already computes `creatureExcessDamage` (CR 120.4a) in `DamageUtils.dealDamageToTarget`;
  the new flag (a) marks the creature with only the lethal portion (`effectiveAmount - excess`) and
  (b) deals the excess to the creature's controller (a recursive player-damage call, same source,
  flag off). X = `DynamicAmounts.zone(You, GRAVEYARD, InstantOrSorcery).count()`. Threaded through
  `DealDamageExecutor`. Regression-checked CombatLethalDamageTest (trample/excess paths unaffected).
- **Test:** 4 I/S in GY → 4 damage to a 2/2 → creature dies (lethal 2) and its controller loses 2
  life (excess).

### Call of the Ring (Black) — Gap 38 (choose-a-Ring-bearer trigger)

- **Oracle:** "At the beginning of your upkeep, the Ring tempts you. Whenever you choose a creature
  as your Ring-bearer, you may pay 2 life. If you do, draw a card."
- **Decision:** the engine already emits `RingTemptedEvent(playerId, temptCount, bearerId, source)`
  from `RingTemptContinuationResumer`. Added a `requireBearerChosen` flag to the
  `EventPattern.RingTemptedEvent` (TriggerMatcher checks `event.bearerId != null` when set) + a new
  `Triggers.WheneverYouChooseRingBearer` facade. So the second ability fires only when a creature is
  actually designated (not when you control none). Upkeep half = `Triggers.YourUpkeep` +
  `TheRingTemptsYou()`. "May pay 2 life, if you do draw" = `MayEffect(Composite(LoseLife(2), Draw(1)))`
  (the established Raiders' Spoils pattern). No serialization/registration churn (EventPattern is
  `@Serializable`).
- **Test:** upkeep tempt with Grizzly Bears present → choose it → pay 2 life → draw (life 18, hand 1,
  temptCount 1); upkeep tempt with no creatures → temptCount 1 but no trigger (no decision, life 20).

### Ringsight (Multicolor) — shares-color-with-controlled-permanent tutor filter

- **Oracle:** "The Ring tempts you. Search your library for a card that shares a color with a
  legendary creature you control, reveal it, put it into your hand, then shuffle."
- **Decision:** added `CardPredicate.SharesColorWithPermanentYouControl(filter)` + filter builder
  `sharingColorWithPermanentYouControl(filter)`. Eval: candidate shares ≥1 projected color with any
  battlefield permanent the evaluating controller controls matching the inner filter. Card composes
  `TheRingTemptsYou() then Patterns.Library.searchLibrary(Any.sharingColorWithPermanentYouControl(
  Creature.legendary()), reveal=true, dest=HAND)`. CardPredicate fan-out (exhaustive whens):
  PredicateEvaluator main (real eval) + projection-list (false), AffectsFilterResolver (false),
  TriggerMatcher (false), CostCalculator (true/permissive). `applyTextReplacement` propagates to the
  inner filter.
- **Test:** mono-blue legendary Naban → only the mono-blue library card (Glorious Gale) is eligible,
  not the mono-green Grizzly Bears; Ring temptation picks the Ring-bearer first, then the search runs.

### Gwaihir the Windlord (Multicolor) — Gap 30 (cost reduction on game history)

- **Oracle:** "Costs {2} less to cast as long as you've drawn two or more cards this turn. Flying,
  vigilance. Other Birds you control have vigilance."
- **Decision:** the cost-reduction machinery (`ModifySpellCost(SelfCast, ReduceGeneric(2),
  gating = CostGating.OnlyIf(condition))`) already existed; the only gap was a "you've drawn N+
  cards this turn" condition. Added `PlayerDrewCardsThisTurn(player, atLeast)` (facade
  `Conditions.YouDrewCardsThisTurn(atLeast)`), reading the existing per-player
  `CardsDrawnThisTurnComponent` (Gap-2 infra, reset for all players at turn start). One new
  `ConditionEvaluator` branch — conditions are `@Serializable`/`@SerialName` sealed, so no manual
  serialization registration. Anthem "Other Birds you control have vigilance" composes via
  `GrantKeyword(VIGILANCE, GroupFilter(Creature.youControl().withSubtype("Bird"), excludeSelf=true))`.
- **Test infra:** added a reusable `ScenarioBuilder.withCardsDrawnThisTurn(player, count)` fixture
  helper. `GwaihirTheWindlordScenarioTest` proves castable on 4 mana after 2 draws, not after 1.

### You Cannot Pass! (White) — Gap 36 (combat-history target filter)

- **Oracle:** "Destroy target creature that blocked or was blocked by a legendary creature this turn."
- **Decision:** added `StatePredicate.BlockedOrWasBlockedByLegendaryThisTurn` (History) +
  `GameObjectFilter.Creature.blockedOrWasBlockedByLegendaryThisTurn()`, backed by a new
  per-creature marker `BlockedOrWasBlockedByLegendaryThisTurnComponent`. The marker is stamped in
  `BlockPhaseManager.commitBlockDeclaration` (the single sync + tax-resume commit point): for each
  blocker↔attacker pairing, if the partner is `projected.isLegendary(...)` *at declaration time*,
  the creature is marked. Capturing legendary-ness at pairing time (not at resolution) makes the
  spell still able to target the creature after the legendary partner leaves the battlefield or
  loses legendary-ness — matching the card's ruling. Cleared at end-of-turn cleanup
  (`CleanupPhaseManager`, alongside `WasDealtDamageThisTurnComponent`).
- **Exhaustive-when fan-out:** PredicateEvaluator (real check), AffectsFilterResolver (real check —
  a battlefield permanent can carry the marker), TriggerMatcher (`-> true` no-gate group),
  BeginningPhaseManager `matchesStatePredicateForUntap` (`-> true`). Registered in `Serialization.kt`.
- **Test gotcha:** after `declareBlockers` in `ScenarioTestBase`, priority sits with the *blocking*
  (non-active) player, so the active player must `passPriority()` once before casting the instant.
  `BlockedOrWasBlockedByLegendaryThisTurnScenarioTest` covers both marker directions + a bystander
  that never fought a legendary (illegal target). Card = `spell { target(...blockedOrWas...) ;
  Effects.Destroy(t) }`.
- **Reusable for:** Witch-king of Angmar will need a *different* combat-history filter (dealt combat
  damage to you this turn), so that gets its own primitive later.

### Elrond, Lord of Rivendell (Blue) — composable (no engine change)

- **Oracle:** "Whenever Elrond or another creature you control enters, scry 1. If this is the second
  time this ability has resolved this turn, the Ring tempts you."
- **Decision:** `Triggers.entersBattlefield(Creature.youControl(), binding = ANY)` →
  `Patterns.Library.scry(1).then(ConditionalEffect(Conditions.SourceAbilityResolvedNTimes(2),
  TheRingTemptsYou()))`. `SourceAbilityResolvedNTimes` already exists (Tannuk Memorial Ensign). No new
  SDK; snapshot only (the "Nth-resolution" gap the triage flagged was already obsolete).


---

## Session: ltr-all-cards sweep (continued 2026-06)

> Goal: implement every remaining unchecked LTR card, one commit per card, building
> whatever engine primitives are required. Decisions for each card below.

### Display of Power (Red) — Gap 9 (copy a spell on the stack)

- **Oracle:** "This spell can't be copied. Copy any number of target instant and/or sorcery
  spells. You may choose new targets for the copies."
- **`cantBeCopied` flag:** added a card-level `cantBeCopied: Boolean` (CardBuilder → CardScript),
  mirroring the existing `cantBeCountered`. `GameInitializer` (and the `ScenarioTestBase`
  test-card builder) attach a new `CantBeCopiedComponent` marker. The single chokepoint
  `StackResolver.putSpellCopy` short-circuits to success-with-no-copy when the source carries
  the marker (CR 707.10 — "if a spell can't be copied, no copy is created"), so *every* copy
  path (Storm, CopyTargetSpell, CopyEachTargetSpell, CopyNextSpellCast) honours it for free.
- **CopyEachTargetSpellEffect:** new stack effect (+ `Effects.CopyEachTargetSpell` facade) that
  reads **every** `ChosenTarget.Spell` chosen for the resolving spell and copies each in turn.
  Per copy that has its own targets it pauses with a `ChooseTargetsDecision` (new
  `CopyEachSpellContinuation`, resumed in `MiscContinuationResumer`); untargeted spells and
  modal spells are copied verbatim (inheriting modes/targets — a legal "decline to retarget").
  Reuses `StackResolver.putSpellCopy` + `StormCopyEffectExecutor.applyCopyMutations`.
- **Targeting:** added `unlimited` param to the `TargetSpell` factory and a
  `Targets.AnyNumberOfInstantOrSorcerySpells` requirement ("any number of target instant and/or
  sorcery spells").
- **Tests:** `DisplayOfPowerScenarioTest` — copy one targeted Bolt and retarget the copy; the
  spell carries `CantBeCopiedComponent`; copy two Bolts at once (4 Bolts → 12 damage).

### Legolas, Master Archer (Green) — Gap 15 + cast-targeting triggers

- **Oracle:** "Reach. Whenever you cast a spell that targets Legolas, put a +1/+1 counter on
  Legolas. Whenever you cast a spell that targets a creature you don't control, Legolas deals
  damage equal to its power to up to one target creature."
- **New cast-time predicates:** added `SpellCastPredicate.TargetsSource` ("targets this") and
  `SpellCastPredicate.TargetsMatching(filter)` ("targets a [filter]"), matched in
  `TriggerMatcher.matchesSpellCastPredicate` (now passed the trigger `sourceId`/`controllerId`):
  it reads the just-cast spell's `TargetsComponent` and checks the chosen permanent/spell targets
  against the source id or the `GameObjectFilter` (via `PredicateEvaluator` + projected state).
  Facades `Triggers.youCastSpellTargetingSource()` / `Triggers.youCastSpellTargeting(filter)`.
- **Damage clause:** composes existing `DealDamage(DynamicAmounts.sourcePower(), upToOneTargetCreature)`
  — the damage source defaults to Legolas (the trigger source), so no new effect was needed
  (the Gap-15 "creature deals its power" infra already exists via `DealDamageEffect.damageSource`).
- **Test:** `LegolasMasterArcherScenarioTest` — Giant Growth on Legolas adds a counter; Giant Growth
  on an opponent's creature fires the damage trigger, killing a 1/1.

### Breaking of the Fellowship (Red) — Gap 15 (composable, no engine change)

- **Oracle:** "Target creature an opponent controls deals damage equal to its power to another
  target creature that player controls. The Ring tempts you."
- **Decision:** rather than build relational cross-target filters, modeled as a single two-target
  requirement `TargetCreature(count = 2, filter = CreatureOpponentControls, sameController = true)`
  — `sameController` enforces "that player controls" and count=2 enforces "another" (distinct
  targets). Effect: `DealDamage(targetPower(0), ContextTarget(1), damageSource = ContextTarget(0))`
  then `TheRingTemptsYou()`. The damage-source/dynamic-power infra (Gap 15) already existed.
- **Test:** `BreakingOfTheFellowshipScenarioTest` — a 3/3 deals 3 to a 2/2 (dies), dealer survives.

### Barrow-Blade (Artifact) — Gap 26 (composable + ATTACHED block trigger)

- **Oracle:** "Equipped creature gets +1/+1. Whenever equipped creature blocks or becomes
  blocked by a creature, that creature loses all abilities until end of turn. Equip {1}."
- **Decision:** the `RemoveAllAbilities` effect and `BlocksOrBecomesBlockedBy` trigger both
  already existed, but the trigger only supported `SELF` binding. Added `ATTACHED`-binding
  support so an Equipment fires off its *equipped* creature's combat: gave the
  `Triggers.BlocksOrBecomesBlockedBy` facade a `binding` param, and taught three engine sites
  to resolve ATTACHED → the equipped creature — `TriggerIndex.triggerToCategories` (index it
  under BLOCKERS_DECLARED instead of the attachment path), `TriggerMatcher.matchesTrigger`
  (both the top-level ATTACHED guard and the `BlocksOrBecomesBlockedByEvent` branch read the
  `AttachedToComponent` target), and `TriggerDetector`'s per-partner block loop. The partner
  is the `TriggeringEntity`, so the card is just `RemoveAllAbilities(TriggeringEntity, EndOfTurn)`.
- **Test:** `BarrowBladeScenarioTest` — equip, attack, opponent blocks with a Wind Drake; after
  the trigger resolves the Drake has lost flying.

### Reveal-to-battlefield family — `MoveCollection.filter` + `ZonePlacement.Tapped`

These three share one small pipeline addition: an optional `filter` on `MoveCollectionEffect`
(move only the matching cards from a gathered pile; the rest stay), composed with the existing
`ZonePlacement.Tapped` (battlefield-tapped entry) and `ToZone.player` (controller override).
`GatherUntilMatchEffect` already supported a dynamic `count` and a `player` (incl. `TargetOpponent`).
(Equivalent to a `FilterCollection` partition; the inline filter just avoids an intermediate name.)

- **Galadriel of Lothlórien** — Ring-tempt→scry 3 (`YouChoseOtherCreatureAsRingBearer` + `scry(3)`);
  `WheneverYouScry` → `MayEffect(GatherTop(1) → Reveal → MoveCollection(filter=Land → battlefield
  tapped))`. Test triggers the scry via Temple of Mystery's ETB scry.
- **Sméagol, Helpful Guide** — end-step Ring tempt (`ControlledCreatureDiedThisTurn`); `RingTemptsYou`
  → target opponent, `GatherUntilMatch(Land, player=TargetOpponent)` → Reveal → land → your
  battlefield tapped (`ToZone(BATTLEFIELD, You, Tapped)` = control override), rest → their graveyard.
  Test triggers the tempt via Birthday Escape.
- **The Ring Goes South** — `TheRingTemptsYou().then(GatherUntilMatch(Land, count = legendary
  creatures you control) → Reveal → lands → battlefield tapped → rest → library bottom random)`.
  Confirms a Ring tempt can chain into later effects (siblings resume after the Ring-bearer choice).

### The One Ring (Artifact) — Gap 8, player-level protection from everything

- **Oracle:** "Indestructible. When The One Ring enters, if you cast it, you gain protection from
  everything until your next turn. At the beginning of your upkeep, you lose 1 life for each burden
  counter on The One Ring. {T}: Put a burden counter on The One Ring, then draw a card for each
  burden counter on The One Ring."
- **Engine gap (Gap 8):** there was no *player-level* protection. Added `Effects.GrantPlayerProtection(
  scope = ProtectionScope.Everything, duration = UntilYourNextTurn, target = Controller)` →
  `GrantPlayerProtectionEffect` + `GrantPlayerProtectionExecutor`, which adds/merges a
  `PlayerProtectionComponent(scopes, removeOn)` on the player. Reuses the existing SDK
  `ProtectionScope` sealed type (so `Everything`/`Color`/`EachOpponent`/… all compose).
- **Enforcement:** new `PlayerProtectionRules.isProtectedFromSource(state, player, source, caster)`
  is the single source of truth consulted by (1) `TargetValidator` (reject a `ChosenTarget.Player`
  the source is protected from), (2) `TargetEnumerationUtils` (drop the player from `TargetPlayer`/
  `TargetOpponent`/`AnyTarget`/… enumeration), and (3) `DamageUtils` (prevent damage from a matching
  source). For a player only the **D**amage and **T**argeting parts of DEBT are meaningful.
- **Duration:** new `PlayerEffectRemoval.UntilYourNextTurn` — `CleanupPhaseManager` clears the
  component on the same post-untap hook as floating `Duration.UntilYourNextTurn` effects (the active
  player's next untap). `PlayerProtectionComponent` registered in `engineSerializersModule`.
- **ETB gate:** "if you cast it" = `triggerCondition = Conditions.WasCast` (CR 603.4 intervening-if).
- **Composition:** burden upkeep loss = `LoseLife(countersOnSelf(burden), Controller)` on
  `YourUpkeep`; `{T}` = `Composite(AddCounters(BURDEN,1,Self), DrawCards(countersOnSelf(burden)))`
  (sequential resolution reads the post-increment count). `Indestructible` is a plain keyword.
- **Test:** `TheOneRingScenarioTest` — (1) casting it grants the controller protection so an
  opponent's Lightning Bolt can't target them; (2) `{T}` adds a burden + draws, and the controller's
  next upkeep loses 1 life for that counter.

### A-The One Ring (Extra, Alchemy) — reuses Gap 8

- **Oracle:** identical to [The One Ring] except the draw ability costs `{1}, {T}` (the Alchemy nerf).
- **Decision:** straight copy of `TheOneRing` with the activated-ability cost changed to
  `Costs.Composite(Costs.Mana("{1}"), Costs.Tap)`. No new engine work — Gap 8 protection already landed.
- **Test:** `AOneRingScenarioTest` — `{1},{T}` adds a burden + draws when a land is available, and
  fails to activate with no mana.

### Glorfindel, Dauntless Rescuer (Green) — Gap 39, granted "can't be blocked by more than one"

- **Oracle:** "Whenever you scry, choose one and Glorfindel gets +1/+1 until end of turn.
  • Glorfindel must be blocked this turn if able. • Glorfindel can't be blocked by more than one
  creature each combat this turn."
- **Engine gap (Gap 39):** `AbilityFlag.CANT_BE_BLOCKED_BY_MORE_THAN_ONE` was defined but enforced
  nowhere. Wired it into `BlockPhaseManager.validateMaxBlockersRequirements`: a projected grant of the
  flag now caps blockers at one, taking the smaller of {printed `CantBeBlockedByMoreThan` static, the
  granted flag}. (Also made the static lookup tolerate token attackers with no `cardDef`.)
- **Card shape:** the +1/+1 always happens (it sits before the bullets), so it is folded into each
  mode of `ModalEffect.chooseOne(...)` — exactly one mode is chosen, so the pump fires once. Modeling
  it as the **top-level** modal effect (not `Composite(pump, modal)`) is what reaches the engine's
  resolution-time modal-decision path (`ModalEffectExecutor` → `ChooseOptionDecision`). Mode 1 =
  `MustBeBlockedEffect(Self, allCreatures = false)`; mode 2 =
  `GrantKeyword(CANT_BE_BLOCKED_BY_MORE_THAN_ONE, Self, EndOfTurn)`.
- **Test:** `GlorfindelDauntlessRescuerScenarioTest` — Temple of Mystery's ETB scry triggers the
  ability; choosing mode 2 grants +1/+1 (3/2→4/3) and the flag, and the opponent can't assign two
  blockers to attacking Glorfindel.

### Pippin, Guard of the Citadel (Azorius) — Gap 8 extended: protection from a chosen card type

- **Oracle:** "Vigilance, ward {1}. {T}: Another target creature you control gains protection from
  the card type of your choice until end of turn."
- **Engine gap (Gap 8, card-type variant):** the repo already had protection from color (floating)
  and from subtype/supertype (static `ProtectionComponent`), but nothing granted protection from a
  *card type*, and there was no resolution-time card-type choice. Added a self-contained
  `Effects.GrantProtectionFromChosenCardType(target, duration)`: its executor presents a
  `ChooseOptionDecision` over the fixed protectable card-type set (Artifact, Creature, Enchantment,
  Instant, Land, Planeswalker, Sorcery, Battle) and pushes a `ChooseCardTypeForProtectionContinuation`;
  the resumer grants a floating `PROTECTION_FROM_CARDTYPE_<TYPE>` keyword (new
  `SerializableModification`/`Modification.GrantProtectionFromCardType` → `EffectApplicator`). Chose a
  dedicated self-contained effect rather than a "ChooseCardTypeThen" combinator because the option set
  is fixed/shared, so there is no per-card variation to compose under (mirrors the chosen-color shape
  in flow, not in needing a wrapper).
- **Enforcement:** parallel CARDTYPE checks added at every site that already honored supertype/subtype
  protection — `TargetValidator` (ability targeting), `StackResolver` (spell targeting, with a printed
  card-type fallback for stack spells not in the projection), `DamageUtils` (damage prevention),
  `CombatDamagePipeline` + `CombatDamageManager` (combat damage), and a new `ProtectionFromCardTypeRule`
  in `BlockEvasionRules` (can't be blocked by). The source's card types come from
  `ProjectedState.getTypes`.
- **Intentionally skipped:** the "can't be enchanted or equipped by anything of that type" clause is
  reminder text. No existing protection-attach legality check exists for color/subtype/supertype either
  (the equip/aura attach executors don't consult protection), so adding one would be a new system out of
  scope for this card — left unenforced to match the existing protection behavior.
- **Card shape:** `keywords(Keyword.VIGILANCE)` + `keywordAbility(KeywordAbility.ward("{1}"))`;
  activated ability `cost = Costs.Tap`, `target = Targets.OtherCreatureYouControl`,
  `effect = Effects.GrantProtectionFromChosenCardType()`.
- **Test:** `PippinGuardOfTheCitadelScenarioTest` — activating `{T}` on a Grizzly Bears and choosing
  Creature grants `PROTECTION_FROM_CARDTYPE_CREATURE`, then (1) a Hill Giant's combat damage is
  prevented when the Bears block it, and (2) the protected Bears can't be blocked by the Giant.

### Aragorn, Company Leader (Selesnya) — Gap 7: keyword counters + "choose a kind of counter"

- **Oracle:** "Whenever the Ring tempts you, if you chose a creature other than Aragorn as your
  Ring-bearer, put your choice of a counter from among first strike, vigilance, deathtouch, and
  lifelink on Aragorn. Whenever you put one or more counters on Aragorn, put one of each of those
  kinds of counters on up to one other target creature."
- **Keyword counters:** first strike / deathtouch / lifelink were already wired in
  `KEYWORD_COUNTER_MAP` (StateProjector); only **vigilance** was missing — added the `VIGILANCE`
  `CounterType` enum value + `Counters.VIGILANCE` constant, the map entry, and the full frontend
  counter wiring (`enums.ts` enum + display name, `counterManaClass: ability-vigilance`, plus
  `getVigilanceCounters`/`getDeathtouchCounters` helpers, badge styles, and GameCard.tsx badge
  blocks — deathtouch had no badge either, added both). A counter of these kinds grants the keyword
  via the projection (CR 122.1d), no static ability needed.
- **"Put your choice of a counter from among …":** reused `Effects.ChooseAction` (the existing
  `ChooseActionEffect`) — a resolution-time `ChooseOptionDecision` over four `EffectChoice`s, each
  `AddCounters(<kind>, 1, Self)`. No new effect type needed; the choice-of-counter-kind reads as a
  choice-of-action over the fixed four kinds, which composes cleanly and is reusable.
- **"Whenever you put one or more counters on Aragorn":** added `Triggers.CountersPlacedOnThis`
  (SELF-bound `CountersPlacedEvent`, `Counters.ANY`). The `TriggerMatcher.CountersPlacedEvent` branch
  previously ignored `binding`; added `SELF`/`OTHER` entity restriction there (general fix) and a
  `binding` parameter on `Triggers.countersPlacedOn`. The payoff is a `CompositeEffect` of one
  `AddCounters` per named kind onto `TargetOther(TargetCreature(optional))` — "up to one OTHER target
  creature" (excludes Aragorn, declinable). "Those kinds" = the four fixed kinds, not whatever was
  just placed, so all four are always added.
- **Test:** `AragornCompanyLeaderScenarioTest` — (a) a Ring tempt with a different Ring-bearer lets
  you choose a counter kind and put it on Aragorn (verifies the first strike counter + projected
  FIRST_STRIKE keyword); (b) the resulting counter triggers the second ability, putting one of each
  of the four kinds on a targeted Bear (verifies all four counters + projected first strike/deathtouch).

### Sting, the Glinting Dagger — Gap 29: conditional keyword grant gated on combat partner's subtype

- **Oracle:** "Equipped creature gets +1/+1 and has haste. At the beginning of each combat, untap
  equipped creature. Equipped creature has first strike as long as it's blocking or blocked by a
  Goblin or Orc. Equip {2}." ({2} Legendary Artifact — Equipment.)
- **Composed pieces (no new types):** `ModifyStats(+1, +1, Filters.EquippedCreature)` +
  `GrantKeyword(Keyword.HASTE, Filters.EquippedCreature)` for "+1/+1 and has haste"; a
  `Triggers.EachCombat` triggered ability with `Effects.Untap(EffectTarget.EquippedCreature)` for the
  begin-of-each-combat untap; `equipAbility("{2}")` for Equip {2}.
- **Gap 29 — new condition `SourceIsBlockingOrBlockedBySubtype(subtypes)`** (in `SourceConditions.kt`,
  facade `Conditions.SourceIsBlockingOrBlockedBySubtype(listOf(...))`): a reusable source-relative
  combat condition. "It" resolves through the source — an Equipment/Aura reads its attached creature
  (so it gates a static granted to the equipped creature), a creature source uses itself. True iff
  that creature is currently in a combat pairing with a creature of one of the given subtypes,
  checking **both** directions (`BlockingComponent.blockedAttackerIds` + `BlockedComponent.blockerIds`)
  and matching partner subtypes via **projected** state. Wrapped in `ConditionalStaticAbility` around
  `GrantKeyword(FIRST_STRIKE, EquippedCreature)`. Evaluated under projection, so the keyword is present
  in `ProjectedState.hasKeyword(...)` that `CombatDamageManager` reads when assigning first-strike
  damage — no combat-damage-path change needed.
- **DSL note:** the facade takes `List<Subtype>` rather than `vararg Subtype` because `Subtype` is an
  inline value class (varargs of value classes are prohibited by the Kotlin compiler).
- **Test:** `StingTheGlintingDaggerScenarioTest` — (1) equip grants +1/+1 and haste; (2) the
  begin-combat trigger untaps a tapped equipped creature; (3) the equipped creature gains projected
  FIRST_STRIKE when blocked by a Goblin (Goblin Guide); (4) it does NOT gain first strike when blocked
  by a non-Goblin/Orc (Savannah Lions).

### Frodo, Sauron's Bane — class-up Citizen → Scout → Rogue + Ring-tempt-count condition

- **Oracle:** {W} Legendary Creature — Halfling Citizen, 1/2.
  "{W/B}{W/B}: If Frodo is a Citizen, it becomes a Halfling Scout with base power and toughness 2/3
  and lifelink. {B}{B}{B}: If Frodo is a Scout, it becomes a Halfling Rogue with 'Whenever this
  creature deals combat damage to a player, that player loses the game if the Ring has tempted you
  four or more times this game. Otherwise, the Ring tempts you.'"
- **Composed pieces (mostly existing):** modelled like Figure of Fable — each ability checks Frodo's
  current subtype on resolution (per the Figure of Fable ruling), so the intervening-if is an
  in-effect `ConditionalEffect(Conditions.SourceHasSubtype(...), then)`, NOT an activation
  restriction; the ability is always legal but no-ops when the type is wrong.
  - **Scout step:** `Effects.BecomeCreature(power=2, toughness=3, keywords={LIFELINK},
    creatureTypes={"Halfling","Scout"}, duration=Permanent)`. Setting subtypes to {Halfling, Scout}
    swaps the Citizen class while keeping Halfling.
  - **Rogue step:** `Effects.SetCreatureSubtypes({"Halfling","Rogue"}, Permanent)` **then**
    `GrantTriggeredAbilityEffect(rogueAbility, Self, Permanent)`. Deliberately does NOT use
    `BecomeCreature` — "becomes a Halfling Rogue" prints no new P/T, so the Rogue step must leave the
    2/3 base-P/T continuous effect from the Scout step in place. The granted trigger is
    `DealsCombatDamageToPlayer → ConditionalEffect(RingHasTemptedYouAtLeast(4),
    then=LoseGame(TriggeringPlayer), else=TheRingTemptsYou)`.
- **New condition `RingHasTemptedPlayerAtLeast(times, player)`** (in `TurnConditions.kt`, facade
  `Conditions.RingHasTemptedYouAtLeast(times)`): reads the cumulative `temptCount` on the player's
  `TheRingComponent` (treats no emblem as 0), evaluated in `ConditionEvaluator` next to the existing
  Ring-bearer conditions. Reusable for any "the Ring has tempted you N+ times this game" payoff.
- **Hybrid mana:** `Costs.Mana("{W/B}{W/B}")` parses hybrid; payable with either white or black.
- **Test:** `FrodoSauronsBaneScenarioTest` — (1) {W/B}{W/B} makes a 2/3 Halfling Scout with lifelink;
  (2) the ability no-ops once he's no longer a Citizen; (3) {B}{B}{B} makes a Halfling Rogue keeping
  2/3 (and no-ops before he's a Scout); (4) Rogue combat damage with tempt count 4 makes the defender
  lose the game; (5) with tempt count 3 it tempts the attacker instead (count → 4, defender survives).

## King of the Oathbreakers (Gap 32 — phasing, CR 702.26)

- **Phasing already existed in the engine** (`Effects.PhaseOut` → `PhasedOutComponent`,
  `GameState.getBattlefield` hides phased-out permanents from projection/combat/targeting/SBAs,
  `BeginningPhaseManager.performUntapStep` phases them back in and emits `PhasedInEvent`). No new
  phasing core was needed — only the two missing trigger shapes.
- **"Becomes the target of a spell" (not abilities):** added `spellsOnly` to
  `EventPattern.BecomesTargetEvent` and `sourceIsSpell` to the engine `BecomesTargetEvent` (set true
  at the spell-cast and copy-of-spell emit sites in `StackResolver`, left false for activated/triggered
  abilities). New facade `Triggers.BecomesTargetOfSpell(filter)`. The ANY binding + filter "a Spirit
  you control" covers both "King … or another Spirit you control" halves (King is itself a Spirit you
  control). `Effects.PhaseOut(EffectTarget.TriggeringEntity)` phases out exactly the targeted permanent;
  the targeting spell then has no legal target and fizzles.
- **"Phases in" trigger:** new `EventPattern.PhasesInEvent(filter?)` + matcher branch (mirrors the
  TapEvent matcher) + `TriggerContext` mapping (`triggeringEntityId = event.entityId`) + facade
  `Triggers.PhasesIn(filter?)`. King makes a **tapped** 1/1 white flying Spirit token on each phase-in
  (added a `tapped` param to the int-count `Effects.CreateToken` facade).
- **No anthem/ward:** the real oracle is only Flying + the two triggers — the prompt's "Spirit anthem
  / ward {2}" paraphrase was wrong and was discarded after reading the JSON oracle text.
- **Test:** `KingOfTheOathbreakersScenarioTest` — Giant Growth targeting our own King phases it out
  (gone from `getBattlefield`, `PhasedOutComponent` stamped with the controller), the spell fizzles,
  King phases back in on its controller's next untap step, and the phase-in trigger makes one tapped
  1/1 white Spirit token.

## Flame of Anor ({1}{U}{R} Instant)

- **Conditional modal count "Choose one. If you control a Wizard as you cast this spell, you may
  choose two instead."** — modeled as a *cast-time* `dynamicChooseCount` on `ModalEffect`, not a new
  effect type. Authored via `modal(chooseCount = 2, minChooseCount = 1, dynamicChooseCount = …)` with
  `DynamicAmount.Conditional(Conditions.YouControlAtLeast(1, GameObjectFilter.Creature.withSubtype("Wizard")),
  ifTrue = Fixed(2), ifFalse = Fixed(1))`. All three pieces (Conditional, YouControlAtLeast, withSubtype)
  already existed — no new SDK primitive needed for the card itself.
- **Engine change (reused field, new evaluation site):** `dynamicChooseCount` previously only fed the
  *resolution-time* path (`ModalEffectExecutor`, where it forces `minChooseCount = 0` — the
  `chooseUpToDynamic` / Riku shape). For modal *spells* the count is locked at cast time, so I extended
  `CastSpellHandler.effectiveModalChooseCounts` to evaluate `dynamicChooseCount` against the cast-time
  battlefield and return `minChooseCount to eval.coerceIn(minChooseCount, modes.size)` — keeping the
  mandatory floor (choose at least one) instead of allowing decline-to-zero. This drives both the
  cast-time mode picker and `validateChosenModeShape`, so submitting two modes with no Wizard is
  rejected ("too many"), and with a Wizard both modes are legal. "As you cast" timing is honored
  because mode selection happens at cast time (CR 601.2b).
- **DSL plumbing:** added a `dynamicChooseCount` param to the `modal { }` builder + `ModalBuilder`
  (previously only the raw `ModalEffect` constructor / `chooseUpToDynamic` factory exposed it).
- **Modes** compose existing facades: `Effects.DrawCards(2, targetPlayer)`, `Effects.Destroy(targetArtifact)`,
  `Effects.DealDamage(5, targetCreature)`.
- **Test:** `FlameOfAnorScenarioTest` — no-Wizard: two modes illegal, one mode resolves (5 damage kills
  a Centaur), cast-time picker offers no second pick; with-Wizard: two modes legal (draw 2 + 5 damage
  both resolve), cast-time picker offers a second pick with "Done".

## Gandalf the Grey

- **Card:** `{3}{U}{R}` Legendary Creature — Avatar Wizard, 3/4. "Whenever you cast an instant or
  sorcery spell, choose one that hasn't been chosen —" with four modes (tap/untap a permanent; 3
  damage to each opponent; copy a controlled instant/sorcery; put Gandalf on top of its library).
- **New SDK primitive — modal "choose one that hasn't been chosen":** added
  `ModalEffect.excludePreviouslyChosenModes` (+ `chooseOneNotYetChosen(*modes)` factory). When set,
  the engine remembers which mode indices the *source permanent* has chosen across the game in a new
  per-source `ChosenModesEverComponent` (battlefield component, NOT cleared at end of turn) and
  excludes them from every later presentation. When all modes are used the ability resolves as a
  no-op. The memory rides on object identity (CR 700.4), so the fourth mode (Gandalf → library)
  resets it by remaking Gandalf as a new object. `ModalEffectExecutor.execute` filters
  `availableIndices`; `ModalAndCloneContinuationResumer.resumeModal` records the chosen index via a
  new `recordChosenModesOnSource` flag threaded through `ModalContinuation`.
- **Modes compose existing facades:** `MayEffect(ChooseAction([Tap, Untap]))` for "you may tap or
  untap target permanent"; `DealDamage(3, PlayerRef(EachOpponent), damageSource = Self)`;
  `Effects.CopyTargetSpell()` (Gap 9 — same copy-with-retarget infra as Display of Power) targeting
  `Targets.InstantOrSorcerySpellYouControl`; `PutOnTopOfLibrary(EffectTarget.Self)`.
- **P/T correction:** the card is 3/4 (Scryfall), not 4/4.
- **Test:** `GandalfTheGreyScenarioTest` — casting an instant triggers the modal; the damage mode hits
  the opponent (3 + Bolt 3 = 6); the damage mode is absent from a later trigger's options
  ("hasn't been chosen", 4 → 3 modes); the copy mode copies the Bolt still on the stack for +3.

## Press the Enemy (Gap 10)

- **Card:** `{2}{U}{U}` Instant. "Return target spell or nonland permanent an opponent controls to
  its owner's hand. You may cast an instant or sorcery spell with equal or lesser mana value from
  your hand without paying its mana cost."
- **New SDK primitive — `Effects.ReturnSpellOrPermanentToOwnersHand(target)`**
  (`ReturnSpellOrPermanentToOwnersHandEffect`, `@Serializable` data class in `StackEffects.kt`).
  Bounces ONE target to its owner's hand, dispatching on what it resolves to: a **spell on the
  stack** is removed from the stack to hand (does not resolve; not a counter, CR 701.27/701.5b) —
  reusing the proven `ReturnSpellToOwnersHandExecutor` logic — and a **permanent** delegates to the
  existing `MoveToZoneEffectExecutor(HAND)` so it shares all leave-the-battlefield cleanup. This is
  the bounce counterpart to `PutOnLibraryPositionOfChoiceEffect` (Swat Away), which already handled
  the dual spell/permanent case for library placement. Executor registered in `StackExecutors`.
  Reusable for any "return target spell or nonland permanent to hand" card.
- **Why a new effect and not `Effects.ReturnToHand`:** the stack is a separate `state.stack` list,
  NOT part of `state.zones`, so `MoveToZoneEffectExecutor.findEntityZone` returns null for a spell
  on the stack and `ReturnToHand` would error. The dual-dispatch executor handles both.
- **Free cast composes existing pipeline atoms:** `StoreNumber("bouncedMv", EntityProperty(Target(0),
  ManaValue))` captures the MV cap BEFORE the bounce (the object becomes untracked once it leaves) →
  `ReturnSpellOrPermanentToOwnersHand` → `GatherCards(FromZone(HAND, You, InstantOrSorcery))` →
  `FilterCollection(ManaValueAtMost(VariableReference("bouncedMv")))` →
  `ConditionalOnCollection(ifNotEmpty = MayEffect(CastFromCollectionWithoutPayingCost))`. Same
  free-cast shape as Breaching Dragonstorm. The `ConditionalOnCollection` gate means a too-expensive
  spell never produces an empty "may cast" prompt.
- **Target:** `TargetSpellOrPermanent(permanentFilter = NonlandPermanent.opponentControls())`.
- **Glamdring (also Gap 10):** the same free-cast-from-hand-with-MV-cap composition (StoreNumber →
  GatherCards(HAND, InstantOrSorcery) → FilterCollection(ManaValueAtMost) →
  MayEffect(CastFromCollectionWithoutPayingCost)) is directly reusable for Glamdring's "cast an
  instant or sorcery with MV <= that damage without paying its mana cost"; no new primitive needed
  there (only the MV source differs — damage dealt vs. bounced object's MV).
- **Test:** `PressTheEnemyScenarioTest` — (a) bounce an opponent's creature then free-cast a MV-1
  sorcery (MV <= 3); (b) a MV-5 sorcery is NOT offered (no pending decision, stays in hand);
  (c) bounce a spell on the stack so it returns to hand without resolving.

## Glamdring (Artifact, Equipment)

- **Card:** `{2}` Legendary Artifact — Equipment. "Equipped creature has first strike and gets
  +1/+0 for each instant and sorcery card in your graveyard. Whenever equipped creature deals combat
  damage to a player, you may cast an instant or sorcery spell from your hand with mana value less
  than or equal to that damage without paying its mana cost. Equip {3}."
- **No engine or SDK change — composes existing primitives.** Static half:
  `GrantKeyword(FIRST_STRIKE)` + `GrantDynamicStatsEffect(powerBonus = DynamicAmounts.zone(You,
  GRAVEYARD, InstantOrSorcery).count())` on `Filters.EquippedCreature`. Trigger half: an
  `ATTACHED`-bound `Triggers.dealsDamage(DamageType.Combat, RecipientFilter.AnyPlayer)` whose effect
  is the same Press-the-Enemy free-cast pipeline — `StoreNumber("combatDamage",
  ContextProperty(TRIGGER_DAMAGE_AMOUNT))` → `GatherCards(FromZone(HAND, You, InstantOrSorcery))` →
  `FilterCollection(ManaValueAtMost(VariableReference("combatDamage")))` →
  `ConditionalOnCollection(ifNotEmpty = MayEffect(CastFromCollectionWithoutPayingCost))`.
- **Root cause of the "free cast never offered" failure (test-only bug, not card/engine):** Glamdring
  grants the equipped creature **first strike**, so a first-striking attacker deals its combat damage
  in the **`FIRST_STRIKE_COMBAT_DAMAGE` step**, not the regular `COMBAT_DAMAGE` step (CR 510.4 /
  702.7b — first strike creates a first combat damage step before the regular one; Step ordering:
  DECLARE_BLOCKERS → FIRST_STRIKE_COMBAT_DAMAGE → COMBAT_DAMAGE). The trigger fired
  correctly there, but the test advanced with `passUntilPhase(COMBAT, COMBAT_DAMAGE)`, which passes
  priority *through* the first-strike step — and the test harness auto-declines any "may" decision
  that arises mid-advance (`ScenarioTestBase.autoResolveDecision`). So the optional free-cast prompt
  was auto-declined during step advancement and there was no pending decision left by the time the
  test stopped at `COMBAT_DAMAGE`. The ATTACHED deals-combat-damage detection path
  (`AttachmentTriggerDetector.detectAttachmentTriggers` via `aurasByTarget`, threaded through
  `TriggerProcessor`/`StackResolver` `triggerDamageAmount`) was correct all along.
- **Fix:** the two combat tests now advance to `Step.FIRST_STRIKE_COMBAT_DAMAGE` (where the
  first-striker's damage and the trigger land) before passing priority / resolving. Mirrors
  `FirstStrikeCombatTest`.
- **Test:** `GlamdringScenarioTest` — (a) first strike + dynamic +X/+0 from instants/sorceries in
  the graveyard (creature card does not count); (b) dealing 3 first-strike combat damage offers a
  free cast of a MV-1 hand sorcery, which resolves; (c) a MV-5 hand sorcery is NOT offered.

## Sauron, the Necromancer (LTR #106)

- **Card:** {3}{B}{B} Legendary Creature — Avatar Horror, 4/4. Menace. Whenever Sauron attacks,
  exile target creature card from your graveyard. Create a tapped and attacking token that's a copy
  of that card, except it's a 3/3 black Wraith with menace. At the beginning of the next end step,
  exile that token unless Sauron is your Ring-bearer.
- **Reuse:** Almost entirely existing primitives — `Triggers.Attacks`,
  `Targets.CreatureCardInYourGraveyard`, `Effects.Exile`, and `Effects.CreateTokenCopyOfTarget`
  (which already supports `tapped`/`attacking` + the override fields `overridePower/Toughness`,
  `overrideColors`, `overrideSubtypes`, `addedKeywords`). The token copies the exiled card's copiable
  values (read from its `CardComponent`, which survives the move to exile), then applies 3/3 / black /
  Wraith / +menace. Composite order is exile-then-copy; verified the copy source still resolves after
  the move (the executor copies from the target's `CardComponent` regardless of zone).
- **New SDK (Gap 33 — token-copy-of-a-card with overrides + delayed conditional exile):** the only
  gap was the third clause. `CreateTokenCopyOfTargetEffect` already had `sacrificeAtStep`, but Sauron
  needs *exile* (not sacrifice) and *unless source is Ring-bearer* (a state gate, not unconditional).
  Added two fields mirroring the existing `sacrificeAtStep` machinery:
  - `exileAtStep: Step?` — schedules one delayed `MoveToZoneEffect(token, EXILE)` per created token at
    that step. No `fireOnPlayerId` gate, so it fires at the *next* end step of any player's turn
    ("the next end step", not "your next end step").
  - `exileUnlessSourceIsRingBearer: Boolean` — wraps the delayed exile in
    `Gate.WhenCondition(SourceIsRingBearer)` (then = no-op, otherwise = exile). The delayed trigger
    bakes Sauron's id as `sourceId`, so `SourceIsRingBearer` (CR 701.54e) re-evaluates against Sauron
    when the trigger fires. Facade params added to `Effects.CreateTokenCopyOfTarget`; description
    builder updated; no new `@Serializable` types or stored Components (reuses `MoveToZoneEffect`,
    `GatedEffect`, `SourceIsRingBearer`, `DelayedTriggeredAbility`).
- **Reusable for Shelob, Child of Ungoliant:** the token-copy-with-overrides path
  (`CreateTokenCopyOfTarget` with `overrideColors`/`overrideSubtypes`/`overridePower/Toughness`/
  `addedKeywords`), and now the `exileAtStep`/`exileUnlessSourceIsRingBearer` delayed-exile sibling,
  are general — any "create a token copy that's a 3/3 black Wraith … exile it at end step unless
  Ring-bearer" card reuses them directly.
- **Test:** `SauronTheNecromancerScenarioTest` — menace; attack exiles the graveyard creature card and
  makes a tapped+attacking 3/3 black Wraith with menace (and inherits the copied card's flying);
  exiled at the next end step when Sauron is NOT the Ring-bearer; survives when Sauron IS the
  Ring-bearer.

## Shelob, Child of Ungoliant (#230) — {4}{B}{G} Legendary Spider Demon, 8/8

- **Keywords:** intrinsic deathtouch + `KeywordAbility.ward("{2}")`.
- **Spider anthem:** "Other Spiders you control have deathtouch and ward {2}" → two static grants over
  `GroupFilter.AllCreaturesYouControl.withSubtype("Spider").other()`: `GrantKeyword(DEATHTOUCH, …)`
  and `GrantWard(WardCost.Mana("{2}"), …)` — reuses the existing anthem-static pattern (Ardyn /
  King of the Oathbreakers).
- **Death-tracking token copy:** "Whenever another creature dealt damage this turn by a Spider you
  controlled dies, create a token that's a copy of that creature, except it's a Food artifact with
  '{2}, {T}, Sacrifice this token: You gain 3 life,' and it loses all other card types."
  - **New observer trigger.** `CreatureDealtDamageBySourceDiesEvent` was extended from a `data object`
    (SELF-only, Soul Collector) to a `data class(sourceFilter: GameObjectFilter? = null)`. `null` keeps
    the SELF shape; a filter makes it an observer (`Triggers.creatureDealtDamageBySourceDies(filter)`,
    binding ANY). The damaging source is matched by **last-known info** captured when it dealt the
    damage — a new `DamagedBySourcesThisTurnComponent` snapshots each source's controller + subtypes +
    creature-ness onto the *damaged* creature, so a Spider that died in the same combat still qualifies
    (CR 608.2h). The snapshot is recorded in both damage paths (`DamageUtils.dealDamageToTarget` and
    `CombatDamageManager.dealFinalDamage`), captured as `ZoneChangeEvent.lastKnownDamageSources` on
    leave, cleared at end of turn, and serialized.
  - **New token-copy overrides.** `CreateTokenCopyOfTargetEffect` gained `overrideCardTypes`
    (replace card types → `{ARTIFACT}` = "loses all other card types"), `addedSubtypes` (union, adds
    "Food"), and `activatedAbilities` (grants the Food sacrifice ability). The copy reads the dying
    creature via `EffectTarget.TriggeringEntity`.
- **Test:** `ShelobChildOfUngoliantScenarioTest` — deathtouch + ward; anthem grants deathtouch + ward
  to another Spider you control; a Bear dealt deathtouch combat damage by a granted-deathtouch Spider
  dies and becomes a Player-1-controlled Food **artifact** token copy that is no longer a creature and
  carries a granted activated ability.

## The Balrog, Durin's Bane (LTR #195) — {5}{B}{R} Legendary Avatar Demon, 7/5 (Gap 30 — cost reduction by per-turn game history)

- **Cost reduction by permanents sacrificed this turn.** "This spell costs {1} less to cast for each
  permanent sacrificed this turn" is the first cost reduction that reads *per-turn game history*
  rather than a current zone/board count. Modeled as a new reusable
  `CostReductionSource.PermanentsSacrificedThisTurn(amountPerPermanent = 1)`, wired into the existing
  `ModifySpellCost(SelfCast, ReduceGenericBy(...))` shape (no card-specific code in the engine).
  - **New turn-scoped counter.** `GameState.permanentsSacrificedThisTurn: Int` — modeled exactly like
    the existing `nonlandPermanentLeftBattlefieldThisTurn` / `spellWarpedThisTurn` per-turn flags.
    Reset to 0 in `TurnManager.startTurn`. Plain `Int`, so no serialization registration needed.
  - **NOT controller-scoped.** The wording is "each permanent sacrificed this turn", not "you
    sacrificed", so the counter is global (every sacrifice by any player this turn counts).
  - **Central sacrifice hook.** The existing `ZoneTransitionService.trackFoodSacrifice` (already called
    at every sacrifice site alongside emitting `PermanentsSacrificedEvent`) was generalized to
    `trackPermanentSacrifice`: it now increments the per-turn counter by the number of sacrificed
    permanents *in addition to* the Food-sacrifice marking. Five emission sites that emitted the event
    without calling the helper (DamageUtils sacrifice-threshold, MoveCollectionExecutor sacrifice move,
    PayOrSufferExecutor, ChainSpellContinuationResumer, plus the extra-event branches in CastSpellHandler
    and SacrificeAndPayContinuationResumer) were wired to call it, so the counter (and the previously-
    leaky Food tracking) is now accurate at all 15 sacrifice sites.
  - The reduction floors the mana component at the colored {B}{R} requirement (CR 601.2f), so the
    cheapest possible cast is {B}{R}.
- **Haste** — `keyword(Keyword.HASTE)`.
- **Can't be blocked except by legendary creatures** — the existing `CantBeBlockedExceptBy` evasion
  static with `GameObjectFilter.Creature.legendary()`.
- **Dies trigger** — `Triggers.Dies` + `Effects.Destroy(ContextTarget(0))` targeting
  `TargetPermanent(TargetFilter(GameObjectFilter.CreatureOrArtifact.opponentControls()))` ("target
  artifact or creature an opponent controls").
- **Test:** `TheBalrogDurinsBaneScenarioTest` — base {5}{B}{R}; {1}-less per sacrifice (counter set
  directly); floor at {B}{R}; a real in-engine sacrifice (Nasty End sac of Grizzly Bears) bumps the
  counter to 1 and discounts the cast to {4}{B}{R}; haste via projected keywords; nonlegendary can't
  block but legendary (Bill the Pony) can; dies trigger Murders → destroys an opponent's creature.

## Witch-king of Angmar (LTR #114) — {3}{B}{B} Legendary Wraith Noble, 5/3 (Gap 36 — combat-history edict filter)

- **Flying** — `keyword(Keyword.FLYING)`.
- **"Whenever one or more creatures deal combat damage to you, each opponent sacrifices a creature of
  their choice that dealt combat damage to you this turn. The Ring tempts you."** — composed from two
  new reusable primitives plus the existing edict + Ring facades:
  - **Defensive combat-damage batch trigger** (new): `Triggers.OneOrMoreCreaturesDealCombatDamageToYou()`
    → SDK `EventPattern.OneOrMoreDealCombatDamageToYouEvent`. The existing per-source
    `dealsDamage(recipient = You, …)` fires once **per connecting creature**, which would over-edict;
    the oracle wants one trigger per combat. Mirrors the offensive `OneOrMoreDealCombatDamageToPlayerEvent`
    but groups the combat-damage batch by the **damaged player** (the observer's controller) instead of
    the source controller. Wired in `TriggerDetector.detectCombatDamageBatchTriggers` (added a
    `combatDamageByDamagedPlayer` grouping + a defensive branch), `TriggerMatcher`, and `TriggerIndex`
    (COMBAT_DAMAGE_BATCH category).
  - **"dealt combat damage to you this turn" filter** (new, Gap 36): the existing
    `HasDealtCombatDamageToPlayerComponent` only records "dealt to *a* player *ever*". Added a per-turn
    `DealtCombatDamageToPlayersThisTurnComponent(playerIds: Set<EntityId>)` set in both player-combat-
    damage paths of `CombatDamageManager`, cleared in `CleanupPhaseManager`, registered in `Serialization`.
    Exposed as source-relative `StatePredicate.DealtCombatDamageToSourceControllerThisTurn` /
    `GameObjectFilter.…dealtCombatDamageToSourceControllerThisTurn()`: it resolves `context.sourceId`'s
    controller, so as an edict filter it reads "...that dealt combat damage to **you** this turn".
    `ForceSacrificeExecutor.findValidPermanents` now threads `sourceId` into the predicate context so the
    source-relative predicate resolves (and the same source flows through `SacrificeContinuation` to
    remaining opponents).
  - **Edict + Ring** — `Effects.Sacrifice(filter = Creature.dealtCombatDamageToSourceControllerThisTurn(),
    target = PlayerRef(Player.EachOpponent)).then(Effects.TheRingTemptsYou())` — reuses the existing
    `ForceSacrificeEffect` (each opponent, their choice, filter-restricted) and the Ring facade.
- **"Discard a card: gains indestructible until end of turn. Tap it."** — activated ability with
  `cost = Costs.DiscardCard`; effect `Effects.GrantKeyword(INDESTRUCTIBLE, Self, EndOfTurn).then(Effects.Tap(Self))`.
  Tapping is an effect, not part of the cost (no `{T}` symbol on the printed cost).
- **Reuse note:** the defensive batch trigger and the "dealt combat damage to you this turn" filter are
  general — they also serve **Witch-king, Bringer of Ruin** and **You Cannot Pass!** (both phrase effects
  off "creatures that dealt combat damage to you this turn").
- **Test:** `WitchKingOfAngmarScenarioTest` — an attacker connects with one creature (Grizzly Bears) while
  a second (Hill Giant) stays home; the trigger fires once, the edict forces the attacker to sacrifice
  only the creature that dealt combat damage this turn (Bears sacrificed, Giant survives — proving the
  filter excludes non-damagers), and the controller's Ring tempt count becomes 1; plus discard-for-
  indestructible taps the Witch-king and grants indestructible (projected keyword).

## Éowyn, Lady of Rohan

- **Combat trigger (modal with equipped override)** — `Triggers.BeginCombat` (already "on your turn"),
  `target("target creature", Targets.Creature)`, then a `ConditionalEffect` gated on
  `Conditions.TargetMatchesFilter(GameObjectFilter.Creature.equipped(), targetIndex = 0)`:
  - **equipped branch** → `Effects.GrantKeyword(FIRST_STRIKE, creature, EndOfTurn).then(GrantKeyword(VIGILANCE, …))`
    — both keywords, no choice.
  - **else branch** → `ModalEffect.chooseOne(Mode.noTarget(GrantKeyword(FIRST_STRIKE, …), "First strike"),
    Mode.noTarget(GrantKeyword(VIGILANCE, …), "Vigilance"))` — the controller picks one.
  - Pure composition of existing primitives; the `ManifoldMouse` (BeginCombat + target + ModalEffect)
    pattern is the precedent.
- **Gap 28 — generic equip-cost reduction** — added a reusable controller-scoped static
  `ReduceEquipCost(amount: Int)` in `MiscStaticAbilities.kt`. The engine reduces only the generic
  portion of the equip cost (floored at {0}; colored pips untouched; multiple sources stack additively),
  keyed off `ActivatedAbility.isEquipAbility`. Wired via `CastPermissionUtils.applyEquipCostReduction`
  (new) into both `ActivateAbilityHandler` (paid cost, two sites) and `ActivatedAbilityEnumerator`
  (displayed cost), applied *before* the `FreeFirstEquipEachTurn` discount. Listed in
  `StaticAbilityHandler`'s equip-permission `when` group (consulted directly, not a continuous effect).
  This is the missing general-purpose static that Forge Anew's `FreeFirstEquipEachTurn` /
  `EquipAbilitiesAtInstantSpeed` left open.
- **Test:** `EowynLadyOfRohanScenarioTest` — at begin of combat on your turn an unequipped target gains
  first strike OR vigilance (controller's choice, both options proven); an equipped target gains BOTH
  with no modal decision; and an inline Equip {3} blade resolves while only {2} mana is available,
  proving the {1} reduction.

## Peregrin Took (LTR #181)

- **New reusable replacement effect `CreateAdditionalToken(tokenType, count, appliesTo)`**
  (`mtg-sdk/.../scripting/ReplacementEffect.kt`) — "If one or more tokens would be created matching
  `appliesTo`, those tokens plus `count` additional `tokenType` token(s) are created instead."
  Models Peregrin Took's `CreateAdditionalToken(tokenType = "Food")`. Fires **once per
  token-creation event** (not per token) and for any kind of token (rulings 2023-06-16).
- **Self-limiting (CR 614.5).** The added predefined tokens are placed by the engine *directly*
  after the primary tokens and deliberately do **not** re-enter the token-creation replacement
  pipeline, so the added Food never triggers another Food — no runaway loop. This is the rules-
  faithful reading of CR 614.5 ("a replacement effect gets only one opportunity to affect an event").
- **Engine wiring (no new executor).** Added `TokenCreationReplacementHelper.createAdditionalTokens(...)`,
  called by both `CreateTokenExecutor` and `CreatePredefinedTokenExecutor` after they create their
  primary tokens (guarded on `createdTokens.isNotEmpty()`). Extracted a shared
  `CreatePredefinedTokenExecutor.placePredefinedToken(...)` companion so both the normal predefined-token
  path and the additional-token path build identical tokens. Classified as runtime in
  `StaticAbilityHandler.isRuntimeReplacementEffect` alongside `DoubleTokenCreation` / `ModifyTokenCount`.
- **Per ruling, the added Food is a plain predefined token** — it doesn't inherit the original tokens'
  granted abilities. (Edge case not modeled: the added token inheriting the original effect's
  `tapped` / "exile at end of combat" flags — uncommon; left as a documented limitation.)
- **Second ability** "Sacrifice three Foods: Draw a card" is pure composition:
  `Costs.SacrificeMultiple(3, GameObjectFilter.Artifact.withSubtype("Food"))` + `Effects.DrawCards(1)`.
- **Test:** `PeregrinTookScenarioTest` — Rally at the Hornburg's two Human Soldiers yield exactly
  one extra Food (fires once per event, not per token); Brandywine Farmer's single ETB Food yields
  exactly two Foods (no loop); and the sacrifice-three-Foods ability (paid via
  `AdditionalCostPayment(sacrificedPermanents = …)`) draws a card and removes all three Foods.

## Lost Isle Calling (LTR #61) — {1}{U} Enchantment

- **Oracle:** "Whenever you scry, put a verse counter on this enchantment. {4}{U}{U}, Exile this
  enchantment: Draw a card for each verse counter on this enchantment. If it had seven or more verse
  counters on it, take an extra turn after this one. Activate only as a sorcery."
- **Scry trigger is pure composition:** `Triggers.WheneverYouScry` (already used by Galadriel,
  Celeborn, Elrond, …) + `Effects.AddCounters(Counters.VERSE, 1, Self)`. The verse counter type
  already existed.
- **The hard part — reading the source's counters after its self-exile cost wiped them.** The
  ability's `Costs.ExileSelf` is part of the *cost*, so by resolution the source is in exile and its
  verse counters are gone (CR 122.2 removes counters on a zone change). Both "draw a card for each
  verse counter on this" and "if it had seven or more" must read the count *as it last existed*
  (CR 112.7a / 608.2h).
- **New reusable primitive: `DynamicAmount.LastKnownSourceCounters(CounterTypeFilter)`** (facade
  `DynamicAmounts.lastKnownSourceCounters(filter)`). `ActivateAbilityHandler` now snapshots the
  source's `CountersComponent` into a new `lastKnownSourceCounters` field on
  `ActivatedAbilityOnStackComponent` (and thence `EffectContext`) *before* paying the cost, but only
  when the cost self-exiles or self-sacrifices (`costExilesOrSacrificesSelf`). The evaluator reads
  that map (filtered by counter type; `Any` sums all). Reusable for any future "exile/sacrifice this:
  do X per counter on it" card. Did **not** extend the trigger-LKI path (`triggerCounterCount` etc.)
  because that fires off a leave-battlefield *event*, whereas a paid cost has no such trigger.
- **Draw + extra turn compose existing nodes:** `DrawCards(lastKnownSourceCounters(Named(VERSE)))` and
  `ConditionalEffect(Compare(lastKnownSourceCounters(Named(VERSE)), GTE, Fixed(7)), TakeExtraTurn())`.
  Added an `Effects.TakeExtraTurn(target, loseAtEndStep)` facade over the existing `TakeExtraTurnEffect`
  (cards previously hand-rolled the raw constructor).
- **Test:** `LostIsleCallingScenarioTest` — scrying once adds one verse counter; activating with 3
  counters draws 3 and exiles the enchantment with no extra turn; activating with 7 draws 7 and grants
  an extra turn (verified via the opponent's `SkipNextTurnComponent`, mirroring Time Walk's model).

## Grishnákh, Brash Instigator

- **Card:** {2}{R} Legendary Goblin Soldier 1/1. "When Grishnákh enters, amass Orcs 2. When you do,
  until end of turn, gain control of target nonlegendary creature an opponent controls with power
  less than or equal to the amassed Army's power. Untap that creature. It gains haste until end of
  turn." Modeled as `Triggers.EntersBattlefield` → `ReflexiveTriggerEffect(action = Amass(2,"Orc"),
  reflexiveEffect = Composite(GainControl(EndOfTurn), Untap, GrantKeyword(HASTE, EndOfTurn)),
  reflexiveTargetRequirements = [TargetCreature(CreatureOpponentControls.nonlegendary()
  .powerAtMostEntity(EntityReference.AmassedArmy))])`. The "when you do" half is the reflexive
  trigger; its target is chosen as the reflexive ability goes on the stack (Scryfall 2023-06-16).
  Control/untap/haste compose existing primitives (same shape as Goatnap).
- **New reusable primitive — pipeline values inside target filters.** Closed the long-standing
  "residual Grishnákh blocker": a target filter's power bound can now reference a resolution-time
  pipeline value. `PredicateContext` gained `storedCollections` (threaded by
  `PredicateContext.fromEffectContext` from `EffectContext.pipeline.storedCollections`), and
  `PredicateEvaluator.resolveEntityReference` now resolves `EntityReference.AmassedArmy` /
  `FromCostStorage` from it (previously hardcoded to null), mirroring
  `TargetResolutionUtils.resolveEntityReference`. `TargetFinder.findLegalTargets` gained an optional
  `pipelineContext: PredicateContext?` folded into the per-candidate `PredicateContext` at every
  battlefield/graveyard call site, so **target enumeration** sees the pipeline.
  `ReflexiveTriggerEffectExecutor.presentReflexiveTargets` passes the resolving effect's pipeline
  context, which is how "creature with power ≤ the amassed Army's power" filters correctly.
- **Why enumeration, not validation:** the reflexive resolve continuation
  (`EffectAndTriggerContinuationResumer.resumeReflexiveTriggerResolve`) executes the reflexive effect
  with the chosen targets and does not re-run `TargetValidator`; the gate is the legal-target set the
  player chooses from, which now excludes power>Army candidates. The existing `PowerAtMostEntity`
  predicate already compared projected powers — only the reference resolution + context threading was
  missing.
- **Unblocks:** Ent-Draught Basin and any future "target creature with power X / ≤ a pipeline-known
  bound" card via the same `powerAtMostEntity(reference)` + `pipelineContext` plumbing.
- **Test:** `GrishnakhBrashInstigatorScenarioTest` — amass Orcs 2 makes a 2/2 Army; the reflexive
  trigger's legal targets include a power-2 Grizzly Bears and exclude a power-3 Hill Giant; stealing
  the bear flips control, untaps it, and grants haste; control reverts to its owner at cleanup.

## Ent-Draught Basin — `PowerEqualsX` target filter for X-cost activated abilities (2026-06-15)

- **Card:** `{2}` Artifact. `{X}, {T}: Put a +1/+1 counter on target creature with power X. Activate
  only as a sorcery.` Composes existing primitives: `Costs.Composite(Costs.Mana("{X}"), Costs.Tap)`
  (X-in-activated-cost, cf. Barad-dûr), `Effects.AddCounters(PLUS_ONE_PLUS_ONE, 1, ContextTarget(0))`,
  `timing = TimingRule.SorcerySpeed`.
- **New SDK primitive — `CardPredicate.PowerEqualsX`** (`.powerEqualsX()` on `ObjectFilter` and
  `TargetFilter`): projected **power exactly equal** to the X chosen for the source spell/ability — the
  power analogue of the existing `ManaValueEqualsX`. Unlike Grishnákh's `PowerAtMostEntity` +
  `AmassedArmy` (which reads a *pipeline-stored entity*), this reads the activation's **X**, already
  carried on the `ActivateAbility` action as `xValue` and threaded into `PredicateContext.xValue`.
- **Enumeration vs validation:** at legal-action enumeration time X is unbound, so the predicate matches
  permissively (`xValue == null → true`, mirroring `ManaValueAtMostX`). The real gate is
  activation-time `TargetValidator.validateTargets(..., xValue = action.xValue)`, which builds a
  `PredicateContext` with X bound and rejects any creature whose power isn't exactly X. No new
  continuation/threading was needed — the activated-ability path already passes `action.xValue` to both
  cost payment and target validation.
- **Client UX:** added `xConstrainsTargetPower` / `LegalActionTargetInfo.xConstrainsPower` flags
  (parallel to `xConstrainsManaValue`) through `TargetInfo` → enricher → DTO → `messages.ts`, and
  `pipelinePhases.ts` re-filters the permissive target list to `card.power === chosenX` after X
  selection, so the player can't click a wrong-power target the server would then reject.
- **Exhaustive-when fan-out:** `PowerEqualsX` needed explicit branches in the cast-record matcher and
  TriggerMatcher/AffectsFilterResolver/CostCalculator (all `→ false`, no X context there).
- **Test:** `EntDraughtBasinScenarioTest` — with X=3, a power-2 and a power-4 creature are rejected by
  validation while the power-3 creature is accepted and gains a +1/+1 counter; activation is illegal at
  instant speed (sorcery-speed gate).

## Shadowfax, Lord of Horses (LTR #227)

- **Anthem:** "Horses you control have haste" is the standard keyword-anthem lord static —
  `GrantKeyword(HASTE, GroupFilter(Creature.withSubtype(Horse).youControl()))`. Shadowfax is itself a
  Horse you control, so the unfiltered (no `excludeSelf`) group correctly grants it haste too.
- **Cheat-in attack trigger:** `Triggers.Attacks` (SELF) + `Patterns.Hand.putFromHand(...)`. "you may"
  is the pattern's built-in `ChooseUpTo(1)` selection (choosing zero declines).
- **"with lesser power":** the chosen hand card's power is strictly < Shadowfax's power, modeled with
  `GameObjectFilter.Creature.powerLessThanEntity(EntityReference.Source)` (the same strict
  source-power filter Rangers of Ithilien uses for battlefield targets — here applied to hand-card
  selection, where it reads the card's printed power and Shadowfax's projected power).
- **New reusable primitive:** added an `entersAttacking: Boolean` parameter to
  `HandPatterns.putFromHand`. When set it routes the move through `ZonePlacement.TappedAndAttacking`
  (the `MoveCollectionExecutor`/`ZoneTransitionService` already supported that placement — it adds the
  `AttackingComponent` against the defending player), so any future "put from hand tapped and
  attacking" card reuses the same pattern. No new effect type or executor was needed.
- **Test:** `ShadowfaxLordOfHorsesScenarioTest` — a summoning-sick Horse can attack the turn it enters
  (haste); a power-3 hand creature is eligible and enters tapped+attacking while a power-5 creature
  stays in hand even as the only option (strict `< 4` power gate).

## Faramir, Prince of Ithilien (LTR #202)

- **Choose an opponent + delayed trigger keyed to that player:** `Triggers.YourEndStep` with
  `target = Targets.Opponent` (the engine's mechanism for "choose an opponent"; auto-selects the lone
  opponent in 1v1). The end-step effect schedules a `CreateDelayedTriggerEffect(step = END,
  fireOnPlayer = ContextTarget(0), timing = NEXT_TURN)` — baking the chosen opponent into
  `fireOnPlayerId` so the delayed trigger fires at the beginning of *that* player's next end step and
  re-exposes them as `Player.TriggeringPlayer` to the inner conditional (same axis Nafs Asp uses).
- **"didn't attack you that turn" (CR 508.6):** added a new reusable condition
  `Conditions.PlayerAttackedPlayerThisTurn(attacker, defender = You)` backed by a new
  `PlayerAttackedPlayersThisTurnComponent` on the attacking player. At declare-attackers,
  `AttackPhaseManager.commitAttackDeclaration` resolves each attacker's defending player (the player, or
  the controller of an attacked planeswalker/battle per CR 508.5) and unions them into the component;
  it's cleared at end of turn in `CleanupPhaseManager` and registered in `Serialization.kt`. The
  card negates it with `Conditions.Not(...)`: didn't attack → `Effects.DrawCards(1)`, otherwise →
  three 1/1 white Human Soldier tokens via `Effects.CreateToken(count = 3)`.
- **Why a new condition, not the existing `PlayerAttackedWithCreaturesThisTurn`:** that one only asks
  "did the player declare any attacker", not *whom* they attacked. Faithful to "attacked **you**",
  Faramir needs the per-defender record, which matters in multiplayer (attacking a different opponent
  shouldn't count). Reusable for any "attacked you/this player this turn" card.
- **Test:** `FaramirPrinceOfIthilienScenarioTest` — both branches: opponent who doesn't attack lets you
  draw at their next end step; opponent who declares an attacker at you (Grizzly Bears) instead gives
  you three Human Soldier tokens.

### Palantír of Orthanc (Gap 6)

- **Card:** `{3}` Legendary Artifact. "At the beginning of your end step, put an influence counter on
  Palantír of Orthanc and scry 2. Then target opponent may have you draw a card. If that player
  doesn't, you mill X cards (X = influence counters on Palantír) and that player loses life equal to
  the total mana value of those cards." Modeled as one `Triggers.YourEndStep` triggered ability with a
  `target("target opponent", Targets.Opponent)`, then `Effects.Composite` of: `AddCounters(INFLUENCE, 1,
  Self)` → `Patterns.Library.scry(2)` → the gated may.
- **Opponent-decides "may" — no new type needed.** `GatedEffect`/the `MayEffect` facade already carries
  a `decisionMaker` that routes the yes/no to a non-controller (used by Magnetic Mountain, Requiem
  Monolith). Set `decisionMaker = <the bound target opponent>` so the *targeted opponent* answers. On
  yes → `Effects.DrawCards(1, Controller)` (you draw); on no → the `otherwise` branch. I only added an
  `otherwise` parameter to the `MayEffect` facade (it already existed on `GatedEffect`) so "target
  opponent may [then]; if they don't, [otherwise]" is expressible through the facade.
- **`DynamicAmount.ManaValueSumOfCollection(collectionName)` (new, reusable).** Else-branch is
  `Patterns.Library.mill(countersOnSelf(INFLUENCE), Controller)` (mills into the default `"milled"`
  collection) → `Effects.LoseLife(DynamicAmounts.manaValueSumOf("milled"), opponent)`. The existing
  `StoredCardManaValue` only reads the *first* card; the new variant sums **every** card in a named
  pipeline collection. Reads each card by entity id, so it is correct after the cards have moved to the
  graveyard. Evaluator branch in `DynamicAmountEvaluator`; facade `DynamicAmounts.manaValueSumOf`. No
  new serialized component — `DynamicAmount` is a sealed interface (auto-polymorphic) and the milled
  set lives in the existing `PipelineState.storedCollections`.
- **Test:** `PalantirOfOrthancScenarioTest` — both branches. Yes: influence counter added, scry 2, then
  opponent (player 2) says yes → you draw exactly one card, nothing milled, opponent at 20. No: pre-load
  2 influence counters (X = 3 after the increment), opponent declines → mill Grizzly Bears (MV 2) +
  Hill Giant (MV 4) + Lightning Bolt (MV 1), opponent loses 7 life (20 → 13).

### Gollum, Scheming Guide (Black) — Gap 38, opponent-guess land/nonland (NEW primitive)

- **Oracle:** "Whenever Gollum attacks, look at the top two cards of your library, put them back in any
  order, then choose land or nonland. An opponent guesses whether the top card of your library is the
  chosen kind. Reveal that card. If they guessed right, remove Gollum from combat. Otherwise, you draw a
  card and Gollum can't be blocked this turn."
- **Engine gap:** no opponent-guess mechanic existed. Built a reusable, parameterized primitive
  `OpponentGuessesTopCardKindEffect(onGuessedRight, onGuessedWrong, chooser, guesser)` rather than a
  card-specific monolith — any "opponent guesses your top card's kind, branch" card composes it.
- **Composition:** trigger = `Triggers.Attacks`; effect = `Patterns.Library.lookAtTopAndReorder(2)` then
  `Effects.OpponentGuessesTopCardKind(onGuessedRight = Effects.RemoveFromCombat(Self), onGuessedWrong =
  Effects.DrawCards(1) then Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, Self, EndOfTurn))`.
- **Decision flow:** two sequenced `ChooseOptionDecision`s via two continuation frames
  (`ChooseGuessKindContinuation` → controller picks framing kind; `GuessTopCardKindContinuation` →
  opponent guesses). On the guess resume the top card is revealed (`CardsRevealedEvent` + reveal marker),
  its actual `typeLine.isLand` compared to the guess, and the matching branch run through
  `EffectContinuationRunner` in the source's original context (so `EffectTarget.Self` = Gollum). A correct
  guess = the guesser's land/nonland call matches reality; the controller's framing choice only phrases
  the prompt. Empty library → no top card → never "right" → wrong branch.
- **Touched:** `GuessEffects.kt` (new effect + `CardKind` enum), `Effects.kt` facade,
  `GuessContinuations.kt` (frames), `OpponentGuessesTopCardKindExecutor.kt`, `GuessContinuationResumer.kt`,
  `CombatExecutors.kt` (register executor), `ContinuationHandler.kt` (register resumer module),
  `Serialization.kt` (register both continuation frames).
- **Test:** `GollumSchemingGuideScenarioTest` — all three branches GREEN: right guess (land/land and
  nonland/nonland) → Gollum removed from combat, no draw, not unblockable; wrong guess (land/nonland) →
  controller draws one card and Gollum gains CANT_BE_BLOCKED for the turn while still attacking.

## Grond, the Gatebreaker (LTR #89) — Gap 31, Vehicle + Crew (reused) + conditional artifact-creature
- **Card:** {3}{B} Legendary Artifact — Vehicle, 5/5. Trample; "As long as it's your turn and you
  control an Army, Grond is an artifact creature"; Crew 3.
- **Finding:** Vehicle (artifact subtype, CR 301.7) and Crew (CR 702.122) already exist in the engine
  (`Subtype.VEHICLE`, `Keyword.CREW`, `KeywordAbility.crew(n)`, `CrewVehicleHandler`,
  `CrewEnumerator`). `CrewVehicleHandler` already animates a Vehicle to its printed P/T with its
  printed keywords via a `BecomeCreatureEffect`, and summoning-sick creatures may crew (matches CR
  702.122c). So **no new primitive was needed** — this was a pure authoring task.
- **Conditional static:** "becomes an artifact creature while X" is the same shape as Synthesizer
  Labship's "it's an artifact creature at 9+": `staticAbility { condition = …; ability =
  GrantCardType("CREATURE", GroupFilter.source()) }` (Layer-4 type change; printed 5/5 applies once
  it's a creature). Condition = `Conditions.All(Conditions.IsYourTurn, Conditions.YouControl(
  GameObjectFilter.Creature.youControl().withSubtype("Army")))`. "An Army" = any Army-subtyped
  creature you control (Armies are creatures from amass).
- **Rule numbers verified** against `MagicCompRules_20260417.txt`: Vehicle subtype = **301.7** (the
  task said 301.6 — that's actually Fortification); Crew = **702.122**.
- **Touched:** only `GrondTheGatebreaker.kt` (card) + `GrondTheGatebreakerScenarioTest.kt` (test) +
  SDK-reference Crew/Vehicle entry. No engine/SDK code changes; no new serializable components.
- **Test:** `GrondTheGatebreakerScenarioTest` — Grond not a creature by default; Crew 3 (tap a 5/5)
  → artifact creature with Trample that can attack; static animates it on your turn with an Army and
  not with a non-Army creature.

## Fear, Fire, Foes! (Gap 35)

- **Card:** {X}{R} Sorcery — "Damage can't be prevented this turn. Fear, Fire, Foes! deals X damage
  to target creature and 1 damage to each other creature with the same controller."
- **Two new reusable primitives:**
  1. **`Effects.DamageCantBePreventedThisTurn()`** (`DamageCantBePreventedThisTurnEffect`, a
     `data object`) — turn-scoped one-shot that sets a new `GameState.damageCantBePreventedThisTurn`
     boolean flag (reset in `TurnManager.startTurn`, alongside `spellWarpedThisTurn`).
     `DamageUtils.isDamagePreventionDisabled(state)` now returns true when the flag is set, so the
     existing damage path ignores all prevention (shields, prevention/replacement-of-damage,
     protection's prevention clause). This is the one-shot complement to the static, permanent-hosted
     `DamageCantBePrevented` replacement effect (Sunspine Lynx) — chosen because a sorcery has no
     battlefield permanent to host the replacement effect.
  2. **`GroupFilter.excludeTarget` / `.otherThanTarget()`** — drops the spell/ability's *first chosen
     target* from a group (distinct from `excludeSelf`, which drops the resolving source). Resolved in
     `ForEachInGroupExecutor`. Paired with extending `PredicateEvaluator.resolveReferencedPlayerFromState`
     to resolve `EffectTarget.TargetController` (controller of the first chosen target) so
     `GameObjectFilter.Creature.targetPlayerControls(EffectTarget.TargetController)` scopes the sweep to
     the target creature's controller. Together they express "each OTHER creature with the same
     controller [as the target]".
- **No new serializable Component:** the turn flag is a `GameState` field (serializes with the state,
  matching `spellWarpedThisTurn`); the new effect is a sealed-interface `@Serializable data object`,
  registered automatically via SDK polymorphism.
- **Rule numbers verified** against `MagicCompRules_20260417.txt`: prevention effects can't apply to
  damage that can't be prevented — **615.6**.
- **Touched:** `FearFireFoes.kt` (card), `DamageEffects.kt` + `Effects.kt` facade (new effect),
  `GroupFilter.kt` (excludeTarget), `ForEachInGroupExecutor.kt`, `PredicateEvaluator.kt`
  (TargetController), `GameState.kt` + `TurnManager.kt` (turn flag), `DamageUtils.kt`,
  `DamageCantBePreventedThisTurnExecutor.kt` + `DamageExecutors.kt`, SDK reference, and
  `FearFireFoesScenarioTest.kt`.
- **Test:** `FearFireFoesScenarioTest` — X damage to the target + 1 to each other creature its
  controller controls (a different player's creatures untouched); and a Battlefield Medic prevent-1
  shield is ignored so full damage lands.

## Fires of Mount Doom (LTR #294) — `onPlayRider` for impulse-play ("when you play a card this way") (2026-06-15)
- **Card:** {2}{R} Legendary Enchantment. ETB: 2 damage to target creature an opponent controls,
  then destroy all Equipment attached to it. {2}{R}: exile top card, may play it this turn; when you
  play a card this way, Fires deals 2 to each player.
- **ETB — fully reused:** `Effects.DealDamage(2, ContextTarget, damageSource = Self)` +
  `Effects.DestroyAllEquipmentOnTarget(ContextTarget)` (both pre-existing). No new code.
- **Activated ability — reused impulse pipeline** (`GatherCards → MoveCollection(EXILE) →
  GrantMayPlayFromExile`), with ONE new SDK primitive for the rider:
  1. **`GrantMayPlayFromExile(..., onPlayRider: Effect?)`** — new optional field on
     `GrantMayPlayFromExileEffect`. The "When you play a card this way, …" payoff is expressed as a
     normal stack triggered ability (CR-faithful — opponents get priority). Mechanically mirrors the
     `DamagePreventedEvent` link-id shield: the grant executor registers a linked **event-based
     delayed triggered ability** (`expiry = EndOfTurn`) alongside the `MayPlayPermission`, sharing a
     `riderLinkId`. Playing a granted card (cast or land) emits a new **`CardPlayedFromPermissionEvent`**
     carrying that id, matched only by `delayedId` in `TriggerDetector.matchesEventForWatchedEntity`,
     so the rider fires on the stack with the granting source as its source.
  - New EventPattern `EventPattern.CardPlayedFromPermissionEvent` (link-id marker, like
    `DamagePreventedEvent`). New engine `GameEvent CardPlayedFromPermissionEvent` (@Serializable +
    registered in `Serialization.kt`; null-mapped in `ClientEvent.kt`; empty categories in
    `TriggerIndex`).
  - Emit sites: `StackResolver.castSpell` (cast-time, when the spell was cast from EXILE via a
    rider-bearing permission) and `PlayLandHandler` (captured before `removeMayPlayPermissionsForCard`,
    threaded into all event-list return paths).
- **No new serializable Component:** the rider link lives on the existing `MayPlayPermission` (new
  `riderLinkId` field) and the existing `DelayedTriggeredAbility` (event-based).
- **Rule numbers:** modeled per oracle wording; the rider is a triggered ability that goes on the
  stack (CR 603) — no specific number cited in code.
- **Touched:** `FiresOfMountDoom.kt` (card), `PipelineEffects.kt` + `Effects.kt` facade
  (`onPlayRider`), `EventPattern.kt` (new pattern), `GameEvent.kt` + `Serialization.kt` +
  `ClientEvent.kt` + `TriggerIndex.kt` (new event), `MayPlayPermission.kt` (`riderLinkId`),
  `ExileTopCardMayPlayFreeExecutor.kt` (register linked delayed trigger), `StackResolver.kt` +
  `PlayLandHandler.kt` (emit), `TriggerDetector.kt` + `TriggerContext.kt` + `TriggerMatcher.kt`
  (match/context), SDK reference, and `FiresOfMountDoomScenarioTest.kt`.
- **Test:** `FiresOfMountDoomScenarioTest` — ETB deals 2 to a targeted opponent creature and destroys
  an Equipment attached to it; the activated ability impulse-exiles the top card, and playing it that
  turn fires the rider dealing 2 to each player.

## Radagast the Brown (LTR #184) — "doesn't share a creature type with a creature you control" filter (2026-06-15)
- **Card:** {2}{G}{G} Legendary Creature — Avatar Wizard, 2/5. "Whenever Radagast or another nontoken
  creature you control enters, look at the top X cards of your library, where X is that creature's
  mana value. You may reveal a creature card that doesn't share a creature type with a creature you
  control from among those cards and put it into your hand. Put the rest on the bottom of your library
  in a random order."
- **Trigger:** `TriggerSpec(ZoneChangeEvent(GameObjectFilter.Creature.youControl().nontoken(), to =
  BATTLEFIELD), binding = TriggerBinding.ANY)` — `ANY` makes it fire for Radagast itself plus any other
  matching creature. X = `DynamicAmounts.triggeringManaValue()` (the entering creature's MV).
- **New filter primitive:** `CardPredicate.DoesNotShareCreatureTypeWithPermanentYouControl(filter)` +
  facade `GameObjectFilter.notSharingCreatureTypeWithPermanentYouControl(filter)`. Negative analogue of
  the existing `SharesColorWithPermanentYouControl`; evaluated in `PredicateEvaluator` via projected
  subtypes (granted types honored), `none { ... }` over the controller's matching battlefield
  permanents. A candidate with no creature types of its own shares none → matches. Sealed
  `@Serializable` CardPredicate — no manual Serialization.kt registration needed.
- **New pattern helper:** `Patterns.Library.lookAtTopRevealMatchingToHand(count, filter, prompt,
  restDestination?, restOrder?)` — Gather top `count` → optional filtered `ChooseUpTo(1)` reveal-to-hand
  → rest to bottom (`CardOrder.Random` default). Generalizes the Star Charter / Whiskervale shape with a
  `DynamicAmount` count and a custom filter.
- **Rule numbers:** modeled per oracle wording; "look at" + optional reveal + bottom-in-random-order are
  CR 701/library-ordering mechanics — no specific number cited in code.
- **Touched:** `RadagastTheBrown.kt` (card), `CardPredicate.kt` + `ObjectFilter.kt` (filter facade),
  `PredicateEvaluator.kt` (evaluation), `LibraryPatterns.kt` (pattern), SDK reference, backlog,
  `RadagastTheBrownScenarioTest.kt`.
- **Test:** `RadagastTheBrownScenarioTest` — proves a Bear/Soldier sharing a type with your creatures
  is shown-but-not-selectable while a Spirit sharing none is selectable and goes to hand; the "may" is
  declinable; the rest go to the bottom.

## Goldberry, River-Daughter

- **Card:** {1}{U} Legendary Creature — Nymph, 1/3. Two activated abilities, both targeting
  "another target permanent you control" (`TargetFilter.PermanentYouControl.other()`,
  `excludeSelf = true`); the *other* permanent is the source/destination depending on the ability,
  with Goldberry herself (`EffectTarget.Self`) as the opposite end. Move-counters-between-permanents
  (Gap 38).
- **Ability A — `{T}`:** "Move a counter of each kind not on Goldberry from another target permanent
  you control onto Goldberry." New effect `MoveCountersEachKindMissing(source, destination)` +
  `MoveCountersEachKindMissingExecutor`. Deterministic, no decision: for each counter kind on the
  source that the destination does *not* already have, move one of that kind onto the destination.
  Kinds the destination already has are left untouched. Honors counter-placement replacements and
  emits proper `CountersRemovedEvent`/`CountersAddedEvent`.
- **Ability B — `{U}, {T}`:** "Move one or more counters from Goldberry onto another target permanent
  you control. If you do, draw a card." New effect `MoveChosenCountersToTarget(source, destination,
  drawCardOnMove)` + `MoveChosenCountersToTargetExecutor`, mirroring the `RemoveAnyNumberOfCounters`
  per-kind pattern: one `ChooseNumberDecision` (0..count) per counter kind on the source, applied via
  new `MoveChosenCountersToTargetContinuation` (registered in Serialization.kt). The destination
  receives the moved counters; after the last kind, the controller draws a card iff `drawCardOnMove`
  and at least one counter was actually moved ("if you do" — conditional draw via
  `services.turnManager.drawCards`).
- **Reusable primitives:** both effects are general parameterized move-counters facades
  (`Effects.MoveCountersEachKindMissing`, `Effects.MoveChosenCountersToTarget`) usable by any future
  "move counters between two permanents" card, not Goldberry-specific.
- **Rule numbers:** modeled per oracle wording (counter movement = remove-then-add); no specific CR
  number cited in code.
- **Touched:** `GoldberryRiverDaughter.kt` (card), `CounterEffects.kt` (SDK effects), `Effects.kt`
  (facades), `MoveCountersEachKindMissingExecutor.kt` + `MoveChosenCountersToTargetExecutor.kt` +
  `PermanentExecutors.kt` (registration), `CardSpecificContinuations.kt` + `Serialization.kt`
  (continuation), `MiscContinuationResumer.kt` (resumer), SDK reference, backlog,
  `GoldberryRiverDaughterScenarioTest.kt`.
- **Test:** `GoldberryRiverDaughterScenarioTest` — Ability A moves a +1/+1 (lacked) onto Goldberry but
  not another charge (already had one), and the source keeps its charge; Ability B moves both chosen
  +1/+1 counters onto another permanent and draws a card.
