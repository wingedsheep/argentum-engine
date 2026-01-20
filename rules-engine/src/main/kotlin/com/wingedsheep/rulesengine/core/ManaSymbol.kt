package com.wingedsheep.rulesengine.core

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
