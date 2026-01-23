package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.scripting.DynamicAmount
import kotlinx.serialization.Serializable

/**
 * Represents a characteristic value that can be either fixed or dynamic.
 *
 * This supports characteristic-defining abilities (CDAs) like Tarmogoyf's
 * power/toughness that depend on game state.
 *
 * Usage:
 * ```kotlin
 * // Fixed value (most creatures)
 * CharacteristicValue.Fixed(3)
 *
 * // Dynamic value (Tarmogoyf)
 * CharacteristicValue.Dynamic(DynamicAmount.CardTypesInAllGraveyards)
 *
 * // Dynamic with offset (Tarmogoyf's toughness = * + 1)
 * CharacteristicValue.DynamicWithOffset(DynamicAmount.CardTypesInAllGraveyards, 1)
 * ```
 */
@Serializable
sealed interface CharacteristicValue {
    val description: String

    /**
     * A fixed integer value.
     * Example: A 2/2 creature has Fixed(2) for both power and toughness.
     */
    @Serializable
    data class Fixed(val value: Int) : CharacteristicValue {
        override val description: String = value.toString()
    }

    /**
     * A dynamic value determined by game state.
     * Example: Tarmogoyf's power is Dynamic(CardTypesInAllGraveyards).
     */
    @Serializable
    data class Dynamic(val source: DynamicAmount) : CharacteristicValue {
        override val description: String = "*"
    }

    /**
     * A dynamic value with a fixed offset.
     * Example: Tarmogoyf's toughness is DynamicWithOffset(CardTypesInAllGraveyards, 1) = *+1.
     */
    @Serializable
    data class DynamicWithOffset(
        val source: DynamicAmount,
        val offset: Int
    ) : CharacteristicValue {
        override val description: String = when {
            offset > 0 -> "*+$offset"
            offset < 0 -> "*$offset"
            else -> "*"
        }
    }

    companion object {
        /**
         * Create a fixed characteristic value.
         */
        fun of(value: Int): CharacteristicValue = Fixed(value)

        /**
         * Create a dynamic characteristic value.
         */
        fun dynamic(source: DynamicAmount): CharacteristicValue = Dynamic(source)

        /**
         * Create a dynamic characteristic value with offset.
         */
        fun dynamic(source: DynamicAmount, offset: Int): CharacteristicValue =
            if (offset == 0) Dynamic(source) else DynamicWithOffset(source, offset)
    }
}

/**
 * Operator for adding offset to CharacteristicValue.
 * Example: CharacteristicValue.dynamic(source) + 1
 */
operator fun CharacteristicValue.plus(offset: Int): CharacteristicValue = when (this) {
    is CharacteristicValue.Fixed -> CharacteristicValue.Fixed(value + offset)
    is CharacteristicValue.Dynamic -> CharacteristicValue.DynamicWithOffset(source, offset)
    is CharacteristicValue.DynamicWithOffset -> CharacteristicValue.DynamicWithOffset(source, this.offset + offset)
}
