## Phase 8 — Final Deletion

After verifying no remaining references across all sets and the engine.

**Files changed**: 2

### Step 8a — Delete deprecated `EffectTarget` variants

Remove all 21 deprecated variants from `EffectTarget.kt`:

**Player/group variants (7):**
- `Opponent`
- `AnyPlayer`
- `EachPlayer`
- `EachOpponent`
- `AllCreatures`
- `AllControlledCreatures`
- `AllOpponentCreatures`

**Hardcoded filtered variants (14):**
- `TargetCreature`
- `TargetOpponentCreature`
- `TargetControlledCreature`
- `TargetPermanent`
- `TargetNonlandPermanent`
- `TargetLand`
- `TargetNonblackCreature`
- `TargetTappedCreature`
- `TargetCreatureWithFlying`
- `AnyTarget`
- `TargetCardInGraveyard`
- `TargetEnchantment`
- `TargetArtifact`
- `TargetOpponentNonlandPermanent`

### Step 8b — Delete `PlayerFilter` sealed interface

Remove the entire `PlayerFilter` sealed interface from `EventFilters.kt` (lines 270–293).

### Step 8c — Verify

```bash
# Verify no remaining references to deleted types
grep -rn "PlayerFilter" mtg-sdk/ rules-engine/ mtg-sets/ game-server/ --include="*.kt"
grep -rn "EffectTarget\.\(Opponent\|AnyPlayer\|EachPlayer\|EachOpponent\)" mtg-sdk/ rules-engine/ mtg-sets/ --include="*.kt"
grep -rn "EffectTarget\.\(AllCreatures\|AllControlledCreatures\|AllOpponentCreatures\)" mtg-sdk/ rules-engine/ mtg-sets/ --include="*.kt"
grep -rn "EffectTarget\.\(TargetCreature\|TargetPermanent\|TargetLand\|AnyTarget\)" mtg-sdk/ rules-engine/ mtg-sets/ --include="*.kt"

# Full test suite
./gradlew test
```
