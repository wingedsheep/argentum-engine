package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Marks a creature as a player's **Ring-bearer** (CR 701.54a–e).
 *
 * Being a Ring-bearer is a designation, not a copiable value (701.54b). A creature "is [ownerId]'s
 * Ring-bearer" only while it has this component **and** is on the battlefield under [ownerId]'s
 * control (701.54e) — so a *transient* loss of control (e.g. a projected/continuous control effect)
 * suspends the designation's effects without the component having to be touched, and they resume if
 * control reverts. A control change that actually transfers the permanent to another player ends the
 * designation *permanently* (701.54a), so the control-change executors strip this component eagerly
 * via `clearRingBearerOnControlChange` — otherwise a temporary steal (Threaten) would silently
 * restore the designation when control reverted. The designation moves to a new creature each time
 * that player is tempted (handled by the tempt executor), and at most one creature carries this
 * component per owner.
 *
 * @property ownerId The player who designated this creature (whose Ring emblem grants its abilities).
 */
@Serializable
data class RingBearerComponent(
    val ownerId: EntityId
) : Component
