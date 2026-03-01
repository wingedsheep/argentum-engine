package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
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
}

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
}

/**
 * Modifies power/toughness for a group of creatures (continuous static ability).
 * Used for lord effects like "Other Bird creatures get +1/+1."
 */
@SerialName("ModifyStatsForCreatureGroup")
@Serializable
data class ModifyStatsForCreatureGroup(
    val powerBonus: Int,
    val toughnessBonus: Int,
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = buildString {
        val powerStr = if (powerBonus >= 0) "+$powerBonus" else "$powerBonus"
        val toughStr = if (toughnessBonus >= 0) "+$toughnessBonus" else "$toughnessBonus"
        append("${filter.description} get $powerStr/$toughStr")
    }
}

/**
 * Modifies power/toughness for creatures of the chosen creature type.
 * Used for "As this enters, choose a creature type. Creatures of the chosen type get +X/+X."
 * The chosen type is stored on the permanent via ChosenCreatureTypeComponent and resolved dynamically.
 * Example: Shared Triumph, Door of Destinies
 */
@SerialName("ModifyStatsForChosenCreatureType")
@Serializable
data class ModifyStatsForChosenCreatureType(
    val powerBonus: Int,
    val toughnessBonus: Int
) : StaticAbility {
    override val description: String = buildString {
        val powerStr = if (powerBonus >= 0) "+$powerBonus" else "$powerBonus"
        val toughStr = if (toughnessBonus >= 0) "+$toughnessBonus" else "$toughnessBonus"
        append("Creatures of the chosen type get $powerStr/$toughStr")
    }
}

/**
 * Grants a keyword to creatures of the chosen creature type.
 * Used for "As this enters, choose a creature type. Creatures of the chosen type have [keyword]."
 * The chosen type is stored on the permanent via ChosenCreatureTypeComponent and resolved dynamically.
 * Example: Cover of Darkness (fear)
 */
@SerialName("GrantKeywordForChosenCreatureType")
@Serializable
data class GrantKeywordForChosenCreatureType(
    val keyword: Keyword
) : StaticAbility {
    override val description: String =
        "Creatures of the chosen type have ${keyword.name.lowercase().replace('_', ' ')}"
}

/**
 * Modifies power/toughness (e.g., +2/+2 from an Equipment).
 */
@SerialName("StaticModifyStats")
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
 * You control enchanted permanent.
 * Used for Auras like Annex that steal control of the enchanted permanent.
 */
@SerialName("ControlEnchantedPermanent")
@Serializable
data object ControlEnchantedPermanent : StaticAbility {
    override val description: String = "You control enchanted permanent"
}

/**
 * Global effect that affects multiple permanents.
 */
@SerialName("GlobalEffect")
@Serializable
data class GlobalEffect(
    val effectType: GlobalEffectType,
    val filter: GroupFilter = GroupFilter.AllCreatures
) : StaticAbility {
    override val description: String = effectType.description
}

/**
 * Prevents a creature from attacking.
 * Used for auras like Pacifism or effects that prevent attacking.
 */
@SerialName("CantAttack")
@Serializable
data class CantAttack(
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "${target.toString().lowercase()} can't attack"
}

/**
 * Forces a creature to attack each combat if able.
 * Used for creatures like Goblin Brigand: "This creature attacks each combat if able."
 */
@SerialName("MustAttack")
@Serializable
data class MustAttack(
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "attacks each combat if able"
}

/**
 * Prevents a creature from blocking.
 * Used for cards like Jungle Lion or effects like "Target creature can't block".
 */
@SerialName("CantBlock")
@Serializable
data class CantBlock(
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "${target.toString().lowercase()} can't block"
}

/**
 * Prevents a group of creatures matching a filter from blocking.
 * Used for cards like Frenetic Raptor: "Beasts can't block."
 *
 * @property filter The group of creatures that can't block
 */
@SerialName("CantBlockForCreatureGroup")
@Serializable
data class CantBlockForCreatureGroup(
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = "${filter.description} can't block"
}

/**
 * Creature assigns combat damage equal to its toughness rather than its power.
 * Conditional variant: only when toughness is greater than power.
 * Used for cards like Bark of Doran, Doran the Siege Tower, etc.
 */
