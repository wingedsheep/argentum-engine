package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Grants a creature subtype to the target (typically the enchanted creature for Auras).
 * Used for Dub: "Enchanted creature is a Knight in addition to its other types."
 *
 * This is a Layer 4 (type-changing) continuous effect that adds a subtype.
 *
 * @property subtype The creature subtype to add (e.g., "Knight")
 * @property target What this ability applies to (typically SourceCreature for Auras → enchanted creature)
 */
@SerialName("GrantSubtype")
@Serializable
data class GrantSubtype(
    val subtype: String,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "is a $subtype in addition to its other types"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newSubtype = replacer.replaceCreatureType(subtype)
        return if (newSubtype != subtype) copy(subtype = newSubtype) else this
    }
}

/**
 * Adds a card type (e.g., "CREATURE") to the target permanent, in addition to its other types.
 * Used for Spacecraft Station mechanic: "It's an artifact creature at 7+."
 *
 * This is a Layer 4 (type-changing) continuous effect that adds a card type.
 *
 * @property cardType The card type to add (e.g., "CREATURE", "ARTIFACT")
 * @property target What this ability applies to
 */
@SerialName("GrantCardType")
@Serializable
data class GrantCardType(
    val cardType: String,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "is also ${
        if (cardType.first().lowercaseChar() in "aeiou") "an" else "a"
    } ${cardType.lowercase()}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Grants a supertype (e.g., Legendary) to the target permanent.
 * Used for On Serra's Wings: "Enchanted creature is legendary."
 *
 * This is a Layer 4 (type-changing) continuous effect that adds a supertype.
 *
 * @property supertype The supertype to add (e.g., "LEGENDARY")
 * @property target What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("GrantSupertype")
