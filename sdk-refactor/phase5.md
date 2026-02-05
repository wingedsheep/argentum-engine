## Phase 5 — Cleanup & Verification

### Step 5a — Remove deprecated types

After confirming no remaining references:
- Delete `PermanentTargetFilter` sealed interface and all variants
- Delete `SpellTargetFilter` sealed interface and all variants
- Delete `CreatureFilter` sealed interface and all variants
- Remove the empty `references/Zone.kt` comment file (the canonical `Zone` lives in `events/Zone.kt`)

### Step 5b — Update `Filters` DSL documentation

Remove the now-redundant `Filters.AllCreatures`, `Filters.CreaturesYouControl` etc. from the top-level section and point users to `Filters.Group.*`:

```kotlin
object Filters {
    // BEFORE: Scattered between top-level and Group namespace
    // AFTER: All group/mass filters live under Group

    @Deprecated("Use Filters.Group.allCreatures", ReplaceWith("Group.allCreatures"))
    val AllCreatures = Group.allCreatures
    // ...
}
```

### Step 5c — Add `GroupFilter` convenience for `DestroyAllEffect`

Since "Destroy all X" is common, add convenience companions:

```kotlin
companion object {
    // Existing...
    
    // New convenience for common destroy-all patterns:
    val AllCreaturesOfColor(color: Color) =
        GroupFilter(GameObjectFilter.Creature.withColor(color))
    val AllLandsWithSubtype(subtype: Subtype) =
        GroupFilter(GameObjectFilter.Land.withSubtype(subtype))
}
```