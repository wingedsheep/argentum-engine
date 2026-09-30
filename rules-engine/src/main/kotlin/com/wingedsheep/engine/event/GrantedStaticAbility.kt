package com.wingedsheep.engine.event

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.StaticAbility
import kotlinx.serialization.Serializable

/**
 * A static ability that has been granted to an entity temporarily.
 *
 * Used for effects like Full Steam Ahead that grant a static ability (e.g.
 * "can't be blocked by more than one creature") until end of turn. Stored in
 * [com.wingedsheep.engine.state.GameState.grantedStaticAbilities] and read at the
 * point of use — combat blocker validation consults granted
 * [com.wingedsheep.sdk.scripting.CantBeBlockedByMoreThan] alongside the creature's
 * printed static abilities.
 *
 * Mirrors [GrantedTriggeredAbility] and [GrantedActivatedAbility]: static abilities are
 * checked where they matter (combat, restrictions) rather than projected onto the entity
 * via the layer system, so a simple GameState-keyed record is the right channel.
 *
 * @property entityId The entity that has the granted ability
 * @property ability The static ability that was granted
 * @property duration How long the grant lasts
 * @property sourceId The permanent whose effect handed out the grant — see
 *   [GrantedActivatedAbility.sourceId]. Required for source-keyed "for as long as …" durations so
 *   `EndedDurationExpiryCheck` can drop the grant when its source leaves the battlefield; null for
 *   durations that need no source.
 * @property controllerId The player who controlled the granting effect. Keys
 *   [Duration.UntilYourNextTurn] expiry to *that* player's next turn (Nahiri, the Unforgiving's +1
 *   aimed at an opponent's creature), not to the granted entity's controller; null for legacy
 *   records and durations that don't need it.
 */
@Serializable
data class GrantedStaticAbility(
    val entityId: EntityId,
    val ability: StaticAbility,
    val duration: Duration,
    val sourceId: EntityId? = null,
    val controllerId: EntityId? = null
)
