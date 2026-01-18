package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.card.CounterType
import com.wingedsheep.rulesengine.ecs.Component
import kotlinx.serialization.Serializable

/**
 * State components represent dynamic state that can be added/removed during gameplay.
 */

/**
 * Marks a permanent as tapped.
 *
 * Presence of this component means the permanent is tapped.
 * Absence means the permanent is untapped.
 *
 * Usage:
 * - Add when tapping: `container.with(TappedComponent)`
 * - Remove when untapping: `container.without<TappedComponent>()`
 * - Check if tapped: `container.has<TappedComponent>()`
 */
@Serializable
data object TappedComponent : Component

/**
 * Marks a creature as having summoning sickness.
 *
 * Creatures with summoning sickness cannot attack or use abilities
 * with the tap symbol in their cost (unless they have haste).
 *
 * This component is removed at the start of its controller's turn.
 */
@Serializable
data object SummoningSicknessComponent : Component

/**
 * Tracks damage marked on a creature.
 *
 * Damage persists until end of turn cleanup step.
 * A creature with damage >= toughness has lethal damage.
 *
 * @property amount The amount of damage marked on this creature (always >= 0)
 */
@Serializable
data class DamageComponent(
    val amount: Int
) : Component {
    init {
        require(amount >= 0) { "Damage cannot be negative: $amount" }
    }

    /**
     * Add more damage to this creature.
     */
    fun addDamage(additionalDamage: Int): DamageComponent =
        copy(amount = amount + additionalDamage)

    /**
     * Clear all damage (happens during cleanup step).
     */
    fun clear(): DamageComponent = copy(amount = 0)
}

/**
 * Tracks counters on a permanent.
 *
 * Supports all counter types: +1/+1, -1/-1, loyalty, charge, etc.
 * Counters are stored in a map for extensibility.
 *
 * @property counters Map of counter type to count
 */
@Serializable
data class CountersComponent(
    val counters: Map<CounterType, Int> = emptyMap()
) : Component {
    /**
     * Get the count of a specific counter type.
     */
    fun getCount(type: CounterType): Int = counters[type] ?: 0

    /**
     * Add counters of a specific type.
     */
    fun add(type: CounterType, amount: Int = 1): CountersComponent {
        val newAmount = getCount(type) + amount
        return copy(counters = counters + (type to newAmount))
    }

    /**
     * Remove counters of a specific type.
     * Cannot go below zero.
     */
    fun remove(type: CounterType, amount: Int = 1): CountersComponent {
        val current = getCount(type)
        val newAmount = (current - amount).coerceAtLeast(0)
        return if (newAmount == 0) {
            copy(counters = counters - type)
        } else {
            copy(counters = counters + (type to newAmount))
        }
    }

    /**
     * Check if this has any counters of a specific type.
     */
    fun has(type: CounterType): Boolean = getCount(type) > 0

    /**
     * Get total +1/+1 counter count (for creature P/T calculations).
     */
    val plusOnePlusOneCount: Int get() = getCount(CounterType.PLUS_ONE_PLUS_ONE)

    /**
     * Get total -1/-1 counter count (for creature P/T calculations).
     */
    val minusOneMinusOneCount: Int get() = getCount(CounterType.MINUS_ONE_MINUS_ONE)

    companion object {
        val EMPTY = CountersComponent()
    }
}
