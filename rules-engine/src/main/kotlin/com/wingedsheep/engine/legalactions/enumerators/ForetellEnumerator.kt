package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Enumerates foretell actions for cards in hand (CR 702.143, Kaldheim).
 *
 * Foretell is a special action available while the controller has priority during their
 * own turn. Like plot it costs mana to set up; the setup cost is a fixed {2} rather than
 * a per-card cost. The cast-from-exile entry for an already-foretold card is emitted by
 * [CastFromZoneEnumerator] (via the may-play permission + fixed-alternative-cost component
 * added by the foretell handler), so it only appears on a later turn for the card's
 * foretell cost.
 */
class ForetellEnumerator : ActionEnumerator {

    /** The fixed setup cost to foretell a card, per CR 702.143a. */
    private val setupCost: ManaCost = ManaCost.parse("{2}")

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        if (!context.canPlaySorcerySpeed) return emptyList()

        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId
        val hand = state.getHand(playerId)
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Foretell>().firstOrNull()
                ?: continue

            val canAfford = context.manaSolver.canPay(
                state, playerId, setupCost, precomputedSources = context.availableManaSources
            )
            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(
                    state, playerId, setupCost, precomputedSources = context.availableManaSources
                )?.sources?.map { it.entityId }
            }
            result.add(
                LegalAction(
                    actionType = "ForetellCard",
                    description = "Foretell ${cardComponent.name}",
                    action = ForetellCard(playerId, cardId),
                    affordable = canAfford,
                    manaCostString = setupCost.toString(),
                    autoTapPreview = autoTapPreview
                )
            )
        }
        return result
    }
}
