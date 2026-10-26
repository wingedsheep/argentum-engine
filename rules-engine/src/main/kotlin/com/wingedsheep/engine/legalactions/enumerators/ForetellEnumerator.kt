package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.handlers.actions.ability.FORETELL_SPEND_CONTEXT
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.ForetellGrants
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.SpecialActionKind

/**
 * Enumerates foretell actions for cards in hand (CR 702.143, Kaldheim).
 *
 * Foretell is a special action available any time the controller has priority during their
 * own turn (CR 116.2h) — or on any turn with a [com.wingedsheep.sdk.scripting.MayForetellOnAnyTurn]
 * (Cosmos Charger). Like plot it costs mana to set up; the setup cost is a fixed {2} rather than a
 * per-card cost, modified by `ModifySpecialActionCost(FORETELL)` statics. Which cards can be
 * foretold — printed foretell, or granted by `GrantForetellToCardsInHand` (Dream Devourer) — is
 * [ForetellGrants]' call, shared with the handler. The cast-from-exile entry for an already-foretold card is emitted by
 * [CastFromZoneEnumerator] (via the may-play permission + fixed-alternative-cost component
 * added by the foretell handler), so it only appears on a later turn for the card's
 * foretell cost.
 */
class ForetellEnumerator : ActionEnumerator {

    /** The printed setup cost to foretell a card, per CR 702.143a. */
    private val printedSetupCost: ManaCost = ManaCost.parse("{2}")

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val state = context.state
        val playerId = context.playerId
        if (!ForetellGrants.mayForetellNow(state, playerId, context.cardRegistry)) return emptyList()

        val result = mutableListOf<LegalAction>()
        val setupCost = context.specialActionCostReducer.effectiveCost(
            state, playerId, SpecialActionKind.FORETELL, printedSetupCost
        )
        // The setup cost is the same for every card, so its affordability is solved once (lazily, as
        // a hand with nothing foretellable needs neither).
        val canAfford by lazy {
            context.manaSolver.canPay(
                state, playerId, setupCost, spellContext = FORETELL_SPEND_CONTEXT,
                precomputedSources = context.availableManaSources
            )
        }
        val autoTapPreview by lazy {
            if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(
                    state, playerId, setupCost, spellContext = FORETELL_SPEND_CONTEXT,
                    precomputedSources = context.availableManaSources
                )?.sources?.map { it.entityId }
            }
        }
        val hand = state.getHand(playerId)
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            ForetellGrants.effectiveForetellCost(
                state, cardId, cardDef, playerId, context.cardRegistry, context.predicateEvaluator
            ) ?: continue

            result.add(
                LegalAction(
                    actionType = "ForetellCard",
                    description = "Foretell ${cardComponent.name}",
                    action = ForetellCard(playerId, cardId),
                    affordable = canAfford,
                    manaCostString = setupCost.toString().ifEmpty { "{0}" },
                    autoTapPreview = autoTapPreview
                )
            )
        }
        return result
    }
}
