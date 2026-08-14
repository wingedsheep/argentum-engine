package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.SaddleMount
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.legalactions.TapForPowerCreatureData
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Enumerates Saddle actions for permanents with Saddle N controlled by the player (CR 702.171a).
 *
 * Saddle reuses the same "tap any number of creatures with total power >= N" selection as Crew,
 * but with two differences: it taps any number of *other* untapped creatures the player controls
 * (the mount itself can't saddle itself), and it can be activated **only as a sorcery**. Summoning
 * sickness does not prevent a creature from saddling (it's not attacking or using a tap ability of
 * its own — the same reasoning as crewing).
 */
class SaddleEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        // "Activate only as a sorcery" (CR 702.171a): your main phase, empty stack, your turn.
        if (!context.canPlaySorcerySpeed) return emptyList()

        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId
        val projected = context.projected

        for (entityId in context.battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            // By definition id, not name — `SaddleMountHandler` resolves the saddle keyword by id,
            // so a renamed copy of a Mount (CR 707.9) would otherwise be saddleable by the engine
            // but never offered the Saddle action.
            val cardDef = context.cardRegistry.getCard(cardComponent.cardDefinitionId) ?: continue

            val saddleAbility = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Numeric>()
                .firstOrNull { it.keyword == Keyword.SADDLE } ?: continue

            // Find all other untapped creatures the player controls that can saddle this mount.
            val validSaddleCreatures = mutableListOf<TapForPowerCreatureData>()
            var totalAvailablePower = 0
            for (creatureId in context.battlefieldPermanents) {
                if (creatureId == entityId) continue // a mount can't saddle itself ("other")
                if (!projected.isCreature(creatureId)) continue
                val creatureContainer = state.getEntity(creatureId) ?: continue
                if (creatureContainer.has<TappedComponent>()) continue
                val power = projected.getPower(creatureId) ?: 0
                val creatureName = creatureContainer.get<CardComponent>()?.name ?: "Unknown"
                validSaddleCreatures.add(TapForPowerCreatureData(creatureId, creatureName, power))
                totalAvailablePower += power
            }

            val canAfford = totalAvailablePower >= saddleAbility.n
            result.add(
                LegalAction(
                    actionType = "SaddleMount",
                    description = "Saddle ${cardComponent.name}",
                    action = SaddleMount(playerId, entityId, emptyList()),
                    affordable = canAfford,
                    tapForPower = true,
                    tapForPowerRequired = saddleAbility.n,
                    tapForPowerCreatures = validSaddleCreatures
                )
            )
        }

        return result
    }
}
