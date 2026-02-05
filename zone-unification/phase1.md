## Phase 1 — Bridge the Scripting Layer with a Typealias

**Files changed**: 1

<details>
<summary><code>scripting/events/Zone.kt</code> — replace enum with typealias</summary>

```kotlin
package com.wingedsheep.sdk.scripting

/**
 * Zone type for the scripting layer.
 *
 * This is a typealias to the canonical [com.wingedsheep.sdk.core.Zone].
 * All scripting code can continue to use `Zone.Battlefield`, `Zone.Graveyard`, etc.
 * unchanged — the typealias is transparent.
 *
 * Previously this was a separate enum with identical values.
 */
typealias Zone = com.wingedsheep.sdk.core.Zone
```

</details>

**What this achieves**: Every file in `com.wingedsheep.sdk.scripting` that currently uses `Zone` continues to work with **zero import changes**. The compiler resolves `Zone` to `com.wingedsheep.sdk.scripting.Zone` which is a typealias for `com.wingedsheep.sdk.core.Zone`. All enum values (`Battlefield`, `Graveyard`, etc.) are accessible through the typealias.

**Serialization**: kotlinx.serialization treats typealiases as the underlying type. Since enum constant names are identical (`Battlefield`, `Graveyard`, etc.), serialized JSON is unchanged.

**Verification**: Build the project. Every existing test should pass with no changes.
