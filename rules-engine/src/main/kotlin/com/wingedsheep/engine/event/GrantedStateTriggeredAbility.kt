package com.wingedsheep.engine.event

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.StateTriggeredAbility
import kotlinx.serialization.Serializable

/**
 * A state-triggered ability (CR 603.8) that has been granted to an entity.
 *
 * The twin of [GrantedTriggeredAbility] for abilities the [StateTriggerPoller] owns rather than
 * the `TriggerIndex`: granted by [com.wingedsheep.sdk.scripting.effects.GrantStateTriggeredAbilityEffect],
 * stored in `GameState.grantedStateTriggeredAbilities`, and folded into the poller's per-permanent
 * ability list alongside the ones printed on the card.
 *
 * Lifecycle suppression is keyed by the source object and the granted ability's own
 * `AbilityId`, exactly like a printed state trigger.
 *
 * @property entityId The entity that has the granted ability
 * @property ability The state-triggered ability that was granted
 * @property duration How long the grant lasts
 */
@Serializable
data class GrantedStateTriggeredAbility(
    val entityId: EntityId,
    val ability: StateTriggeredAbility,
    val duration: Duration
)
