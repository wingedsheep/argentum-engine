package com.wingedsheep.engine.event

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
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
 * @property fireOnlyOnControllersTurn If true, only fires when the active player is the controller
 */
@Serializable
data class DelayedTriggeredAbility(
    val id: String,
    val effect: Effect,
    /** Step at which this step-based delayed trigger fires. Null for event-based triggers. */
    val fireAtStep: Step? = null,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val fireOnlyOnControllersTurn: Boolean = false,
    /**
     * For event-based delayed triggers: the TriggerSpec describing what event fires this.
     * When non-null, this ability is event-based and [fireAtStep] is unused.
     */
    val trigger: TriggerSpec? = null,
    /**
     * For event-based delayed triggers: the entity that scopes the trigger.
     * The trigger only fires for events whose relevant entity (e.g. damage source)
     * matches this id.
     */
    val watchedEntityId: EntityId? = null,
    /** For event-based delayed triggers: the expiry rule. */
    val expiry: DelayedTriggerExpiry? = null
)
