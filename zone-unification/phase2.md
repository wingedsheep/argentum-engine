## Phase 2 — Migrate `ZoneType` to `Zone`

**Files changed**: 2

### Step 2a — Update `ActivatedAbility`

This is the only SDK usage of `ZoneType`:

<details>
<summary><code>scripting/ActivatedAbility.kt</code></summary>

```kotlin
// Before:
import com.wingedsheep.sdk.core.ZoneType

data class ActivatedAbility(
    // ...
    val activateFromZone: ZoneType = ZoneType.BATTLEFIELD
)

// After:
import com.wingedsheep.sdk.core.Zone

data class ActivatedAbility(
    // ...
    val activateFromZone: Zone = Zone.Battlefield
)
```

</details>

### Step 2b — Deprecate `ZoneType`

<details>
<summary><code>core/ZoneType.kt</code></summary>

```kotlin
package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

/**
 * @deprecated Use [Zone] instead. This type will be removed in a future version.
 */
@Deprecated(
    message = "Use Zone instead. ZoneType and Zone have been unified into a single type.",
    replaceWith = ReplaceWith("Zone", "com.wingedsheep.sdk.core.Zone"),
    level = DeprecationLevel.WARNING
)
@Serializable
enum class ZoneType {
    LIBRARY,
    HAND,
    BATTLEFIELD,
    GRAVEYARD,
    STACK,
    EXILE,
    COMMAND;

    val isPublic: Boolean
        get() = this in listOf(BATTLEFIELD, GRAVEYARD, STACK, EXILE, COMMAND)
    val isHidden: Boolean
        get() = this in listOf(LIBRARY, HAND)
    val isShared: Boolean
        get() = this in listOf(BATTLEFIELD, STACK, EXILE)

    /** Convert to the canonical Zone type. */
    fun toZone(): Zone = when (this) {
        LIBRARY -> Zone.Library
        HAND -> Zone.Hand
        BATTLEFIELD -> Zone.Battlefield
        GRAVEYARD -> Zone.Graveyard
        STACK -> Zone.Stack
        EXILE -> Zone.Exile
        COMMAND -> Zone.Command
    }
}
```

</details>

The `toZone()` method gives the engine team a migration bridge — they can call it at boundaries while they migrate their own code.

**Verification**: Build the project. The only compile change is in `ActivatedAbility`. Engine code using `ZoneType` still compiles but shows deprecation warnings.
