package com.wingedsheep.engine.event

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import kotlinx.serialization.Serializable

/**
 * A delayed triggered ability that fires at a specific step.
 *
 * Used for effects like Astral Slide that exile a creature and return it
 * "at the beginning of the next end step."
 *
 * @property id Unique identifier for this delayed trigger
 * @property effect The effect to execute when the trigger fires
 * @property fireAtStep The step at which this trigger should fire
 * @property sourceId The entity that created this delayed trigger
 * @property sourceName Human-readable name of the source
 * @property controllerId The player who controls this delayed trigger
 */
@Serializable
data class DelayedTriggeredAbility(
    val id: String,
    val effect: Effect,
    val fireAtStep: Step,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId
)
