package com.wingedsheep.engine.state

import kotlinx.serialization.Serializable

/** Histories belong to one source object, not the reusable entity representing its card. */
@Serializable
data class SourceObjectRecord(
    val source: ObjectRef,
    val battlefieldTimestamp: Long? = null,
    val slots: Map<String, List<ObjectRef>> = emptyMap(),
)
