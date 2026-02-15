package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Library Effects
// =============================================================================

/**
 * Look at the top N cards and choose some to keep.
 * "Look at the top N cards of your library. Put X of them into your hand and the rest into your graveyard."
 */
@SerialName("LookAtTopCards")
@Serializable
data class LookAtTopCardsEffect(
    val count: Int,
    val keepCount: Int,
    val restToGraveyard: Boolean = true
) : Effect {
    override val description: String = buildString {
        append("Look at the top $count cards of your library. ")
        append("Put $keepCount of them into your hand and the rest into your ")
        append(if (restToGraveyard) "graveyard" else "library in any order")
    }
}

/**
 * Look at the top N cards of a library and put them back in any order.
 * This is the atomic "scry-like" or "look and reorder" primitive.
 *
 * Use with CompositeEffect for patterns like Omen:
 * CompositeEffect(LookAtTopAndReorderEffect(3), MayEffect(ShuffleLibraryEffect()), DrawCardsEffect(1))
 */
@SerialName("LookAtTopAndReorder")
@Serializable
data class LookAtTopAndReorderEffect(
    val count: DynamicAmount,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    constructor(count: Int, target: EffectTarget = EffectTarget.Controller) : this(DynamicAmount.Fixed(count), target)

    override val description: String = "Look at the top ${count.description} cards of your library and put them back in any order"
}

/**
 * Surveil N - Look at the top N cards of your library, then put any number of them
 * into your graveyard and the rest on top of your library in any order.
 * "Surveil 2"
 */
@SerialName("Surveil")
@Serializable
data class SurveilEffect(
    val count: Int
) : Effect {
    override val description: String = "Surveil $count"
}

/**
 * Mill N - Put the top N cards of a player's library into their graveyard.
 * "Mill 3" or "Target player mills 3 cards"
 */
@SerialName("Mill")
@Serializable
data class MillEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Mill $count"
        else -> "${target.description.replaceFirstChar { it.uppercase() }} mills $count"
    }
}

/**
 * Scry N - Look at the top N cards of your library, then put any number of them
 * on the bottom of your library and the rest on top in any order.
 * "Scry 2"
 */
@SerialName("Scry")
@Serializable
data class ScryEffect(
    val count: Int
) : Effect {
    override val description: String = "Scry $count"
}

/**
 * Look at the top X cards of your library where X is determined dynamically,
 * and put any number of cards matching a filter onto the battlefield.
 * Then shuffle.
 *
 * Used for effects like Ajani's ultimate:
 * "Look at the top X cards of your library, where X is your life total.
 *  You may put any number of nonland permanent cards with mana value 3 or less
 *  from among them onto the battlefield. Then shuffle."
 *
 * @property countSource What determines how many cards to look at
 * @property filter Cards matching this filter may be put onto the battlefield
 * @property shuffleAfter Whether to shuffle after (typically true)
 */
@SerialName("LookAtTopXPutOntoBattlefield")
@Serializable
data class LookAtTopXPutOntoBattlefieldEffect(
    val countSource: DynamicAmount,
    val filter: GameObjectFilter,
    val shuffleAfter: Boolean = true
) : Effect {
    override val description: String = buildString {
        append("Look at the top X cards of your library, where X is ${countSource.description}. ")
        append("You may put any number of ${filter.description}s from among them onto the battlefield")
        if (shuffleAfter) append(". Then shuffle")
    }
}

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
 * Search library for cards matching a filter.
 * "Search your library for a Forest card and put it onto the battlefield"
 */
@SerialName("SearchLibrary")
@Serializable
data class SearchLibraryEffect(
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val count: Int = 1,
    val destination: SearchDestination = SearchDestination.HAND,
    val entersTapped: Boolean = false,
    val shuffleAfter: Boolean = true,
    val reveal: Boolean = false
) : Effect {
    override val description: String = buildString {
        append("Search your library for ")
        append(if (count == 1) "a" else "up to $count")
        append(" ${filter.description}")
        if (count != 1) append("s")
        if (reveal) append(", reveal ${if (count == 1) "it" else "them"},")
        append(" and put ${if (count == 1) "it" else "them"} ")
        append(destination.description)
        if (entersTapped && destination == SearchDestination.BATTLEFIELD) {
            append(" tapped")
        }
        if (shuffleAfter) append(". Then shuffle your library")
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
 * Look at top cards of target opponent's library, put some in graveyard, rest on top.
 * "Look at the top five cards of target opponent's library. Put one of them into that
 * player's graveyard and the rest back on top of their library in any order."
 * Used for Cruel Fate.
 *
 * @property count Number of cards to look at
 * @property toGraveyard Number of cards to put in graveyard
 */
@SerialName("LookAtOpponentLibrary")
@Serializable
data class LookAtOpponentLibraryEffect(
    val count: Int,
    val toGraveyard: Int = 1
) : Effect {
    override val description: String = buildString {
        append("Look at the top $count cards of target opponent's library. ")
        append("Put ${if (toGraveyard == 1) "one of them" else "$toGraveyard of them"} ")
        append("into that player's graveyard and the rest on top of their library in any order")
    }
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
 * Reveal the top N cards of your library. An opponent chooses a card matching the filter
 * from among them. Put that card onto the battlefield and the rest into your graveyard.
 *
 * Used for Animal Magnetism: "Reveal the top five cards of your library. An opponent
 * chooses a creature card from among them. Put that card onto the battlefield and
 * the rest into your graveyard."
 *
 * @property count Number of cards to reveal
 * @property filter Filter for cards the opponent can choose (e.g., Creature)
 */
@SerialName("RevealAndOpponentChooses")
@Serializable
data class RevealAndOpponentChoosesEffect(
    val count: Int,
    val filter: GameObjectFilter
) : Effect {
    override val description: String = buildString {
        append("Reveal the top $count cards of your library. ")
        append("An opponent chooses a ${filter.description} card from among them. ")
        append("Put that card onto the battlefield and the rest into your graveyard")
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
 */
@SerialName("EachPlayerMayRevealCreatures")
@Serializable
data class EachPlayerMayRevealCreaturesEffect(
    val tokenPower: Int,
    val tokenToughness: Int,
    val tokenColors: Set<com.wingedsheep.sdk.core.Color>,
    val tokenCreatureTypes: Set<String>
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

@SerialName("PutCreatureFromHandSharingTypeWithTapped")
@Serializable
data object PutCreatureFromHandSharingTypeWithTappedEffect : Effect {
    override val description: String =
        "You may put a creature card from your hand that shares a creature type " +
        "with each creature tapped this way onto the battlefield"
}
