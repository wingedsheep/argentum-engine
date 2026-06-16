package com.wingedsheep.mtg.sets.discovery

import com.wingedsheep.mtg.sets.definitions.scg.ScourgeSet
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Smoke test for [CardDiscovery] against the real Scourge package.
 *
 * Scourge is a small, complete, well-tested set, so it makes a good fixture:
 * if discovery agrees with the hand-maintained `ScourgeSet.cards` list, the
 * mechanism (top-level-val scanning + basic-land filtering) works end-to-end.
 *
 * Phase 2 will generalise this into an equivalence harness that runs across
 * every set in `MtgSetCatalog.all`.
 */
class CardDiscoveryTest : FunSpec({

    val scourgePackage = "com.wingedsheep.mtg.sets.definitions.scg.cards"

    test("discovers a non-empty set of cards from the Scourge package") {
        val discovered = CardDiscovery.findIn(scourgePackage)
        discovered.size shouldBeGreaterThan 0
    }

    test("discovery matches the hand-maintained ScourgeSet.cards by name") {
        val discoveredNames = CardDiscovery.findIn(scourgePackage).map { it.name }
        val manualNames = ScourgeSet.cards.map { it.name }
        discoveredNames.shouldContainExactlyInAnyOrder(manualNames)
    }

    test("discovery picks up Printing reprints declared in the cards package") {
        // EOE's reprints live in `cards/BanishingLightReprint.kt` and `cards/Annul.kt`;
        // KTK's is `cards/NaturalizeReprint.kt`. All should surface via [findPrintingsIn]
        // without a hand-maintained list.
        val eoePrintings = CardDiscovery.findPrintingsIn("com.wingedsheep.mtg.sets.definitions.eoe.cards")
        eoePrintings.map { it.name } shouldContainExactlyInAnyOrder listOf("Annul", "Banishing Light")
        eoePrintings.forEach { it.setCode shouldBe "EOE" }

        val ktkPrintings = CardDiscovery.findPrintingsIn("com.wingedsheep.mtg.sets.definitions.ktk.cards")
        ktkPrintings.map { it.name } shouldContainExactlyInAnyOrder listOf(
            "Arc Lightning",
            "Bloodstained Mire",
            "Cancel",
            "Crippling Chill",
            "Flooded Strand",
            "Incremental Growth",
            "Naturalize",
            "Polluted Delta",
            "Shatter",
        )
        ktkPrintings.forEach { it.setCode shouldBe "KTK" }
    }

    test("discovery skips Printing-typed vals as cards (and vice versa)") {
        // CardDefinition discovery must not leak the Printing entries; otherwise EOE's
        // `cards` list would be polluted with non-CardDefinition rows.
        val eoeCards = CardDiscovery.findIn("com.wingedsheep.mtg.sets.definitions.eoe.cards")
        eoeCards.map { it.name } shouldNotContain "Banishing Light"
    }
})
