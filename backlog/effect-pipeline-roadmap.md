# Effect Pipeline Decomposition Roadmap

This document captures the architectural analysis and phased plan for decomposing monolithic effects
into composable pipelines using the existing Gather/Select/Move vocabulary.

**Goal:** Enable most future cards to be implemented as pure `EffectPatterns` compositions without
writing new executor code.

**Current state:** ~60% of zone-manipulation effects use pipelines. Target: ~85%+.

---

## Current Pipeline Vocabulary (Atoms)

These are the reusable building blocks already in place:

| Atom | File | Purpose |
|------|------|---------|
| `GatherCardsEffect` | PipelineEffects.kt | Collect cards from source into named collection |
| `SelectFromCollectionEffect` | PipelineEffects.kt | Player/opponent/random selects from collection |
| `MoveCollectionEffect` | PipelineEffects.kt | Move named collection to zone with placement/order |
| `RevealUntilEffect` | PipelineEffects.kt | Reveal from library until filter matches |
| `ShuffleLibraryEffect` | LibraryEffects.kt | Shuffle library |
| `ChooseCreatureTypeEffect` | PipelineEffects.kt | Player chooses a creature type |
| `ChooseOptionEffect` | PipelineEffects.kt | Generic choice (type, color) stored in context |
| `ForEachPlayerEffect` | CompositeEffects.kt | Iterate pipeline per player |
| `ForEachTargetEffect` | CompositeEffects.kt | Iterate pipeline per target |
| `ForEachInGroupEffect` | GroupEffects.kt | Apply effect to each entity matching filter |
| `ConditionalOnCollectionEffect` | PipelineEffects.kt | Branch based on collection emptiness |
| `MoveToZoneEffect` | RemovalEffects.kt | Move single resolved target between zones |
| `CompositeEffect` | CompositeEffects.kt | Chain effects sequentially via `then` |

### Already Decomposed via EffectPatterns (~25 compositions)

- Scry, Surveil, Mill, SearchLibrary, Wheel, Discard, DiscardRandom, DiscardHand
- LookAtTopAndKeep, LookAtTopAndReorder, RevealUntilNonland variants
- HeadGames, SearchTargetLibraryExile, EachPlayerSearches, EachOpponentDiscards
- PutFromHand, ShuffleGraveyardIntoLibrary, EachPlayerDiscardsDraws
- RevealAndOpponentChooses, ChooseCreatureTypeRevealTop, and more

---

## Phase 1: Quick Wins (No New Atoms)

Migration of existing card definitions to use pipeline patterns that already exist.

- [x] **Audit `EachOpponentDiscardsEffect` usage** — Already migrated: 5 cards use
      `EffectPatterns.eachOpponentDiscards()`. Only Syphon Mind needs `controllerDrawsPerDiscard=1`
      (can't use pipeline). Words of Waste needs concrete type for replacement effects.
- [x] **Audit direct `DrawCardsEffect` in composites** — Migrated 25 card files from direct
      `DrawCardsEffect` to `Effects.DrawCards()` facade. Zero remaining direct usages in mtg-sets.
- [x] **Audit `ReturnAllToHandEffect` usage** — Only 1 usage (Thousand Winds). Needs Phase 2
      atoms (`CardSource.BattlefieldMatching`) before migration; deferred to Phase 2d.
- [x] **Document convention** — CLAUDE.md already documents pipeline preference in "Atomic Effects"
      and "Adding a New Mechanic" sections. Added explicit guidance to prefer `Effects.*` facade
      over direct effect constructors.

---

## Phase 2: `MoveType.Destroy` + `CardSource.BattlefieldMatching`

These two additions unlock pipeline decomposition of all "destroy group" and "bounce group" cards.

### 2a. Add `MoveType.Destroy`

- [x] **Add `Destroy` variant to `MoveType` enum** in `PipelineEffects.kt`
  - Semantics: respects indestructible, triggers regeneration, emits destruction events
- [x] **Update `MoveCollectionExecutor`** to handle `MoveType.Destroy`
  - Checks indestructible via projected state (skips those permanents)
  - Checks regeneration shields (consumes shield, taps creature, creature stays)
  - Routes destroyed cards to owner's graveyard
  - Removes floating effects targeting destroyed entities (Rule 400.7)
- [x] **Add tests** for `MoveType.Destroy` in `MoveCollectionExecutor`
  - Indestructible permanents survive
  - Regeneration shields consume and prevent destruction
  - Mixed collection (indestructible + normal) — only normal die
  - Routes to owner's graveyard (not controller's)
  - Strips battlefield components
  - Removes floating effects
  - Empty collection is no-op
  - Correct ZoneChangeEvent emitted

### 2b. Add `CardSource.BattlefieldMatching`

