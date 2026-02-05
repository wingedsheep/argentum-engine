# EffectTarget Refactor — Implementation Plan

## Goals

1. **Add composable `EffectTarget` variants** (`PlayerRef`, `GroupRef`, `FilteredTarget`) that leverage the unified filter system
2. **Migrate card definitions** from hardcoded `EffectTarget` variants to `ContextTarget(0)` bindings
3. **Fix `ReturnFromGraveyardEffect`** missing explicit target reference (+ card definition bugs)
4. **Simplify effect `description` logic** — eliminate growing `when` blocks in effect types
5. **Bridge `PlayerFilter` → `Player`** — remove the duplicate type
6. **Bundle four quick wins** identified in the analysis (thread safety, dummy defaults, dead field, CantBlock consistency)
7. **Unify zone-moving effects** into a single `MoveToZoneEffect(target, destination, placement)` with `TargetObject` requirement

---

## Current State Audit

### EffectTarget Variant Usage in Portal Cards

| Variant | Used In Cards | Role | Action |
|---------|--------------|------|--------|
| `Controller` | ~12 cards (life effects) | Self-reference | **Keep** |
| `Self` | 5 cards (Endless Cockroaches, Undying Beast, etc.) | Self-reference | **Keep** |
| `ContextTarget(i)` | ~50 cards | Binding | **Keep** |
| `StoredEntityTarget` | 0 cards (engine use) | Binding | **Keep** |
| `EnchantedCreature` | 0 Portal cards | Self-reference | **Keep** |
| `TargetController` | 0 Portal cards | Self-reference | **Keep** |
| `TargetCreature` | 3 cards | Hardcoded filter | **→ `ContextTarget(0)`** |
| `AnyPlayer` | 1 card (FalsePeace) | Hardcoded player | **→ `ContextTarget(0)`** |
| `EachPlayer` | 1 card (WindsOfChange) + effect defaults | Group player | **→ `PlayerRef(Player.Each)`** |
| `Opponent` | 0 cards, effect defaults only | Hardcoded player | **→ `PlayerRef`** |
| `EachOpponent` | 0 cards, effect defaults only | Group player | **→ `PlayerRef`** |
| `AllCreatures` | 0 cards, effect defaults only | Group | **→ `GroupRef`** |
| `AllControlledCreatures` | 0 cards, effect defaults only | Group | **→ `GroupRef`** |
| `AllOpponentCreatures` | 0 cards, effect defaults only | Group | **→ `GroupRef`** |
| `AnyTarget` | 0 cards, effect defaults only | Hardcoded filter | **→ `ContextTarget`** |
| `TargetNonblackCreature` | 0 cards | Hardcoded filter | **Deprecate** |
| `TargetTappedCreature` | 0 cards | Hardcoded filter | **Deprecate** |
| `TargetOpponentCreature` | 0 cards | Hardcoded filter | **Deprecate** |
| `TargetControlledCreature` | 0 cards | Hardcoded filter | **Deprecate** |
| `TargetPermanent` | 0 cards | Hardcoded filter | **Deprecate** |
| `TargetNonlandPermanent` | 0 cards | Hardcoded filter | **Deprecate** |
| `TargetLand` | 0 cards | Hardcoded filter | **Deprecate** |
| `TargetEnchantment` | 0 cards | Hardcoded filter | **Deprecate** |
| `TargetArtifact` | 0 cards | Hardcoded filter | **Deprecate** |
| `TargetOpponentNonlandPermanent` | 0 cards | Hardcoded filter | **Deprecate** |
| `TargetCreatureWithFlying` | 0 cards | Hardcoded filter | **Deprecate** |
| `TargetCardInGraveyard` | 0 cards | Hardcoded filter | **Deprecate** |

### Key Insight

The Portal cards already overwhelmingly use `ContextTarget(0)`. Only **4 cards** use hardcoded filtered variants, and only **1 card** uses a group/player variant directly. The bulk of the work is in the **effect type definitions** and **DSL facades**, not in card files.

---

## File Change Summary

