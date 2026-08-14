package com.wingedsheep.gameserver.coverage

import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual
import io.kotest.matchers.doubles.shouldBeLessThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotBeEmpty

/**
 * The denominator (canonical totals) is a committed resource; the numerator (implemented
 * cards) is the live [com.wingedsheep.mtg.sets.MtgSetCatalog]. These tests pin the join:
 * coverage is an intersection, so `implemented` can never exceed `total`, and a fully
 * implemented set reads 100%.
 */
class SetCoverageServiceTest : FunSpec({

    val service = SetCoverageService()
    val coverage = service.coverage()

    test("reports coverage for the catalogued sets") {
        coverage.shouldNotBeEmpty()
        coverage.map { it.code }.toSet().size shouldBe coverage.size // codes are unique
    }

    test("implemented never exceeds total (booster and extra) and percent is well-formed") {
        coverage.forEach { s ->
            withClue(s.code) {
                s.implemented shouldBeGreaterThanOrEqualTo 0
                s.implemented shouldBeLessThanOrEqualTo s.total
                s.extraImplemented shouldBeGreaterThanOrEqualTo 0
                s.extraImplemented shouldBeLessThanOrEqualTo s.extraTotal
                s.percent shouldBeGreaterThanOrEqual 0.0
                s.percent shouldBeLessThanOrEqual 100.0
            }
        }
    }

    test("percent is over booster (draft) cards only") {
        coverage.forEach { s ->
            val expected = if (s.total == 0) 0.0 else Math.round(s.implemented * 1000.0 / s.total) / 10.0
            withClue(s.code) { s.percent shouldBe expected }
        }
    }

    test("rows are ordered newest release first") {
        val dates = coverage.map { it.releaseDate ?: "" }
        dates shouldBe dates.sortedDescending()
    }

    test("a set with all booster cards implemented reads 100% — Bloomburrow is 266/266 draft") {
        val blb = coverage.find { it.code == "BLB" }.shouldNotBeNull()
        // 261 numbered main-set cards plus the 5 basic lands, which are Play Booster cards at
        // #262–281 even though Scryfall's preferred printing for each is the full-art promo.
        blb.total shouldBe 266
        blb.implemented shouldBe 266
        blb.percent shouldBe 100.0
        // The 13 completionist extras are reported separately, not folded into the headline %.
        blb.extraTotal shouldBe 13
    }

    test("Aetherdrift basic lands count as implemented") {
        val dft = coverage.find { it.code == "DFT" }.shouldNotBeNull()
        dft.total shouldBe 276
        dft.implemented shouldBe 276
        dft.percent shouldBe 100.0

        val detail = service.detail("DFT").shouldNotBeNull()
        val basics = setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
        detail.draft.filter { it.name in basics }.all { it.implemented } shouldBe true
    }

    test("a card counts as draft when *any* of its printings in the set is boosterable") {
        // Regression: the booster flag used to be read off a single arbitrary printing per name
        // (`unique=cards`), so a card Scryfall happened to serve as its non-booster printing was
        // filed as a completionist extra. Foundations is the worst case — 14 of its Play Booster
        // cards (collector # 1–291) also have a Beginner Box or 2026 set-extension reprint, and
        // the arbitrary pick landed on the reprint, shrinking the booster denominator to 262.
        val fdn = coverage.find { it.code == "FDN" }.shouldNotBeNull()
        fdn.total shouldBe 276
        val detail = service.detail("FDN").shouldNotBeNull()
        // Serra Angel is FDN #147 (booster) and #740 (Beginner Box); it belongs to the draft pool.
        detail.draft.map { it.name } shouldContain "Serra Angel"
        detail.extraGroups.flatMap { it.cards }.map { it.name } shouldNotContain "Serra Angel"
    }

    test("Foundations exposes selectable non-booster products from Scryfall promo types") {
        val products = service.limitedProducts("FDN")
        products["beginnerbox"].shouldNotBeNull() shouldContain "Ancestor Dragon"
        products["startercollection"].shouldNotBeNull() shouldContain "Angelic Destiny"
        products["setextension"].shouldNotBeNull().shouldNotBeEmpty()
    }

    test("a set with no booster falls back to the whole set — Bloomburrow Commander has cards, not 0%") {
        // Commander / supplemental sets flag every card booster:false, so without the fallback
        // the draft-only headline would read a useless 0/0. The whole set is the main pool instead.
        val blc = coverage.find { it.code == "BLC" }.shouldNotBeNull()
        blc.total shouldBe 312
        blc.implemented shouldBeGreaterThanOrEqualTo 1
        blc.extraTotal shouldBe 0 // no separate "extra" bucket — the set IS the main pool
    }

    test("detail lists every canonical card with an implemented flag, counts agreeing with the grid") {
        val grid = coverage.find { it.code == "BLB" }.shouldNotBeNull()
        val detail = service.detail("blb").shouldNotBeNull() // case-insensitive
        val extras = detail.extraGroups.flatMap { it.cards }
        detail.draft.size shouldBe grid.total + grid.notPlanned
        extras.size shouldBe grid.extraTotal + grid.extraNotPlanned
        detail.draft.count { it.implemented } shouldBe grid.implemented
        extras.count { it.implemented } shouldBe grid.extraImplemented
        detail.percent shouldBe grid.percent
    }

    test("extras are split into Scryfall's set-page sections, not one undifferentiated pile") {
        // Eldraine is the clean case: scryfall.com/sets/eld shows exactly these three groups of
        // non-booster cards, and each is derived here from the printings' Scryfall `promo_types`.
        val eld = service.detail("ELD").shouldNotBeNull()
        eld.extraGroups.map { it.label } shouldBe listOf("Planeswalker Decks", "Brawl Decks", "Promos")
        eld.extraGroups.map { it.cards.size } shouldBe listOf(10, 20, 1)
        // Sectioning only partitions the extras — it never moves a card in or out of the totals.
        eld.extraGroups.sumOf { it.total } shouldBe eld.extraTotal
        eld.extraGroups.sumOf { it.implemented } shouldBe eld.extraImplemented
    }

    context("cards we've decided never to implement") {
        test("drop out of the denominator, so Antiquities is complete with Bronze Tablet unbuilt") {
            // Bronze Tablet needs the ante zone the engine will never carry. Left in the total it
            // would peg ATQ at 84/85 forever; excluded, the set reads done because everything we
            // intend to build is built.
            val atq = coverage.find { it.code == "ATQ" }.shouldNotBeNull()
            atq.notPlanned shouldBe 1
            atq.implemented shouldBe atq.total
            atq.percent shouldBe 100.0
        }

        test("are still listed in the detail view, each carrying its reason") {
            val detail = service.detail("ATQ").shouldNotBeNull()
            val tablet = detail.draft.find { it.name == "Bronze Tablet" }.shouldNotBeNull()
            tablet.implemented shouldBe false
            tablet.notPlanned.shouldNotBeNull().kind shouldBe "ante"
            // Exclusion is carried as its reason, so a flagged card can never lack an explanation.
            tablet.notPlanned.shouldNotBeNull().why.shouldNotBeEmpty()
            detail.notPlanned shouldBe 1
            detail.total shouldBe detail.draft.count { it.notPlanned == null }
        }

        test("only excuse cards that are actually still missing — Arabian Nights keeps its real gaps") {
            // ARN's holes are two policy exclusions (Jeweled Bird = ante, Shahrazad = subgame) and
            // cards we could still build (City in a Bottle, Ring of Ma'rûf). Excluding the first
            // pair must not paper over the second, so the set stays short of 100%.
            val arn = coverage.find { it.code == "ARN" }.shouldNotBeNull()
            arn.notPlanned shouldBe 2
            arn.total - arn.implemented shouldBeGreaterThanOrEqualTo 2
            arn.percent shouldBeLessThan 100.0
            val detail = service.detail("ARN").shouldNotBeNull()
            detail.draft.filter { it.notPlanned != null }.map { it.name } shouldBe
                listOf("Jeweled Bird", "Shahrazad")
        }

        test("a set that reaches 100% is no longer flagged incomplete") {
            // `incomplete` gates `fullyImplemented`, which is what keeps a set out of the lobby's
            // set picker and forces the combined-pool booster path. Finishing a set and leaving the
            // flag behind silently hides a draftable set — Antiquities and Foundations both sat
            // there. Coverage knows when a set is done, so it can hold the flag honest.
            val stale = coverage
                .filter { it.total > 0 && it.implemented == it.total }
                .filter { MtgSetCatalog.byCode(it.code)?.incomplete == true }
                .map { "${it.code} (${it.implemented}/${it.total})" }
            withClue("complete sets still flagged `incomplete = true` — drop the override: $stale") {
                stale shouldBe emptyList()
            }
        }

        test("summary counts them once across every set that prints them") {
            val summary = service.summary()
            // Exclusions key on card name, so Contract from Below is one not-planned card whether
            // it shows up in Alpha, Beta or Unlimited.
            summary.distinctNotPlanned shouldBeGreaterThanOrEqualTo 1
            summary.setsComplete shouldBe coverage.count { it.percent >= 100.0 }
        }
    }

    test("detail returns null for an unknown set") {
        service.detail("ZZZ") shouldBe null
    }

    test("summary printing totals equal the sum of the per-set rows") {
        val summary = service.summary()
        summary.printingsImplemented shouldBe coverage.sumOf { it.implemented }
        summary.printingsTotal shouldBe coverage.sumOf { it.total }
        summary.setsComplete shouldBe coverage.count { it.percent >= 100.0 }
        summary.setCount shouldBe coverage.size
    }

    test("distinct dedupes reprints, so it never exceeds the printing sum but covers the same universe") {
        val summary = service.summary()
        // Reprints inflate the printing sum; deduping by name can only shrink it.
        summary.distinctImplemented shouldBeLessThanOrEqualTo summary.printingsImplemented
        summary.distinctTotal shouldBeLessThanOrEqualTo summary.printingsTotal
        // Intersection numerator can never exceed the deduped denominator.
        summary.distinctImplemented shouldBeLessThanOrEqualTo summary.distinctTotal
        summary.distinctImplemented shouldBeGreaterThanOrEqualTo 1
        val expected =
            if (summary.distinctTotal == 0) 0.0
            else Math.round(summary.distinctImplemented * 1000.0 / summary.distinctTotal) / 10.0
        summary.distinctPercent shouldBe expected
    }

    test("distinct extras are well-formed and partitioned away from the booster universe") {
        val summary = service.summary()
        summary.extraDistinctImplemented shouldBeGreaterThanOrEqualTo 0
        summary.extraDistinctImplemented shouldBeLessThanOrEqualTo summary.extraDistinctTotal
        // Extras live alongside, never inside, the booster pool — so the two distinct universes
        // partition the catalog: their sizes can't overlap into a count above the raw printing extras.
        summary.extraDistinctTotal shouldBeLessThanOrEqualTo coverage.sumOf { it.extraTotal }
        val expected =
            if (summary.extraDistinctTotal == 0) 0.0
            else Math.round(summary.extraDistinctImplemented * 1000.0 / summary.extraDistinctTotal) / 10.0
        summary.extraDistinctPercent shouldBe expected
    }

    test("progress is a non-empty, monotonically non-decreasing cumulative series") {
        val series = service.progress()
        series.shouldNotBeEmpty()
        series.last().total shouldBeGreaterThanOrEqualTo 1
        series.zipWithNext().forEach { (a, b) ->
            withClue("${a.date} -> ${b.date}") {
                b.total shouldBeGreaterThanOrEqualTo a.total // cumulative never drops
                b.total shouldBe a.total + b.added // each day's total = prior total + that day's adds
            }
        }
    }
})
