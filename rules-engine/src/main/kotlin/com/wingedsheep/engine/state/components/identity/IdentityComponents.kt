package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Effect
import kotlinx.serialization.Serializable

/**
 * Core identity of a card - links to its definition.
 */
@Serializable
data class CardComponent(
    val cardDefinitionId: String,
    val name: String,
    val manaCost: ManaCost,
    val typeLine: TypeLine,
    val oracleText: String = "",
    val baseStats: CreatureStats? = null,
    val baseKeywords: Set<Keyword> = emptySet(),
    val colors: Set<Color> = emptySet(),
    val ownerId: EntityId? = null,  // Original owner of the card
    val spellEffect: Effect? = null,  // Effect for instants/sorceries
    val imageUri: String? = null  // Optional image URI for card art (used for tokens)
) : Component {
    // Convenience accessors
    val isCreature: Boolean get() = typeLine.isCreature
    val isLand: Boolean get() = typeLine.isLand
    val isPermanent: Boolean get() = typeLine.isPermanent
    val isAura: Boolean get() = typeLine.isAura
    val isPlaneswalker: Boolean get() = CardType.PLANESWALKER in typeLine.cardTypes
    val manaValue: Int get() = manaCost.cmc
}

/**
 * Who owns this entity (original owner, doesn't change).
 */
@Serializable
data class OwnerComponent(
    val playerId: EntityId
) : Component

/**
 * Who controls this entity (can change via effects).
 */
@Serializable
data class ControllerComponent(
    val playerId: EntityId
) : Component

/**
 * Marks an entity as a player.
 */
@Serializable
data class PlayerComponent(
    val name: String,
    val startingLifeTotal: Int = 20
) : Component

/**
 * Player's current life total.
 */
@Serializable
data class LifeTotalComponent(
    val life: Int
) : Component

/**
 * Marks an entity as a token (not a real card).
 */
@Serializable
data object TokenComponent : Component

/**
 * Face-down status for morph/manifest.
 */
@Serializable
data object FaceDownComponent : Component

/**
 * Stores the morph cost and original card identity for face-down creatures.
 * This allows the creature to be turned face up by paying the morph cost.
 */
@Serializable
data class MorphDataComponent(
    val morphCost: ManaCost,
    val originalCardDefinitionId: String
) : Component

/**
 * Tracks which players have been revealed this card's identity.
 * Used for "look at opponent's hand" effects where the viewing player
 * should continue to see those cards even though they're in a hidden zone.
 */
@Serializable
data class RevealedToComponent(
    val playerIds: Set<EntityId>
) : Component {
    /**
     * Add a player who can see this card.
     */
    fun withPlayer(playerId: EntityId): RevealedToComponent =
        copy(playerIds = playerIds + playerId)

    /**
     * Check if a player can see this card.
     */
    fun isRevealedTo(playerId: EntityId): Boolean = playerId in playerIds

    companion object {
        /**
         * Create a component revealing the card to a single player.
         */
        fun to(playerId: EntityId): RevealedToComponent =
            RevealedToComponent(setOf(playerId))
    }
}