@SerialName("AssignDamageEqualToToughness")
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
 * This creature's combat damage may be divided as its controller chooses among
 * the defending player and/or any number of creatures they control.
 * Used for Butcher Orgg.
 *
 * Auto-assigns: lethal to each blocker in order, remainder to defending player.
 */
@SerialName("DivideCombatDamageFreely")
@Serializable
data class DivideCombatDamageFreely(
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String =
        "You may assign this creature's combat damage divided as you choose among defending player and/or any number of creatures they control"
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
    CREATURES_CANT_BLOCK("Creatures can't block"),
    ALL_CREATURES_MUST_ATTACK("All creatures attack each combat if able"),
    ALL_CREATURES_MUST_BLOCK("All creatures block each combat if able")
}

/**
 * Target for static abilities (what the ability affects).
 */
@Serializable
sealed interface StaticTarget {
    @SerialName("AttachedCreature")
    @Serializable
    data object AttachedCreature : StaticTarget

    @SerialName("SourceCreature")
    @Serializable
    data object SourceCreature : StaticTarget

    @SerialName("Controller")
    @Serializable
    data object Controller : StaticTarget

    @SerialName("AllControlledCreatures")
    @Serializable
    data object AllControlledCreatures : StaticTarget

    @SerialName("SpecificCard")
    @Serializable
    data class SpecificCard(val entityId: EntityId) : StaticTarget
}

/**
 * Grants dynamic power/toughness bonus based on a variable amount.
 * Used for effects like "Creatures you control get +X/+X where X is..."
 */
@SerialName("GrantDynamicStats")
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
@SerialName("CantReceiveCounters")
@Serializable
data class CantReceiveCounters(
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "${target.toString().lowercase()} can't have counters put on it"
}

/**
 * Reduces the cost of spells with a matching subtype cast by the controller of this permanent.
 * Used for "Goblin spells you cast cost {1} less" (Goblin Warchief),
 * "Zombie spells you cast cost {1} less" (Undead Warchief),
 * "Dragon spells you cast cost {2} less" (Dragonspeaker Shaman), etc.
 *
 * This is a battlefield-based static ability — the permanent with this ability
 * must be on the battlefield to provide the reduction.
 *
 * @property subtype The creature subtype that spells must have to benefit from the reduction
 * @property amount The amount of generic mana to reduce
 */
@SerialName("ReduceSpellCostBySubtype")
@Serializable
data class ReduceSpellCostBySubtype(
    val subtype: String,
    val amount: Int
) : StaticAbility {
    override val description: String = "$subtype spells you cast cost {$amount} less to cast"
}

/**
 * Reduces the colored mana cost of spells with a matching subtype cast by the controller of this permanent.
 * Unlike ReduceSpellCostBySubtype (which reduces generic mana), this removes specific colored mana symbols.
 * Used for "Cleric spells you cast cost {W}{B} less to cast" (Edgewalker).
 *
 * The manaReduction string specifies which colored symbols to remove (e.g., "{W}{B}").
 * This effect reduces only colored mana, never generic mana.
 *
 * @property subtype The creature subtype that spells must have to benefit from the reduction
 * @property manaReduction The colored mana symbols to remove, as a mana cost string (e.g., "{W}{B}")
 */
@SerialName("ReduceSpellColoredCostBySubtype")
@Serializable
data class ReduceSpellColoredCostBySubtype(
    val subtype: String,
    val manaReduction: String
) : StaticAbility {
    override val description: String = "$subtype spells you cast cost $manaReduction less to cast"
}

/**
 * Reduces the cost of spells matching a filter cast by the controller of this permanent.
 * A general-purpose cost reduction that uses GameObjectFilter's card predicates to match spells.
 *
 * Examples:
 * - "Creature spells with MV 6+ cost {2} less" → ReduceSpellCostByFilter(Creature.manaValueAtLeast(6), 2)
 * - "Red spells cost {1} less" → ReduceSpellCostByFilter(Any.withColor(Color.RED), 1)
 * - "Dragon spells cost {2} less" → ReduceSpellCostByFilter(Any.withSubtype("Dragon"), 2)
 *
 * This is a battlefield-based static ability — the permanent with this ability
 * must be on the battlefield to provide the reduction.
 *
 * @property filter The filter that spells must match to benefit from the reduction (card predicates only)
 * @property amount The amount of generic mana to reduce
 */
