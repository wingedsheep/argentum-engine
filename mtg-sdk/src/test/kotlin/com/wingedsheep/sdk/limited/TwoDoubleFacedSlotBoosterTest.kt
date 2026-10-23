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

class TwoDoubleFacedSlotBoosterTest : DescribeSpec({
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

    val singleFaced = (1..90).map { card("Common $it", Rarity.COMMON) } +
        (1..60).map { card("Uncommon $it", Rarity.UNCOMMON) } +
        (1..53).map { card("Rare $it", Rarity.RARE) } +
        (1..15).map { card("Mythic $it", Rarity.MYTHIC) }
    val dfcCommons = (1..10).map { card("DFC C $it", Rarity.COMMON, doubleFaced = true) }
    val dfcUncommons = (1..23).map { card("DFC U $it", Rarity.UNCOMMON, doubleFaced = true) }
    val dfcRares = (1..11).map { card("DFC R $it", Rarity.RARE, doubleFaced = true) } +
        (1..5).map { card("DFC M $it", Rarity.MYTHIC, doubleFaced = true) }
    val pool = singleFaced + dfcCommons + dfcUncommons + dfcRares
    val strategy = TwoDoubleFacedSlotBooster()
    fun CardDefinition.isRareOrMythic() = metadata.rarity == Rarity.RARE || metadata.rarity == Rarity.MYTHIC

    describe("Innistrad: Midnight Hunt draft collation") {
        it("opens a double-faced common and one double-faced uncommon-or-rare beside 9 commons, 3 uncommons and a rare") {
            repeat(500) { seed ->
                val pack = strategy.generate(pool, Random(seed))
                pack shouldHaveSize 14
                pack.map { it.name }.toSet().size shouldBe 14
                pack.count { it.isDoubleFaced } shouldBe 2
                pack.count { it.isDoubleFaced && it.metadata.rarity == Rarity.COMMON } shouldBe 1
                pack.count { !it.isDoubleFaced && it.metadata.rarity == Rarity.COMMON } shouldBe 9
                pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
                pack.count { it.isRareOrMythic() } shouldBe 1
            }
        }

        it("makes the double-faced card the rare in a sixth of packs and keeps the mythic upgrade") {
            val random = Random(7)
            val packs = 8000
            var dfcRares = 0
            var mythics = 0
            repeat(packs) {
                val pack = strategy.generate(pool, random)
                if (pack.single { it.isRareOrMythic() }.isDoubleFaced) dfcRares++
                mythics += pack.count { it.metadata.rarity == Rarity.MYTHIC }
            }
            (dfcRares.toDouble() / packs) shouldBe ((1.0 / 6) plusOrMinus 0.015)
            (mythics.toDouble() / packs) shouldBe (0.125 plusOrMinus 0.015)
        }
    }

    describe("sparse pools") {
        it("keeps the pair single-faced rare when every double-faced rare and mythic is banned") {
            repeat(200) { seed ->
                val pack = strategy.generate(singleFaced + dfcCommons + dfcUncommons, Random(seed))
                pack shouldHaveSize 14
                pack.single { it.isRareOrMythic() }.isDoubleFaced shouldBe false
                pack.count { it.isDoubleFaced && it.metadata.rarity == Rarity.UNCOMMON } shouldBe 1
            }
        }

        it("fills the double-faced slots from the single-faced sheets without double-faced cards") {
            val pack = strategy.generate(singleFaced, Random(1))
            pack shouldHaveSize 14
            pack.count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            pack.count { it.isRareOrMythic() } shouldBe 1
        }
    }
})
