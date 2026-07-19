package com.wingedsheep.gameserver

import com.wingedsheep.gameserver.coverage.SetCoverageService
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Verifies the Set Completion view's `inStandard` flag, baked into `set-totals.json` by
 * `scripts/gen-set-totals` from the full canonical Scryfall pool (a core/expansion/draft-innovation
 * set whose cards are majority Standard-legal — the same rule as `scripts/card-status`).
 *
 * These assertions pin both ends of the split against the real bundled resource: a set that is
 * *currently* in Standard vs. an old or rotated one. Because the flag is computed over the whole
 * canonical pool rather than the implemented-card legality mirror, it is correct even for a set we've
 * only partially implemented (see the all-supplemental MSH case).
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

    test("an all-supplemental Standard set (no booster pool) is flagged in Standard") {
        // Marvel Super Heroes is a Standard-legal expansion whose cards are all `booster: false`
        // (no draft pool) and which we've only partially implemented. A per-implemented-card
        // derivation would under-count it; the baked full-pool flag gets it right.
        inStandard("MSH") shouldBe true
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
