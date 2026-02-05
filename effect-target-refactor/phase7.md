## Phase 7 — Deprecate Old `EffectTarget` Variants

**Files changed**: 1

<details>
<summary><code>mtg-sdk/.../scripting/targets/EffectTarget.kt</code></summary>

Add `@Deprecated` annotations with `ReplaceWith` to every retired variant:

```kotlin
// Player/group variants:

@Deprecated(
    "Use ContextTarget(0) when a target is declared, or PlayerRef(Player.TargetOpponent) for defaults",
    ReplaceWith("ContextTarget(0)")
)
@Serializable
data object Opponent : EffectTarget { ... }

@Deprecated(
    "Use ContextTarget(0) when a target is declared, or PlayerRef(Player.TargetPlayer) for defaults",
    ReplaceWith("ContextTarget(0)")
)
@Serializable
data object AnyPlayer : EffectTarget { ... }

@Deprecated("Use PlayerRef(Player.Each)", ReplaceWith("PlayerRef(Player.Each)"))
@Serializable
data object EachPlayer : EffectTarget { ... }

@Deprecated("Use PlayerRef(Player.EachOpponent)", ReplaceWith("PlayerRef(Player.EachOpponent)"))
@Serializable
data object EachOpponent : EffectTarget { ... }

@Deprecated("Use GroupRef(GroupFilter.AllCreatures)", ReplaceWith("GroupRef(GroupFilter.AllCreatures)"))
@Serializable
data object AllCreatures : EffectTarget { ... }

@Deprecated("Use GroupRef(GroupFilter.AllCreaturesYouControl)", ReplaceWith("GroupRef(GroupFilter.AllCreaturesYouControl)"))
@Serializable
data object AllControlledCreatures : EffectTarget { ... }

@Deprecated("Use GroupRef(GroupFilter.AllCreaturesOpponentsControl)", ReplaceWith("GroupRef(GroupFilter.AllCreaturesOpponentsControl)"))
@Serializable
data object AllOpponentCreatures : EffectTarget { ... }

// Hardcoded filtered variants:

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetCreature : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetOpponentCreature : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetControlledCreature : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetPermanent : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetNonlandPermanent : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetLand : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetNonblackCreature : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetTappedCreature : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetCreatureWithFlying : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object AnyTarget : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetCardInGraveyard : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetEnchantment : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetArtifact : EffectTarget { ... }

@Deprecated("Use ContextTarget(0)", ReplaceWith("ContextTarget(0)"))
@Serializable
data object TargetOpponentNonlandPermanent : EffectTarget { ... }
```

Don't delete yet — other sets or engine code may reference them.

</details>
