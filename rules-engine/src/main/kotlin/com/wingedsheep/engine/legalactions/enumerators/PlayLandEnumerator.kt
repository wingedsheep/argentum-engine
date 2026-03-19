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

        // Lands from hand
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

        return result
    }
}
