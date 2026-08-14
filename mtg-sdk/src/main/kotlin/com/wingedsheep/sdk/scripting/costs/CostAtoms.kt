package com.wingedsheep.sdk.scripting.costs

import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Operations over the shared [CostAtom] vocabulary.
 */

/**
 * This atom as it reads when the same cost has to be paid [times] times over — "discard a card"
 * paid twice is "discard two cards".
 *
 * Magic never asks for the same cost twice as two separate payments: CR 601.2f folds repeated
 * costs into one total the player pays once, and every selection channel the engine carries a
 * payment through ([com.wingedsheep.sdk.scripting.AdditionalCostPayment.discardedCards] and its
 * siblings) is a single flat list. Two copies of `Discard(1)` in a cost list would therefore both
 * be satisfied by one discarded card; one `Discard(2)` is the shape that validates and charges
 * correctly.
 *
 * The caller with a repeated cost today is **escalate** (CR 702.120a) with a non-mana cost —
 * `ModalEffect.additionalCostPerExtraMode`, paid once for each mode chosen beyond the first.
 *
 * `times = 0` yields a cost with nothing to pay, and `times = 1` returns the atom unchanged.
 *
 * @throws IllegalArgumentException for a [CostAtom.RemoveCounters] whose count is a runtime
 *   [DynamicAmount] — a variable amount has no fixed number to multiply. No printed repeated cost
 *   removes a variable number of counters.
 */
fun CostAtom.repeated(times: Int): CostAtom {
    require(times >= 0) { "Cannot repeat a cost a negative number of times: $times" }
    if (times == 1) return this
    return when (this) {
        is CostAtom.Mana -> copy(cost = cost * times)
        is CostAtom.PayLife -> copy(amount = amount * times)
        is CostAtom.Mill -> copy(count = count * times)
        is CostAtom.Sacrifice -> copy(count = count * times)
        is CostAtom.VariablePermanents -> copy(minCount = minCount * times)
        is CostAtom.Discard -> copy(count = count * times)
        is CostAtom.ExileFrom -> copy(count = count * times)
        is CostAtom.TapPermanents -> copy(count = count * times)
        is CostAtom.ReturnToHand -> copy(count = count * times)
        is CostAtom.RemoveCounters -> {
            val fixed = count as? DynamicAmount.Fixed
                ?: throw IllegalArgumentException(
                    "Cannot repeat a counter-removal cost with a variable count: $count"
                )
            copy(count = DynamicAmount.Fixed(fixed.amount * times))
        }
        is CostAtom.PutCountersOnSelf -> copy(count = count * times)
        is CostAtom.RevealFromHand -> copy(count = count * times)
        // Collecting evidence N twice is collecting evidence 2N: CR 601.2f folds the repeated cost
        // into one payment, and one exile of total mana value 2N satisfies that just as two
        // separate exiles of N would. (No printed card repeats it — escalate is the only caller —
        // but the threshold multiplies cleanly, so there's nothing to reject.)
        is CostAtom.CollectEvidence -> copy(amount = amount * times)
    }
}
