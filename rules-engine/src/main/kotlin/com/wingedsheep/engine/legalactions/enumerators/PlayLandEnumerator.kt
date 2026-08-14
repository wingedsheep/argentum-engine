package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Zone

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
                if (cardComponent.typeLine.isLand) {
                    result.add(LegalAction(
                        actionType = "PlayLand",
                        description = "Play ${cardComponent.name}",
                        action = PlayLand(playerId, cardId)
                    ))
                }
            }
        }

        // Lands from graveyard (Muldrotha)
        if (context.castPermissionUtils.hasGraveyardPlayPermissionForType(state, playerId, "LAND")) {
            val graveyardCards = state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))
            for (cardId in graveyardCards) {
                val cardComponent = state.getEntity(cardId)?.get<CardComponent>() ?: continue
                if (cardComponent.typeLine.isLand) {
                    result.add(LegalAction(
                        actionType = "PlayLand",
                        description = "Play ${cardComponent.name}",
                        action = PlayLand(playerId, cardId),
                        sourceZone = "GRAVEYARD"
                    ))
                }
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

        // Lands exiled with a permanent granting "you may play cards exiled with this" (Valgavoth).
        val seenLinkedLands = mutableSetOf<com.wingedsheep.sdk.model.EntityId>()
        for (granter in com.wingedsheep.engine.handlers.effects.linkedexile.LinkedExilePlayUtils
            .landGranters(state, playerId, context.cardRegistry)) {
            for (exiledId in granter.exiledIds) {
                if (!seenLinkedLands.add(exiledId)) continue
                val cardComponent = state.getEntity(exiledId)?.get<CardComponent>() ?: continue
                if (!cardComponent.typeLine.isLand) continue
                if (granter.ability.ownedByYou && cardComponent.ownerId != playerId) continue
                val inExile = state.turnOrder.any { pid -> exiledId in state.getZone(ZoneKey(pid, Zone.EXILE)) }
                if (!inExile) continue
                result.add(LegalAction(
                    actionType = "PlayLand",
                    description = "Play ${cardComponent.name}",
                    action = PlayLand(playerId, exiledId),
                    sourceZone = "EXILE"
                ))
            }
        }

        return result
    }
}
