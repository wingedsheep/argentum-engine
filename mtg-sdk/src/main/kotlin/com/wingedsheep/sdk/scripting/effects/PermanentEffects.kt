package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.TriggeredAbility
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Permanent Modification Effects
// =============================================================================

/**
 * Tap/Untap target effect.
 * "Tap target creature" or "Untap target creature"
 */
@SerialName("TapUntap")
@Serializable
data class TapUntapEffect(
    val target: EffectTarget,
    val tap: Boolean = true
) : Effect {
    override val description: String = "${if (tap) "Tap" else "Untap"} ${target.description}"
}

/**
 * Modify power/toughness effect.
 * "Target creature gets +X/+Y until end of turn"
 */
@SerialName("ModifyStats")
@Serializable
data class ModifyStatsEffect(
    val powerModifier: Int,
    val toughnessModifier: Int,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gets ")
        append(if (powerModifier >= 0) "+$powerModifier" else "$powerModifier")
        append("/")
        append(if (toughnessModifier >= 0) "+$toughnessModifier" else "$toughnessModifier")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

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
}

/**
 * Add counters effect.
 * "Put X +1/+1 counters on target creature"
 */
@SerialName("AddCounters")
@Serializable
data class AddCountersEffect(
    val counterType: String,
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Put $count $counterType counter${if (count != 1) "s" else ""} on ${target.description}"
}

/**
 * Remove counters effect.
 * "Remove X -1/-1 counters from target creature"
 */
@SerialName("RemoveCounters")
@Serializable
data class RemoveCountersEffect(
    val counterType: String,
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Remove $count $counterType counter${if (count != 1) "s" else ""} from ${target.description}"
}

/**
 * Remove all counters of a specific type from all creatures.
 * Used for Aurification: "remove all gold counters from all creatures."
 */
@SerialName("RemoveAllCountersOfType")
@Serializable
data class RemoveAllCountersOfTypeEffect(
    val counterType: String
) : Effect {
    override val description: String = "Remove all $counterType counters from all creatures"
}

/**
 * Grant a keyword to a target until end of turn.
 * "Target creature gains flying until end of turn."
 */
