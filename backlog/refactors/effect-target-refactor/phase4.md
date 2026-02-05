## Phase 4 — Migrate Player/Group `EffectTarget` Variants in Effect Defaults

Update effect type definitions to use the new composable variants for their defaults and description logic. This is the largest phase.

**Files changed**: ~10

### Step 4a — Default mapping table

| Effect | Old Default | New Default |
|--------|-------------|-------------|
| `DealDamageToPlayersEffect` | `EffectTarget.EachPlayer` | `EffectTarget.PlayerRef(Player.Each)` |
| `WheelEffect` | `EffectTarget.EachPlayer` | `EffectTarget.PlayerRef(Player.Each)` |
| `LoseLifeEffect` | `EffectTarget.Opponent` | `EffectTarget.PlayerRef(Player.TargetOpponent)` |
| `DiscardCardsEffect` | `EffectTarget.Opponent` | `EffectTarget.PlayerRef(Player.TargetOpponent)` |
| `DiscardRandomEffect` | `EffectTarget.Opponent` | `EffectTarget.PlayerRef(Player.TargetOpponent)` |
| `SkipCombatPhasesEffect` | `EffectTarget.AnyPlayer` | `EffectTarget.PlayerRef(Player.TargetPlayer)` |
| `SkipUntapEffect` | `EffectTarget.Opponent` | `EffectTarget.PlayerRef(Player.TargetOpponent)` |
| `TauntEffect` | `EffectTarget.AnyPlayer` | `EffectTarget.PlayerRef(Player.TargetPlayer)` |

Effects with `EffectTarget.Controller` default are **unchanged** — `Controller` is a self-reference we're keeping.

### Step 4b — Update `DealDamageToPlayersEffect`

<details>
<summary><code>mtg-sdk/.../scripting/effects/DamageEffects.kt</code></summary>

```kotlin
// Before:
data class DealDamageToPlayersEffect(
    val amount: DynamicAmount,
    val target: EffectTarget = EffectTarget.EachPlayer
) : Effect {
    constructor(amount: Int, target: EffectTarget = EffectTarget.EachPlayer) : this(...)

    override val description: String = when (target) {
        EffectTarget.EachPlayer -> "Deal ${amount.description} damage to each player"
        EffectTarget.Controller -> "Deal ${amount.description} damage to you"
        EffectTarget.Opponent -> "Deal ${amount.description} damage to target opponent"
        EffectTarget.EachOpponent -> "Deal ${amount.description} damage to each opponent"
        else -> "Deal ${amount.description} damage to ${target.description}"
    }
}

// After:
data class DealDamageToPlayersEffect(
    val amount: DynamicAmount,
    val target: EffectTarget = EffectTarget.PlayerRef(Player.Each)
) : Effect {
    constructor(amount: Int, target: EffectTarget = EffectTarget.PlayerRef(Player.Each)) : this(...)

    override val description: String = when (target) {
        EffectTarget.Controller -> "Deal ${amount.description} damage to you"
        else -> "Deal ${amount.description} damage to ${target.description}"
    }
}
```

The `when` block shrinks from 5 branches to 2. The `else` branch handles all composable variants automatically via `target.description`.

</details>

### Step 4c — Update `LoseLifeEffect`

<details>
<summary><code>mtg-sdk/.../scripting/effects/LifeEffects.kt</code></summary>

```kotlin
// Before:
data class LoseLifeEffect(
    val amount: Int,
    val target: EffectTarget = EffectTarget.Opponent
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "You lose $amount life"
        EffectTarget.Opponent -> "Target opponent loses $amount life"
        EffectTarget.AnyPlayer -> "Target player loses $amount life"
        else -> "Lose $amount life"
    }
}

// After:
data class LoseLifeEffect(
    val amount: Int,
    val target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "You lose $amount life"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} loses $amount life"
    }
}
```

</details>

### Step 4d — Apply same pattern to remaining effects

Each follows the same transformation: replace the old default, shrink the `when`:

| File | Effects Updated |
|------|----------------|
| `LifeEffects.kt` | `GainLifeEffect`, `LoseLifeEffect`, `LoseHalfLifeEffect` |
| `DrawingEffects.kt` | `DiscardCardsEffect`, `DiscardRandomEffect`, `WheelEffect` |
| `DamageEffects.kt` | `DealDamageToPlayersEffect` |
| `LibraryEffects.kt` | `MillEffect`, `ShuffleLibraryEffect` |
| `PlayerEffects.kt` | `SkipCombatPhasesEffect`, `SkipUntapEffect` |
| `CombatEffects.kt` | `TauntEffect` |

The pattern is identical in every case:

```kotlin
// 1. Replace old default
target: EffectTarget = EffectTarget.Opponent
// becomes:
target: EffectTarget = EffectTarget.PlayerRef(Player.TargetOpponent)

// 2. Collapse when block
when (target) {
    EffectTarget.Controller -> "You ..."      // keep special case for subject "You"
    else -> "${target.description} ..."         // handles all composable variants
}
```

### Step 4e — Update `WindsOfChange`

The one Portal card that directly uses `EachPlayer`:

<details>
<summary><code>mtg-sets/.../portal/cards/WindsOfChange.kt</code></summary>

```kotlin
// Before:
effect = WheelEffect(target = EffectTarget.EachPlayer)

// After:
effect = WheelEffect(target = EffectTarget.PlayerRef(Player.Each))
```

</details>

### Step 4f — Cards relying on updated defaults (no changes needed)

Cards that rely on the old defaults via convenience constructors need no card-level changes — the default is updated at the effect level:

```kotlin
// These call DealDamageToPlayersEffect(amount) which used EachPlayer default
DrySpell:        DealDamageToPlayersEffect(1)       // ← no change, default updated
FireTempest:     DealDamageToPlayersEffect(6)       // ← no change, default updated
Earthquake:      DealDamageToPlayersEffect(XValue)  // ← no change, default updated
Hurricane:       DealDamageToPlayersEffect(XValue)  // ← no change, default updated
```
