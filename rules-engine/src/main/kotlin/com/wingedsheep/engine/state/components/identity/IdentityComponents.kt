package com.wingedsheep.engine.state.components.identity

import com.wingedsheep.engine.state.Component
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.costs.PayCost
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
    val baseFlags: Set<AbilityFlag> = emptySet(),
    val colors: Set<Color> = emptySet(),
    val ownerId: EntityId? = null,  // Original owner of the card
    val spellEffect: Effect? = null,  // Effect for instants/sorceries
    val imageUri: String? = null,  // Optional image URI for card art
    val cantBeCountered: Boolean = false  // Whether this spell can't be countered
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
 * Tracks that an entity is a copy of another card.
 * The originalCardDefinitionId preserves what the card originally was (e.g., Clone),
 * while copiedCardDefinitionId tracks what it's currently copying.
 */
@Serializable
data class CopyOfComponent(
    val originalCardDefinitionId: String,
    val copiedCardDefinitionId: String
) : Component

/**
 * Face-down status for morph/manifest.
 */
@Serializable
data object FaceDownComponent : Component

/**
 * Static protection from one or more colors (Rule 702.16).
 * Attached to permanents/cards that have innate protection (e.g., Disciple of Grace).
 * Dynamic protection granted by spells (e.g., Akroma's Blessing) uses floating effects instead.
 */
@Serializable
data class ProtectionComponent(
    val colors: Set<Color>,
    val subtypes: Set<String> = emptySet()
) : Component

/**
 * Stores the morph cost and original card identity for face-down creatures.
 * This allows the creature to be turned face up by paying the morph cost.
 */
@Serializable
data class MorphDataComponent(
    val morphCost: PayCost,
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

/**
 * Stores the color chosen when this permanent entered the battlefield.
 * Used by cards like Riptide Replicator ("As this artifact enters, choose a color").
 */
@Serializable
data class ChosenColorComponent(
    val color: Color
) : Component

/**
 * Stores the creature type chosen when this permanent entered the battlefield.
 * Used by cards like Doom Cannon ("As this artifact enters, choose a creature type").
 */
@Serializable
data class ChosenCreatureTypeComponent(
    val creatureType: String
) : Component

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

/**
 * Category of text replacement for Layer 3 text-changing effects.
 */
@Serializable
enum class TextReplacementCategory {
    CREATURE_TYPE,
    COLOR_WORD,
    BASIC_LAND_TYPE
}

/**
 * A single text replacement rule (e.g., "Elf" → "Goblin").
 */
@Serializable
data class TextReplacement(
    val fromWord: String,
    val toWord: String,
    val category: TextReplacementCategory
)

/**
 * Stores text replacement rules for Layer 3 text-changing effects.
 *
 * Used by cards like Artificial Evolution: "Change the text of target spell or permanent
 * by replacing all instances of one creature type with another."
 *
 * Multiple replacements can stack (e.g., two Artificial Evolutions on the same permanent).
 */
@Serializable
data class TextReplacementComponent(
    val replacements: List<TextReplacement> = emptyList()
) : Component {

    /**
     * Apply creature type replacements to a subtype string.
     */
    fun applyToCreatureType(subtype: String): String {
        var result = subtype
        for (r in replacements) {
            if (r.category == TextReplacementCategory.CREATURE_TYPE &&
                result.equals(r.fromWord, ignoreCase = true)) {
                result = r.toWord
            }
        }
        return result
    }

    /**
     * Apply creature type replacements to a Subtype.
     */
    fun applyToSubtype(subtype: Subtype): Subtype {
        val replaced = applyToCreatureType(subtype.value)
        return if (replaced == subtype.value) subtype else Subtype(replaced)
    }

    /**
     * Add a new replacement, returning a new component.
     */
    fun withReplacement(replacement: TextReplacement): TextReplacementComponent =
        copy(replacements = replacements + replacement)
}
