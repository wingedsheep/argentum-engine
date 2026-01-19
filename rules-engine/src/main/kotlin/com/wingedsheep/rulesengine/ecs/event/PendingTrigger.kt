package com.wingedsheep.rulesengine.ecs.event

import com.wingedsheep.rulesengine.ability.TriggeredAbility
import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Represents a triggered ability that has been triggered and is waiting to go on the stack.
 * Contains information about the source, controller, and any relevant context.
 *
 * ECS version using EntityId instead of CardId/PlayerId.
 */
@Serializable
data class PendingTrigger(
    val ability: TriggeredAbility,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val triggerContext: TriggerContext
) {
    val description: String
        get() = "$sourceName: ${ability.description}"

    val isOptional: Boolean
        get() = ability.optional
}

/**
 * A triggered ability on the stack, ready to resolve.
 * This is what gets put on the stack when a trigger fires.
 */
@Serializable
data class StackedTrigger(
    val pendingTrigger: PendingTrigger,
    val chosenTargets: List<ChosenTarget> = emptyList()
) {
    val sourceId: EntityId get() = pendingTrigger.sourceId
    val controllerId: EntityId get() = pendingTrigger.controllerId
    val description: String get() = pendingTrigger.description
}

/**
 * A target that has been chosen for an effect.
 */
@Serializable
sealed interface ChosenTarget {
    @Serializable
    data class Player(val playerId: EntityId) : ChosenTarget

    @Serializable
    data class Permanent(val entityId: EntityId) : ChosenTarget

    @Serializable
    data class Card(val cardId: EntityId, val zoneId: com.wingedsheep.rulesengine.ecs.ZoneId) : ChosenTarget
}
