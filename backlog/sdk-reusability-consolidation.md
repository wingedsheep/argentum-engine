# SDK Reusability Consolidation

Catalog of duplicated/parameterizable primitives in `mtg-sdk`. Complement to
[sdk-composability-gaps.md](archived/sdk-composability-gaps.md) — that file tracks **missing**
primitives; this one tracks **redundant** ones.

**Premise:** the SDK already has the right primitives (`GameObjectFilter`, `GroupFilter`,
`EntityNumericProperty`, `TurnTracker`, `WardCost`, `CostReductionSource`, `MoveType`,
`EffectTarget.BoundVariable`, `ChoiceType`). Many one-off classes haven't been retired
since those primitives landed. The biggest reuse win is finishing those existing
migrations rather than designing new abstractions.

**Goal:** every concept has exactly one canonical encoding. Card authors never have to
ask "is this in `LibraryPatterns` or `ExilePatterns`?" or "do I want `GrantKeyword` or
`GrantKeywordToCreatureGroup`?".

---

## Top 10 by impact

### 1. `KeywordAbility` flatten — [HIGH]

`scripting/KeywordAbility.kt` (24KB) has ~25 case classes that are pairs/triples of the
same shape. The required primitives already exist; the consolidation just hasn't
happened.

| Group | Current | Target | Status |
|-------|---------|--------|--------|
| Ward | `WardMana` / `WardLife` / `WardDiscard` / `WardSacrifice` | `Ward(cost: WardCost)` (interface exists in `StackEffects.kt:220`, just not used here) | ✅ done |
| Protection | `ProtectionFromColor` / `FromColors` / `FromCardType` / `FromCreatureSubtype` / `FromEverything` / `FromEachOpponent` / `HexproofFromColor` | `Protection(scope: ProtectionScope)`, `Hexproof(scope)` | ✅ done |
| Numeric | `Annihilator` / `Bushido` / `Rampage` / `Absorb` / `Afflict` / `Toxic` / `Crew` / `Modular` / `Fading` / `Vanishing` / `Renown` / `Fabricate` / `Tribute` (×13) | `Numeric(keyword: Keyword, n: Int)` driven by `Keyword.displayName` | ✅ done |
| Cycling | `Cycling` / `Typecycling` / `BasicLandcycling` | `Cycling(cost, searchFilter?, displayPrefix?)` | ✅ done |
| Kicker | `Kicker` / `KickerWithAdditionalCost` / `Multikicker` / `Offspring` | `Kicker(manaCost?, additionalCost?, multi)` | ✅ done |

**Win:** ~25 case classes deleted, ~14KB removed.

### 2. Static-ability single-vs-group unification — [HIGH] ✅ done

Every grant-X had both a `StaticTarget` form and a `GroupFilter` form:
`GrantKeyword` / `GrantKeywordToCreatureGroup`, `ModifyStats` / `ModifyStatsForCreatureGroup`,
`GrantWard` / `GrantWardToGroup`, `CantAttack` / `CantAttackForCreatureGroup`,
`CantBlock` / `CantBlockForCreatureGroup`, `MustAttack` / `MustAttackForCreatureGroup`,
`GrantTriggeredAbilityToAttachedCreature` / `GrantTriggeredAbilityToCreatureGroup`,
`GrantActivatedAbilityToAttachedCreature` / `GrantActivatedAbilityToCreatureGroup`, etc.

**Fix applied:** `GroupFilter` gained a `scope: Scope` field
(`Battlefield` / `Self` / `AttachedTo` / `Specific`) plus `source()` / `attachedCreature()` /
`specific(id)` factories. Every static ability now carries `filter: GroupFilter`
(default `attachedCreature()` for Aura/Equipment-shaped grants, `source()` for
"this creature ..." grants). The engine's `convertGroupFilter` short-circuits on
non-Battlefield scope before falling through to the existing battlefield logic.
`StaticTarget` and `convertStaticTarget` deleted entirely; `Filters.kt` aliases
retyped from `StaticTarget` to `GroupFilter`. `GrantSubtype` and
`GrantAdditionalTypesToGroup` were left as separate classes — both already take
`GroupFilter`, and the only duplicated dimension was `StaticTarget`-vs-`GroupFilter`.

