package com.wingedsheep.engine.state.components.player

import com.wingedsheep.engine.state.Component
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * A player's chosen companion (CR 702.139a, 103.2b): the card they revealed from outside the game
 * before it began. Attached to the **player** at game setup; the card itself waits in its owner's
 * [Zone.SIDEBOARD] — the engine's model of "outside the game" — where, unlike the rest of that
 * private zone, it is visible to every player and spectator (it was revealed, and stays revealed).
 *
 * @property companionId the revealed companion card.
 * @property putIntoHand whether the player has taken the companion special action (CR 116.2g) —
 *   pay {3}, put the companion into hand. It may be taken only once per game, so this stays true
 *   even if the card later leaves the hand.
 */
@Serializable
data class CompanionComponent(
    val companionId: EntityId,
    val putIntoHand: Boolean = false,
) : Component

/**
 * The companion [playerId] revealed that is still waiting outside the game — revealed, not yet put
 * into hand, and still in its owner's sideboard (a wish could have fetched it some other way).
 * Null when there is none.
 */
fun GameState.companionOutsideGame(playerId: EntityId): EntityId? {
    val companion = getEntity(playerId)?.get<CompanionComponent>() ?: return null
    if (companion.putIntoHand) return null
    return companion.companionId.takeIf { it in getZone(ZoneKey(playerId, Zone.SIDEBOARD)) }
}

/** True when [entityId] is a revealed companion waiting outside the game in [ownerId]'s sideboard. */
fun GameState.isRevealedCompanion(ownerId: EntityId, entityId: EntityId): Boolean =
    companionOutsideGame(ownerId) == entityId
