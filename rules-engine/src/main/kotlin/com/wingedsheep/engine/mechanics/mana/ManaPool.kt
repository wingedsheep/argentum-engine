package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.engine.state.components.player.RestrictedManaEntry
import kotlinx.serialization.Serializable

/**
 * Represents a player's mana pool.
 * Tracks available mana that can be spent on costs.
 */
/**
 * Context about the spell being cast, used to evaluate mana spending restrictions.
 */
data class SpellPaymentContext(
    val isInstantOrSorcery: Boolean = false,
    val isKicked: Boolean = false,
    val isCreature: Boolean = false,
    val manaValue: Int = 0,
    val hasXInCost: Boolean = false,
    val subtypes: Set<String> = emptySet()
)

/**
 * Check whether a mana restriction is satisfied by the spell being cast.
 */
fun ManaRestriction.isSatisfiedBy(context: SpellPaymentContext): Boolean = when (this) {
    is ManaRestriction.InstantOrSorceryOnly -> context.isInstantOrSorcery
    is ManaRestriction.KickedSpellsOnly -> context.isKicked
    is ManaRestriction.CreatureMV4OrXCost -> context.isCreature && (context.manaValue >= 4 || context.hasXInCost)
    is ManaRestriction.SpellsMV4OrGreater -> context.manaValue >= 4
    is ManaRestriction.CreatureSpellsOnly -> context.isCreature
    is ManaRestriction.SubtypeSpellsOrAbilitiesOnly ->
        context.subtypes.any { it.equals(subtype, ignoreCase = true) }
}

