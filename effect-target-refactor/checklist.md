. # EffectTarget Refactor — Checklist

**Goal:** Add composable `EffectTarget` variants (`PlayerRef`, `GroupRef`, `FilteredTarget`), migrate hardcoded variants to `ContextTarget(0)`, fix `ReturnFromGraveyardEffect`, simplify effect descriptions, and bridge `PlayerFilter` → `Player`.

---

## Phase 0 — Bundled Quick Wins
- [x] **0a** Make `AbilityId.generate()` thread-safe with `AtomicLong`
- [x] **0b** Remove `selectedCardIds` from `SearchLibraryEffect`
- [x] **0c** Builder dummy defaults → fail-fast (`requireNotNull(effect)`)
  - [x] `ModeBuilder`
  - [x] `TriggeredAbilityBuilder`
  - [x] `ActivatedAbilityBuilder`
  - [x] `LoyaltyAbilityBuilder`
- [x] **0d** Consistent `CantBlock()` defaults (remove explicit `StaticTarget.SourceCreature`)
  - [x] JungleLion
  - [x] CravenGiant
  - [x] HulkingCyclops
  - [x] HulkingGoblin

## Phase 1 — Add New Composable `EffectTarget` Variants
- [x] **1a** Add `PlayerRef(player: Player)` variant to `EffectTarget`
- [x] **1b** Add `GroupRef(filter: GroupFilter)` variant to `EffectTarget`
- [x] **1c** Add `FilteredTarget(filter: TargetFilter)` variant to `EffectTarget`

## Phase 2 — Fix `ReturnFromGraveyardEffect` Target Gap
- [x] **2a** Replace `filter: GameObjectFilter` with `target: EffectTarget` in `ReturnFromGraveyardEffect`
- [x] **2b** Fix card definitions missing target declarations:
  - [x] ElvenCache — add `TargetCardInGraveyard` requirement
  - [x] DejaVu — add `TargetCardInGraveyard` requirement
- [x] **2c** Update cards that already declare targets:
  - [x] Gravedigger — `filter` → `target = EffectTarget.ContextTarget(0)`
  - [x] RaiseDead — same pattern
  - [x] BreathOfLife — same pattern
- [x] **2d** Update engine: effect resolver reads `target` instead of inferring from context
- [x] **2e** Fix CastSpellHandler target validation gap (reject empty targets when spell requires them)

## Phase 3 — Migrate Hardcoded Filtered Targets → `ContextTarget(0)` in Cards
- [x] **3a** AngelicBlessing — `TargetCreature` → `ContextTarget(0)` (×2)
- [x] **3b** DefiantStand — `TargetCreature` → `ContextTarget(0)` (×2)
- [x] **3c** SternMarshal — `TargetCreature` → `ContextTarget(0)`
- [x] **3d** FalsePeace — `AnyPlayer` → `ContextTarget(0)`

## Phase 4 — Migrate Player/Group `EffectTarget` Variants in Effect Defaults
- [x] **4a** Map old defaults to new composable variants (see phase4.md table)
- [x] **4b** Update `DealDamageToPlayersEffect` — `EachPlayer` → `PlayerRef(Player.Each)`
- [x] **4c** Update `LoseLifeEffect` — `Opponent` → `PlayerRef(Player.TargetOpponent)`
- [x] **4d** Apply same pattern to remaining effects:
  - [x] `GainLifeEffect` (if applicable)
  - [x] `LoseHalfLifeEffect`
  - [x] `DiscardCardsEffect`
  - [x] `DiscardRandomEffect`
  - [x] `WheelEffect`
  - [x] `MillEffect`
  - [x] `ShuffleLibraryEffect`
  - [x] `SkipCombatPhasesEffect`
  - [x] `SkipUntapEffect`
  - [x] `TauntEffect`
- [x] **4e** Update WindsOfChange — `EachPlayer` → `PlayerRef(Player.Each)`
- [x] **4f** Verify cards relying on updated defaults need no changes (DrySpell, FireTempest, Earthquake, Hurricane)

## Phase 5 — Update DSL Facades
- [ ] **5a** Update `Effects.kt` facade defaults to use new composable variants
- [ ] **5b** Remove default from `DealDamage` — force explicit target
- [ ] **5c** Update `LoseLife`, `Discard`, `Sacrifice` defaults to `PlayerRef`

## Phase 6 — Bridge `PlayerFilter` → `Player`
- [ ] **6a** Add `@Deprecated` with `ReplaceWith` to `PlayerFilter` and all variants
- [ ] **6b** Change `GameEvent` types to use `Player` instead of `PlayerFilter`:
  - [ ] `DrawEvent`
  - [ ] `LifeGainEvent`
  - [ ] `LifeLossEvent`
  - [ ] `DiscardEvent`
  - [ ] `SearchLibraryEvent`
