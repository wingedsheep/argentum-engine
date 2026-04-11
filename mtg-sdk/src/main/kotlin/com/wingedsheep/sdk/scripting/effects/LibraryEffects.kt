package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.text.TextReplacer
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

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
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
 * Take the top card from the source permanent's linked exile pile and put it
 * into the controller's hand. Used by Parallel Thoughts and similar cards that
 * exile a pile of cards and later retrieve from it.
 *
 * If the linked exile pile is empty, nothing happens.
 */
/**
 * Repeatedly exile cards from the top of a player's library until a card matching
 * [matchFilter] is found, then put that card into the player's hand. If the matched
 * card's mana value is at least [repeatIfManaValueAtLeast], repeat the process.
 * After the process completes, the source deals damage to the player equal to the
 * number of cards put into their hand this way (multiplied by [damagePerCard]).
 *
 * Land cards exiled this way remain in exile.
 *
 * Used for Demonlord Belzenlok and similar "exile-reveal-repeat" library effects.
 */
@SerialName("ExileFromTopRepeating")
@Serializable
data class ExileFromTopRepeatingEffect(
    val matchFilter: GameObjectFilter = GameObjectFilter.Nonland,
    val repeatIfManaValueAtLeast: Int = 4,
    val damagePerCard: Int = 1
) : Effect {
    override val description: String = buildString {
        append("Exile cards from the top of your library until you exile a ${matchFilter.description} card, ")
        append("then put that card into your hand. ")
        append("If the card's mana value is $repeatIfManaValueAtLeast or greater, repeat this process. ")
        if (damagePerCard > 0) {
            append("Deal $damagePerCard damage to you for each card put into your hand this way.")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect = this
}

