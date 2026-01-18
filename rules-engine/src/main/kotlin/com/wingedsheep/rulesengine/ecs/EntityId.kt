package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.core.CardId
import com.wingedsheep.rulesengine.player.PlayerId
import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Universal identifier for all game entities.
 * Can represent cards, tokens, players, abilities, emblems, etc.
 *
 * This replaces the more specific CardId for ECS usage while maintaining
 * interoperability with existing code through conversion functions.
 */
@JvmInline
@Serializable
value class EntityId(val value: String) {

    override fun toString(): String = value

    companion object {
        /**
         * Generate a new unique entity ID.
         */
        fun generate(): EntityId = EntityId(UUID.randomUUID().toString())

        /**
         * Create an EntityId from a string value.
         */
        fun of(value: String): EntityId = EntityId(value)

        /**
         * Convert a CardId to an EntityId.
         * Used during migration from old state to ECS state.
         */
        fun fromCardId(cardId: CardId): EntityId = EntityId(cardId.value)

        /**
         * Convert a PlayerId to an EntityId.
         * Used during migration from old state to ECS state.
         */
        fun fromPlayerId(playerId: PlayerId): EntityId = EntityId(playerId.value)
    }

    /**
     * Convert this EntityId to a CardId.
     * Used during migration from ECS state back to old state.
     */
    fun toCardId(): CardId = CardId(value)

    /**
     * Convert this EntityId to a PlayerId.
     * Used during migration from ECS state back to old state.
     */
    fun toPlayerId(): PlayerId = PlayerId.of(value)
}
