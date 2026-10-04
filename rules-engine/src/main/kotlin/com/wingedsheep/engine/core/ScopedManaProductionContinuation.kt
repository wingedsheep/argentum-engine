package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.player.ManaSourceTag
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/** One activation's entire production, including effects that pause more than once. */
@Serializable
data class ScopedManaProductionContinuation(
    val playerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val sourceCard: CardComponent,
    val poolBefore: ManaPoolComponent,
    val sourceTag: ManaSourceTag,
    val costsTap: Boolean,
) : AutomaticContinuation
