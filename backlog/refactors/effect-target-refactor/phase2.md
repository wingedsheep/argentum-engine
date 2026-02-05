## Phase 2 — Fix `ReturnFromGraveyardEffect` Target Gap

`ReturnFromGraveyardEffect` currently takes a `filter: GameObjectFilter` instead of a target parameter, forcing implicit target resolution in the engine. This also exposes bugs in card definitions that omit target declarations entirely.

**Files changed**: 7

### Step 2a — Add `target` parameter to `ReturnFromGraveyardEffect`

<details>
<summary><code>mtg-sdk/.../scripting/effects/RemovalEffects.kt</code> (line 214)</summary>

```kotlin
// Before:
data class ReturnFromGraveyardEffect(
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val destination: SearchDestination = SearchDestination.HAND
) : Effect {
    override val description: String
        get() = "Return ${filter.description} from your graveyard ${destination.description}"
}

// After:
data class ReturnFromGraveyardEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val destination: SearchDestination = SearchDestination.HAND
) : Effect {
    override val description: String
        get() = "Return ${target.description} from your graveyard ${destination.description}"
}
```

The `filter` is removed because the filtering is already handled by the `TargetRequirement` declared on the spell/ability. The effect just needs to know *which* target to return.

</details>

### Step 2b — Fix card definitions that were missing target declarations

These cards use `ReturnFromGraveyardEffect` without declaring a `TargetCardInGraveyard` requirement. The engine was implicitly resolving this, masking a correctness issue.

<details>
<summary><code>ElvenCache.kt</code> — add missing target requirement</summary>

```kotlin
// Before:
spell {
    effect = ReturnFromGraveyardEffect(
        filter = GameObjectFilter.Any,
        destination = SearchDestination.HAND
    )
}

// After:
spell {
    target = TargetCardInGraveyard(
        filter = TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.Graveyard)
    )
    effect = ReturnFromGraveyardEffect(
        target = EffectTarget.ContextTarget(0),
        destination = SearchDestination.HAND
    )
}
```

</details>

<details>
<summary><code>DejaVu.kt</code> — add missing target requirement</summary>

```kotlin
// Before:
spell {
    effect = ReturnFromGraveyardEffect(GameObjectFilter.Sorcery)
}

// After:
spell {
    target = TargetCardInGraveyard(
        filter = TargetFilter(GameObjectFilter.Sorcery.ownedByYou(), zone = Zone.Graveyard)
    )
    effect = ReturnFromGraveyardEffect(
        target = EffectTarget.ContextTarget(0),
        destination = SearchDestination.HAND
    )
}
```

</details>

### Step 2c — Update cards that already declare targets

These cards already have correct `TargetCardInGraveyard` requirements — they just need the effect signature updated from `filter` to `target`.

| Card | Change |
|------|--------|
| **Gravedigger** | `ReturnFromGraveyardEffect(filter, destination)` → `ReturnFromGraveyardEffect(EffectTarget.ContextTarget(0), destination)` |
| **RaiseDead** | Same pattern |
| **BreathOfLife** | Same pattern, `destination = SearchDestination.BATTLEFIELD` |

<details>
<summary>Example — <code>Gravedigger.kt</code></summary>

```kotlin
// Before:
triggeredAbility {
    trigger = OnEnterBattlefield()
    optional = true
    target = TargetCardInGraveyard(filter = TargetFilter.CreatureInYourGraveyard)
    effect = ReturnFromGraveyardEffect(
        filter = GameObjectFilter.Creature,
        destination = SearchDestination.HAND
    )
}

// After:
triggeredAbility {
    trigger = OnEnterBattlefield()
    optional = true
    target = TargetCardInGraveyard(filter = TargetFilter.CreatureInYourGraveyard)
    effect = ReturnFromGraveyardEffect(
        target = EffectTarget.ContextTarget(0),
        destination = SearchDestination.HAND
    )
}
```

</details>

### Step 2d — Update engine effect resolver

The engine's `ReturnFromGraveyardEffect` executor currently uses `effect.filter` to find targets. It must switch to resolving `effect.target` via the standard target resolution path (same as other effects using `ContextTarget`).
