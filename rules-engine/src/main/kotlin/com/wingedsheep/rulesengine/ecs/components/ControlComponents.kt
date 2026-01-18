package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Components for control and attachment relationships.
 */

/**
 * Tracks who controls a permanent.
 *
 * Control is separate from ownership (tracked in CardComponent).
 * Control can change during the game through various effects.
 *
 * - Owner: Who the card belongs to (for "return to owner's hand" effects)
 * - Controller: Who currently controls the permanent (for "you control" effects)
 *
 * By default, controller = owner when a permanent enters the battlefield.
 *
 * @property controllerId The entity ID of the player who controls this permanent
 */
@Serializable
data class ControllerComponent(
    val controllerId: EntityId
) : Component {
    /**
     * Change control to a different player.
     */
    fun changeController(newControllerId: EntityId): ControllerComponent =
        copy(controllerId = newControllerId)
}

/**
 * Marks an aura or equipment as attached to another permanent.
 *
 * For auras: The enchanted permanent
 * For equipment: The equipped creature
 *
 * When the target leaves the battlefield or becomes an illegal target,
 * this component should be removed and the attachment may need to be
 * destroyed (auras) or become unattached (equipment).
 *
 * @property targetId The entity ID of the permanent this is attached to
 */
@Serializable
data class AttachedToComponent(
    val targetId: EntityId
) : Component {
    /**
     * Attach to a different target.
     */
    fun attachTo(newTargetId: EntityId): AttachedToComponent =
        copy(targetId = newTargetId)
}