@Serializable
data class GrantSupertype(
    val supertype: String,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "is ${supertype.lowercase()}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Adds a color to the target permanent.
 * Used for Deep Freeze: "Enchanted creature is a blue Wall in addition to its other colors and types."
 *
 * This is a Layer 5 (color-changing) continuous effect that adds a color.
 *
 * @property color The color to add
 * @property target What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("GrantColor")
@Serializable
data class GrantColor(
    val color: Color,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "is ${color.name.lowercase()} in addition to its other colors"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Adds the chosen color (resolved from the source's `ChosenColorComponent`) to the target.
 * Used for auras like Shimmerwilds Growth: "Enchanted land is the chosen color."
 *
 * This is a Layer 5 (color-changing) continuous effect. If the source has no chosen color
 * (e.g., somehow on the battlefield without a choice), no color is added.
 *
 * @property target What this ability applies to (typically AttachedCreature for auras;
 *   for enchant-land auras use `StaticTarget.AttachedCreature` — it resolves to whatever
 *   permanent the aura is attached to via `AttachedToComponent`).
 */
@SerialName("GrantChosenColor")
@Serializable
data class GrantChosenColor(
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "is the chosen color"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Adds a creature type to all creatures that have a specific counter type.
 * Used for Aurification: "Each creature with a gold counter on it is a Wall."
 *
 * @property creatureType The creature type to add
 * @property counterType The counter type that creatures must have
 */
@SerialName("AddCreatureTypeByCounter")
@Serializable
data class AddCreatureTypeByCounter(
    val creatureType: String,
    val counterType: String
) : StaticAbility {
    override val description: String =
        "Each creature with a $counterType counter on it is a $creatureType in addition to its other creature types"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newCreatureType = replacer.replaceCreatureType(creatureType)
        return if (newCreatureType != creatureType) copy(creatureType = newCreatureType) else this
    }
}

/**
 * Adds a basic land type to all lands that have a specific counter type, in addition to their other types.
 * Used for Eluge, the Shoreless Sea: "It's an Island in addition to its other types for as long as it has a flood counter on it."
 *
 * This is a Layer 4 (type-changing) continuous effect that adds a land subtype.
 * Unlike SetEnchantedLandType, this does NOT replace existing land subtypes.
 *
 * @property landType The basic land type to add (e.g., "Island", "Plains")
 * @property counterType The counter type that lands must have
 */
@SerialName("AddLandTypeByCounter")
@Serializable
data class AddLandTypeByCounter(
    val landType: String,
    val counterType: String
) : StaticAbility {
    override val description: String =
        "Each land with a $counterType counter on it is ${
            if (landType.first().lowercaseChar() in "aeiou") "an" else "a"
        } $landType in addition to its other types"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Causes the target permanent to lose all abilities.
 * Used for Deep Freeze: "Enchanted creature loses all other abilities."
 * Also used for Humility, Overwhelming Splendor, and similar effects.
 *
 * This is a Layer 6 (ABILITY) continuous effect that clears all keywords and
 * suppresses activated, triggered, and static abilities.
 *
 * @property target What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("LoseAllAbilities")
@Serializable
data class LoseAllAbilities(
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "loses all abilities"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Enchanted land becomes a specific basic land type.
 * Used for auras like Sea's Claim: "Enchanted land is an Island."
 * This replaces all existing land subtypes with the specified type (Rule 305.7).
 *
 * @property landType The basic land type to set (e.g., "Island", "Plains")
 */
@SerialName("SetEnchantedLandType")
@Serializable
data class SetEnchantedLandType(
    val landType: String
) : StaticAbility {
    override val description: String = "Enchanted land is ${
        if (landType.first().lowercaseChar() in "aeiou") "an" else "a"
    } $landType"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Adds card types and subtypes to a group of permanents, in addition to their existing types.
 * Used for Ygra: "Other creatures are Food artifacts in addition to their other types."
 *
 * This generates Layer 4 (TYPE) continuous effects:
 * - AddType for each card type (e.g., "ARTIFACT")
 * - AddSubtype for each subtype (e.g., "Food")
 *
 * @property filter Which permanents are affected (e.g., other creatures)
 * @property addCardTypes Card types to add (e.g., ["ARTIFACT"])
 * @property addSubtypes Subtypes to add (e.g., ["Food"])
 */
@SerialName("GrantAdditionalTypesToGroup")
@Serializable
data class GrantAdditionalTypesToGroup(
    val filter: GroupFilter,
    val addCardTypes: List<String> = emptyList(),
    val addSubtypes: List<String> = emptyList()
) : StaticAbility {
    override val description: String = buildString {
        append("${filter.description} are ")
        val parts = mutableListOf<String>()
        parts.addAll(addSubtypes)
        parts.addAll(addCardTypes.map { it.lowercase() + "s" })
        append(parts.joinToString(" "))
        append(" in addition to their other types")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Animates a group of lands into creatures while keeping them as lands.
 * Used for Ambush Commander: "Forests you control are 1/1 green Elf creatures that are still lands."
 *
 * This generates multiple continuous effects across different layers:
 * - Layer 4 (TYPE): AddType("CREATURE") + AddSubtype for each creature subtype
 * - Layer 5 (COLOR): AddColor for specified colors
 * - Layer 7b (POWER_TOUGHNESS, SET_VALUES): SetPowerToughness
 *
 * @property filter Which lands to animate (e.g., Forests you control)
 * @property power Base power to set
 * @property toughness Base toughness to set
 * @property creatureSubtypes Creature subtypes to add (e.g., ["Elf"])
 * @property colors Colors to add to the animated lands
 */
@SerialName("AnimateLandGroup")
@Serializable
data class AnimateLandGroup(
    val filter: GroupFilter,
    val power: Int,
    val toughness: Int,
    val creatureSubtypes: List<String> = emptyList(),
    val colors: Set<Color> = emptySet()
) : StaticAbility {
    override val description: String = buildString {
        append("Lands matching filter are $power/$toughness")
        if (colors.isNotEmpty()) append(" ${colors.joinToString("/") { it.name.lowercase() }}")
        if (creatureSubtypes.isNotEmpty()) append(" ${creatureSubtypes.joinToString(" ")}")
        append(" creatures that are still lands")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newSubtypes = creatureSubtypes.map { replacer.replaceCreatureType(it) }
        return if (newFilter !== filter || newSubtypes != creatureSubtypes) copy(filter = newFilter, creatureSubtypes = newSubtypes) else this
    }
}

/**
 * Transforms the enchanted permanent into a completely different card type identity.
 * Used for Sugar Coat: "Enchanted permanent is a colorless Food artifact..."
 * Used for Imprisoned in the Moon, Darksteel Mutation, Song of the Dryads, etc.
 *
 * This generates multiple continuous effects across layers:
 * - Layer 4 (TYPE): SetCardTypes to replace all card types, SetAllSubtypes to replace all subtypes
 * - Layer 5 (COLOR): ChangeColor to set color identity (empty set = colorless)
 *
 * Note: Combine with LoseAllAbilities and GrantActivatedAbilityToAttachedCreature
 * separately for ability removal/granting.
 *
 * @property setCardTypes Card types to set (replaces ALL existing card types)
 * @property setSubtypes Subtypes to set (replaces ALL existing subtypes)
 * @property setColors Colors to set (null = don't change, empty = colorless)
 * @property target What this ability applies to
 */
@SerialName("TransformPermanent")
@Serializable
data class TransformPermanent(
    val setCardTypes: Set<String> = emptySet(),
    val setSubtypes: Set<String> = emptySet(),
    val setColors: Set<Color>? = null,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = buildString {
        append("is ")
        if (setColors?.isEmpty() == true) append("a colorless ")
        else if (setColors != null) append("a ${setColors.joinToString("/") { it.name.lowercase() }} ")
        if (setSubtypes.isNotEmpty()) append(setSubtypes.joinToString(" "))
        if (setCardTypes.isNotEmpty()) {
            if (setSubtypes.isNotEmpty()) append(" ")
            append(setCardTypes.joinToString(" ") { it.lowercase() })
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}
