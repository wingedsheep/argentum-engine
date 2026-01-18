package com.wingedsheep.rulesengine.zone

import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import kotlinx.serialization.Serializable

@Serializable
data class ZonedCard(
    val card: CardInstance,
    val zone: ZoneType,
    val zoneOwnerId: String? = null
) {
    val id: CardId get() = card.id
    val name: String get() = card.name

    val isOnBattlefield: Boolean get() = zone == ZoneType.BATTLEFIELD
    val isInHand: Boolean get() = zone == ZoneType.HAND
    val isInLibrary: Boolean get() = zone == ZoneType.LIBRARY
    val isInGraveyard: Boolean get() = zone == ZoneType.GRAVEYARD
    val isOnStack: Boolean get() = zone == ZoneType.STACK
    val isExiled: Boolean get() = zone == ZoneType.EXILE

    fun moveTo(newZone: ZoneType, newZoneOwnerId: String? = null): ZonedCard =
        copy(zone = newZone, zoneOwnerId = newZoneOwnerId)

    companion object {
        fun inLibrary(card: CardInstance, ownerId: String): ZonedCard =
            ZonedCard(card, ZoneType.LIBRARY, ownerId)

        fun inHand(card: CardInstance, ownerId: String): ZonedCard =
            ZonedCard(card, ZoneType.HAND, ownerId)

        fun onBattlefield(card: CardInstance): ZonedCard =
            ZonedCard(card, ZoneType.BATTLEFIELD, null)

        fun inGraveyard(card: CardInstance, ownerId: String): ZonedCard =
            ZonedCard(card, ZoneType.GRAVEYARD, ownerId)

        fun onStack(card: CardInstance): ZonedCard =
            ZonedCard(card, ZoneType.STACK, null)

        fun exiled(card: CardInstance): ZonedCard =
            ZonedCard(card, ZoneType.EXILE, null)
    }
}
