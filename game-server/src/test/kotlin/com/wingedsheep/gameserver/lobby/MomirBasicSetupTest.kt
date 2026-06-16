package com.wingedsheep.gameserver.lobby

import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Unit coverage for the Momir Basic server setup helper that turns the card catalog into
 * [com.wingedsheep.sdk.core.Format.MomirBasic] data. The engine's behaviour with that data is
 * proven separately by `MomirBasicScenarioTest` in `rules-engine`.
 */
class MomirBasicSetupTest : FunSpec({

    test("fixed deck is 60 cards: 12 of each of the five basics") {
        MomirBasicSetup.fixedBasicDeck shouldBe mapOf(
            "Plains" to 12, "Island" to 12, "Swamp" to 12, "Mountain" to 12, "Forest" to 12,
        )
        MomirBasicSetup.fixedBasicDeck.values.sum() shouldBe 60
    }

    test("creature pool is sorted, de-duplicated, and creatures-only for a real set") {
        val set = MtgSetCatalog.all.first { set -> set.cards.any { it.isCreature } }
        val pool = MomirBasicSetup.creaturePool(listOf(set.code))

        pool.size shouldBeGreaterThan 0
        // Pre-sorted so the engine's seeded GameRng.pick is replay-stable.
        pool shouldBeSortedWith naturalOrder()
        // No duplicate names (reprints within a set collapse to one entry).
        pool shouldContainExactly pool.distinct()
        // Every entry is a creature actually printed in that set.
        val creatureNames = set.cards.filter { it.isCreature }.map { it.name }.toSet()
        pool.all { it in creatureNames } shouldBe true
    }

    test("creature pool unions multiple sets and skips unknown codes") {
        val twoSets = MtgSetCatalog.all.filter { set -> set.cards.any { it.isCreature } }.take(2)
        val codes = twoSets.map { it.code }

        val expected = twoSets
            .flatMap { it.cards }
            .filter { it.isCreature }
            .map { it.name }
            .distinct()
            .sorted()

        // An unknown code contributes nothing rather than throwing.
        MomirBasicSetup.creaturePool(codes + "ZZZ-not-a-set") shouldBe expected
    }

    test("allCreaturePool spans every set, sorted and de-duplicated") {
        val pool = MomirBasicSetup.allCreaturePool()
        val allCodes = MtgSetCatalog.all.map { it.code }

        pool shouldBe MomirBasicSetup.creaturePool(allCodes)
        pool shouldBeSortedWith naturalOrder()
        pool shouldContainExactly pool.distinct()
        // Strictly larger than any single set's pool (it unions them all).
        val biggestSinglePool = MtgSetCatalog.all.maxOf { MomirBasicSetup.creaturePool(listOf(it.code)).size }
        pool.size shouldBeGreaterThan biggestSinglePool
    }

    test("format() carries the full all-sets pool") {
        MomirBasicSetup.format().eligibleCreatureNames shouldBe MomirBasicSetup.allCreaturePool()
    }
})
