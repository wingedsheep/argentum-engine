package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.coverage.SetCoverageService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies the Set Completion view's `inStandard` flag, which is *derived* from the bundled per-card
 * Scryfall legalities (there is no per-set Standard source — see [SetCoverageService.isInStandard]).
 *
 * The derivation requires a majority of a set's booster pool to be Standard-legal, which separates a
 * set that is *currently* in Standard (essentially all of its own cards are legal) from an old or
 * rotated set that merely keeps a few reprinted staples legal. These assertions pin both ends of
 * that bimodal split against the real bundled legality data, so a regression (e.g. reverting to
 * "any card is legal") would flip the back catalog on and fail here.
 */
class SetCoverageStandardTest : FunSpec({

    val coverage = SetCoverageService().coverage().associateBy { it.code }

    fun inStandard(code: String): Boolean =
        (coverage[code] ?: error("no coverage row for $code")).inStandard

    test("recent expansion / core sets are flagged in Standard") {
        // As of this corpus, Bloomburrow, Duskmourn and Foundations are Standard-legal.
        inStandard("BLB") shouldBe true
        inStandard("DSK") shouldBe true
        inStandard("FDN") shouldBe true
    }

    test("old and rotated-out sets are not flagged in Standard") {
        // Decades-old sets keep a handful of reprinted staples legal but are nowhere near a majority.
        inStandard("LEA") shouldBe false // Alpha (1993)
        inStandard("ONS") shouldBe false // Onslaught (2002)
        inStandard("10E") shouldBe false // Tenth Edition (2007), an all-reprint core set
        // Dominaria United (2022) has rotated out of Standard.
        inStandard("DMU") shouldBe false
    }
})
