package com.wingedsheep.engine.event

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import kotlinx.serialization.Serializable

/**
 * A triggered ability that has been created by a spell or effect and is not
 * attached to any specific permanent. Used for effects like False Cure that
 * create global triggered abilities until end of turn.
 *
 * Stored in GameState.globalGrantedTriggeredAbilities and checked by
 * TriggerDetector when looking for triggers on game events.
 *
 * @property ability The triggered ability
 * @property controllerId The player who controls this ability
 * @property sourceId The entity that created this ability (for stack display)
 * @property sourceName The name of the card that created this ability
 * @property duration How long the ability lasts
 */
@Serializable
data class GlobalGrantedTriggeredAbility(
    val ability: TriggeredAbility,
    val controllerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val duration: Duration
)
