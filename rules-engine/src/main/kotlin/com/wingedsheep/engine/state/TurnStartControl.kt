package com.wingedsheep.engine.state

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/** The controller and rules object present when the current turn began. */
@Serializable
data class TurnStartControl(val controllerId: EntityId, val objectRef: ObjectRef)
