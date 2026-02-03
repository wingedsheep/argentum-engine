package com.wingedsheep.sdk.scripting

import kotlinx.serialization.Serializable

// =============================================================================
// Library Effects
// =============================================================================

/**
 * Shuffle a card into its owner's library.
 * "Shuffle this card into its owner's library"
 */
@Serializable
data class ShuffleIntoLibraryEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Shuffle ${target.description} into its owner's library"
}

/**
 * Put a card on top of its owner's library.
 * "Put this card on top of its owner's library"
 */
@Serializable
data class PutOnTopOfLibraryEffect(
    val target: EffectTarget
) : Effect {
    override val description: String = "Put ${target.description} on top of its owner's library"
}

/**
 * Look at the top N cards and choose some to keep.
 * "Look at the top N cards of your library. Put X of them into your hand and the rest into your graveyard."
 */
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
@Serializable
data class LookAtTopAndReorderEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = "Look at the top $count cards of your library and put them back in any order"
}

/**
 * Surveil N - Look at the top N cards of your library, then put any number of them
 * into your graveyard and the rest on top of your library in any order.
 * "Surveil 2"
 */
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
@Serializable
data class MillEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        is EffectTarget.Controller -> "Mill $count"
        is EffectTarget.AnyPlayer -> "Target player mills $count"
        is EffectTarget.EachOpponent -> "Each opponent mills $count"
        is EffectTarget.Opponent -> "Target opponent mills $count"
        else -> "${target.description} mills $count"
    }
}

/**
 * Scry N - Look at the top N cards of your library, then put any number of them
 * on the bottom of your library and the rest on top in any order.
 * "Scry 2"
 */
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
@Serializable
data class LookAtTopXPutOntoBattlefieldEffect(
    val countSource: DynamicAmount,
    val filter: CardFilter,
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
@Serializable
data class ShuffleLibraryEffect(
    val target: EffectTarget = EffectTarget.Controller
) : Effect {
    override val description: String = when (target) {
        EffectTarget.Controller -> "Shuffle your library"
        EffectTarget.Opponent -> "Target opponent shuffles their library"
        else -> "Target player shuffles their library"
    }
}

/**
 * Search library for cards matching a filter.
 * "Search your library for a Forest card and put it onto the battlefield"
 *
 * Supports both legacy [CardFilter] and unified [GameObjectFilter]. When both are provided,
 * [unifiedFilter] takes precedence for evaluation.
 */
@Serializable
data class SearchLibraryEffect(
    @Deprecated("Use unifiedFilter instead")
    val filter: CardFilter = CardFilter.AnyCard,
    val count: Int = 1,
    val destination: SearchDestination = SearchDestination.HAND,
    val entersTapped: Boolean = false,
    val shuffleAfter: Boolean = true,
    val reveal: Boolean = false,
    val selectedCardIds: List<com.wingedsheep.sdk.model.EntityId>? = null,
    /** Unified filter using the new predicate-based architecture. Preferred over [filter]. */
    val unifiedFilter: GameObjectFilter? = null
) : Effect {
    override val description: String = buildString {
        val filterDesc = unifiedFilter?.description ?: filter.description
        append("Search your library for ")
        append(if (count == 1) "a" else "up to $count")
        append(" $filterDesc")
        if (count != 1) append("s")
        if (reveal) append(", reveal ${if (count == 1) "it" else "them"},")
        append(" and put ${if (count == 1) "it" else "them"} ")
        append(destination.description)
        if (entersTapped && destination == SearchDestination.BATTLEFIELD) {
            append(" tapped")
        }
        if (shuffleAfter) append(". Then shuffle your library")
    }

    companion object {
        /** Create a SearchLibraryEffect with a unified filter */
        operator fun invoke(
            unifiedFilter: GameObjectFilter,
            count: Int = 1,
            destination: SearchDestination = SearchDestination.HAND,
            entersTapped: Boolean = false,
            shuffleAfter: Boolean = true,
            reveal: Boolean = false
        ) = SearchLibraryEffect(
            filter = CardFilter.AnyCard,
            count = count,
            destination = destination,
            entersTapped = entersTapped,
            shuffleAfter = shuffleAfter,
            reveal = reveal,
            unifiedFilter = unifiedFilter
        )
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
 * Put a creature card from your hand onto the battlefield.
 * Used for effects like Kinscaer Sentry.
 */
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
