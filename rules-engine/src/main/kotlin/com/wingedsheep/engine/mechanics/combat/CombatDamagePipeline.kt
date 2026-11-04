package com.wingedsheep.engine.mechanics.combat

import com.wingedsheep.sdk.model.EntityId

/** Damage assigned by one combatant before replacement and prevention effects apply. */
data class CombatDamageAssignment(
    val sourceId: EntityId,
    val targetId: EntityId,
    val amount: Int
)