| Phase | Description | Files | Risk |
|-------|-------------|-------|------|
| **0** | Quick wins | 5 (AbilityId, LibraryEffects, CardBuilder, 4 card files) | Low |
| **1** | Add new EffectTarget variants | 1 (EffectTarget.kt) | Low — additive |
| **2** | Fix ReturnFromGraveyardEffect | 7 (1 effect + 5 cards + 1 DSL import) | Medium — signature change |
| **3** | Cards: hardcoded → ContextTarget(0) | 4 card files | Low — mechanical |
| **4** | Effect defaults + descriptions | ~10 effect files + 1 card | Medium — most logic changes |
| **5** | DSL facade updates | 1 (Effects.kt) | Low |
| **6** | PlayerFilter → Player bridge | 2 (GameEvent.kt, EventFilters.kt) | Low — type swap |
| **7** | Deprecate old variants | 1 (EffectTarget.kt) | Low — annotations only |
| **8** | Delete deprecated code | 2 files | Low — after verification |
| **9** | Add MoveToZoneEffect + TargetObject | 4 (SDK types + engine executor + registry) | Medium — new effect type |
| **10** | Migrate cards to MoveToZoneEffect | ~57 card files + 6 old executors | High — largest migration |
| **11** | DSL convenience functions + cleanup | ~3 (Effects.kt facade + deprecation + deletion) | Low — after migration |

**Total**: ~40+ files

---

## Engine Impact

| SDK Change | Engine Must Update |
|---|---|
| `ReturnFromGraveyardEffect` gains `target` field | Effect resolver reads `target` instead of inferring from context |
| New `EffectTarget.PlayerRef` | Target resolver matches `PlayerRef.player` to resolve player reference |
| New `EffectTarget.GroupRef` | Effect resolver iterates `GroupRef.filter` (same path as existing `GroupFilter` effects) |
| New `EffectTarget.FilteredTarget` | Target resolver evaluates `filter.baseFilter` against game state |
| `GameEvent` uses `Player` instead of `PlayerFilter` | Replacement effect matcher evaluates `Player` (superset of `PlayerFilter`) |
| New `MoveToZoneEffect` | New unified executor resolves target, moves to destination zone with placement/destruction semantics |
| New `TargetObject` requirement | Target validator evaluates `TargetFilter` across any zone (generalizes `TargetCardInGraveyard`) |

The `PlayerRef` and `GroupRef` resolvers are extensions of code paths that already handle `Player` and `GroupFilter` in other contexts (e.g., `DynamicAmount.Count`, `ModifyStatsForGroupEffect`). No fundamentally new evaluation logic is needed.

The `MoveToZoneEffectExecutor` consolidates logic from 6 existing executors (Destroy, Exile, ReturnToHand, ReturnFromGraveyard, ShuffleIntoLibrary, PutOnTopOfLibrary). The `byDestruction` flag preserves indestructible/regeneration replacement effect handling.

---

## Recommended Execution Order

```
Phase 0   ←  ship immediately (independent quick wins)
  ↓
Phase 1   ←  ship next (additive, enables all later phases)
  ↓
Phase 2   ←  ReturnFromGraveyardEffect fix (correctness)
  ↓
Phase 3   ←  card migrations (mechanical, small)
  ↓
Phase 4   ←  effect defaults + descriptions (largest logic change)
  ↓
Phase 5   ←  DSL update (small follow-up)
  ↓
Phase 6   ←  PlayerFilter bridge (independent, can parallelize with 3-5)
  ↓
Phase 7   ←  deprecation annotations
  ↓
Phase 8   ←  deletion (after all sets + engine verified)
  ↓
Phase 9   ←  add MoveToZoneEffect + ZonePlacement + TargetObject (foundation)
  ↓
Phase 10  ←  migrate cards from old zone-moving effects (largest migration)
  ↓
Phase 11  ←  DSL convenience functions + deprecate/delete old effects
```

Phases 3, 5, and 6 can run in parallel — they touch non-overlapping files. Phase 4 is the critical path. Phase 10 is the largest migration (57 card files) and can be split into sub-batches by effect type.

---

## Unified `MoveToZoneEffect` Design (Phases 9–11)

### Motivation

Six separate effect types all do the same thing — move an object from one zone to another. They differ only in destination and semantics:

| Current Effect | Cards | Unified Form |
|----------------|-------|-------------|
| `DestroyEffect(target)` | 32 | `MoveToZoneEffect(target, Zone.Graveyard, byDestruction = true)` |
| `ReturnFromGraveyardEffect(target, dest)` | 10 | `MoveToZoneEffect(target, dest)` |
| `ReturnToHandEffect(target)` | 9 | `MoveToZoneEffect(target, Zone.Hand)` |
| `PutOnTopOfLibraryEffect(target)` | 4 | `MoveToZoneEffect(target, Zone.Library, ZonePlacement.Top)` |
| `ShuffleIntoLibraryEffect(target)` | 2 | `MoveToZoneEffect(target, Zone.Library, ZonePlacement.Shuffled)` |
| `ExileEffect(target)` | 0 | `MoveToZoneEffect(target, Zone.Exile)` |

