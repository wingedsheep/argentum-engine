package com.wingedsheep.sdk.scripting.costs

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CostAtom.repeated(times)` for the two **sum-gated** graveyard exile atoms.
 *
 * Both scale their threshold rather than their count, which is the branch that is easy to get wrong:
 * every other atom multiplies a card/permanent count, and multiplying a *floor* is only correct
 * because CR 601.2f folds a repeated cost into one total the player pays once — so one exile
 * reaching `times * minTotal` satisfies the repeated cost exactly as `times` separate exiles would.
 * Escalate (CR 702.120a) is the only caller today and no printed card repeats either atom, so this
 * test is the only thing pinning the reasoning.
 */
class CostAtomRepeatedTest : FunSpec({

    val zemoCost = CostAtom.ExileFromGraveyardForTotal(
        filter = GameObjectFilter.Any.withColor(Color.BLACK),
        measure = CardMeasure.ColoredManaSymbols(listOf(Color.BLACK)),
        minTotal = 15,
    )

    test("repeating a sum-gated exile multiplies the threshold, not a card count") {
        val doubled = zemoCost.repeated(2) as CostAtom.ExileFromGraveyardForTotal
        doubled.minTotal shouldBe 30
        // The pool and the measure are unchanged — only the floor scales.
        doubled.filter shouldBe zemoCost.filter
        doubled.measure shouldBe zemoCost.measure
        // The selection is still "any number of cards": the count was never the constraint.
        doubled.selectionCount shouldBe 1
    }

    test("collecting evidence N twice is collecting evidence 2N") {
        (CostAtom.CollectEvidence(amount = 3).repeated(2) as CostAtom.CollectEvidence)
            .amount shouldBe DynamicAmount.Fixed(6)
    }

    test("a derived collect-evidence threshold can't be repeated — there is no number to double") {
        shouldThrow<IllegalArgumentException> {
            CostAtom.CollectEvidence(CostAtom.CollectEvidence.TARGET_SUM).repeated(2)
        }
    }

    test("the atom rejects an amount no cost-time evaluator could price") {
        shouldThrow<IllegalArgumentException> {
            CostAtom.CollectEvidence(DynamicAmount.Count(Player.You, Zone.HAND))
        }
    }

    test("a literal threshold prints its number; a derived one prints X, as both cards are worded") {
        CostAtom.CollectEvidence(6).description shouldBe "collect evidence 6"
        CostAtom.CollectEvidence(CostAtom.CollectEvidence.TARGET_SUM)
            .description shouldBe "collect evidence X"
    }

    test("repeating once returns the atom unchanged") {
        zemoCost.repeated(1) shouldBe zemoCost
        CostAtom.CollectEvidence(amount = 3).repeated(1) shouldBe CostAtom.CollectEvidence(3)
    }
})
