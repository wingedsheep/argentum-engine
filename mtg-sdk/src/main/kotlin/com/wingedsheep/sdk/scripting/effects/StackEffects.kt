package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.ManaCost
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Stack Effects
// =============================================================================

/**
 * Counter target spell.
 * "Counter target spell."
 *
 * The countered spell is removed from the stack and placed in its owner's
 * graveyard without resolving (no effects happen).
 */
@SerialName("CounterSpell")
@Serializable
data object CounterSpellEffect : Effect {
    override val description: String = "Counter target spell"
}

/**
 * Counter target spell that matches a filter.
 * Used for Mystic Denial: "Counter target creature or sorcery spell."
 */
@SerialName("CounterSpellWithFilter")
@Serializable
data class CounterSpellWithFilterEffect(
    val filter: TargetFilter = TargetFilter.SpellOnStack
) : Effect {
    override val description: String = "Counter target ${filter.baseFilter.description} spell"
}

/**
 * Counter target spell unless its controller pays a mana cost.
 * "Counter target spell unless its controller pays {cost}."
 *
 * If the spell's controller can pay the cost, they are given a yes/no choice.
 * If they choose to pay, the mana is deducted and the spell resolves normally.
 * If they cannot pay or choose not to, the spell is countered.
 */
@SerialName("CounterUnlessPays")
@Serializable
data class CounterUnlessPaysEffect(
    val cost: ManaCost
) : Effect {
    override val description: String = "Counter target spell unless its controller pays $cost"
}

/**
 * Change the target of a spell that has exactly one target, and that target is a creature,
 * to another creature.
 * "If target spell has only one target and that target is a creature, change that spell's target to another creature."
 *
 * When [targetMustBeSource] is true, the spell's target must be the source of this effect
 * (e.g., Quicksilver Dragon: "If target spell has only one target and that target is this creature").
 */
@SerialName("ChangeSpellTarget")
@Serializable
data class ChangeSpellTargetEffect(
    val targetMustBeSource: Boolean = false
) : Effect {
    override val description: String = if (targetMustBeSource) {
        "Change target spell's target if it targets this creature"
    } else {
        "Change target spell's target to another creature"
    }
}
