package com.wingedsheep.engine.handlers.keywords

import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.player.CardsDiscardedThisTurnComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Implements the Mayhem graveyard-cast permission check.
 *
 * A card may be cast via Mayhem only if:
 * 1. It has [KeywordAbility.Mayhem].
 * 2. It is in the casting player's graveyard.
 * 3. It was discarded by the casting player during the current turn
 *    (tracked via [CardsDiscardedThisTurnComponent]).
 */
object MayhemHandler {

    /**
     * Returns the [KeywordAbility.Mayhem] ability if [playerId] may cast [cardId] from
     * the graveyard via the Mayhem alternative cost, or null if the condition is not met.
     */
    fun mayhemAbilityFor(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        keywordAbilities: List<KeywordAbility>
    ): KeywordAbility.Mayhem? {
        val mayhem = keywordAbilities.filterIsInstance<KeywordAbility.Mayhem>().firstOrNull()
            ?: return null
        if (cardId !in state.getZone(ZoneKey(playerId, Zone.GRAVEYARD))) return null
        val discardedThisTurn = state.getEntity(playerId)
            ?.get<CardsDiscardedThisTurnComponent>()
            ?.cardIds
            ?: emptySet()
        if (cardId !in discardedThisTurn) return null
        return mayhem
    }
}
