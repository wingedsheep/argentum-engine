package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Grants a triggered ability to a group of creatures (continuous static ability).
 * Used for Slivers and other creatures that share abilities with a group.
 * Example: Hunter Sliver — "All Sliver creatures have provoke."
 *
 * Unlike GrantKeywordToCreatureGroup (which handles keyword display in the layer system),
 * this grants the actual functional triggered ability. The TriggerDetector checks for
 * this static ability on battlefield permanents when detecting triggers.
 *
 * @property ability The triggered ability to grant to matching creatures
 * @property filter The group of creatures that gain the ability
 */
@SerialName("GrantTriggeredAbilityToCreatureGroup")
@Serializable
data class GrantTriggeredAbilityToCreatureGroup(
    val ability: TriggeredAbility,
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = "${filter.description} have ${ability.trigger}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAbility !== ability) copy(filter = newFilter, ability = newAbility) else this
    }
}

/**
 * Grants a triggered ability to the creature this aura/equipment is attached to.
 * E.g., Combat Research granting "Whenever this creature deals combat damage to
 * a player, draw a card" to the enchanted creature.
 *
 * The TriggerAbilityResolver scans for this static ability when computing
 * triggered abilities for the attached creature.
 *
 * @property ability The triggered ability to grant to the attached creature
 */
@SerialName("GrantTriggeredAbilityToAttachedCreature")
@Serializable
data class GrantTriggeredAbilityToAttachedCreature(
    val ability: TriggeredAbility
) : StaticAbility {
    override val description: String = "Enchanted creature has ${ability.trigger}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newAbility !== ability) copy(ability = newAbility) else this
    }
}

/**
 * Grants an activated ability to a group of creatures (continuous static ability).
 * Used for Slivers and other creatures that share activated abilities with a group.
 * Example: Spectral Sliver — "All Sliver creatures have '{2}: This creature gets +1/+1 until end of turn.'"
 *
 * The LegalActionsCalculator scans battlefield permanents for this static ability
 * when computing legal activated abilities for each creature.
 *
 * @property ability The activated ability to grant to matching creatures
 * @property filter The group of creatures that gain the ability
 */
@SerialName("GrantActivatedAbilityToCreatureGroup")
@Serializable
data class GrantActivatedAbilityToCreatureGroup(
    val ability: ActivatedAbility,
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = "${filter.description} have ${ability.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAbility !== ability) copy(filter = newFilter, ability = newAbility) else this
    }
}

/**
 * Grants an activated ability to the creature this aura/equipment is attached to.
 * E.g., Singing Bell Strike granting "{6}: Untap this creature" to the enchanted creature.
 *
 * The LegalActionsCalculator and ActivateAbilityHandler scan for this static ability
 * when computing legal activated abilities for each creature.
 *
 * @property ability The activated ability to grant to the attached creature
 */
@SerialName("GrantActivatedAbilityToAttachedCreature")
@Serializable
data class GrantActivatedAbilityToAttachedCreature(
    val ability: ActivatedAbility
) : StaticAbility {
    override val description: String = "Enchanted creature has ${ability.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newAbility !== ability) copy(ability = newAbility) else this
    }
}
