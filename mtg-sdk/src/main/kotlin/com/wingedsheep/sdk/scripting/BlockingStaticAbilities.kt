package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This creature can't be blocked.
 * Used for cards with unconditional unblockability or conditional via ConditionalStaticAbility.
 */
@SerialName("CantBeBlocked")
@Serializable
data class CantBeBlocked(
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "This creature can't be blocked."
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This creature can't be blocked by creatures of the specified color(s).
 * Used for cards like Sacred Knight: "can't be blocked by black creatures"
 * or "can't be blocked by black and/or red creatures."
 *
 * @property colors The set of colors that cannot block this creature
 * @property target What this ability applies to
 */
@SerialName("CantBeBlockedByColor")
@Serializable
data class CantBeBlockedByColor(
    val colors: Set<Color>,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = buildString {
        append("can't be blocked by ")
        val colorNames = colors.map { it.displayName.lowercase() }
        when (colorNames.size) {
            1 -> append("${colorNames.first()} creatures")
            2 -> append("${colorNames[0]} and/or ${colorNames[1]} creatures")
            else -> append(colorNames.dropLast(1).joinToString(", ") + ", and/or ${colorNames.last()} creatures")
        }
    }

    /** Convenience constructor for a single color. */
    constructor(color: Color, target: StaticTarget = StaticTarget.SourceCreature)
        : this(setOf(color), target)

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This creature can only block creatures with a specific keyword.
 * Used for cards like Cloud Spirit: "can block only creatures with flying."
 */
@SerialName("CanOnlyBlockCreaturesWithKeyword")
@Serializable
data class CanOnlyBlockCreaturesWithKeyword(
    val keyword: Keyword,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can block only creatures with ${keyword.displayName.lowercase()}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Grants "can't be blocked except by creatures with subtype X" to a group of creatures.
 * Used for Shifting Sliver: "Slivers can't be blocked except by Slivers."
 *
 * @property filter The group of creatures that gain the evasion
 * @property requiredSubtype The subtype that blockers must have
 */
@SerialName("GrantCantBeBlockedExceptBySubtype")
@Serializable
data class GrantCantBeBlockedExceptBySubtype(
    val filter: GroupFilter,
    val requiredSubtype: String
) : StaticAbility {
    override val description: String = "${filter.description} can't be blocked except by ${requiredSubtype}s"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        val newSubtype = replacer.replaceCreatureType(requiredSubtype)
        return if (newFilter !== filter || newSubtype != requiredSubtype) copy(filter = newFilter, requiredSubtype = newSubtype) else this
    }
}

/**
 * This creature can't be blocked by creatures with power X or greater.
 * Used for cards like Fleet-Footed Monk: "can't be blocked by creatures with power 2 or greater."
 *
 * @property minPower The minimum power a creature must have to be excluded from blocking
 * @property target What this ability applies to (typically SourceCreature)
 */
@SerialName("CantBeBlockedByPower")
@Serializable
data class CantBeBlockedByPower(
    val minPower: Int,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked by creatures with power $minPower or greater"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This creature can't be blocked by creatures with power X or less.
 * Used for cards like War-Name Aspirant: "can't be blocked by creatures with power 1 or less."
 *
 * @property maxPower The maximum power a creature can have and still be excluded from blocking
 * @property target What this ability applies to (typically SourceCreature)
 */
@SerialName("CantBeBlockedByPowerOrLess")
@Serializable
data class CantBeBlockedByPowerOrLess(
    val maxPower: Int,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked by creatures with power $maxPower or less"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This creature can't block creatures with power greater than this creature's power.
 * Used for cards like Spitfire Handler.
 *
 * The comparison uses projected power (accounts for buffs/debuffs).
 *
 * @property target What this ability applies to (typically SourceCreature)
 */
@SerialName("CantBlockCreaturesWithGreaterPower")
@Serializable
data class CantBlockCreaturesWithGreaterPower(
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't block creatures with power greater than this creature's power"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This creature can't be blocked by creatures with a specific subtype.
 * Used for Juggernaut: "can't be blocked by Walls."
 *
 * @property subtype The subtype that prevents blocking (e.g., "Wall")
 * @property target What this ability applies to (typically SourceCreature)
 */
@SerialName("CantBeBlockedBySubtype")
@Serializable
data class CantBeBlockedBySubtype(
    val subtype: String,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked by ${subtype}s"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This creature can't be blocked except by creatures with a specific keyword.
 * Used for Flying, Shadow, Horsemanship, etc.
 *
 * @property requiredKeyword The keyword blockers must have
 * @property target What this ability applies to
 */
@SerialName("CantBeBlockedExceptByKeyword")
@Serializable
data class CantBeBlockedExceptByKeyword(
    val requiredKeyword: Keyword,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked except by creatures with ${requiredKeyword.displayName.lowercase()}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This creature can't be blocked by more than N creatures.
 * Used for Charging Rhino: "can't be blocked by more than one creature."
 *
 * @property maxBlockers The maximum number of creatures that can block this creature
 * @property target What this ability applies to
 */
@SerialName("CantBeBlockedByMoreThan")
@Serializable
data class CantBeBlockedByMoreThan(
    val maxBlockers: Int,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked by more than ${
        if (maxBlockers == 1) "one creature" else "$maxBlockers creatures"
    }"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Creatures you control with power or toughness N or less can't be blocked.
 * Used for Tetsuko Umezawa, Fugitive: "Creatures you control with power or
 * toughness 1 or less can't be blocked."
 *
 * This is implemented as a blocking restriction checked at declare blockers,
 * using fully projected power/toughness values. The check scans the attacking
 * player's battlefield for permanents with this ability.
 *
 * @property maxValue The maximum power or toughness value for the evasion
 */
@SerialName("GrantCantBeBlockedToSmallCreatures")
@Serializable
data class GrantCantBeBlockedToSmallCreatures(
    val maxValue: Int
) : StaticAbility {
    override val description: String =
        "Creatures you control with power or toughness $maxValue or less can't be blocked"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This creature can't be blocked if its controller has cast a spell matching
 * the given filter this turn. Used for Relic Runner: "can't be blocked if you've
 * cast a historic spell this turn."
 *
 * The engine tracks spell records per player per turn in
 * `GameState.spellsCastThisTurnByPlayer`. The block evasion rule evaluates the filter
 * against those records.
 *
 * @property spellFilter The filter that cast spells must match to grant unblockability
 * @property target What this ability applies to
 */
@SerialName("CantBeBlockedIfCastSpellType")
@Serializable
data class CantBeBlockedIfCastSpellType(
    val spellFilter: GameObjectFilter,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked if you've cast a ${spellFilter.description} spell this turn"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This creature can block any number of creatures.
 * Used for Ironfist Crusher and similar cards.
 *
 * @property target What this ability applies to
 */
@SerialName("CanBlockAnyNumber")
@Serializable
data class CanBlockAnyNumber(
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can block any number of creatures"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Grants "can block an additional N creatures" to a group of creatures.
 * Used for Brave the Sands and similar cards.
 * Cumulative: multiple instances stack (e.g., two Brave the Sands = block 3 creatures).
 *
 * @property count Number of additional creatures that can be blocked (default 1)
 * @property filter The group of creatures that gain the ability
 */
@SerialName("CanBlockAdditionalForCreatureGroup")
@Serializable
data class CanBlockAdditionalForCreatureGroup(
    val count: Int = 1,
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = "${filter.description} can block an additional $count creature${if (count > 1) "s" else ""} each combat"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This creature can't be blocked unless defending player controls N or more
 * creatures that share a creature type.
 * Used for Graxiplon: "can't be blocked unless defending player controls
 * three or more creatures that share a creature type."
 *
 * @property minSharedCount The minimum number of creatures sharing a type required to allow blocking
 * @property target What this ability applies to
 */
@SerialName("CantBeBlockedUnlessDefenderSharesCreatureType")
@Serializable
data class CantBeBlockedUnlessDefenderSharesCreatureType(
    val minSharedCount: Int,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked unless defending player controls $minSharedCount or more creatures that share a creature type"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}
