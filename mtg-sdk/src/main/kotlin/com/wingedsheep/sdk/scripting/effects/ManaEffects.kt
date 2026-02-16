package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Mana Effects
// =============================================================================

/**
 * Add mana effect.
 * "Add {G}" or "Add {R}{R}"
 */
@SerialName("AddMana")
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
@SerialName("AddColorlessMana")
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
@SerialName("AddAnyColorMana")
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

/**
 * Add a dynamic amount of mana of a single specific color.
 * "Add {R} for each Goblin on the battlefield."
 *
 * @property color The color of mana to add
 * @property amountSource What determines the amount of mana to add
 */
@SerialName("AddDynamicColorMana")
@Serializable
data class AddDynamicColorManaEffect(
    val color: Color,
    val amountSource: DynamicAmount
) : Effect {
    override val description: String = buildString {
        append("Add {${color.symbol}} for each ${amountSource.description}")
    }
}

/**
 * Add a dynamic amount of colorless mana.
 * "Add an amount of {C} equal to the number of creatures you control that share a creature type with it."
 *
 * @property amountSource What determines the amount of mana to add
 */
@SerialName("AddDynamicColorlessMana")
@Serializable
data class AddDynamicColorlessManaEffect(
    val amountSource: DynamicAmount
) : Effect {
    override val description: String = "Add an amount of {C} equal to ${amountSource.description}"
}
