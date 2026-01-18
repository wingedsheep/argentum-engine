package com.wingedsheep.rulesengine.card

import kotlinx.serialization.Serializable

@Serializable
data class CreatureStats(
    val basePower: Int,
    val baseToughness: Int
) {
    init {
        require(basePower >= 0) { "Base power cannot be negative: $basePower" }
        require(baseToughness >= 0) { "Base toughness cannot be negative: $baseToughness" }
    }

    override fun toString(): String = "$basePower/$baseToughness"

    companion object {
        fun parse(power: String, toughness: String): CreatureStats {
            val p = power.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid power value: $power")
            val t = toughness.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid toughness value: $toughness")
            return CreatureStats(p, t)
        }

        fun of(power: Int, toughness: Int): CreatureStats = CreatureStats(power, toughness)
    }
}