Merged-and-deleted classes (15): `GrantKeywordToCreatureGroup`,
`ModifyStatsForCreatureGroup`, `GrantWardToGroup`,
`CantAttackForCreatureGroup`, `CantBlockForCreatureGroup`,
`MustAttackForCreatureGroup`, `MustBlockForCreatureGroup`,
`GrantTriggeredAbilityToAttachedCreature`, `GrantTriggeredAbilityToCreatureGroup`
(merged into `GrantTriggeredAbility`), `GrantActivatedAbilityToAttachedCreature`,
`GrantActivatedAbilityToCreatureGroup` (merged into `GrantActivatedAbility`), plus
the `StaticTarget` sealed interface and its five subtypes.

### 3. `CostStaticAbilities` cost-modifier sprawl — [HIGH] ✅ done

The 10 per-shape classes (`ReduceSpellCostBySubtype`,
`ReduceSpellColoredCostBySubtype`, `ReduceSpellCostByFilter`, `SpellCostReduction`,
`FaceDownSpellCostReduction`, `ReduceFirstSpellOfTypeColoredCost`,
`ReduceFaceDownCastingCost`, `IncreaseSpellCostByFilter`,
`IncreaseSpellCostByPlayerSpellsCast`, `IncreaseMorphCost`) all collapsed into
one `ModifySpellCost(target, modification, gating)`:

- `target: SpellCostTarget` — `SelfCast` / `YouCast(filter)` / `AnyCaster(filter)` /
  `FaceDownYouCast` / `MorphActivation`. Captures *whose* cast is affected and which
  mechanic (cast cost vs morph activation cost).
- `modification: CostModification` — `ReduceGeneric(amount)` /
  `ReduceGenericBy(CostReductionSource)` / `ReduceColored(symbols)` /
  `ReduceColoredPerUnit(symbols, countSource)` / `IncreaseGeneric(amount)` /
  `IncreaseGenericPerOtherSpellThisTurn(amountPerSpell)`.
- `gating: CostGating` — `None` / `FirstOfTypePerTurn` (Eluge).

`CostCalculator` rewritten around the new shape: a single
`scanBattlefieldModifySpellCost` walks every battlefield permanent once,
`targetMatchesSpell` filters by target/scope (using *projected* controller, not
base), `gatingApplies` checks first-of-type-per-turn, and `applyToSpellCast`
dispatches by modification kind. Face-down (`calculateFaceDownCost`) and morph
(`calculateMorphCostIncrease`) walk the same scan with target-typed filters.
`ReduceFaceDownCastingCost` was unused dead code and dropped entirely.

### 4. `CardPredicate` numeric collapse — [HIGH]

13 P/T/MV predicates: `PowerEquals`/`AtMost`/`AtLeast`, `ToughnessEquals`/`AtMost`/`AtLeast`,
`ManaValueEquals`/`AtMost`/`AtLeast`, `PowerOrToughnessAtLeast`, `TotalPowerAndToughnessAtMost`,
`ToughnessGreaterThanPower`, plus combinators.

**Fix:** one `NumericPredicate(property: EntityNumericProperty, op: ComparisonOperator, value: Int | EntityNumericProperty)`.
`EntityNumericProperty` already exists. `ToughnessGreaterThanPower` becomes
`NumericPredicate(Toughness, GT, Power)`.

**Win:** 13 → 1.

### 5. TurnTracker condition collapse — [HIGH] ✅ done

`scripting/conditions/TurnConditions.kt`'s 11 hand-rolled "did X this turn"
classes deleted. New `TurnTracker` keys (`LIFE_LOST`, `PLAYER_ATTACKED`,
`DEALT_COMBAT_DAMAGE`, `COUNTERS_PUT_ON_CREATURE`, `LANDS_PLAYED`,
`FOOD_SACRIFICED`, `CARDS_LEFT_GRAVEYARD`) backed by the existing per-player
markers/accumulators; markers report as 0 or 1, count-bearing components
return their full count. Each `Conditions.X` DSL accessor now produces
`Compare(DynamicAmount.TurnTracking(player, key), GTE, Fixed(n))` — the
projection-side `SourceProjectionCondition.Compare` branch already delegated
to `DynamicAmountEvaluator`, so the dead `ControllerLostLifeThisTurn`
projection variant + its `StaticAbilityHandler` mapping were dropped. AND/OR
composites collapse to `AllConditions` / `AnyCondition` over the basic
trackers; `OpponentLostLifeThisTurn` now reads
`TurnTracking(Player.Opponent, LIFE_LOST)`. Card-sites that imported the data
objects directly (ktk Mardu cycle, Rock Jockey, Thought Stalker Warlock) were
moved onto the `Conditions` facade.

