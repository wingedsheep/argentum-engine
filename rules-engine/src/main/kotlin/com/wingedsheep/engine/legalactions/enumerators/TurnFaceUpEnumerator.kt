package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownTurnUpComponent
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost

/**
 * Enumerates turn-face-up actions for face-down creatures on the battlefield.
 *
 * Turning a creature face up is a special action that doesn't use the stack
 * and can be done any time the player has priority (CR 702.37e).
 *
 * Mana morph costs keep their rich enumeration here (X selection + auto-tap preview). Every other
 * morph cost is gated by a single [CostPaymentService.canAfford] check; the cost-specific selection
 * is then driven by [CostPaymentService] as a decision pause when the action is taken, rather than
 * pre-selected via [AdditionalCostData][com.wingedsheep.engine.legalactions.AdditionalCostData].
 */
class TurnFaceUpEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        for (entityId in context.battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue

            // Must be face-down
            if (!container.has<FaceDownComponent>()) continue

            // Must have morph data (to get the morph cost)
            val turnUpData = container.get<FaceDownTurnUpComponent>() ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Check if player can afford the morph cost (including any morph cost increases)
            val morphCostIncrease = context.costCalculator.calculateMorphCostIncrease(state)
            val cost = turnUpData.turnUpCost
            val manaMorph = (cost as? PayCost.Atom)?.atom as? CostAtom.Mana
            when {
                manaMorph != null -> {
                    val effectiveCost = context.costCalculator.increaseGenericCost(manaMorph.cost, morphCostIncrease)
                    if (effectiveCost.hasX) {
                        // X morph cost (e.g., {X}{X}{R}) — always show as available with X selection
                        val availableSources = context.manaSolver.getAvailableManaCount(state, playerId, precomputedSources = context.availableManaSources)
                        val fixedCost = effectiveCost.cmc // X contributes 0 to CMC
                        val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
                        val maxX = ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
                        result.add(
                            LegalAction(
                                actionType = "TurnFaceUp",
                                description = "Turn face-up (${cost.description})",
                                action = TurnFaceUp(playerId, entityId),
                                manaCostString = effectiveCost.toString(),
                                hasXCost = true,
                                maxAffordableX = maxX
                            )
                        )
                    } else if (context.manaSolver.canPay(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)) {
                        val autoTapPreview = if (context.skipAutoTapPreview) null else {
                            context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                                ?.sources?.map { it.entityId }
                        }
                        result.add(
                            LegalAction(
                                actionType = "ActivateAbility",
                                description = "Turn face-up (${cost.description})",
                                action = TurnFaceUp(playerId, entityId),
                                manaCostString = effectiveCost.toString(),
                                autoTapPreview = autoTapPreview
                            )
                        )
                    }
                }
                // Every non-mana morph cost is paid through CostPaymentService at resolution, so the
                // legal action only needs an affordability gate here — the cost-specific selection
                // happens afterward as a decision pause (handled by the standard decision flow), not
                // via AdditionalCostData pre-selection. One canAfford check replaces the former
                // per-variant target-finding branches and unlocks the variants the morph handler used
                // to reject (Tap / Choice / OwnManaCost).
                else -> {
                    if (CostPaymentService.canAfford(state, playerId, cost, entityId, context.manaSolver)) {
                        result.add(
                            LegalAction(
                                actionType = "ActivateAbility",
                                description = "Turn face-up (${cost.description})",
                                action = TurnFaceUp(playerId, entityId)
                            )
                        )
                    }
                }
            }
        }

        return result
    }
}
