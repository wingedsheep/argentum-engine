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
 * Mirrors [GrantedTriggeredAbility] and [GrantedActivatedAbility]: the record is consulted where
 * it matters (combat, restrictions) for the halves that never lower to a continuous effect, and —
 * when [layerTimestamp] is set — its layer-system half is also projected onto the holder.
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
 * @property layerTimestamp Non-null when the grant is a real ability the permanent gained from a
 *   resolving effect ([com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect] on a
 *   battlefield permanent): `StateProjector` then lowers its layer-system part (can't block, can't
 *   attack, keyword grants, P/T changes, ...) into continuous effects sourced from the holder, at
 *   this timestamp — the later of the holder's and the grant's (CR 613.7a). Null for records that
 *   only mirror statics already baked into the holder's `ContinuousEffectSourceComponent` (a
 *   token's own statics), which must not be applied a second time, and for player- or
 *   graveyard-anchored grants, which have no battlefield object to project onto.
 */
@Serializable
data class GrantedStaticAbility(
    val entityId: EntityId,
    val ability: StaticAbility,
    val duration: Duration,
    val sourceId: EntityId? = null,
    val controllerId: EntityId? = null,
    val layerTimestamp: Long? = null
)
