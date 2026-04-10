package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
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
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
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
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
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
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Prevents a group of creatures matching a filter from attacking.
 * Used for enchantments like "Creatures can't attack."
 *
 * @property filter The group of creatures that can't attack
 */
@SerialName("CantAttackForCreatureGroup")
@Serializable
data class CantAttackForCreatureGroup(
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = "${filter.description} can't attack"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Forces a group of creatures matching a filter to attack each combat if able.
 * Used for enchantments like Grand Melee: "All creatures attack each combat if able."
 *
 * @property filter The group of creatures that must attack
 */
@SerialName("MustAttackForCreatureGroup")
@Serializable
data class MustAttackForCreatureGroup(
    val filter: GroupFilter
) : StaticAbility {
    override val description: String = "${filter.description} attack each combat if able"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Forces a group of creatures matching a filter to block each combat if able.
 * Used for enchantments like Grand Melee: "All creatures block each combat if able."
 *
 * @property filter The group of creatures that must block
 */
@SerialName("MustBlockForCreatureGroup")
@Serializable
data class MustBlockForCreatureGroup(
    val filter: GroupFilter
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
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
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
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

/**
 * This creature may assign its combat damage as though it weren't blocked.
 * When blocked, the controller chooses whether to assign damage to blockers
 * or to the defending player/planeswalker. Used for Thorn Elemental.
 */
@SerialName("AssignCombatDamageAsUnblocked")
@Serializable
data class AssignCombatDamageAsUnblocked(
    val target: StaticTarget = StaticTarget.SourceCreature
) : StaticAbility {
    override val description: String =
        "You may have this creature assign its combat damage as though it weren't blocked"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
}

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
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
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
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility = this
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
