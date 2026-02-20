package com.wingedsheep.sdk.scripting.filters.unified

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.Serializable

/**
 * Filter for selecting groups of permanents (for mass effects).
 * Used for effects like "all creatures", "creatures you control", etc.
 *
 * This replaces CreatureGroupFilter, CreatureDamageFilter, and similar types
 * with a unified approach.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * // All creatures
 * GroupFilter.AllCreatures
 *
 * // All creatures you control
 * GroupFilter.AllCreaturesYouControl
 *
 * // Other creatures you control (excluding source)
 * GroupFilter.OtherCreaturesYouControl
 *
 * // Custom: all tapped creatures with flying
 * GroupFilter(GameObjectFilter.Creature.tapped().withKeyword(Keyword.FLYING))
 *
 * // All nonwhite creatures opponents control
 * GroupFilter(GameObjectFilter.Creature.notColor(Color.WHITE).opponentControls())
 * ```
 */
@Serializable
data class GroupFilter(
    val baseFilter: GameObjectFilter,
    val excludeSelf: Boolean = false
) {
    val description: String
        get() = buildDescription()

    private fun buildDescription(): String = buildString {
        append("all ")
        if (excludeSelf) append("other ")
        append(baseFilter.description)
        if (!baseFilter.description.endsWith("s")) {
            append("s")  // Pluralize simple types
        }
    }

    // =============================================================================
    // Pre-built Creature Groups
    // =============================================================================

    companion object {
        /** All creatures on the battlefield */
        val AllCreatures = GroupFilter(GameObjectFilter.Companion.Creature)

        /** All creatures you control */
        val AllCreaturesYouControl = GroupFilter(GameObjectFilter.Companion.Creature.youControl())

        /** All creatures opponents control */
        val AllCreaturesOpponentsControl = GroupFilter(GameObjectFilter.Companion.Creature.opponentControls())

        /** All other creatures (excluding source) */
        val AllOtherCreatures = GroupFilter(GameObjectFilter.Companion.Creature, excludeSelf = true)

        /** All other creatures you control */
        val OtherCreaturesYouControl = GroupFilter(GameObjectFilter.Companion.Creature.youControl(), excludeSelf = true)

        /** All other creatures opponents control */
        val OtherCreaturesOpponentsControl = GroupFilter(GameObjectFilter.Companion.Creature.opponentControls(), excludeSelf = true)

        /** All other tapped creatures you control (for effects like Adept Watershaper) */
        val OtherTappedCreaturesYouControl = GroupFilter(GameObjectFilter.Companion.Creature.youControl().tapped(), excludeSelf = true)

        /** All attacking creatures */
        val AttackingCreatures = GroupFilter(GameObjectFilter.Companion.Creature.attacking())

        /** All blocking creatures */
        val BlockingCreatures = GroupFilter(GameObjectFilter.Companion.Creature.blocking())

        /** All tapped creatures */
        val TappedCreatures = GroupFilter(GameObjectFilter.Companion.Creature.tapped())

        /** All untapped creatures */
        val UntappedCreatures = GroupFilter(GameObjectFilter.Companion.Creature.untapped())

        // =============================================================================
        // Pre-built Permanent Groups
        // =============================================================================

        /** All permanents */
        val AllPermanents = GroupFilter(GameObjectFilter.Companion.Permanent)

        /** All permanents you control */
        val AllPermanentsYouControl = GroupFilter(GameObjectFilter.Companion.Permanent.youControl())

        /** All nonland permanents */
        val AllNonlandPermanents = GroupFilter(GameObjectFilter.Companion.NonlandPermanent)

        /** All artifacts */
        val AllArtifacts = GroupFilter(GameObjectFilter.Companion.Artifact)

        /** All enchantments */
        val AllEnchantments = GroupFilter(GameObjectFilter.Companion.Enchantment)

        /** All lands */
        val AllLands = GroupFilter(GameObjectFilter.Companion.Land)

        // =============================================================================
        // Convenience Builders for Common Destroy-All Patterns
        // =============================================================================

        /** All creatures of a specific color (e.g., "Destroy all black creatures") */
        fun allCreaturesOfColor(color: Color) = GroupFilter(GameObjectFilter.Companion.Creature.withColor(color))

        /** All lands with a specific subtype (e.g., "Destroy all Islands") */
        fun allLandsWithSubtype(subtype: String) = GroupFilter(GameObjectFilter.Companion.Land.withSubtype(subtype))

        /** All lands with a specific subtype (e.g., "Destroy all Islands") */
        fun allLandsWithSubtype(subtype: Subtype) = GroupFilter(GameObjectFilter.Companion.Land.withSubtype(subtype))

        /** All creatures with a specific subtype (e.g., "Destroy all Goblins") */
        fun allCreaturesWithSubtype(subtype: String) = GroupFilter(GameObjectFilter.Companion.Creature.withSubtype(subtype))
    }

    // =============================================================================
    // Fluent Builder Methods (delegates to GameObjectFilter)
    // =============================================================================

    /** Add color requirement */
    fun withColor(color: Color) = copy(baseFilter = baseFilter.withColor(color))

    /** Exclude color */
    fun notColor(color: Color) = copy(baseFilter = baseFilter.notColor(color))

    /** Add subtype requirement */
    fun withSubtype(subtype: Subtype) = copy(baseFilter = baseFilter.withSubtype(subtype))

    /** Add subtype by string */
    fun withSubtype(subtype: String) = copy(baseFilter = baseFilter.withSubtype(subtype))

    /** Add keyword requirement */
    fun withKeyword(keyword: Keyword) = copy(baseFilter = baseFilter.withKeyword(keyword))

    /** Exclude keyword */
    fun withoutKeyword(keyword: Keyword) = copy(baseFilter = baseFilter.withoutKeyword(keyword))

    /** Power at most */
    fun powerAtMost(max: Int) = copy(baseFilter = baseFilter.powerAtMost(max))

    /** Power at least */
    fun powerAtLeast(min: Int) = copy(baseFilter = baseFilter.powerAtLeast(min))

    /** Toughness at most */
    fun toughnessAtMost(max: Int) = copy(baseFilter = baseFilter.toughnessAtMost(max))

    /** Must be tapped */
    fun tapped() = copy(baseFilter = baseFilter.tapped())

    /** Must be untapped */
    fun untapped() = copy(baseFilter = baseFilter.untapped())

    /** Must be attacking */
    fun attacking() = copy(baseFilter = baseFilter.attacking())

    /** Must be controlled by you */
    fun youControl() = copy(baseFilter = baseFilter.youControl())

    /** Must be controlled by opponent */
    fun opponentControls() = copy(baseFilter = baseFilter.opponentControls())

    /** Exclude the source permanent */
    fun other() = copy(excludeSelf = true)
}
