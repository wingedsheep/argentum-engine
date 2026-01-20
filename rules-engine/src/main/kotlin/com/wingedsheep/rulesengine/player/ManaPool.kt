package com.wingedsheep.rulesengine.player

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.ManaSymbol
import kotlinx.serialization.Serializable

@Serializable
data class ManaPool(
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0
) {
    val isEmpty: Boolean
        get() = white == 0 && blue == 0 && black == 0 && red == 0 && green == 0 && colorless == 0

    val total: Int
        get() = white + blue + black + red + green + colorless

    fun get(color: Color): Int = when (color) {
        Color.WHITE -> white
        Color.BLUE -> blue
        Color.BLACK -> black
        Color.RED -> red
        Color.GREEN -> green
    }

    fun add(color: Color, amount: Int = 1): ManaPool = when (color) {
        Color.WHITE -> copy(white = white + amount)
        Color.BLUE -> copy(blue = blue + amount)
        Color.BLACK -> copy(black = black + amount)
        Color.RED -> copy(red = red + amount)
        Color.GREEN -> copy(green = green + amount)
    }

    fun addColorless(amount: Int = 1): ManaPool = copy(colorless = colorless + amount)

    fun spend(color: Color, amount: Int = 1): ManaPool {
        require(get(color) >= amount) { "Not enough ${color.displayName} mana: have ${get(color)}, need $amount" }
        return when (color) {
            Color.WHITE -> copy(white = white - amount)
            Color.BLUE -> copy(blue = blue - amount)
            Color.BLACK -> copy(black = black - amount)
            Color.RED -> copy(red = red - amount)
            Color.GREEN -> copy(green = green - amount)
        }
    }

    fun spendColorless(amount: Int = 1): ManaPool {
        require(colorless >= amount) { "Not enough colorless mana: have $colorless, need $amount" }
        return copy(colorless = colorless - amount)
    }

    fun spendGeneric(amount: Int): ManaPool {
        require(total >= amount) { "Not enough mana: have $total, need $amount" }
        var remaining = amount
        var pool = this

        // Spend colorless first for generic costs
        val colorlessToSpend = minOf(remaining, pool.colorless)
        pool = pool.copy(colorless = pool.colorless - colorlessToSpend)
        remaining -= colorlessToSpend

        // Then spend colored mana (order: WUBRG)
        if (remaining > 0 && pool.white > 0) {
            val toSpend = minOf(remaining, pool.white)
            pool = pool.copy(white = pool.white - toSpend)
            remaining -= toSpend
        }
        if (remaining > 0 && pool.blue > 0) {
            val toSpend = minOf(remaining, pool.blue)
            pool = pool.copy(blue = pool.blue - toSpend)
            remaining -= toSpend
        }
        if (remaining > 0 && pool.black > 0) {
            val toSpend = minOf(remaining, pool.black)
            pool = pool.copy(black = pool.black - toSpend)
            remaining -= toSpend
        }
        if (remaining > 0 && pool.red > 0) {
            val toSpend = minOf(remaining, pool.red)
            pool = pool.copy(red = pool.red - toSpend)
            remaining -= toSpend
        }
        if (remaining > 0 && pool.green > 0) {
            val toSpend = minOf(remaining, pool.green)
            pool = pool.copy(green = pool.green - toSpend)
            remaining -= toSpend
        }

        return pool
    }

    fun canPay(cost: ManaCost): Boolean {
        var availableForGeneric = total

        // Check each colored mana requirement
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    val available = get(symbol.color)
                    val required = cost.colorCount[symbol.color] ?: 0
                    if (available < required) return false
                    availableForGeneric -= required
                }
                is ManaSymbol.Colorless -> {
                    if (colorless < 1) return false
                    availableForGeneric -= 1
                }
                is ManaSymbol.Generic -> {
                    // Will check after colored requirements
                }
                is ManaSymbol.X -> {
                    // X can be 0, so always payable
                }
                is ManaSymbol.Hybrid -> {
                    // Can pay with either color, check if at least one is available
                    val available1 = get(symbol.color1)
                    val available2 = get(symbol.color2)
                    if (available1 < 1 && available2 < 1) return false
                    availableForGeneric -= 1
                }
                is ManaSymbol.Phyrexian -> {
                    // Can always pay (with life as fallback), but prefer mana
                    availableForGeneric -= 1
                }
            }
        }

        // Check if we have enough for generic costs
        // Subtract colored requirements from available
        val coloredRequired = cost.symbols.filterIsInstance<ManaSymbol.Colored>()
            .groupingBy { it.color }
            .eachCount()

        var remaining = total
        for ((color, count) in coloredRequired) {
            remaining -= count
        }
        remaining -= cost.symbols.count { it is ManaSymbol.Colorless }

        return remaining >= cost.genericAmount
    }

    /**
     * Pay a mana cost by spending the required mana from the pool.
     *
     * Spends colored mana first for colored requirements, then uses
     * remaining mana for generic costs (preferring colorless first).
     *
     * @param cost The mana cost to pay
     * @return A new ManaPool with the cost deducted
     * @throws IllegalArgumentException if the cost cannot be paid
     */
    fun pay(cost: ManaCost): ManaPool {
        require(canPay(cost)) { "Cannot pay cost $cost with pool $this" }

        var pool = this

        // Pay colored mana requirements first
        for ((color, count) in cost.colorCount) {
            pool = pool.spend(color, count)
        }

        // Pay colorless requirements (specifically {C} mana)
        val colorlessRequired = cost.symbols.count { it is ManaSymbol.Colorless }
        if (colorlessRequired > 0) {
            pool = pool.spendColorless(colorlessRequired)
        }

        // Pay generic mana requirement
        if (cost.genericAmount > 0) {
            pool = pool.spendGeneric(cost.genericAmount)
        }

        return pool
    }

    fun empty(): ManaPool = EMPTY

    override fun toString(): String = buildString {
        if (white > 0) append("${white}W ")
        if (blue > 0) append("${blue}U ")
        if (black > 0) append("${black}B ")
        if (red > 0) append("${red}R ")
        if (green > 0) append("${green}G ")
        if (colorless > 0) append("${colorless}C")
    }.trim()

    companion object {
        val EMPTY = ManaPool()

        fun of(
            white: Int = 0,
            blue: Int = 0,
            black: Int = 0,
            red: Int = 0,
            green: Int = 0,
            colorless: Int = 0
        ): ManaPool = ManaPool(white, blue, black, red, green, colorless)
    }
}
