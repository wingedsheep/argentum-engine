package com.wingedsheep.engine.limited

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * Wastes is a pack-only basic in Limited: Oath of the Gatewatch prints it at common (two
 * printings, so it opens twice as often as any other common), and a player can play only the Wastes
 * they open or draft. The generator keeps it in the booster pool, never among the land-station
 * basics, while the five nonsnow basics stay out of packs and in the land station.
 */
class BoosterGeneratorPackOnlyBasicTest : DescribeSpec({

    fun card(name: String, rarity: Rarity) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        metadata = ScryfallMetadata(rarity = rarity, collectorNumber = name),
    )

    val wastes = listOf("183", "184").map { number -> basicLand("Wastes") { collectorNumber = number } }
    val plains = basicLand("Plains") { collectorNumber = "250" }
    val commons = (1..68).map { card("Common $it", Rarity.COMMON) }
    val pool = commons +
        (1..60).map { card("Uncommon $it", Rarity.UNCOMMON) } +
        (1..42).map { card("Rare $it", Rarity.RARE) } +
        plains +
        wastes

    val generator = BoosterGenerator(
        mapOf(
            "OGW" to BoosterGenerator.SetConfig(
                setCode = "OGW",
                setName = "Oath of the Gatewatch",
                cards = pool,
                basicLands = listOf(plains) + wastes,
                boosterStrategy = StandardBooster(commons = 10),
            ),
        ),
    )

    it("offers only the freely added basics at deck building, never Wastes") {
        generator.getBasicLands("OGW").keys shouldBe setOf("Plains")
    }

    it("opens Wastes in the common slot, about twice as often as each other common") {
        val packs = 3000
        val opened = List(packs) { generator.generateBooster("OGW").map { it.name } }

        opened.all { pack -> "Plains" !in pack } shouldBe true
        opened.all { pack -> pack.size == 14 } shouldBe true

        val wastesRate = opened.count { "Wastes" in it }.toDouble() / packs
        val commonRate = opened.sumOf { pack -> pack.count { it.startsWith("Common ") } }.toDouble() /
            (packs * commons.size)
        // Two printings among 70 common entries; the no-repeated-name rule trims the double a little.
        wastesRate shouldBeGreaterThan 1.6 * commonRate
    }
})
