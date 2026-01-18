package com.wingedsheep.rulesengine.zone

import com.wingedsheep.rulesengine.core.CardId
import kotlinx.serialization.Serializable

@Serializable
data class ZoneTransition(
    val cardId: CardId,
    val cardName: String,
    val from: ZoneType,
    val to: ZoneType,
    val fromOwnerId: String?,
    val toOwnerId: String?
) {
    val entersBattlefield: Boolean
        get() = from != ZoneType.BATTLEFIELD && to == ZoneType.BATTLEFIELD

    val leavesBattlefield: Boolean
        get() = from == ZoneType.BATTLEFIELD && to != ZoneType.BATTLEFIELD

    val dies: Boolean
        get() = from == ZoneType.BATTLEFIELD && to == ZoneType.GRAVEYARD

    val isDrawn: Boolean
        get() = from == ZoneType.LIBRARY && to == ZoneType.HAND

    val isDiscarded: Boolean
        get() = from == ZoneType.HAND && to == ZoneType.GRAVEYARD

    val isCast: Boolean
        get() = from == ZoneType.HAND && to == ZoneType.STACK

    val isExiled: Boolean
        get() = to == ZoneType.EXILE && from != ZoneType.EXILE

    val returnsToHand: Boolean
        get() = to == ZoneType.HAND && from != ZoneType.HAND && from != ZoneType.LIBRARY

    companion object {
        fun draw(cardId: CardId, cardName: String, ownerId: String): ZoneTransition =
            ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.LIBRARY,
                to = ZoneType.HAND,
                fromOwnerId = ownerId,
                toOwnerId = ownerId
            )

        fun cast(cardId: CardId, cardName: String, ownerId: String): ZoneTransition =
            ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.HAND,
                to = ZoneType.STACK,
                fromOwnerId = ownerId,
                toOwnerId = null
            )

        fun resolve(cardId: CardId, cardName: String): ZoneTransition =
            ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.STACK,
                to = ZoneType.BATTLEFIELD,
                fromOwnerId = null,
                toOwnerId = null
            )

        fun die(cardId: CardId, cardName: String, ownerId: String): ZoneTransition =
            ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = ZoneType.BATTLEFIELD,
                to = ZoneType.GRAVEYARD,
                fromOwnerId = null,
                toOwnerId = ownerId
            )

        fun exile(cardId: CardId, cardName: String, from: ZoneType, fromOwnerId: String?): ZoneTransition =
            ZoneTransition(
                cardId = cardId,
                cardName = cardName,
                from = from,
                to = ZoneType.EXILE,
                fromOwnerId = fromOwnerId,
                toOwnerId = null
            )
    }
}