**Win:** 11 condition classes + 12 hand-written evaluator branches deleted;
future "did X this turn" cards add a tracker key, not a class.

### 6. DynamicAmount context-property collapse — [HIGH] ✅ done

10 of the 11 cases collapsed into `DynamicAmount.ContextProperty(key:
ContextPropertyKey)`. The single evaluator branch dispatches by key:
`TRIGGER_DAMAGE_AMOUNT` / `TRIGGER_LIFE_GAINED` / `TRIGGER_LIFE_LOST` all
share `EffectContext.triggerDamageAmount` (the per-event accumulator already
populates it with the absolute amount of life moved);
`LAST_KNOWN_PLUS_ONE_COUNTER_COUNT` / `LAST_KNOWN_TOTAL_COUNTER_COUNT` read
the trigger payload's last-known counter snapshot;
`ADDITIONAL_COST_EXILED_COUNT` / `ADDITIONAL_COST_BLIGHT_AMOUNT` read
cast-time cost accumulators; `TARGET_COUNT` reads `context.targets.size`;
`LINKED_EXILE_CARD_COUNT` / `LINKED_EXILE_DISTINCT_CARD_TYPE_COUNT` read the
source's `LinkedExileComponent`. `CreaturesSharingTypeWithEntity(entity)`
stays as a separate parameterized class — its `EntityReference` parameter
doesn't fit the enum-keyed shape.

Per-key oracle text lives on the enum's `description` so card text generation
remains stable.

**Win:** 10 case classes → 1 + 10-entry enum.

### 7. Targeting OR-type collapse — [HIGH]

`scripting/targets/TargetRequirement.kt` has 6 "X or Y" sealed sub-types:
`TargetCreatureOrPlayer`, `TargetOpponentOrPlaneswalker`, `TargetPlayerOrPlaneswalker`,
`TargetCreatureOrPlaneswalker`, `TargetSpellOrPermanent`, `AnyTarget`.
`TargetObject(filter: TargetFilter)` is already the right unification.

**Fix:** delete the 6 OR-cases; use `TargetObject(filter = TargetFilter.creatureOrPlayer)`
etc. Three truly distinct shapes remain: `TargetObject`, `TargetPlayer/Opponent`,
`TargetOther(base)`. The `withId(name)` extension function (line 329) currently has an
exhaustive `when` over all 9 cases — it shrinks to 3.

**Win:** 6 case classes deleted; targeting becomes filter-driven uniformly.

### 8. `ReplacementEffect` parameterization — [HIGH]

`scripting/ReplacementEffect.kt` (35KB, ~30 sub-types). Several pairs differ only by a
parameter:

- `Undying` / `Persist` → `EntersFromGraveyardWithCounter(counterType)`.
- `DoubleTokenCreation` / `ModifyTokenCount`, `DoubleCounterPlacement` / `ModifyCounterPlacement`,
  `PreventLifeGain` / `ModifyLifeGain` → `Modifier = Add(n) | Multiply(n) | Set(n)`.
- `EntersWithCounters` / `EntersWithDynamicCounters` → single class with `count: DynamicAmount`.
- `EntersTapped` flag fields (`unlessCondition`, `payLifeCost`) → `TappedCondition = Always | Unless(c) | UnlessPayLife(n)`.

(`EntersWithChoice` already collapsed three predecessors per its inline comment — apply
the same pattern.)

### 9. DSL file splits — [HIGH]

`dsl/Effects.kt` (78KB, 1835 lines), `dsl/CardBuilder.kt` (45KB, 1225 lines),
`dsl/Triggers.kt` (36KB, 1101 lines). These get touched by every card author.

**Fix:** split along the `scripting/effects/` partition lines:

- `Effects.kt` → `EffectsZone` / `EffectsCombat` / `EffectsCounters` / `EffectsMana` /
  `EffectsTokens` / `EffectsCopy` / `EffectsControl` / `EffectsModal` / `EffectsPlayer` /
  `EffectsKeywords`.
- `CardBuilder.kt` → metadata builders + basic-land helper + planeswalker/Saga DSL +
  Class/level-up DSL split out.
- `Triggers.kt` → split by category mirroring scripting/triggers (zone change / combat /
  phase / damage / spell). The file already uses comment headers for these sections.

Pure ergonomics, no behavior change.

### 10. `StatePredicate` exhaustiveness — [HIGH] ✅ done

