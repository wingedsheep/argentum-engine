package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.mana.SpellPaymentContext
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Enumerates face-down casting options from hand — morph (CR 702.37a) and disguise (CR 702.168a).
 *
 * Both mechanics read identically here: the card may be cast face down as a 2/2 creature at
 * sorcery speed for {3}. What differs is what the resulting permanent looks like (disguise adds
 * ward {2}) and what it costs to turn face up, and neither is this enumerator's business — the
 * mode is derived from the card at resolution by
 * [com.wingedsheep.engine.mechanics.stack.StackResolver.faceDownCastMode].
 *
 * When a face-down cast is affordable but the normal cast isn't, this also adds an unaffordable
 * normal CastSpell entry so the player sees both options.
 *
 * **Known limitation:** hand only. CR 702.37c and 702.168b both allow a face-down cast from any
 * zone the card could normally be cast from (e.g. a morph card with flashback, or one playable
 * from exile), which this enumerator does not offer. That gap predates disguise and is unchanged
 * by it.
 */
class MorphCastEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        // Casting face down is sorcery speed only
        if (!context.canPlaySorcerySpeed) return emptyList()

        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        val faceDownCost = context.costCalculator.calculateFaceDownCost(state, playerId)
        // CR 708.2 — the spell on the stack is a nameless 2/2 creature, whatever card it came
        // from, so every card in hand shares one payment context. That context is what makes
        // "spend this mana only to cast face-down spells" mana (Tin Street Gossip) count toward
        // affordability here.
        val faceDownContext = SpellPaymentContext.faceDownCast()
        val canAffordFaceDown = context.manaSolver.canPay(
            state, playerId, faceDownCost,
            spellContext = faceDownContext,
            precomputedSources = context.availableManaSources
        )
        val faceDownAutoTapPreview = if (context.skipAutoTapPreview) null else {
            context.manaSolver.solve(
                state, playerId, faceDownCost,
                spellContext = faceDownContext,
                precomputedSources = context.availableManaSources
            )?.sources?.map { it.entityId }
        }

        val hand = state.getHand(playerId)
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            // Check if the card can be cast face down at all (morph or disguise)
            val castableFaceDown = cardDef.keywordAbilities.any {
                it is KeywordAbility.Morph || it is KeywordAbility.Disguise
            }
            if (!castableFaceDown) continue

            // Add face-down cast action (affordable or not) — client shows greyed out if unaffordable
            result.add(
                LegalAction(
                    actionType = "CastFaceDown",
                    description = "Cast ${cardComponent.name} face-down",
                    action = CastSpell(playerId, cardId, castFaceDown = true),
                    affordable = canAffordFaceDown,
                    manaCostString = faceDownCost.toString(),
                    autoTapPreview = faceDownAutoTapPreview
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
