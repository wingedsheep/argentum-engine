package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class DoubleFacedSlotBoosterTest : DescribeSpec({
    fun card(name: String, rarity: Rarity, doubleFaced: Boolean = false): CardDefinition {
        val front = CardDefinition(
            manaCost = ManaCost.ZERO,
            name = name,
            typeLine = TypeLine(cardTypes = setOf(CardType.CREATURE)),
            creatureStats = CreatureStats(1, 1),
            metadata = ScryfallMetadata(rarity = rarity),
        )
        return if (doubleFaced) front.copy(backFace = front.copy(name = "$name Back")) else front
    }

    val ordinary = (1..111).map { card("Common $it", Rarity.COMMON) } +
        (1..80).map { card("Uncommon $it", Rarity.UNCOMMON) } +
        (1..53).map { card("Rare $it", Rarity.RARE) } +
        (1..15).map { card("Mythic $it", Rarity.MYTHIC) }
    val dfcCommons = (1..6).map { card("DFC C $it", Rarity.COMMON, doubleFaced = true) }
    val dfcUncommons = (1..8).map { card("DFC U $it", Rarity.UNCOMMON, doubleFaced = true) }
    val dfcRares = (1..6).map { card("DFC R $it", Rarity.RARE, doubleFaced = true) }
    val pool = ordinary + dfcCommons + dfcUncommons + dfcRares
    val strategy = DoubleFacedSlotBooster()
    fun CardDefinition.onDfcSheet() = isDoubleFaced && metadata.rarity != Rarity.RARE && metadata.rarity != Rarity.MYTHIC

    describe("Kamigawa: Neon Dynasty draft collation") {
        it("opens one double-faced common or uncommon beside nine commons, three uncommons and one rare") {
            repeat(500) { seed ->
                val pack = strategy.generate(pool, Random(seed))
                pack shouldHaveSize 14
                pack.map { it.name }.toSet().size shouldBe 14
                pack.count { it.onDfcSheet() } shouldBe 1
                pack.count { it.metadata.rarity == Rarity.COMMON && !it.isDoubleFaced } shouldBe 9
                pack.count { it.metadata.rarity == Rarity.UNCOMMON && !it.isDoubleFaced } shouldBe 3
                pack.count { it.metadata.rarity == Rarity.RARE || it.metadata.rarity == Rarity.MYTHIC } shouldBe 1
            }
        }

        it("makes the double-faced slot an uncommon in a third of packs and keeps the mythic upgrade") {
            val random = Random(11)
            val packs = 8000
            var dfcUncommons = 0
            var mythics = 0
            repeat(packs) {
                val pack = strategy.generate(pool, random)
                if (pack.single { it.onDfcSheet() }.metadata.rarity == Rarity.UNCOMMON) dfcUncommons++
                mythics += pack.count { it.metadata.rarity == Rarity.MYTHIC }
            }
            (dfcUncommons.toDouble() / packs) shouldBe ((1.0 / 3) plusOrMinus 0.02)
            (mythics.toDouble() / packs) shouldBe (0.125 plusOrMinus 0.015)
        }

        it("keeps double-faced rares in the rare slot") {
            val seen = (0 until 3000).any { seed ->
                strategy.generate(pool, Random(seed)).any { it.isDoubleFaced && it.metadata.rarity == Rarity.RARE }
            }
            seen shouldBe true
        }
    }

    describe("sparse pools") {
        it("uses double-faced commons when every double-faced uncommon is banned") {
            repeat(100) { seed ->
                val pack = strategy.generate(ordinary + dfcCommons, Random(seed))
                pack shouldHaveSize 14
                pack.single { it.onDfcSheet() }.metadata.rarity shouldBe Rarity.COMMON
            }
        }

        it("uses double-faced uncommons when every double-faced common is banned") {
            repeat(100) { seed ->
                val pack = strategy.generate(ordinary + dfcUncommons, Random(seed))
                pack shouldHaveSize 14
                pack.single { it.onDfcSheet() }.metadata.rarity shouldBe Rarity.UNCOMMON
            }
        }

        it("fills the slot with an ordinary common without double-faced cards") {
            val pack = strategy.generate(ordinary, Random(1))
            pack shouldHaveSize 14
            pack.count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
        }
    }
})
