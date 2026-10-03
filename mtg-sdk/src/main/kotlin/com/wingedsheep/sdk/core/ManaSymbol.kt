package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

@Serializable
sealed interface ManaSymbol {
    val cmc: Int

    /**
     * The color(s) this one symbol *is* — the single rule every "count/collect the colored mana
     * symbols" read in the engine goes through (object color CR 202.2, devotion CR 700.5, and
     * "with one or more blue mana symbols in its mana cost").
     *
     * Per CR 107.4e a hybrid symbol is also a colored mana symbol and **is all of its component
     * colors**, so `{U/R}` is both blue and red and a monocolored hybrid `{2/U}` is blue. Per
     * CR 107.4f a Phyrexian symbol is a colored mana symbol of its color, so `{U/P}` is blue, and a
     * hybrid Phyrexian symbol is both of its component colors, so `{R/G/P}` is red and green.
     * Generic (`{2}`), colorless (`{C}`) and `{X}` symbols are no color at all (CR 107.4b/c).
     *
     * A symbol is one symbol however many colors it is: counting *symbols* matching a color set
     * must count the symbol once, which is why this returns a set and callers use `any { }` rather
     * than summing (CR 700.5's "devotion to [color 1] and [color 2]" says the same thing).
     */
    val colors: Set<Color>
        get() = when (this) {
            is Colored -> setOf(color)
            is Hybrid -> setOf(color1, color2)
            is Phyrexian -> setOf(color)
            is HybridPhyrexian -> setOf(color1, color2)
            is MonocolorHybrid -> setOf(color)
            is Generic, Colorless, X, Snow -> emptySet()
        }

    /**
     * The color a "pay this pip with 2 life" choice names this symbol by, or `null` when the symbol
     * can't be paid with life. `{B/P}` is BLACK; hybrid Phyrexian `{R/G/P}` is its first color,
     * RED (CR 107.4f).
     */
    val phyrexianLifeColor: Color?
        get() = when (this) {
            is Phyrexian -> color
            is HybridPhyrexian -> color1
            else -> null
        }

    @Serializable
    data class Colored(val color: Color) : ManaSymbol {
        override val cmc: Int = 1
        override fun toString(): String = "{${color.symbol}}"
    }

    @Serializable
    data class Generic(val amount: Int) : ManaSymbol {
        override val cmc: Int = amount
        override fun toString(): String = "{$amount}"
    }

    @Serializable
    data object Colorless : ManaSymbol {
        override val cmc: Int = 1
        override fun toString(): String = "{C}"
    }

    /**
     * The snow mana symbol `{S}` (CR 107.4h): a one-mana cost payable only with mana of any type
     * produced by a snow source. Snow is neither a color nor a type of mana, so the symbol is no
     * color, and effects that reduce generic mana don't reduce it. Paid by a snow unit in the
     * floating pool or by tapping a snow source; "if {S} was spent" reads `DynamicAmount.SnowManaSpent`.
     */
    @Serializable
    data object Snow : ManaSymbol {
        override val cmc: Int = 1
        override fun toString(): String = "{S}"
    }

    @Serializable
    data object X : ManaSymbol {
        override val cmc: Int = 0
        override fun toString(): String = "{X}"
    }

    /**
     * A symbol payable with one mana of either of two colors — [Hybrid] `{G/U}` and, when paid with
     * mana rather than life, [HybridPhyrexian] `{G/U/P}`. Mana-payment code matches
     * `is Hybrid, is HybridPhyrexian ->` and reads [color1]/[color2] off this shared shape.
     */
    sealed interface HybridPair : ManaSymbol {
        val color1: Color
        val color2: Color
    }

    /**
     * Hybrid mana symbol - can be paid with either of two colors.
     * Example: {G/U} can be paid with {G} or {U}
     */
    @Serializable
    data class Hybrid(override val color1: Color, override val color2: Color) : HybridPair {
        override val cmc: Int = 1
        override fun toString(): String = "{${color1.symbol}/${color2.symbol}}"
    }

    /**
     * Phyrexian mana symbol - can be paid with colored mana or 2 life.
     * Example: {G/P} can be paid with {G} or 2 life
     */
    @Serializable
    data class Phyrexian(val color: Color) : ManaSymbol {
        override val cmc: Int = 1
        override fun toString(): String = "{${color.symbol}/P}"
    }

    /**
     * Hybrid Phyrexian mana symbol — can be paid with one mana of either color or 2 life (CR 107.4f).
     * Example: {R/G/P} can be paid with {R}, {G}, or 2 life.
     *
     * Paid with mana it is exactly a [Hybrid]; paid with life it is exactly a [Phyrexian]. The
     * life choice names the pip by [color1] (see [ManaCost.withPhyrexianPaidByLife]), which is the
     * color a client reads off the front of `{R/G/P}`.
     */
    @Serializable
    data class HybridPhyrexian(override val color1: Color, override val color2: Color) : HybridPair {
        override val cmc: Int = 1
        override fun toString(): String = "{${color1.symbol}/${color2.symbol}/P}"
    }

    /**
     * Monocolored hybrid ("twobrid") mana symbol - can be paid with either [generic]
     * generic mana OR a single mana of [color]. Example: {2/B} can be paid with {2} or {B}.
     *
     * Its mana value is the generic component (the larger of the two options), per the
     * hybrid mana-value rule (CR 202.3f). For printed cards [generic] is always 2, but the
     * field is kept general so future "{N/C}" twobrid pips need no new type.
     */
    @Serializable
    data class MonocolorHybrid(val generic: Int, val color: Color) : ManaSymbol {
        override val cmc: Int = generic
        override fun toString(): String = "{$generic/${color.symbol}}"
    }

    companion object {
        val W = Colored(Color.WHITE)
        val U = Colored(Color.BLUE)
        val B = Colored(Color.BLACK)
        val R = Colored(Color.RED)
        val G = Colored(Color.GREEN)
        val C = Colorless
        val S = Snow

        fun generic(amount: Int): ManaSymbol = Generic(amount)
    }
}
