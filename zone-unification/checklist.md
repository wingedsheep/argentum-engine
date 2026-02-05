# Zone Unification — Checklist

**Goal:** Retire `ZoneType` and `CostZone`. Card authors and engine code use only `Zone` (from `core/`) for zone identity. `SearchDestination` stays separate.

---

## Phase 0 — Create Canonical `Zone` in `core/`
- [ ] Create `core/Zone.kt` with `description`, `simpleName`, `isPublic`, `isHidden`, `isShared`
- [ ] Verify it compiles alongside existing `scripting/events/Zone.kt` (different package, no conflict)

## Phase 1 — Bridge the Scripting Layer with a Typealias
- [ ] **1a** Replace `scripting/events/Zone.kt` enum with `typealias Zone = com.wingedsheep.sdk.core.Zone`
- [ ] **1b** Verify all existing scripting code compiles with zero import changes
- [ ] **1c** Verify serialized JSON is unchanged (enum constant names are identical)

## Phase 2 — Migrate `ZoneType` to `Zone`
- [ ] **2a** Change `ActivatedAbility.activateFromZone` from `ZoneType` to `Zone` in `ActivatedAbility.kt`
- [ ] **2b** Update any card definitions using `activateFromZone` with `ZoneType` values
- [ ] **2c** Deprecate `ZoneType` with `@Deprecated` annotation and `toZone()` bridge method
- [ ] **2d** Verify engine compiles (may need `toZone()` calls at boundary)

## Phase 3 — Migrate `CostZone` to `Zone`
- [ ] **3a** Change `AdditionalCost.ExileCards.fromZone` from `CostZone` to `Zone`
- [ ] **3b** Update description to use `Zone.simpleName` instead of `CostZone.description`
- [ ] **3c** Update any card definitions using `ExileCards` with `CostZone` values
- [ ] **3d** Deprecate `CostZone` with `@Deprecated` annotation and `toZone()` bridge method

## Phase 4 — Document `SearchDestination` Relationship
- [ ] **4a** Add KDoc to `SearchDestination` explaining relationship to `Zone`
- [ ] **4b** Cross-reference `Zone` from `SearchDestination` docs

## Phase 5 — Delete `references/Zone.kt` Comment File
- [ ] **5a** Delete `scripting/references/Zone.kt` (replaced by typealias in Phase 1)

## Phase 6 — Final Cleanup (After Engine Migration)
- [ ] **6a** Delete `core/ZoneType.kt` entirely
- [ ] **6b** Delete `CostZone` enum from `AdditionalCost.kt`
- [ ] **6c** Remove `toZone()` bridge methods
- [ ] **6d** Full test suite passes with no deprecation warnings from retired types
