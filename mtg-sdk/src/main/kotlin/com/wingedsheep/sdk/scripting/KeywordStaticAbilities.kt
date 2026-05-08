package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Grants a keyword (or ability flag) to a filtered set of permanents.
 *
 * Use [GroupFilter.attachedCreature] for Equipment/Auras granting a keyword to the
 * attached creature, [GroupFilter.source] for "this creature has flying", or any
 * battlefield-scoped filter for lord effects ("Other creatures you control have flying").
 *
 * The [keyword] field stores the enum name (e.g., "FLYING", "DOESNT_UNTAP") which the
 * engine uses for string-based keyword checks in projected state.
 */
@SerialName("GrantKeyword")
@Serializable
data class GrantKeyword(
    val keyword: String,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    constructor(keyword: Keyword, filter: GroupFilter = GroupFilter.attachedCreature()) :
        this(keyword.name, filter)

    override val description: String =
        "${filter.description} have ${keyword.lowercase().replace('_', ' ')}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Removes a keyword from the affected permanents (continuous static ability).
 * Used for Equipment that causes the equipped creature to lose a keyword.
 * E.g., Starforged Sword: "Equipped creature loses flying."
 */
@SerialName("RemoveKeywordStatic")
@Serializable
data class RemoveKeywordStatic(
    val keyword: String,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    constructor(keyword: Keyword, filter: GroupFilter = GroupFilter.attachedCreature()) :
        this(keyword.name, filter)

    override val description: String = "Removes ${keyword.lowercase().replace('_', ' ')}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants ward (with a configurable cost) to a filtered set of permanents.
 *
 * Use [GroupFilter.attachedCreature] for Auras/Equipment that grant ward to the
 * attached creature, or any battlefield-scoped filter for lord-style "Other
 * creatures you control have ward {1}" effects.
 *
 * Both the WARD keyword display (via the layer system) and the enforcement
 * trigger (via TriggerAbilityResolver.getWardTriggeredAbilities) are generated
 * by the engine.
 *
 * Examples:
 *   Innkeeper's Talent L2 — "Permanents you control with counters have ward {1}."
 *     → GrantWard(WardCost.Mana("{1}"), GroupFilter(...))
 *   Hexing Squelcher — "Other creatures you control have 'Ward—Pay 2 life.'"
 *     → GrantWard(WardCost.Life(2), GroupFilter.OtherCreaturesYouControl)
 *   Aura granting ward to enchanted creature → GrantWard(cost) (default attachedCreature scope)
 */
@SerialName("GrantWard")
@Serializable
data class GrantWard(
    val cost: WardCost,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = when (cost) {
        is WardCost.Mana -> "${filter.description} have ward ${cost.manaCost}"
        is WardCost.Life -> "${filter.description} have \"Ward—Pay ${cost.amount} life.\""
        is WardCost.Discard -> "${filter.description} have \"Ward—Discard ${cost.description}.\""
        is WardCost.Sacrifice -> "${filter.description} have \"Ward—Sacrifice ${cost.description}.\""
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
