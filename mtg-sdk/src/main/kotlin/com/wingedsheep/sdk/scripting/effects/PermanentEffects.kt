package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
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
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Modify power/toughness effect.
 * "Target creature gets +X/+Y until end of turn"
 *
 * Supports both fixed and dynamic amounts via [DynamicAmount].
 */
@SerialName("ModifyStats")
@Serializable
data class ModifyStatsEffect(
    val powerModifier: DynamicAmount,
    val toughnessModifier: DynamicAmount,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    constructor(powerModifier: Int, toughnessModifier: Int, target: EffectTarget, duration: Duration = Duration.EndOfTurn) :
        this(DynamicAmount.Fixed(powerModifier), DynamicAmount.Fixed(toughnessModifier), target, duration)

    override val description: String = buildString {
        append("${target.description} gets ")
        val pDesc = powerModifier.let {
            if (it is DynamicAmount.Fixed) {
                if (it.amount >= 0) "+${it.amount}" else "${it.amount}"
            } else {
                it.description
            }
        }
        val tDesc = toughnessModifier.let {
            if (it is DynamicAmount.Fixed) {
                if (it.amount >= 0) "+${it.amount}" else "${it.amount}"
            } else {
                it.description
            }
        }
        append("$pDesc/$tDesc")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun runtimeDescription(resolver: (DynamicAmount) -> Int): String = buildString {
        append("${target.description} gets ")
        fun fmt(v: Int) = if (v >= 0) "+$v" else "$v"
        append("${fmt(resolver(powerModifier))}/${fmt(resolver(toughnessModifier))}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newPower = powerModifier.applyTextReplacement(replacer)
        val newToughness = toughnessModifier.applyTextReplacement(replacer)
        return if (newPower !== powerModifier || newToughness !== toughnessModifier)
            copy(powerModifier = newPower, toughnessModifier = newToughness) else this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Grant a keyword or ability flag to a target until end of turn.
 * "Target creature gains flying until end of turn."
 *
 * The [keyword] field stores the enum name (e.g., "FLYING", "DOESNT_UNTAP")
 * which the engine uses for string-based keyword checks in projected state.
 */
@SerialName("GrantKeyword")
@Serializable
data class GrantKeywordEffect(
    val keyword: String,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    constructor(keyword: Keyword, target: EffectTarget, duration: Duration = Duration.EndOfTurn) :
        this(keyword.name, target, duration)

    override val description: String = buildString {
        append("${target.description} gains ${keyword.lowercase().replace('_', ' ')}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Remove a keyword or ability flag from a target until end of turn.
 * "All other creatures lose flying until end of turn."
 *
 * The [keyword] field stores the enum name (e.g., "FLYING")
 * which the engine uses for string-based keyword checks in projected state.
 */
@SerialName("RemoveKeyword")
@Serializable
data class RemoveKeywordEffect(
    val keyword: String,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    constructor(keyword: Keyword, target: EffectTarget, duration: Duration = Duration.EndOfTurn) :
        this(keyword.name, target, duration)

    override val description: String = buildString {
        append("${target.description} loses ${keyword.lowercase().replace('_', ' ')}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Remove all abilities from a target creature until the specified duration.
 * "Target creature loses all abilities until end of turn."
 *
 * @property target The creature that loses all abilities
 * @property duration How long the effect lasts
 */
@SerialName("RemoveAllAbilities")
@Serializable
data class RemoveAllAbilitiesEffect(
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} loses all abilities")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newType = addCreatureType?.let { replacer.replaceCreatureType(it) }
        return if (newType != addCreatureType) copy(addCreatureType = newType) else this
    }
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

/**
 * Grant a triggered ability to a target until end of turn.
 * "Target creature gains 'When this creature deals combat damage to a player, ...'"
 *
 * @property ability The triggered ability to grant
 * @property target The creature to grant the ability to
 * @property duration How long the grant lasts
 */
@SerialName("GrantTriggeredAbility")
@Serializable
data class GrantTriggeredAbilityEffect(
    val ability: TriggeredAbility,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains \"${ability.description}\"")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newAbility !== ability) copy(ability = newAbility) else this
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
@SerialName("GrantActivatedAbility")
@Serializable
data class GrantActivatedAbilityEffect(
    val ability: ActivatedAbility,
    val target: EffectTarget,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains \"${ability.description}\"")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newAbility !== ability) copy(ability = newAbility) else this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val new = replacer.replaceSubtype(subtype)
        return if (new == subtype) this else copy(subtype = new)
    }
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Gain control of target permanent for the active player (whoever's turn it is).
 * Unlike GainControlEffect which gives control to the ability's controller,
 * this gives control to the current active player.
 *
 * Used by Risky Move: "At the beginning of each player's upkeep, that player
 * gains control of Risky Move."
 */
@SerialName("GainControlByActivePlayer")
@Serializable
data class GainControlByActivePlayerEffect(
    val target: EffectTarget = EffectTarget.Self
) : Effect {
    override val description: String = "that player gains control of ${target.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Choose a creature type. Each creature becomes that type until end of turn.
 *
 * @deprecated Use [com.wingedsheep.sdk.dsl.EffectPatterns.becomeChosenTypeAllCreatures] instead,
 * which composes ChooseOptionEffect → ForEachInGroupEffect → SetCreatureSubtypesEffect pipeline.
 */
@Deprecated("Use EffectPatterns.becomeChosenTypeAllCreatures() pipeline instead")
@SerialName("BecomeChosenTypeAllCreatures")
@Serializable
data class BecomeChosenTypeAllCreaturesEffect(
    val excludedTypes: List<String> = emptyList(),
    val controllerOnly: Boolean = false,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("Choose a creature type")
        if (excludedTypes.isNotEmpty()) append(" other than ${excludedTypes.joinToString(", ")}")
        append(". Each creature ")
        if (controllerOnly) append("you control ")
        append("becomes that type")
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
 * Give control of a permanent to a targeted player.
 * Unlike GainControlEffect (which always gives control to the ability's controller),
 * this effect gives control to a player resolved from a target.
 *
 * Used by Custody Battle: "target opponent gains control of this creature unless you sacrifice a land."
 *
 * @property permanent Which permanent changes control (default: enchanted creature)
 * @property newController Which player gains control (default: first target, expected to be a player)
 */
@SerialName("GiveControlToTargetPlayer")
@Serializable
data class GiveControlToTargetPlayerEffect(
    val permanent: EffectTarget = EffectTarget.EnchantedCreature,
    val newController: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = "target opponent gains control of ${permanent.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Exchange control of two target creatures.
 * "You may exchange control of target creature you control and target creature an opponent controls."
 *
 * Creates two floating effects at Layer.CONTROL:
 * 1. Target A (yours) → opponent gains control
 * 2. Target B (opponent's) → you gain control
 *
 * @property target1 The creature you control (becomes opponent's)
 * @property target2 The creature an opponent controls (becomes yours)
 */
@SerialName("ExchangeControl")
@Serializable
data class ExchangeControlEffect(
    val target1: EffectTarget = EffectTarget.ContextTarget(0),
    val target2: EffectTarget = EffectTarget.ContextTarget(1)
) : Effect {
    override val description: String =
        "Exchange control of ${target1.description} and ${target2.description}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAbility !== ability)
            copy(filter = newFilter, ability = newAbility) else this
    }
}

/**
 * @deprecated Use `EffectPatterns.chooseCreatureTypeGainControl()` pipeline instead.
 * Kept for backward compatibility with serialized data.
 */
@Deprecated("Use EffectPatterns.chooseCreatureTypeGainControl() pipeline instead")
@SerialName("ChooseCreatureTypeGainControl")
@Serializable
data class ChooseCreatureTypeGainControlEffect(
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String =
        "Choose a creature type. If you control more creatures of that type than each other player, you gain control of all creatures of that type"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Distribute any number of counters from this creature onto other creatures.
 * "At the beginning of your upkeep, you may move any number of +1/+1 counters
 * from Forgotten Ancient onto other creatures."
 *
 * At resolution time, the executor:
 * 1. Checks how many counters of the given type are on the source creature
 * 2. Finds all other creatures on the battlefield
 * 3. If 0 counters or no other creatures, does nothing
 * 4. Presents a DistributeDecision with total = counter count, targets = other creatures
 * 5. On response, removes distributed counters from self and adds them per the distribution
 *
 * Does not target — the recipient creatures are chosen at resolution time.
 *
 * @property counterType The type of counter to move (e.g., "+1/+1")
 */
@SerialName("DistributeCountersFromSelf")
@Serializable
data class DistributeCountersFromSelfEffect(
    val counterType: String = "+1/+1"
) : Effect {
    override val description: String =
        "Move any number of $counterType counters from this creature onto other creatures"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Distribute a fixed number of counters among the targets from context.
 * "Distribute N counters among one or more target creatures you control."
 *
 * Distribution is deterministic when totalCounters equals number of targets * minPerTarget.
 * With 1 target, all counters go on it. With multiple targets, counters are divided evenly
 * (remainder goes to the first target).
 *
 * @property totalCounters Total number of counters to distribute
 * @property counterType The type of counter (e.g., "+1/+1")
 * @property minPerTarget Minimum counters each target must receive (per MTG rules, typically 1)
 */
@SerialName("DistributeCountersAmongTargets")
@Serializable
data class DistributeCountersAmongTargetsEffect(
    val totalCounters: Int,
    val counterType: String = "+1/+1",
    val minPerTarget: Int = 1
) : Effect {
    override val description: String =
        "Distribute $totalCounters $counterType counter${if (totalCounters != 1) "s" else ""} among targets"

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Set a creature's base power to a specific value.
 * "{1}{U}: Change this creature's base power to target creature's power."
 *
 * Creates a floating effect at Layer.POWER_TOUGHNESS, Sublayer.SET_VALUES that only
 * overrides the power, leaving toughness unchanged. The effect lasts for the specified duration.
 *
 * @property target The creature whose base power is being set
 * @property power The value to set the base power to (evaluated at resolution time)
 * @property duration How long the effect lasts (typically Permanent for indefinite effects)
 */
@SerialName("SetBasePower")
@Serializable
data class SetBasePowerEffect(
    val target: EffectTarget,
    val power: DynamicAmount,
    val duration: Duration = Duration.Permanent
) : Effect {
    override val description: String = buildString {
        append("Change ${target.description}'s base power to ${power.description}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newPower = power.applyTextReplacement(replacer)
        return if (newPower !== power) copy(power = newPower) else this
    }
}

/**
 * Attach this equipment to a target creature.
 * Detaches from the currently equipped creature (if any) before attaching to the new one.
 * "Attach to target creature you control."
 *
 * @property target The creature to attach to
 */
@SerialName("AttachEquipment")
@Serializable
data class AttachEquipmentEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = "Attach this equipment to ${target.description}"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Target permanent becomes a creature with specified characteristics until end of turn.
 * More general than AnimateLandEffect — can also remove types (e.g., Planeswalker),
 * grant keywords, set subtypes, and change color.
 *
 * Used for Sarkhan, the Dragonspeaker's +1: "becomes a legendary 4/4 red Dragon creature
 * with flying, indestructible, and haste."
 *
 * Creates floating effects across multiple layers:
 * - Layer.TYPE: AddType("CREATURE"), RemoveType for each removeType, SetCreatureSubtypes
 * - Layer.COLOR: ChangeColor if colors specified
 * - Layer.ABILITY: GrantKeyword for each keyword
 * - Layer.POWER_TOUGHNESS + Sublayer.SET_VALUES: SetPowerToughness
 *
 * @property target The permanent to animate
 * @property power The base power to set
 * @property toughness The base toughness to set
 * @property keywords Keywords to grant (e.g., flying, indestructible, haste)
 * @property creatureTypes Creature subtypes to set (e.g., "Dragon")
 * @property removeTypes Types to remove (e.g., "PLANESWALKER")
 * @property colors Colors to set (null = keep existing)
 * @property duration How long the effect lasts
 */
@SerialName("BecomeCreature")
@Serializable
data class BecomeCreatureEffect(
    val target: EffectTarget = EffectTarget.Self,
    val power: Int,
    val toughness: Int,
    val keywords: Set<Keyword> = emptySet(),
    val creatureTypes: Set<String> = emptySet(),
    val removeTypes: Set<String> = emptySet(),
    val colors: Set<String>? = null,
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} becomes a $power/$toughness creature")
        if (creatureTypes.isNotEmpty()) append(" ${creatureTypes.joinToString("/")}")
        if (keywords.isNotEmpty()) append(" with ${keywords.joinToString(", ") { it.name.lowercase() }}")
        if (duration.description.isNotEmpty()) append(" ${duration.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Mark a permanent so that if it would leave the battlefield, it is exiled instead.
 * Used by Kheru Lich Lord, Whip of Erebos, Sneak Attack, and similar reanimation effects.
 *
 * @property target The permanent to mark
 */
@SerialName("GrantExileOnLeave")
@Serializable
data class GrantExileOnLeaveEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String =
        "If ${target.description} would leave the battlefield, exile it instead of putting it anywhere else"

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
