package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Static abilities provide continuous effects that don't use the stack.
 * These include effects from enchantments, equipment, and other permanents.
 *
 * Static abilities are data objects - application is handled by the ECS
 * layer system (StateProjector) which calculates the projected game state.
 */
@Serializable
sealed interface StaticAbility {
    val description: String
}

/**
 * Grants keywords to creatures (e.g., Equipment granting flying).
 */
@Serializable
data class GrantKeyword(
    val keyword: Keyword,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "Grants ${keyword.name.lowercase().replace('_', ' ')}"
}

/**
 * Grants a keyword to a group of creatures (continuous static ability).
 * Used for lord effects like "Other creatures you control have flying" or
 * conditional effects like "Other tapped creatures you control have indestructible."
 */
@Serializable
data class GrantKeywordToCreatureGroup(
    val keyword: Keyword,
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = "${filter.description} have ${keyword.name.lowercase().replace('_', ' ')}"
}

/**
 * Modifies power/toughness (e.g., +2/+2 from an Equipment).
 */
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
}

/**
 * Global effect that affects multiple permanents.
 */
@Serializable
data class GlobalEffect(
    val effectType: GlobalEffectType,
    val filter: GroupFilter = GroupFilter.AllCreatures
) : StaticAbility {
    override val description: String = effectType.description
}

/**
 * Prevents a creature from blocking.
 * Used for cards like Jungle Lion or effects like "Target creature can't block".
 */
@Serializable
data class CantBlock(
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "${target.toString().lowercase()} can't block"
}

/**
 * Creature assigns combat damage equal to its toughness rather than its power.
 * Conditional variant: only when toughness is greater than power.
 * Used for cards like Bark of Doran, Doran the Siege Tower, etc.
 */
@Serializable
data class AssignDamageEqualToToughness(
    val target: StaticTarget = StaticTarget.AttachedCreature,
    val onlyWhenToughnessGreaterThanPower: Boolean = true
) : StaticAbility {
    override val description: String = buildString {
        if (onlyWhenToughnessGreaterThanPower) {
            append("As long as equipped creature's toughness is greater than its power, it ")
        } else {
            append("This creature ")
        }
        append("assigns combat damage equal to its toughness rather than its power")
    }
}

/**
 * Types of global effects from enchantments.
 */
@Serializable
enum class GlobalEffectType(val description: String) {
    ALL_CREATURES_GET_PLUS_ONE_PLUS_ONE("All creatures get +1/+1"),
    YOUR_CREATURES_GET_PLUS_ONE_PLUS_ONE("Creatures you control get +1/+1"),
    OPPONENT_CREATURES_GET_MINUS_ONE_MINUS_ONE("Creatures your opponents control get -1/-1"),
    ALL_CREATURES_HAVE_FLYING("All creatures have flying"),
    YOUR_CREATURES_HAVE_VIGILANCE("Creatures you control have vigilance"),
    YOUR_CREATURES_HAVE_LIFELINK("Creatures you control have lifelink"),
    CREATURES_CANT_ATTACK("Creatures can't attack"),
    CREATURES_CANT_BLOCK("Creatures can't block")
}

/**
 * Target for static abilities (what the ability affects).
 */
@Serializable
sealed interface StaticTarget {
    @Serializable
    data object AttachedCreature : StaticTarget

    @Serializable
    data object SourceCreature : StaticTarget

    @Serializable
    data object Controller : StaticTarget

    @Serializable
    data object AllControlledCreatures : StaticTarget

    @Serializable
    data class SpecificCard(val entityId: EntityId) : StaticTarget
}

/**
 * Grants dynamic power/toughness bonus based on a variable amount.
 * Used for effects like "Creatures you control get +X/+X where X is..."
 */
@Serializable
data class GrantDynamicStatsEffect(
    val target: StaticTarget,
    val powerBonus: DynamicAmount,
    val toughnessBonus: DynamicAmount
) : StaticAbility {
    override val description: String = buildString {
        append("Creatures get +X/+X where X is ${powerBonus.description}")
    }
}

/**
 * Prevents a permanent from having counters put on it.
 * Used for Auras like Blossombind.
 */
@Serializable
data class CantReceiveCounters(
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "${target.toString().lowercase()} can't have counters put on it"
}

/**
 * Reduces the cost to cast this spell.
 * Used for Vivid and similar cost-reduction mechanics.
 *
 * Note: This is a static ability that affects casting cost.
 * Full implementation requires cost calculation during spell casting.
 *
 * @property reductionSource How the reduction amount is determined
 */
@Serializable
data class SpellCostReduction(
    val reductionSource: CostReductionSource
) : StaticAbility {
    override val description: String = "This spell costs {X} less to cast, where X is ${reductionSource.description}"
}

/**
 * Sources for cost reduction amounts.
 */
@Serializable
sealed interface CostReductionSource {
    val description: String

