package com.wingedsheep.engine.state.components.battlefield

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ReplacementEffect
import kotlinx.serialization.Serializable

/**
 * Marks a permanent as tapped.
 */
@Serializable
data object TappedComponent : Component

/**
 * Marks a creature as having summoning sickness.
 * Removed at the beginning of the controller's turn.
 */
@Serializable
data object SummoningSicknessComponent : Component

/**
 * Counters on a permanent.
 */
@Serializable
data class CountersComponent(
    val counters: Map<CounterType, Int> = emptyMap()
) : Component {
    fun getCount(type: CounterType): Int = counters[type] ?: 0

    fun withAdded(type: CounterType, amount: Int): CountersComponent {
        val current = getCount(type)
        return CountersComponent(counters + (type to current + amount))
    }

    fun withRemoved(type: CounterType, amount: Int): CountersComponent {
        val current = getCount(type)
        val newCount = (current - amount).coerceAtLeast(0)
        return if (newCount == 0) {
            CountersComponent(counters - type)
        } else {
            CountersComponent(counters + (type to newCount))
        }
    }

    /**
     * Set counters to a specific value.
     * Used for planeswalkers entering the battlefield with starting loyalty.
     */
    fun withCounters(type: CounterType, amount: Int): CountersComponent {
        return if (amount <= 0) {
            CountersComponent(counters - type)
        } else {
            CountersComponent(counters + (type to amount))
        }
    }
}

/**
 * Damage marked on a creature (cleared at cleanup).
 */
@Serializable
data class DamageComponent(
    val amount: Int
) : Component

/**
 * Aura/Equipment attachment.
 */
@Serializable
data class AttachedToComponent(
    val targetId: EntityId
) : Component

/**
 * Tracks what is attached to this permanent.
 */
@Serializable
data class AttachmentsComponent(
    val attachedIds: List<EntityId>
) : Component

/**
 * Permanent entered the battlefield this turn.
 */
@Serializable
data object EnteredThisTurnComponent : Component

/**
 * Stores replacement effects on a permanent (e.g., Daunting Defender's damage prevention).
 * These are static replacement effects that are continuously active while the permanent
 * is on the battlefield, as opposed to one-shot floating effect shields.
 */
@Serializable
data class ReplacementEffectSourceComponent(
    val replacementEffects: List<ReplacementEffect>
) : Component

/**
 * Timestamp for ordering effects (Rule 613).
 */
@Serializable
data class TimestampComponent(
    val timestamp: Long
) : Component
