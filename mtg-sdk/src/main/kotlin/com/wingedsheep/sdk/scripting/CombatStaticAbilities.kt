package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
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
 * Creatures can't attack you unless their controller pays a mana cost for each
 * attacking creature. Used for Ghostly Prison, Propaganda, Windborn Muse.
 *
 * Only applies when attacking the controller of this permanent (not their planeswalkers).
 * Multiple AttackTax effects from different permanents stack additively.
 *
 * @property manaCostPerAttacker The mana cost that must be paid per attacking creature (e.g., "{2}")
 */
@SerialName("AttackTax")
@Serializable
data class AttackTax(
    val manaCostPerAttacker: String
) : StaticAbility {
    override val description: String =
        "Creatures can't attack you unless their controller pays $manaCostPerAttacker for each creature they control that's attacking you"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * Creatures without a specified keyword can't attack the controller of this permanent.
 * Used for Form of the Dragon: "Creatures without flying can't attack you."
 *
 * This is a defender-side restriction — the engine checks the defending player's battlefield
 * for permanents with this ability, and blocks any attacker that lacks the required keyword.
 *
 * @property requiredKeyword The keyword attackers must have (e.g., FLYING)
 */
@SerialName("CantBeAttackedWithout")
@Serializable
data class CantBeAttackedWithout(
    val requiredKeyword: Keyword
) : StaticAbility {
    override val description: String =
        "Creatures without ${requiredKeyword.displayName.lowercase()} can't attack you"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}
