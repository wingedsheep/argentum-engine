## Phase 0 — Fix `SpellBuilder.condition` Bug

**Problem**: `SpellBuilder.condition` is declared but never wired into the built `CardScript`. Gift of Estates always searches regardless of land count.

**Files changed**: 1

<details>
<summary><code>CardBuilder.kt</code> — wire condition into effect</summary>

```kotlin
// In CardBuilder.build(), change:
val script = CardScript(
    spellEffect = spellBuilder?.effect,
    // ...
)

// To:
val rawEffect = spellBuilder?.effect
val conditionWrappedEffect = if (spellBuilder?.condition != null && rawEffect != null) {
    ConditionalEffect(spellBuilder!!.condition!!, rawEffect)
} else {
    rawEffect
}
val script = CardScript(
    spellEffect = conditionWrappedEffect,
    // ...
)
```

</details>

**Verification**: Gift of Estates should now only search when an opponent controls more lands.