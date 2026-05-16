package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.ManaColorSet
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
 * Add mana of a color the player chooses from a [ManaColorSet] resolved at resolution time.
 *
 * This is the atomic "add mana of a constrained-choice color" primitive — it replaces a
 * family of older monolithic effects (AnyColor, CommanderIdentity, AmongPermanents,
 * LandsCouldProduce, SourceChosenColor) with one effect composed from a [ManaColorSet]
 * and a [DynamicAmount].
 *
 * Resolution:
 *   1. The engine resolves [colorSet] to a concrete `Set<Color>`. If empty, no mana
 *      is produced.
 *   2. If the resolved set has exactly one color, that color is used directly.
 *   3. Otherwise, the engine reads `EffectContext.manaColorChoice` (set by activated
 *      mana abilities when the player picked at activation time) or
 *      `EffectContext.chosenColor` (set by a wrapping `ChooseColorThenEffect`). If
 *      neither is present, the engine pauses for a `ChooseManaColorContinuation`.
 *   4. The chosen color is added to the controller's mana pool [amount] times.
 *
 * Example bindings:
 *   - "Add one mana of any color" → `AddManaOfChoiceEffect(ManaColorSet.AnyColor)`
 *   - Command Tower → `AddManaOfChoiceEffect(ManaColorSet.CommanderIdentity)`
 *   - Mox Amber → `AddManaOfChoiceEffect(ManaColorSet.AmongPermanents(filter))`
 *   - Fellwar Stone → `AddManaOfChoiceEffect(ManaColorSet.LandsCouldProduce(OPPONENTS))`
 *   - Uncharted Haven → `AddManaOfChoiceEffect(ManaColorSet.SourceChosenColor)`
 */
@SerialName("AddManaOfChoice")
@Serializable
data class AddManaOfChoiceEffect(
    val colorSet: ManaColorSet = ManaColorSet.AnyColor,
    val amount: DynamicAmount = DynamicAmount.Fixed(1),
    val restriction: ManaRestriction? = null,
) : Effect {
    constructor(colorSet: ManaColorSet, amount: Int, restriction: ManaRestriction? = null) :
        this(colorSet, DynamicAmount.Fixed(amount), restriction)

    override val description: String = buildString {
        val amountText = when (val a = amount) {
            is DynamicAmount.Fixed -> if (a.amount == 1) "one mana of" else "${a.amount} mana of"
            else -> "${a.description} mana of"
        }
        append("Add $amountText ${colorSet.description}")
        if (restriction != null) append(". ${restriction.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Add dynamic mana effect where the amount is determined at resolution time.
 * "Add X mana in any combination of {G} and/or {W}, where X is the number of other creatures you control."
 *
 * Distinct from [AddManaOfChoiceEffect]: this effect distributes the entire X total
 * across allowed colors per the player's choice (e.g., 2{G} + 1{W} for X=3), rather
 * than producing X copies of a single chosen color.
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
 * Add one mana of any color, restricted to spells (and optionally activated abilities)
 * of the source's chosen subtype, optionally carrying spell riders.
 *
 * Examples:
 *  - Unclaimed Territory: `AddAnyColorManaSpendOnChosenTypeEffect()` —
 *    "Spend this mana only to cast a spell of the chosen type or activate an ability of a
 *    source of the chosen type."
 *  - Cavern of Souls: `AddAnyColorManaSpendOnChosenTypeEffect(creatureOnly = true,
 *    riders = setOf(ManaSpellRider.MakesSpellUncounterable))` —
 *    "Spend this mana only to cast a creature spell of the chosen type, and that spell
 *    can't be countered."
 *
 * At resolution, the executor reads the source's ChosenCreatureTypeComponent,
 * prompts the player to choose a color, and adds restricted mana whose
 * [ManaRestriction] is a freshly-minted [ManaRestriction.SubtypeSpellsOrAbilitiesOnly]
 * carrying that subtype (and [creatureOnly]), with [riders] propagated. If no creature
 * type has been chosen, no mana is produced.
 */
@SerialName("AddAnyColorManaSpendOnChosenType")
@Serializable
data class AddAnyColorManaSpendOnChosenTypeEffect(
    val amount: DynamicAmount = DynamicAmount.Fixed(1),
    val creatureOnly: Boolean = false,
    val riders: Set<ManaSpellRider> = emptySet()
) : Effect {
    constructor(amount: Int) : this(DynamicAmount.Fixed(amount))
    constructor(
        amount: Int,
        creatureOnly: Boolean = false,
        riders: Set<ManaSpellRider> = emptySet()
    ) : this(DynamicAmount.Fixed(amount), creatureOnly, riders)

    override val description: String = buildString {
        append(when (val a = amount) {
            is DynamicAmount.Fixed -> if (a.amount == 1) "Add one mana of any color" else "Add ${a.amount} mana of any color"
            else -> "Add ${a.description} mana of any color"
        })
        if (creatureOnly) {
            append(". Spend this mana only to cast a creature spell of the chosen type")
        } else {
            append(". Spend this mana only to cast a spell of the chosen type or activate an ability of a source of the chosen type")
        }
        for (rider in riders) append(". ${rider.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

/**
 * Add one mana of each color found among permanents matching a filter.
 * "{T}: For each color among permanents you control, add one mana of that color."
 *
 * At resolution, the executor examines the matching permanents, takes the union of their
 * colors (using projected state), and adds one mana of each color in that set (0–5 mana
 * total). No player choice — all colors present produce one mana each simultaneously.
 *
 * @property filter The filter to match permanents whose colors determine which manas are produced
 */
@SerialName("AddOneManaOfEachColorAmong")
@Serializable
data class AddOneManaOfEachColorAmongEffect(
    val filter: GameObjectFilter,
    val restriction: ManaRestriction? = null
) : Effect {
    override val description: String = buildString {
        append("For each color among matching permanents, add one mana of that color")
        if (restriction != null) append(". ${restriction.description}")
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}
