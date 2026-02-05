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
- [x] **5a** Update `Effects.kt` facade defaults to use new composable variants
- [x] **5b** Remove default from `DealDamage` — force explicit target
- [x] **5c** Update `LoseLife`, `Discard`, `Sacrifice` defaults to `PlayerRef`

## Phase 6 — Bridge `PlayerFilter` → `Player`
- [x] **6a** Add `@Deprecated` with `ReplaceWith` to `PlayerFilter` and all variants
- [x] **6b** Change `GameEvent` types to use `Player` instead of `PlayerFilter`:
  - [x] `DrawEvent`
  - [x] `LifeGainEvent`
  - [x] `LifeLossEvent`
  - [x] `DiscardEvent`
  - [x] `SearchLibraryEvent`
- [x] **6c** Verify `ReplacementEffect` usages — defaults should just work
- [x] **6d** Update engine: replacement effect matcher evaluates `Player` (no engine code referenced PlayerFilter — already clean)

## Phase 7 — Deprecate Old `EffectTarget` Variants
*Skipped — went directly to deletion in Phase 8.*

## Phase 8 — Final Deletion
- [x] **8a** Delete all deprecated `EffectTarget` variants (21 variants removed)
- [x] **8b** Delete `PlayerFilter` sealed interface entirely from `EventFilters.kt`
- [x] **8c** Full test suite passes with no deprecation warnings from retired types

## Phase 9 — Add `MoveToZoneEffect`, `ZonePlacement`, and `TargetObject`
- [x] **9a** Add `ZonePlacement` enum to SDK (`ZonePlacement.kt`)
- [x] **9b** Add `MoveToZoneEffect` data class to SDK (`RemovalEffects.kt`)
- [x] **9c** Add `TargetObject` to `TargetRequirement.kt` (generalizes `TargetCardInGraveyard`)
- [x] **9d** Create `MoveToZoneEffectExecutor` in rules-engine
- [x] **9e** Register executor in `RemovalExecutors`
- [x] **9f** Add `TargetObject` validation in `TargetValidator` and `TargetFinder`

## Phase 10 — Migrate Cards to `MoveToZoneEffect`
- [x] **10a** Migrate `ReturnFromGraveyardEffect` → `MoveToZoneEffect` (5 cards):
  - [x] Gravedigger, RaiseDead, BreathOfLife, ElvenCache, DejaVu
- [x] **10b** Migrate `ReturnToHandEffect` → `MoveToZoneEffect` (4 cards):
  - [x] Man-o'-War, EndlessCockroaches, CommandOfUnsummoning, SymbolOfUnsummoning
- [x] **10c** Migrate `PutOnTopOfLibraryEffect` → `MoveToZoneEffect` (2 cards):
  - [x] TimeEbb, UndyingBeast
- [x] **10d** Migrate `ShuffleIntoLibraryEffect` → `MoveToZoneEffect` (1 card):
  - [x] AlabasterDragon
- [x] **10e** Migrate `DestroyEffect` → `MoveToZoneEffect` (15 cards):
  - [x] Swat, Smother, AssassinsBlade, FireSnake, SerpentAssassin, WintersGrasp
  - [x] WickedPact, HandOfDeath, KingsAssassin, PathOfPeace, Vengeance
  - [x] RainOfSalt, StoneRain, RainOfTears, LavaFlow
- [x] **10f** Migrate `ExileEffect` → `MoveToZoneEffect` (0 cards — verified none in mtg-sets)
- [x] **10g** Migrate `TargetCardInGraveyard` → `TargetObject` in card target declarations
  - [x] Gravedigger, RaiseDead, BreathOfLife, ElvenCache, DejaVu
- [x] **10h** Update TestCards.kt, CardDslTest.kt, GravediggerTest.kt — all migrated
- [x] **10i** Full build passes, all tests green

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
