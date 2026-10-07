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
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class EchoedPairsPlayBoosterTest : DescribeSpec({

    val pairs = (1..6).map { "Echo $it A" to "Echo $it B" }
    val echoedRarities = listOf(Rarity.UNCOMMON, Rarity.UNCOMMON, Rarity.UNCOMMON, Rarity.UNCOMMON, Rarity.RARE, Rarity.MYTHIC)
    val echoedCards = pairs.zip(echoedRarities).flatMap { (pair, rarity) ->
        listOf(card(pair.first, rarity), card(pair.second, rarity))
    }
    val echoedNames = echoedCards.map { it.name }.toSet()
    val pool = regularPool() + echoedCards
    val strategy = EchoedPairsPlayBooster(echoedPairs = pairs)

    fun completePairs(pack: List<CardDefinition>): List<Pair<String, String>> {
        val names = pack.map { it.name }.toSet()
        return pairs.filter { it.first in names && it.second in names }
    }

    describe("EchoedPairsPlayBooster") {

        it("produces 13-card packs without duplicate names") {
            repeat(200) { seed ->
                val pack = strategy.generate(pool, Random(seed.toLong()))
                pack shouldHaveSize 13
                pack.map { it.name }.toSet().size shouldBe 13
            }
        }

        it("always opens one complete echoed pair plus an echoed card from another pair") {
            repeat(500) { seed ->
                val pack = strategy.generate(pool, Random(seed.toLong()))
                // The wildcard can add a fourth echoed card, which may complete a second pair.
                completePairs(pack).size shouldBeGreaterThan 0
                pack.count { it.name in echoedNames } shouldBeGreaterThan 2
            }
        }

        it("keeps echoed cards out of the regular slots") {
            repeat(200) { seed ->
                val pack = strategy.generate(pool, Random(seed.toLong()))
                // Echoed slot (3) plus at most one wildcard.
                (pack.count { it.name in echoedNames } in 3..4) shouldBe true
                pack.count { it.metadata.rarity == Rarity.COMMON && it.name !in echoedNames } shouldBeGreaterThan 5
            }
        }

        it("never opens a pair with a half missing from the pool") {
            val withoutHalf = pool.filterNot { it.name == "Echo 1 B" }
            repeat(200) { seed ->
                val pack = strategy.generate(withoutHalf, Random(seed.toLong()))
                pack shouldHaveSize 13
                completePairs(pack).size shouldBeGreaterThan 0
                completePairs(pack).none { it.first == "Echo 1 A" } shouldBe true
            }
        }

        it("fills missing echoed slots after regular uncommons run out") {
            val withoutUncommons = regularPool().filterNot { it.metadata.rarity == Rarity.UNCOMMON }
            repeat(100) { seed ->
                val pack = strategy.generate(withoutUncommons, Random(seed))
                pack shouldHaveSize 13
                pack.map { it.name }.toSet().size shouldBe 13
            }
        }

        it("fills missing echoed slots from rares and mythics when commons also run out") {
            for (fallbackRarity in listOf(Rarity.RARE, Rarity.MYTHIC)) {
                val sparsePool = (1..8).map { card("Common $it", Rarity.COMMON) } +
                    (1..5).map { card("Fallback $it", fallbackRarity) }
                repeat(100) { seed ->
                    val pack = strategy.generate(sparsePool, Random(seed))
                    pack shouldHaveSize 13
                    pack.map { it.name }.toSet().size shouldBe 13
                }
            }
        }

        it("keeps the pack size when no echoed cards are in the pool") {
            val pack = strategy.generate(regularPool(), Random(1))
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

private fun regularPool(): List<CardDefinition> =
    (1..40).map { card("Common $it", Rarity.COMMON) } +
        (1..20).map { card("Uncommon $it", Rarity.UNCOMMON) } +
        (1..10).map { card("Rare $it", Rarity.RARE) } +
        (1..3).map { card("Mythic $it", Rarity.MYTHIC) }
