package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modifies power/toughness (e.g., +2/+2 from an Equipment).
 */
@SerialName("StaticModifyStats")
@Serializable
data class ModifyStats(
    val powerBonus: Int,
    val toughnessBonus: Int,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = buildString {
        val powerStr = if (powerBonus >= 0) "+$powerBonus" else "$powerBonus"
        val toughStr = if (toughnessBonus >= 0) "+$toughnessBonus" else "$toughnessBonus"
        append("$powerStr/$toughStr")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Modifies power/toughness for a group of creatures (continuous static ability).
 * Used for lord effects like "Other Bird creatures get +1/+1."
 */
@SerialName("ModifyStatsForCreatureGroup")
@Serializable
data class ModifyStatsForCreatureGroup(
    val powerBonus: Int,
    val toughnessBonus: Int,
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = buildString {
        val powerStr = if (powerBonus >= 0) "+$powerBonus" else "$powerBonus"
        val toughStr = if (toughnessBonus >= 0) "+$toughnessBonus" else "$toughnessBonus"
        append("${filter.description} get $powerStr/$toughStr")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Modifies power/toughness for creatures of the chosen creature type.
 * Used for "As this enters, choose a creature type. Creatures of the chosen type get +X/+X."
 * The chosen type is stored on the permanent via ChosenCreatureTypeComponent and resolved dynamically.
 * Example: Shared Triumph, Door of Destinies, Patchwork Banner
 *
 * @param youControlOnly If true, only affects creatures you control (e.g., Patchwork Banner).
 *                       If false, affects all creatures of the chosen type (e.g., Shared Triumph).
 */
@SerialName("ModifyStatsForChosenCreatureType")
@Serializable
data class ModifyStatsForChosenCreatureType(
    val powerBonus: Int,
    val toughnessBonus: Int,
    val youControlOnly: Boolean = false
) : StaticAbility {
    override val description: String = buildString {
        val powerStr = if (powerBonus >= 0) "+$powerBonus" else "$powerBonus"
        val toughStr = if (toughnessBonus >= 0) "+$toughnessBonus" else "$toughnessBonus"
        if (youControlOnly) {
            append("Creatures you control of the chosen type get $powerStr/$toughStr")
        } else {
            append("Creatures of the chosen type get $powerStr/$toughStr")
        }
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Grants dynamic power/toughness bonus based on a variable amount.
 * Used for effects like "Creatures you control get +X/+X where X is..."
 */
@SerialName("GrantDynamicStats")
@Serializable
data class GrantDynamicStatsEffect(
    val target: StaticTarget,
    val powerBonus: DynamicAmount,
    val toughnessBonus: DynamicAmount
) : StaticAbility {
    override val description: String = buildString {
        append("Creatures get +X/+X where X is ${powerBonus.description}")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newPower = powerBonus.applyTextReplacement(replacer)
        val newToughness = toughnessBonus.applyTextReplacement(replacer)
        return if (newPower !== powerBonus || newToughness !== toughnessBonus) copy(powerBonus = newPower, toughnessBonus = newToughness) else this
    }
}

/**
 * Modifies the attached creature's power/toughness based on counters on the source permanent.
 * Used for auras like Withering Hex: "Enchanted creature gets -1/-1 for each plague counter
 * on this Aura."
 *
 * The modification is dynamic — recalculated during state projection based on the current
 * number of counters on the source (the aura itself).
 *
 * @property counterType The counter type to count on the source
 * @property powerModPerCounter Power modification per counter (e.g., -1)
 * @property toughnessModPerCounter Toughness modification per counter (e.g., -1)
 * @property target What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("ModifyStatsByCounterOnSource")
@Serializable
data class ModifyStatsByCounterOnSource(
    val counterType: String,
    val powerModPerCounter: Int,
    val toughnessModPerCounter: Int,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = buildString {
        val powerStr = if (powerModPerCounter >= 0) "+$powerModPerCounter" else "$powerModPerCounter"
        val toughStr = if (toughnessModPerCounter >= 0) "+$toughnessModPerCounter" else "$toughnessModPerCounter"
        append("$powerStr/$toughStr for each $counterType counter on this permanent")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Modifies power/toughness based on the number of other creatures that share a creature type
 * with the target creature. Used for Alpha Status: "Enchanted creature gets +2/+2 for each
 * other creature on the battlefield that shares a creature type with it."
 *
 * @property powerModPerCreature Power bonus per matching creature (e.g., +2)
 * @property toughnessModPerCreature Toughness bonus per matching creature (e.g., +2)
 * @property target What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("ModifyStatsPerSharedCreatureType")
@Serializable
data class ModifyStatsPerSharedCreatureType(
    val powerModPerCreature: Int,
    val toughnessModPerCreature: Int,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = buildString {
        val powerStr = if (powerModPerCreature >= 0) "+$powerModPerCreature" else "$powerModPerCreature"
        val toughStr = if (toughnessModPerCreature >= 0) "+$toughnessModPerCreature" else "$toughnessModPerCreature"
        append("$powerStr/$toughStr for each other creature that shares a creature type with it")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Sets the base power and toughness of the target permanent.
 * Used for Deep Freeze: "Enchanted creature has base power and toughness 0/4."
 * Also used for Turn to Frog, Darksteel Mutation, and similar effects.
 *
 * This is a Layer 7b (POWER_TOUGHNESS, SET_VALUES) continuous effect.
 *
 * @property power The base power to set
 * @property toughness The base toughness to set
 * @property target What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("SetBasePowerToughnessStatic")
@Serializable
data class SetBasePowerToughnessStatic(
    val power: Int,
    val toughness: Int,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "has base power and toughness $power/$toughness"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}
