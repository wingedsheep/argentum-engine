package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Enumerates face-down morph casting options from hand.
 *
 * Morph cards can be cast face-down as 2/2 creatures at sorcery speed for {3}.
 * When a morph is affordable but the normal cast isn't, this also adds an
 * unaffordable normal CastSpell entry so the player sees both options.
 */
class MorphCastEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        // Morph face-down is sorcery speed only
        if (!context.canPlaySorcerySpeed) return emptyList()

        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        val morphCost = context.costCalculator.calculateFaceDownCost(state, playerId)
        val canAffordMorph = context.manaSolver.canPay(state, playerId, morphCost, precomputedSources = context.availableManaSources)
        val morphAutoTapPreview = if (context.skipAutoTapPreview) null else {
            context.manaSolver.solve(state, playerId, morphCost, precomputedSources = context.availableManaSources)
                ?.sources?.map { it.entityId }
        }

        val hand = state.getHand(playerId)
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            // Check if card has Morph keyword
            val hasMorph = cardDef.keywordAbilities.any { it is KeywordAbility.Morph }
            if (!hasMorph) continue

            // Add morph action (affordable or not) — client shows greyed out if unaffordable
            result.add(
                LegalAction(
                    actionType = "CastFaceDown",
                    description = "Cast ${cardComponent.name} face-down",
                    action = CastSpell(playerId, cardId, castFaceDown = true),
                    affordable = canAffordMorph,
                    manaCostString = morphCost.toString(),
                    autoTapPreview = morphAutoTapPreview
                )
            )

            // Check if we can afford to cast normally — if not, add unaffordable cast action
            // This ensures the player sees both options in the cast modal
            val normalEffectiveCost = context.costCalculator.calculateEffectiveCost(state, cardDef, playerId)
            val canAffordNormal = context.manaSolver.canPay(state, playerId, normalEffectiveCost, precomputedSources = context.availableManaSources)
            if (!canAffordNormal) {
                result.add(
                    LegalAction(
                        actionType = "CastSpell",
                        description = "Cast ${cardComponent.name}",
                        action = CastSpell(playerId, cardId),
                        affordable = false,
                        manaCostString = normalEffectiveCost.toString()
                    )
                )
            }
        }

        return result
    }
}