### New SDK Types

```kotlin
data class MoveToZoneEffect(
    val target: EffectTarget,
    val destination: Zone,
    val placement: ZonePlacement = ZonePlacement.Default,
    val byDestruction: Boolean = false
) : Effect {
    override val description: String = buildString {
        when {
            byDestruction -> append("Destroy ${target.description}")
            destination == Zone.Hand -> append("Return ${target.description} to its owner's hand")
            destination == Zone.Exile -> append("Exile ${target.description}")
            destination == Zone.Library && placement == ZonePlacement.Shuffled ->
                append("Shuffle ${target.description} into its owner's library")
            destination == Zone.Library && placement == ZonePlacement.Top ->
                append("Put ${target.description} on top of its owner's library")
            destination == Zone.Battlefield && placement == ZonePlacement.Tapped ->
                append("Put ${target.description} onto the battlefield tapped")
            destination == Zone.Battlefield ->
                append("Put ${target.description} onto the battlefield")
            else -> append("Put ${target.description} into ${destination.description}")
        }
    }
}

enum class ZonePlacement { Default, Top, Bottom, Shuffled, Tapped }
```

### New `TargetObject` Requirement

Generalizes `TargetCardInGraveyard` to target objects in any zone based on a `TargetFilter`:

```kotlin
data class TargetObject(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: TargetFilter
) : TargetRequirement
```

The `TargetFilter` already carries zone information (e.g., `TargetFilter.CreatureInYourGraveyard`), so the requirement doesn't need a separate zone field.

### DSL Convenience Functions

```kotlin
object Effects {
    fun Destroy(target: EffectTarget) =
        MoveToZoneEffect(target, Zone.Graveyard, byDestruction = true)
    fun Exile(target: EffectTarget) =
        MoveToZoneEffect(target, Zone.Exile)
    fun ReturnToHand(target: EffectTarget) =
        MoveToZoneEffect(target, Zone.Hand)
    fun PutOnTopOfLibrary(target: EffectTarget) =
        MoveToZoneEffect(target, Zone.Library, ZonePlacement.Top)
    fun ShuffleIntoLibrary(target: EffectTarget) =
        MoveToZoneEffect(target, Zone.Library, ZonePlacement.Shuffled)
    fun PutOntoBattlefield(target: EffectTarget, tapped: Boolean = false) =
        MoveToZoneEffect(target, Zone.Battlefield,
            if (tapped) ZonePlacement.Tapped else ZonePlacement.Default)
}
```

### Card Examples (Target Pattern)

Every card reads as "target this, move it there":

```kotlin
// Gravedigger — ETB reanimate to hand
target = TargetObject(filter = TargetFilter.CreatureInYourGraveyard)
effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Hand)

// Breath of Life — reanimate to battlefield
target = TargetObject(filter = TargetFilter.CreatureInYourGraveyard)
effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Battlefield)

// Time Ebb — tuck creature
target = TargetObject(filter = TargetFilter.Creature)
effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Library, ZonePlacement.Top)

// Alabaster Dragon — death trigger shuffle self
trigger = Triggers.Dies
effect = MoveToZoneEffect(EffectTarget.Self, Zone.Library, ZonePlacement.Shuffled)

// Man-o'-War — ETB bounce creature
target = TargetObject(filter = TargetFilter.Creature)
effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Hand)
```

### Out of Scope

These effect types are **not** candidates for `MoveToZoneEffect` — they have complex semantics beyond simple zone movement:

- `DestroyAllEffect` — group destruction with filter + noRegenerate flag
- `ExileUntilLeavesEffect` — temporary exile with linked return trigger
- `ExileAndReplaceWithTokenEffect` — composite exile + token creation
- `SacrificeEffect` / `ForceSacrificeEffect` — sacrifice semantics (player choice)
- `SearchLibraryEffect` — multi-step search UI with reveal/shuffle options
- `DrawCardsEffect` — triggers "draw" events, not just zone movement
- `DiscardCardsEffect` — triggers "discard" events, player choice involved
