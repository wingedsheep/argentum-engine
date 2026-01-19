package com.wingedsheep.rulesengine.ecs

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Universal identifier for all game entities.
 * Can represent cards, tokens, players, abilities, emblems, etc.
 *
 * EntityId is the single unified identifier type in the ECS architecture.
 * All game objects (players, cards, tokens, abilities) use this type.
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
    }
}
