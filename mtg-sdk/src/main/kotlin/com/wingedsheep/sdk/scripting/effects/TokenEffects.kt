package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import kotlinx.serialization.Serializable

// =============================================================================
// Dynamic Token Effects
// =============================================================================

/**
 * Create tokens with a dynamic count determined at resolution time.
 * "Create X 1/1 green Insect creature tokens, where X is the damage dealt."
 *
 * @property count Dynamic amount determining how many tokens to create
 * @property power Token power
 * @property toughness Token toughness
 * @property colors Token colors
 * @property creatureTypes Token creature types
 * @property keywords Keywords the token has
 * @property name Optional token name (defaults to creature types + "Token")
 * @property imageUri Optional image URI for the token artwork
 */
@Serializable
data class CreateDynamicTokensEffect(
    val count: DynamicAmount,
    val power: Int,
    val toughness: Int,
    val colors: Set<Color>,
    val creatureTypes: Set<String>,
    val keywords: Set<Keyword> = emptySet(),
    val name: String? = null,
    val imageUri: String? = null
) : Effect {
    override val description: String = buildString {
        append("Create ")
        append(count.description)
        append(" $power/$toughness ")
        append(colors.joinToString(" and ") { it.displayName.lowercase() })
        append(" ")
        append(creatureTypes.joinToString(" "))
        append(" creature token")
        append("s")
        if (keywords.isNotEmpty()) {
            append(" with ")
            append(keywords.joinToString(", ") { it.name.lowercase() })
        }
    }
}

// =============================================================================
// Token Effects
// =============================================================================

/**
 * Create token effect.
 * "Create a 1/1 white Soldier creature token"
 *
 * @property count Number of tokens to create
 * @property power Token power
 * @property toughness Token toughness
 * @property colors Token colors
 * @property creatureTypes Token creature types (e.g., "Soldier", "Kithkin")
 * @property keywords Keywords the token has (e.g., flying, vigilance)
 * @property name Optional token name (defaults to creature types + "Token")
 * @property imageUri Optional image URI for the token artwork
 */
@Serializable
data class CreateTokenEffect(
    val count: Int = 1,
    val power: Int,
    val toughness: Int,
    val colors: Set<Color>,
    val creatureTypes: Set<String>,
    val keywords: Set<Keyword> = emptySet(),
    val name: String? = null,
    val imageUri: String? = null
) : Effect {
    override val description: String = buildString {
        append("Create ")
        append(if (count == 1) "a" else "$count")
        append(" $power/$toughness ")
        append(colors.joinToString(" and ") { it.displayName.lowercase() })
        append(" ")
        append(creatureTypes.joinToString(" "))
        append(" creature token")
        if (count != 1) append("s")
        if (keywords.isNotEmpty()) {
            append(" with ")
            append(keywords.joinToString(", ") { it.name.lowercase() })
        }
    }
}

/**
 * Create Treasure artifact tokens.
 * Treasure tokens have "{T}, Sacrifice this artifact: Add one mana of any color."
 */
@Serializable
data class CreateTreasureTokensEffect(
    val count: Int = 1
) : Effect {
    override val description: String = if (count == 1) {
        "Create a Treasure token"
    } else {
        "Create $count Treasure tokens"
    }
}

/**
 * Effect that can be activated from the graveyard.
 * Used for cards like Goldmeadow Nomad with graveyard abilities.
 * Note: This is typically handled as an activated ability, not a spell effect.
 */
@Serializable
data class CreateTokenFromGraveyardEffect(
    val power: Int,
    val toughness: Int,
    val colors: Set<Color>,
    val creatureTypes: Set<String>
) : Effect {
    override val description: String = buildString {
        append("Create a $power/$toughness ")
        append(colors.joinToString(" and ") { it.displayName.lowercase() })
        append(" ")
        append(creatureTypes.joinToString(" "))
        append(" creature token")
    }
}
