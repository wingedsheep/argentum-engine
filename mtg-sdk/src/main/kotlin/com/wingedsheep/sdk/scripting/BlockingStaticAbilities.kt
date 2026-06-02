package com.wingedsheep.sdk.scripting

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
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "This creature can't be blocked."
}

/**
 * This creature can't be blocked by creatures matching the filter.
 * Unified type replacing CantBeBlockedByColor, CantBeBlockedByPower,
 * CantBeBlockedByPowerOrLess, and CantBeBlockedBySubtype.
 *
 * Examples:
 * - Can't be blocked by Walls: `CantBeBlockedBy(GameObjectFilter.Creature.withSubtype("Wall"))`
 * - Can't be blocked by creatures with power 2+: `CantBeBlockedBy(GameObjectFilter.Creature.powerAtLeast(2))`
 * - Can't be blocked by black/red creatures: `CantBeBlockedBy(GameObjectFilter.Creature.withAnyColor(BLACK, RED))`
 *
 * @property blockerFilter Filter describing which creatures cannot block this creature
 * @property filter What this ability applies to
 */
@SerialName("CantBeBlockedBy")
@Serializable
data class CantBeBlockedBy(
    val blockerFilter: GameObjectFilter,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked by ${blockerFilter.description}"
}

/**
 * This creature can't be blocked except by creatures matching the filter.
 * Unified type replacing CantBeBlockedExceptByKeyword.
 *
 * Examples:
 * - Can't be blocked except by flyers: `CantBeBlockedExceptBy(GameObjectFilter.Creature.withKeyword(FLYING))`
 *
 * @property blockerFilter Filter describing which creatures CAN block this creature
 * @property filter What this ability applies to
 */
@SerialName("CantBeBlockedExceptBy")
@Serializable
data class CantBeBlockedExceptBy(
    val blockerFilter: GameObjectFilter,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked except by ${blockerFilter.description}"
}

/**
 * This creature can only block creatures matching a filter.
 * Generalized "can block only X creatures" — used for the Spirit token from Realm of Koh
 * ("can't block or be blocked by non-Spirit creatures") and any analogous restriction.
 *
 * Examples:
 * - Spirits only: `CanOnlyBlockCreaturesWith(GameObjectFilter.Creature.withSubtype("Spirit"))`
 * - Power 3 or less: `CanOnlyBlockCreaturesWith(GameObjectFilter.Creature.powerAtMost(3))`
 *
 * Applied via projected state, so it works on tokens as well as registered cards.
 *
 * @property blockerFilter Filter describing which creatures this creature is allowed to block
 * @property filter The group of creatures this restriction applies to (default: source)
 */
@SerialName("CanOnlyBlockCreaturesWith")
@Serializable
data class CanOnlyBlockCreaturesWith(
    val blockerFilter: GameObjectFilter,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can block only ${blockerFilter.description}"
}


/**
 * This creature can't block creatures with power greater than this creature's power.
 * Used for cards like Spitfire Handler.
 *
 * The comparison uses projected power (accounts for buffs/debuffs).
 *
 * @property filter What this ability applies to (typically SourceCreature)
 */
@SerialName("CantBlockCreaturesWithGreaterPower")
@Serializable
data class CantBlockCreaturesWithGreaterPower(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't block creatures with power greater than this creature's power"
}

/**
 * This creature can't be blocked by creatures with power less than this creature's power.
 * Used for cards like Formation Breaker. The attacker-side dual of
 * [CantBlockCreaturesWithGreaterPower].
 *
 * The comparison uses projected power for both the attacker (source) and each potential
 * blocker, so it accounts for buffs/debuffs that change power.
 *
 * @property filter What this ability applies to (typically [GroupFilter.source])
 */
@SerialName("CantBeBlockedByCreaturesWithLessPower")
@Serializable
data class CantBeBlockedByCreaturesWithLessPower(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked by creatures with power less than this creature's power"
}

/**
 * This creature can't be blocked by more than N creatures.
 * Used for Charging Rhino: "can't be blocked by more than one creature."
 *
 * @property maxBlockers The maximum number of creatures that can block this creature
 * @property filter What this ability applies to
 */
@SerialName("CantBeBlockedByMoreThan")
@Serializable
data class CantBeBlockedByMoreThan(
    val maxBlockers: Int,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked by more than ${
        if (maxBlockers == 1) "one creature" else "$maxBlockers creatures"
    }"
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
 * @property filter What this ability applies to
 */
@SerialName("CantBeBlockedIfCastSpellType")
@Serializable
data class CantBeBlockedIfCastSpellType(
    val spellFilter: GameObjectFilter,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked if you've cast a ${spellFilter.description} spell this turn"
}

/**
 * This creature can block any number of creatures.
 * Used for Ironfist Crusher and similar cards.
 *
 * @property filter What this ability applies to
 */
@SerialName("CanBlockAnyNumber")
@Serializable
data class CanBlockAnyNumber(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can block any number of creatures"
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
}

/**
 * This creature can't be blocked unless defending player controls N or more
 * creatures that share a creature type.
 * Used for Graxiplon: "can't be blocked unless defending player controls
 * three or more creatures that share a creature type."
 *
 * @property minSharedCount The minimum number of creatures sharing a type required to allow blocking
 * @property filter What this ability applies to
 */
@SerialName("CantBeBlockedUnlessDefenderSharesCreatureType")
@Serializable
data class CantBeBlockedUnlessDefenderSharesCreatureType(
    val minSharedCount: Int,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't be blocked unless defending player controls $minSharedCount or more creatures that share a creature type"
}
