package com.wingedsheep.sdk.scripting.values

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a variable that can store values during effect execution.
 *
 * This enables effects to:
 * 1. Store the result of an action (e.g., which card was exiled)
 * 2. Store a count (e.g., how many lands were sacrificed)
 * 3. Reference these stored values in subsequent effects
 *
 * Usage:
 * ```kotlin
 * // Store the exiled card reference
 * StoreResultEffect(
 *     effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.Exile),
 *     storeAs = EffectVariable.EntityRef("exiledCard")
 * )
 *
 * // Later, return it from exile using StoredEntityTarget("exiledCard")
 * ```
 */
@Serializable
sealed interface EffectVariable {
    /** Name used to reference this variable */
    val name: String

    /** Human-readable description */
    val description: String

    /**
     * Stores a reference to an entity (card, permanent, player).
     * Used for: "exile target creature... return the exiled card"
     */
    @SerialName("EntityRef")
    @Serializable
    data class EntityRef(override val name: String) : EffectVariable {
        override val description: String = "the $name"
    }

    /**
     * Stores a count/number.
     * Used for: "sacrifice any number of lands... search for that many lands"
     */
    @SerialName("Count")
    @Serializable
    data class Count(override val name: String) : EffectVariable {
        override val description: String = "the number of $name"
    }

    /**
     * Stores an amount (damage dealt, life gained, etc.).
     * Used for: "deal damage equal to the damage dealt this way"
     */
    @SerialName("Amount")
    @Serializable
    data class Amount(override val name: String) : EffectVariable {
        override val description: String = "the $name amount"
    }
}
