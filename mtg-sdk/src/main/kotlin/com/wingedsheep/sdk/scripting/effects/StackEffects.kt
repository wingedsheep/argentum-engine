package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount
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
 * Counter target spell unless its controller pays a dynamic generic mana cost.
 * "Counter target spell unless its controller pays {X} for each [something]."
 *
 * The total generic mana cost is determined at resolution by evaluating the DynamicAmount.
 * Uses the same continuation as CounterUnlessPaysEffect.
 */
@SerialName("CounterUnlessDynamicPays")
@Serializable
data class CounterUnlessDynamicPaysEffect(
    val amount: DynamicAmount
) : Effect {
    override val description: String = "Counter target spell unless its controller pays ${amount.description}"
}

/**
 * Counter the spell that triggered this ability (non-targeted).
 * "Counter that spell."
 *
 * Uses context.triggeringEntityId to identify the spell to counter,
 * rather than requiring a target selection. Used for triggered abilities
 * like Decree of Silence: "Whenever an opponent casts a spell, counter that spell."
 */
@SerialName("CounterTriggeringSpell")
@Serializable
data object CounterTriggeringSpellEffect : Effect {
    override val description: String = "Counter that spell"
}

/**
 * Counter target activated or triggered ability.
 * "Counter target activated or triggered ability."
 *
 * The countered ability is removed from the stack without resolving.
 * Unlike countering a spell, the ability doesn't go to any zone since
 * abilities are not cards.
 */
@SerialName("CounterAbility")
@Serializable
data object CounterAbilityEffect : Effect {
    override val description: String = "Counter target activated or triggered ability"
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

/**
 * Change the target of target spell or ability with a single target.
 * "Change the target of target spell or ability with a single target."
 *
 * Unlike ChangeSpellTargetEffect which only redirects creature targets to other creatures,
 * this effect works on any target type (creature, player, etc.) and can target
 * both spells and abilities on the stack.
 *
 * The spell or ability must have exactly one target. If it has zero or multiple targets,
 * the effect does nothing.
 */
@SerialName("ChangeTarget")
@Serializable
data object ChangeTargetEffect : Effect {
    override val description: String = "Change the target of target spell or ability with a single target"
}
