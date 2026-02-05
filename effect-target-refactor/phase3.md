## Phase 3 — Migrate Hardcoded Filtered Targets → `ContextTarget(0)` in Cards

These cards use `EffectTarget.TargetCreature` or `EffectTarget.AnyPlayer` when they already have an explicit target declaration that should be bound via `ContextTarget(0)`.

**Files changed**: 4

| Card | Current | After |
|------|---------|-------|
| **AngelicBlessing** | `EffectTarget.TargetCreature` (×2) | `EffectTarget.ContextTarget(0)` (×2) |
| **DefiantStand** | `EffectTarget.TargetCreature` (×2) | `EffectTarget.ContextTarget(0)` (×2) |
| **SternMarshal** | `EffectTarget.TargetCreature` | `EffectTarget.ContextTarget(0)` |
| **FalsePeace** | `EffectTarget.AnyPlayer` | `EffectTarget.ContextTarget(0)` |

<details>
<summary><code>AngelicBlessing.kt</code></summary>

```kotlin
// Before:
spell {
    target = Targets.Creature
    effect = Effects.Composite(
        Effects.ModifyStats(3, 3, EffectTarget.TargetCreature),
        Effects.GrantKeyword(Keyword.FLYING, EffectTarget.TargetCreature)
    )
}

// After:
spell {
    target = Targets.Creature
    effect = Effects.Composite(
        Effects.ModifyStats(3, 3, EffectTarget.ContextTarget(0)),
        Effects.GrantKeyword(Keyword.FLYING, EffectTarget.ContextTarget(0))
    )
}
```

</details>

<details>
<summary><code>DefiantStand.kt</code></summary>

```kotlin
// Before:
spell {
    target = Targets.Creature
    effect = Effects.Composite(
        Effects.ModifyStats(1, 3, EffectTarget.TargetCreature),
        Effects.Untap(EffectTarget.TargetCreature)
    )
}

// After:
spell {
    target = Targets.Creature
    effect = Effects.Composite(
        Effects.ModifyStats(1, 3, EffectTarget.ContextTarget(0)),
        Effects.Untap(EffectTarget.ContextTarget(0))
    )
}
```

</details>

<details>
<summary><code>SternMarshal.kt</code></summary>

```kotlin
// Before:
activatedAbility {
    // ...
    target = Targets.Creature
    effect = ModifyStatsEffect(2, 2, target = EffectTarget.TargetCreature)
}

// After:
activatedAbility {
    // ...
    target = Targets.Creature
    effect = ModifyStatsEffect(2, 2, target = EffectTarget.ContextTarget(0))
}
```

</details>

<details>
<summary><code>FalsePeace.kt</code></summary>

```kotlin
// Before:
spell {
    target = Targets.Player
    effect = SkipCombatPhasesEffect(EffectTarget.AnyPlayer)
}

// After:
spell {
    target = Targets.Player
    effect = SkipCombatPhasesEffect(EffectTarget.ContextTarget(0))
}
```

</details>

**Verification**: These are mechanical substitutions. The engine already resolves `ContextTarget(0)` to the same entity that `TargetCreature`/`AnyPlayer` would resolve to, since the spell has an explicit `target` requirement. Tests pass unchanged.
