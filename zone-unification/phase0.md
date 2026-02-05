## Phase 0 — Create Canonical `Zone` in `core/`

**Problem**: Four overlapping zone types (`Zone`, `ZoneType`, `CostZone`, `SearchDestination`) force card authors to choose between identical concepts with different names.

**Files changed**: 1 new

<details>
<summary><code>core/Zone.kt</code> — new file</summary>

```kotlin
package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

/**
 * The canonical zone type for all game zones in Magic: The Gathering.
 *
 * Combines the description text (for card text generation) with
 * classification properties (for engine zone management).
 *
 * @property description Human-readable description for card text (e.g., "the battlefield")
 * @property simpleName Short name without articles (e.g., "battlefield", "graveyard")
 */
@Serializable
enum class Zone(val description: String, val simpleName: String) {
    Battlefield("the battlefield", "battlefield"),
    Graveyard("a graveyard", "graveyard"),
    Hand("a hand", "hand"),
    Library("a library", "library"),
    Exile("exile", "exile"),
    Stack("the stack", "stack"),
    Command("the command zone", "command zone");

    /** Zone contents are visible to all players */
    val isPublic: Boolean
        get() = this in PUBLIC_ZONES

    /** Zone contents are hidden (only visible to owner) */
    val isHidden: Boolean
        get() = this in HIDDEN_ZONES

    /** Zone is shared (not player-specific in display) */
    val isShared: Boolean
        get() = this in SHARED_ZONES

    companion object {
        private val PUBLIC_ZONES = setOf(Battlefield, Graveyard, Stack, Exile, Command)
        private val HIDDEN_ZONES = setOf(Library, Hand)
        private val SHARED_ZONES = setOf(Battlefield, Stack, Exile)
    }
}
```

</details>

**Key design decisions**:
- `description` matches existing `Zone` enum values ("the battlefield", "a graveyard")
- `simpleName` covers what `CostZone.description` provided ("hand", "graveyard") — no articles, suitable for "from your graveyard"
- `isPublic`/`isHidden`/`isShared` absorbed from `ZoneType`
- PascalCase constants match the scripting layer's serialization convention

**Verification**: Project compiles. No existing code affected — this is purely additive.
