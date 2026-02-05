## Phase 4 — Migrate `CreatureFilter`

**Files changed**: ~2

### Step 4a — Change `GlobalEffect` to use `GroupFilter`

<details>
<summary><code>StaticAbility.kt</code></summary>

```kotlin
// Before:
data class GlobalEffect(
    val effectType: GlobalEffectType,
    val filter: CreatureFilter = CreatureFilter.All
) : StaticAbility { ... }

// After:
data class GlobalEffect(
    val effectType: GlobalEffectType,
    val filter: GroupFilter = GroupFilter.AllCreatures
) : StaticAbility { ... }
```

</details>

### Step 4b — Update `Filters` DSL facade

<details>
<summary><code>Filters.kt</code></summary>

```kotlin
// Before (Creature Filters section):
val AllCreatures: CreatureFilter = CreatureFilter.All
val CreaturesYouControl: CreatureFilter = CreatureFilter.YouControl
val CreaturesOpponentsControl: CreatureFilter = CreatureFilter.OpponentsControl
fun CreaturesWithKeyword(kw: Keyword): CreatureFilter = CreatureFilter.WithKeyword(kw)
fun CreaturesWithoutKeyword(kw: Keyword): CreatureFilter = CreatureFilter.WithoutKeyword(kw)

// After:
val AllCreatures: GroupFilter = GroupFilter.AllCreatures
val CreaturesYouControl: GroupFilter = GroupFilter.AllCreaturesYouControl
val CreaturesOpponentsControl: GroupFilter = GroupFilter.AllCreaturesOpponentsControl
fun CreaturesWithKeyword(kw: Keyword): GroupFilter =
    GroupFilter.AllCreatures.withKeyword(kw)
fun CreaturesWithoutKeyword(kw: Keyword): GroupFilter =
    GroupFilter.AllCreatures.withoutKeyword(kw)
```

</details>

### Step 4c — Update card definitions

Scan all sets for `GlobalEffect` usage. No Portal cards use it directly (they use `ModifyStatsForGroupEffect`, `GrantKeywordToGroupEffect`, etc., which already take `GroupFilter`). Any other sets using `GlobalEffect` would need the same migration.

### Step 4d — Mark `CreatureFilter` deprecated

```kotlin
@Deprecated(
    "Use GroupFilter instead",
    level = DeprecationLevel.WARNING
)
sealed interface CreatureFilter { ... }
```
