package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId

/**
 * The engine's read-only answer to "what would this draft action cost, and is it payable?"
 *
 * A client building a cast or activation step by step (announce X, exile for delve, tap for
 * convoke, pick targets, then choose lands) needs the price *given the choices so far*. That
 * price is a pure function the handlers already own — the same cost pipeline `validate` runs
 * before anything is paid — so the preview is that function exposed without the execution.
 * Nothing about a preview is authoritative for the submission: the real action is validated
 * again when it arrives.
 *
 * @property manaCostString the mana still owed once every payment in the draft is credited —
 *   convoke/delve/improvise/harmonize taps and exiles, an emerge sacrifice, the per-target tax,
 *   kicker — with an announced `{X}` folded into the generic (CR 107.3a). Empty when nothing is
 *   owed.
 * @property genericRemaining the generic mana in [manaCostString]; the number of further
 *   delve exiles / improvise taps that could still buy anything.
 * @property xValue the X that will actually be paid as mana (a harmonize tap can lower it
 *   below the announced X).
 * @property affordable whether the draft, as it stands, could be paid — under auto-pay from
 *   the player's untapped sources and floating mana, or, when the draft already names its
 *   sources, by exactly those.
 * @property error why it can't be, in the handler's own words; null when [affordable].
 * @property autoTapPreview the sources the engine would tap to pay [manaCostString] under
 *   auto-pay, when it can; null when the draft isn't affordable or names its own sources.
 */
data class CostPreview(
    val manaCostString: String,
    val genericRemaining: Int,
    val xValue: Int,
    val affordable: Boolean,
    val error: String? = null,
    val autoTapPreview: List<EntityId>? = null,
) {
    companion object {
        /** A draft that can't be priced at all — the card is gone, the ability isn't there. */
        fun unavailable(reason: String): CostPreview =
            CostPreview(manaCostString = "", genericRemaining = 0, xValue = 0, affordable = false, error = reason)
    }
}