- [x] **Add `BattlefieldMatching` variant to `CardSource`** in `PipelineEffects.kt`
  - Parameters: `filter: GameObjectFilter`, `player: Player = Player.Each`
  - Gathers all permanents on the battlefield matching the filter (any controller)
  - Must use projected state for type/color/keyword checks
- [x] **Update `GatherCardsExecutor`** to handle `CardSource.BattlefieldMatching`
  - Use `predicateEvaluator.matchesWithProjection()` (not base state)
- [x] **Add tests** for `CardSource.BattlefieldMatching`

### 2c. Decompose `DestroyAllEffect` into Pipeline

- [x] **Add `EffectPatterns.destroyAllPipeline(filter)`** factory method
  ```
  GatherCards(BattlefieldMatching(filter)) → MoveCollection(Graveyard, moveType=Destroy)
  ```
- [x] **Support `storeDestroyedAs`** — Added `storeMovedAs` to `MoveCollectionEffect` +
      `noRegenerate` flag. `MoveCollectionExecutor` returns destroyed IDs in `updatedCollections`.
- [x] **Support `exceptSubtypesFromStored`** — Added `FilterCollectionEffect` atom with
      `CollectionFilter.ExcludeSubtypesFromStored`. Factory: `EffectPatterns.destroyAllExceptStoredSubtypes()`.
- [x] **Migrate `DestroyAllEffect` usages** — Decree of Pain → `Effects.DestroyAll()`,
      Harsh Mercy → `EffectPatterns.destroyAllExceptStoredSubtypes()`
- [x] **Deprecate `DestroyAllEffect`** and `DestroyAllExecutor` (kept for backward compat,
      marked `@Deprecated`)

### 2d. Decompose `ReturnAllToHandEffect` into Pipeline

- [ ] **Add `EffectPatterns.returnAllToHand(filter)`** factory method
  ```
  GatherCards(BattlefieldMatching(filter)) → MoveCollection(Hand)
  ```
- [ ] **Migrate `ReturnAllToHandEffect` usages**
- [ ] **Deprecate `ReturnAllToHandEffect`** and `ReturnAllToHandExecutor`

### 2e. Decompose `DestroyAllSharingTypeWithSacrificedEffect`

- [ ] **Add `EffectPatterns.destroyAllSharingTypeWithSacrificed()`** factory method
  - Read sacrificed creature's subtypes from context
  - Gather battlefield creatures matching those subtypes
  - Move to graveyard with `MoveType.Destroy`
- [ ] **Migrate `DestroyAllSharingTypeWithSacrificedEffect` usages**
- [ ] **Deprecate executor**

---

## Phase 3: Dynamic GroupFilter from Chosen Values

Eliminates 4-5 bespoke "choose a creature type, then affect that type" executors.

### 3a. Extend GroupFilter to Reference Chosen Values

- [ ] **Add `GroupFilter.ChosenSubtype` variant** (or similar)
  - At resolution time, reads `chosenValues["chosenCreatureType"]` from EffectContext
  - Filters battlefield permanents that have the chosen subtype
- [ ] **Update `AffectsFilterResolver` / `predicateEvaluator`** to resolve `ChosenSubtype` filters
      against projected state
- [ ] **Add tests** for dynamic GroupFilter resolution

### 3b. Decompose `ChooseCreatureTypeModifyStatsEffect`

- [ ] **Express as pipeline:**
  ```
  ChooseOption(CREATURE_TYPE)
  → ForEachInGroup(ChosenSubtype, ModifyStats(+X/+Y))
  ```
- [ ] **Handle optional `grantKeyword`** — compose with `GrantKeywordUntilEndOfTurn` in the
      ForEachInGroup body
- [ ] **Migrate card usages** (Defensive Maneuvers, Tribal Unity, Tribal Forcemage)
- [ ] **Deprecate `ChooseCreatureTypeModifyStatsExecutor`**

### 3c. Decompose `BecomeChosenTypeAllCreaturesEffect`

- [ ] **Express as pipeline:**
  ```
  ChooseOption(CREATURE_TYPE, excludedOptions)
  → ForEachInGroup(AllCreatures, SetCreatureSubtypes(fromChosenValue))
  ```
- [ ] **Migrate card usages** (Standardize, etc.)
- [ ] **Deprecate `BecomeChosenTypeAllCreaturesExecutor`**

### 3d. Decompose `ChooseCreatureTypeGainControlEffect`

- [ ] **Express as pipeline:**
  ```
  ChooseOption(CREATURE_TYPE)
  → Conditional(youControlMostOfChosenType)
  → ForEachInGroup(ChosenSubtype, GainControl)
  ```
- [ ] **Requires `Condition.ControlsMostOfChosenType`** or similar
- [ ] **Migrate card usages** (Peer Pressure)
- [ ] **Deprecate `ChooseCreatureTypeGainControlExecutor`**

### 3e. Decompose `ChooseCreatureTypeMustAttackEffect`

