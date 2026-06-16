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
}

/**
 * Non-optional payment of a *dynamically computed* amount of generic mana during resolution —
 * the dynamic, payer-parametric twin of [PayManaCostEffect].
 *
 * [amount] is evaluated at resolution and turned into that many generic mana, which is auto-tapped
 * and deducted from [payer]'s pool. This is the building block for "pay {N} for each X" templating:
 * compose it with a pipeline selection and read the selection size, e.g.
 * `PayDynamicManaCostEffect(DynamicAmount.Multiply(DynamicAmount.VariableReference("chosen_count"), 4),
 * Player.TriggeringPlayer)` for "pay {4} for each creature chosen this way", paid by the player whose
 * upkeep it is. An evaluated amount of 0 pays nothing and succeeds.
 *
 * Unlike [PayManaCostEffect] (which always pays from the resolving ability's controller), [payer] lets
 * a *different* player foot the bill — the common shape for each-player triggers ("that player may
 * pay ..."). When wrapped in a [Gate.MayPay], pair it with a matching `decisionMaker` so the same
 * player is prompted and charged.
 *
 * @property amount Generic mana to pay, computed at resolution.
 * @property payer Who pays. Defaults to the controller ([Player.You]); use [Player.TriggeringPlayer]
 *   etc. for cross-player payments.
 */
@SerialName("PayDynamicManaCost")
@Serializable
data class PayDynamicManaCostEffect(
    val amount: DynamicAmount,
    val payer: com.wingedsheep.sdk.scripting.references.Player =
        com.wingedsheep.sdk.scripting.references.Player.You,
    /**
     * When set, the evaluated [amount] is paid as that many copies of this colored symbol
     * (e.g. `Color.GREEN` → `{G}{G}…`, for "pay {G} for each wind counter" — Cyclone). When null
     * (the default) the amount is paid as generic mana (`{N}`).
     */
    val color: Color? = null
) : Effect {
    override val description: String =
        if (color != null) "Pay {${color.symbol}} for each (${amount.description})"
        else "Pay {${amount.description}}"

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newAmount = amount.applyTextReplacement(replacer)
        return if (newAmount !== amount) copy(amount = newAmount) else this
    }
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
    val restriction: ManaRestriction? = null,
    /**
     * When this mana leaves the pool. [ManaExpiry.END_OF_TURN] (the default) is ordinary
     * mana; [ManaExpiry.END_OF_COMBAT] is firebending-style mana that the pool keeps through
     * combat and discards when combat ends.
     */
    val expiry: ManaExpiry = ManaExpiry.END_OF_TURN
) : Effect {
    constructor(color: Color, amount: Int, restriction: ManaRestriction? = null, expiry: ManaExpiry = ManaExpiry.END_OF_TURN) :
        this(color, DynamicAmount.Fixed(amount), restriction, expiry)

    override val description: String = buildString {
        append(when (val a = amount) {
            is DynamicAmount.Fixed -> "Add ${"{${color.symbol}}".repeat(a.amount)}"
            else -> "Add {${color.symbol}} for each ${a.description}"
        })
        if (restriction != null) append(". ${restriction.description}")
        if (expiry == ManaExpiry.END_OF_COMBAT) {
            append(". Until end of combat, you don't lose this mana as steps and phases end")
        }
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
    /**
     * Side-effects attached to the produced mana. When non-empty, the mana is stored
     * as restricted-mana entries so the rider set is preserved through the pool;
     * if [restriction] is null, [ManaRestriction.AnySpend] is used as a no-op marker
     * (the mana remains spendable on anything).
     */
    val riders: Set<ManaSpellRider> = emptySet(),
) : Effect {
    constructor(colorSet: ManaColorSet, amount: Int, restriction: ManaRestriction? = null) :
        this(colorSet, DynamicAmount.Fixed(amount), restriction)

    override val description: String = buildString {
        val amountText = when (val a = amount) {
            is DynamicAmount.Fixed -> if (a.amount == 1) "one mana of" else "${a.amount} mana of"
            else -> "${a.description} mana of"
        }
        append("Add $amountText ${colorSet.description}")
        if (restriction != null && restriction.description.isNotEmpty()) append(". ${restriction.description}")
        for (rider in riders) append(". ${rider.description}")
    }
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
 * At resolution, the executor reads the source's CastChoicesComponent,
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
}
