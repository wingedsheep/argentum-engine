## Phase 1 — Prepare the Unified Types

Before migrating, we need to fill gaps in the unified types so every `PermanentTargetFilter` / `SpellTargetFilter` / `CreatureFilter` variant has a direct equivalent.

**Files changed**: 3

### Step 1a — Add missing `GameObjectFilter` pre-builts

<details>
<summary><code>ObjectFilter.kt</code></summary>

```kotlin
companion object {
    // ... existing ...

    // New: needed for Devastation, LavaFlow
    val CreatureOrLand = GameObjectFilter(
        cardPredicates = listOf(
            CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsLand))
        )
    )

    // New: needed for SpellTargetFilter.CreatureOrSorcery migration
    val CreatureOrSorcery = GameObjectFilter(
        cardPredicates = listOf(
            CardPredicate.Or(listOf(CardPredicate.IsCreature, CardPredicate.IsSorcery))
        )
    )

    // New: explicit noncreature permanent
    val NoncreaturePermanent = GameObjectFilter(
        cardPredicates = listOf(CardPredicate.IsPermanent, CardPredicate.IsNoncreature)
    )
}
```

</details>

### Step 1b — Add missing `TargetFilter` pre-builts

<details>
<summary><code>TargetFilter.kt</code></summary>

```kotlin
companion object {
    // ... existing ...

    // For TargetPermanent migration:
    val PermanentOpponentControls = TargetFilter(GameObjectFilter.Permanent.opponentControls())
    val CreatureOrLandPermanent = TargetFilter(GameObjectFilter.CreatureOrLand)
    val NoncreaturePermanent = TargetFilter(GameObjectFilter.NoncreaturePermanent)

    // For TargetSpell migration:
    val SorcerySpellOnStack = TargetFilter(GameObjectFilter.Sorcery, zone = Zone.Stack)
    val CreatureOrSorcerySpellOnStack = TargetFilter(GameObjectFilter.CreatureOrSorcery, zone = Zone.Stack)
    val InstantSpellOnStack = TargetFilter(GameObjectFilter.Instant, zone = Zone.Stack)
}
```

</details>

### Step 1c — Fix `GroupFilter` pluralization

<details>
<summary><code>GroupFilter.kt</code></summary>

The current logic skips pluralization when the description contains a space, which breaks "green creature" → "all green creature".

```kotlin
// Before:
if (baseFilter.description.endsWith("s").not() &&
    !baseFilter.description.contains(" ")) {
    append("s")
}

// After:
if (!baseFilter.description.endsWith("s")) {
    append("s")
}
```

This gives us:
- "creature" → "all creatures" ✓
- "green creature" → "all green creatures" ✓
- "lands" → "all lands" ✓ (no double-s)

</details>