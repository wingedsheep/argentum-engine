package com.wingedsheep.gameserver.config

import com.wingedsheep.gameserver.coverage.SetCoverageService
import com.wingedsheep.sdk.limited.EchoedPairsPlayBooster
import com.wingedsheep.sdk.limited.LandSlotBooster
import com.wingedsheep.sdk.limited.LegendarySlotBooster
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.Rarity
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Guards booster-pool construction for both all-reprint sets and mixed sets. Every distinct
 * booster-eligible card authored in the set or contributed as a resolvable reprint must be present.
 */
class GameBeansConfigBoosterPoolTest : FunSpec({

    val config = GameBeansConfig(GameProperties())
    val cardRegistry = config.cardRegistry()
    val setCoverageService = SetCoverageService()
    val boosterGenerator = config.boosterGenerator(cardRegistry, setCoverageService)

    test("The Brothers' War draft pack is 10 commons, 3 uncommons and one rare or mythic from its booster pool") {
        val bro = boosterGenerator.availableSets["BRO"].shouldNotBeNull()
        bro.fullyImplemented shouldBe true
        bro.boosterStrategy shouldBe StandardBooster(commons = 10)
        val eligible = bro.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand && !it.meldResult }
        // Scryfall booster pool minus the three meld backs, which never open on their own.
        eligible.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 101, Rarity.UNCOMMON to 80, Rarity.RARE to 63, Rarity.MYTHIC to 23,
        )
        val eligibleNames = eligible.map { it.name }.toSet()
        var mythics = 0
        val packs = 2000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("BRO")
            pack.size shouldBe 14
            pack.map { it.name }.toSet().size shouldBe 14
            withClue("pack ${pack.map { it.name }} holds an off-sheet card") {
                pack.all { it.name in eligibleNames } shouldBe true
            }
            pack.count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            mythics += pack.count { it.metadata.rarity == Rarity.MYTHIC }
        }
        // 1 in 8 rare slots upgrades to a mythic.
        (mythics.toDouble() / packs) shouldBe (0.125 plusOrMinus 0.03)
    }

    test("Streets of New Capenna draft pack is 10 commons, 3 uncommons and one rare or mythic from its booster pool") {
        // Draft Booster: 1 rare/mythic, 3 uncommons, 10 commons, 1 basic land (a full-art Metropolis
        // basic in a third of packs; the generator supplies basics at deck building) and a foil
        // replacing a common in a third of packs (cosmetic, not modelled).
        val snc = boosterGenerator.availableSets["SNC"].shouldNotBeNull()
        snc.fullyImplemented shouldBe true
        snc.boosterStrategy shouldBe StandardBooster(commons = 10)
        val eligible = snc.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
        // Scryfall `set:snc is:booster -t:basic`.
        eligible.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 101, Rarity.UNCOMMON to 80, Rarity.RARE to 60, Rarity.MYTHIC to 20,
        )
        val eligibleNames = eligible.map { it.name }.toSet()
        var mythics = 0
        val packs = 2000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("SNC")
            pack.size shouldBe 14
            pack.map { it.name }.toSet().size shouldBe 14
            withClue("pack ${pack.map { it.name }} holds an off-sheet card") {
                pack.all { it.name in eligibleNames } shouldBe true
            }
            pack.count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            mythics += pack.count { it.metadata.rarity == Rarity.MYTHIC }
        }
        // 1 in 8 rare slots upgrades to a mythic.
        (mythics.toDouble() / packs) shouldBe (0.125 plusOrMinus 0.03)
    }

    test("every Dominaria United draft pack opens exactly one legendary creature from its booster pool") {
        val dmu = boosterGenerator.availableSets["DMU"].shouldNotBeNull()
        dmu.fullyImplemented shouldBe true
        dmu.boosterStrategy.shouldBeInstanceOf<LegendarySlotBooster>()
        val eligibleNames = dmu.cards
            .filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
            .mapTo(hashSetOf()) { it.name }
        repeat(500) {
            val pack = boosterGenerator.generateBooster("DMU")
            pack.size shouldBe 14
            pack.map { it.name }.toSet().size shouldBe 14
            withClue("pack ${pack.map { it.name }} holds an off-sheet card") {
                pack.all { it.name in eligibleNames } shouldBe true
            }
            pack.count { it.typeLine.isLegendary && it.typeLine.isCreature } shouldBe 1
            pack.count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.RARE || it.metadata.rarity == Rarity.MYTHIC } shouldBe 1
        }
    }

    test("Reality Fracture is selectable as a complete set and generates a nonempty pack") {
        val fra = boosterGenerator.availableSets["FRA"].shouldNotBeNull()
        fra.setName shouldBe "Reality Fracture"
        fra.fullyImplemented shouldBe true
        fra.cards.map { it.name } shouldContain "Rank Rat"
        fra.cards.map { it.name } shouldContain "Unsummon"
        fra.cards.all { it.metadata.inBooster } shouldBe true
        boosterGenerator.generateBooster("FRA").size shouldBeGreaterThan 0
    }

    test("every Reality Fracture pack opens a complete echoed pair") {
        val strategy = boosterGenerator.availableSets["FRA"].shouldNotBeNull().boosterStrategy
            .shouldBeInstanceOf<LandSlotBooster>().base.shouldBeInstanceOf<EchoedPairsPlayBooster>()
        repeat(200) {
            val names = boosterGenerator.generateBooster("FRA").map { it.name }.toSet()
            val pairs = strategy.echoedPairs.filter { it.first in names && it.second in names }
            withClue("pack $names has no echoed pair") { pairs.size shouldBeGreaterThan 0 }
        }
    }

    test("Reality Fracture reserves common duals for the fourteenth land slot") {
        val strategy = boosterGenerator.availableSets["FRA"].shouldNotBeNull().boosterStrategy
            .shouldBeInstanceOf<LandSlotBooster>()
        strategy.nonbasicLandNames.size shouldBe 10
        strategy.basicLands.size shouldBe 25
        strategy.basicLands.all { it.setCode == "FRA" } shouldBe true
        repeat(200) {
            val pack = boosterGenerator.generateBooster("FRA")
            pack.size shouldBe 14
            pack.dropLast(1).none { it.name in strategy.nonbasicLandNames || it.typeLine.isBasicLand } shouldBe true
            val land = pack.last()
            (land.typeLine.isBasicLand || land.name in strategy.nonbasicLandNames) shouldBe true
        }
        repeat(20) {
            val pack = boosterGenerator.generateBooster("FRA", bannedCardNames = strategy.nonbasicLandNames)
            pack.size shouldBe 14
            pack.last().typeLine.isBasicLand shouldBe true
            pack.none { it.name in strategy.nonbasicLandNames } shouldBe true
        }
    }

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

    test("The Dark offers Portal basic lands for sealed deckbuilding") {
        val drk = boosterGenerator.availableSets["DRK"].shouldNotBeNull()

        drk.basicLands.map { it.name }.toSet() shouldBe
            setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
        drk.basicLands.all { it.setCode == "POR" } shouldBe true
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
