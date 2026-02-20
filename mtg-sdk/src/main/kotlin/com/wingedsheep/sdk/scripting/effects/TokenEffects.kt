package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Chosen Token Effects
// =============================================================================

/**
 * Create a creature token using the chosen color and creature type from the source permanent.
 * Power/toughness are determined by dynamic amounts evaluated at resolution time.
 *
 * Used for Riptide Replicator: "Create an X/X creature token of the chosen color and type,
 * where X is the number of charge counters on Riptide Replicator."
 *
 * Reads ChosenColorComponent and ChosenCreatureTypeComponent from the source.
 *
 * @property dynamicPower Dynamic amount for the token's power
 * @property dynamicToughness Dynamic amount for the token's toughness
 */
@SerialName("CreateChosenToken")
@Serializable
data class CreateChosenTokenEffect(
    val dynamicPower: DynamicAmount,
    val dynamicToughness: DynamicAmount
) : Effect {
    override val description: String =
        "Create an ${dynamicPower.description}/${dynamicToughness.description} creature token of the chosen color and type"
}

// =============================================================================
// Token Effects
// =============================================================================

/**
 * Create token effect.
 * "Create a 1/1 white Soldier creature token" or "Create X 1/1 green Insect creature tokens"
 *
 * Supports both fixed and dynamic counts via [DynamicAmount].
 *
 * @property count Number of tokens to create (fixed or dynamic)
 * @property power Token power
 * @property toughness Token toughness
 * @property colors Token colors
 * @property creatureTypes Token creature types (e.g., "Soldier", "Kithkin")
 * @property keywords Keywords the token has (e.g., flying, vigilance)
 * @property name Optional token name (defaults to creature types + "Token")
 * @property imageUri Optional image URI for the token artwork
 */
@SerialName("CreateToken")
@Serializable
data class CreateTokenEffect(
    val count: DynamicAmount = DynamicAmount.Fixed(1),
    val power: Int,
    val toughness: Int,
    val colors: Set<Color>,
    val creatureTypes: Set<String>,
    val keywords: Set<Keyword> = emptySet(),
    val name: String? = null,
    val imageUri: String? = null
) : Effect {
    constructor(
        count: Int,
        power: Int,
        toughness: Int,
        colors: Set<Color>,
        creatureTypes: Set<String>,
        keywords: Set<Keyword> = emptySet(),
        name: String? = null,
        imageUri: String? = null
    ) : this(DynamicAmount.Fixed(count), power, toughness, colors, creatureTypes, keywords, name, imageUri)

    override val description: String = buildString {
        append("Create ")
        when (val c = count) {
            is DynamicAmount.Fixed -> {
                append(if (c.amount == 1) "a" else "${c.amount}")
                append(" $power/$toughness ")
                append(colors.joinToString(" and ") { it.displayName.lowercase() })
                append(" ")
                append(creatureTypes.joinToString(" "))
                append(" creature token")
                if (c.amount != 1) append("s")
            }
            else -> {
                append(c.description)
                append(" $power/$toughness ")
                append(colors.joinToString(" and ") { it.displayName.lowercase() })
                append(" ")
                append(creatureTypes.joinToString(" "))
                append(" creature tokens")
            }
        }
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
@SerialName("CreateTreasureTokens")
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
@SerialName("CreateTokenFromGraveyard")
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
