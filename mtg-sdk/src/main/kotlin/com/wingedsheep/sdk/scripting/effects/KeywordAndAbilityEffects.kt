package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Keyword and Ability Grant Effects
// =============================================================================

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
 * Grant Toxic N to a target until end of turn. Resolves to a `TOXIC_<n>` keyword
 * grant; combat damage reads granted toxic amounts from projected keywords.
 */
@SerialName("GrantToxic")
@Serializable
data class GrantToxicEffect(
    val amount: Int,
    val target: EffectTarget = EffectTarget.ContextTarget(0),
    val duration: Duration = Duration.EndOfTurn
) : Effect {
    override val description: String = buildString {
        append("${target.description} gains toxic $amount")
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
