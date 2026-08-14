package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.SuspendCardFromHand
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Enumerates suspend actions for cards in hand (CR 702.62, Time Spiral).
 *
 * Suspend is a special action (CR 116.2f) available any time the controller could begin to
 * cast the card by putting it on the stack — instant speed for an instant or a card with
 * flash, sorcery speed otherwise — independent of whether the card's own mana cost could
 * ever actually be paid (Ancestral Vision has none). The exile-side countdown and eventual
 * free cast are driven entirely by the engine's synthesized
 * [com.wingedsheep.sdk.scripting.Suspend.countdownAbility] once [SuspendCardFromHandHandler]
 * marks the card suspended, so no cast-from-exile enumeration is needed here.
 */
class SuspendEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId
        val hand = state.getHand(playerId)
        for (cardId in hand) {
            val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue
            val suspend = cardDef.keywordAbilities.filterIsInstance<KeywordAbility.Suspend>().firstOrNull()
                ?: continue

            // CR 702.62c: "take into consideration any effects that would prohibit that card from
            // being cast" — the same blanket/per-spell cast-prohibition check every other cast
            // enumerator consults, even though suspend never actually casts the card.
            if (context.cantCastSpell(cardId)) continue

            // Printed flash is read off the CardDefinition, not projected state: projection is
            // only ever built for battlefield entities, so hasKeyword() on a hand-zone card
            // silently returns false regardless of what's actually printed. A battlefield-granted
            // flash (GrantFlashToSpellType, e.g. Quick Sliver) counts too.
            val hasFlash = cardDef.keywords.contains(Keyword.FLASH) ||
                context.castPermissionUtils.hasGrantedFlash(state, cardId)
            val isInstantSpeed = cardComponent.typeLine.isInstant || hasFlash
            if (!isInstantSpeed && !context.canPlaySorcerySpeed) continue

            val canAfford = context.manaSolver.canPay(
                state, playerId, suspend.cost, precomputedSources = context.availableManaSources
            )
            val autoTapPreview = if (context.skipAutoTapPreview) null else {
                context.manaSolver.solve(
                    state, playerId, suspend.cost, precomputedSources = context.availableManaSources
                )?.sources?.map { it.entityId }
            }
            result.add(
                LegalAction(
                    actionType = "SuspendCardFromHand",
                    description = "Suspend ${cardComponent.name}",
                    action = SuspendCardFromHand(playerId, cardId),
                    affordable = canAfford,
                    manaCostString = suspend.cost.toString(),
                    autoTapPreview = autoTapPreview
                )
            )
        }
        return result
    }
}
