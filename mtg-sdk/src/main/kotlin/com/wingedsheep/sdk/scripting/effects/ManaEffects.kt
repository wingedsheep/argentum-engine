package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Mana Effects
// =============================================================================

/**
 * Non-optional mana payment during resolution.
 * Auto-taps lands and deducts the cost from the controller's mana pool.
 */
@SerialName("PayManaCost")
@Serializable
data class PayManaCostEffect(val cost: ManaCost) : Effect {
    override val description: String = "Pay $cost"
    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Add mana effect.
 * "Add {G}" or "Add {R}{R}" or "Add {R} for each Goblin on the battlefield."
 *
 * Supports both fixed and dynamic amounts via [DynamicAmount].
 */
@SerialName("AddMana")
@Serializable
data class AddManaEffect(
    val color: Color,
    val amount: DynamicAmount = DynamicAmount.Fixed(1),
    val restriction: ManaRestriction? = null
) : Effect {
    constructor(color: Color, amount: Int, restriction: ManaRestriction? = null) : this(color, DynamicAmount.Fixed(amount), restriction)

    override val description: String = buildString {
        append(when (val a = amount) {
            is DynamicAmount.Fixed -> "Add ${"{${color.symbol}}".repeat(a.amount)}"
            else -> "Add {${color.symbol}} for each ${a.description}"
        })
        if (restriction != null) append(". ${restriction.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Add colorless mana effect.
 * "Add {C}{C}" or "Add an amount of {C} equal to..."
 *
 * Supports both fixed and dynamic amounts via [DynamicAmount].
 */
@SerialName("AddColorlessMana")
@Serializable
data class AddColorlessManaEffect(
    val amount: DynamicAmount,
    val restriction: ManaRestriction? = null
) : Effect {
    constructor(amount: Int, restriction: ManaRestriction? = null) : this(DynamicAmount.Fixed(amount), restriction)

    override val description: String = buildString {
        append(when (val a = amount) {
            is DynamicAmount.Fixed -> "Add ${"{C}".repeat(a.amount)}"
            else -> "Add an amount of {C} equal to ${a.description}"
        })
        if (restriction != null) append(". ${restriction.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Add one mana of any color effect.
 * "{T}: Add one mana of any color."
 *
 * The player chooses the color when this ability resolves.
 * Supports both fixed and dynamic amounts via [DynamicAmount].
 */
@SerialName("AddAnyColorMana")
@Serializable
data class AddAnyColorManaEffect(
    val amount: DynamicAmount = DynamicAmount.Fixed(1),
    val restriction: ManaRestriction? = null
) : Effect {
    constructor(amount: Int, restriction: ManaRestriction? = null) : this(DynamicAmount.Fixed(amount), restriction)

    override val description: String = buildString {
        append(when (val a = amount) {
            is DynamicAmount.Fixed -> if (a.amount == 1) {
                "Add one mana of any color"
            } else {
                "Add ${a.amount} mana of any color"
            }
            else -> "Add ${a.description} mana of any color"
        })
        if (restriction != null) append(". ${restriction.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Add dynamic mana effect where the amount is determined at resolution time.
 * "Add X mana in any combination of {G} and/or {W}, where X is the number of other creatures you control."
 *
 * @property amountSource What determines the amount of mana to add
 * @property allowedColors The colors of mana that can be produced (player chooses distribution)
 */
@SerialName("AddDynamicMana")
@Serializable
data class AddDynamicManaEffect(
    val amountSource: DynamicAmount,
    val allowedColors: Set<Color>,
    val restriction: ManaRestriction? = null
) : Effect {
    override val description: String = buildString {
        append("Add X mana in any combination of ")
        append(allowedColors.joinToString(" and/or ") { "{${it.symbol}}" })
        append(", where X is ${amountSource.description}")
        if (restriction != null) append(". ${restriction.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amountSource.applyTextReplacement(replacer)
        return if (newAmount !== amountSource) copy(amountSource = newAmount) else this
    }
}

/**
 * Add one mana of any color among permanents matching a filter that you control.
 * "{T}: Add one mana of any color among legendary creatures and planeswalkers you control."
 *
 * At resolution, the executor examines the controller's battlefield for permanents matching [filter],
 * collects the union of their colors, and lets the player choose one. If no colors are available
 * (no matching permanents, or all are colorless), no mana is produced.
 *
 * @property filter The filter to match permanents whose colors determine the available mana colors
 */
@SerialName("AddManaOfColorAmong")
@Serializable
data class AddManaOfColorAmongEffect(
    val filter: GameObjectFilter,
    val restriction: ManaRestriction? = null
) : Effect {
    override val description: String = buildString {
        append("Add one mana of any color among matching permanents you control")
        if (restriction != null) append(". ${restriction.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
