package com.wingedsheep.engine.mechanics.mana

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifyPlotCost
import com.wingedsheep.sdk.scripting.PlotCostTarget

/**
 * Computes the effective cost of the **Plot** special action (CR 718) after applying battlefield
 * [ModifyPlotCost] static abilities.
 *
 * Plot is not a spell, so [CostCalculator] (which works on `ModifySpellCost`) does not touch it;
 * this is plot's dedicated, parallel cost-reduction path. Both [com.wingedsheep.engine.legalactions.enumerators.PlotEnumerator]
 * (affordability) and [com.wingedsheep.engine.handlers.actions.ability.PlotCardHandler] (validate +
 * pay) route their plot cost through here so the two stay in lockstep.
 *
 * Only [PlotCostTarget.YouPlotFromHand] is matched today (Doc Aurlock, Grizzled Genius:
 * "Plotting cards from your hand costs {2} less"); a future "plot from top of library" reduction
 * adds a [PlotCostTarget] variant without changing call sites. Only generic [CostModification]
 * reductions/increases are meaningful for plot — the printed plot cost is a flat mana cost.
 */
class PlotCostReducer(
    private val cardRegistry: CardRegistry,
) {
    /**
     * The plot cost [plotterId] actually pays for a card plotted **from hand**, after reductions.
     * Floored at {0} generic; colored pips are never reduced below the printed requirement.
     */
    fun effectivePlotCostFromHand(state: GameState, plotterId: EntityId, baseCost: ManaCost): ManaCost {
        var totalReduction = 0
        var totalIncrease = 0
        for ((sourceId, ability) in scanBattlefield(state)) {
            if (ability.target != PlotCostTarget.YouPlotFromHand) continue
            if (state.projectedState.getController(sourceId) != plotterId) continue
            when (val mod = ability.modification) {
                is CostModification.ReduceGeneric -> totalReduction += mod.amount
                is CostModification.IncreaseGeneric -> totalIncrease += mod.amount
                else -> { /* plot only supports flat generic adjustments */ }
            }
        }
        var cost = baseCost
        if (totalIncrease > 0) cost = increaseGeneric(cost, totalIncrease)
        if (totalReduction > 0) cost = cost.reduceGeneric(totalReduction)
        return cost
    }

    private fun increaseGeneric(cost: ManaCost, increase: Int): ManaCost {
        if (increase <= 0) return cost
        val colored = cost.symbols.filter { it !is com.wingedsheep.sdk.core.ManaSymbol.Generic }
        val newGeneric = cost.genericAmount + increase
        val symbols = if (newGeneric > 0) {
            listOf(com.wingedsheep.sdk.core.ManaSymbol.Generic(newGeneric)) + colored
        } else colored
        return ManaCost(symbols)
    }

    private fun scanBattlefield(state: GameState): List<Pair<EntityId, ModifyPlotCost>> {
        val results = mutableListOf<Pair<EntityId, ModifyPlotCost>>()
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val container = state.getEntity(entityId) ?: continue
                val card = container.get<CardComponent>() ?: continue
                val def = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                val classLevel = container.get<ClassLevelComponent>()?.currentLevel
                for (ability in def.script.effectiveStaticAbilities(classLevel)) {
                    if (ability is ModifyPlotCost) results += entityId to ability
                }
            }
        }
        return results
    }
}
