package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.cost.CostPaymentService
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.PayCost

/**
 * Enumerates turn-face-up actions for face-down permanents on the battlefield.
 *
 * Turning a permanent face up is a special action that doesn't use the stack and can be done any
 * time the player has priority (CR 702.37e / 702.168d / 701.40b / 701.58b). Which cost that takes
 * is decided once, at face-down entry, by
 * [com.wingedsheep.engine.handlers.effects.FaceDownTurnUp]; this enumerator only offers what the
 * resulting [MorphDataComponent] lists. A manifested or cloaked card that also prints morph or
 * disguise lists two procedures (CR 701.40c/d, 701.58c/d) and so produces two legal actions on the
 * same permanent, distinguished by `procedureIndex`.
 *
 * Mana turn-up costs keep their rich enumeration here (X selection + auto-tap preview). Every other
 * cost is gated by a single [CostPaymentService.canAfford] check; the cost-specific selection
 * is then driven by [CostPaymentService] as a decision pause when the action is taken, rather than
 * pre-selected via [AdditionalCostData][com.wingedsheep.engine.legalactions.AdditionalCostData].
 */
class TurnFaceUpEnumerator : ActionEnumerator {

    // Lets restricted mana tagged "spend only to turn permanents face up" count toward
    // affordability of the turn-face-up special action (Overgrown Zealot, Creeping Peeper).
    private val faceUpContext = SpellPaymentContext(isTurnFaceUpAction = true)

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        for (entityId in context.battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue

            // Must be face-down
            if (!container.has<FaceDownComponent>()) continue

            // "It can't be turned face up" (Unable to Scream) suppresses the special action.
            if (state.projectedState.cantBeTurnedFaceUp(entityId)) continue

            // Must have turn-up data (to get the cost)
            val morphData = container.get<MorphDataComponent>() ?: continue

            // Morph cost increases (Exiled Doomsayer) apply to every turn-up procedure alike.
            val morphCostIncrease = context.costCalculator.calculateMorphCostIncrease(state)

            morphData.procedures.forEachIndexed { procedureIndex, procedure ->
                val cost = procedure.cost
                // Only label the mechanic when there is a choice to make; a lone procedure reads
                // the way it always has.
                val label = if (morphData.procedures.size > 1) {
                    "Turn face-up — ${procedure.label} (${cost.description})"
                } else {
                    "Turn face-up (${cost.description})"
                }
                val manaMorph = (cost as? PayCost.Atom)?.atom as? CostAtom.Mana
                when {
                    manaMorph != null -> {
                        val effectiveCost = context.costCalculator.increaseGenericCost(manaMorph.cost, morphCostIncrease)
                        if (effectiveCost.hasX) {
                            // X turn-up cost (e.g., {X}{X}{R}) — always show as available with X selection
                            val availableSources = context.manaSolver.getAvailableManaCount(state, playerId, precomputedSources = context.availableManaSources)
                            val fixedCost = effectiveCost.cmc // X contributes 0 to CMC
                            val xSymbolCount = effectiveCost.xCount.coerceAtLeast(1)
                            val maxX = ((availableSources - fixedCost) / xSymbolCount).coerceAtLeast(0)
                            result.add(
                                LegalAction(
                                    actionType = "TurnFaceUp",
                                    description = label,
                                    action = TurnFaceUp(playerId, entityId, procedureIndex = procedureIndex),
                                    manaCostString = effectiveCost.toString(),
                                    hasXCost = true,
                                    maxAffordableX = maxX
                                )
                            )
                        } else if (context.manaSolver.canPay(state, playerId, effectiveCost, spellContext = faceUpContext, precomputedSources = context.availableManaSources)) {
                            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                                context.manaSolver.solve(state, playerId, effectiveCost, spellContext = faceUpContext, precomputedSources = context.availableManaSources)
                                    ?.sources?.map { it.entityId }
                            }
                            result.add(
                                LegalAction(
                                    actionType = "ActivateAbility",
                                    description = label,
                                    action = TurnFaceUp(playerId, entityId, procedureIndex = procedureIndex),
                                    manaCostString = effectiveCost.toString(),
                                    autoTapPreview = autoTapPreview
                                )
                            )
                        }
                    }
                    // Every non-mana turn-up cost is paid through CostPaymentService at resolution, so
                    // the legal action only needs an affordability gate here — the cost-specific
                    // selection happens afterward as a decision pause (handled by the standard
                    // decision flow), not via AdditionalCostData pre-selection.
                    else -> {
                        if (CostPaymentService.canAfford(state, playerId, cost, entityId, context.manaSolver)) {
                            result.add(
                                LegalAction(
                                    actionType = "ActivateAbility",
                                    description = label,
                                    action = TurnFaceUp(playerId, entityId, procedureIndex = procedureIndex)
                                )
                            )
                        }
                    }
                }
            }
        }

        return result
    }
}
