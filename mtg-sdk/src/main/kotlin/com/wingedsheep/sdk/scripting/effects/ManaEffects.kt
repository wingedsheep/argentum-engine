package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.Serializable

// =============================================================================
// Mana Effects
// =============================================================================

/**
 * Add mana effect.
 * "Add {G}" or "Add {R}{R}"
 */
@Serializable
data class AddManaEffect(
    val color: Color,
    val amount: Int = 1
) : Effect {
    override val description: String = "Add ${"{${color.symbol}}".repeat(amount)}"
}

/**
 * Add colorless mana effect.
 * "Add {C}{C}"
 */
@Serializable
data class AddColorlessManaEffect(
    val amount: Int
) : Effect {
    override val description: String = "Add ${"{C}".repeat(amount)}"
}

/**
 * Add one mana of any color effect.
 * "{T}: Add one mana of any color."
 *
 * The player chooses the color when this ability resolves.
 */
@Serializable
data class AddAnyColorManaEffect(
    val amount: Int = 1
) : Effect {
    override val description: String = if (amount == 1) {
        "Add one mana of any color"
    } else {
        "Add $amount mana of any color"
    }
}

/**
 * Add dynamic mana effect where the amount is determined at resolution time.
 * "Add X mana in any combination of {G} and/or {W}, where X is the number of other creatures you control."
 *
 * @property amountSource What determines the amount of mana to add
 * @property allowedColors The colors of mana that can be produced (player chooses distribution)
 */
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
