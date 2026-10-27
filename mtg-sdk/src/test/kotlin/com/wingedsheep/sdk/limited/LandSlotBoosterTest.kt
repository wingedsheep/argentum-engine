package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class LandSlotBoosterTest : DescribeSpec({
    val duals = (1..10).map { land("Dual $it", "$it", basic = false) }
    val basics = (1..25).map { land("Basic ${(it - 1) / 5}", "$it", basic = true) }
    val ordinary = (1..71).map {
        CardDefinition(manaCost = ManaCost.ZERO, name = "Common $it", typeLine = TypeLine(cardTypes = setOf(CardType.SORCERY)))
    } + (1..43).map {
        CardDefinition(manaCost = ManaCost.ZERO, name = "Uncommon $it", typeLine = TypeLine(cardTypes = setOf(CardType.SORCERY)),
            metadata = ScryfallMetadata(rarity = Rarity.UNCOMMON))
    } + (1..50).map {
        CardDefinition(manaCost = ManaCost.ZERO, name = "Rare $it", typeLine = TypeLine(cardTypes = setOf(CardType.SORCERY)),
            metadata = ScryfallMetadata(rarity = Rarity.RARE))
    }
    val dualNames = duals.mapTo(hashSetOf()) { it.name }
    val strategy = LandSlotBooster(
        base = EchoedPairsPlayBooster(emptyList()), nonbasicLandNames = dualNames,
        basicLands = basics, nonbasicChance = 6.0 / 11,
    )

    describe("paper land slot") {
        it("keeps duals out of all thirteen other slots and appends exactly one land") {
            repeat(200) { seed ->
                val pack = strategy.generate(ordinary + duals, Random(seed))
                pack shouldHaveSize 14
                pack.dropLast(1).none { it.name in dualNames || it.typeLine.isLand } shouldBe true
                pack.last().typeLine.isLand shouldBe true
                pack.map { it.name }.toSet().size shouldBe 14
            }
        }

        it("draws duals at 6/11 and basic arts uniformly across the other 5/11") {
            val random = Random(42)
            val counts = mutableMapOf<String, Int>()
            var dualCount = 0
            repeat(10000) {
                val last = strategy.generate(ordinary + duals, random).last()
                if (last.name in dualNames) dualCount++
                else counts.merge(last.metadata.collectorNumber!!, 1, Int::plus)
            }
            (dualCount in 5250..5650) shouldBe true
            counts.size shouldBe 25
            // Each physical basic art occupies 1/55 of the land sheet.
            counts.values.all { it in 120..245 } shouldBe true
        }

        it("uses basics when all nonbasics have been filtered out") {
            repeat(100) { seed ->
                val pack = strategy.generate(ordinary, Random(seed))
                pack shouldHaveSize 14
                pack.last().typeLine.isBasicLand shouldBe true
            }
        }

        it("uses available nonbasics when no basics are supplied") {
            val pack = strategy.copy(basicLands = emptyList()).generate(ordinary + duals, Random(1))
            pack shouldHaveSize 14
            (pack.last().name in dualNames) shouldBe true
        }

        it("opens basic snow lands left in the pool as the basic group") {
            val snowBasics = (0..4).map { land("Snow Basic $it", "s$it", basic = true, snow = true) }
            val snowSlot = strategy.copy(basicLands = emptyList(), nonbasicChance = 50.0 / 120)
            val random = Random(7)
            var dualCount = 0
            repeat(6000) {
                val pack = snowSlot.generate(ordinary + duals + snowBasics, random)
                pack shouldHaveSize 14
                pack.dropLast(1).none { it.typeLine.isLand } shouldBe true
                if (pack.last().name in dualNames) dualCount++ else pack.last().typeLine.isSnow shouldBe true
            }
            (dualCount in 2300..2700) shouldBe true
        }

        it("does not repeat a land name already selected by the base strategy") {
            val base = BoosterStrategy { _, _ -> listOf(basics.first()) }
            val pack = strategy.copy(base = base).generate(ordinary + duals, Random(1))
            pack shouldHaveSize 2
            pack.map { it.name }.toSet().size shouldBe 2
        }
    }
})

private fun land(name: String, number: String, basic: Boolean, snow: Boolean = false): CardDefinition = CardDefinition(
    name = name,
    manaCost = ManaCost.ZERO,
    typeLine = TypeLine(cardTypes = setOf(CardType.LAND),
        supertypes = buildSet {
            if (basic) add(Supertype.BASIC)
            if (snow) add(Supertype.SNOW)
        }),
    metadata = ScryfallMetadata(collectorNumber = number),
)
