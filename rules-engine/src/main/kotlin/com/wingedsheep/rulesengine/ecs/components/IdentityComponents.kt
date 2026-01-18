package com.wingedsheep.rulesengine.ecs.components

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.ecs.Component
import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Identity components define what kind of entity this is and its core identity.
 */

/**
 * Marks an entity as a card and links it to its definition.
 *
 * All cards (both from decks and tokens created as copies) have this component.
 * The definition contains the immutable card data (name, type line, base stats, etc.)
 *
 * @property definition The immutable card definition
 * @property ownerId The player who owns this card (for ownership rules)
 */
@Serializable
data class CardComponent(
    val definition: CardDefinition,
    val ownerId: EntityId
) : Component {
    val name: String get() = definition.name
    val isCreature: Boolean get() = definition.isCreature
    val isLand: Boolean get() = definition.isLand
    val isPermanent: Boolean get() = definition.isPermanent
    val isEnchantment: Boolean get() = definition.typeLine.isEnchantment
    val isArtifact: Boolean get() = definition.typeLine.isArtifact
    val isAura: Boolean get() = definition.typeLine.isAura
    val isEquipment: Boolean get() = definition.typeLine.isEquipment
    val isInstant: Boolean get() = definition.typeLine.isInstant
    val isSorcery: Boolean get() = definition.typeLine.isSorcery
}

/**
 * Marks an entity as a player.
 *
 * Players are entities too in the ECS model, allowing them to have
 * components attached (life, mana pool, poison counters, etc.)
 *
 * @property name The player's display name
 */
@Serializable
data class PlayerComponent(
    val name: String
) : Component

/**
 * Marks an entity as a token (created during the game, not from a deck).
 *
 * Tokens differ from regular cards in several rules ways:
 * - They cease to exist when they leave the battlefield
 * - They don't go to the graveyard (briefly exist there then cease to exist)
 * - They are not part of a deck
 *
 * @property createdBy The entity that created this token (for "tokens you created" effects)
 */
@Serializable
data class TokenComponent(
    val createdBy: EntityId? = null
) : Component
