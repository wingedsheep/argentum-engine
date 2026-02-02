package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import kotlinx.serialization.Serializable

// =============================================================================
// Permanent Modification Effects
// =============================================================================

/**
 * Tap/Untap target effect.
 * "Tap target creature" or "Untap target creature"
 */
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
 * Grant a keyword to a target until end of turn.
 * "Target creature gains flying until end of turn."
 */
@Serializable
data class GrantKeywordUntilEndOfTurnEffect(
    val keyword: Keyword,
    val target: EffectTarget
) : Effect {
    override val description: String = "${target.description} gains ${keyword.displayName.lowercase()} until end of turn"
}

/**
 * Grant a keyword to multiple creatures until end of turn.
 * Used for cards like Nature's Cloak: "Green creatures you control gain forestwalk until end of turn."
 *
 * @property keyword The keyword to grant
 * @property filter Which creatures are affected
 */
@Serializable
data class GrantKeywordToGroupEffect(
    val keyword: Keyword,
    val filter: CreatureGroupFilter,
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
 */
@Serializable
data class ModifyStatsForGroupEffect(
    val powerModifier: Int,
    val toughnessModifier: Int,
    val filter: CreatureGroupFilter,
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
@Serializable
data class TapAllCreaturesEffect(
    val filter: CreatureGroupFilter = CreatureGroupFilter.All
) : Effect {
    override val description: String = "Tap ${filter.description.replaceFirstChar { it.lowercase() }}"
}

/**
 * Untap all creatures you control.
 * Used for Mobilize: "Untap all creatures you control."
 */
@Serializable
data object UntapAllCreaturesYouControlEffect : Effect {
    override val description: String = "Untap all creatures you control"
}

/**
 * Tap up to X target creatures with a filter.
 * Used for Tidal Surge: "Tap up to three target creatures without flying."
 */
@Serializable
data class TapTargetCreaturesEffect(
    val maxTargets: Int,
    val filter: CreatureTargetFilter = CreatureTargetFilter.Any
) : Effect {
    override val description: String = buildString {
        append("Tap up to $maxTargets target ")
        if (filter != CreatureTargetFilter.Any) {
            append("${filter.description} ")
        }
        append("creature${if (maxTargets > 1) "s" else ""}")
    }
}