- [ ] **Express as pipeline:**
  ```
  ChooseOption(CREATURE_TYPE)
  → ForEachInGroup(ChosenSubtype, MarkMustAttack)
  ```
- [ ] **Migrate card usages** (Walking Desecration)
- [ ] **Deprecate `ChooseCreatureTypeMustAttackExecutor`**

---

## Phase 4: Linked Exile Pipeline Support

Decompose linked exile patterns (Day of the Dragons, Dimensional Breach, etc.).

### 4a. Add `CardSource.FromLinkedExile`

- [ ] **Add `FromLinkedExile` variant to `CardSource`**
  - Reads `LinkedExileComponent` from the source entity
  - Returns the list of exiled entity IDs
- [ ] **Update `GatherCardsExecutor`** to handle `FromLinkedExile`
- [ ] **Add tests**

### 4b. Decompose `ExileGroupAndLinkEffect`

- [ ] **Express as pipeline:**
  ```
  GatherCards(BattlefieldMatching(filter)) → MoveCollection(Exile, linkToSource=true)
  ```
  - `linkToSource` flag already exists on `MoveCollectionEffect`
- [ ] **Verify `linkToSource` behavior** in `MoveCollectionExecutor`
- [ ] **Migrate card usages** (Day of the Dragons)
- [ ] **Deprecate `ExileGroupAndLinkExecutor`**

### 4c. Decompose `ReturnLinkedExileEffect`

- [ ] **Express as pipeline:**
  ```
  GatherCards(FromLinkedExile) → MoveCollection(Battlefield)
  ```
- [ ] **Handle `underOwnersControl` flag** — add controller routing to MoveCollectionEffect
- [ ] **Migrate card usages**
- [ ] **Deprecate `ReturnLinkedExileExecutor`**

### 4d. Add `FilterCollectionEffect` (Optional)

- [ ] **New pipeline atom:** Filter a named collection by predicate, output matches + non-matches
  - Useful for "destroy all except chosen subtypes" (Harsh Mercy) and similar patterns
  - Distinct from `SelectFromCollectionEffect` because no player choice — purely automatic
- [ ] **Implement `FilterCollectionExecutor`**
- [ ] **Add to EffectPatterns** where needed

---

## Do NOT Decompose (Correctly Atomic)

These effects are irreducibly complex or interact with engine internals that pipelines can't express:

| Effect | Reason |
|--------|--------|
| `DealDamageEffect`, `FightEffect` | Truly atomic damage operations |
| `GainLifeEffect`, `LoseLifeEffect`, `SetLifeTotalEffect` | Atomic life operations |
| `AddManaEffect`, `AddAnyColorManaEffect` | Atomic mana operations |
| `AddCountersEffect`, `RemoveCountersEffect` | Atomic counter operations |
| `ModifyStatsEffect`, `SetBasePowerEffect` | Atomic stat modifications |
| `GrantKeywordUntilEndOfTurnEffect` | Atomic keyword grants |
| `TapUntapEffect` | Atomic tap/untap |
| `GainControlEffect`, `ExchangeControlEffect` | Atomic control changes |
| `CreateTokenEffect`, `CreateTreasureTokensEffect` | Atomic token creation |
| `CounterSpellEffect`, `CounterUnlessPaysEffect` | Atomic stack manipulation |
| `RegenerateEffect`, `CantBeRegeneratedEffect` | Creates engine-internal shields |
| `MarkExileOnDeathEffect` | Creates floating SBA markers |
| `ExileUntilLeavesEffect` | Engine-wired source linkage |
| `AnimateLandEffect` | Multi-layer effect (TYPE + P/T simultaneously) |
| `StormCopyEffect`, `CopyTargetSpellEffect` | Deep stack manipulation |
| All prevention/redirection effects | Interact with damage pipeline internals |
| `GrantTriggeredAbilityUntilEndOfTurnEffect` | Grants ability objects, not zone/stat changes |
| `MoveToZoneEffect` | Already the atomic single-target zone primitive |

---

## Impact Summary

| Phase | New Atoms | Executors Eliminated | Card Coverage |
|-------|-----------|---------------------|---------------|
| 1 | 0 | 0 (migration only) | Convention alignment |
| 2 | 2 (`MoveType.Destroy`, `CardSource.BattlefieldMatching`) | 3 (`DestroyAll`, `ReturnAllToHand`, `DestroyAllSharingType`) | ~75% pipeline |
| 3 | 1 (`GroupFilter.ChosenSubtype`) | 4 (`ChooseCreatureTypeModifyStats`, `BecomeChosenTypeAllCreatures`, `ChooseCreatureTypeGainControl`, `ChooseCreatureTypeMustAttack`) | ~82% pipeline |
| 4 | 1-2 (`CardSource.FromLinkedExile`, optional `FilterCollectionEffect`) | 2 (`ExileGroupAndLink`, `ReturnLinkedExile`) | ~85% pipeline |
| **Total** | **4-5** | **~9** | **~85%** |
