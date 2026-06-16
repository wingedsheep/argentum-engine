package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/**
 * Modifies power/toughness on a filtered set of permanents.
 *
 * Use [GroupFilter.attachedCreature] for "Equipped/Enchanted creature gets +X/+Y"
 * (Equipment, Auras), [GroupFilter.source] for "this creature gets +X/+Y", or any
 * battlefield-scoped filter for lord effects like "Other Bird creatures get +1/+1".
 */
@SerialName("ModifyStats")
@Serializable
data class ModifyStats(
    val powerBonus: Int,
    val toughnessBonus: Int,
    val filter: GroupFilter = GroupFilter.attachedCreature()
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
    val filter: GroupFilter,
    val powerBonus: DynamicAmount,
    val toughnessBonus: DynamicAmount
) : StaticAbility {
    override val description: String = buildString {
        append("Creatures get +X/+X where X is ${powerBonus.description}")
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newPower = powerBonus.applyTextReplacement(replacer)
        val newToughness = toughnessBonus.applyTextReplacement(replacer)
        return if (newFilter !== filter || newPower !== powerBonus || newToughness !== toughnessBonus)
            copy(filter = newFilter, powerBonus = newPower, toughnessBonus = newToughness) else this
    }
}

/**
 * Sets the base power and toughness of a group of creatures to dynamic amounts — a
 * characteristic-defining ability (CDA) that recomputes continuously rather than snapshotting
 * the value once at creation time.
 *
 * Used for star/star creatures whose printed power and toughness are each defined by a count,
 * e.g. token CDAs like Beau ("Beau's power and toughness are each equal to the number of lands
 * you control"), Tarmogoyf-style cards, Lhurgoyf, etc.
 *
 * This is a Layer 7b (POWER_TOUGHNESS, SET_VALUES) continuous effect — distinct from
 * [GrantDynamicStatsEffect], which is a Layer 7c *bonus* added on top of an existing base. Use
 * this when the dynamic value *is* the base P/T, so a later base-setting effect (e.g. "becomes
 * a 0/2") overwrites it rather than stacking on top of it.
 *
 * @property power The dynamic value to set base power to (evaluated continuously at projection)
 * @property toughness The dynamic value to set base toughness to
 * @property filter Which creatures are affected (typically [GroupFilter.source] for a CDA on self)
 */
@SerialName("SetBasePowerToughnessDynamicStatic")
@Serializable
data class SetBasePowerToughnessDynamicStatic(
    val power: DynamicAmount,
    val toughness: DynamicAmount,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String =
        "${filter.description} has base power and toughness each equal to ${power.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newPower = power.applyTextReplacement(replacer)
        val newToughness = toughness.applyTextReplacement(replacer)
        return if (newFilter !== filter || newPower !== power || newToughness !== toughness)
            copy(filter = newFilter, power = newPower, toughness = newToughness) else this
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
 * @property filter What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("SetBasePowerToughnessStatic")
@Serializable
data class SetBasePowerToughnessStatic(
    val power: Int,
    val toughness: Int,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "has base power and toughness $power/$toughness"
}