@SerialName("ReduceSpellCostByFilter")
@Serializable
data class ReduceSpellCostByFilter(
    val filter: GameObjectFilter,
    val amount: Int
) : StaticAbility {
    override val description: String = "${filter.description} spells you cast cost {$amount} less to cast"
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
@SerialName("SpellCostReduction")
@Serializable
data class SpellCostReduction(
    val reductionSource: CostReductionSource
) : StaticAbility {
    override val description: String = "This spell costs {X} less to cast, where X is ${reductionSource.description}"
}

/**
 * Reduces the cost to cast face-down creature spells (morph).
 * Used for Dream Chisel: "Face-down creature spells you cast cost {1} less to cast."
 *
 * This is a static ability on a permanent that reduces the morph casting cost
 * for its controller. The engine scans battlefield permanents for this ability
 * when calculating face-down spell costs.
 *
 * @property reductionSource How the reduction amount is determined
 */
@SerialName("FaceDownSpellCostReduction")
@Serializable
data class FaceDownSpellCostReduction(
    val reductionSource: CostReductionSource
) : StaticAbility {
    override val description: String = "Face-down creature spells you cast cost {${reductionSource.description}} less to cast"
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
    @SerialName("ColorsAmongPermanentsYouControl")
    @Serializable
    data object ColorsAmongPermanentsYouControl : CostReductionSource {
        override val description: String = "the number of colors among permanents you control"
    }

    /**
     * Reduces cost by a fixed amount.
     */
    @SerialName("Fixed")
    @Serializable
    data class Fixed(val amount: Int) : CostReductionSource {
        override val description: String = "$amount"
    }

    /**
     * Reduces cost by number of creatures you control.
     */
    @SerialName("CreaturesYouControl")
    @Serializable
    data object CreaturesYouControl : CostReductionSource {
        override val description: String = "the number of creatures you control"
    }

    /**
     * Reduces cost by total power of creatures you control.
     * Used for Ghalta, Primal Hunger.
     */
    @SerialName("TotalPowerYouControl")
    @Serializable
    data object TotalPowerYouControl : CostReductionSource {
        override val description: String = "the total power of creatures you control"
    }

    /**
     * Reduces cost by number of artifacts you control.
     * Used for Affinity for artifacts.
     */
    @SerialName("ArtifactsYouControl")
    @Serializable
    data object ArtifactsYouControl : CostReductionSource {
        override val description: String = "the number of artifacts you control"
    }
}

// =============================================================================
// Blocking Restrictions
// =============================================================================

/**
 * Modifies the attached creature's power/toughness based on counters on the source permanent.
 * Used for auras like Withering Hex: "Enchanted creature gets -1/-1 for each plague counter
 * on this Aura."
 *
 * The modification is dynamic — recalculated during state projection based on the current
 * number of counters on the source (the aura itself).
 *
 * @property counterType The counter type to count on the source
 * @property powerModPerCounter Power modification per counter (e.g., -1)
 * @property toughnessModPerCounter Toughness modification per counter (e.g., -1)
 * @property target What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("ModifyStatsByCounterOnSource")
@Serializable
data class ModifyStatsByCounterOnSource(
    val counterType: String,
    val powerModPerCounter: Int,
    val toughnessModPerCounter: Int,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = buildString {
        val powerStr = if (powerModPerCounter >= 0) "+$powerModPerCounter" else "$powerModPerCounter"
        val toughStr = if (toughnessModPerCounter >= 0) "+$toughnessModPerCounter" else "$toughnessModPerCounter"
        append("$powerStr/$toughStr for each $counterType counter on this permanent")
    }
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
@SerialName("ConditionalStaticAbility")
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
}

// =============================================================================
// Protection Abilities
// =============================================================================

/**
 * Grants protection from a color to the target creature.
 * Used for auras like Crown of Awe: "Enchanted creature has protection from black and from red."
 *
 * @property color The color to grant protection from
 * @property target What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("GrantProtection")
@Serializable
data class GrantProtection(
    val color: Color,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = "Grants protection from ${color.displayName.lowercase()}"
}

// =============================================================================
// Counter-Based Static Abilities
// =============================================================================

/**
 * Grants a keyword to all creatures that have a specific counter type.
 * Used for Aurification: "Each creature with a gold counter on it has defender."
 *
 * @property keyword The keyword to grant
 * @property counterType The counter type that creatures must have
 */
@SerialName("GrantKeywordByCounter")
@Serializable
data class GrantKeywordByCounter(
    val keyword: Keyword,
    val counterType: String
) : StaticAbility {
    override val description: String =
        "Each creature with a $counterType counter on it has ${keyword.name.lowercase().replace('_', ' ')}"
}

/**
 * Adds a creature type to all creatures that have a specific counter type.
 * Used for Aurification: "Each creature with a gold counter on it is a Wall."
 *
 * @property creatureType The creature type to add
 * @property counterType The counter type that creatures must have
 */
@SerialName("AddCreatureTypeByCounter")
@Serializable
data class AddCreatureTypeByCounter(
    val creatureType: String,
    val counterType: String
) : StaticAbility {
    override val description: String =
        "Each creature with a $counterType counter on it is a $creatureType in addition to its other creature types"
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
}

// =============================================================================
// Conditional Attack/Block Restrictions
// =============================================================================

/**
 * Conditions for conditional attack/block restrictions.
 * "Opponent" means the defending player for attack restrictions
 * and the attacking player for block restrictions.
 */
@Serializable
sealed interface CombatCondition {
    val description: String

    /**
     * You control more creatures than the opponent.
     * Used for Goblin Goon, Mogg Toady.
     */
    @SerialName("ControlMoreCreatures")
    @Serializable
    data object ControlMoreCreatures : CombatCondition {
        override val description: String = "you control more creatures than"
    }

    /**
     * The opponent controls a land of a specific basic type.
     * Used for Deep-Sea Serpent, Slipstream Eel, etc.
     */
    @SerialName("OpponentControlsLandType")
    @Serializable
    data class OpponentControlsLandType(val landType: String) : CombatCondition {
        override val description: String = "defending player controls ${
            if (landType.first().lowercaseChar() in "aeiou") "an" else "a"
        } $landType"
    }
}

