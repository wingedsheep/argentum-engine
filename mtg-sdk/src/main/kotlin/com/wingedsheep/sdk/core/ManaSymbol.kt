package com.wingedsheep.sdk.core

import kotlinx.serialization.Serializable

@Serializable
sealed interface ManaSymbol {
    val cmc: Int

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

    @Serializable
    data object X : ManaSymbol {
        override val cmc: Int = 0
        override fun toString(): String = "{X}"
    }

    /**
     * Hybrid mana symbol - can be paid with either of two colors.
     * Example: {G/U} can be paid with {G} or {U}
     */
    @Serializable
    data class Hybrid(val color1: Color, val color2: Color) : ManaSymbol {
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

        fun generic(amount: Int): ManaSymbol = Generic(amount)
    }
}
