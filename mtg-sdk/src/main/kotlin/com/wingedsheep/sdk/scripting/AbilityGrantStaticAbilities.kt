package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Grants a triggered ability to a filtered set of permanents.
 *
 * Use [GroupFilter.attachedCreature] for "enchanted/equipped creature has ..." auras and
 * equipment, [GroupFilter.source] for "this creature has ..." abilities, or any
 * battlefield-scoped filter for lord/sliver-style "all X creatures have ..." effects.
 *
 * Both `TriggerDetector` (for battlefield scope) and `TriggerAbilityResolver` (for
 * Self/AttachedTo scope) consult this static ability when computing triggered
 * abilities to fire.
 *
 * @property ability The triggered ability to grant.
 * @property filter The permanents that gain the ability.
 */
@SerialName("GrantTriggeredAbility")
@Serializable
data class GrantTriggeredAbility(
    val ability: TriggeredAbility,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "${filter.description} have ${ability.trigger}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAbility !== ability) copy(filter = newFilter, ability = newAbility) else this
    }
}

/**
 * Grants an activated ability to a filtered set of permanents.
 *
 * Use [GroupFilter.attachedCreature] for "enchanted/equipped creature has ..." auras
 * and equipment, [GroupFilter.source] for "this creature has ..." abilities, or any
 * battlefield-scoped filter for lord/sliver-style "all X creatures have ..." effects.
 *
 * `LegalActionsCalculator` and `ActivateAbilityHandler` consult this static ability
 * when computing legal activated abilities for each permanent.
 *
 * @property ability The activated ability to grant.
 * @property filter The permanents that gain the ability.
 */
@SerialName("GrantActivatedAbility")
@Serializable
data class GrantActivatedAbility(
    val ability: ActivatedAbility,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "${filter.description} have ${ability.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newAbility = ability.applyTextReplacement(replacer)
        return if (newFilter !== filter || newAbility !== ability) copy(filter = newFilter, ability = newAbility) else this
    }
}
