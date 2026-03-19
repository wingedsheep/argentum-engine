package com.wingedsheep.engine.legalactions.enumerators

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.legalactions.ActionEnumerator
import com.wingedsheep.engine.legalactions.CrewCreatureData
import com.wingedsheep.engine.legalactions.EnumerationContext
import com.wingedsheep.engine.legalactions.LegalAction
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
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
                .filterIsInstance<KeywordAbility.Crew>()
                .firstOrNull() ?: continue

            // Find all untapped creatures controlled by the player that can crew
            val validCrewCreatures = mutableListOf<CrewCreatureData>()
            var totalAvailablePower = 0
            for (creatureId in context.battlefieldPermanents) {
                if (creatureId == entityId) continue // Vehicle can't crew itself
                if (!projected.isCreature(creatureId)) continue
                val creatureContainer = state.getEntity(creatureId) ?: continue
                if (creatureContainer.has<TappedComponent>()) continue
                // Summoning sickness does NOT prevent crewing
                val power = projected.getPower(creatureId) ?: 0
                val creatureName = creatureContainer.get<CardComponent>()?.name ?: "Unknown"
                validCrewCreatures.add(CrewCreatureData(creatureId, creatureName, power))
                totalAvailablePower += power
            }

            val canAfford = totalAvailablePower >= crewAbility.power
            result.add(
                LegalAction(
                    actionType = "CrewVehicle",
                    description = "Crew ${cardComponent.name}",
                    action = CrewVehicle(playerId, entityId, emptyList()),
                    affordable = canAfford,
                    hasCrew = true,
                    crewPower = crewAbility.power,
                    crewCreatures = validCrewCreatures
                )
            )
        }

        return result
    }
}
