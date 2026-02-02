package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import kotlinx.serialization.Serializable

/**
 * Filter for mass damage effects targeting creatures.
 * Used with DealDamageToGroupEffect to specify which creatures are affected.
 *
 * @deprecated Use [GroupFilter] instead. GroupFilter provides composable
 * predicate-based filtering with fluent builder methods.
 *
 * Migration examples:
 * - `CreatureDamageFilter.All` -> `GroupFilter.AllCreatures`
 * - `CreatureDamageFilter.WithKeyword(Keyword.FLYING)` -> `GroupFilter.AllCreatures.withKeyword(Keyword.FLYING)`
 * - `CreatureDamageFilter.WithoutKeyword(Keyword.FLYING)` -> `GroupFilter.AllCreatures.withoutKeyword(Keyword.FLYING)`
 * - `CreatureDamageFilter.OfColor(Color.RED)` -> `GroupFilter.AllCreatures.withColor(Color.RED)`
 * - `CreatureDamageFilter.Attacking` -> `GroupFilter.AttackingCreatures`
 *
 * Use `CreatureDamageFilter.toUnified()` extension function for automatic conversion.
 *
 * @see GroupFilter
 * @see toUnified
 */
@Deprecated(
    message = "Use GroupFilter instead for composable predicate-based filtering",
    replaceWith = ReplaceWith("GroupFilter", "com.wingedsheep.sdk.scripting.GroupFilter")
)
@Serializable
sealed interface CreatureDamageFilter {
    val description: String

    /** All creatures */
    @Serializable
    data object All : CreatureDamageFilter {
        override val description: String = "each creature"
    }

    /** Creatures with a specific keyword */
    @Serializable
    data class WithKeyword(val keyword: Keyword) : CreatureDamageFilter {
        override val description: String = "each creature with ${keyword.displayName.lowercase()}"
    }

    /** Creatures without a specific keyword */
    @Serializable
    data class WithoutKeyword(val keyword: Keyword) : CreatureDamageFilter {
        override val description: String = "each creature without ${keyword.displayName.lowercase()}"
    }

    /** Creatures of a specific color */
    @Serializable
    data class OfColor(val color: Color) : CreatureDamageFilter {
        override val description: String = "each ${color.displayName.lowercase()} creature"
    }

    /** Creatures not of a specific color */
    @Serializable
    data class NotOfColor(val color: Color) : CreatureDamageFilter {
        override val description: String = "each non${color.displayName.lowercase()} creature"
    }

    /** Attacking creatures only */
    @Serializable
    data object Attacking : CreatureDamageFilter {
        override val description: String = "each attacking creature"
    }
}

/**
 * Filter for groups of creatures affected by mass effects.
 *
 * @deprecated Use [GroupFilter] instead. GroupFilter provides composable
 * predicate-based filtering with fluent builder methods.
 *
 * Migration examples:
 * - `CreatureGroupFilter.AllYouControl` -> `GroupFilter.AllCreaturesYouControl`
 * - `CreatureGroupFilter.AllOpponentsControl` -> `GroupFilter.AllCreaturesOpponentsControl`
 * - `CreatureGroupFilter.All` -> `GroupFilter.AllCreatures`
 * - `CreatureGroupFilter.AllOther` -> `GroupFilter.AllOtherCreatures`
 * - `CreatureGroupFilter.ColorYouControl(Color.RED)` -> `GroupFilter.AllCreaturesYouControl.withColor(Color.RED)`
 * - `CreatureGroupFilter.WithKeywordYouControl(Keyword.FLYING)` -> `GroupFilter.AllCreaturesYouControl.withKeyword(Keyword.FLYING)`
 *
 * Use `CreatureGroupFilter.toUnified()` extension function for automatic conversion.
 *
 * @see GroupFilter
 * @see toUnified
 */
@Deprecated(
    message = "Use GroupFilter instead for composable predicate-based filtering",
    replaceWith = ReplaceWith("GroupFilter", "com.wingedsheep.sdk.scripting.GroupFilter")
)
@Serializable
sealed interface CreatureGroupFilter {
    val description: String

