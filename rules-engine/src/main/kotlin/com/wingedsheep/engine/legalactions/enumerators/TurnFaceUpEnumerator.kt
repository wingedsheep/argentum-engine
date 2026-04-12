package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.sdk.scripting.costs.PayCost

/**
 * Enumerates turn-face-up actions for face-down creatures on the battlefield.
 *
 * Turning a creature face up is a special action that doesn't use the stack
 * and can be done any time the player has priority (CR 702.36e).
 *
 * Handles morph costs: Mana (including X), PayLife, ReturnToHand, Sacrifice,
 * Discard, RevealCard, and Exile.
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
            val morphData = container.get<MorphDataComponent>() ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            // Check if player can afford the morph cost (including any morph cost increases)
            val morphCostIncrease = context.costCalculator.calculateMorphCostIncrease(state)
            when (val cost = morphData.morphCost) {
                is PayCost.Mana -> {
                    val effectiveCost = context.costCalculator.increaseGenericCost(cost.cost, morphCostIncrease)
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
                        val autoTapSolution = context.manaSolver.solve(state, playerId, effectiveCost, precomputedSources = context.availableManaSources)
                        val autoTapPreview = autoTapSolution?.sources?.map { it.entityId }
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
                is PayCost.PayLife -> {
                    // Life-payment morph is always available (paying life that kills you is legal)
                    result.add(
                        LegalAction(
                            actionType = "ActivateAbility",
                            description = "Turn face-up (${cost.description})",
                            action = TurnFaceUp(playerId, entityId)
                        )
                    )
                }
                is PayCost.ReturnToHand -> {
                    val validTargets = context.costUtils.findReturnToHandTargets(state, playerId, cost.filter, entityId)
                    if (validTargets.size >= cost.count) {
                        result.add(
                            LegalAction(
                                actionType = "ActivateAbility",
                                description = "Turn face-up (${cost.description})",
                                action = TurnFaceUp(playerId, entityId),
                                additionalCostInfo = AdditionalCostData(
                                    description = cost.description,
                                    costType = "BouncePermanent",
                                    validSacrificeTargets = validTargets,
                                    sacrificeCount = cost.count
                                )
                            )
                        )
                    }
                }
                is PayCost.Sacrifice -> {
                    val validTargets = context.costUtils.findMorphSacrificeTargets(state, playerId, cost.filter, entityId)
                    if (validTargets.size >= cost.count) {
                        result.add(
                            LegalAction(
                                actionType = "ActivateAbility",
                                description = "Turn face-up (${cost.description})",
                                action = TurnFaceUp(playerId, entityId),
                                additionalCostInfo = AdditionalCostData(
                                    description = cost.description,
                                    costType = "SacrificePermanent",
                                    validSacrificeTargets = validTargets,
                                    sacrificeCount = cost.count
                                )
                            )
                        )
                    }
                }
                is PayCost.Discard -> {
                    val validTargets = context.costUtils.findMorphDiscardTargets(state, playerId, cost.filter)
                    if (validTargets.size >= cost.count) {
                        result.add(
                            LegalAction(
                                actionType = "ActivateAbility",
                                description = "Turn face-up (${cost.description})",
                                action = TurnFaceUp(playerId, entityId),
                                additionalCostInfo = AdditionalCostData(
                                    description = cost.description,
                                    costType = "DiscardCard",
                                    validDiscardTargets = validTargets,
                                    discardCount = cost.count
                                )
                            )
                        )
                    }
                }
                is PayCost.RevealCard -> {
                    val validTargets = context.costUtils.findMorphRevealTargets(state, playerId, cost.filter)
                    if (validTargets.size >= cost.count) {
                        result.add(
                            LegalAction(
                                actionType = "ActivateAbility",
                                description = "Turn face-up (${cost.description})",
                                action = TurnFaceUp(playerId, entityId),
                                additionalCostInfo = AdditionalCostData(
                                    description = cost.description,
                                    costType = "RevealCard",
                                    validDiscardTargets = validTargets,
                                    discardCount = cost.count
                                )
                            )
                        )
                    }
                }
                is PayCost.Exile -> {
                    val validTargets = context.costUtils.findMorphExileTargets(state, playerId, cost.filter, cost.zone)
                    if (validTargets.size >= cost.count) {
                        result.add(
                            LegalAction(
                                actionType = "ActivateAbility",
                                description = "Turn face-up (${cost.description})",
                                action = TurnFaceUp(playerId, entityId),
                                additionalCostInfo = AdditionalCostData(
                                    description = cost.description,
                                    costType = "ExileFromZone",
                                    validExileTargets = validTargets,
                                    exileMinCount = cost.count,
                                    exileMaxCount = cost.count
                                )
                            )
                        )
                    }
                }
                is PayCost.Choice -> {
                    // Choice morph costs not supported
                }
            }
        }

        return result
    }
}
