# Zone Unification — Implementation Plan

## Goals

1. **Create canonical `Zone` in `core/`** with all properties (`description`, `simpleName`, `isPublic`, `isHidden`, `isShared`)
2. **Retire `ZoneType`** — replace with `Zone` (only 1 SDK usage in `ActivatedAbility`)
3. **Retire `CostZone`** — replace with `Zone` using `simpleName` (only 1 SDK usage in `AdditionalCost.ExileCards`)
4. **Keep `SearchDestination` separate** — it represents movement semantics with prepositions, not zone identity
5. End state: card authors only think in `Zone` for zone identity, `SearchDestination` for effect destinations

---

## File Change Summary

| Phase | Files Changed | Description | Risk |
|-------|--------------|-------------|------|
| **0** — Canonical Zone | 1 new (`core/Zone.kt`) | Create unified Zone enum | None — additive |
| **1** — Scripting bridge | 1 modified (`scripting/events/Zone.kt`) | Replace enum with typealias | Low — transparent to all consumers |
| **2** — Migrate ZoneType | 2 modified (`ActivatedAbility.kt`, `ZoneType.kt`) | Change field + deprecate | Low — 1 field change |
| **3** — Migrate CostZone | 1 modified (`AdditionalCost.kt`) | Change field + deprecate | Low — 1 field change, uses `simpleName` |
| **4** — Document SearchDestination | 1 modified (`LibraryEffects.kt`) | Add relationship docs | None — docs only |
| **5** — Delete comment file | 1 deleted (`references/Zone.kt`) | Remove dead weight | None |
| **6** — Final cleanup | 2 deleted (`ZoneType.kt`, `CostZone` from `AdditionalCost.kt`) | Remove deprecated types | Low — after engine migration |

**Total**: 6 files touched, 1 new, 1 deleted, 4 modified (+ 2 deferred deletions)

---

## Engine Impact

> The engine pattern-matches on `ZoneType` for zone management. The deprecated `ZoneType` provides a `toZone()` bridge
> method for incremental engine migration.

| Retired SDK Type | Engine Must Update |
|---|---|
| `ZoneType` → `Zone` | Wherever engine pattern-matches on `ZoneType` values |
| `ZoneType.isPublic/isHidden/isShared` | Same property names on `Zone` — drop-in replacement |

The engine already knows about `Zone` from the scripting layer, so this is a **convergence** of two types the engine
already handles, not a new concept.

---

## Mapping Reference for Engine Team

| `ZoneType` | `Zone` |
|------------|--------|
| `ZoneType.BATTLEFIELD` | `Zone.Battlefield` |
| `ZoneType.GRAVEYARD` | `Zone.Graveyard` |
| `ZoneType.HAND` | `Zone.Hand` |
| `ZoneType.LIBRARY` | `Zone.Library` |
| `ZoneType.STACK` | `Zone.Stack` |
| `ZoneType.EXILE` | `Zone.Exile` |
| `ZoneType.COMMAND` | `Zone.Command` |
| `zoneType.isPublic` | `zone.isPublic` *(same property name)* |
| `zoneType.isHidden` | `zone.isHidden` |
| `zoneType.isShared` | `zone.isShared` |

---

## Recommended Execution Order

```
Phase 0 + 1  <-  ship together (creates canonical type + transparent bridge)
    |
Phase 2      <-  ship next (tiny change, enables ZoneType deprecation)
    |
Phase 3      <-  ship next (tiny change, enables CostZone deprecation)
    |
Phase 4 + 5  <-  ship together (docs + delete comment file)
    |
Phase 6      <-  ship after engine team confirms migration complete
```

Phases 0-5 can ship in a single PR if preferred — the total diff is small. Phase 6 is gated on external consumers.
