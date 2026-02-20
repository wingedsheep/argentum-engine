package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

/**
 * Determines how a triggered ability's source entity relates to the event entity.
 *
 * Replaces the scattered `selfOnly`/`youControlOnly`/`controllerOnly` booleans
 * from the old Trigger hierarchy.
 */
@Serializable
enum class TriggerBinding {
    /** The event entity IS this permanent (e.g., "when this creature enters"). */
    SELF,
    /** The event entity is NOT this permanent (e.g., "when another creature enters"). */
    OTHER,
    /** Any entity, or the trigger has no entity concept (e.g., phase triggers, spell cast triggers). */
    ANY
}
