package com.wingedsheep.sdk.scripting.effects

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================================
// Open-Bid Effects
// =============================================================================

/**
 * Open life-bidding auction between the caster and another participant.
 *
 * "You and [participant] bid life. You start the bidding with a bid of 1. In turn order, each
 * player may top the high bid. The bidding ends if the high bid stands. The high bidder loses
 * life equal to the high bid. If you win the bidding, [onWin]."
 *
 * The two bidders alternate topping the bid. When a player declines to top, the bid stands and
 * the high bidder loses that much life. [onWin] runs **only** when the caster is the high bidder,
 * with the original targets in context (so a `CounterEffect` counters the spell that was bid over).
 *
 * Mages' Contest is the canonical user: it bids "you" against the targeted spell's controller
 * (`participant = Player.ControllerOf("target spell")`) and counters that spell on a win. Other
 * cards can bid against any resolved opponent by supplying a different [participant] reference.
 *
 * @property onWin Effect executed if the caster wins the auction (e.g. counter the targeted spell)
 * @property participant The other bidder, resolved against the effect context. If it resolves to
 *   the caster (or to nobody), the caster is the sole bidder and wins at the opening bid of 1.
 */
@SerialName("OpenLifeBid")
@Serializable
data class OpenLifeBidEffect(
    val onWin: Effect,
    val participant: Player = Player.Opponent
) : Effect {
    override val description: String =
        "You and ${participant.description} bid life. You start the bidding with a bid of 1. " +
            "In turn order, each player may top the high bid. The bidding ends if the high bid stands. " +
            "The high bidder loses life equal to the high bid. If you win the bidding, " +
            "${onWin.description.replaceFirstChar(Char::lowercase)}."

    override fun applyTextReplacement(replacer: TextReplacer): Effect {
        val newOnWin = onWin.applyTextReplacement(replacer)
        return if (newOnWin !== onWin) copy(onWin = newOnWin) else this
    }
}
