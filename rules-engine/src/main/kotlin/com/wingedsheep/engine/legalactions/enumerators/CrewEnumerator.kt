package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.TapForPowerCreatureData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Enumerates crew actions for Vehicle permanents controlled by the player.
 *
 * A Vehicle can be crewed by tapping any number of untapped creatures whose
 * total power meets or exceeds the crew requirement. Summoning sickness does
 * NOT prevent a creature from crewing.
 */
class CrewEnumerator : ActionEnumerator {

    override fun enumerate(context: EnumerationContext): List<LegalAction> {
        val result = mutableListOf<LegalAction>()
        val state = context.state
        val playerId = context.playerId
        val projected = context.projected

        for (entityId in context.battlefieldPermanents) {
            val container = state.getEntity(entityId) ?: continue
            val cardComponent = container.get<CardComponent>() ?: continue
            val cardDef = context.cardRegistry.getCard(cardComponent.name) ?: continue

            val crewAbility = cardDef.keywordAbilities
                .filterIsInstance<KeywordAbility.Numeric>()
                .firstOrNull { it.keyword == Keyword.CREW } ?: continue

            // "Crew N. Activate only once each turn." — once it's already been crewed this turn,
            // the crew action is no longer available (Luxurious Locomotive).
            if (crewAbility.onceEachTurn) {
                val crewActivations = container
                    .get<com.wingedsheep.engine.state.components.battlefield.CrewSaddleContributorsComponent>()
                    ?.crewActivations ?: 0
                if (crewActivations >= 1) continue
            }

            // Find all untapped creatures controlled by the player that can crew
            val validCrewCreatures = mutableListOf<TapForPowerCreatureData>()
            var totalAvailablePower = 0
            for (creatureId in context.battlefieldPermanents) {
                if (creatureId == entityId) continue // Vehicle can't crew itself
                if (!projected.isCreature(creatureId)) continue
                val creatureContainer = state.getEntity(creatureId) ?: continue
                if (creatureContainer.has<TappedComponent>()) continue
                // Summoning sickness does NOT prevent crewing
                val power = projected.getPower(creatureId) ?: 0
                val creatureName = creatureContainer.get<CardComponent>()?.name ?: "Unknown"
                validCrewCreatures.add(TapForPowerCreatureData(creatureId, creatureName, power))
                totalAvailablePower += power
            }

            val canAfford = totalAvailablePower >= crewAbility.n
            result.add(
                LegalAction(
                    actionType = "CrewVehicle",
                    description = "Crew ${cardComponent.name}",
                    action = CrewVehicle(playerId, entityId, emptyList()),
                    affordable = canAfford,
                    tapForPower = true,
                    tapForPowerRequired = crewAbility.n,
                    tapForPowerCreatures = validCrewCreatures
                )
            )
        }

        return result
    }
}
