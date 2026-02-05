## Phase 0 — Bundled Quick Wins

Four isolated fixes from the analysis that are trivial and independent.

**Files changed**: 5

### Step 0a — `AbilityId.generate()` thread safety

<details>
<summary><code>mtg-sdk/.../scripting/AbilityId.kt</code></summary>

```kotlin
// Before (line 12):
companion object {
    private var counter = 0L
    fun generate(): AbilityId = AbilityId("ability_${++counter}")
}

// After:
companion object {
    private val counter = java.util.concurrent.atomic.AtomicLong(0)
    fun generate(): AbilityId = AbilityId("ability_${counter.incrementAndGet()}")
}
```

</details>

### Step 0b — Remove `selectedCardIds` from `SearchLibraryEffect`

<details>
<summary><code>mtg-sdk/.../scripting/effects/LibraryEffects.kt</code></summary>

```kotlin
// Remove this field entirely (line 159):
data class SearchLibraryEffect(
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val count: Int = 1,
    val destination: SearchDestination = SearchDestination.HAND,
    val entersTapped: Boolean = false,
    val shuffleAfter: Boolean = true,
    val reveal: Boolean = false
    // REMOVED: val selectedCardIds: List<EntityId>? = null
)
```

Resolution state belongs in the engine's action layer, not the effect definition.

</details>

### Step 0c — Builder dummy defaults → fail-fast

<details>
<summary><code>mtg-sdk/.../dsl/CardBuilder.kt</code></summary>

All four builders currently use `DrawCardsEffect(0, EffectTarget.Controller)` as a dummy default. Replace with nullable + validation:

```kotlin
// ModeBuilder (line 543):
// Before:
var effect: Effect = DrawCardsEffect(0, EffectTarget.Controller)
// After:
var effect: Effect? = null
fun build(): Mode {
    requireNotNull(effect) { "Mode '$description' must have an effect" }
    // ...
}

// TriggeredAbilityBuilder (line 573):
// Before:
var effect: Effect = DrawCardsEffect(0, EffectTarget.Controller)
// After:
var effect: Effect? = null
fun build(): TriggeredAbility {
    requireNotNull(effect) { "Triggered ability must have an effect" }
    // ...
}

// ActivatedAbilityBuilder (line 592):
// Before:
var effect: Effect = DrawCardsEffect(0, EffectTarget.Controller)
// After:
var effect: Effect? = null
fun build(): ActivatedAbility {
    requireNotNull(effect) { "Activated ability must have an effect" }
    // ...
}

// LoyaltyAbilityBuilder (line 685):
// Before:
var effect: Effect = DrawCardsEffect(0, EffectTarget.Controller)
// After:
var effect: Effect? = null
fun build(): ActivatedAbility {
    requireNotNull(effect) { "Loyalty ability must have an effect" }
    // ...
}
```

</details>

### Step 0d — Consistent `CantBlock()` defaults

<details>
<summary>4 card files</summary>

Standardize all to use the default (no explicit argument). `CantBlock()` already defaults to `StaticTarget.SourceCreature`.

| Card | Before | After |
|------|--------|-------|
| JungleLion | `CantBlock(StaticTarget.SourceCreature)` | `CantBlock()` |
| CravenGiant | `CantBlock(StaticTarget.SourceCreature)` | `CantBlock()` |
| HulkingCyclops | `CantBlock(StaticTarget.SourceCreature)` | `CantBlock()` |
| HulkingGoblin | `CantBlock(StaticTarget.SourceCreature)` | `CantBlock()` |
| CravenKnight | `CantBlock()` | `CantBlock()` (already correct) |

</details>

**Verification**: All existing tests pass. No behavioral change — only correctness and consistency improvements.
