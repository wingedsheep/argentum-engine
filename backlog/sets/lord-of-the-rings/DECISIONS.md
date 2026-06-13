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

