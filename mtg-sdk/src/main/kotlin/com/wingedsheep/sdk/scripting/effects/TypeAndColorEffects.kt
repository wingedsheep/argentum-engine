package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Type, Subtype, and Color Change Effects
// =============================================================================

/**
 * Remove all creature types from a target creature.
 * "Target creature loses all creature types until end of turn"
 */
@SerialName("LoseAllCreatureTypes")
@Serializable
data class LoseAllCreatureTypesEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} loses all creature types")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Target creature becomes the creature type of your choice until end of turn.
 * This replaces all creature subtypes with the chosen type.
 *
 * @property target The creature to change
 * @property duration How long the type change lasts
 */
@SerialName("BecomeCreatureType")
@Serializable
data class BecomeCreatureTypeEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn,
    val excludedTypes: List<String> = emptyList()
) : Effect {
    override val description: String = buildString {
        append("${target.description} becomes the creature type of your choice")
        if (excludedTypes.isNotEmpty()) append(" other than ${excludedTypes.joinToString(", ")}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Set creature subtypes for a single target creature.
 * "It becomes a Bird Giant." (replaces existing creature subtypes)
 *
 * Creates a floating effect on Layer.TYPE that replaces creature subtypes.
 *
 * @property subtypes The set of subtypes to replace existing creature subtypes with (used when fromChosenValueKey is null)
 * @property target The creature to change
 * @property duration How long the effect lasts
 * @property fromChosenValueKey When non-null, reads the subtype from EffectContext.chosenValues[key]
 *   instead of using the hardcoded [subtypes] field. Used in pipeline compositions where the
 *   subtype is chosen at runtime via ChooseOptionEffect.
 */
@SerialName("SetCreatureSubtypes")
@Serializable
data class SetCreatureSubtypesEffect(
    val subtypes: Set<String> = emptySet(),
    val target: EffectTarget = EffectTarget.Self,
    val duration: Duration = Duration.Permanent,
    val fromChosenValueKey: String? = null
) : Effect {
    override val description: String = buildString {
        if (fromChosenValueKey != null) {
            append("${target.description} becomes the chosen creature type")
        } else {
            append("${target.description} becomes a ${subtypes.joinToString(" ")}")
        }
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newSubtypes = subtypes.map { replacer.replaceCreatureType(it) }.toSet()
        return if (newSubtypes != subtypes) copy(subtypes = newSubtypes) else this
    }
}

/**
 * Set creature subtypes for a group of creatures.
 * "Each creature you control becomes a Shade until end of turn."
 *
 * Creates a floating effect on Layer.TYPE that replaces creature subtypes.
 *
 * @property subtypes The set of subtypes to replace existing creature subtypes with
 * @property filter Which creatures are affected
 * @property duration How long the effect lasts
 */
@SerialName("SetGroupCreatureSubtypes")
@Serializable
data class SetGroupCreatureSubtypesEffect(
    val subtypes: Set<String>,
    val filter: GroupFilter = GroupFilter.AllCreaturesYouControl,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${filter.description} become ${subtypes.joinToString(" ")}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newSubtypes = subtypes.map { replacer.replaceCreatureType(it) }.toSet()
        return if (newFilter !== filter || newSubtypes != subtypes)
            copy(filter = newFilter, subtypes = newSubtypes) else this
    }
}

/**
 * Add a creature subtype to a target in addition to its other types.
 * "Target creature becomes a Zombie in addition to its other types until end of turn."
 *
 * Creates a floating effect on Layer.TYPE that adds the subtype.
 *
 * @property subtype The creature subtype to add (e.g., "Zombie", "Warrior")
 * @property target Which entity to modify
 * @property duration How long the effect lasts
 * @property fromChosenValueKey When non-null, reads the subtype from EffectContext.chosenValues[key]
 *   instead of using the hardcoded [subtype] field.
 */
@SerialName("AddCreatureType")
@Serializable
data class AddCreatureTypeEffect(
    val subtype: String = "",
    val target: EffectTarget = EffectTarget.Self,
    val duration: Duration = Duration.Permanent,
    val fromChosenValueKey: String? = null
) : Effect {
    override val description: String = buildString {
        if (fromChosenValueKey != null) {
            append("${target.description} becomes the chosen creature type in addition to its other types")
        } else {
            append("${target.description} becomes a $subtype in addition to its other types")
        }
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newSubtype = replacer.replaceCreatureType(subtype)
        return if (newSubtype != subtype) copy(subtype = newSubtype) else this
    }
}

/**
 * Add a subtype to a target permanent (any type — creature, land, etc.).
 * "Target land becomes the basic land type of your choice in addition to its other types."
 *
 * This is a general-purpose version of AddCreatureTypeEffect that doesn't check
 * if the target is a creature. Use this for adding land subtypes or any non-creature subtypes.
 *
 * @property subtype The subtype to add (static value)
 * @property target Which permanent to modify
 * @property duration How long the effect lasts
 * @property fromChosenValueKey If set, reads the subtype from EffectContext.chosenValues instead of [subtype]
 */
@SerialName("AddSubtype")
@Serializable
data class AddSubtypeEffect(
    val subtype: String = "",
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.EndOfTurn,
    val fromChosenValueKey: String? = null
) : Effect {
    override val description: String = buildString {
        if (fromChosenValueKey != null) {
            append("${target.description} becomes the chosen type in addition to its other types")
        } else {
            append("${target.description} gains $subtype in addition to its other types")
        }
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Add a card type to a target permanent.
 * "That creature becomes an artifact in addition to its other types."
 *
 * Creates a floating effect on Layer.TYPE with AddType modification.
 * Unlike AnimateLandEffect, this does NOT set P/T — it only adds the type.
 *
 * @property cardType The card type to add (e.g., "ARTIFACT", "CREATURE")
 * @property target The permanent to modify
 * @property duration How long the type change lasts (default: Permanent)
 */
@SerialName("AddCardType")
@Serializable
data class AddCardTypeEffect(
    val cardType: String,
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String =
        "${target.description} becomes a${if (cardType.startsWith("A", ignoreCase = true)) "n" else ""} ${cardType.lowercase()} in addition to its other types"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Change color for a group of creatures.
 * "Each creature you control becomes black until end of turn."
 *
 * Creates a floating effect on Layer.COLOR that sets colors.
 *
 * @property colors The set of color names to set (replaces existing colors)
 * @property filter Which creatures are affected
 * @property duration How long the effect lasts
 */
@SerialName("ChangeGroupColor")
@Serializable
data class ChangeGroupColorEffect(
    val colors: Set<String>,
    val filter: GroupFilter = GroupFilter.AllCreaturesYouControl,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${filter.description} become ${colors.joinToString(", ") { it.lowercase() }}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Choose a color and store that choice on a target permanent.
 *
 * Used by permanents that make a color choice during resolution, rather than as
 * they enter, and then have static abilities reading the chosen color.
 */
@SerialName("ChooseColorForTarget")
@Serializable
data class ChooseColorForTargetEffect(
    val target: EffectTarget = EffectTarget.Self,
    val prompt: String = "Choose a color"
) : Effect {
    override val description: String = prompt

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Change the text of target spell or permanent by replacing all instances of one
 * creature type with another.
 * (This effect lasts indefinitely.)
 *
 * Used by Artificial Evolution. Resolution involves two player decisions:
 * 1. Choose the creature type to replace (FROM)
 * 2. Choose the replacement creature type (TO)
 *
 * @param excludedTypes Creature types that cannot be chosen as the replacement (TO) type.
 *   For Artificial Evolution this is listOf("Wall"). Other cards may have no restriction.
 *
 * The executor adds a TextReplacementComponent to the target entity.
 */
@SerialName("ChangeCreatureTypeText")
@Serializable
data class ChangeCreatureTypeTextEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val excludedTypes: List<String> = emptyList()
) : Effect {
    override val description: String = buildString {
        append("Change the text of ${target.description} by replacing all instances of one creature type with another.")
        if (excludedTypes.isNotEmpty()) {
            append(" The new creature type can't be ${excludedTypes.joinToString(" or ")}.")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
