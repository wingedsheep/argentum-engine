package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.AdditionalCostData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivationRestriction

/**
 * Enumerates activated abilities on cards in the player's graveyard.
 *
 * Cards like Undead Gladiator have activated abilities that can be used
 * from the graveyard (activateFromZone == Zone.GRAVEYARD). This handles
 * cost checking for Mana, Discard, Composite (with ReturnToHand), and
 * target requirements.
 */
class GraveyardAbilityEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        val graveyardCards = state.getGraveyard(playerId)
        for (entityId in graveyardCards) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue

            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val graveyardAbilities = cardDef.script.activatedAbilities.filter {
                it.activateFromZone == Zone.GRAVEYARD
            }

            for (ability in graveyardAbilities) {
                // Check activation restrictions
                var restrictionsMet = true
                for (restriction in ability.restrictions) {
                    if (!context.castPermissionUtils.checkActivationRestriction(
                            state, playerId, restriction, entityId, ability.id
                        )
                    ) {
                        restrictionsMet = false
                        break
                    }
                }
                if (!restrictionsMet) continue

                // Check cost requirements and build cost info
                val effectiveCost = ability.cost
                var costCanBePaid = true
                val handCards = state.getZone(playerId, Zone.HAND)
                var hasDiscardCost = false

                when (effectiveCost) {
                    is AbilityCost.Mana -> {
                        if (!context.manaSolver.canPay(state, playerId, effectiveCost.cost)) costCanBePaid = false
                    }
                    is AbilityCost.Discard -> {
                        hasDiscardCost = true
                        if (handCards.isEmpty()) costCanBePaid = false
                    }
                    is AbilityCost.Composite -> {
                        for (subCost in effectiveCost.costs) {
                            when (subCost) {
                                is AbilityCost.Mana -> {
                                    if (!context.manaSolver.canPay(state, playerId, subCost.cost)) {
                                        costCanBePaid = false; break
                                    }
                                }
                                is AbilityCost.Discard -> {
                                    hasDiscardCost = true
                                    if (handCards.isEmpty()) {
                                        costCanBePaid = false; break
                                    }
                                }
                                is AbilityCost.ReturnToHand -> {
                                    val targets = context.costUtils.findAbilityBounceTargets(state, playerId, subCost.filter)
                                    if (targets.size < subCost.count) {
                                        costCanBePaid = false; break
                                    }
                                }
                                is AbilityCost.ExileSelf -> {
                                    // Always payable — the card is in the graveyard
                                }
                                else -> {}
                            }
                        }
                    }
                    else -> {}
                }
                if (!costCanBePaid) continue

                // Build cost info for bounce or discard costs
                val bounceCostFromGraveyard = when (effectiveCost) {
                    is AbilityCost.Composite -> effectiveCost.costs
                        .filterIsInstance<AbilityCost.ReturnToHand>().firstOrNull()
                    is AbilityCost.ReturnToHand -> effectiveCost
                    else -> null
                }
                val costInfo = if (bounceCostFromGraveyard != null) {
                    val bounceTargets = context.costUtils.findAbilityBounceTargets(
                        state, playerId, bounceCostFromGraveyard.filter
                    )
                    AdditionalCostData(
                        description = bounceCostFromGraveyard.description,
                        costType = "BouncePermanent",
                        validBounceTargets = bounceTargets,
                        bounceCount = bounceCostFromGraveyard.count
                    )
                } else if (hasDiscardCost) {
                    AdditionalCostData(
                        description = "Discard a card",
                        costType = "DiscardCard",
                        validDiscardTargets = handCards,
                        discardCount = 1
                    )
                } else null

                // Calculate X cost info
                val abilityManaCost = when (ability.cost) {
                    is AbilityCost.Mana -> (ability.cost as AbilityCost.Mana).cost
                    is AbilityCost.Composite -> (ability.cost as AbilityCost.Composite).costs
                        .filterIsInstance<AbilityCost.Mana>().firstOrNull()?.cost
                    else -> null
                }
                val graveyardManaCostString = abilityManaCost?.toString()
                val abilityHasXCost = abilityManaCost?.hasX == true
                val abilityMaxAffordableX: Int? = if (abilityHasXCost) {
                    val availableSources = context.manaSolver.getAvailableManaCount(state, playerId)
                    val fixedCost = abilityManaCost.cmc
                    (availableSources - fixedCost).coerceAtLeast(0)
                } else null

                // Compute auto-tap preview for UI highlighting
                val abilityAutoTapPreview = if (abilityManaCost != null && !abilityHasXCost) {
                    context.manaSolver.solve(state, playerId, abilityManaCost)?.sources?.map { it.entityId }
                } else null

                // Check for target requirements
                val targetReqs = ability.targetRequirements
                if (targetReqs.isNotEmpty()) {
                    val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                    val allSatisfied = context.targetUtils.allRequirementsSatisfied(targetInfos)
                    if (!allSatisfied) continue

                    val firstReq = targetReqs.first()
                    val firstInfo = targetInfos.first()

                    if (targetReqs.size == 1 &&
                        context.targetUtils.shouldAutoSelectPlayerTarget(firstReq, firstInfo.validTargets)
                    ) {
                        val autoSelectedTarget = ChosenTarget.Player(firstInfo.validTargets.first())
                        result.add(
                            LegalAction(
                                actionType = "ActivateAbility",
                                description = ability.description,
                                action = ActivateAbility(
                                    playerId, entityId, ability.id,
                                    targets = listOf(autoSelectedTarget)
                                ),
                                additionalCostInfo = costInfo,
                                hasXCost = abilityHasXCost,
                                maxAffordableX = abilityMaxAffordableX,
                                autoTapPreview = abilityAutoTapPreview,
                                manaCostString = graveyardManaCostString
                            )
                        )
                    } else {
                        result.add(
                            LegalAction(
                                actionType = "ActivateAbility",
                                description = ability.description,
                                action = ActivateAbility(playerId, entityId, ability.id),
                                validTargets = firstInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                                additionalCostInfo = costInfo,
                                hasXCost = abilityHasXCost,
                                maxAffordableX = abilityMaxAffordableX,
                                autoTapPreview = abilityAutoTapPreview,
                                manaCostString = graveyardManaCostString
                            )
                        )
                    }
                } else {
                    result.add(
                        LegalAction(
                            actionType = "ActivateAbility",
                            description = ability.description,
                            action = ActivateAbility(playerId, entityId, ability.id),
                            additionalCostInfo = costInfo,
                            hasXCost = abilityHasXCost,
                            maxAffordableX = abilityMaxAffordableX,
                            autoTapPreview = abilityAutoTapPreview,
                            manaCostString = graveyardManaCostString
                        )
                    )
                }
            }
        }

        return result
    }
}
