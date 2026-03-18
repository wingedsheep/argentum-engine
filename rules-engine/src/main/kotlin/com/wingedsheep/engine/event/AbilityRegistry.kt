package com.wingedsheep.engine.event

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Registry that provides triggered abilities for entities.
 */
class AbilityRegistry {
    private val abilitiesByDefinition = mutableMapOf<String, List<TriggeredAbility>>()

    /**
     * Register triggered abilities for a card definition.
     */
    fun register(cardDefinitionId: String, abilities: List<TriggeredAbility>) {
        abilitiesByDefinition[cardDefinitionId] = abilities
    }

    /**
     * Get all triggered abilities for an entity.
     */
    fun getTriggeredAbilities(entityId: EntityId, cardDefinitionId: String): List<TriggeredAbility> {
        return abilitiesByDefinition[cardDefinitionId] ?: emptyList()
    }

    /**
     * Clear all registered abilities.
     */
    fun clear() {
        abilitiesByDefinition.clear()
    }
}
