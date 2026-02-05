## Phase 2 — Migrate `PermanentTargetFilter`

This is the largest migration. It touches `TargetPermanent`, `DestroyAllEffect`, the `Targets` DSL, and 13 card definitions.

**Files changed**: ~18

### Step 2a — Change `TargetPermanent` to use `TargetFilter`

<details>
<summary><code>TargetRequirement.kt</code></summary>

```kotlin
// Before:
data class TargetPermanent(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: PermanentTargetFilter = PermanentTargetFilter.Any
) : TargetRequirement { ... }

// After:
data class TargetPermanent(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: TargetFilter = TargetFilter.Permanent
) : TargetRequirement {
    override val description: String = buildString {
        if (optional) append("up to ")
        append("target ")
        if (filter != TargetFilter.Permanent) {
            append(filter.description)
        } else {
            append(if (count == 1) "permanent" else "$count permanents")
        }
    }
}
```

</details>

### Step 2b — Change `DestroyAllEffect` to use `GroupFilter`

<details>
<summary><code>RemovalEffects.kt</code></summary>

```kotlin
// Before:
data class DestroyAllEffect(
    val filter: PermanentTargetFilter = PermanentTargetFilter.Any,
    val noRegenerate: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Destroy all ")
        when (filter) {
            is PermanentTargetFilter.Any -> append("permanents")
            is PermanentTargetFilter.Creature -> append("creatures")
            // ... many branches
        }
        if (noRegenerate) append(". They can't be regenerated")
    }
}

// After:
data class DestroyAllEffect(
    val filter: GroupFilter = GroupFilter.AllPermanents,
    val noRegenerate: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Destroy ")
        append(filter.description)
        if (noRegenerate) append(". They can't be regenerated")
    }
}
```

The `when` branches disappear entirely — `GroupFilter.description` already produces "all creatures", "all lands", etc.

</details>

### Step 2c — Update `Targets` DSL facade

<details>
<summary><code>Targets.kt</code></summary>

```kotlin
// Before:
val Permanent: TargetRequirement = TargetPermanent()
val NonlandPermanent: TargetRequirement = TargetPermanent(filter = PermanentTargetFilter.NonLand)
val Artifact: TargetRequirement = TargetPermanent(filter = PermanentTargetFilter.Artifact)
val Enchantment: TargetRequirement = TargetPermanent(filter = PermanentTargetFilter.Enchantment)
val Land: TargetRequirement = TargetPermanent(filter = PermanentTargetFilter.Land)

// After:
val Permanent: TargetRequirement = TargetPermanent()
val NonlandPermanent: TargetRequirement = TargetPermanent(filter = TargetFilter.NonlandPermanent)
val Artifact: TargetRequirement = TargetPermanent(filter = TargetFilter.Artifact)
val Enchantment: TargetRequirement = TargetPermanent(filter = TargetFilter.Enchantment)
val Land: TargetRequirement = TargetPermanent(filter = TargetFilter.Land)
```

</details>

### Step 2d — Update card definitions

Here's the complete mapping for every affected card:

| Card | Before | After |
|------|--------|-------|
| **Armageddon** | `DestroyAllEffect(PermanentTargetFilter.Land)` | `DestroyAllEffect(GroupFilter.AllLands)` |
| **BoilingSeas** | `DestroyAllEffect(PTF.And(Land, WithSubtype(ISLAND)))` | `DestroyAllEffect(GroupFilter(GOF.Land.withSubtype(Subtype.ISLAND)))` |
| **Flashfires** | `DestroyAllEffect(PTF.And(Land, WithSubtype(PLAINS)))` | `DestroyAllEffect(GroupFilter(GOF.Land.withSubtype(Subtype.PLAINS)))` |
| **NaturesRuin** | `DestroyAllEffect(PTF.And(Creature, WithColor(GREEN)))` | `DestroyAllEffect(GroupFilter(GOF.Creature.withColor(Color.GREEN)))` |
| **VirtuesRuin** | `DestroyAllEffect(PTF.And(Creature, WithColor(WHITE)))` | `DestroyAllEffect(GroupFilter(GOF.Creature.withColor(Color.WHITE)))` |
| **WrathOfGod** | `DestroyAllEffect(PTF.Creature, noRegenerate = true)` | `DestroyAllEffect(GroupFilter.AllCreatures, noRegenerate = true)` |
| **Devastation** | `DestroyAllEffect(PTF.CreatureOrLand)` | `DestroyAllEffect(GroupFilter(GOF.CreatureOrLand))` |
| **WintersGrasp** | `TargetPermanent(filter = PTF.Land)` | `TargetPermanent(filter = TargetFilter.Land)` |
| **RainOfTears** | `TargetPermanent(filter = PTF.Land)` | `TargetPermanent(filter = TargetFilter.Land)` |
| **StoneRain** | `TargetPermanent(filter = PTF.Land)` | `TargetPermanent(filter = TargetFilter.Land)` |
| **RainOfSalt** | `TargetPermanent(count=2, filter = PTF.Land)` | `TargetPermanent(count=2, filter = TargetFilter.Land)` |
| **LavaFlow** | `TargetPermanent(filter = PTF.CreatureOrLand)` | `TargetPermanent(filter = TargetFilter.CreatureOrLandPermanent)` |
| **FireSnake** | `TargetPermanent(filter = PTF.Land)` | `TargetPermanent(filter = TargetFilter.Land)` |

Example — **NaturesRuin** before/after:

```kotlin
// Before:
spell {
    effect = DestroyAllEffect(
        PermanentTargetFilter.And(
            listOf(PermanentTargetFilter.Creature, PermanentTargetFilter.WithColor(Color.GREEN))
        )
    )
}

// After:
spell {
    effect = DestroyAllEffect(
        GroupFilter(GameObjectFilter.Creature.withColor(Color.GREEN))
    )
}
```

### Step 2e — Mark `PermanentTargetFilter` deprecated

Don't delete yet — other sets or engine code might reference it. Add:

```kotlin
@Deprecated(
    "Use TargetFilter for targeting or GroupFilter for mass effects",
    level = DeprecationLevel.WARNING
)
sealed interface PermanentTargetFilter { ... }
```
