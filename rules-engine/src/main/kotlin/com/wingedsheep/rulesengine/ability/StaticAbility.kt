package com.wingedsheep.rulesengine.ability

import com.wingedsheep.rulesengine.core.Keyword
import com.wingedsheep.rulesengine.ecs.EntityId
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
    val filter: CreatureFilter = CreatureFilter.All
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
 * Filter for which creatures are affected by a static ability.
 *
 * Filters are pure data - evaluation is handled by the ECS layer system
 * (ScriptModifierProvider converts these to ModifierFilter for the layer engine).
 */
@Serializable
sealed interface CreatureFilter {
    @Serializable
    data object All : CreatureFilter

    @Serializable
    data object YouControl : CreatureFilter

    @Serializable
    data object OpponentsControl : CreatureFilter

    @Serializable
    data class WithKeyword(val keyword: Keyword) : CreatureFilter

    @Serializable
    data class WithoutKeyword(val keyword: Keyword) : CreatureFilter
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
}