Three sites had `else -> true` for `StatePredicate`:
`TriggerMatcher.matchesStatePredicateForTrigger` (only handled `IsFaceDown` + combinators),
`BeginningPhaseManager.matchesStatePredicateForUntap` (only handled `HasCounter` + combinators),
and `TriggerAbilityResolver` granted-ward filter (only handled `HasAnyCounter`). All three now
delegate to the single exhaustive `PredicateEvaluator.matchesStatePredicate`. The sub-interface
split (projection-vs-history) was skipped — the centralised exhaustive evaluator already closes
the bug class. Future `StatePredicate` cases get picked up by every consumer automatically
because the central `when` is exhaustive without an `else` branch.

---

## Other notable findings

### Effects (`scripting/effects/`, `dsl/Effects.kt`)

- **`MoveType` unification** — `MoveToZoneEffect.byDestruction:Boolean` (`RemovalEffects.kt:262`)
  vs `MoveCollectionEffect.moveType:MoveType` (`PipelineEffects.kt:519`). Same concept,
  two encodings. Unify on `MoveType { Default, Discard, Sacrifice, Destroy }`.
- **`AddManaEffect` cluster (×8)** — `AddManaEffect`, `AddColorlessManaEffect`,
  `AddAnyColorManaEffect`, `AddAnyColorManaSpendOnChosenTypeEffect`, `AddDynamicManaEffect`,
  `AddManaOfChosenColorEffect`, `AddManaOfColorAmongEffect`, `AddOneManaOfEachColorAmongEffect`
  → one `AddManaEffect(spec: ManaSpec, amount, restriction?)`.
- **`ChainCopy` facades (×5)** — `DestroyAndChainCopy` / `BounceAndChainCopy` /
  `DamageAndChainCopy` / `DiscardAndChainCopy` / `PreventDamageAndChainCopy` →
  `ChainCopy(action: Effect)`.
- **`CreateTokenCopyOf{Source,Target,EquippedCreature,ChosenPermanent}`** →
  `CopyPermanentEffect(source: CopySource)`.
- **Future-turn modifications** — `SkipNextTurnEffect`, `HijackNextTurnEffect`,
  `TakeExtraTurnEffect`, `SkipCombatPhasesEffect`, `SkipUntapEffect` →
  `ModifyFutureTurnEffect(target, modification)`.
- **`MoveAllLastKnownCountersEffect`** — whole effect class for one card (Essence
  Channeler). Fold into `AddCountersEffect` with a `from-source-last-known`
  `DynamicAmount`.
- **Counter removal (×3)** — `RemoveAnyNumberOfCountersEffect` /
  `RemoveAllCountersEffect` / `RemoveCountersEffect` → one with
  `mode = Fixed(type, n) | All | AnyNumberControllerChooses`.
- **`CopyNextSpellCastEffect` vs `CopyEachSpellCastEffect`** — boolean "consume after
  first use" → `CopyCastEffect(copies, mode = OneShot | UntilEndOfTurn)`.
- **`AddCountersEffect` vs `AddDynamicCountersEffect`** — Int vs DynamicAmount; keep
  only the dynamic, wrap with `DynamicAmount.Fixed`.
- **`SetBasePowerEffect` vs `SetBasePowerToughnessEffect`** — same operation at different
  sublayers; one `SetBaseStatsEffect(power?, toughness?, target, duration)`.
- **`DistributeCountersFromSelfEffect` vs `DistributeCountersAmongTargetsEffect`** —
  one `DistributeCountersEffect(source, recipients, total, ...)`.
- **`PutOnLibraryPositionOfChoiceEffect`** overlaps with `MoveToZoneEffect.positionFromTop`.
- **`ReturnLinkedExile{,UnderOwnersControl,ToHand}` / `ReturnOneFromLinkedExileEffect` /
  `TakeFromLinkedExile` (×5)** → `EffectPatterns.moveLinkedExile(...)`.
- **`GrantHexproofEffect` / `GrantShroudEffect` / `GrantHexproofFromChosenColorEffect` /
  `GrantToxicEffect`** → extend `GrantKeywordEffect` with `parameter: KeywordParameter?`.
  Removes the `"TOXIC_N"` string-keyword hack.

### Conditions (`scripting/conditions/`)

- `SourceIsTapped` / `SourceIsUntapped` / `SourceIsAttacking` / `SourceIsBlocking` map
  1:1 to existing `StatePredicate` siblings — replace with
  `SourceMatches(GameObjectFilter)`.
