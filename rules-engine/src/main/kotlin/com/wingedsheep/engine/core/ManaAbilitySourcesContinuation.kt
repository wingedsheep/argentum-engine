package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import kotlinx.serialization.Serializable

/** Presence in the stack bounds a source restriction across nested payment suspensions. */
@Serializable
data class ManaAbilitySourcesContinuation(
    val playerId: EntityId,
    val sources: GameObjectFilter,
    val context: EffectContext,
) : AutomaticContinuation
