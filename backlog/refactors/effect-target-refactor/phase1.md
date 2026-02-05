## Phase 1 — Add New Composable `EffectTarget` Variants

Additive only — no existing code changes, no breakage.

**Files changed**: 1

<details>
<summary><code>mtg-sdk/.../scripting/targets/EffectTarget.kt</code></summary>

Add three new variants at the end of the sealed interface (after `StoredEntityTarget`):

```kotlin
sealed interface EffectTarget {
    // ... all existing variants unchanged ...

    /**
     * PLAYER REFERENCE: Refers to a player or set of players.
     * Replaces the overlapping Opponent, AnyPlayer, EachOpponent, EachPlayer variants.
     *
     * Usage:
     * - PlayerRef(Player.Each) → "each player"
     * - PlayerRef(Player.EachOpponent) → "each opponent"
     * - PlayerRef(Player.TargetOpponent) → "target opponent"
     * - PlayerRef(Player.TargetPlayer) → "target player"
     */
    @Serializable
    data class PlayerRef(val player: Player) : EffectTarget {
        override val description: String = player.description
    }

    /**
     * GROUP REFERENCE: Refers to a group of permanents for mass effects.
     * Replaces AllCreatures, AllControlledCreatures, AllOpponentCreatures.
     *
     * Usage:
     * - GroupRef(GroupFilter.AllCreatures) → "all creatures"
     * - GroupRef(GroupFilter.AllCreaturesYouControl) → "creatures you control"
     * - GroupRef(GroupFilter(GameObjectFilter.Creature.withColor(Color.RED))) → "all red creatures"
     */
    @Serializable
    data class GroupRef(val filter: GroupFilter) : EffectTarget {
        override val description: String = filter.description
    }

    /**
     * FILTERED TARGET: Refers to a target matching a composable filter.
     * For cases where ContextTarget isn't appropriate (e.g., dynamic effect targets
     * not bound at cast time).
     *
     * Usage:
     * - FilteredTarget(TargetFilter.Creature.notColor(Color.BLACK).opponentControls())
     */
    @Serializable
    data class FilteredTarget(val filter: TargetFilter) : EffectTarget {
        override val description: String = "target ${filter.description}"
    }
}
```

</details>

**Verification**: Existing tests pass unchanged. The three new variants have no usages yet — they enable Phases 2–7.