@SerialName("GrantKeywordUntilEndOfTurn")
@Serializable
data class GrantKeywordUntilEndOfTurnEffect(
    val keyword: Keyword,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains ${keyword.displayName.lowercase()}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Grant a keyword to multiple creatures until end of turn.
 * Used for cards like Nature's Cloak: "Green creatures you control gain forestwalk until end of turn."
 *
 * @property keyword The keyword to grant
 * @property filter Which creatures are affected
 * @property duration How long the effect lasts
 */
@SerialName("GrantKeywordToGroup")
@Serializable
data class GrantKeywordToGroupEffect(
    val keyword: Keyword,
    val filter: GroupFilter = GroupFilter.AllCreatures,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${filter.description} gain ${keyword.displayName.lowercase()}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Modify power/toughness for a group of creatures until end of turn.
 * Used for cards like Warrior's Charge: "Creatures you control get +1/+1 until end of turn."
 *
 * @property powerModifier Power bonus (can be negative)
 * @property toughnessModifier Toughness bonus (can be negative)
 * @property filter Which creatures are affected
 * @property duration How long the effect lasts
 */
@SerialName("ModifyStatsForGroup")
@Serializable
data class ModifyStatsForGroupEffect(
    val powerModifier: Int,
    val toughnessModifier: Int,
    val filter: GroupFilter = GroupFilter.AllCreatures,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${filter.description} get ")
        val powerStr = if (powerModifier >= 0) "+$powerModifier" else "$powerModifier"
        val toughStr = if (toughnessModifier >= 0) "+$toughnessModifier" else "$toughnessModifier"
        append("$powerStr/$toughStr")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Put -1/-1 counters on a creature.
 * Used for blight effects and wither-style damage.
 *
 * @property count Number of -1/-1 counters to place
 * @property target The creature to receive the counters
 */
@SerialName("AddMinusCounters")
@Serializable
data class AddMinusCountersEffect(
    val count: Int,
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Put $count -1/-1 counter${if (count != 1) "s" else ""} on ${target.description}"
}

/**
 * Effect that transforms multiple creatures at once.
 * Used for effects like Curious Colossus: "each creature target opponent controls
 * loses all abilities, becomes a Coward, and has base P/T 1/1."
 *
 * @property target Which creatures are affected
 * @property loseAllAbilities If true, creatures lose all abilities
 * @property addCreatureType Type to add (if any)
 * @property setBasePower New base power (if set)
 * @property setBaseToughness New base toughness (if set)
 */
@SerialName("TransformAllCreatures")
@Serializable
data class TransformAllCreaturesEffect(
    val target: EffectTarget,
    val loseAllAbilities: Boolean = false,
    val addCreatureType: String? = null,
    val setBasePower: Int? = null,
    val setBaseToughness: Int? = null
) : Effect {
    override val description: String = buildString {
        append("Each creature ${target.description}")
        val effects = mutableListOf<String>()
        if (loseAllAbilities) effects.add("loses all abilities")
        if (addCreatureType != null) effects.add("becomes a $addCreatureType in addition to its other types")
        if (setBasePower != null && setBaseToughness != null) {
            effects.add("has base power and toughness $setBasePower/$setBaseToughness")
        }
        append(" ")
        append(effects.joinToString(", "))
    }
}

/**
 * Tap all creatures matching a filter.
 * "Tap all nonwhite creatures."
 * Used for Blinding Light.
 *
 * @property filter Which creatures are affected
 */
@SerialName("TapAllCreatures")
@Serializable
data class TapAllCreaturesEffect(
    val filter: GroupFilter = GroupFilter.AllCreatures
) : Effect {
    override val description: String = "Tap ${filter.description.replaceFirstChar { it.lowercase() }}"
}

/**
 * Untap all creatures you control.
 * Used for Mobilize: "Untap all creatures you control."
 */
@SerialName("UntapAllCreaturesYouControl")
@Serializable
data object UntapAllCreaturesYouControlEffect : Effect {
    override val description: String = "Untap all creatures you control"
}

/**
 * Tap up to X target creatures.
 * Used for Tidal Surge: "Tap up to three target creatures without flying."
 * Note: The targeting filter is specified in the spell's TargetCreature, not here.
 */
@SerialName("TapTargetCreatures")
@Serializable
data class TapTargetCreaturesEffect(
    val maxTargets: Int
) : Effect {
    override val description: String = "Tap up to $maxTargets target creature${if (maxTargets > 1) "s" else ""}"
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
}

/**
 * Grant a triggered ability to a target until end of turn.
 * "Target creature gains 'When this creature deals combat damage to a player, ...'"
 *
 * @property ability The triggered ability to grant
 * @property target The creature to grant the ability to
 * @property duration How long the grant lasts
 */
@SerialName("GrantTriggeredAbilityUntilEndOfTurn")
@Serializable
data class GrantTriggeredAbilityUntilEndOfTurnEffect(
    val ability: TriggeredAbility,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains \"${ability.description}\"")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Grant an activated ability to a target until end of turn.
 * "Target creature gains '{cost}: {effect}' until end of turn"
 *
 * Used for cards like Run Wild that grant activated abilities temporarily.
 *
 * @property ability The activated ability to grant
 * @property target The creature to grant the ability to
 * @property duration How long the grant lasts
 */
@Serializable
data class GrantActivatedAbilityUntilEndOfTurnEffect(
    val ability: ActivatedAbility,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains \"${ability.description}\"")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Modify power/toughness by a dynamic amount.
 * "Target creature gets -X/-X until end of turn, where X is the number of Zombies on the battlefield."
 */
@SerialName("DynamicModifyStats")
@Serializable
data class DynamicModifyStatsEffect(
    val powerModifier: DynamicAmount,
    val toughnessModifier: DynamicAmount,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gets ")
        append("${powerModifier.description}/${toughnessModifier.description}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Gain control of a permanent based on who controls the most creatures of a subtype.
 * The player with strictly more creatures of the given subtype than all other players
 * gains control of the target permanent.
 *
 * Used by Thoughtbound Primoc: "At the beginning of your upkeep, if a player controls
 * more Wizards than each other player, that player gains control of Thoughtbound Primoc."
 */
@SerialName("GainControlByMostOfSubtype")
@Serializable
data class GainControlByMostOfSubtypeEffect(
    val subtype: Subtype,
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String =
        "the player who controls the most ${subtype.value}s gains control of ${target.description}"
}

/**
 * Gain control of target permanent.
 * "Gain control of target permanent."
 *
 * Used by Blatant Thievery and similar control-stealing effects.
 */
@SerialName("GainControl")
@Serializable
data class GainControlEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = buildString {
        append("gain control of ${target.description}")
        if (duration != Duration.Permanent && duration.description.isNotEmpty()) {
            append(" ${duration.description}")
        }
    }
}

/**
 * Turn target creature face down.
 * "Turn target creature with a morph ability face down."
 * Used for Backslide and similar effects.
 */
@SerialName("TurnFaceDown")
@Serializable
data class TurnFaceDownEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Turn ${target.description} face down"
}

/**
 * Turn target face-down creature face up.
 * "Turn target face-down creature an opponent controls face up."
 * Used for Break Open and similar effects.
 */
@SerialName("TurnFaceUp")
@Serializable
data class TurnFaceUpEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Turn ${target.description} face up"
}

/**
 * Choose a creature type. Creatures of the chosen type get +X/+Y until end of turn.
 *
 * Used for Defensive Maneuvers: "Creatures of the creature type of your choice get +0/+4
 * until end of turn."
 * Used for Tribal Unity: "Creatures of the creature type of your choice get +X/+X
 * until end of turn."
 *
 * At resolution time, the executor:
 * 1. Evaluates dynamic amounts (e.g., X value)
 * 2. Presents a ChooseOptionDecision with all creature types
 * 3. Pushes a ChooseCreatureTypeModifyStatsContinuation with resolved values
 * 4. On response, creates a floating effect for all creatures of the chosen type
 *
 * @property powerModifier Power bonus (can be dynamic, e.g., DynamicAmount.XValue)
 * @property toughnessModifier Toughness bonus (can be dynamic)
 * @property duration How long the effect lasts
 */
@SerialName("ChooseCreatureTypeModifyStats")
@Serializable
data class ChooseCreatureTypeModifyStatsEffect(
    val powerModifier: DynamicAmount,
    val toughnessModifier: DynamicAmount,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    constructor(powerModifier: Int, toughnessModifier: Int, duration: Duration = Duration.EndOfTurn) :
        this(DynamicAmount.Fixed(powerModifier), DynamicAmount.Fixed(toughnessModifier), duration)

    override val description: String = buildString {
        append("Creatures of the creature type of your choice get ")
        append("+${powerModifier.description}/+${toughnessModifier.description}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Choose a creature type. Each creature becomes that type until end of turn.
 *
 * Used for Standardize: "Choose a creature type other than Wall. Each creature becomes
 * that type until end of turn."
 *
 * At resolution time, the executor:
 * 1. Presents a ChooseOptionDecision with creature types (excluding any in excludedTypes)
 * 2. Pushes a BecomeChosenTypeAllCreaturesContinuation
 * 3. On response, creates a floating effect that sets all creatures' subtypes
 *
 * @property excludedTypes Creature types that cannot be chosen (e.g., "Wall")
 * @property duration How long the effect lasts
 */
@Serializable
data class BecomeChosenTypeAllCreaturesEffect(
    val excludedTypes: List<String> = emptyList(),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("Choose a creature type")
        if (excludedTypes.isNotEmpty()) append(" other than ${excludedTypes.joinToString(", ")}")
        append(". Each creature becomes that type")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
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
}

/**
 * Give control of a permanent to a targeted player.
 * Unlike GainControlEffect (which always gives control to the ability's controller),
 * this effect gives control to a player resolved from a target.
 *
 * Used by Custody Battle: "target opponent gains control of this creature unless you sacrifice a land."
 *
 * @property permanent Which permanent changes control (default: enchanted creature)
 * @property newController Which player gains control (default: first target, expected to be a player)
 */
@Serializable
data class GiveControlToTargetPlayerEffect(
    val permanent: EffectTarget = EffectTarget.EnchantedCreature,
    val newController: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = "target opponent gains control of ${permanent.description}"
}

/**
 * Grant stats and/or a keyword to the enchanted creature and all other creatures
 * that share a creature type with it.
 *
 * Used by the Crown cycle from Onslaught (Crown of Fury, Crown of Vigor, etc.)
 * which sacrifice themselves to buff creatures sharing a type with the enchanted creature.
 *
 * At resolution time, the executor:
 * 1. Finds the enchanted creature via AttachedToComponent on the source (aura)
 * 2. Gets the enchanted creature's creature subtypes
 * 3. Applies the effect to all creatures sharing at least one subtype
 *
 * @property powerModifier Power bonus (can be negative)
 * @property toughnessModifier Toughness bonus (can be negative)
 * @property keyword Optional keyword to grant
 * @property protectionColors Optional set of colors to grant protection from
 * @property duration How long the effect lasts
 */
@SerialName("GrantToEnchantedCreatureTypeGroup")
@Serializable
data class GrantToEnchantedCreatureTypeGroupEffect(
    val powerModifier: Int = 0,
    val toughnessModifier: Int = 0,
    val keyword: Keyword? = null,
    val protectionColors: Set<Color> = emptySet(),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("Enchanted creature and other creatures that share a creature type with it")
        if (powerModifier != 0 || toughnessModifier != 0) {
            val powerStr = if (powerModifier >= 0) "+$powerModifier" else "$powerModifier"
            val toughStr = if (toughnessModifier >= 0) "+$toughnessModifier" else "$toughnessModifier"
            append(" get $powerStr/$toughStr")
        }
        if (keyword != null) {
            if (powerModifier != 0 || toughnessModifier != 0) append(" and")
            append(" gain ${keyword.displayName.lowercase()}")
        }
        if (protectionColors.isNotEmpty()) {
            if (powerModifier != 0 || toughnessModifier != 0 || keyword != null) append(" and")
            append(" gain protection from ")
            append(protectionColors.joinToString(" and from ") { it.displayName.lowercase() })
        }
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
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
}

/**
 * Choose a creature type. Untap all creatures of the chosen type.
 *
 * Used for Riptide Chronologist: "{U}, Sacrifice Riptide Chronologist:
 * Untap all creatures of the creature type of your choice."
 *
 * At resolution time, the executor:
 * 1. Presents a ChooseOptionDecision with all creature types
 * 2. Pushes a ChooseCreatureTypeUntapContinuation
 * 3. On response, untaps all creatures of the chosen type
 */
@SerialName("ChooseCreatureTypeUntap")
@Serializable
data object ChooseCreatureTypeUntapEffect : Effect {
    override val description: String =
        "Untap all creatures of the creature type of your choice"
}

/**
 * Grant an activated ability to a group of creatures.
 * "Each creature you control gains '{B}: This creature gets +1/+1 until end of turn.'"
 *
 * Adds GrantedActivatedAbility entries for each matching creature.
 *
 * @property ability The activated ability to grant
 * @property filter Which creatures are affected
 * @property duration How long the grant lasts
 */
@SerialName("GrantActivatedAbilityToGroup")
@Serializable
data class GrantActivatedAbilityToGroupEffect(
    val ability: ActivatedAbility,
    val filter: GroupFilter = GroupFilter.AllCreaturesYouControl,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${filter.description} gain \"${ability.description}\"")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }
}

/**
 * Gain control of all creatures matching a filter until end of turn.
 * "Gain control of all creatures until end of turn."
 *
 * Used by Insurrection and similar mass-control effects.
 *
 * @property filter Which creatures are affected
 * @property duration How long the control change lasts
 */
@SerialName("GainControlOfGroup")
@Serializable
data class GainControlOfGroupEffect(
    val filter: GroupFilter = GroupFilter.AllCreatures,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("Gain control of ${filter.description}")
        if (duration != Duration.Permanent && duration.description.isNotEmpty()) {
            append(" ${duration.description}")
        }
    }
}

/**
 * Choose a creature type. If you control more creatures of that type than each other
 * player, you gain control of all creatures of that type. (This effect lasts indefinitely.)
 *
 * Used by Peer Pressure.
 *
 * At resolution time, the executor:
 * 1. Presents a ChooseOptionDecision with all creature types
 * 2. Pushes a ChooseCreatureTypeGainControlContinuation
 * 3. After the player chooses, counts creatures of that type per player
 * 4. If the controller has strictly more than each other player, gains control of all such creatures
 */
@SerialName("ChooseCreatureTypeGainControl")
@Serializable
data class ChooseCreatureTypeGainControlEffect(
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String =
        "Choose a creature type. If you control more creatures of that type than each other player, you gain control of all creatures of that type"
}

/**
 * Untap all creatures matching a filter.
 * "Untap all creatures."
 *
 * @property filter Which creatures are affected
 */
@SerialName("UntapGroup")
@Serializable
data class UntapGroupEffect(
    val filter: GroupFilter = GroupFilter.AllCreatures
) : Effect {
    override val description: String = "Untap ${filter.description.replaceFirstChar { it.lowercase() }}"
}

/**
 * Target land becomes an X/Y creature until end of turn. It's still a land.
 * Used for Kamahl, Fist of Krosa: "{G}: Target land becomes a 1/1 creature until end of turn. It's still a land."
 *
 * Creates two floating effects:
 * 1. Layer.TYPE + AddType("Creature") - adds the Creature type
 * 2. Layer.POWER_TOUGHNESS + Sublayer.SET_VALUES + SetPowerToughness - sets base P/T
 *
 * @property target The land to animate
 * @property power The base power to set
 * @property toughness The base toughness to set
 * @property duration How long the effect lasts
 */
@SerialName("AnimateLand")
@Serializable
data class AnimateLandEffect(
    val target: EffectTarget,
    val power: Int = 1,
    val toughness: Int = 1,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} becomes a $power/$toughness creature")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
        append(". It's still a land.")
    }
}