- [ ] **6c** Verify `ReplacementEffect` usages — defaults should just work
- [ ] **6d** Update engine: replacement effect matcher evaluates `Player`

## Phase 7 — Deprecate Old `EffectTarget` Variants
- [ ] **7a** Add `@Deprecated` to player/group variants:
  - [ ] `Opponent`
  - [ ] `AnyPlayer`
  - [ ] `EachPlayer`
  - [ ] `EachOpponent`
  - [ ] `AllCreatures`
  - [ ] `AllControlledCreatures`
  - [ ] `AllOpponentCreatures`
- [ ] **7b** Add `@Deprecated` to hardcoded filtered variants:
  - [ ] `TargetCreature`
  - [ ] `TargetOpponentCreature`
  - [ ] `TargetControlledCreature`
  - [ ] `TargetPermanent`
  - [ ] `TargetNonlandPermanent`
  - [ ] `TargetLand`
  - [ ] `TargetNonblackCreature`
  - [ ] `TargetTappedCreature`
  - [ ] `TargetCreatureWithFlying`
  - [ ] `AnyTarget`
  - [ ] `TargetCardInGraveyard`
  - [ ] `TargetEnchantment`
  - [ ] `TargetArtifact`
  - [ ] `TargetOpponentNonlandPermanent`

## Phase 8 — Final Deletion
- [ ] **8a** Delete all deprecated `EffectTarget` variants
- [ ] **8b** Delete `PlayerFilter` sealed interface entirely from `EventFilters.kt`
- [ ] **8c** Full test suite passes with no deprecation warnings from retired types

## Phase 9 — Add `MoveToZoneEffect`, `ZonePlacement`, and `TargetObject`
- [ ] **9a** Add `ZonePlacement` enum to SDK (`ZonePlacement.kt`)
- [ ] **9b** Add `MoveToZoneEffect` data class to SDK (`RemovalEffects.kt` or new file)
- [ ] **9c** Add `TargetObject` to `TargetRequirement.kt` (generalizes `TargetCardInGraveyard`)
- [ ] **9d** Create `MoveToZoneEffectExecutor` in rules-engine
- [ ] **9e** Register executor in `EffectExecutorRegistry`
- [ ] **9f** Add `TargetObject` validation in `TargetValidator`

## Phase 10 — Migrate Cards to `MoveToZoneEffect`
- [ ] **10a** Migrate `ReturnFromGraveyardEffect` → `MoveToZoneEffect` (~10 cards):
  - [ ] Gravedigger, RaiseDead, BreathOfLife, ElvenCache, DejaVu
  - [ ] + any other cards using ReturnFromGraveyardEffect
- [ ] **10b** Migrate `ReturnToHandEffect` → `MoveToZoneEffect` (~9 cards):
  - [ ] Man-o'-War, Rescue, Time Ebb (bounce effects), etc.
- [ ] **10c** Migrate `PutOnTopOfLibraryEffect` → `MoveToZoneEffect` (~4 cards)
- [ ] **10d** Migrate `ShuffleIntoLibraryEffect` → `MoveToZoneEffect` (~2 cards)
- [ ] **10e** Migrate `DestroyEffect` → `MoveToZoneEffect` (~32 cards):
  - [ ] Terror, Dark Banishing, Wrath of God (single-target destroy), etc.
- [ ] **10f** Migrate `ExileEffect` → `MoveToZoneEffect` (0 cards — just verify)
- [ ] **10g** Migrate `TargetCardInGraveyard` → `TargetObject` in card target declarations
- [ ] **10h** Update TestCards.kt and scenario tests

## Phase 11 — DSL Convenience + Deprecate/Delete Old Zone Effects
- [ ] **11a** Add DSL convenience functions (`Effects.Destroy()`, `Effects.Exile()`, `Effects.ReturnToHand()`, etc.)
- [ ] **11b** Deprecate old effect types:
  - [ ] `DestroyEffect`
  - [ ] `ReturnToHandEffect`
  - [ ] `ReturnFromGraveyardEffect`
  - [ ] `ExileEffect`
  - [ ] `ShuffleIntoLibraryEffect`
  - [ ] `PutOnTopOfLibraryEffect`
- [ ] **11c** Deprecate `TargetCardInGraveyard` in favor of `TargetObject`
- [ ] **11d** Delete deprecated effect types and their executors
- [ ] **11e** Full test suite passes
