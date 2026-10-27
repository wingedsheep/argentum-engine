package com.wingedsheep.engine.state

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * One player's control continuity across their upkeeps: [current] holds the permanents they have
 * controlled since the beginning of their most recent upkeep, [previous] those since the upkeep
 * before it (null if there was none). During that player's upkeep "your last upkeep" is the
 * previous one — the current one is the upkeep echo is triggering in — so readers pick [previous]
 * then and [current] otherwise, and [previous] is cleared when the upkeep step ends. Entries are
 * only ever dropped, like the turn-start snapshot.
 */
@Serializable
data class UpkeepControl(
    val current: Map<EntityId, ObjectRef>,
    val previous: Map<EntityId, ObjectRef>? = null,
)
