package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.targets.EffectTarget
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
