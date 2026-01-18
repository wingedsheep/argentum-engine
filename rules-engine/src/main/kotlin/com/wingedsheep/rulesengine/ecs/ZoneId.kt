package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.zone.ZoneType
import kotlinx.serialization.Serializable

/**
 * Unique identifier for a game zone.
 *
 * Combines the zone type with an optional owner for player-specific zones.
 * Shared zones (battlefield, stack, exile) have no owner.
 * Player zones (library, hand, graveyard) are owned by a specific player.
 *
 * Example usage:
 * ```kotlin
 * // Shared zones (no owner)
 * val battlefield = ZoneId.BATTLEFIELD
 * val stack = ZoneId.STACK
 *
 * // Player-specific zones
 * val playerLibrary = ZoneId.library(playerId)
 * val playerHand = ZoneId.hand(playerId)
 * ```
 */
@Serializable
data class ZoneId(
    val type: ZoneType,
    val ownerId: EntityId? = null
) {
    /**
     * Whether this zone is shared by all players.
     */
    val isShared: Boolean get() = type.isShared

    /**
     * Whether this zone is owned by a specific player.
     */
    val isPlayerOwned: Boolean get() = ownerId != null

    /**
     * Whether this zone's contents are publicly visible.
     */
    val isPublic: Boolean get() = type.isPublic

    /**
     * Whether this zone's contents are hidden.
     */
    val isHidden: Boolean get() = type.isHidden

    override fun toString(): String =
        ownerId?.let { "${type.name}:${it.value}" } ?: type.name

    companion object {
        // Shared zones (no owner)
        val BATTLEFIELD = ZoneId(ZoneType.BATTLEFIELD)
        val STACK = ZoneId(ZoneType.STACK)
        val EXILE = ZoneId(ZoneType.EXILE)
        val COMMAND = ZoneId(ZoneType.COMMAND)

        /**
         * Create a library zone for a specific player.
         */
        fun library(playerId: EntityId) = ZoneId(ZoneType.LIBRARY, playerId)

        /**
         * Create a hand zone for a specific player.
         */
        fun hand(playerId: EntityId) = ZoneId(ZoneType.HAND, playerId)

        /**
         * Create a graveyard zone for a specific player.
         */
        fun graveyard(playerId: EntityId) = ZoneId(ZoneType.GRAVEYARD, playerId)

        /**
         * Create a zone from a ZoneType and optional owner string.
         * Useful for converting from old Zone representation.
         */
        fun from(type: ZoneType, ownerId: String?): ZoneId =
            ZoneId(type, ownerId?.let { EntityId.of(it) })
    }
}