    /**
     * Vivid - reduces cost by number of colors among permanents you control.
     */
    @Serializable
    data object ColorsAmongPermanentsYouControl : CostReductionSource {
        override val description: String = "the number of colors among permanents you control"
    }

    /**
     * Reduces cost by a fixed amount.
     */
    @Serializable
    data class Fixed(val amount: Int) : CostReductionSource {
        override val description: String = "$amount"
    }

    /**
     * Reduces cost by number of creatures you control.
     */
    @Serializable
    data object CreaturesYouControl : CostReductionSource {
        override val description: String = "the number of creatures you control"
    }

    /**
     * Reduces cost by total power of creatures you control.
     * Used for Ghalta, Primal Hunger.
     */
    @Serializable
    data object TotalPowerYouControl : CostReductionSource {
        override val description: String = "the total power of creatures you control"
    }

    /**
     * Reduces cost by number of artifacts you control.
     * Used for Affinity for artifacts.
     */
    @Serializable
    data object ArtifactsYouControl : CostReductionSource {
        override val description: String = "the number of artifacts you control"
    }
}

// =============================================================================
// Blocking Restrictions
// =============================================================================

/**
 * This creature can't be blocked by creatures of a specific color.
 * Used for cards like Sacred Knight: "can't be blocked by black creatures."
 */
@Serializable
data class CantBeBlockedByColor(
    val color: Color,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked by ${color.displayName.lowercase()} creatures"
}

/**
 * This creature can't be blocked by creatures of any of the specified colors.
 * Used for cards like Sacred Knight: "can't be blocked by black and/or red creatures."
 *
 * @property colors The set of colors that cannot block this creature
 * @property target What this ability applies to
 */
@Serializable
data class CantBeBlockedByColors(
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
}

/**
 * Limits the maximum number of creatures that can block this creature.
 * Used for cards like Charging Rhino/Stalking Tiger: "can't be blocked by more than one creature."
 */
@Serializable
data class MaxBlockersRestriction(
    val maxBlockers: Int,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked by more than $maxBlockers creature${if (maxBlockers != 1) "s" else ""}"
}

/**
 * This creature can only block creatures with a specific keyword.
 * Used for cards like Cloud Spirit: "can block only creatures with flying."
 */
@Serializable
data class CanOnlyBlockCreaturesWithKeyword(
    val keyword: Keyword,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can block only creatures with ${keyword.displayName.lowercase()}"
}

// =============================================================================
// Conditional Static Abilities
// =============================================================================

/**
 * A static ability that only applies when a condition is met.
 * Used for cards like Karakyk Guardian: "hexproof if it hasn't dealt damage yet"
 *
 * The engine checks the condition during state projection and only applies
 * the underlying ability's effect when the condition is true.
 *
 * @property ability The underlying static ability to apply when condition is met
 * @property condition The condition that must be true for the ability to apply
 */
@Serializable
data class ConditionalStaticAbility(
    val ability: StaticAbility,
    val condition: Condition
) : StaticAbility {
    override val description: String = "${ability.description} ${condition.description}"
}

// =============================================================================
// Evasion Abilities
// =============================================================================

/**
 * This creature can't be blocked by creatures with power X or greater.
 * Used for cards like Fleet-Footed Monk: "can't be blocked by creatures with power 2 or greater."
 *
 * @property minPower The minimum power a creature must have to be excluded from blocking
 * @property target What this ability applies to (typically SourceCreature)
 */
@Serializable
data class CantBeBlockedByPower(
    val minPower: Int,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked by creatures with power $minPower or greater"
}

/**
 * This creature can't be blocked except by creatures with a specific keyword.
 * Used for Flying, Shadow, Horsemanship, etc.
 *
 * @property requiredKeyword The keyword blockers must have
 * @property target What this ability applies to
 */
@Serializable
data class CantBeBlockedExceptByKeyword(
    val requiredKeyword: Keyword,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked except by creatures with ${requiredKeyword.displayName.lowercase()}"
}

/**
 * This creature can't be blocked by more than N creatures.
 * Used for Charging Rhino: "can't be blocked by more than one creature."
 *
 * @property maxBlockers The maximum number of creatures that can block this creature
 * @property target What this ability applies to
 */
@Serializable
data class CantBeBlockedByMoreThan(
    val maxBlockers: Int,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't be blocked by more than ${
        if (maxBlockers == 1) "one creature" else "$maxBlockers creatures"
    }"
}

// =============================================================================
// Attack Restrictions
// =============================================================================

/**
 * This creature can't attack unless defending player controls a land of a specific type.
 * Used for Deep-Sea Serpent: "can't attack unless defending player controls an Island."
 *
 * @property landType The basic land type the defending player must control
 * @property target What this ability applies to
 */
@Serializable
data class CantAttackUnlessDefenderControlsLandType(
    val landType: String,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't attack unless defending player controls ${
        if (landType.first().lowercaseChar() in "aeiou") "an" else "a"
    } $landType"
}
