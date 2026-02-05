# Filter Unification & GiftOfEstates Bug Fix — Implementation Plan

## Goals

1. **Fix the GiftOfEstates `condition` bug** (runtime correctness, standalone fix)
2. **Retire `PermanentTargetFilter`** — replace with `TargetFilter`/`GroupFilter` (largest surface area)
3. **Retire `SpellTargetFilter`** — replace with `TargetFilter` (small surface area)
4. **Retire `CreatureFilter`** — replace with `GroupFilter` (smallest, possibly only in `GlobalEffect`)
5. End state: card authors only think in `GameObjectFilter`, `TargetFilter`, and `GroupFilter`

---

## File Change Summary

| Phase                                   | Files Changed                                                 | Risk                          |
|-----------------------------------------|---------------------------------------------------------------|-------------------------------|
| **0** — Bug fix                         | 1 (`CardBuilder.kt`)                                          | Low — isolated change         |
| **1** — Prep unified types              | 3 (`ObjectFilter.kt`, `TargetFilter.kt`, `GroupFilter.kt`)    | Low — additive only           |
| **2** — Migrate `PermanentTargetFilter` | 18 (1 SDK type + 1 effect + 1 DSL + 13 cards + 1 deprecation) | Medium — largest surface area |
| **3** — Migrate `SpellTargetFilter`     | 3 (1 SDK type + 1 DSL + 1 deprecation)                        | Low — small surface           |
| **4** — Migrate `CreatureFilter`        | 3 (1 SDK type + 1 DSL + 1 deprecation)                        | Low — may need other-set scan |
| **5** — Cleanup                         | 3–4 (deletions + doc updates)                                 | Low — removing dead code      |

**Total**: ~28 files, phased so each step is independently shippable and testable.

---

## Engine Impact

> The engine pattern-matches on these filter types to evaluate targeting and effect resolution. Each retired type
> requires a corresponding engine change.

| Retired SDK Type                         | Engine Must Update                            |
|------------------------------------------|-----------------------------------------------|
| `PermanentTargetFilter` → `TargetFilter` | Target validation for `TargetPermanent`       |
| `PermanentTargetFilter` → `GroupFilter`  | `DestroyAllEffect` resolution                 |
| `SpellTargetFilter` → `TargetFilter`     | Target validation for `TargetSpell`           |
| `CreatureFilter` → `GroupFilter`         | `GlobalEffect` application in state projector |

The engine already evaluates `GameObjectFilter` (for `TargetCreature` targets) and `GroupFilter` (for
`ModifyStatsForGroupEffect`, etc.), so the new evaluators are **extensions of existing code paths**, not greenfield
work.

---

## Recommended Execution Order

```
Phase 0  ←  ship immediately (bug fix)
  ↓
Phase 1  ←  ship next (additive, no breakage)
  ↓
Phase 2  ←  largest batch, do with engine team
  ↓
Phase 3  ←  small follow-up
  ↓
Phase 4  ←  small follow-up
  ↓
Phase 5  ←  cleanup after all sets verified
```

Phases 2–4 can be parallelized if different engineers work on each, since they touch non-overlapping filter types. Phase
2 is the critical path.
