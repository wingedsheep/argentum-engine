package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Grants keywords or ability flags to creatures (e.g., Equipment granting flying).
 *
 * The [keyword] field stores the enum name (e.g., "FLYING", "DOESNT_UNTAP")
 * which the engine uses for string-based keyword checks in projected state.
 */
@SerialName("GrantKeyword")
@Serializable
data class GrantKeyword(
    val keyword: String,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    constructor(keyword: Keyword, target: StaticTarget = StaticTarget.AttachedCreature) :
        this(keyword.name, target)

    override val description: String = "Grants ${keyword.lowercase().replace('_', ' ')}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Removes a keyword from the attached/target creature (continuous static ability).
 * Used for Equipment that causes the equipped creature to lose a keyword.
 * E.g., Starforged Sword: "Equipped creature loses flying."
 */
@SerialName("RemoveKeywordStatic")
@Serializable
data class RemoveKeywordStatic(
    val keyword: String,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    constructor(keyword: Keyword, target: StaticTarget = StaticTarget.AttachedCreature) :
        this(keyword.name, target)

    override val description: String = "Removes ${keyword.lowercase().replace('_', ' ')}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Grants a keyword to a group of creatures (continuous static ability).
 * Used for lord effects like "Other creatures you control have flying" or
 * conditional effects like "Other tapped creatures you control have indestructible."
 */
@SerialName("GrantKeywordToCreatureGroup")
@Serializable
data class GrantKeywordToCreatureGroup(
    val keyword: Keyword,
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = "${filter.description} have ${keyword.name.lowercase().replace('_', ' ')}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants ward (with a configurable cost) to permanents matching a filter.
 * Unlike GrantKeywordToCreatureGroup (which only grants the keyword flag), this also
 * generates a ward triggered ability so ward is mechanically enforced.
 *
 * Examples:
 *   Innkeeper's Talent L2 — "Permanents you control with counters have ward {1}."
 *     → GrantWardToGroup(WardCost.Mana("{1}"), …)
 *   Hexing Squelcher — "Other creatures you control have 'Ward—Pay 2 life.'"
 *     → GrantWardToGroup(WardCost.Life(2), …)
 */
@SerialName("GrantWardToGroup")
@Serializable
data class GrantWardToGroup(
    val cost: com.wingedsheep.sdk.scripting.effects.WardCost,
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = when (cost) {
        is com.wingedsheep.sdk.scripting.effects.WardCost.Mana ->
            "${filter.description} have ward ${cost.manaCost}"
        is com.wingedsheep.sdk.scripting.effects.WardCost.Life ->
            "${filter.description} have \"Ward—Pay ${cost.amount} life.\""
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants a keyword to all creatures that have a specific counter type.
 * Used for Aurification: "Each creature with a gold counter on it has defender."
 * With [controllerOnly] = true, only affects creatures you control (e.g., outlast lords).
 *
 * @property keyword The keyword to grant
 * @property counterType The counter type that creatures must have
 * @property controllerOnly If true, only affects creatures you control
 */
@SerialName("GrantKeywordByCounter")
@Serializable
data class GrantKeywordByCounter(
    val keyword: Keyword,
    val counterType: String,
    val controllerOnly: Boolean = false
) : StaticAbility {
    override val description: String =
        "Each creature ${if (controllerOnly) "you control " else ""}with a $counterType counter on it has ${keyword.name.lowercase().replace('_', ' ')}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}
