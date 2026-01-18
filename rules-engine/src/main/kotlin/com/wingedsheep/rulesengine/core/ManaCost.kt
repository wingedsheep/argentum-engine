package com.wingedsheep.rulesengine.core

import kotlinx.serialization.Serializable

@Serializable
data class ManaCost(val symbols: List<ManaSymbol>) {

    val cmc: Int
        get() = symbols.sumOf { it.cmc }

    val colors: Set<Color>
        get() = symbols
            .filterIsInstance<ManaSymbol.Colored>()
            .map { it.color }
            .toSet()

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

    fun isEmpty(): Boolean = symbols.isEmpty()

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
                    else -> throw IllegalArgumentException("Unknown mana symbol: {$content}")
                }
                symbols.add(symbol)
            }

            return ManaCost(symbols)
        }

        fun of(vararg symbols: ManaSymbol): ManaCost = ManaCost(symbols.toList())
    }
}
