# SDK Filter Unification — Checklist

**Goal:** Retire `PermanentTargetFilter`, `SpellTargetFilter`, and `CreatureFilter`. Card authors only use `GameObjectFilter`, `TargetFilter`, and `GroupFilter`.

---

## Phase 0 — Fix `SpellBuilder.condition` Bug
- [x] Wire `SpellBuilder.condition` into `ConditionalEffect` in `CardBuilder.kt`
- [x] Verify Gift of Estates only searches when opponent controls more lands

## Phase 1 — Prepare the Unified Types
- [x] **1a** Add missing `GameObjectFilter` pre-builts in `ObjectFilter.kt` (`CreatureOrLand`, `CreatureOrSorcery`, `NoncreaturePermanent`)
- [x] **1b** Add missing `TargetFilter` pre-builts in `TargetFilter.kt` (`PermanentOpponentControls`, `CreatureOrLandPermanent`, `NoncreaturePermanent`, stack-based spell filters)
- [x] **1c** Fix `GroupFilter` pluralization — remove the `contains(" ")` guard so "green creature" → "all green creatures"

## Phase 2 — Migrate `PermanentTargetFilter`
- [x] **2a** Change `TargetPermanent` to accept `TargetFilter` instead of `PermanentTargetFilter` in `TargetRequirement.kt`
- [x] **2b** Change `DestroyAllEffect` to accept `GroupFilter` instead of `PermanentTargetFilter` in `RemovalEffects.kt`
- [x] **2c** Update `Targets` DSL facade in `Targets.kt` (Permanent, NonlandPermanent, Artifact, Enchantment, Land)
- [x] **2d** Update card definitions (13 cards):
  - [x] Armageddon
  - [x] BoilingSeas
  - [x] Flashfires
  - [x] NaturesRuin
  - [x] VirtuesRuin
  - [x] WrathOfGod
  - [x] Devastation
  - [x] WintersGrasp
  - [x] RainOfTears
  - [x] StoneRain
  - [x] RainOfSalt
  - [x] LavaFlow
  - [x] FireSnake
- [x] **2e** Mark `PermanentTargetFilter` as `@Deprecated`
- [x] **2f** Update engine: target validation for `TargetPermanent` and `DestroyAllEffect` resolution

## Phase 3 — Migrate `SpellTargetFilter`
- [x] **3a** Change `TargetSpell` to accept `TargetFilter` instead of `SpellTargetFilter` in `TargetRequirement.kt`
- [x] **3b** Update `Targets` DSL facade (Spell, CreatureSpell, NoncreatureSpell, CreatureOrSorcerySpell, SpellWithManaValueAtMost)
- [x] **3c** Verify card definitions (MysticDenial handled by DSL change — no card file edits needed; TestCards.kt updated)
- [x] **3d** Mark `SpellTargetFilter` as `@Deprecated`
- [x] **3e** Update engine: target validation for `TargetSpell`

## Phase 4 — Migrate `CreatureFilter`
- [x] **4a** Change `GlobalEffect` to accept `GroupFilter` instead of `CreatureFilter` in `StaticAbility.kt`
- [x] **4b** Update `Filters` DSL facade (AllCreatures, CreaturesYouControl, CreaturesOpponentsControl, CreaturesWithKeyword, CreaturesWithoutKeyword)
- [x] **4c** Scan all sets for `GlobalEffect` usage and update card definitions if needed
- [x] **4d** Mark `CreatureFilter` as `@Deprecated`
- [x] **4e** Update engine: `GlobalEffect` application in state projector

## Phase 5 — Cleanup & Verification
- [x] **5a** Delete `PermanentTargetFilter` sealed interface and all variants
- [x] **5b** Delete `SpellTargetFilter` sealed interface and all variants
- [x] **5c** Delete `CreatureFilter` sealed interface and all variants
- [x] **5d** Remove empty `references/Zone.kt` comment file
- [x] **5e** Update `Filters` DSL documentation — deprecate scattered top-level aliases, point to `Filters.Group.*`
- [x] **5f** Add `GroupFilter` convenience companions for common destroy-all patterns (color, subtype)
- [x] **5g** Full test suite passes with no deprecation warnings from retired types
