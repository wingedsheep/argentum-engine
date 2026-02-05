## Phase 3 — Migrate `CostZone` to `Zone`

**Files changed**: 1

<details>
<summary><code>scripting/AdditionalCost.kt</code></summary>

```kotlin
// Before:
data class ExileCards(
    val count: Int = 1,
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val fromZone: CostZone = CostZone.GRAVEYARD
) : AdditionalCost {
    override val description: String = buildString {
        // ...
        append(" from your ${fromZone.description}")
    }
}

@Serializable
enum class CostZone(val description: String) {
    HAND("hand"),
    GRAVEYARD("graveyard"),
    LIBRARY("library"),
    BATTLEFIELD("battlefield")
}

// After:
data class ExileCards(
    val count: Int = 1,
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val fromZone: Zone = Zone.Graveyard
) : AdditionalCost {
    override val description: String = buildString {
        // ...
        append(" from your ${fromZone.simpleName}")  // <- uses simpleName
    }
}

@Deprecated(
    message = "Use Zone instead.",
    replaceWith = ReplaceWith("Zone", "com.wingedsheep.sdk.core.Zone"),
    level = DeprecationLevel.WARNING
)
@Serializable
enum class CostZone(val description: String) {
    HAND("hand"),
    GRAVEYARD("graveyard"),
    LIBRARY("library"),
    BATTLEFIELD("battlefield");

    fun toZone(): Zone = when (this) {
        HAND -> Zone.Hand
        GRAVEYARD -> Zone.Graveyard
        LIBRARY -> Zone.Library
        BATTLEFIELD -> Zone.Battlefield
    }
}
```

</details>

**Why `simpleName`**: `CostZone.GRAVEYARD.description` was `"graveyard"` — no article. The `ExileCards` description reads `"from your graveyard"`. Using `Zone.Graveyard.description` ("a graveyard") would produce `"from your a graveyard"`. The `simpleName` property added in Phase 0 provides the article-free form.

**Verification**: Build the project. Any card using `ExileCards` with `CostZone` values needs updating to `Zone` values.
