package com.wingedsheep.engine.mechanics.cost

import com.wingedsheep.engine.core.CountersRemovedEvent
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.handlers.costs.CostAtomAmounts
import com.wingedsheep.engine.mechanics.cost.spell.SpellCosts
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/** An exact payment from the payer, never from the source permanent or another player. */
object PlayerCounterPayment {
    fun abilityAtoms(cost: AbilityCost): List<CostAtom.PayPlayerCounters> = when (cost) {
        is AbilityCost.Atom -> listOfNotNull(cost.atom as? CostAtom.PayPlayerCounters)
        is AbilityCost.Composite -> cost.costs.flatMap(::abilityAtoms)
        else -> emptyList()
    }

    /** Reserve the combined fixed payments, including repeated atoms of the same type. */
    fun canAffordAbility(state: GameState, payer: EntityId, cost: AbilityCost): Boolean =
        canAfford(state, payer, abilityAtoms(cost))

    fun canAffordSpell(state: GameState, payer: EntityId, costs: List<AdditionalCost>): Boolean =
        canAfford(state, payer, spellAtoms(costs))

    private fun spellAtoms(costs: List<AdditionalCost>): List<CostAtom.PayPlayerCounters> =
        SpellCosts.flattenComposites(costs).mapNotNull {
            (it as? AdditionalCost.Atom)?.atom as? CostAtom.PayPlayerCounters
        }

    private fun canAfford(state: GameState, payer: EntityId, atoms: List<CostAtom.PayPlayerCounters>): Boolean =
        atoms.groupBy { it.counterType }.all { (type, payments) ->
            available(state, payer, type) >= payments.sumOf {
                CostAtomAmounts.evaluate(state, it.amount)
            }
        }

    /** Additional-cost X is independent of whether the mana cost contains an X symbol. */
    fun spellMaxX(state: GameState, payer: EntityId, costs: List<AdditionalCost>): Int? {
        val atoms = spellAtoms(costs)
        val caps = atoms.groupBy { it.counterType }.mapNotNull { (type, payments) ->
            val xs = payments.count { it.amount is DynamicAmount.XValue }
            if (xs == 0) null else {
                val fixed = payments.sumOf { (it.amount as? DynamicAmount.Fixed)?.amount ?: 0 }
                (available(state, payer, type) - fixed).coerceAtLeast(0) / xs
            }
        }
        return caps.minOrNull()
    }

    fun available(state: GameState, payer: EntityId, type: CounterType): Int =
        state.getEntity(payer)?.get<CountersComponent>()?.getCount(type) ?: 0

    fun pay(state: GameState, payer: EntityId, type: CounterType, amount: Int): Pair<GameState, List<GameEvent>>? {
        if (amount < 0 || state.getEntity(payer) == null || available(state, payer, type) < amount) return null
        if (amount == 0) return state to emptyList()
        val counters = state.getEntity(payer)!!.get<CountersComponent>()!!
        return state.updateEntity(payer) { it.with(counters.withRemoved(type, amount)) } to
            listOf(CountersRemovedEvent(payer, type, amount, ""))
    }
}
