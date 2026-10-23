package com.wingedsheep.engine.mechanics.targeting

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.ManaValues
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
            CardNumericProperty.MANA_VALUE -> if (projected.isFaceDown(id)) 0 else ManaValues.of(state, id, card)
            CardNumericProperty.COUNTERS -> state.getEntity(id)?.get<CountersComponent>()?.counters?.values?.sum() ?: 0
            CardNumericProperty.COLOR_COUNT -> if (id in state.getBattlefield()) projected.getColors(id).size
                else card.colors.size
        }
    }

    fun isSatisfied(state: GameState, ids: List<EntityId>, property: CardNumericProperty): Boolean {
        if (ids.size < 2) return true
        val first = value(state, ids.first(), property) ?: return false
        return ids.drop(1).all { value(state, it, property) == first }
    }

    /** "… with different mana values": no two of [ids] share a value of [property]. */
    fun isDistinct(state: GameState, ids: List<EntityId>, property: CardNumericProperty): Boolean {
        val values = ids.map { value(state, it, property) ?: return false }
        return values.size == values.toSet().size
    }

    fun eligible(state: GameState, ids: List<EntityId>, requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
                 minimum: Int = requirement.effectiveMinCount): List<EntityId> {
        return compatibleGroups(state, ids, requirement, minimum)?.flatten() ?: ids
    }

    /** Equality pools that can also supply every required owner. */
    fun compatibleGroups(
        state: GameState, ids: List<EntityId>,
        requirement: com.wingedsheep.sdk.scripting.targets.TargetRequirement,
        minimum: Int = requirement.effectiveMinCount,
    ): List<List<EntityId>>? {
        val property = DynamicTargetCount.objectOf(requirement)?.sameNumericProperty ?: return null
        val owners = OwnerTargetDistribution.bounds(state, requirement, ids)
        val needed = maxOf(minimum, owners?.min ?: 0)
        return groups(state, ids, property).filter { group ->
            group.size >= needed && (owners == null || OwnerTargetDistribution.groups(state, group).size >= owners.min)
        }
    }

    /** Each group is a server-approved pool from which any subset can be chosen together. */
    fun groups(state: GameState, ids: List<EntityId>, property: CardNumericProperty): List<List<EntityId>> =
        ids.mapNotNull { id -> value(state, id, property)?.let { it to id } }
            .groupBy({ it.first }, { it.second }).values.toList()
}
