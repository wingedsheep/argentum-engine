## Phase 6 — Bridge `PlayerFilter` → `Player`

`PlayerFilter` in `EventFilters.kt` duplicates a subset of `Player` in `references/Player.kt`. Since `PlayerFilter` is used in `GameEvent` types (which are serializable data), this is a type signature change.

**Files changed**: 2

### Step 6a — Add `@Deprecated` with `ReplaceWith` to `PlayerFilter`

<details>
<summary><code>mtg-sdk/.../scripting/events/EventFilters.kt</code> (line 270)</summary>

```kotlin
@Deprecated("Use Player instead", level = DeprecationLevel.WARNING)
@Serializable
sealed interface PlayerFilter {
    val description: String

    @Deprecated("Use Player.You", ReplaceWith("Player.You", "com.wingedsheep.sdk.scripting.references.Player"))
    @Serializable
    data object You : PlayerFilter { override val description = "you" }

    @Deprecated("Use Player.Opponent", ReplaceWith("Player.Opponent", "com.wingedsheep.sdk.scripting.references.Player"))
    @Serializable
    data object Opponent : PlayerFilter { override val description = "an opponent" }

    @Deprecated("Use Player.Any", ReplaceWith("Player.Any", "com.wingedsheep.sdk.scripting.references.Player"))
    @Serializable
    data object Any : PlayerFilter { override val description = "a player" }

    @Deprecated("Use Player.Each", ReplaceWith("Player.Each", "com.wingedsheep.sdk.scripting.references.Player"))
    @Serializable
    data object Each : PlayerFilter { override val description = "each player" }
}
```

</details>

### Step 6b — Change `GameEvent` types to use `Player`

<details>
<summary><code>mtg-sdk/.../scripting/events/GameEvent.kt</code></summary>

```kotlin
// Before:
data class DrawEvent(val player: PlayerFilter = PlayerFilter.You) : GameEvent
data class LifeGainEvent(val player: PlayerFilter = PlayerFilter.You) : GameEvent
data class LifeLossEvent(val player: PlayerFilter = PlayerFilter.You) : GameEvent
data class DiscardEvent(
    val player: PlayerFilter = PlayerFilter.You,
    val cardFilter: GameObjectFilter? = null
) : GameEvent
data class SearchLibraryEvent(val player: PlayerFilter = PlayerFilter.You) : GameEvent

// After:
data class DrawEvent(val player: Player = Player.You) : GameEvent
data class LifeGainEvent(val player: Player = Player.You) : GameEvent
data class LifeLossEvent(val player: Player = Player.You) : GameEvent
data class DiscardEvent(
    val player: Player = Player.You,
    val cardFilter: GameObjectFilter? = null
) : GameEvent
data class SearchLibraryEvent(val player: Player = Player.You) : GameEvent
```

</details>

### Step 6c — Verify `ReplacementEffect` usages

Scan `ReplacementEffect.kt` for `PlayerFilter` references in defaults. The replacement effects that reference these events (e.g., `PreventDraw`, `PreventLifeGain`, `ReplaceLifeGain`, `ModifyLifeGain`, `ReplaceDrawWithEffect`) all use the event defaults, which are now `Player.You`. No changes needed in these files — they just construct events with the default player.

### Step 6d — Update engine

The engine's replacement effect matcher currently pattern-matches on `PlayerFilter` variants to determine if an event applies. It must switch to matching on `Player` variants instead. Since `Player` is a superset of `PlayerFilter`, this is a straightforward type swap in the matcher logic.
