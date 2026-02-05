## Phase 6 — Final Cleanup (After Engine Migration)

Once the engine team has migrated off `ZoneType` and `CostZone`:

**Files changed**: 2 deletions + removal of deprecated code

### Step 6a — Delete `ZoneType`

```
Delete: core/ZoneType.kt
```

### Step 6b — Delete `CostZone`

Remove the `CostZone` enum from `AdditionalCost.kt`. The `toZone()` bridge method is no longer needed.

### Step 6c — Remove bridge methods

Any remaining `toZone()` calls in the engine should have been replaced with direct `Zone` usage by this point.

### Step 6d — Verify

Full test suite passes with no deprecation warnings from retired types.

**Gating**: This phase only ships after confirming no remaining usages of `ZoneType` or `CostZone` in engine code.
