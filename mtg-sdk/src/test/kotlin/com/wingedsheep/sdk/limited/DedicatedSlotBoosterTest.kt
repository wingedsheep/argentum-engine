package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.shouldBe
import kotlin.random.Random

class DedicatedSlotBoosterTest : DescribeSpec({
    fun card(
        name: String,
        rarity: Rarity,
        doubleFaced: Boolean = false,
        legendary: Boolean = false,
        battle: Boolean = false,
    ): CardDefinition {
        val front = CardDefinition(
            manaCost = ManaCost.ZERO,
            name = name,
            typeLine = TypeLine(
                supertypes = if (legendary) setOf(Supertype.LEGENDARY) else emptySet(),
                cardTypes = setOf(if (battle) CardType.BATTLE else CardType.CREATURE),
            ),
            creatureStats = if (battle) null else CreatureStats(1, 1),
            metadata = ScryfallMetadata(rarity = rarity),
        )
        return if (doubleFaced || battle) front.copy(backFace = front.copy(name = "$name Back")) else front
    }
    fun CardDefinition.isRareOrMythic() = metadata.rarity == Rarity.RARE || metadata.rarity == Rarity.MYTHIC

    val singleFaced = (1..90).map { card("Common $it", Rarity.COMMON) } +
        (1..60).map { card("Uncommon $it", Rarity.UNCOMMON) } +
        (1..53).map { card("Rare $it", Rarity.RARE) } +
        (1..15).map { card("Mythic $it", Rarity.MYTHIC) }
    val dfcCommons = (1..10).map { card("DFC C $it", Rarity.COMMON, doubleFaced = true) }
    val dfcUncommons = (1..23).map { card("DFC U $it", Rarity.UNCOMMON, doubleFaced = true) }
    val dfcRares = (1..11).map { card("DFC R $it", Rarity.RARE, doubleFaced = true) } +
        (1..5).map { card("DFC M $it", Rarity.MYTHIC, doubleFaced = true) }
    val dfcPool = singleFaced + dfcCommons + dfcUncommons + dfcRares

    describe("two double-faced slots (Innistrad: Midnight Hunt / Crimson Vow)") {
        val strategy = DedicatedSlotBooster(
            base = StandardBooster(commons = 9, uncommons = 2),
            slots = listOf(
                DedicatedSlot(SheetSelector.DoubleFaced, mapOf(Rarity.COMMON to 1.0)),
                DedicatedSlot(SheetSelector.DoubleFaced, mapOf(Rarity.UNCOMMON to 1.0), rareChance = 1.0 / 6),
            ),
        )

        it("opens a double-faced common and one double-faced uncommon-or-rare beside 9 commons, 3 uncommons and a rare") {
            repeat(500) { seed ->
                val pack = strategy.generate(dfcPool, Random(seed))
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
                val pack = strategy.generate(dfcPool, random)
                if (pack.single { it.isRareOrMythic() }.isDoubleFaced) dfcRares++
                mythics += pack.count { it.metadata.rarity == Rarity.MYTHIC }
            }
            (dfcRares.toDouble() / packs) shouldBe ((1.0 / 6) plusOrMinus 0.015)
            (mythics.toDouble() / packs) shouldBe (0.125 plusOrMinus 0.015)
        }

        it("keeps the rare single-faced when every double-faced rare and mythic is banned") {
            repeat(200) { seed ->
                val pack = strategy.generate(singleFaced + dfcCommons + dfcUncommons, Random(seed))
                pack shouldHaveSize 14
                pack.single { it.isRareOrMythic() }.isDoubleFaced shouldBe false
                pack.count { it.isDoubleFaced && it.metadata.rarity == Rarity.UNCOMMON } shouldBe 1
            }
        }

        it("fills the dedicated slots from the ordinary sheets when the set has no double-faced cards") {
            val pack = strategy.generate(singleFaced, Random(1))
            pack shouldHaveSize 14
            pack.count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            pack.count { it.isRareOrMythic() } shouldBe 1
        }
    }

    describe("a common-or-uncommon double-faced slot whose rares stay in the rare slot (Neon Dynasty)") {
        val strategy = DedicatedSlotBooster(
            base = StandardBooster(commons = 9, uncommons = 3),
            slots = listOf(DedicatedSlot(SheetSelector.DoubleFaced, mapOf(Rarity.COMMON to 2.0, Rarity.UNCOMMON to 1.0))),
        )

        it("opens exactly one double-faced common or uncommon, and double-faced rares only in the rare slot") {
            val random = Random(3)
            var dfcUncommonPacks = 0
            var dfcRarePacks = 0
            val packs = 6000
            repeat(packs) {
                val pack = strategy.generate(dfcPool, random)
                pack shouldHaveSize 14
                pack.count { it.isDoubleFaced && !it.isRareOrMythic() } shouldBe 1
                pack.count { it.metadata.rarity == Rarity.UNCOMMON && !it.isDoubleFaced } shouldBe 3
                if (pack.any { it.isDoubleFaced && it.metadata.rarity == Rarity.UNCOMMON }) dfcUncommonPacks++
                if (pack.single { it.isRareOrMythic() }.isDoubleFaced) dfcRarePacks++
            }
            (dfcUncommonPacks.toDouble() / packs) shouldBe ((1.0 / 3) plusOrMinus 0.02)
            // Double-faced rares share the ordinary rare sheet: 11 of 64 rares, 5 of 20 mythics.
            (dfcRarePacks.toDouble() / packs) shouldBe ((0.875 * 11 / 64 + 0.125 * 5 / 20) plusOrMinus 0.02)
        }
    }

    describe("a legendary slot that may hold the rare (Dominaria United)") {
        val ordinary = singleFaced + card("Legendary Artifact", Rarity.RARE, legendary = true).copy(
            typeLine = TypeLine(supertypes = setOf(Supertype.LEGENDARY), cardTypes = setOf(CardType.ARTIFACT)),
            creatureStats = null,
        )
        val legends = (1..20).map { card("Legend U $it", Rarity.UNCOMMON, legendary = true) } +
            (1..14).map { card("Legend R $it", Rarity.RARE, legendary = true) } +
            (1..7).map { card("Legend M $it", Rarity.MYTHIC, legendary = true) }
        val strategy = DedicatedSlotBooster(
            base = StandardBooster(commons = 10, uncommons = 2),
            slots = listOf(DedicatedSlot(SheetSelector.LegendaryCreature, mapOf(Rarity.UNCOMMON to 1.0), rareChance = 0.25)),
        )
        fun CardDefinition.isLegendaryCreature() = typeLine.isLegendary && typeLine.isCreature

        it("opens exactly one legendary creature, a quarter of the time as the rare") {
            val random = Random(11)
            val packs = 6000
            var legendaryRares = 0
            repeat(packs) {
                val pack = strategy.generate(ordinary + legends, random)
                pack shouldHaveSize 14
                pack.count { it.isLegendaryCreature() } shouldBe 1
                pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
                pack.count { it.isRareOrMythic() } shouldBe 1
                if (pack.single { it.isLegendaryCreature() }.isRareOrMythic()) legendaryRares++
            }
            (legendaryRares.toDouble() / packs) shouldBe (0.25 plusOrMinus 0.02)
        }
    }

    describe("two floating-rare slots (March of the Machine)") {
        val battles = (1..12).map { card("Battle U $it", Rarity.UNCOMMON, battle = true) } +
            (1..6).map { card("Battle R $it", Rarity.RARE, battle = true) }
        val strategy = DedicatedSlotBooster(
            base = StandardBooster(commons = 8, uncommons = 2),
            slots = listOf(
                DedicatedSlot(
                    SheetSelector.Except(SheetSelector.DoubleFaced, SheetSelector.Battle),
                    mapOf(Rarity.COMMON to 1.0, Rarity.UNCOMMON to 1.0),
                    rareChance = 15.0 / 140,
                ),
                DedicatedSlot(SheetSelector.Battle, mapOf(Rarity.UNCOMMON to 1.0), rareChance = 27.0 / 140),
            ),
        )

        it("opens one battle and one transforming card, and exactly one rare across all slots") {
            val random = Random(5)
            val packs = 8000
            var battleRares = 0
            var dfcRares = 0
            repeat(packs) {
                val pack = strategy.generate(dfcPool + battles, random)
                pack shouldHaveSize 13
                pack.map { it.name }.toSet().size shouldBe 13
                pack.count { it.typeLine.isBattle } shouldBe 1
                pack.count { it.isDoubleFaced && !it.typeLine.isBattle } shouldBe 1
                                val rare = pack.single { it.isRareOrMythic() }
                if (rare.typeLine.isBattle) battleRares++ else if (rare.isDoubleFaced) dfcRares++
            }
            (battleRares.toDouble() / packs) shouldBe ((27.0 / 140) plusOrMinus 0.015)
            (dfcRares.toDouble() / packs) shouldBe ((15.0 / 140) plusOrMinus 0.015)
        }
    }

    describe("an extra slot whose rares also stay on the ordinary sheets") {
        val named = setOf("Common 1", "Uncommon 1", "Rare 1")
        val strategy = DedicatedSlotBooster(
            base = PlayBooster(commons = 7, uncommons = 2),
            slots = listOf(
                DedicatedSlot(
                    SheetSelector.Named(named),
                    mapOf(Rarity.COMMON to 1.0, Rarity.UNCOMMON to 1.0, Rarity.RARE to 1.0),
                    sharedRarities = setOf(Rarity.RARE),
                ),
            ),
        )

        it("keeps unshared sheet cards out of the ordinary slots and still opens the shared rare there") {
            val random = Random(9)
            var rareInRareSlot = 0
            repeat(4000) {
                val pack = strategy.generate(singleFaced, random)
                pack shouldHaveSize 13
                pack.map { it.name }.toSet().size shouldBe 13
                val ordinarySlots = pack.take(12)
                ordinarySlots.none { it.name == "Common 1" || it.name == "Uncommon 1" } shouldBe true
                (pack.last().name in named) shouldBe true
                if (ordinarySlots.any { it.name == "Rare 1" }) rareInRareSlot++
            }
            (rareInRareSlot > 0) shouldBe true
        }
    }

    describe("a slot that is an ordinary common unless it rolls a double-faced rare (Eldritch Moon)") {
        val strategy = DedicatedSlotBooster(
            base = StandardBooster(commons = 8, uncommons = 3),
            slots = listOf(
                DedicatedSlot(SheetSelector.DoubleFaced, mapOf(Rarity.COMMON to 1.0, Rarity.UNCOMMON to 1.0)),
                DedicatedSlot(
                    SheetSelector.DoubleFaced,
                    mapOf(Rarity.COMMON to 7.0, Rarity.RARE to 10.0 / 12, Rarity.MYTHIC to 2.0 / 12),
                    ordinaryRarities = setOf(Rarity.COMMON),
                ),
            ),
        )

        it("opens a single-faced common in seven packs of eight and a double-faced rare or mythic in the eighth") {
            val random = Random(11)
            val packs = 8000
            var dfcRares = 0
            var dfcMythics = 0
            repeat(packs) {
                val pack = strategy.generate(dfcPool, random)
                pack shouldHaveSize 14
                pack.map { it.name }.toSet().size shouldBe 14
                pack.count { it.isDoubleFaced && !it.isRareOrMythic() } shouldBe 1
                pack.count { it.isRareOrMythic() && !it.isDoubleFaced } shouldBe 1
                pack.count { it.metadata.rarity == Rarity.UNCOMMON && !it.isDoubleFaced } shouldBe 3
                val sheetRare = pack.count { it.isDoubleFaced && it.isRareOrMythic() }
                pack.count { it.metadata.rarity == Rarity.COMMON && !it.isDoubleFaced } shouldBe 9 - sheetRare
                dfcRares += pack.count { it.isDoubleFaced && it.metadata.rarity == Rarity.RARE }
                dfcMythics += pack.count { it.isDoubleFaced && it.metadata.rarity == Rarity.MYTHIC }
            }
            ((dfcRares + dfcMythics).toDouble() / packs) shouldBe (0.125 plusOrMinus 0.012)
            (dfcMythics.toDouble() / (dfcRares + dfcMythics)) shouldBe ((1.0 / 6) plusOrMinus 0.04)
        }

        it("keeps double-faced rares out of the ordinary rare slot") {
            repeat(500) { seed ->
                val pack = strategy.generate(dfcPool, Random(seed))
                pack.take(12).none { it.isDoubleFaced } shouldBe true
            }
        }

        it("opens an ordinary common in the slot when every double-faced rare and mythic is banned") {
            repeat(200) { seed ->
                val pack = strategy.generate(singleFaced + dfcCommons + dfcUncommons, Random(seed))
                pack shouldHaveSize 14
                pack.count { it.isDoubleFaced } shouldBe 1
                pack.count { it.metadata.rarity == Rarity.COMMON && !it.isDoubleFaced } shouldBe 9
            }
        }

        it("rejects ordinary rarities the slot never rolls, and a slot with nothing left on its own sheet") {
            shouldThrow<IllegalArgumentException> {
                DedicatedSlot(SheetSelector.DoubleFaced, mapOf(Rarity.RARE to 1.0), ordinaryRarities = setOf(Rarity.COMMON))
            }
            shouldThrow<IllegalArgumentException> {
                DedicatedSlot(SheetSelector.DoubleFaced, mapOf(Rarity.COMMON to 1.0), ordinaryRarities = setOf(Rarity.COMMON))
            }
        }
    }
})
