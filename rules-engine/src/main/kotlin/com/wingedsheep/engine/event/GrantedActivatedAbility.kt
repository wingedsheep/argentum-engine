package com.wingedsheep.engine.event

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.Duration
import kotlinx.serialization.Serializable

/**
 * An activated ability that has been granted to an entity temporarily.
 *
 * Used for effects like Run Wild that grant activated abilities
 * until end of turn. Stored in GameState.grantedActivatedAbilities
 * and checked by GameSession when computing legal actions and by
 * ActivateAbilityHandler when validating/executing activations.
 *
 * @property entityId The entity that has the granted ability
 * @property ability The activated ability that was granted
 * @property duration How long the grant lasts
 */
@Serializable
data class GrantedActivatedAbility(
    val entityId: EntityId,
    val ability: ActivatedAbility,
    val duration: Duration
)
