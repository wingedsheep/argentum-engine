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
