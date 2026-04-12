package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.CycleCard
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Enumerates cycling and typecycling actions for cards in hand.
 *
 * For cards with cycling/typecycling, also adds a normal CastSpell action
 * (affordable with targeting or unaffordable) so the player sees both options.
 * The CastSpell actions added here are independently calculated — the coordinator
 * deduplicates any CastSpell actions that were already emitted by CastSpellEnumerator.
 */
class CyclingEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId
        val cyclingPrevented = context.cyclingPrevented

        val hand = state.getHand(playerId)
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            val allAbilities = cardDef.keywordAbilities
            val cyclingAbility = allAbilities
                .filterIsInstance<KeywordAbility.Cycling>()
                .firstOrNull()
            val typecyclingAbility = allAbilities
                .filterIsInstance<KeywordAbility.Typecycling>()
                .firstOrNull()

            if (cyclingAbility == null && typecyclingAbility == null) continue

            // Add cycling action if present and not prevented
            if (cyclingAbility != null && !cyclingPrevented) {
                val canAffordCycling = context.manaSolver.canPay(state, playerId, cyclingAbility.cost, precomputedSources = context.availableManaSources)
                val cyclingAutoTapSolution = context.manaSolver.solve(state, playerId, cyclingAbility.cost, precomputedSources = context.availableManaSources)
                val cyclingAutoTapPreview = cyclingAutoTapSolution?.sources?.map { it.entityId }
                result.add(
                    LegalAction(
                        actionType = "CycleCard",
                        description = "Cycle ${cardComponent.name}",
                        action = CycleCard(playerId, cardId),
                        affordable = canAffordCycling,
                        manaCostString = cyclingAbility.cost.toString(),
                        autoTapPreview = cyclingAutoTapPreview
                    )
                )
            }

            // Add typecycling action if present and not prevented
            if (typecyclingAbility != null && !cyclingPrevented) {
                val canAffordTypecycling = context.manaSolver.canPay(state, playerId, typecyclingAbility.cost, precomputedSources = context.availableManaSources)
                val typecyclingAutoTapSolution = context.manaSolver.solve(state, playerId, typecyclingAbility.cost, precomputedSources = context.availableManaSources)
                val typecyclingAutoTapPreview = typecyclingAutoTapSolution?.sources?.map { it.entityId }
                result.add(
                    LegalAction(
                        actionType = "TypecycleCard",
                        description = "${typecyclingAbility.type}cycling ${cardComponent.name}",
                        action = TypecycleCard(playerId, cardId),
                        affordable = canAffordTypecycling,
                        manaCostString = typecyclingAbility.cost.toString(),
                        autoTapPreview = typecyclingAutoTapPreview
                    )
                )
            }

            // For cards with cycling/typecycling, also add the normal cast option
            // This ensures the player sees both options in the cast modal
            if (!cardComponent.typeLine.isLand) {
                val isInstant = cardComponent.typeLine.isInstant
                val hasFlash = cardDef.keywords.contains(Keyword.FLASH)
                val grantedFlash = hasFlash || context.castPermissionUtils.hasGrantedFlash(state, cardId)
                val canCastTiming = isInstant || grantedFlash || context.canPlaySorcerySpeed

                if (canCastTiming) {
                    // Check if we can afford to cast normally (with cost reductions)
                    val cycleEffectiveCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                    val canAffordNormal = context.manaSolver.canPay(state, playerId, cycleEffectiveCost, precomputedSources = context.availableManaSources)

                    // If the spell requires targets, check if valid targets exist
                    val targetReqs = cardDef.script.targetRequirements
                    val hasRequiredTargets = targetReqs.any { it.effectiveMinCount > 0 }
                    val canSatisfyTargets = if (hasRequiredTargets) {
                        targetReqs.all { req ->
                            val validTargets = context.targetUtils.findValidTargets(state, playerId, req)
                            validTargets.isNotEmpty() || req.effectiveMinCount == 0
                        }
                    } else {
                        true
                    }

                    if (canAffordNormal && canSatisfyTargets && targetReqs.isNotEmpty()) {
                        // Spell is affordable and has valid targets — add with full targeting info
                        val targetInfos = context.targetUtils.buildTargetInfos(state, playerId, targetReqs)
                        val firstReq = targetReqs.first()
                        val firstInfo = targetInfos.first()
                        result.add(
                            LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                validTargets = firstInfo.validTargets,
                                requiresTargets = true,
                                targetCount = firstReq.count,
                                minTargets = firstReq.effectiveMinCount,
                                targetDescription = firstReq.description,
                                targetRequirements = if (targetInfos.size > 1) targetInfos else null,
                                affordable = true,
                                manaCostString = cycleEffectiveCost.toString()
                            )
                        )
                    } else {
                        // Spell is unaffordable or has no valid targets — show greyed out
                        result.add(
                            LegalAction(
                                actionType = "CastSpell",
                                description = "Cast ${cardComponent.name}",
                                action = CastSpell(playerId, cardId),
                                affordable = false,
                                manaCostString = cycleEffectiveCost.toString()
                            )
                        )
                    }
                } else {
                    // Wrong timing (not main phase / not active player) — show greyed out
                    val wrongTimingCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
                    result.add(
                        LegalAction(
                            actionType = "CastSpell",
                            description = "Cast ${cardComponent.name}",
                            action = CastSpell(playerId, cardId),
                            affordable = false,
                            manaCostString = wrongTimingCost.toString()
                        )
                    )
                }
            }
        }

        return result
    }
}
