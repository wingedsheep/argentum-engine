package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.SoulbondPairing
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.chosenCreatureOf
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.Scope

/**
 * Whether any of [targetIds] is a permanent [group] matches, read relative to the cost static on
 * [staticSourceId] controlled by [controllerId] — the "that target …" half of a cost modifier:
 * a spell tax ([com.wingedsheep.sdk.scripting.SpellCostTarget.OpponentsCastTargeting]) or an
 * activated-ability one (`ReduceActivatedAbilityCost.targeting` /
 * `IncreaseActivatedAbilityCost.targeting`). `any`, not a count: a spell or ability targeting
 * several matching permanents is modified once (Kopala, Warden of Waves' ruling).
 */
internal fun anyTargetInGroup(
    state: GameState,
    predicateEvaluator: PredicateEvaluator,
    staticSourceId: EntityId,
    controllerId: EntityId,
    group: GroupFilter,
    targetIds: List<EntityId>,
): Boolean {
    if (targetIds.isEmpty()) return false
    val context = PredicateContext(controllerId = controllerId, sourceId = staticSourceId)
    val projected = state.projectedState
    return targetIds.any { targetId ->
        when (val scope = group.scope) {
            is Scope.Self -> targetId == staticSourceId
            is Scope.Specific -> targetId == scope.entityId
            is Scope.AttachedTo ->
                state.getEntity(staticSourceId)?.get<AttachedToComponent>()?.targetId == targetId
            is Scope.SoulbondPair -> SoulbondPairing.isInPairOf(state, staticSourceId, targetId)
            is Scope.ChosenCreature -> state.chosenCreatureOf(staticSourceId) == targetId
            is Scope.Battlefield ->
                predicateEvaluator.matches(state, projected, targetId, group.baseFilter, context)
        }
    }
}
