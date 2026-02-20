package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.Serializable

/**
 * Represents a creature's power and toughness.
 *
 * Supports both fixed values (most creatures) and dynamic values
 * (characteristic-defining abilities like Tarmogoyf).
 *
 * Usage:
 * ```kotlin
 * // Fixed stats (most creatures)
 * CreatureStats(2, 3)
 *
 * // Dynamic stats (Tarmogoyf)
 * CreatureStats(
 *     power = CharacteristicValue.dynamic(DynamicAmount.CardTypesInAllGraveyards),
 *     toughness = CharacteristicValue.dynamic(DynamicAmount.CardTypesInAllGraveyards) + 1
 * )
 * ```
 */
@Serializable
data class CreatureStats(
    val power: CharacteristicValue,
    val toughness: CharacteristicValue
) {
    /**
     * Backwards-compatible constructor for fixed power/toughness.
     */
    constructor(basePower: Int, baseToughness: Int) : this(
        CharacteristicValue.Fixed(basePower),
        CharacteristicValue.Fixed(baseToughness)
    )

    init {
        // Validate fixed values are non-negative
        if (power is CharacteristicValue.Fixed) {
            require(power.value >= 0) { "Base power cannot be negative: ${power.value}" }
        }
        if (toughness is CharacteristicValue.Fixed) {
            require(toughness.value >= 0) { "Base toughness cannot be negative: ${toughness.value}" }
        }
    }

    /**
     * Get base power as Int (for fixed stats only).
     * Returns null for dynamic stats.
     */
    val basePower: Int?
        get() = (power as? CharacteristicValue.Fixed)?.value

    /**
     * Get base toughness as Int (for fixed stats only).
     * Returns null for dynamic stats.
     */
    val baseToughness: Int?
        get() = (toughness as? CharacteristicValue.Fixed)?.value

    /**
     * Whether this creature has dynamic (characteristic-defining) stats.
     */
    val isDynamic: Boolean
        get() = power !is CharacteristicValue.Fixed || toughness !is CharacteristicValue.Fixed

    override fun toString(): String = "${power.description}/${toughness.description}"

    companion object {
        /**
         * Parse power/toughness from strings.
         * Supports both numeric ("2", "3") and star ("*") notation.
         */
        fun parse(power: String, toughness: String): CreatureStats {
            val p = parseCharacteristicValue(power)
            val t = parseCharacteristicValue(toughness)
            return CreatureStats(p, t)
        }

        private fun parseCharacteristicValue(value: String): CharacteristicValue {
            // Handle star notation
            if (value == "*") {
                // Default to a generic dynamic source - engine will need to resolve this
                return CharacteristicValue.Dynamic(DynamicAmount.Fixed(0))
            }
            // Handle *+N or *-N notation
            val starMatch = Regex("""\*([+-]\d+)""").matchEntire(value)
            if (starMatch != null) {
                val offset = starMatch.groupValues[1].toInt()
                return CharacteristicValue.DynamicWithOffset(DynamicAmount.Fixed(0), offset)
            }
            // Handle plain integer
            val intValue = value.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid power/toughness value: $value")
            return CharacteristicValue.Fixed(intValue)
        }

        /**
         * Create creature stats with fixed values.
         */
        fun of(power: Int, toughness: Int): CreatureStats = CreatureStats(power, toughness)

        /**
         * Create creature stats with dynamic power and toughness from the same source.
         * Example: Tarmogoyf uses this with offset for toughness.
         */
        fun dynamic(
            source: DynamicAmount,
            powerOffset: Int = 0,
            toughnessOffset: Int = 0
        ): CreatureStats = CreatureStats(
            power = CharacteristicValue.dynamic(source, powerOffset),
            toughness = CharacteristicValue.dynamic(source, toughnessOffset)
        )
    }
}
