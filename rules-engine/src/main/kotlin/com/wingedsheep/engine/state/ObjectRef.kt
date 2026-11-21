package com.wingedsheep.engine.state

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/** One rules object, distinct from the stable entity that represents its card across zones. */
@Serializable
data class ObjectRef(val entityId: EntityId, val generation: Long)

/**
 * Persistent bookkeeping: removal/pop does not end a visit until a destination is committed.
 *
 * [previousGeneration] is the generation of the visit this one directly followed (null for an
 * object's first visit). It lets an effect follow one object to the *very next* object its card
 * became — "the sacrificed card" found in whatever public zone a replacement put it (CR 400.7) —
 * while rejecting a card that has since moved on again.
 */
@Serializable
data class ObjectIdentity(
    val generation: Long,
    val logicalZone: ZoneKey,
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    @kotlinx.serialization.EncodeDefault(kotlinx.serialization.EncodeDefault.Mode.NEVER)
    val previousGeneration: Long? = null,
)
