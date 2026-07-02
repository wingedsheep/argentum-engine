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
 * @property filter What this ability applies to (typically SourceCreature for Auras → enchanted creature)
 */
@SerialName("GrantSubtype")
@Serializable
data class GrantSubtype(
    val subtype: String,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "is a $subtype in addition to its other types"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newSubtype = replacer.replaceCreatureType(subtype)
        return if (newSubtype != subtype) copy(subtype = newSubtype) else this
    }
}

/**
 * Grants the creature type chosen as the source entered (resolved from the source's
 * `CastChoicesComponent`) to a group, in addition to their other types. The chosen-value
 * counterpart to [GrantSubtype], mirroring [GrantChosenColor]'s relationship to [GrantColor].
 * Used by Leyline of Transformation / Conspiracy / Xenograft: "Creatures you control are the
 * chosen type in addition to their other types."
 *
 * This is a Layer 4 (type-changing) continuous effect. If the source has no chosen creature
 * type, no subtype is added.
 *
 * The [filter] half is the standard battlefield projection (Layer 4). The two cross-zone flags
 * extend the grant beyond the battlefield, modeling the Conspiracy / Leyline-of-Transformation
 * clause "The same is true for creature spells you control and creature cards you own that aren't
 * on the battlefield":
 *  - [includeControlledSpells] — creature **spells the source's controller controls** (on the
 *    stack) are also the chosen type.
 *  - [includeOwnedCardsOutsideBattlefield] — creature **cards the source's controller owns** in
 *    any non-battlefield, non-stack zone (hand, library, graveyard, exile, command) are also the
 *    chosen type.
 *
 * Type-changing statics are projected through the layer system, which iterates only the
 * battlefield, so the cross-zone flags are honored by a separate overlay (`ProjectedState`'s
 * cross-zone grant list, consulted by every subtype read-site for non-battlefield objects)
 * rather than by Layer 4 projection. Leave both flags `false` for battlefield-only effects
 * like Xenograft.
 *
 * @property filter Which battlefield permanents are affected (typically `AllCreaturesYouControl`).
 * @property includeControlledSpells Whether creature spells the controller controls are also the type.
 * @property includeOwnedCardsOutsideBattlefield Whether creature cards the controller owns outside
 *   the battlefield are also the type.
 */