- `SourceHasSubtype` / `SourceHasKeyword` / `SourceHasCounter` /
  `EnchantedCreatureHasSubtype` / `EnchantedCreatureIsLegendary` →
  `SourceMatches(filter, scope = Self | AttachedTo)`.
- `WasCastFromHand` / `WasCast` / `WasCastFromZone` →
  `WasCastFrom(zones: Set<Zone>?)`.
- `ManaSpentToCastIncludes` — generalize as
  `Compare(DynamicAmount.ManaSpentForColor(color), GTE, Fixed(n))`.
- `TriggeringEntityWasHistoric` / `TriggeringEntityEnteredOrWasCastFromGraveyard` /
  `TriggeringEntityHadMinusOneMinusOneCounter` →
  `TriggeringEntityMatches(filter)` once those properties are exposed as predicates.

### Predicates / Filters (`scripting/predicates/`, `scripting/filters/`)

- `IsNonland` / `IsNoncreature` / `IsNonenchantment` are just `Not(IsX)` — `Not`
  already exists at `CardPredicate.kt:497`. Delete.
- `HasSubtype` / `NotSubtype` / `HasAnyOfSubtypes` →
  `HasSubtype(set, negate = false)`. Same for `HasColor` / `NotColor`.
- `SharesCreatureTypeWith{Source,TriggeringEntity,EntityRef}` plus 5 chosen/stored
  variants (`HasChosenSubtype`, `HasSubtypeFromVariable`, `HasSubtypeInStoredList`,
  `HasSubtypeInEachStoredGroup`, `NotOfSourceChosenType`) →
  `SharesCreatureTypeWith(EntityReference)` + `HasSubtypeMatching(SubtypeSource)`.
  8 → 2.
- `RecipientFilter` and `SourceFilter` hand-rolled cases (`AnyCreature`,
  `CreatureYouControl`, `EnchantedCreature`, `EquippedCreature`, …) duplicate
  `GameObjectFilter`. Delete; force callers through `RecipientFilter.Matching(filter)`.
- `ControllerPredicate.ControlledByYou` / `Opponent` / `TargetOpponent` /
  `TargetPlayer` / `ReferencedPlayer(t)` / `Any` → one `ControlledBy(Player)` reusing
  `references/Player.kt`.
- `CounterTypeFilter.PlusOnePlusOne` / `MinusOneMinusOne` / `Loyalty` are just `Named`
  with hardcoded constants. Delete; route through a `Counters` constants object.
  (Resolves the "counter-type string → enum resolution" bug noted in memory.)

### Numeric values (`scripting/values/`)

- **Delete `CardNumericProperty`** (3 entries) — strict subset of `EntityNumericProperty`
  (6+ entries). Have `AggregateBattlefield` / `AggregateZone` accept
  `EntityNumericProperty` directly. The git status already shows both files modified —
  good moment to finish the migration.
- **Delete `Count`** (`DynamicAmount.kt:395`) — redundant with
  `AggregateBattlefield(COUNT)` and `AggregateZone(COUNT)`. The constructor's docstring
  even claims "preferred counting primitive" but the same file then introduces two
  more.
- **Delete `YourLifeTotal`** — redundant with `LifeTotal(Player.You)`.
- **`StoredCardManaValue(name)`** is a one-off — add
  `EntityReference.PipelineCollection(name, index)` and use `EntityProperty(...)`.
- **Split `Aggregation` enum** into `Aggregation` (numeric: COUNT/MAX/MIN/SUM) and
  `DistinctOver` (DISTINCT_TYPES/COLORS/NAMES). The current mix forces engine branches
  to swallow nonsensical combinations.

### Triggers (`dsl/Triggers.kt`)

The data-layer (`scripting/TriggerSpec.kt`) is fine. The DSL exposes 30+ pre-shaped
ZoneChange/SpellCast/Attack/DealsDamage variants:

- Most reduce to 4 base helpers parameterized by `(filter, binding, fromZone, toZone)`.
  Expose `Triggers.zoneChange(filter, from, to, binding = OTHER)` and most named `val`s
  become one-line builders: `OtherCreatureWithSubtypeDies(s)`,
  `FilteredBecomesBlocked(filter)`, `BlocksOrBecomesBlockedBy(filter)`,
  `YouAttackWithFilter(filter)`, `YouCastSubtype(s)`, `CreatureTurnedFaceUp(player)`,
  etc.

