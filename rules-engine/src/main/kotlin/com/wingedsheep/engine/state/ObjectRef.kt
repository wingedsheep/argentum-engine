package com.wingedsheep.engine.state

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/** One rules object, distinct from the stable entity that represents its card across zones. */
@Serializable
data class ObjectRef(val entityId: EntityId, val generation: Long)

/** Persistent bookkeeping: removal/pop does not end a visit until a destination is committed. */
@Serializable
data class ObjectIdentity(val generation: Long, val logicalZone: ZoneKey)
