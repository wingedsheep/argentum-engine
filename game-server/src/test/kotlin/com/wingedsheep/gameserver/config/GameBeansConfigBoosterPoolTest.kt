package com.wingedsheep.gameserver.config

import com.wingedsheep.gameserver.coverage.SetCoverageService
import com.wingedsheep.sdk.model.Rarity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Guards booster-pool construction for both all-reprint sets and mixed sets. Every distinct
 * booster-eligible card authored in the set or contributed as a resolvable reprint must be present.
 */
class GameBeansConfigBoosterPoolTest : FunSpec({

    val config = GameBeansConfig(GameProperties())
    val cardRegistry = config.cardRegistry()
    val setCoverageService = SetCoverageService()
    val boosterGenerator = config.boosterGenerator(cardRegistry, setCoverageService)

    test("Eighth Edition appears as a selectable set") {
        boosterGenerator.availableSets shouldContainKey "8ED"
    }

    test("Eighth Edition's booster pool is resolved from its reprints") {
        val ed8 = boosterGenerator.availableSets["8ED"].shouldNotBeNull()
        // 8ED is an all-reprint set: its ~187 cards are resolved from printings, not own definitions.
        ed8.cards.size shouldBeGreaterThan 150
        ed8.distinctCardCount shouldBeGreaterThan 150
        // Every resolved card is stamped as the 8ED printing.
        ed8.cards.all { it.setCode == "8ED" } shouldBe true
    }

    test("resolved reprints span multiple rarities so booster slots can be filled") {
        val ed8 = boosterGenerator.availableSets["8ED"].shouldNotBeNull()
        val rarities = ed8.cards.map { it.metadata.rarity }.toSet()
        rarities shouldContain Rarity.COMMON
        rarities shouldContain Rarity.RARE
    }

    test("own cards are stamped with their Limited set printing identity") {
        val por = boosterGenerator.availableSets["POR"].shouldNotBeNull()
        val ragingGoblin = por.cards.first { it.name == "Raging Goblin" }

        ragingGoblin.setCode shouldBe "POR"
        ragingGoblin.metadata.collectorNumber shouldBe "145"
        com.wingedsheep.engine.limited.BoosterGenerator.withCardArt(
            mapOf("Raging Goblin" to 1), listOf(ragingGoblin),
        ) shouldBe mapOf("Raging Goblin#POR-145" to 1)
    }

    test("Foundations includes its own cards and its reprints in the booster pool") {
        val fdn = boosterGenerator.availableSets["FDN"].shouldNotBeNull()
        val names = fdn.cards.map { it.name }

        // The baked Scryfall draft universe has 276 distinct names: 271 gameplay cards plus the
        // five basic-land names (20 physical basic-land printings), which live in basicLands.
        names.size shouldBe 271
        fdn.distinctCardCount shouldBe 271
        names.toSet().size shouldBe names.size
        names shouldContain "Abyssal Harvester" // Canonical definition originates in FDN.
        names shouldContain "Serra Angel" // The canonical definition lives in an earlier set.
        names.contains("Gilded Lotus") shouldBe false // Starter Collection, not a Play Booster card.
        fdn.cards.first { it.name == "Serra Angel" }.setCode shouldBe "FDN"
    }

    test("Foundations keeps implemented non-booster products available as optional extras") {
        val fdn = boosterGenerator.availableSets["FDN"].shouldNotBeNull()
        val beginnerBox = fdn.extraCardsByProduct["beginnerbox"].shouldNotBeNull().map { it.name }
        val starterCollection = fdn.extraCardsByProduct["startercollection"].shouldNotBeNull().map { it.name }

        beginnerBox shouldContain "Ancestor Dragon"
        starterCollection shouldContain "Angelic Destiny"
        val pacifism = fdn.extraCardsByProduct.values.flatten().first { it.name == "Pacifism" }
        pacifism.setCode shouldBe "FDN"
        pacifism.metadata.collectorNumber shouldBe "501"
        pacifism.metadata.imageUri shouldBe
            "https://cards.scryfall.io/normal/front/8/3/839160d2-44a3-4566-be9d-558d043beac8.jpg?1730490501"
        fdn.cards.map { it.name }.contains("Ancestor Dragon") shouldBe false
        fdn.cards.map { it.name }.contains("Angelic Destiny") shouldBe false
    }

    test("every resolvable reprint is included for every mixed set") {
        for ((setCode, setConfig) in boosterGenerator.availableSets) {
            val poolNames = setConfig.cards.mapTo(hashSetOf()) { it.name }
            val eligibleNames = setCoverageService.limitedCardNames(setCode)
            val missing = setConfig.printings
                .asSequence()
                .map { it.name }
                .distinct()
                .filter { eligibleNames == null || it in eligibleNames }
                .filter { cardRegistry.getCardsByName(it).isNotEmpty() }
                .filterNot { it in poolNames }
                .toList()

            missing shouldBe emptyList<String>()
        }
    }
})
