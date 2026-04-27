package com.wingedsheep.sdk.core

import com.wingedsheep.sdk.serialization.ManaCostStringSerializer
import kotlinx.serialization.Serializable

@Serializable(with = ManaCostStringSerializer::class)
data class ManaCost(val symbols: List<ManaSymbol>) {

    val cmc: Int
        get() = symbols.sumOf { it.cmc }

    val colors: Set<Color>
        get() = symbols.flatMap { symbol ->
            when (symbol) {
                is ManaSymbol.Colored -> listOf(symbol.color)
                is ManaSymbol.Hybrid -> listOf(symbol.color1, symbol.color2)
                is ManaSymbol.Phyrexian -> listOf(symbol.color)
                else -> emptyList()
            }
        }.toSet()

    val colorCount: Map<Color, Int>
        get() = symbols
            .filterIsInstance<ManaSymbol.Colored>()
            .groupingBy { it.color }
            .eachCount()

    val genericAmount: Int
        get() = symbols
            .filterIsInstance<ManaSymbol.Generic>()
            .sumOf { it.amount }

    val colorlessAmount: Int
        get() = symbols.count { it is ManaSymbol.Colorless }

    val hasX: Boolean
        get() = symbols.any { it is ManaSymbol.X }

    val xCount: Int
        get() = symbols.count { it is ManaSymbol.X }

    fun isEmpty(): Boolean = symbols.isEmpty()

    /**
     * Reduce the generic mana portion of this cost by [amount].
     * Colored/hybrid/phyrexian symbols are unaffected.
     */
    fun reduceGeneric(amount: Int): ManaCost {
        if (amount <= 0) return this
        val coloredSymbols = symbols.filter { it !is ManaSymbol.Generic }
        val newGenericAmount = (genericAmount - amount).coerceAtLeast(0)
        return if (newGenericAmount > 0) {
            ManaCost(listOf(ManaSymbol.Generic(newGenericAmount)) + coloredSymbols)
        } else {
            ManaCost(coloredSymbols)
        }
    }

    /**
     * Reduce this cost by the maximum convoke contribution from the given creatures.
     * Each creature pays for one colored symbol matching its color, or one generic mana.
     * Colored symbols are matched greedily first, then remaining creatures pay generic.
     */
    fun reduceByConvoke(creatureColors: List<Set<Color>>): ManaCost {
        if (creatureColors.isEmpty()) return this
        val remainingColored = symbols.filter { it is ManaSymbol.Colored }.toMutableList()
        var genericReduction = 0

        for (colors in creatureColors) {
            // Try to match a colored symbol first
            val matchIndex = remainingColored.indexOfFirst { symbol ->
                symbol is ManaSymbol.Colored && symbol.color in colors
            }
            if (matchIndex >= 0) {
                remainingColored.removeAt(matchIndex)
            } else {
                // Pay generic
                genericReduction++
            }
        }

        val otherSymbols = symbols.filter { it !is ManaSymbol.Generic && it !is ManaSymbol.Colored }
        val newGenericAmount = (genericAmount - genericReduction).coerceAtLeast(0)
        val genericSymbols = if (newGenericAmount > 0) listOf(ManaSymbol.Generic(newGenericAmount)) else emptyList()
        return ManaCost(genericSymbols + remainingColored + otherSymbols)
    }

    /**
     * Return a relaxed cost where every colored, hybrid, phyrexian, and colorless requirement
     * is converted into generic mana — suitable for "mana of any type can be spent" effects
     * (e.g. Taster of Wares, Cruelclaw's Heist).
     *
     * Generic, X, and existing generic-equivalent symbols are preserved; only color/type
     * requirements are dropped.
     */
    fun relaxColors(): ManaCost {
        if (symbols.none { it is ManaSymbol.Colored || it is ManaSymbol.Hybrid ||
                it is ManaSymbol.Phyrexian || it is ManaSymbol.Colorless }) return this
        var addedGeneric = 0
        val keptSymbols = mutableListOf<ManaSymbol>()
        for (symbol in symbols) {
            when (symbol) {
                is ManaSymbol.Colored, is ManaSymbol.Hybrid,
                is ManaSymbol.Phyrexian, is ManaSymbol.Colorless -> addedGeneric++
                is ManaSymbol.Generic, is ManaSymbol.X -> keptSymbols.add(symbol)
            }
        }
        val existingGeneric = keptSymbols.filterIsInstance<ManaSymbol.Generic>().sumOf { it.amount }
        val nonGenericKept = keptSymbols.filterNot { it is ManaSymbol.Generic }
        val totalGeneric = existingGeneric + addedGeneric
        val genericList = if (totalGeneric > 0) listOf(ManaSymbol.Generic(totalGeneric)) else emptyList()
        return ManaCost(genericList + nonGenericKept)
    }

    operator fun plus(other: ManaCost): ManaCost {
        val mergedGeneric = this.genericAmount + other.genericAmount
        val nonGeneric = this.symbols.filterNot { it is ManaSymbol.Generic } +
            other.symbols.filterNot { it is ManaSymbol.Generic }
        val genericSymbol = if (mergedGeneric > 0) listOf(ManaSymbol.generic(mergedGeneric)) else emptyList()
        return ManaCost(genericSymbol + nonGeneric)
    }

    override fun toString(): String = symbols.joinToString("")

    companion object {
        val ZERO = ManaCost(emptyList())

        fun parse(costString: String): ManaCost {
            if (costString.isBlank()) return ZERO

            val symbols = mutableListOf<ManaSymbol>()
            val regex = Regex("""\{([^}]+)}""")

            regex.findAll(costString).forEach { match ->
                val content = match.groupValues[1]
                val symbol = when {
                    content == "W" -> ManaSymbol.W
                    content == "U" -> ManaSymbol.U
                    content == "B" -> ManaSymbol.B
                    content == "R" -> ManaSymbol.R
                    content == "G" -> ManaSymbol.G
                    content == "C" -> ManaSymbol.C
                    content == "X" -> ManaSymbol.X
                    content.toIntOrNull() != null -> ManaSymbol.generic(content.toInt())
                    // Hybrid mana: {W/U}, {G/U}, etc.
                    content.contains("/") && !content.contains("P") -> {
                        val parts = content.split("/")
                        if (parts.size == 2) {
                            val color1 = Color.fromSymbol(parts[0][0])
                            val color2 = Color.fromSymbol(parts[1][0])
                            if (color1 != null && color2 != null) {
                                ManaSymbol.Hybrid(color1, color2)
                            } else {
                                throw IllegalArgumentException("Unknown hybrid mana symbol: {$content}")
                            }
                        } else {
                            throw IllegalArgumentException("Unknown mana symbol: {$content}")
                        }
                    }
                    // Phyrexian mana: {W/P}, {G/P}, etc.
                    content.contains("/P") -> {
                        val colorPart = content.substringBefore("/P")
                        val color = Color.fromSymbol(colorPart[0])
                        if (color != null) {
                            ManaSymbol.Phyrexian(color)
                        } else {
                            throw IllegalArgumentException("Unknown Phyrexian mana symbol: {$content}")
                        }
                    }
                    else -> throw IllegalArgumentException("Unknown mana symbol: {$content}")
                }
                symbols.add(symbol)
            }

            return ManaCost(symbols)
        }

        fun of(vararg symbols: ManaSymbol): ManaCost = ManaCost(symbols.toList())
    }
}
