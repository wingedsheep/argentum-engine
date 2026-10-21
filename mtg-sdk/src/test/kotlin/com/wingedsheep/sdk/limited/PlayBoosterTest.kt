package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class PlayBoosterTest : DescribeSpec({

    describe("PlayBooster") {

        it("produces packs of exactly 13 cards with a well-stocked pool") {
            val pack = PlayBooster().generate(generousPool(), Random(0))
            pack shouldHaveSize 13
        }

        it("ships 7 commons, 3 uncommons, 1 rare/mythic, and 2 wildcards") {
            repeat(50) { seed ->
                val pack = PlayBooster().generate(generousPool(), Random(seed.toLong()))
                pack shouldHaveSize 13
                // Dedicated slots are a floor; wildcards can add to any rarity bucket.
                pack.count { it.metadata.rarity == Rarity.COMMON } shouldBeGreaterThanOrEqual 7
                pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBeGreaterThanOrEqual 3
                pack.count {
                    it.metadata.rarity == Rarity.RARE || it.metadata.rarity == Rarity.MYTHIC
                } shouldBeGreaterThanOrEqual 1
            }
        }

        it("never duplicates a card name within a single pack") {
            repeat(50) { seed ->
                val pack = PlayBooster().generate(generousPool(), Random(seed.toLong()))
                pack.map { it.name }.toSet().size shouldBe pack.size
            }
        }

        it("rolls roughly 20% of wildcards as rare or mythic, so some packs carry extra rares") {
            val pool = generousPool()
            var extraRareOrMythicPacks = 0
            val runs = 500
            repeat(runs) { seed ->
                val pack = PlayBooster().generate(pool, Random(seed.toLong()))
                val rareOrMythic = pack.count {
                    it.metadata.rarity == Rarity.RARE || it.metadata.rarity == Rarity.MYTHIC
                }
                if (rareOrMythic >= 2) extraRareOrMythicPacks++
            }
            // Two ~20% wildcard slots → ~36% of packs hold a 2nd rare/mythic. Wide tolerance.
            val ratio = extraRareOrMythicPacks.toDouble() / runs
            ratio shouldBeGreaterThan 0.20
            ratio shouldBeLessThan 0.55
        }

        it("falls back gracefully when the pool has no mythics") {
            val pool = commonsPool(20) + uncommonsPool(10) + raresPool(5)
            repeat(20) { seed ->
                val pack = PlayBooster().generate(pool, Random(seed.toLong()))
                pack shouldHaveSize 13
                pack.count { it.metadata.rarity == Rarity.MYTHIC } shouldBe 0
            }
        }

        it("rolls wildcard rarities from custom weights, falling through rarities the pool lacks") {
            // An Epilogue-style pack: no commons, 2 uncommons, a rare/mythic, two 1-in-6 rare wildcards.
            val strategy = PlayBooster(
                commons = 0, uncommons = 2, raresOrMythics = 1, wildcards = 2,
                wildcardWeights = linkedMapOf(Rarity.UNCOMMON to 40.0, Rarity.RARE to 7.0, Rarity.MYTHIC to 1.0),
            )
            val pool = uncommonsPool(15) + raresPool(25) + mythicsPool(10)
            val runs = 3000
            var wildcardRares = 0
            repeat(runs) { seed ->
                val pack = strategy.generate(pool, Random(seed.toLong()))
                pack shouldHaveSize 5
                pack.map { it.name }.toSet().size shouldBe 5
                val uncommons = pack.count { it.metadata.rarity == Rarity.UNCOMMON }
                uncommons shouldBeGreaterThanOrEqual 2
                wildcardRares += 4 - uncommons
            }
            // Each of 2 wildcards per pack is a rare or mythic 1 time in 6.
            (wildcardRares.toDouble() / (runs * 2)) shouldBe (1.0 / 6 plusOrMinus 0.02)
        }

        it("keeps the default wildcard odds when none are given") {
            PlayBooster().wildcardWeights shouldBe PlayBooster.DEFAULT_WILDCARD_WEIGHTS
        }

        it("fills wildcard slots from other rarities when commons run dry") {
            // 7 commons exactly cover the dedicated common slots; wildcards must fall through.
            val pool = commonsPool(7) + uncommonsPool(10) + raresPool(5) + mythicsPool(2)
            val pack = PlayBooster().generate(pool, Random(7))
            pack shouldHaveSize 13
        }
    }
})

private fun card(name: String, rarity: Rarity): CardDefinition = CardDefinition(
    name = name,
    manaCost = ManaCost.parse("{1}"),
    typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE), subtypes = setOf(Subtype("Test"))),
    creatureStats = CreatureStats(1, 1),
    metadata = ScryfallMetadata(rarity = rarity),
)

private fun commonsPool(n: Int): List<CardDefinition> =
    (1..n).map { card("Common $it", Rarity.COMMON) }

private fun uncommonsPool(n: Int): List<CardDefinition> =
    (1..n).map { card("Uncommon $it", Rarity.UNCOMMON) }

private fun raresPool(n: Int): List<CardDefinition> =
    (1..n).map { card("Rare $it", Rarity.RARE) }

private fun mythicsPool(n: Int): List<CardDefinition> =
    (1..n).map { card("Mythic $it", Rarity.MYTHIC) }

private fun generousPool(): List<CardDefinition> =
    commonsPool(40) + uncommonsPool(20) + raresPool(10) + mythicsPool(3)
