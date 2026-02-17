package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Library Effects
// =============================================================================

/**
 * Shuffle a player's library.
 * "Shuffle your library" or "Target player shuffles their library"
 */
@SerialName("ShuffleLibrary")
@Serializable
data class ShuffleLibraryEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Shuffle your library"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} shuffles their library"
    }
}


/**
 * Destination for searched cards.
 */
@Serializable
enum class SearchDestination(val description: String) {
    HAND("into your hand"),
    BATTLEFIELD("onto the battlefield"),
    GRAVEYARD("into your graveyard"),
    TOP_OF_LIBRARY("on top of your library")
}

/**
 * Put a creature card from your hand onto the battlefield.
 * Used for effects like Kinscaer Sentry.
 */
@SerialName("PutCreatureFromHandOntoBattlefield")
@Serializable
data class PutCreatureFromHandOntoBattlefieldEffect(
    val maxManaValueSource: DynamicAmount,
    val entersTapped: Boolean = false,
    val entersAttacking: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Put a creature card with mana value ${maxManaValueSource.description} or less ")
        append("from your hand onto the battlefield")
        if (entersTapped) append(" tapped")
        if (entersAttacking) append(" and attacking")
    }
}


/**
 * Shuffle target player's graveyard into their library.
 * "Target player shuffles their graveyard into their library."
 */
@SerialName("ShuffleGraveyardIntoLibrary")
@Serializable
data class ShuffleGraveyardIntoLibraryEffect(
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Shuffle your graveyard into your library"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} shuffles their graveyard into their library"
    }
}

/**
 * Put a land card from your hand onto the battlefield.
 * "You may put a basic land card from your hand onto the battlefield tapped."
 *
 * @property filter Filter for which land cards qualify (default: BasicLand)
 * @property entersTapped Whether the land enters tapped
 */
@SerialName("PutLandFromHandOntoBattlefield")
@Serializable
data class PutLandFromHandOntoBattlefieldEffect(
    val filter: GameObjectFilter = GameObjectFilter.BasicLand,
    val entersTapped: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Put a ${filter.description} card from your hand onto the battlefield")
        if (entersTapped) append(" tapped")
    }
}

/**
 * Each player may reveal any number of creature cards from their hand.
 * Then each player creates tokens for each card they revealed this way.
 *
 * Used for Kamahl's Summons: "Each player may reveal any number of creature cards
 * from their hand. Then each player creates a 2/2 green Bear creature token for
 * each card they revealed this way."
 *
 * @property tokenPower Power of the created tokens
 * @property tokenToughness Toughness of the created tokens
 * @property tokenColors Colors of the created tokens
 * @property tokenCreatureTypes Creature types of the created tokens
 * @property tokenImageUri Optional image URI for the token artwork
 */
@SerialName("EachPlayerMayRevealCreatures")
@Serializable
data class EachPlayerMayRevealCreaturesEffect(
    val tokenPower: Int,
    val tokenToughness: Int,
    val tokenColors: Set<com.wingedsheep.sdk.core.Color>,
    val tokenCreatureTypes: Set<String>,
    val tokenImageUri: String? = null
) : Effect {
    override val description: String =
        "Each player may reveal any number of creature cards from their hand. " +
        "Then each player creates a $tokenPower/$tokenToughness " +
        "${tokenColors.joinToString(" and ") { it.displayName.lowercase() }} " +
        "${tokenCreatureTypes.joinToString(" ")} creature token for each card they revealed this way"
}

/**
 * You may put a creature card from your hand that shares a creature type
 * with each creature tapped as part of the cost onto the battlefield.
 *
 * Used for Cryptic Gateway: "Tap two untapped creatures you control:
 * You may put a creature card from your hand that shares a creature type
 * with each creature tapped this way onto the battlefield."
 *
 * Requires tappedPermanents in EffectContext to determine valid choices.
 */

@SerialName("PutCreatureFromHandSharingTypeWithTapped")
@Serializable
data object PutCreatureFromHandSharingTypeWithTappedEffect : Effect {
    override val description: String =
        "You may put a creature card from your hand that shares a creature type " +
        "with each creature tapped this way onto the battlefield"
}
