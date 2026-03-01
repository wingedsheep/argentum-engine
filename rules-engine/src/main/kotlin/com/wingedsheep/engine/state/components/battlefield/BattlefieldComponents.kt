package com.wingedsheep.engine.state.components.battlefield

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
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

/**
 * Tracks which activated abilities have been activated this turn.
 * Used for "Activate only once each turn" restrictions.
 * Cleared at end of turn by TurnManager.
 */
@Serializable
data class AbilityActivatedThisTurnComponent(
    val abilityIds: Set<AbilityId> = emptySet()
) : Component {
    fun withActivated(abilityId: AbilityId): AbilityActivatedThisTurnComponent =
        copy(abilityIds = abilityIds + abilityId)

    fun hasActivated(abilityId: AbilityId): Boolean = abilityId in abilityIds
}

/**
 * Tracks which creatures this entity dealt damage to this turn.
 * Used for triggers like Soul Collector: "Whenever a creature dealt damage by Soul Collector this turn dies..."
 * Cleared at end of turn by TurnManager.
 */
@Serializable
data class DamageDealtToCreaturesThisTurnComponent(
    val creatureIds: Set<EntityId> = emptySet()
) : Component {
    fun withCreature(creatureId: EntityId): DamageDealtToCreaturesThisTurnComponent =
        copy(creatureIds = creatureIds + creatureId)
}

/**
 * Marks a permanent as granting shroud to its controller.
 * Used for True Believer: "You have shroud."
 * When the permanent leaves the battlefield, the component goes with it — no cleanup needed.
 */
@Serializable
data object GrantsControllerShroudComponent : Component

/**
 * Tracks entity IDs of cards exiled by this permanent, so they can be
 * returned when the permanent leaves the battlefield.
 *
 * Used for Day of the Dragons-style effects where exiled cards are linked
 * to a specific source permanent.
 *
 * Not stripped by [stripBattlefieldComponents] — intentionally persists when
 * the permanent moves zones so LTB triggers can still read it.
 */
@Serializable
data class LinkedExileComponent(
    val exiledIds: List<EntityId> = emptyList()
) : Component
