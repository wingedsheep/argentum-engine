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
 * Sets the base toughness of a group of creatures.
 * Used for Maha, Its Feathers Night: "Creatures your opponents control have base toughness 1."
 *
 * This is a Layer 7b (POWER_TOUGHNESS, SET_VALUES) continuous effect that only sets toughness,
 * leaving power unchanged.
 *
 * @property toughness The base toughness to set
 * @property filter Which creatures are affected
 */
@SerialName("SetBaseToughnessForCreatureGroup")
@Serializable
data class SetBaseToughnessForCreatureGroup(
    val toughness: Int,
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = "${filter.description} have base toughness $toughness"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
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
