package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.mechanics.ModalDfcCasts
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId

class PlayLandEnumerator : ActionEnumerator {
    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        if (!context.canPlayLand) return emptyList()

        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId

        // Lands from hand — suppressed by Memory Vessel's "they can't play cards from their hand"
        // (hand-scoped only; the graveyard/exile loops below are unaffected).
        if (!context.cantPlayCardsFromHand) {
            val hand = state.getHand(playerId)
            for (cardId in hand) {
                val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                result.addAll(landPlays(context, cardId, cardComponent))
            }
        }

        // Lands from graveyard (Muldrotha, Crucible of Worlds)
        if (context.castPermissionUtils.hasGraveyardPlayPermissionForType(state, playerId, "LAND")) {
            val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))
            for (cardId in graveyardCards) {
                val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                result.addAll(landPlays(context, cardId, cardComponent, sourceZone = "GRAVEYARD"))
            }
        } else {
            // A `MayCastFromGraveyard(playsLands = true)` grant (Serra Paragon) covers only the land
            // cards its filter admits, and only while its per-turn allowance is unspent — asked per
            // card through the same resolver `PlayLandHandler` authorizes with. Lands only: the
            // grant's filter is matched against the card in the graveyard, i.e. its front face.
            val zones = com.wingedsheep.engine.handlers.actions.spell.CastZoneResolver(
                context.cardRegistry, context.conditionEvaluator, context.legality
            )
            for (cardId in state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))) {
                val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                if (!cardComponent.typeLine.isLand) continue
                if (zones.graveyardLandPlayGrant(state, playerId, cardId) == null) continue
                result.addAll(landPlays(context, cardId, cardComponent, sourceZone = "GRAVEYARD"))
            }
        }

        // Lands played from the graveyard via Mayhem (CR 702.187c): a Mayhem land you discarded
        // this turn is *played* (not cast) at no cost. Oscorp Industries. `context.canPlayLand`
        // (checked above) already enforces land timing — main phase, empty stack, a land drop left.
        val discardedThisTurn = state.getEntity(playerId)
            ?.get<com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent>()
            ?.cardIds ?: emptyList()
        if (discardedThisTurn.isNotEmpty()) {
            val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))
            for (cardId in graveyardCards) {
                if (cardId !in discardedThisTurn) continue
                val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                if (!cardComponent.typeLine.isLand) continue
                if (context.cantPlayLand(cardId)) continue
                val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue
                if (com.wingedsheep.engine.mechanics.MayhemGrants.effectiveMayhem(state, cardId, cardDef) == null) continue
                result.add(LegalAction(
                    actionType = "PlayLand",
                    description = "Play ${cardComponent.name} (Mayhem)",
                    action = PlayLand(playerId, cardId),
                    sourceZone = "GRAVEYARD"
                ))
            }
        }

        // Lands exiled with a permanent granting "you may play cards exiled with this" (Valgavoth,
        // Hauken's Insight). Each candidate goes back through `landGranterFor` so the offer and the
        // handler's authorization answer the same question — filter included.
        val seenLinkedLands = mutableSetOf<com.wingedsheep.sdk.model.EntityId>()
        for (granter in com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExilePlayUtils
            .landGranters(state, playerId, context.legality)) {
            for (exiledId in granter.exiledIds) {
                if (!seenLinkedLands.add(exiledId)) continue
                val cardComponent = state.getEntity(exiledId)?.get<CardComponent>() ?: continue
                if (!cardComponent.typeLine.isLand) continue
                if (!com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExilePlayUtils
                        .canPlayLand(state, playerId, exiledId, context.legality)
                ) continue
                result.addAll(landPlays(context, exiledId, cardComponent, sourceZone = "EXILE"))
            }
        }

        return result
    }

    /**
     * Every land play [cardId] offers from [sourceZone] — one per land *face*, not one per card.
     *
     * A single-faced land is one action, as before. A modal double-faced card is up to two, because
     * CR 712.12 makes the face a choice taken on the way in: *"A player playing a modal double-faced
     * card as a land chooses one of its faces that's a land before putting it onto the battlefield.
     * It enters the battlefield with that face up."* Riverglide Pathway // Lavaglide Pathway is the
     * cycle this exists for — one card in hand, two entries in the drag-to-play menu, each naming
     * the face it plays so the choice is legible rather than a mode prompt after the fact.
     *
     * The front offer is gated on [cardComponent]'s own type line, which off the battlefield is the
     * front face's (CR 712.8a) — so a modal DFC whose *front* is a spell and whose back is a land
     * correctly offers only the back.
     */
    private fun landPlays(
        context: EnumerationContext,
        cardId: EntityId,
        cardComponent: CardComponent,
        sourceZone: String? = null,
    ): List<LegalAction> {
        // Hot path: legal-action enumeration runs on every priority pass and every AI/MCTS node,
        // and the hand loop now reaches this for *every* card rather than only for lands. A card
        // that is neither a land nor double-faced can offer no land play, and answering that from
        // two booleans already on the CardComponent keeps the registry lookup below off the common
        // path entirely.
        if (!cardComponent.typeLine.isLand && !cardComponent.isDoubleFaced) return emptyList()

        // A filtered "players can't play <these> lands" lock (City in a Bottle). Whole-card, not
        // per-face: the predicates it can carry — an originally-printed-in set, a name — belong to
        // the card, so a locked modal DFC offers neither of its faces. Cheap in the common case,
        // where `EnumerationContext` has already cached that no filtered lock is in play at all.
        if (context.cantPlayLand(cardId)) return emptyList()

        val result = mutableListOf<LegalAction>()
        if (cardComponent.typeLine.isLand) {
            result.add(LegalAction(
                actionType = "PlayLand",
                description = "Play ${cardComponent.name}",
                action = PlayLand(context.playerId, cardId),
                sourceZone = sourceZone
            ))
        }
        val backLand = ModalDfcCasts.landFace(
            context.cardRegistry.getCard(cardComponent.cardDefinitionId)
        )
        if (backLand != null) {
            result.add(LegalAction(
                actionType = "PlayLand",
                description = "Play ${backLand.name}",
                action = PlayLand(context.playerId, cardId, asBackFace = true),
                sourceZone = sourceZone
            ))
        }
        return result
    }
}
