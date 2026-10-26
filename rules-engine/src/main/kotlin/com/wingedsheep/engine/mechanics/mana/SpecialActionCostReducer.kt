package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.controlledLiveStaticAbilities
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpecialActionCost
import com.wingedsheep.sdk.scripting.SpecialActionKind

/**
 * Computes the effective cost of a **special action** (CR 116.2) — plotting, unlocking a door,
 * foretelling — after applying battlefield [ModifySpecialActionCost] static abilities.
 *
 * None of these actions is a spell, so [CostCalculator] (which works on `ModifySpellCost`) doesn't
 * touch them; this is their shared, parallel cost-modification path. Each action's enumerator
 * (affordability) and handler (validate + pay) route its cost through here so the two stay in
 * lockstep: plot (`PlotEnumerator` / `PlotCardHandler`), unlock (`UnlockRoomDoorEnumerator` /
 * `UnlockRoomDoorHandler`), foretell (`ForetellEnumerator` / `ForetellCardHandler`).
 *
 * Only generic [CostModification] reductions/increases are meaningful — every printed special-action
 * cost is a flat mana cost. Increases apply first, then reductions, floored at {0} generic; coloured
 * pips are never reduced.
 */
class SpecialActionCostReducer(
    private val cardRegistry: CardRegistry,
) {
    /** The cost [actorId] actually pays to take [action], whose printed cost is [baseCost]. */
    fun effectiveCost(state: GameState, actorId: EntityId, action: SpecialActionKind, baseCost: ManaCost): ManaCost {
        var totalReduction = 0
        var totalIncrease = 0
        for ((_, ability) in state.controlledLiveStaticAbilities(actorId, cardRegistry)) {
            if (ability !is ModifySpecialActionCost || ability.action != action) continue
            when (val mod = ability.modification) {
                is CostModification.ReduceGeneric -> totalReduction += mod.amount
                is CostModification.IncreaseGeneric -> totalIncrease += mod.amount
                else -> { /* special actions only support flat generic adjustments */ }
            }
        }
        var cost = baseCost
        if (totalIncrease > 0) cost = increaseGeneric(cost, totalIncrease)
        if (totalReduction > 0) cost = cost.reduceGeneric(totalReduction)
        return cost
    }

    private fun increaseGeneric(cost: ManaCost, increase: Int): ManaCost {
        val colored = cost.symbols.filter { it !is ManaSymbol.Generic }
        return ManaCost(listOf(ManaSymbol.Generic(cost.genericAmount + increase)) + colored)
    }
}