    /** All creatures you control */
    @Serializable
    data object AllYouControl : CreatureGroupFilter {
        override val description: String = "Creatures you control"
    }

    /** All creatures opponents control */
    @Serializable
    data object AllOpponentsControl : CreatureGroupFilter {
        override val description: String = "Creatures your opponents control"
    }

    /** All creatures */
    @Serializable
    data object All : CreatureGroupFilter {
        override val description: String = "All creatures"
    }

    /** All other creatures (except the source) */
    @Serializable
    data object AllOther : CreatureGroupFilter {
        override val description: String = "All other creatures"
    }

    /** Creatures you control with a specific color */
    @Serializable
    data class ColorYouControl(val color: Color) : CreatureGroupFilter {
        override val description: String = "${color.displayName} creatures you control"
    }

    /** Creatures you control with a specific keyword */
    @Serializable
    data class WithKeywordYouControl(val keyword: Keyword) : CreatureGroupFilter {
        override val description: String = "Creatures you control with ${keyword.displayName.lowercase()}"
    }

    /** Other tapped creatures you control (excludes self) */
    @Serializable
    data object OtherTappedYouControl : CreatureGroupFilter {
        override val description: String = "Other tapped creatures you control"
    }

    /** All nonwhite creatures */
    @Serializable
    data object NonWhite : CreatureGroupFilter {
        override val description: String = "All nonwhite creatures"
    }

    /** Creatures that are not a specific color */
    @Serializable
    data class NotColor(val excludedColor: Color) : CreatureGroupFilter {
        override val description: String = "All non${excludedColor.displayName.lowercase()} creatures"
    }
}

/**
 * Filter for creature targeting.
 *
 * @deprecated Use [TargetFilter] instead. TargetFilter provides composable
 * predicate-based filtering with fluent builder methods.
 *
 * Migration examples:
 * - `CreatureTargetFilter.Any` -> `TargetFilter.Creature`
 * - `CreatureTargetFilter.WithFlying` -> `TargetFilter.Creature.withKeyword(Keyword.FLYING)`
 * - `CreatureTargetFilter.WithoutFlying` -> `TargetFilter.Creature.withoutKeyword(Keyword.FLYING)`
 * - `CreatureTargetFilter.Tapped` -> `TargetFilter.TappedCreature`
 * - `CreatureTargetFilter.Untapped` -> `TargetFilter.UntappedCreature`
 * - `CreatureTargetFilter.Attacking` -> `TargetFilter.AttackingCreature`
 * - `CreatureTargetFilter.Nonblack` -> `TargetFilter.Creature.notColor(Color.BLACK)`
 *
 * Use `CreatureTargetFilter.toUnified()` extension function for automatic conversion.
 *
 * @see TargetFilter
 * @see toUnified
 */
@Deprecated(
    message = "Use TargetFilter instead for composable predicate-based filtering",
    replaceWith = ReplaceWith("TargetFilter", "com.wingedsheep.sdk.scripting.TargetFilter")
)
@Serializable
sealed interface CreatureTargetFilter {
    val description: String

    @Serializable
    data object Any : CreatureTargetFilter {
        override val description: String = ""
    }

    @Serializable
    data object WithoutFlying : CreatureTargetFilter {
        override val description: String = "without flying"
    }

    @Serializable
    data object WithFlying : CreatureTargetFilter {
        override val description: String = "with flying"
    }

    @Serializable
    data object Tapped : CreatureTargetFilter {
        override val description: String = "tapped"
    }

    @Serializable
    data object Untapped : CreatureTargetFilter {
        override val description: String = "untapped"
    }

    @Serializable
    data object Attacking : CreatureTargetFilter {
        override val description: String = "attacking"
    }

    @Serializable
    data object Nonblack : CreatureTargetFilter {
        override val description: String = "nonblack"
    }
}
