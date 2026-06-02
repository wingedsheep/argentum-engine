package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Prevents the affected permanents from attacking.
 * Use [GroupFilter.source] for "this creature can't attack", [GroupFilter.attachedCreature]
 * for Pacifism-style auras, or any battlefield filter for "Creatures can't attack" effects.
 */
@SerialName("CantAttack")
@Serializable
data class CantAttack(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "${filter.description} can't attack"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Forces the affected permanents to attack each combat if able.
 * Use [GroupFilter.source] for "this creature attacks each combat", or any battlefield
 * filter for "All creatures attack each combat if able" effects (e.g. Grand Melee).
 */
@SerialName("MustAttack")
@Serializable
data class MustAttack(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "${filter.description} attack each combat if able"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Prevents the affected permanents from blocking.
 * Use [GroupFilter.source] for "this creature can't block", or any battlefield filter
 * for "Beasts can't block" / "Creatures can't block" effects.
 */
@SerialName("CantBlock")
@Serializable
data class CantBlock(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "${filter.description} can't block"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Forces the affected permanents to block each combat if able.
 * Used for enchantments like Grand Melee: "All creatures block each combat if able."
 */
@SerialName("MustBlock")
@Serializable
data class MustBlock(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "${filter.description} block each combat if able"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Creature assigns combat damage equal to its toughness rather than its power.
 * Conditional variant: only when toughness is greater than power.
 * Used for cards like Bark of Doran, Doran the Siege Tower, etc.
 */
@SerialName("AssignDamageEqualToToughness")
@Serializable
data class AssignDamageEqualToToughness(
    val filter: GroupFilter = GroupFilter.attachedCreature(),
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
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Creatures this permanent's controller controls that are tapped to activate a Station
 * ability contribute their toughness (rather than their power), as long as toughness
 * is greater than power.
 *
 * Engine wiring: Station abilities use `DynamicAmount.EntityProperty(TappedAsCost, Power)`
 * for the cost-input formula. While a permanent with this static ability is on the
 * battlefield, the evaluator substitutes toughness for power when the tapped creature's
 * controller matches and toughness > power. The substitution is re-evaluated at
 * resolution time and uses last-known characteristics if the tapped creature has left
 * the battlefield (Rule 112.7a — Tapestry Warden 2025-07-25 rulings). A per-creature
 * filter is not currently supported; the override applies to all of the controller's
 * creatures meeting the toughness > power condition.
 *
 * Used for Tapestry Warden: "Each creature you control with toughness greater than its
 * power stations permanents using its toughness rather than its power."
 */
@SerialName("StationUsingToughness")
@Serializable
data object StationUsingToughness : StaticAbility {
    override val description: String =
        "Each creature you control with toughness greater than its power stations permanents using its toughness rather than its power"
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
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String =
        "You may assign this creature's combat damage divided as you choose among defending player and/or any number of creatures they control"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * This creature may assign its combat damage as though it weren't blocked.
 * When blocked, the controller chooses whether to assign damage to blockers
 * or to the defending player/planeswalker. Used for Thorn Elemental.
 */
@SerialName("AssignCombatDamageAsUnblocked")
@Serializable
data class AssignCombatDamageAsUnblocked(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String =
        "You may have this creature assign its combat damage as though it weren't blocked"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * This creature can't attack unless a condition is met.
 * Checked at attack declaration time when the defending player is known.
 *
 * The condition is evaluated with "you" = the creature's controller and
 * "opponent" = the defending player.
 *
 * @property condition The condition that must be met for the creature to attack
 * @property filter What this ability applies to
 */
@SerialName("CantAttackUnless")
@Serializable
data class CantAttackUnless(
    val condition: Condition,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't attack unless ${condition.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * This creature can't attack unless another creature being declared as an attacker in the
 * same declaration matches [coAttackerFilter]. Used for cards like Scarred Puma
 * ("This creature can't attack unless a black or green creature also attacks").
 *
 * Unlike [CantAttackUnless], this restriction depends on the set of co-attackers rather than
 * on the defending player, so it is checked against the full proposed attacker group at
 * declaration time. The creature itself is never counted as its own co-attacker.
 *
 * @property coAttackerFilter The filter a *different* attacking creature must match.
 * @property filter What this ability applies to.
 */
@SerialName("CantAttackUnlessCoAttacker")
@Serializable
data class CantAttackUnlessCoAttacker(
    val coAttackerFilter: com.wingedsheep.sdk.scripting.GameObjectFilter,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't attack unless ${coAttackerFilter.description} also attacks"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * This creature can't block unless a condition is met.
 * Checked at block declaration time when the attacking player is known.
 *
 * The condition is evaluated with "you" = the creature's controller and
 * "opponent" = the attacking player.
 *
 * @property condition The condition that must be met for the creature to block
 * @property filter What this ability applies to
 */
@SerialName("CantBlockUnless")
@Serializable
data class CantBlockUnless(
    val condition: Condition,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can't block unless ${condition.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Creatures can't attack you unless their controller pays generic mana for each
 * attacking creature. Used for Ghostly Prison, Propaganda, Windborn Muse, and
 * Domain-scaled variants like Collective Restraint.
 *
 * Only applies when attacking the controller of this permanent (not their planeswalkers).
 * Multiple AttackTax effects from different permanents stack additively.
 *
 * The per-attacker amount is a [DynamicAmount] so it can scale with game state
 * (e.g., [com.wingedsheep.sdk.dsl.DynamicAmounts.domain] for "{X} where X is your
 * domain"). Evaluated with the source permanent's controller as "you".
 *
 * @property amountPerAttacker Generic mana to pay per attacking creature.
 */
@SerialName("AttackTax")
@Serializable
data class AttackTax(
    val amountPerAttacker: DynamicAmount
) : StaticAbility {
    override val description: String =
        "Creatures can't attack you unless their controller pays {${amountPerAttacker.description}} for each creature they control that's attacking you"
}

/**
 * This creature can attack as though it didn't have defender, as long as a condition is met.
 * "As long as this creature has a counter on it, it can attack as though it didn't have defender."
 *
 * Checked at attack declaration time. The condition is evaluated with "you" = the creature's
 * controller. The filter defaults to the source creature itself.
 *
 * @property condition The condition under which the defender restriction is bypassed
 * @property filter What this ability applies to
 */
@SerialName("CanAttackDespiteDefender")
@Serializable
data class CanAttackDespiteDefender(
    val condition: Condition,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String = "can attack as though it didn't have defender as long as ${condition.description}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Creatures without a specified keyword can't attack the controller of this permanent.
 * Used for Form of the Dragon: "Creatures without flying can't attack you."
 *
 * This is a defender-side restriction — the engine checks the defending player's battlefield
 * for permanents with this ability, and blocks any attacker that lacks the required keyword.
 *
 * An optional [attackerFilter] narrows which attackers the restriction applies to. When set,
 * only attackers matching the filter (evaluated with this permanent as the predicate source,
 * so chosen-color/subtype predicates resolve against it) are restricted; all others may attack
 * freely. Used for Teferi's Moat: "Creatures of the chosen color without flying can't attack
 * you." When null (the default) the restriction applies to every attacker (Form of the Dragon).
 *
 * @property requiredKeyword The keyword attackers must have (e.g., FLYING)
 * @property attackerFilter Optional filter limiting which attackers are restricted
 */
@SerialName("CantBeAttackedWithout")
@Serializable
data class CantBeAttackedWithout(
    val requiredKeyword: Keyword,
    val attackerFilter: com.wingedsheep.sdk.scripting.GameObjectFilter? = null
) : StaticAbility {
    override val description: String =
        if (attackerFilter == null) {
            "Creatures without ${requiredKeyword.displayName.lowercase()} can't attack you"
        } else {
            "${attackerFilter.description} without ${requiredKeyword.displayName.lowercase()} can't attack you"
        }
}

/**
 * Global cap on how many creatures may attack in a single combat (Dueling Grounds —
 * "No more than one creature can attack each combat").
 *
 * Unlike per-creature restrictions, this constrains the *total* declared attacker set
 * regardless of controller, so it is enforced as a whole-declaration check rather than a
 * per-attacker [AttackRestrictionRule]. While any permanent with this ability is on the
 * battlefield, an attack declaration with more than [maxAttackers] attackers is illegal.
 */
@SerialName("AttackerCountLimit")
@Serializable
data class AttackerCountLimit(
    val maxAttackers: Int
) : StaticAbility {
    override val description: String =
        "No more than $maxAttackers creature${if (maxAttackers == 1) "" else "s"} can attack each combat"
}

/**
 * Global cap on how many creatures may block in a single combat (Dueling Grounds —
 * "No more than one creature can block each combat").
 *
 * Constrains the *total* declared blocker set regardless of controller, so it is enforced as
 * a whole-declaration check rather than a per-blocker rule. While any permanent with this
 * ability is on the battlefield, a block declaration with more than [maxBlockers] blockers is
 * illegal.
 */
@SerialName("BlockerCountLimit")
@Serializable
data class BlockerCountLimit(
    val maxBlockers: Int
) : StaticAbility {
    override val description: String =
        "No more than $maxBlockers creature${if (maxBlockers == 1) "" else "s"} can block each combat"
}
