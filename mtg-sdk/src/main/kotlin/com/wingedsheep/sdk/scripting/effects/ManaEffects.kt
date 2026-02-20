package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Mana Effects
// =============================================================================

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
    val amount: DynamicAmount = DynamicAmount.Fixed(1)
) : Effect {
    constructor(color: Color, amount: Int) : this(color, DynamicAmount.Fixed(amount))

    override val description: String = when (val a = amount) {
        is DynamicAmount.Fixed -> "Add ${"{${color.symbol}}".repeat(a.amount)}"
        else -> "Add {${color.symbol}} for each ${a.description}"
    }
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
    val amount: DynamicAmount
) : Effect {
    constructor(amount: Int) : this(DynamicAmount.Fixed(amount))

    override val description: String = when (val a = amount) {
        is DynamicAmount.Fixed -> "Add ${"{C}".repeat(a.amount)}"
        else -> "Add an amount of {C} equal to ${a.description}"
    }
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
    val amount: DynamicAmount = DynamicAmount.Fixed(1)
) : Effect {
    constructor(amount: Int) : this(DynamicAmount.Fixed(amount))

    override val description: String = when (val a = amount) {
        is DynamicAmount.Fixed -> if (a.amount == 1) {
            "Add one mana of any color"
        } else {
            "Add ${a.amount} mana of any color"
        }
        else -> "Add ${a.description} mana of any color"
    }
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
    val allowedColors: Set<Color>
) : Effect {
    override val description: String = buildString {
        append("Add X mana in any combination of ")
        append(allowedColors.joinToString(" and/or ") { "{${it.symbol}}" })
        append(", where X is ${amountSource.description}")
    }
}


