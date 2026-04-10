package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Secret Bid Effects
// =============================================================================

/**
 * Each player secretly chooses a number. Then those numbers are revealed.
 *
 * Effects are executed per matching bidder with `xValue` = bid amount and
 * `controllerId` = that bidder. Use `ConditionalEffect(YouControlSource, ...)`
 * to gate effects that should only apply to the source's controller.
 *
 * When all players bid the same non-zero value, they are all both highest
 * and lowest — both effects fire. Zero-bids are excluded from all outcomes.
 *
 * @property highestBidderEffect Effect executed per player with the highest bid (or null)
 * @property lowestBidderEffect Effect executed per player with the lowest non-zero bid (or null)
 * @property tiedBidderEffect Effect executed per player when all non-zero bids are equal (or null)
 */
@SerialName("SecretBid")
@Serializable
data class SecretBidEffect(
    val highestBidderEffect: Effect? = null,
    val lowestBidderEffect: Effect? = null,
    val tiedBidderEffect: Effect? = null
) : Effect {
    override val description: String = buildString {
        append("Each player secretly chooses a number. Then those numbers are revealed.")
        highestBidderEffect?.let {
            append(" Each player with the highest number ${it.description}.")
        }
        lowestBidderEffect?.let {
            append(" Each player with the lowest number ${it.description}.")
        }
        tiedBidderEffect?.let {
            append(" If there is a tie, each tied player ${it.description}.")
        }
    }

    override fun applyTextReplacement(replacer: TextReplacer): Effect =
        copy(
            highestBidderEffect = highestBidderEffect?.applyTextReplacement(replacer),
            lowestBidderEffect = lowestBidderEffect?.applyTextReplacement(replacer),
            tiedBidderEffect = tiedBidderEffect?.applyTextReplacement(replacer)
        )
}