### Static abilities (multiple files, see §2 for the big one)

- **`AddCreatureTypeByCounter`** / **`AddLandTypeByCounter`** / **`GrantKeywordByCounter`**
  → use `GroupFilter.withCounter(type)` (or existing `AffectsFilter.CreaturesWithCounter`)
  with `GrantKeywordToCreatureGroup` / `GrantAdditionalTypesToGroup`. 3 → 0.
- **`GrantHexproofToController`** / **`GrantShroudToController`** / **`GrantCantLoseGame`** /
  **`CantBeTargetedByOpponentAbilities`** / **`ExtraLoyaltyActivation`** /
  **`GrantAdditionalLandDrop`** → `GrantPlayerEffect(PlayerStaticEffect)`. 6 → 1.
- **`UntapDuringOtherUntapSteps`** vs **`UntapFilteredDuringOtherUntapSteps`** — pure
  duplication. Default the filter to `GameObjectFilter.Any`.
- **`AdditionalManaOnTap`** vs **`AdditionalManaOnLandTap`** — one
  `AdditionalManaOnLandTap(filter, amount, color)`; aura scoping via
  `GroupFilter.attachedLand()`.
- **`PlayFromTopOfLibrary`** / **`CastSpellTypesFromTopOfLibrary`** /
  **`LookAtTopOfLibrary`** / **`PlayLandsAndCastFilteredFromTopOfLibrary`** →
  `LibraryTopAccess(view, play)`. 4 → 1.
- **`AdditionalETBTriggers`** vs **`AdditionalSourceTriggers`** →
  `AdditionalTriggers(triggerScope, sourceFilter)`.

### Targeting (`scripting/targets/`)

- **`EffectTarget.ContextTarget(index)` vs `BoundVariable(name)`** — keep named form,
  retire indexed. Memory notes the named form is preferred; multi-target indexed form
  already exists.
- **`PipelineTarget` / `ControllerOfPipelineTarget`** → either fold via a
  `dimension = Entity | Controller` field, or introduce a generic
  `EffectTarget.ControllerOf(EffectTarget)` wrapper that subsumes
  `TargetController`, `ControllerOfTriggeringEntity`, `ControllerOfPipelineTarget`
  (3 → 1).

### Costs (`scripting/AdditionalCost.kt`, `scripting/costs/PayCost.kt`)

- **`AdditionalCost.BlightOrPay`** / **`BeholdOrPay`** → wrap any cost with
  `OrPayMana(inner: AdditionalCost, manaCost: String)`.
- **Strategic:** `AdditionalCost` (cast-time) and `PayCost` (activation-time) share
  concepts but neither references the other. Eventually unify under a single `Cost`
  sealed interface — flag, not an immediate change.

### DSL pattern files (`dsl/*Patterns.kt`)

- `LibraryPatterns` / `ExilePatterns` / `HandPatterns` / `GroupPatterns` /
  `MiscPatterns` / `CreatureTypePatterns` / `EffectPatterns` overlap. "Exile from top
  of library until X" appears in 3 of them.
- **Fix:** make `EffectPatterns.kt` the only public facade; specialised helpers move
  into `internal` files.

---

## Recommended ordering

If picking one at a time, by leverage:

1. **`KeywordAbility` flatten** — self-contained, ~25 case classes deleted, primitives
   already exist.
2. **Static-ability single-vs-group unification** — every card author currently has to
   know which form exists.
3. **`CostStaticAbilities` consolidation** — cost-modification is the muddiest area.
4. **TurnTracker + DynamicAmount context-property collapse** ✅ done — kills
   the "one new condition per card" pattern.
5. **`StatePredicate` exhaustiveness** ✅ done — closed the known bug class by centralising
   every `when` through the exhaustive `PredicateEvaluator.matchesStatePredicate`.
6. **Targeting OR-type collapse** — small, high signal-to-noise.
7. **`MoveType` unification** + **`AddManaEffect` cluster** + **`ChainCopy` facades** —
   the effect-side cleanups.
8. **`CardNumericProperty` deletion** — already mid-flight per git status.
9. **DSL file splits** (`Effects.kt`, `CardBuilder.kt`, `Triggers.kt`) — pure
   ergonomics, no risk.
10. **`ReplacementEffect` parameterization** — biggest file, highest risk; do last.

Each finding is independently verifiable; none requires a coordinated multi-file
rewrite.
