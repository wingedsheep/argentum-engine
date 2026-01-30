package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import kotlinx.serialization.Serializable

/**
 * Represents a player's mana pool.
 * Tracks available mana that can be spent on costs.
 */
@Serializable
data class ManaPool(
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0
) {
    /**
     * Get amount of mana for a specific color.
     */
    fun get(color: Color): Int = when (color) {
        Color.WHITE -> white
        Color.BLUE -> blue
        Color.BLACK -> black
        Color.RED -> red
        Color.GREEN -> green
    }

    /**
     * Total mana in the pool.
     */
    val total: Int get() = white + blue + black + red + green + colorless

    /**
     * Check if the pool is empty.
     */
    fun isEmpty(): Boolean = total == 0

    /**
     * Add mana of a specific color.
     */
    fun add(color: Color, amount: Int = 1): ManaPool = when (color) {
        Color.WHITE -> copy(white = white + amount)
        Color.BLUE -> copy(blue = blue + amount)
        Color.BLACK -> copy(black = black + amount)
        Color.RED -> copy(red = red + amount)
        Color.GREEN -> copy(green = green + amount)
    }

    /**
     * Add colorless mana.
     */
    fun addColorless(amount: Int = 1): ManaPool = copy(colorless = colorless + amount)

    /**
     * Remove mana of a specific color.
     */
    fun spend(color: Color, amount: Int = 1): ManaPool? {
        val current = get(color)
        if (current < amount) return null
        return when (color) {
            Color.WHITE -> copy(white = white - amount)
            Color.BLUE -> copy(blue = blue - amount)
            Color.BLACK -> copy(black = black - amount)
            Color.RED -> copy(red = red - amount)
            Color.GREEN -> copy(green = green - amount)
        }
    }

    /**
     * Remove colorless mana.
     */
    fun spendColorless(amount: Int = 1): ManaPool? {
        if (colorless < amount) return null
        return copy(colorless = colorless - amount)
    }

    /**
     * Check if this pool can pay a mana cost.
     */
    fun canPay(cost: ManaCost): Boolean {
        var remaining = this

        // First, pay colored costs
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    remaining = remaining.spend(symbol.color) ?: return false
                }
                is ManaSymbol.Colorless -> {
                    remaining = remaining.spendColorless() ?: return false
                }
                is ManaSymbol.Generic -> {
                    // Will handle in second pass
                }
                is ManaSymbol.X -> {
                    // X is 0 unless specified otherwise
                }
                is ManaSymbol.Hybrid -> {
                    // Try first color, then second
                    val spent = remaining.spend(symbol.color1)
                        ?: remaining.spend(symbol.color2)
                        ?: return false
                    remaining = spent
                }
                is ManaSymbol.Phyrexian -> {
                    // Can be paid with color or 2 life - for now just check color
                    remaining = remaining.spend(symbol.color) ?: return false
                }
            }
        }

        // Then, pay generic costs with any remaining mana
        val genericAmount = cost.genericAmount
        if (remaining.total < genericAmount) return false

        return true
    }

    /**
     * Pay a mana cost, returning the new pool or null if can't pay.
     */
    fun pay(cost: ManaCost): ManaPool? {
        if (!canPay(cost)) return null

        var remaining = this

        // Pay colored costs
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    remaining = remaining.spend(symbol.color)!!
                }
                is ManaSymbol.Colorless -> {
                    remaining = remaining.spendColorless()!!
                }
                is ManaSymbol.Generic -> {
                    // Will handle separately
                }
                is ManaSymbol.X -> {
                    // Handled by caller
                }
                is ManaSymbol.Hybrid -> {
                    remaining = remaining.spend(symbol.color1)
                        ?: remaining.spend(symbol.color2)!!
                }
                is ManaSymbol.Phyrexian -> {
                    remaining = remaining.spend(symbol.color)!!
                }
            }
        }

        // Pay generic costs - spend colorless first, then any color
        var genericRemaining = cost.genericAmount
        while (genericRemaining > 0 && remaining.colorless > 0) {
            remaining = remaining.spendColorless()!!
            genericRemaining--
        }

        // Spend colored mana for remaining generic
        for (color in Color.entries) {
            while (genericRemaining > 0 && remaining.get(color) > 0) {
                remaining = remaining.spend(color)!!
                genericRemaining--
            }
        }

        return remaining
    }

    /**
     * Result of a partial mana payment.
     */
    data class PartialPaymentResult(
        val newPool: ManaPool,
        val remainingCost: ManaCost,
        val manaSpent: ManaPool
    )

    /**
     * Pay as much of a mana cost as possible from this pool.
     * Returns the new pool, the remaining unpaid cost, and the mana that was spent.
     * This is used for AutoPay to use floating mana before tapping lands.
     */
    fun payPartial(cost: ManaCost): PartialPaymentResult {
        var remaining = this
        val unpaidSymbols = mutableListOf<ManaSymbol>()

        // Track mana spent
        var whiteSpent = 0
        var blueSpent = 0
        var blackSpent = 0
        var redSpent = 0
        var greenSpent = 0
        var colorlessSpent = 0

        // Try to pay colored costs first
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    val spent = remaining.spend(symbol.color)
                    if (spent != null) {
                        remaining = spent
                        when (symbol.color) {
                            Color.WHITE -> whiteSpent++
                            Color.BLUE -> blueSpent++
                            Color.BLACK -> blackSpent++
                            Color.RED -> redSpent++
                            Color.GREEN -> greenSpent++
                        }
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Colorless -> {
                    val spent = remaining.spendColorless()
                    if (spent != null) {
                        remaining = spent
                        colorlessSpent++
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Hybrid -> {
                    val spent = remaining.spend(symbol.color1)
                        ?: remaining.spend(symbol.color2)
                    if (spent != null) {
                        remaining = spent
                        // Track which color was used
                        val usedColor1 = this.get(symbol.color1) > remaining.get(symbol.color1)
                        if (usedColor1) {
                            when (symbol.color1) {
                                Color.WHITE -> whiteSpent++
                                Color.BLUE -> blueSpent++
                                Color.BLACK -> blackSpent++
                                Color.RED -> redSpent++
                                Color.GREEN -> greenSpent++
                            }
                        } else {
                            when (symbol.color2) {
                                Color.WHITE -> whiteSpent++
                                Color.BLUE -> blueSpent++
                                Color.BLACK -> blackSpent++
                                Color.RED -> redSpent++
                                Color.GREEN -> greenSpent++
                            }
                        }
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Phyrexian -> {
                    val spent = remaining.spend(symbol.color)
                    if (spent != null) {
                        remaining = spent
                        when (symbol.color) {
                            Color.WHITE -> whiteSpent++
                            Color.BLUE -> blueSpent++
                            Color.BLACK -> blackSpent++
                            Color.RED -> redSpent++
                            Color.GREEN -> greenSpent++
                        }
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Generic -> {
                    // Add to unpaid for now, will handle below
                    unpaidSymbols.add(symbol)
                }
                is ManaSymbol.X -> {
                    // X is handled by caller
                    unpaidSymbols.add(symbol)
                }
            }
        }

        // Now pay generic costs with remaining mana
        var genericRemaining = unpaidSymbols.filterIsInstance<ManaSymbol.Generic>().sumOf { it.amount }
        unpaidSymbols.removeAll { it is ManaSymbol.Generic }

        // Spend colorless first for generic
        while (genericRemaining > 0 && remaining.colorless > 0) {
            remaining = remaining.spendColorless()!!
            colorlessSpent++
            genericRemaining--
        }

        // Spend colored mana for remaining generic
        for (color in Color.entries) {
            while (genericRemaining > 0 && remaining.get(color) > 0) {
                remaining = remaining.spend(color)!!
                when (color) {
                    Color.WHITE -> whiteSpent++
                    Color.BLUE -> blueSpent++
                    Color.BLACK -> blackSpent++
                    Color.RED -> redSpent++
                    Color.GREEN -> greenSpent++
                }
                genericRemaining--
            }
        }

        // Add remaining generic back to unpaid
        if (genericRemaining > 0) {
            unpaidSymbols.add(ManaSymbol.Generic(genericRemaining))
        }

        return PartialPaymentResult(
            newPool = remaining,
            remainingCost = ManaCost(unpaidSymbols),
            manaSpent = ManaPool(
                white = whiteSpent,
                blue = blueSpent,
                black = blackSpent,
                red = redSpent,
                green = greenSpent,
                colorless = colorlessSpent
            )
        )
    }

    /**
     * Empty the mana pool (at end of phases).
     */
    fun empty(): ManaPool = EMPTY

    companion object {
        val EMPTY = ManaPool()
    }
}
