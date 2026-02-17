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
 * Choose a creature type, then return up to N creature cards of that type from your graveyard to your hand.
 *
 * Used for Aphetto Dredging: "Return up to three target creature cards of the creature type
 * of your choice from your graveyard to your hand."
 *
 * @property count Maximum number of cards to return
 */
@SerialName("ChooseCreatureTypeReturnFromGraveyard")
@Serializable
data class ChooseCreatureTypeReturnFromGraveyardEffect(
    val count: Int = 3
) : Effect {
    override val description: String = buildString {
        append("Return up to $count target creature cards of the creature type of your choice ")
        append("from your graveyard to your hand")
    }
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
 * Choose a creature type, then reveal the top card of your library.
 * If that card is a creature card of the chosen type, put it into your hand.
 * Otherwise, put it into your graveyard.
 *
 * Used for Bloodline Shaman: "{T}: Choose a creature type. Reveal the top card
 * of your library. If that card is a creature card of the chosen type, put it
 * into your hand. Otherwise, put it into your graveyard."
 */
@SerialName("ChooseCreatureTypeRevealTop")
@Serializable
data object ChooseCreatureTypeRevealTopEffect : Effect {
    override val description: String =
        "Choose a creature type. Reveal the top card of your library. " +
        "If that card is a creature card of the chosen type, put it into your hand. " +
        "Otherwise, put it into your graveyard"
}

/**
 * Reveal cards from the top of your library until you reveal a nonland card.
 * Deal damage equal to that card's mana value to the target.
 * Put the revealed cards on the bottom of your library in any order.
 *
 * Used for Erratic Explosion: "Choose any target. Reveal cards from the top of your library
 * until you reveal a nonland card. Erratic Explosion deals damage equal to that card's
 * mana value to that permanent or player. Put the revealed cards on the bottom of your
 * library in any order."
 *
 * @property target The target to deal damage to
 */
@SerialName("RevealUntilNonlandDealDamage")
@Serializable
data class RevealUntilNonlandDealDamageEffect(
    val target: EffectTarget
) : Effect {
    override val description: String =
        "Reveal cards from the top of your library until you reveal a nonland card. " +
        "Deal damage equal to that card's mana value to ${target.description}. " +
        "Put the revealed cards on the bottom of your library in any order"
}

/**
 * Reveal cards from the top of your library until you reveal a nonland card.
 * This creature gets +X/+0 until end of turn, where X is that card's mana value.
 * Put the revealed cards on the bottom of your library in any order.
 *
 * Used for Goblin Machinist: "{2}{R}: Reveal cards from the top of your library until you
 * reveal a nonland card. Goblin Machinist gets +X/+0 until end of turn, where X is that
 * card's mana value. Put the revealed cards on the bottom of your library in any order."
 */
@SerialName("RevealUntilNonlandModifyStats")
@Serializable
data object RevealUntilNonlandModifyStatsEffect : Effect {
    override val description: String =
        "Reveal cards from the top of your library until you reveal a nonland card. " +
        "This creature gets +X/+0 until end of turn, where X is that card's mana value. " +
        "Put the revealed cards on the bottom of your library in any order"
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
 * Each opponent may put any number of cards matching a filter from their hand onto the battlefield.
 * "Each opponent may put any number of artifact, creature, enchantment, and/or land cards
 * from their hand onto the battlefield."
 *
 * Used for Tempting Wurm.
 *
 * @property filter Filter for which cards qualify (e.g., GameObjectFilter.Permanent)
 */
@SerialName("EachOpponentMayPutFromHand")
@Serializable
data class EachOpponentMayPutFromHandEffect(
    val filter: GameObjectFilter = GameObjectFilter.Permanent
) : Effect {
    override val description: String =
        "Each opponent may put any number of ${filter.description} cards from their hand onto the battlefield"
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
/**
 * Choose a creature type. Reveal cards from the top of your library until you reveal
 * a creature card of that type. Put that card onto the battlefield and shuffle the rest
 * into your library.
 *
 * Used for Riptide Shapeshifter: "{2}{U}{U}, Sacrifice Riptide Shapeshifter:
 * Choose a creature type. Reveal cards from the top of your library until you reveal
 * a creature card of that type. Put that card onto the battlefield and shuffle the
 * rest into your library."
 */
@SerialName("RevealUntilCreatureType")
@Serializable
data object RevealUntilCreatureTypeEffect : Effect {
    override val description: String =
        "Choose a creature type. Reveal cards from the top of your library until you reveal " +
        "a creature card of that type. Put that card onto the battlefield and shuffle the " +
        "rest into your library"
}

/**
 * Each player may search their library for up to X cards matching a filter,
 * reveal those cards, put them into their hand, then shuffle.
 *
 * Used for Weird Harvest: "Each player may search their library for up to X
 * creature cards, reveal those cards, put them into their hand, then shuffle."
 *
 * @property filter Filter for which cards qualify (e.g., Creature)
 * @property count How many cards each player may search for (typically DynamicAmount.XValue)
 */
@SerialName("EachPlayerSearchesLibrary")
@Serializable
data class EachPlayerSearchesLibraryEffect(
    val filter: GameObjectFilter,
    val count: DynamicAmount
) : Effect {
    override val description: String = buildString {
        append("Each player may search their library for up to ${count.description} ")
        append("${filter.description} cards, reveal those cards, put them into their hand, then shuffle")
    }
}

/**
 * For each target, reveal cards from the top of your library until you reveal a nonland card.
 * Deal damage equal to that card's mana value to that target.
 * Put the revealed cards on the bottom of your library in any order.
 *
 * Used for Kaboom!: "Choose any number of target players or planeswalkers. For each of them,
 * reveal cards from the top of your library until you reveal a nonland card, Kaboom! deals
 * damage equal to that card's mana value to that player or planeswalker, then you put the
 * revealed cards on the bottom of your library in any order."
 */
@SerialName("RevealUntilNonlandDealDamageEachTarget")
@Serializable
data object RevealUntilNonlandDealDamageEachTargetEffect : Effect {
    override val description: String =
        "For each target, reveal cards from the top of your library until you reveal a nonland card. " +
        "Deal damage equal to that card's mana value to that target. " +
        "Put the revealed cards on the bottom of your library in any order"
}

@SerialName("PutCreatureFromHandSharingTypeWithTapped")
@Serializable
data object PutCreatureFromHandSharingTypeWithTappedEffect : Effect {
    override val description: String =
        "You may put a creature card from your hand that shares a creature type " +
        "with each creature tapped this way onto the battlefield"
}
