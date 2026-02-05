## Phase 3 — Migrate `SpellTargetFilter`

**Files changed**: ~3

### Step 3a — Change `TargetSpell` to use `TargetFilter`

<details>
<summary><code>TargetRequirement.kt</code></summary>

```kotlin
// Before:
data class TargetSpell(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: SpellTargetFilter = SpellTargetFilter.Any
) : TargetRequirement { ... }

// After:
data class TargetSpell(
    override val count: Int = 1,
    override val optional: Boolean = false,
    val filter: TargetFilter = TargetFilter.SpellOnStack
) : TargetRequirement {
    override val description: String = buildString {
        append("target ")
        if (filter != TargetFilter.SpellOnStack) {
            append(filter.baseFilter.description)
            append(" ")
        }
        append("spell")
    }
}
```

</details>

### Step 3b — Update `Targets` DSL facade

<details>
<summary><code>Targets.kt</code></summary>

```kotlin
// Before:
val Spell: TargetRequirement = TargetSpell()
val CreatureSpell: TargetRequirement = TargetSpell(filter = SpellTargetFilter.Creature)
val NoncreatureSpell: TargetRequirement = TargetSpell(filter = SpellTargetFilter.Noncreature)
val CreatureOrSorcerySpell: TargetRequirement = TargetSpell(filter = SpellTargetFilter.CreatureOrSorcery())
fun SpellWithManaValueAtMost(mv: Int): TargetRequirement =
    TargetSpell(filter = SpellTargetFilter.WithManaValueAtMost(mv))

// After:
val Spell: TargetRequirement = TargetSpell()
val CreatureSpell: TargetRequirement = TargetSpell(filter = TargetFilter.CreatureSpellOnStack)
val NoncreatureSpell: TargetRequirement = TargetSpell(filter = TargetFilter.NoncreatureSpellOnStack)
val CreatureOrSorcerySpell: TargetRequirement = TargetSpell(filter = TargetFilter.CreatureOrSorcerySpellOnStack)
fun SpellWithManaValueAtMost(mv: Int): TargetRequirement =
    TargetSpell(filter = TargetFilter(GameObjectFilter.Any.manaValueAtMost(mv), zone = Zone.Stack))
```

</details>

### Step 3c — Update card definitions

Only **MysticDenial** in Portal uses `Targets.CreatureOrSorcerySpell`, which is handled by the DSL change above. No card file changes needed.

### Step 3d — Mark `SpellTargetFilter` deprecated

```kotlin
@Deprecated(
    "Use TargetFilter with zone = Zone.Stack instead",
    level = DeprecationLevel.WARNING
)
sealed interface SpellTargetFilter { ... }
```