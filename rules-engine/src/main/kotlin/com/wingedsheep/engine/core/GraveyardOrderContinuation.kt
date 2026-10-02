package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.EntitySnapshot
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/** The moves have happened; publish their events only after all owners order their new cards. */
@Serializable
data class GraveyardOrderContinuation(
    val ownerId: EntityId,
    val cards: List<EntityId>,
    val remaining: Map<EntityId, List<EntityId>>,
    val events: List<GameEvent>,
    val collections: Map<String, List<EntityId>> = emptyMap(),
    val numbers: Map<String, Int> = emptyMap(),
    val chosenValues: Map<String, String> = emptyMap(),
    val subtypeGroups: Map<String, List<Set<String>>> = emptyMap(),
    val sacrificed: List<EntitySnapshot> = emptyList(),
) : AnswerContinuation
