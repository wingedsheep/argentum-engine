package com.wingedsheep.engine.event

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import kotlinx.serialization.Serializable

/**
 * A triggered ability that has been granted to an entity temporarily.
 *
 * Used for effects like Commando Raid that grant triggered abilities
 * until end of turn. Stored in GameState.grantedTriggeredAbilities
 * and checked by TriggerDetector when looking up abilities for entities.
 *
 * @property entityId The entity that has the granted ability
 * @property ability The triggered ability that was granted
 * @property duration How long the grant lasts
 */
@Serializable
data class GrantedTriggeredAbility(
    val entityId: EntityId,
    val ability: TriggeredAbility,
    val duration: Duration
)