/**
 * This creature can't attack unless a combat condition is met.
 * Checked at attack declaration time when the defending player is known.
 *
 * @property condition The condition that must be met for the creature to attack
 * @property target What this ability applies to
 */
@SerialName("CantAttackUnless")
@Serializable
data class CantAttackUnless(
    val condition: CombatCondition,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't attack unless ${condition.description}"
}

/**
 * This creature can't block unless a combat condition is met.
 * Checked at block declaration time when the attacking player is known.
 *
 * @property condition The condition that must be met for the creature to block
 * @property target What this ability applies to
 */
@SerialName("CantBlockUnless")
@Serializable
data class CantBlockUnless(
    val condition: CombatCondition,
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String = "can't block unless ${condition.description}"
}

// =============================================================================
// Attack Restrictions
// =============================================================================

/**
 * Reduces the cost of face-down creature spells you cast.
 * Used for Dream Chisel: "Face-down creature spells you cast cost {1} less to cast."
 *
 * This is a battlefield-based static ability — the permanent with this ability
 * must be on the battlefield to provide the reduction.
 *
 * @property amount The amount of generic mana to reduce
 */
@SerialName("ReduceFaceDownCastingCost")
@Serializable
data class ReduceFaceDownCastingCost(
    val amount: Int
) : StaticAbility {
    override val description: String = "Face-down creature spells you cast cost {$amount} less to cast"
}

/**
 * Enchanted land becomes a specific basic land type.
 * Used for auras like Sea's Claim: "Enchanted land is an Island."
 * This replaces all existing land subtypes with the specified type (Rule 305.7).
 *
 * @property landType The basic land type to set (e.g., "Island", "Plains")
 */
@SerialName("SetEnchantedLandType")
@Serializable
data class SetEnchantedLandType(
    val landType: String
) : StaticAbility {
    override val description: String = "Enchanted land is ${
        if (landType.first().lowercaseChar() in "aeiou") "an" else "a"
    } $landType"
}

/**
 * You have shroud. (You can't be the target of spells or abilities.)
 * Grants shroud to the permanent's controller (player-level shroud).
 * Used for True Believer.
 */
@SerialName("GrantShroudToController")
@Serializable
data object GrantShroudToController : StaticAbility {
    override val description: String = "You have shroud"
}

