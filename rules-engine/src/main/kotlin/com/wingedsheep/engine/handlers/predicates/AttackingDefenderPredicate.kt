package com.wingedsheep.engine.handlers.predicates

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.TargetResolutionUtils
import com.wingedsheep.engine.mechanics.combat.CombatDefenders
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.targets.EffectTarget

internal fun isAttackingDefenderOf(
    state: GameState,
    projected: ProjectedState,
    attacker: EntityId,
    reference: EffectTarget.SingleEntity,
    context: PredicateContext,
): Boolean {
    val referenced = TargetResolutionUtils.resolveEntity(reference, context.toEffectContext(), state, projected) ?: return false
    val player = if (referenced in state.turnOrder) referenced else projected.getController(referenced) ?: return false
    val attack = state.getEntity(attacker)?.get<AttackingComponent>() ?: return false
    val defendingPlayer = CombatDefenders.defendingPlayerOf(state, attack, projected) ?: return false
    return player in state.sharedTurnTeam(defendingPlayer)
}
