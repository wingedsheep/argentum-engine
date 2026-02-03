package com.wingedsheep.engine.handlers

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId

/**
 * Context for effect execution.
 */
data class EffectContext(
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val targets: List<ChosenTarget> = emptyList(),
    val xValue: Int? = null,
    val sacrificedPermanents: List<EntityId> = emptyList(),
    /** Pre-chosen damage distribution for DividedDamageEffect spells (target ID -> damage amount) */
    val damageDistribution: Map<EntityId, Int>? = null
)
