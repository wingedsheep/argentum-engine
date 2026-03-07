package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * Tracks which players have been revealed this card's identity.
 * Used for "look at opponent's hand" effects where the viewing player
 * should continue to see those cards even though they're in a hidden zone.
 */
@Serializable
data class RevealedToComponent(
    val playerIds: Set<EntityId>
) : Component {
    fun withPlayer(playerId: EntityId): RevealedToComponent =
        copy(playerIds = playerIds + playerId)

    fun isRevealedTo(playerId: EntityId): Boolean = playerId in playerIds

    companion object {
        fun to(playerId: EntityId): RevealedToComponent =
            RevealedToComponent(setOf(playerId))
    }
}

/**
 * Marks a card in exile as playable by the specified player until end of turn.
 * Applied by impulse-draw effects like Mind's Desire, Chandra's +1, Act on Impulse.
 * This only grants *permission* to play from exile — it does NOT make the card free.
 * Pair with [PlayWithoutPayingCostComponent] to also waive the mana cost.
 *
 * @param controllerId The player who may play this card from exile.
 */
@Serializable
data class MayPlayFromExileComponent(
    val controllerId: EntityId
) : Component

/**
 * Marks a card as playable without paying its mana cost until end of turn.
 * Applied by effects like Mind's Desire, Cascade, Omniscience.
 * This only waives the mana cost — the card must still be in a zone where
 * it can be played (hand, or exile with [MayPlayFromExileComponent]).
 *
 * @param controllerId The player who may play this card for free.
 */
@Serializable
data class PlayWithoutPayingCostComponent(
    val controllerId: EntityId
) : Component