@SerialName("GrantChosenSubtype")
@Serializable
data class GrantChosenSubtype(
    val filter: GroupFilter = GroupFilter.source(),
    val includeControlledSpells: Boolean = false,
    val includeOwnedCardsOutsideBattlefield: Boolean = false
) : StaticAbility {
    override val description: String = buildString {
        append("${filter.description} are the chosen type in addition to their other types")
        if (includeControlledSpells || includeOwnedCardsOutsideBattlefield) {
            append("; so are your creature spells and creature cards outside the battlefield")
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants every creature type to the target, in addition to its existing types,
 * without granting the Changeling keyword. Used for cards like Stalactite Dagger:
 * "Equipped creature ... is all creature types." The card text doesn't say
 * "changeling", so the equipped creature shouldn't display a Changeling badge.
 *
 * This is a Layer 4 (type-changing) continuous effect.
 *
 * @property filter What this ability applies to (typically AttachedCreature for Equipment)
 */
@SerialName("IsAllCreatureTypes")
@Serializable
data class IsAllCreatureTypes(
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "is all creature types"
}

/**
 * Adds a card type (e.g., "CREATURE") to the target permanent, in addition to its other types.
 * Used for Spacecraft Station mechanic: "It's an artifact creature at 7+."
 *
 * This is a Layer 4 (type-changing) continuous effect that adds a card type.
 *
 * @property cardType The card type to add (e.g., "CREATURE", "ARTIFACT")
 * @property filter What this ability applies to
 */
@SerialName("GrantCardType")
@Serializable
data class GrantCardType(
    val cardType: String,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "is also ${
        if (cardType.first().lowercaseChar() in "aeiou") "an" else "a"
    } ${cardType.lowercase()}"
}

/**
 * Removes a card type (e.g., "CREATURE") from the target permanent.
 * The mirror of [GrantCardType] — a Layer 4 (type-changing) continuous effect.
 *
 * Used by Impending ("isn't a creature until the last time counter is removed"), gated
 * behind a [ConditionalStaticAbility]. General-purpose: any "it's no longer a [type]"
 * effect can reuse it.
 *
 * @property cardType The card type to remove (e.g., "CREATURE", "ARTIFACT")
 * @property filter What this ability applies to
 */
@SerialName("RemoveCardType")
@Serializable
data class RemoveCardType(
    val cardType: String,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "isn't ${
        if (cardType.first().lowercaseChar() in "aeiou") "an" else "a"
    } ${cardType.lowercase()}"
}

/**
 * Grants a supertype (e.g., Legendary) to the target permanent.
 * Used for On Serra's Wings: "Enchanted creature is legendary."
 *
 * This is a Layer 4 (type-changing) continuous effect that adds a supertype.
 *
 * @property supertype The supertype to add (e.g., "LEGENDARY")
 * @property filter What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("GrantSupertype")
@Serializable
data class GrantSupertype(
    val supertype: String,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "is ${supertype.lowercase()}"
}

/**
 * Adds a color to the target permanent.
 * Used for Deep Freeze: "Enchanted creature is a blue Wall in addition to its other colors and types."
 *
 * This is a Layer 5 (color-changing) continuous effect that adds a color.
 *
 * @property color The color to add
 * @property filter What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("GrantColor")
@Serializable
data class GrantColor(
    val color: Color,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "is ${color.name.lowercase()} in addition to its other colors"
}

/**
 * Adds the chosen color (resolved from the source's `CastChoicesComponent`) to the target.
 * Used for auras like Shimmerwilds Growth: "Enchanted land is the chosen color."
 *
 * This is a Layer 5 (color-changing) continuous effect. If the source has no chosen color
 * (e.g., somehow on the battlefield without a choice), no color is added.
 *
 * @property filter What this ability applies to (typically AttachedCreature for auras;
 *   for enchant-land auras use `GroupFilter.attachedCreature()` — it resolves to whatever
 *   permanent the aura is attached to via `AttachedToComponent`).
 */
@SerialName("GrantChosenColor")
@Serializable
data class GrantChosenColor(
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "is the chosen color"
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
}

/**
 * Causes the target permanent to lose all abilities.
 * Used for Deep Freeze: "Enchanted creature loses all other abilities."
 * Also used for Humility, Overwhelming Splendor, and similar effects.
 *
 * This is a Layer 6 (ABILITY) continuous effect that clears all keywords and
 * suppresses activated, triggered, and static abilities.
 *
 * @property filter What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("LoseAllAbilities")
@Serializable
data class LoseAllAbilities(
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "loses all abilities"
}

/**
 * Sets the affected permanent's name to a fixed string, overriding its printed name.
 * Used for auras like Honest Work: "Enchanted creature ... named Humble Merchant."
 *
 * Setting a name is a text-changing effect (CR 612 / 613.1c), so this projects in Layer 3
 * (TEXT) via [com.wingedsheep.engine.mechanics.layers.Modification.SetName]. The override is
 * read through `ProjectedState.getName`. Pair with [TransformPermanent], [LoseAllAbilities],
 * [SetBasePowerToughnessStatic] and [GrantActivatedAbility] to model the full
 * "becomes a 1/1 X named Y with '...'" composite.
 *
 * @property name The fixed name to set (e.g., "Humble Merchant").
 * @property filter What this ability applies to (typically AttachedCreature for auras).
 */
@SerialName("SetName")
@Serializable
data class SetName(
    val name: String,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "is named $name"
}

/**
 * The affected permanent can't be turned face up.
 * Used for Unable to Scream: "As long as enchanted creature is face down, it can't be turned
 * face up." Only meaningful while the affected permanent is face down (a face-up permanent
 * can't be "turned face up" anyway), so the static is applied unconditionally to the attached
 * creature — the turn-face-up special action reads the projected flag and is rejected.
 *
 * This is a Layer 6 (ABILITY) continuous effect.
 *
 * @property filter What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("CantBeTurnedFaceUp")
@Serializable
data class CantBeTurnedFaceUp(
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "can't be turned face up"
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
}

/**
 * Enchanted land becomes the basic land type chosen as the source entered (resolved from the
 * source's `CastChoicesComponent`). Used for auras like Phantasmal Terrain:
 * "As this Aura enters, choose a basic land type. Enchanted land is the chosen type."
 * This replaces all existing land subtypes with the chosen type (Rule 305.7). The
 * chosen-value counterpart to [SetEnchantedLandType], mirroring [GrantChosenColor]'s
 * relationship to [GrantColor]. If the source has no chosen land type, no change is applied.
 */
@SerialName("SetEnchantedLandTypeFromChosen")
@Serializable
data object SetEnchantedLandTypeFromChosen : StaticAbility {
    override val description: String = "Enchanted land is the chosen type"
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
 * Sets the basic land type(s) of a *group* of lands, replacing all of their existing land
 * subtypes and stripping the abilities those subtypes / their rules text granted (CR 305.7).
 * The group counterpart of the single-target [SetEnchantedLandType], and the "set/replace"
 * counterpart of [GrantAdditionalTypesToGroup] (which *adds* types and keeps abilities).
 *
 * Used for Blood Moon / Magus of the Moon: "Nonbasic lands are Mountains. (They lose all other
 * land types and abilities and have '{T}: Add {R}.')". Gate behind a [ConditionalStaticAbility]
 * for conditional variants (e.g. Zhao, the Moon Slayer — active only while it has a conqueror
 * counter).
 *
 * This lowers to two continuous effects (see `StaticAbilityHandler.convertSetLandTypesForGroup`):
 * - Layer 4 (TYPE): [com.wingedsheep.engine.mechanics.layers.Modification.SetBasicLandTypes] —
 *   removes the old basic land subtypes and adds [landTypes] (CR 305.7).
 * - Layer 6 (ABILITY): [com.wingedsheep.engine.mechanics.layers.Modification.RemoveAllAbilities] —
 *   removes the lands' printed abilities. The new basic land type's intrinsic mana ability
 *   (e.g. "{T}: Add {R}" for Mountain) is *not* removed: it is derived from the projected
 *   subtypes by `IntrinsicManaAbilities`, which takes priority over ability suppression.
 *
 * @property filter Which lands are affected (e.g. all nonbasic lands — [GroupFilter] over
 *   `GameObjectFilter.NonbasicLand`, both players' lands).
 * @property landTypes The basic land type(s) to set (e.g. {"Mountain"}), replacing all others.
 */
@SerialName("SetLandTypesForGroup")
@Serializable
data class SetLandTypesForGroup(
    val filter: GroupFilter,
    val landTypes: Set<String>
) : StaticAbility {
    override val description: String = buildString {
        append(filter.description)
        append(" are ")
        append(landTypes.joinToString(" "))
        append(" (they lose all other land types and abilities)")
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
 * Used for Witness Protection: "...is a green and white Citizen creature with base power
 * and toughness 1/1 named Legitimate Businessperson."
 *
 * This generates multiple continuous effects across layers:
 * - Layer 3 (TEXT): SetName to overwrite the object's name (CR 612.8)
 * - Layer 4 (TYPE): SetCardTypes to replace all card types, SetAllSubtypes to replace all subtypes
 * - Layer 5 (COLOR): ChangeColor to set color identity (empty set = colorless)
 *
 * Note: Combine with LoseAllAbilities and GrantActivatedAbility
 * separately for ability removal/granting.
 *
 * @property setCardTypes Card types to set (replaces ALL existing card types)
 * @property setSubtypes Subtypes to set (replaces ALL existing subtypes). A non-empty set
 *   replaces all subtypes; an empty set leaves subtypes unchanged unless [clearSubtypes] is set.
 * @property setColors Colors to set (null = don't change, empty = colorless)
 * @property setName Name to set (null = don't change). Per CR 612.8 the object "loses any
 *   names it had and has only the specified name" — applied at Layer 3 (TEXT), before the
 *   type/color layers. Used by Witness Protection ("named Legitimate Businessperson").
 * @property clearSubtypes When true, removes all subtypes (used with `setSubtypes = emptySet()`
 *   to express "has no subtypes" — e.g. the Enduring cycle's enchantment-only return, which
 *   strips Sheep/Glimmer). Distinguishes "clear subtypes" from "don't change subtypes".
 * @property filter What this ability applies to
 */
@SerialName("TransformPermanent")
@Serializable
data class TransformPermanent(
    val setCardTypes: Set<String> = emptySet(),
    val setSubtypes: Set<String> = emptySet(),
    val setColors: Set<Color>? = null,
    val setName: String? = null,
    val clearSubtypes: Boolean = false,
    val filter: GroupFilter = GroupFilter.attachedCreature()
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
        if (setName != null) append(" named $setName")
    }
}