/**
 * Whenever enchanted land is tapped for mana, its controller adds additional mana.
 * Used for auras like Elvish Guidance: "Whenever enchanted land is tapped for mana,
 * its controller adds an additional {G} for each Elf on the battlefield."
 *
 * This is a triggered mana ability that resolves immediately (doesn't use the stack).
 * The engine checks for this ability after a mana ability on the enchanted land resolves.
 *
 * @property color The color of additional mana to produce
 * @property amount How much additional mana to produce (evaluated dynamically)
 */
@SerialName("AdditionalManaOnTap")
@Serializable
data class AdditionalManaOnTap(
    val color: Color,
    val amount: DynamicAmount
) : StaticAbility {
    override val description: String = "Whenever enchanted land is tapped for mana, its controller adds additional mana"
}

/**
 * Play with the top card of your library revealed.
 * You may play lands and cast spells from the top of your library.
 * Used for Future Sight.
 */
@SerialName("PlayFromTopOfLibrary")
@Serializable
data object PlayFromTopOfLibrary : StaticAbility {
    override val description: String =
        "Play with the top card of your library revealed. You may play lands and cast spells from the top of your library."
}

/**
 * Modifies power/toughness based on the number of other creatures that share a creature type
 * with the target creature. Used for Alpha Status: "Enchanted creature gets +2/+2 for each
 * other creature on the battlefield that shares a creature type with it."
 *
 * @property powerModPerCreature Power bonus per matching creature (e.g., +2)
 * @property toughnessModPerCreature Toughness bonus per matching creature (e.g., +2)
 * @property target What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("ModifyStatsPerSharedCreatureType")
@Serializable
data class ModifyStatsPerSharedCreatureType(
    val powerModPerCreature: Int,
    val toughnessModPerCreature: Int,
    val target: StaticTarget = StaticTarget.AttachedCreature
) : StaticAbility {
    override val description: String = buildString {
        val powerStr = if (powerModPerCreature >= 0) "+$powerModPerCreature" else "$powerModPerCreature"
        val toughStr = if (toughnessModPerCreature >= 0) "+$toughnessModPerCreature" else "$toughnessModPerCreature"
        append("$powerStr/$toughStr for each other creature that shares a creature type with it")
    }
}

/**
 * Prevents all players from cycling cards.
 * Used for Stabilizer: "Players can't cycle cards."
 *
 * The engine checks for this static ability on any permanent on the battlefield
 * when determining if cycling/typecycling is a legal action.
 */
@SerialName("PreventCycling")
@Serializable
data object PreventCycling : StaticAbility {
    override val description: String = "Players can't cycle cards"
}

/**
 * Reveal the first card the controller draws each turn.
 * Used for Primitive Etchings and similar "reveal as you draw" effects.
 *
 * The engine checks for this static ability during draws. When the controller
 * draws their first card of a turn and this ability is active, the drawn card
 * is revealed (a CardRevealedFromDrawEvent is emitted). Other triggered abilities
 * can then trigger off that reveal event.
 */
@SerialName("RevealFirstDrawEachTurn")
@Serializable
data object RevealFirstDrawEachTurn : StaticAbility {
    override val description: String = "Reveal the first card you draw each turn"
}

/**
 * All morph costs cost more to pay (turning face-down creatures face up).
 * Used for Exiled Doomsayer: "All morph costs cost {2} more."
 *
 * This affects all players' morph (turn face-up) costs globally.
 * The engine scans all battlefield permanents for this ability when calculating
 * the effective cost to turn a face-down creature face up.
 * Does not affect the cost to cast creature spells face down.
 *
 * @property amount The amount of additional generic mana required
 */
@SerialName("IncreaseMorphCost")
@Serializable
data class IncreaseMorphCost(
    val amount: Int
) : StaticAbility {
    override val description: String = "All morph costs cost {$amount} more"
}

/**
 * Increases the cost of spells matching a filter for ALL players.
 * Used for tax effects like Glowrider: "Noncreature spells cost {1} more to cast."
 *
 * This is a global effect — it applies to all players, not just the controller.
 * The engine scans all battlefield permanents for this ability when calculating
 * effective spell costs.
 *
 * @property filter The filter that spells must match to be taxed (card predicates only)
 * @property amount The amount of generic mana to increase
 */
@SerialName("IncreaseSpellCostByFilter")
@Serializable
data class IncreaseSpellCostByFilter(
    val filter: GameObjectFilter,
    val amount: Int
) : StaticAbility {
    override val description: String = "${filter.description} spells cost {$amount} more to cast"
}
