package com.wingedsheep.rulesengine.core

import kotlinx.serialization.Serializable

@Serializable
enum class Color(val symbol: Char, val displayName: String) {
    WHITE('W', "White"),
    BLUE('U', "Blue"),
    BLACK('B', "Black"),
    RED('R', "Red"),
    GREEN('G', "Green");

    companion object {
        fun fromSymbol(symbol: Char): Color? = entries.find { it.symbol == symbol }
    }
}
