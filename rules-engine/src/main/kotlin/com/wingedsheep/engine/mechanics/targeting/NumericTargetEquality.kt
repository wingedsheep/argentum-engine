package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/** Shared reading for announcement, interactive decisions, enumeration and resolution. */
object NumericTargetEquality {
    private val amounts = PredicateEvaluator(cardRegistry = null).amounts
    fun value(state: GameState, id: EntityId, property: CardNumericProperty): Int? {
        val projected = state.projectedState
        val card = state.getEntity(id)?.get<CardComponent>() ?: return null
        return when (property) {
            CardNumericProperty.POWER -> if (id in state.getBattlefield()) projected.getPower(id)
                else amounts.offBattlefieldStat(state, id, true, projected)
            CardNumericProperty.TOUGHNESS -> if (id in state.getBattlefield()) projected.getToughness(id)
                else amounts.offBattlefieldStat(state, id, false, projected)
            CardNumericProperty.MANA_VALUE -> if (projected.isFaceDown(id)) 0 else card.manaValue
            CardNumericProperty.COUNTERS -> state.getEntity(id)?.get<CountersComponent>()?.counters?.values?.sum() ?: 0
        }
    }

    fun isSatisfied(state: GameState, ids: List<EntityId>, property: CardNumericProperty): Boolean {
        if (ids.size < 2) return true
        val first = value(state, ids.first(), property) ?: return false
        return ids.drop(1).all { value(state, it, property) == first }
    }

    fun eligible(state: GameState, ids: List<EntityId>, requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
                 minimum: Int = requirement.effectiveMinCount): List<EntityId> {
        val property = (requirement as? com.wingedsheep.sdk.scripting.targets.TargetObject)?.sameNumericProperty ?: return ids
        return groups(state, ids, property).filter { it.size >= minimum }.flatten()
    }

    /** Each group is a server-approved pool from which any subset can be chosen together. */
    fun groups(state: GameState, ids: List<EntityId>, property: CardNumericProperty): List<List<EntityId>> =
        ids.mapNotNull { id -> value(state, id, property)?.let { it to id } }
            .groupBy({ it.first }, { it.second }).values.toList()
}

/** Attach the shared equality constraint to every targeting pause, including free casts. */
fun com.wingedsheep.engine.core.TargetRequirementInfo.withNumericEquality(
    state: GameState,
    requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
    candidates: List<EntityId>,
): com.wingedsheep.engine.core.TargetRequirementInfo {
    val distributed = withOwnerDistribution(state, requirement, candidates)
    val property = (requirement as? com.wingedsheep.sdk.scripting.targets.TargetObject)?.sameNumericProperty ?: return distributed
    val groups = NumericTargetEquality.groups(state, candidates, property).filter { it.size >= minTargets }
    return distributed.copy(sameNumericProperty = property, selectionGroups = groups,
        maxTargets = minOf(maxTargets, groups.maxOfOrNull { it.size } ?: 0))
}