@Serializable
data class ManaPool(
    val white: Int = 0,
    val blue: Int = 0,
    val black: Int = 0,
    val red: Int = 0,
    val green: Int = 0,
    val colorless: Int = 0,
    val restrictedMana: List<RestrictedManaEntry> = emptyList()
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
    /**
     * Total unrestricted mana in the pool (does not include restricted mana).
     */
    val total: Int get() = white + blue + black + red + green + colorless

    /**
     * Total mana including restricted mana eligible for a given spell context.
     */
    fun totalForSpell(context: SpellPaymentContext): Int = total + getTotalEligibleRestricted(context)

    /**
     * Check if the pool is empty (including restricted mana).
     */
    fun isEmpty(): Boolean = total == 0 && restrictedMana.isEmpty()

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
     * Add restricted mana to the pool.
     */
    fun addRestricted(color: Color?, amount: Int, restriction: ManaRestriction): ManaPool {
        val entries = (1..amount).map { RestrictedManaEntry(color, restriction) }
        return copy(restrictedMana = restrictedMana + entries)
    }

    /**
     * Spend one unit of restricted mana matching the given color and whose restriction is satisfied by the spell context.
     * Returns null if no matching restricted mana is available.
     */
    fun spendRestricted(color: Color?, context: SpellPaymentContext): ManaPool? {
        val index = restrictedMana.indexOfFirst { it.color == color && it.restriction.isSatisfiedBy(context) }
        if (index == -1) return null
        return copy(restrictedMana = restrictedMana.toMutableList().apply { removeAt(index) })
    }

    /**
     * Count eligible restricted mana of a given color for a spell.
     */
    fun getEligibleRestrictedCount(color: Color?, context: SpellPaymentContext): Int =
        restrictedMana.count { it.color == color && it.restriction.isSatisfiedBy(context) }

    /**
     * Count all eligible restricted mana (any color) for a spell.
     */
    fun getTotalEligibleRestricted(context: SpellPaymentContext): Int =
        restrictedMana.count { it.restriction.isSatisfiedBy(context) }

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
     * When [spellContext] is provided, eligible restricted mana is considered (spent first).
     */
    fun canPay(cost: ManaCost, spellContext: SpellPaymentContext? = null): Boolean {
        var remaining = this

        // First, pay colored costs — try restricted mana first, then unrestricted
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext) ?: return false
                }
                is ManaSymbol.Colorless -> {
                    remaining = remaining.trySpendColorless(spellContext) ?: return false
                }
                is ManaSymbol.Generic -> {
                    // Will handle in second pass
                }
                is ManaSymbol.X -> {
                    // X is 0 unless specified otherwise
                }
                is ManaSymbol.Hybrid -> {
                    val spent = remaining.trySpendColored(symbol.color1, spellContext)
                        ?: remaining.trySpendColored(symbol.color2, spellContext)
                        ?: return false
                    remaining = spent
                }
                is ManaSymbol.Phyrexian -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext) ?: return false
                }
            }
        }

        // Then, pay generic costs with any remaining mana (restricted first, then unrestricted)
        val genericAmount = cost.genericAmount
        val availableForGeneric = if (spellContext != null) {
            remaining.total + remaining.getTotalEligibleRestricted(spellContext)
        } else {
            remaining.total
        }
        if (availableForGeneric < genericAmount) return false

        return true
    }

    /**
     * Try to spend one colored mana, preferring eligible restricted mana first.
     */
    private fun trySpendColored(color: Color, spellContext: SpellPaymentContext?): ManaPool? {
        if (spellContext != null) {
            val fromRestricted = spendRestricted(color, spellContext)
            if (fromRestricted != null) return fromRestricted
        }
        return spend(color)
    }

    /**
     * Try to spend one colorless mana, preferring eligible restricted mana first.
     */
    private fun trySpendColorless(spellContext: SpellPaymentContext?): ManaPool? {
        if (spellContext != null) {
            val fromRestricted = spendRestricted(null, spellContext)
            if (fromRestricted != null) return fromRestricted
        }
        return spendColorless()
    }

    /**
     * Pay a mana cost, returning the new pool or null if can't pay.
     * When [spellContext] is provided, eligible restricted mana is spent first.
     */
    fun pay(cost: ManaCost, spellContext: SpellPaymentContext? = null): ManaPool? {
        if (!canPay(cost, spellContext)) return null

        var remaining = this

        // Pay colored costs — restricted first, then unrestricted
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext)!!
                }
                is ManaSymbol.Colorless -> {
                    remaining = remaining.trySpendColorless(spellContext)!!
                }
                is ManaSymbol.Generic -> {
                    // Will handle separately
                }
                is ManaSymbol.X -> {
                    // Handled by caller
                }
                is ManaSymbol.Hybrid -> {
                    remaining = remaining.trySpendColored(symbol.color1, spellContext)
                        ?: remaining.trySpendColored(symbol.color2, spellContext)!!
                }
                is ManaSymbol.Phyrexian -> {
                    remaining = remaining.trySpendColored(symbol.color, spellContext)!!
                }
            }
        }

        // Pay generic costs - spend eligible restricted first, then colorless, then colored
        var genericRemaining = cost.genericAmount

        // Spend eligible restricted mana for generic costs (any color)
        if (spellContext != null) {
            for (entry in remaining.restrictedMana.toList()) {
                if (genericRemaining <= 0) break
                if (entry.restriction.isSatisfiedBy(spellContext)) {
                    remaining = remaining.spendRestricted(entry.color, spellContext)!!
                    genericRemaining--
                }
            }
        }

        while (genericRemaining > 0 && remaining.colorless > 0) {
            remaining = remaining.spendColorless()!!
            genericRemaining--
        }

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
     * When [spellContext] is provided, eligible restricted mana is spent first.
     */
    fun payPartial(cost: ManaCost, spellContext: SpellPaymentContext? = null): PartialPaymentResult {
        var remaining = this
        val unpaidSymbols = mutableListOf<ManaSymbol>()

        // Track mana spent
        var whiteSpent = 0
        var blueSpent = 0
        var blackSpent = 0
        var redSpent = 0
        var greenSpent = 0
        var colorlessSpent = 0

        fun trackColorSpent(color: Color) {
            when (color) {
                Color.WHITE -> whiteSpent++
                Color.BLUE -> blueSpent++
                Color.BLACK -> blackSpent++
                Color.RED -> redSpent++
                Color.GREEN -> greenSpent++
            }
        }

        // Try to pay colored costs first — restricted mana preferred
        for (symbol in cost.symbols) {
            when (symbol) {
                is ManaSymbol.Colored -> {
                    val spent = remaining.trySpendColored(symbol.color, spellContext)
                    if (spent != null) {
                        remaining = spent
                        trackColorSpent(symbol.color)
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Colorless -> {
                    val spent = remaining.trySpendColorless(spellContext)
                    if (spent != null) {
                        remaining = spent
                        colorlessSpent++
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Hybrid -> {
                    val beforeRemaining = remaining
                    val spent = remaining.trySpendColored(symbol.color1, spellContext)
                        ?: remaining.trySpendColored(symbol.color2, spellContext)
                    if (spent != null) {
                        // Determine which color was used by checking what changed
                        val color1Before = beforeRemaining.get(symbol.color1) +
                            beforeRemaining.restrictedMana.count { it.color == symbol.color1 }
                        val color1After = spent.get(symbol.color1) +
                            spent.restrictedMana.count { it.color == symbol.color1 }
                        if (color1Before > color1After) {
                            trackColorSpent(symbol.color1)
                        } else {
                            trackColorSpent(symbol.color2)
                        }
                        remaining = spent
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Phyrexian -> {
                    val spent = remaining.trySpendColored(symbol.color, spellContext)
                    if (spent != null) {
                        remaining = spent
                        trackColorSpent(symbol.color)
                    } else {
                        unpaidSymbols.add(symbol)
                    }
                }
                is ManaSymbol.Generic -> {
                    unpaidSymbols.add(symbol)
                }
                is ManaSymbol.X -> {
                    unpaidSymbols.add(symbol)
                }
            }
        }

        // Now pay generic costs with remaining mana
        var genericRemaining = unpaidSymbols.filterIsInstance<ManaSymbol.Generic>().sumOf { it.amount }
        unpaidSymbols.removeAll { it is ManaSymbol.Generic }

        // Spend eligible restricted mana for generic costs first
        if (spellContext != null) {
            for (entry in remaining.restrictedMana.toList()) {
                if (genericRemaining <= 0) break
                if (entry.restriction.isSatisfiedBy(spellContext)) {
                    val spent = remaining.spendRestricted(entry.color, spellContext)
                    if (spent != null) {
                        remaining = spent
                        if (entry.color != null) trackColorSpent(entry.color) else colorlessSpent++
                        genericRemaining--
                    }
                }
            }
        }

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
                trackColorSpent(color)
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
