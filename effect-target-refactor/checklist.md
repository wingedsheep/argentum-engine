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
- [ ] **1a** Add `PlayerRef(player: Player)` variant to `EffectTarget`
- [ ] **1b** Add `GroupRef(filter: GroupFilter)` variant to `EffectTarget`
- [ ] **1c** Add `FilteredTarget(filter: TargetFilter)` variant to `EffectTarget`

## Phase 2 — Fix `ReturnFromGraveyardEffect` Target Gap
- [ ] **2a** Replace `filter: GameObjectFilter` with `target: EffectTarget` in `ReturnFromGraveyardEffect`
- [ ] **2b** Fix card definitions missing target declarations:
  - [ ] ElvenCache — add `TargetCardInGraveyard` requirement
  - [ ] DejaVu — add `TargetCardInGraveyard` requirement
- [ ] **2c** Update cards that already declare targets:
  - [ ] Gravedigger — `filter` → `target = EffectTarget.ContextTarget(0)`
  - [ ] RaiseDead — same pattern
  - [ ] BreathOfLife — same pattern
- [ ] **2d** Update engine: effect resolver reads `target` instead of inferring from context

## Phase 3 — Migrate Hardcoded Filtered Targets → `ContextTarget(0)` in Cards
- [ ] **3a** AngelicBlessing — `TargetCreature` → `ContextTarget(0)` (×2)
- [ ] **3b** DefiantStand — `TargetCreature` → `ContextTarget(0)` (×2)
- [ ] **3c** SternMarshal — `TargetCreature` → `ContextTarget(0)`
- [ ] **3d** FalsePeace — `AnyPlayer` → `ContextTarget(0)`

## Phase 4 — Migrate Player/Group `EffectTarget` Variants in Effect Defaults
- [ ] **4a** Map old defaults to new composable variants (see phase4.md table)
- [ ] **4b** Update `DealDamageToPlayersEffect` — `EachPlayer` → `PlayerRef(Player.Each)`
- [ ] **4c** Update `LoseLifeEffect` — `Opponent` → `PlayerRef(Player.TargetOpponent)`
- [ ] **4d** Apply same pattern to remaining effects:
  - [ ] `GainLifeEffect` (if applicable)
  - [ ] `LoseHalfLifeEffect`
  - [ ] `DiscardCardsEffect`
  - [ ] `DiscardRandomEffect`
  - [ ] `WheelEffect`
  - [ ] `MillEffect`
  - [ ] `ShuffleLibraryEffect`
  - [ ] `SkipCombatPhasesEffect`
  - [ ] `SkipUntapEffect`
  - [ ] `TauntEffect`
- [ ] **4e** Update WindsOfChange — `EachPlayer` → `PlayerRef(Player.Each)`
- [ ] **4f** Verify cards relying on updated defaults need no changes (DrySpell, FireTempest, Earthquake, Hurricane)

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
