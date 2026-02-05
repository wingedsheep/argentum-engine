# EffectTarget Refactor — Implementation Plan

## Goals

1. **Add composable `EffectTarget` variants** (`PlayerRef`, `GroupRef`, `FilteredTarget`) that leverage the unified filter system
2. **Migrate card definitions** from hardcoded `EffectTarget` variants to `ContextTarget(0)` bindings
3. **Fix `ReturnFromGraveyardEffect`** missing explicit target reference (+ card definition bugs)
4. **Simplify effect `description` logic** — eliminate growing `when` blocks in effect types
5. **Bridge `PlayerFilter` → `Player`** — remove the duplicate type
6. **Bundle four quick wins** identified in the analysis (thread safety, dummy defaults, dead field, CantBlock consistency)

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

**Total**: ~28 files

---

## Engine Impact

| SDK Change | Engine Must Update |
|---|---|
| `ReturnFromGraveyardEffect` gains `target` field | Effect resolver reads `target` instead of inferring from context |
| New `EffectTarget.PlayerRef` | Target resolver matches `PlayerRef.player` to resolve player reference |
| New `EffectTarget.GroupRef` | Effect resolver iterates `GroupRef.filter` (same path as existing `GroupFilter` effects) |
| New `EffectTarget.FilteredTarget` | Target resolver evaluates `filter.baseFilter` against game state |
| `GameEvent` uses `Player` instead of `PlayerFilter` | Replacement effect matcher evaluates `Player` (superset of `PlayerFilter`) |

The `PlayerRef` and `GroupRef` resolvers are extensions of code paths that already handle `Player` and `GroupFilter` in other contexts (e.g., `DynamicAmount.Count`, `ModifyStatsForGroupEffect`). No fundamentally new evaluation logic is needed.

---

## Recommended Execution Order

```
Phase 0  ←  ship immediately (independent quick wins)
  ↓
Phase 1  ←  ship next (additive, enables all later phases)
  ↓
Phase 2  ←  ReturnFromGraveyardEffect fix (correctness)
  ↓
Phase 3  ←  card migrations (mechanical, small)
  ↓
Phase 4  ←  effect defaults + descriptions (largest logic change)
  ↓
Phase 5  ←  DSL update (small follow-up)
  ↓
Phase 6  ←  PlayerFilter bridge (independent, can parallelize with 3-5)
  ↓
Phase 7  ←  deprecation annotations
  ↓
Phase 8  ←  deletion (after all sets + engine verified)
```

Phases 3, 5, and 6 can run in parallel — they touch non-overlapping files. Phase 4 is the critical path.

---

## Future Roadmap: Unified `MoveToZoneEffect`

After the EffectTarget refactor stabilizes, the natural next step is to unify all zone-moving effects into a single `MoveToZoneEffect(target, destination, placement)`. This collapses 6+ effect types into one:

| Before | After |
|--------|-------|
| `ReturnFromGraveyardEffect(target, HAND)` | `MoveToZoneEffect(target, Zone.Hand)` |
| `ReturnFromGraveyardEffect(target, BATTLEFIELD)` | `MoveToZoneEffect(target, Zone.Battlefield)` |
| `ReturnToHandEffect(target)` | `MoveToZoneEffect(target, Zone.Hand)` |
| `ExileEffect(target)` | `MoveToZoneEffect(target, Zone.Exile)` |
| `ShuffleIntoLibraryEffect(target)` | `MoveToZoneEffect(target, Zone.Library, ZonePlacement.Shuffled)` |
| `PutOnTopOfLibraryEffect(target)` | `MoveToZoneEffect(target, Zone.Library, ZonePlacement.Top)` |
| `DestroyEffect(target)` | `MoveToZoneEffect(target, Zone.Graveyard, byDestruction = true)` |

The effect definition:

```kotlin
data class MoveToZoneEffect(
    val target: EffectTarget,
    val destination: Zone,
    val placement: ZonePlacement = ZonePlacement.Default,
    val byDestruction: Boolean = false
) : Effect

enum class ZonePlacement { Default, Top, Bottom, Shuffled, Tapped }
```

Old effects become DSL convenience functions:

```kotlin
object Effects {
    fun Destroy(target: EffectTarget) = MoveToZoneEffect(target, Zone.Graveyard, byDestruction = true)
    fun Exile(target: EffectTarget) = MoveToZoneEffect(target, Zone.Exile)
    fun ReturnToHand(target: EffectTarget) = MoveToZoneEffect(target, Zone.Hand)
    // etc.
}
```

Combined with `TargetObject(filter = TargetFilter.CreatureInYourGraveyard)`, every card reads as a simple statement: "target this, move it there." The what-to-target is in the `TargetFilter`, the where-to-put-it is in the `Zone`, and the binding between them is `ContextTarget(0)`.

This is tracked separately and will be planned after Phase 8 completes.
