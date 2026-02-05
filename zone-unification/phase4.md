## Phase 4 — Audit `SearchDestination` (No Migration, Document Relationship)

`SearchDestination` stays separate because it represents a **destination with movement semantics**, not a zone identity:

| `SearchDestination` | Corresponding `Zone` | Why it's different |
|---|---|---|
| `HAND` | `Zone.Hand` | Includes preposition: "**into** your hand" |
| `BATTLEFIELD` | `Zone.Battlefield` | Includes preposition: "**onto** the battlefield" |
| `GRAVEYARD` | `Zone.Graveyard` | Includes preposition: "**into** your graveyard" |
| `TOP_OF_LIBRARY` | — | **Not a zone** — it's a position within `Zone.Library` |

**Action**: Add documentation linking the two concepts:

<details>
<summary><code>scripting/effects/LibraryEffects.kt</code> — documentation only</summary>

```kotlin
/**
 * Destination for cards moved by search/return effects.
 *
 * This is intentionally separate from [Zone] because destinations carry
 * movement semantics (prepositions) and include non-zone positions like
 * [TOP_OF_LIBRARY]. Use [Zone] when identifying which game zone an object
 * is in; use [SearchDestination] when specifying where an effect puts cards.
 *
 * @see Zone for game zone identification
 */
@Serializable
enum class SearchDestination(val description: String) {
    HAND("into your hand"),
    BATTLEFIELD("onto the battlefield"),
    GRAVEYARD("into your graveyard"),
    TOP_OF_LIBRARY("on top of your library")
}
```

</details>

**Verification**: No functional changes — docs only.
