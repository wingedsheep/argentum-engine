package com.wingedsheep.engine.handlers.actions.ability

import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.CrewSaddleCharacteristic
import com.wingedsheep.sdk.scripting.CrewSaddleContribution

/**
 * Reads the value a creature contributes toward a crew or saddle cost.
 *
 * Printed and granted instances are both considered: printed abilities cover ordinary cards, while
 * token text is stored in [GameState.grantedStaticAbilities]. Multiple instances do not stack; each
 * is an alternative way to determine the contribution, so the controller gets the greatest value.
 */
internal object CrewSaddleContributionEvaluator {
    fun evaluate(
        state: GameState,
        projected: ProjectedState,
        cardRegistry: CardRegistry,
        creatureId: EntityId
    ): Int {
        val printed = state.getEntity(creatureId)
            ?.get<CardComponent>()
            ?.let { cardRegistry.getCard(it.cardDefinitionId) }
            ?.staticAbilities
            .orEmpty()
        val granted = state.grantedStaticAbilities
            .asSequence()
            .filter { it.entityId == creatureId }
            .map { it.ability }
            .toList()
        val alternatives = (printed + granted).filterIsInstance<CrewSaddleContribution>()

        val actualPower = projected.getPower(creatureId) ?: 0
        if (alternatives.isEmpty()) return actualPower

        return alternatives.maxOf { ability ->
            val base = when (ability.characteristic) {
                CrewSaddleCharacteristic.POWER -> actualPower
                CrewSaddleCharacteristic.TOUGHNESS -> projected.getToughness(creatureId) ?: 0
            }
            base + ability.modifier
        }
    }
}
