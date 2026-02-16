package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Search target player's library for up to N cards and exile them. Then that player shuffles.
 *
 * Used for Supreme Inquisitor: "Tap five untapped Wizards you control: Search target player's
 * library for up to five cards and exile them. Then that player shuffles."
 *
 * @property count Maximum number of cards to exile
 * @property target The player whose library is being searched
 */
@SerialName("SearchTargetLibraryExile")
@Serializable
data class SearchTargetLibraryExileEffect(
    val count: Int,
    val target: EffectTarget = EffectTarget.ContextTarget(0)
) : Effect {
    override val description: String = buildString {
        append("Search target player's library for up to $count cards and exile them. ")
        append("Then that player shuffles")
    }
}
