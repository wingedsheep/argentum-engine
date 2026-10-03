package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** Answer with a normal cast or land-play action; it grants no priority. */
@Serializable
@SerialName("PlayCardDecision")
data class PlayCardDecision(
    override val id: String,
    override val playerId: EntityId,
    override val prompt: String,
    override val context: DecisionContext,
    val cardId: EntityId,
) : PendingDecision

@Serializable
@SerialName("PlayCardResponse")
data class PlayCardResponse(override val decisionId: String, val action: GameAction) : DecisionResponse
