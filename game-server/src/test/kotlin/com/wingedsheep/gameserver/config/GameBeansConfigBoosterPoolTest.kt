package com.wingedsheep.gameserver.config

import com.wingedsheep.gameserver.coverage.SetCoverageService
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.limited.DedicatedSlotBooster
import com.wingedsheep.sdk.limited.EchoedPairsPlayBooster
import com.wingedsheep.sdk.limited.LandSlotBooster
import com.wingedsheep.sdk.limited.PlayBooster
import com.wingedsheep.sdk.limited.PrintSheetBooster
import com.wingedsheep.sdk.limited.StandardBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.plusOrMinus
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.collections.shouldNotContain
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

    test("Kaldheim draft pack opens 10 commons, 3 uncommons, a rare or mythic and one snow land") {
        // WotC "Kaldheim Product Overview": 1 rare/mythic, 3 uncommons, 10 commons, 1 snow land (basic
        // or dual). MTGJSON's land sheet weights each snow dual 5 and each snow-basic art 7 (50/120
        // duals); the foil replacing a common in a third of packs is cosmetic and not modelled.
        val khm = boosterGenerator.availableSets["KHM"].shouldNotBeNull()
        khm.fullyImplemented shouldBe true
        val strategy = khm.boosterStrategy.shouldBeInstanceOf<LandSlotBooster>()
        strategy.base shouldBe StandardBooster(commons = 10, mythicChance = 20.0 / 148)
        strategy.basicLands shouldBe emptyList()
        val duals = strategy.nonbasicLandNames
        duals.size shouldBe 10
        val snowBasics = khm.cards.filter { it.typeLine.isBasicLand }
        snowBasics.map { it.name }.toSet() shouldBe setOf(
            "Snow-Covered Plains", "Snow-Covered Island", "Snow-Covered Swamp",
            "Snow-Covered Mountain", "Snow-Covered Forest",
        )
        snowBasics.all { it.typeLine.isSnow && it.metadata.inBooster && it.setCode == "KHM" } shouldBe true
        val eligible = khm.cards.filter {
            it.metadata.inBooster && !it.typeLine.isBasicLand && !it.meldResult && it.name !in duals
        }
        // MTGJSON `booster.draft` sheets: the non-land commons, uncommons and rares/mythics by name.
        eligible.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 101, Rarity.UNCOMMON to 80, Rarity.RARE to 64, Rarity.MYTHIC to 20,
        )
        val eligibleNames = eligible.map { it.name }.toSet()
        val snowBasicNames = snowBasics.map { it.name }.toSet()
        var mythics = 0
        var dualLands = 0
        val packs = 3000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("KHM")
            pack.size shouldBe 15
            pack.map { it.name }.toSet().size shouldBe 15
            withClue("pack ${pack.map { it.name }} holds an off-sheet card outside the land slot") {
                pack.dropLast(1).all { it.name in eligibleNames } shouldBe true
            }
            pack.dropLast(1).count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            val land = pack.last()
            (land.name in duals || land.name in snowBasicNames) shouldBe true
            if (land.name in duals) dualLands++
            mythics += pack.count { it.metadata.rarity == Rarity.MYTHIC }
        }
        (dualLands.toDouble() / packs) shouldBe (50.0 / 120 plusOrMinus 0.035)
        (mythics.toDouble() / packs) shouldBe (20.0 / 148 plusOrMinus 0.025)
        repeat(20) {
            val pack = boosterGenerator.generateBooster("KHM", bannedCardNames = duals)
            pack.size shouldBe 15
            (pack.last().name in snowBasicNames) shouldBe true
        }
        // Snow basics are pack cards only: deck building still offers just the nonsnow basics.
        boosterGenerator.getBasicLands("KHM").keys shouldBe setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
    }

    test("Core Set 2020 draft pack opens 10 commons, 3 uncommons, a rare or mythic and a land-slot card") {
        // M20 draft sheets (taw/magic-search-engine, mtg.wtf/pack/m20): 10 commons, 3 uncommons,
        // 1 rare/mythic (rares weighted 2, mythics 1 -> 15/121), and a land slot of 31 cards at 1/31 each:
        // the ten gain lands, Evolving Wilds and the twenty basic arts. The foil replacing a common in a
        // third of packs is cosmetic and not modelled.
        val m20 = boosterGenerator.availableSets["M20"].shouldNotBeNull()
        m20.fullyImplemented shouldBe true
        val strategy = m20.boosterStrategy.shouldBeInstanceOf<LandSlotBooster>()
        strategy.base shouldBe StandardBooster(commons = 10, mythicChance = 15.0 / 121)
        strategy.nonbasicChance shouldBe 11.0 / 31
        strategy.basicLands.map { it.name }.toSet() shouldBe setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
        val slotLands = strategy.nonbasicLandNames
        slotLands.size shouldBe 11
        val eligible = m20.cards.filter {
            it.metadata.inBooster && !it.typeLine.isBasicLand && !it.meldResult && it.name !in slotLands
        }
        // The draft sheets' commons (Evolving Wilds and the gain lands sit in the land slot), uncommons and
        // rares/mythics, by distinct name.
        eligible.distinctBy { it.name }.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 101, Rarity.UNCOMMON to 80, Rarity.RARE to 53, Rarity.MYTHIC to 15,
        )
        val eligibleNames = eligible.map { it.name }.toSet()
        var mythics = 0
        var nonbasicPacks = 0
        val packs = 3000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("M20")
            pack.size shouldBe 15
            pack.map { it.name }.toSet().size shouldBe 15
            withClue("pack ${pack.map { it.name }} holds an off-sheet card outside the land slot") {
                pack.dropLast(1).all { it.name in eligibleNames } shouldBe true
            }
            pack.dropLast(1).count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            val land = pack.last()
            (land.name in slotLands || land.typeLine.isBasicLand) shouldBe true
            if (land.name in slotLands) nonbasicPacks++
            mythics += pack.count { it.metadata.rarity == Rarity.MYTHIC }
        }
        (nonbasicPacks.toDouble() / packs) shouldBe (11.0 / 31 plusOrMinus 0.035)
        (mythics.toDouble() / packs) shouldBe (15.0 / 121 plusOrMinus 0.025)
        // A host ban on every land-slot nonbasic leaves the land slot to the basics.
        repeat(20) {
            val pack = boosterGenerator.generateBooster("M20", bannedCardNames = slotLands)
            pack.size shouldBe 15
            pack.last().typeLine.isBasicLand shouldBe true
        }
    }

    test("Ravnica Allegiance draft pack opens 10 commons, 3 uncommons, a rare or mythic and a Guildgate") {
        // RNA draft sheets (taw/magic-search-engine, mtg.wtf/pack/rna): 10 commons, 3 uncommons,
        // 1 rare/mythic (rares weighted 2, mythics 1 -> 15/121), and a land slot that is always one of the
        // five Guildgates, which the common sheet never carries. The foil replacing a common is not modelled.
        val rna = boosterGenerator.availableSets["RNA"].shouldNotBeNull()
        rna.fullyImplemented shouldBe true
        val strategy = rna.boosterStrategy.shouldBeInstanceOf<LandSlotBooster>()
        strategy.base shouldBe StandardBooster(commons = 10, mythicChance = 15.0 / 121)
        strategy.nonbasicChance shouldBe 1.0
        val gates = strategy.nonbasicLandNames
        gates shouldBe setOf("Azorius Guildgate", "Gruul Guildgate", "Orzhov Guildgate", "Rakdos Guildgate", "Simic Guildgate")
        val eligible = rna.cards.filter {
            it.metadata.inBooster && !it.typeLine.isBasicLand && !it.meldResult && it.name !in gates
        }
        // The draft sheets by distinct name: commons (with Gateway Plaza), uncommons and rares/mythics;
        // the Planeswalker Deck cards and the buy-a-box promo are off every sheet.
        eligible.distinctBy { it.name }.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 101, Rarity.UNCOMMON to 80, Rarity.RARE to 53, Rarity.MYTHIC to 15,
        )
        val eligibleNames = eligible.map { it.name }.toSet()
        eligibleNames shouldContain "Gateway Plaza"
        // Scryfall's booster list names split cards by their first half; the pool still admits them.
        eligibleNames shouldContain "Carnival // Carnage"
        for (deckOnly in listOf("Dovin, Architect of Law", "Domri, City Smasher", "Ragefire", "The Haunt of Hightower")) {
            eligibleNames shouldNotContain deckOnly
        }
        var mythics = 0
        val packs = 3000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("RNA")
            pack.size shouldBe 15
            pack.map { it.name }.toSet().size shouldBe 15
            withClue("pack ${pack.map { it.name }} holds an off-sheet card outside the land slot") {
                pack.dropLast(1).all { it.name in eligibleNames } shouldBe true
            }
            pack.dropLast(1).count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            (pack.last().name in gates) shouldBe true
            mythics += pack.count { it.metadata.rarity == Rarity.MYTHIC }
        }
        (mythics.toDouble() / packs) shouldBe (15.0 / 121 plusOrMinus 0.025)
        // A host ban on every Guildgate falls back to a basic land in the land slot.
        repeat(20) {
            val pack = boosterGenerator.generateBooster("RNA", bannedCardNames = gates)
            pack.size shouldBe 15
            pack.last().typeLine.isBasicLand shouldBe true
        }
    }

    test("Ikoria draft pack opens 10 commons, 3 uncommons, a rare or mythic and a gain land or basic") {
        // IKO draft sheets (taw/magic-search-engine, mtg.wtf/pack/iko-draft): 10 commons, 3 uncommons,
        // 1 rare/mythic (rares weighted 2, mythics 1 -> 15/121), and a basic_or_gainland slot where the ten
        // gain lands (1/20 each) and fifteen basic arts (1/30 each) split it 50/50. Showcase variants and
        // the foil replacing a common in a third of packs are cosmetic and not modelled.
        val iko = boosterGenerator.availableSets["IKO"].shouldNotBeNull()
        iko.fullyImplemented shouldBe true
        val strategy = iko.boosterStrategy.shouldBeInstanceOf<LandSlotBooster>()
        strategy.base shouldBe StandardBooster(commons = 10, mythicChance = 15.0 / 121)
        strategy.nonbasicChance shouldBe 0.5
        strategy.basicLands.map { it.name }.toSet() shouldBe setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
        val gainLands = strategy.nonbasicLandNames
        gainLands.size shouldBe 10
        val eligible = iko.cards.filter {
            it.metadata.inBooster && !it.typeLine.isBasicLand && !it.meldResult && it.name !in gainLands
        }
        // The draft sheets' non-land commons, uncommons and rares/mythics, by distinct name.
        eligible.distinctBy { it.name }.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 101, Rarity.UNCOMMON to 80, Rarity.RARE to 53, Rarity.MYTHIC to 15,
        )
        val eligibleNames = eligible.map { it.name }.toSet()
        var mythics = 0
        var gainLandPacks = 0
        val packs = 3000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("IKO")
            pack.size shouldBe 15
            pack.map { it.name }.toSet().size shouldBe 15
            withClue("pack ${pack.map { it.name }} holds an off-sheet card outside the land slot") {
                pack.dropLast(1).all { it.name in eligibleNames } shouldBe true
            }
            pack.dropLast(1).count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            val land = pack.last()
            (land.name in gainLands || land.typeLine.isBasicLand) shouldBe true
            if (land.name in gainLands) gainLandPacks++
            mythics += pack.count { it.metadata.rarity == Rarity.MYTHIC }
        }
        (gainLandPacks.toDouble() / packs) shouldBe (0.5 plusOrMinus 0.035)
        (mythics.toDouble() / packs) shouldBe (15.0 / 121 plusOrMinus 0.025)
        // A host ban on every gain land leaves the land slot to the basics.
        repeat(20) {
            val pack = boosterGenerator.generateBooster("IKO", bannedCardNames = gainLands)
            pack.size shouldBe 15
            pack.last().typeLine.isBasicLand shouldBe true
        }
    }

    test("Throne of Eldraine draft pack is 10 commons, 3 uncommons and one rare or mythic from its booster pool") {
        // Draft Booster (Project Booster Fun, magic.wizards.com 2019-07-20): 1 rare/mythic, 3 uncommons,
        // 10 commons, 1 basic land (the generator supplies basics at deck building) and an ad/token card.
        // Showcase and borderless variants and the foil replacing a common are cosmetic, not modelled.
        // The Brawl-deck cards (Oko, the Trickster etc.) are not booster cards.
        val eld = boosterGenerator.availableSets["ELD"].shouldNotBeNull()
        eld.fullyImplemented shouldBe true
        eld.boosterStrategy shouldBe StandardBooster(commons = 10)
        val eligible = eld.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
        // Scryfall `set:eld is:booster -t:basic`.
        eligible.distinctBy { it.name }.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 101, Rarity.UNCOMMON to 80, Rarity.RARE to 53, Rarity.MYTHIC to 15,
        )
        eligible.map { it.name } shouldNotContain "Oko, the Trickster"
        val eligibleNames = eligible.map { it.name }.toSet()
        var mythics = 0
        val packs = 2000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("ELD")
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

    test("Adventures in the Forgotten Realms draft pack is 10 commons, 3 uncommons and one rare or mythic from its booster pool") {
        // Draft Booster: 1 rare/mythic, 3 uncommons, 10 commons, 1 basic land (the generator supplies
        // basics at deck building), a token/ad card, and a foil of any rarity replacing a common in a
        // third of packs (cosmetic, not modelled). Dungeons are tokens, not booster cards.
        val afr = boosterGenerator.availableSets["AFR"].shouldNotBeNull()
        afr.fullyImplemented shouldBe true
        afr.boosterStrategy shouldBe StandardBooster(commons = 10)
        val eligible = afr.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
        // Scryfall `set:afr is:booster -t:basic`.
        eligible.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 101, Rarity.UNCOMMON to 80, Rarity.RARE to 60, Rarity.MYTHIC to 20,
        )
        val eligibleNames = eligible.map { it.name }.toSet()
        var mythics = 0
        val packs = 2000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("AFR")
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

    test("Kamigawa: Neon Dynasty draft pack opens one double-faced common or uncommon beside 9 commons, 3 uncommons and a rare") {
        // Draft Booster (product overview): 1 rare/mythic, 1 double-faced common or uncommon, 3 uncommons,
        // 9 commons, 1 basic land (an ukiyo-e basic in a third of packs; the generator supplies basics at
        // deck building) and a foil replacing a common in a third of packs (cosmetic, not modelled).
        val neo = boosterGenerator.availableSets["NEO"].shouldNotBeNull()
        neo.fullyImplemented shouldBe true
        neo.boosterStrategy.shouldBeInstanceOf<DedicatedSlotBooster>()
        val eligible = neo.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
        // Scryfall `set:neo is:booster -t:basic`.
        eligible.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 117, Rarity.UNCOMMON to 88, Rarity.RARE to 59, Rarity.MYTHIC to 18,
        )
        // Scryfall `set:neo is:booster is:dfc (r:c or r:u)`: 6 commons and 8 uncommons.
        val dfcSheet = eligible.filter {
            it.isDoubleFaced && (it.metadata.rarity == Rarity.COMMON || it.metadata.rarity == Rarity.UNCOMMON)
        }
        dfcSheet.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(Rarity.COMMON to 6, Rarity.UNCOMMON to 8)
        val dfcSheetNames = dfcSheet.mapTo(hashSetOf()) { it.name }
        val eligibleNames = eligible.mapTo(hashSetOf()) { it.name }
        var mythics = 0
        val packs = 2000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("NEO")
            pack.size shouldBe 14
            pack.map { it.name }.toSet().size shouldBe 14
            withClue("pack ${pack.map { it.name }} holds an off-sheet card") {
                pack.all { it.name in eligibleNames } shouldBe true
            }
            pack.count { it.name in dfcSheetNames } shouldBe 1
            pack.count { it.metadata.rarity == Rarity.COMMON && it.name !in dfcSheetNames } shouldBe 9
            pack.count { it.metadata.rarity == Rarity.UNCOMMON && it.name !in dfcSheetNames } shouldBe 3
            mythics += pack.count { it.metadata.rarity == Rarity.MYTHIC }
        }
        (mythics.toDouble() / packs) shouldBe (0.125 plusOrMinus 0.03)
    }

    test("Innistrad: Midnight Hunt draft pack opens two double-faced cards beside 9 commons, 3 uncommons and a rare") {
        // Draft Booster (collation per mtg.wtf/pack/mid-draft): 1 double-faced common, 9 single-faced commons,
        // 2 single-faced uncommons, and a double-faced/single-faced pair holding one uncommon and one
        // rare/mythic (the double-faced card is the rare in 3 packs of 18), plus a basic land (supplied at
        // deck building). The foil replacing a common in a third of packs is cosmetic and not modelled.
        val mid = boosterGenerator.availableSets["MID"].shouldNotBeNull()
        mid.fullyImplemented shouldBe true
        mid.boosterStrategy.shouldBeInstanceOf<DedicatedSlotBooster>()
        val eligible = mid.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
        // Scryfall `set:mid is:booster -t:basic`.
        eligible.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 100, Rarity.UNCOMMON to 83, Rarity.RARE to 64, Rarity.MYTHIC to 20,
        )
        // Scryfall `set:mid is:booster is:dfc`: 10 commons, 23 uncommons, 11 rares, 5 mythics.
        eligible.filter { it.isDoubleFaced }.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 10, Rarity.UNCOMMON to 23, Rarity.RARE to 11, Rarity.MYTHIC to 5,
        )
        val eligibleNames = eligible.mapTo(hashSetOf()) { it.name }
        var dfcRares = 0
        val packs = 2000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("MID")
            pack.size shouldBe 14
            pack.map { it.name }.toSet().size shouldBe 14
            withClue("pack ${pack.map { it.name }} holds an off-sheet card") {
                pack.all { it.name in eligibleNames } shouldBe true
            }
            pack.count { it.isDoubleFaced } shouldBe 2
            pack.count { it.isDoubleFaced && it.metadata.rarity == Rarity.COMMON } shouldBe 1
            pack.count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            val rare = pack.single { it.metadata.rarity == Rarity.RARE || it.metadata.rarity == Rarity.MYTHIC }
            if (rare.isDoubleFaced) dfcRares++
        }
        (dfcRares.toDouble() / packs) shouldBe ((1.0 / 6) plusOrMinus 0.03)
    }

    test("Zendikar Rising draft pack opens exactly one modal double-faced card beside 10 commons and a rare") {
        // Draft Booster (MTGJSON `booster.draft`): 10 commons, 3 uncommons, 1 rare or mythic and a basic land
        // (supplied at deck building), one of the uncommons or the rare being a modal double-faced card — the
        // double-faced card is the rare in 81 packs of 444. The foil replacing a common is not modelled.
        val znr = boosterGenerator.availableSets["ZNR"].shouldNotBeNull()
        znr.fullyImplemented shouldBe true
        znr.boosterStrategy.shouldBeInstanceOf<DedicatedSlotBooster>()
        val eligible = znr.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
        // Scryfall `set:znr is:booster -t:basic`.
        eligible.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 101, Rarity.UNCOMMON to 80, Rarity.RARE to 64, Rarity.MYTHIC to 20,
        )
        // Scryfall `set:znr is:booster is:dfc`: 20 uncommons, 11 rares, 5 mythics.
        eligible.filter { it.isDoubleFaced }.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.UNCOMMON to 20, Rarity.RARE to 11, Rarity.MYTHIC to 5,
        )
        val eligibleNames = eligible.mapTo(hashSetOf()) { it.name }
        var dfcRares = 0
        var mythics = 0
        val packs = 2000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("ZNR")
            pack.size shouldBe 14
            pack.map { it.name }.toSet().size shouldBe 14
            withClue("pack ${pack.map { it.name }} holds an off-sheet card") {
                pack.all { it.name in eligibleNames } shouldBe true
            }
            pack.count { it.isDoubleFaced } shouldBe 1
            pack.count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            val rare = pack.single { it.metadata.rarity == Rarity.RARE || it.metadata.rarity == Rarity.MYTHIC }
            if (rare.isDoubleFaced) dfcRares++
            if (rare.metadata.rarity == Rarity.MYTHIC) mythics++
        }
        (dfcRares.toDouble() / packs) shouldBe ((81.0 / 444) plusOrMinus 0.03)
        (mythics.toDouble() / packs) shouldBe (((15.0 + 45.0) / 444) plusOrMinus 0.03)
    }

    test("Strixhaven draft pack opens a Lesson beside 10 non-Lesson commons, 3 uncommons and a rare") {
        // Draft Booster (MTGJSON `booster.draft`): 9 non-Lesson commons, 3 uncommons, a non-Lesson rare or
        // mythic (60 mythic draws in 444), a Lesson (137 per common, 18 per rare, 9 for the mythic Lesson:
        // rare or mythic in 99 of 1332) and a Mystical Archive card. STA isn't a set here, so the Archive
        // slot opens a tenth STX common; the foil replacing a common is not modelled.
        val stx = boosterGenerator.availableSets["STX"].shouldNotBeNull()
        stx.fullyImplemented shouldBe true
        stx.boosterStrategy.shouldBeInstanceOf<DedicatedSlotBooster>()
        val eligible = stx.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
        // Scryfall `set:stx is:booster -t:basic`.
        eligible.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 105, Rarity.UNCOMMON to 80, Rarity.RARE to 69, Rarity.MYTHIC to 21,
        )
        val isLesson = { card: CardDefinition -> card.typeLine.hasSubtype(Subtype.LESSON) }
        // Scryfall `set:stx t:lesson`: 9 commons, 5 uncommons, 5 rares, 1 mythic.
        eligible.filter(isLesson).groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 9, Rarity.UNCOMMON to 5, Rarity.RARE to 5, Rarity.MYTHIC to 1,
        )
        val eligibleNames = eligible.mapTo(hashSetOf()) { it.name }
        val rareOrMythic = { card: CardDefinition -> card.metadata.rarity == Rarity.RARE || card.metadata.rarity == Rarity.MYTHIC }
        var rareLessons = 0
        var mythics = 0
        val packs = 2000
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("STX")
            pack.size shouldBe 15
            pack.map { it.name }.toSet().size shouldBe 15
            withClue("pack ${pack.map { it.name }} holds an off-sheet card") {
                pack.all { it.name in eligibleNames } shouldBe true
            }
            val slotLesson = pack.single { isLesson(it) && it.metadata.rarity != Rarity.UNCOMMON }
            pack.count { it.metadata.rarity == Rarity.COMMON && !isLesson(it) } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            val rare = pack.single { rareOrMythic(it) && !isLesson(it) }
            if (rareOrMythic(slotLesson)) rareLessons++
            if (rare.metadata.rarity == Rarity.MYTHIC) mythics++
        }
        (rareLessons.toDouble() / packs) shouldBe ((99.0 / 1332) plusOrMinus 0.02)
        (mythics.toDouble() / packs) shouldBe ((60.0 / 444) plusOrMinus 0.03)
    }

    test("March of the Machine: The Aftermath opens a 5-card Epilogue Booster from its 50-card pool") {
        // 2 uncommons, a rare/mythic and two any-rarity wildcards (foil + Booster Fun) that are each a
        // rare or mythic about 1 time in 6; the ad card/token slot is not modelled, so 5 playable cards.
        val mat = boosterGenerator.availableSets["MAT"].shouldNotBeNull()
        mat.fullyImplemented shouldBe true
        mat.extensionSet shouldBe true
        mat.boosterStrategy.shouldBeInstanceOf<PlayBooster>()
        val eligible = mat.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
        eligible.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.UNCOMMON to 15, Rarity.RARE to 25, Rarity.MYTHIC to 10,
        )
        val eligibleNames = eligible.map { it.name }.toSet()
        val packs = 2000
        var extraRares = 0
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("MAT")
            pack.size shouldBe 5
            pack.map { it.name }.toSet().size shouldBe 5
            withClue("pack ${pack.map { it.name }} holds an off-sheet card") {
                pack.all { it.name in eligibleNames } shouldBe true
            }
            val uncommons = pack.count { it.metadata.rarity == Rarity.UNCOMMON }
            (uncommons >= 2) shouldBe true
            extraRares += 4 - uncommons
        }
        (extraRares.toDouble() / (packs * 2)) shouldBe (1.0 / 6 plusOrMinus 0.03)

        // Host bans thin the pool: with every rare banned the rare slot falls through, no banned card opens.
        val bannedRares = eligible.filter { it.metadata.rarity == Rarity.RARE }.map { it.name }.toSet()
        repeat(200) {
            val pack = boosterGenerator.generateBooster("MAT", bannedCardNames = bannedRares)
            pack.size shouldBe 5
            pack.none { it.name in bannedRares } shouldBe true
        }
    }

    test("Legends draft pack is the classic 11 commons, 3 uncommons and a rare from its full pool") {
        // 15-card booster, 11C / 3U / 1R from the three print sheets; the set is 75 commons, 114
        // uncommons and 121 rares, with the C2/U2 doubling (46 commons, 9 uncommons) as print-run weights.
        val leg = boosterGenerator.availableSets["LEG"].shouldNotBeNull()
        leg.fullyImplemented shouldBe true
        leg.boosterStrategy.shouldBeInstanceOf<PrintSheetBooster>()
        val eligible = leg.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
        eligible.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 75, Rarity.UNCOMMON to 114, Rarity.RARE to 121,
        )
        val eligibleNames = eligible.map { it.name }.toSet()
        repeat(500) {
            val pack = boosterGenerator.generateBooster("LEG")
            pack.size shouldBe 15
            pack.map { it.name }.toSet().size shouldBe 15
            withClue("pack ${pack.map { it.name }} holds an off-sheet card") {
                pack.all { it.name in eligibleNames } shouldBe true
            }
            pack.count { it.metadata.rarity == Rarity.COMMON } shouldBe 11
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            pack.count { it.metadata.rarity == Rarity.RARE } shouldBe 1
        }
        leg.basicLands.map { it.name }.toSet() shouldBe
            setOf("Plains", "Island", "Swamp", "Mountain", "Forest")
    }

    test("every Dominaria United draft pack opens exactly one legendary creature from its booster pool") {
        val dmu = boosterGenerator.availableSets["DMU"].shouldNotBeNull()
        dmu.fullyImplemented shouldBe true
        dmu.boosterStrategy.shouldBeInstanceOf<DedicatedSlotBooster>()
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

    test("every War of the Spark draft pack opens exactly one planeswalker and no Planeswalker Deck card") {
        // taw/magic-search-engine `war/draft.yaml`: 10 commons, 3 uncommons, 1 rare or mythic, and one
        // planeswalker — at the rare slot in 29 of 121 packs, otherwise one of the three uncommons.
        val war = boosterGenerator.availableSets["WAR"].shouldNotBeNull()
        war.fullyImplemented shouldBe true
        war.boosterStrategy.shouldBeInstanceOf<DedicatedSlotBooster>()
        val eligible = war.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
        // Scryfall `set:war is:booster -t:basic` and `set:war is:booster t:planeswalker`.
        eligible.distinctBy { it.name }.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.COMMON to 101, Rarity.UNCOMMON to 80, Rarity.RARE to 53, Rarity.MYTHIC to 15,
        )
        eligible.filter { it.isPlaneswalker() }.groupingBy { it.metadata.rarity }.eachCount() shouldBe mapOf(
            Rarity.UNCOMMON to 20, Rarity.RARE to 13, Rarity.MYTHIC to 3,
        )
        val deckExclusives = listOf(
            "Gideon, the Oathsworn", "Desperate Lunge", "Gideon's Battle Cry", "Gideon's Company",
            "Orzhov Guildgate", "Jace, Arcane Strategist", "Guildpact Informant", "Jace's Projection",
            "Jace's Ruse", "Simic Guildgate", "Tezzeret, Master of the Bridge",
        )
        deckExclusives.forEach { eligible.map { card -> card.name } shouldNotContain it }
        val eligibleNames = eligible.mapTo(hashSetOf()) { it.name }
        val packs = 2000
        var rarePlaneswalkers = 0
        repeat(packs) {
            val pack = boosterGenerator.generateBooster("WAR")
            pack.size shouldBe 14
            pack.map { it.name }.toSet().size shouldBe 14
            withClue("pack ${pack.map { it.name }} holds an off-sheet card") {
                pack.all { it.name in eligibleNames } shouldBe true
            }
            pack.count { it.isPlaneswalker() } shouldBe 1
            pack.count { it.metadata.rarity == Rarity.COMMON } shouldBe 10
            pack.count { it.metadata.rarity == Rarity.UNCOMMON } shouldBe 3
            val rare = pack.single { it.metadata.rarity == Rarity.RARE || it.metadata.rarity == Rarity.MYTHIC }
            if (rare.isPlaneswalker()) rarePlaneswalkers++
        }
        (rarePlaneswalkers.toDouble() / packs) shouldBe ((29.0 / 121) plusOrMinus 0.04)
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

    test("complete sets open their paper pack size, never repeating a name") {
        // Paper pack minus what the engine doesn't model (basic-land slots outside a land-slot set,
        // foil/List/Special Guest/bonus-sheet cards).
        val expected = mapOf(
            "ARN" to 8, "ATQ" to 8, "DRK" to 8, "FEM" to 8, "LEG" to 15, "PTK" to 8,
            "POR" to 13, "P02" to 13, "S99" to 13, "VOW" to 14, "KHM" to 15, "IKO" to 15, "M20" to 15, "RNA" to 15, "LCI" to 15, "MOM" to 14,
            "MKM" to 13, "OTJ" to 13, "MH3" to 13, "BLB" to 13, "DSK" to 14, "FDN" to 14, "INR" to 13,
            "DFT" to 14, "TDM" to 14, "FIN" to 14, "EOE" to 13, "SPM" to 14, "TLA" to 14, "ECL" to 13,
            "TMT" to 14, "SOS" to 13, "MSH" to 14, "HOB" to 14, "ZNR" to 14, "STX" to 15,
        )
        for ((code, size) in expected) {
            boosterGenerator.availableSets[code].shouldNotBeNull().fullyImplemented shouldBe true
            repeat(100) {
                val pack = boosterGenerator.generateBooster(code)
                withClue("$code pack ${pack.map { it.name }}") {
                    pack.size shouldBe size
                    pack.map { it.name }.toSet().size shouldBe size
                }
            }
        }
    }

    test("land-slot sets open their common nonbasic lands only in the land slot") {
        for ((code, setConfig) in boosterGenerator.availableSets) {
            val strategy = setConfig.boosterStrategy as? LandSlotBooster ?: continue
            val poolNames = setConfig.cards.mapTo(hashSetOf()) { it.name }
            withClue("$code land-slot names missing from its pool") {
                strategy.nonbasicLandNames.filterNot { it in poolNames } shouldBe emptyList<String>()
            }
            repeat(100) {
                val pack = boosterGenerator.generateBooster(code)
                withClue("$code pack ${pack.map { it.name }}") {
                    pack.dropLast(1).none { it.name in strategy.nonbasicLandNames || it.typeLine.isBasicLand } shouldBe true
                    val land = pack.last()
                    (land.typeLine.isBasicLand || land.name in strategy.nonbasicLandNames) shouldBe true
                }
            }
        }
    }

    test("Arabian Nights opens 8-card packs from its print sheets") {
        val arn = boosterGenerator.availableSets["ARN"].shouldNotBeNull()
        val sheets = arn.boosterStrategy.shouldBeInstanceOf<PrintSheetBooster>().sheets
        sheets.map { it.slots } shouldBe listOf(6, 2)
        // Every booster-eligible card sits on a sheet, so none is unreachable.
        val sheetNames = sheets.flatMap { it.cardCounts.keys }.toSet()
        arn.cards.filter { it.metadata.inBooster && !it.typeLine.isBasicLand }
            .map { it.name }.filterNot { it in sheetNames } shouldBe emptyList<String>()
        repeat(200) {
            val pack = boosterGenerator.generateBooster("ARN")
            pack.take(6).all { it.metadata.rarity == Rarity.COMMON } shouldBe true
            pack.drop(6).none { it.metadata.rarity == Rarity.COMMON } shouldBe true
        }
    }

    test("March of the Machine opens one battle and one transforming double-faced card per pack") {
        repeat(300) {
            val pack = boosterGenerator.generateBooster("MOM")
            withClue("pack ${pack.map { it.name }}") {
                pack.count { it.typeLine.isBattle } shouldBe 1
                pack.count { it.isDoubleFaced && !it.typeLine.isBattle } shouldBe 1
                pack.count { it.metadata.rarity == Rarity.RARE || it.metadata.rarity == Rarity.MYTHIC } shouldBe 1
            }
        }
    }

    test("Crimson Vow opens a double-faced common and a double-faced uncommon-or-rare per pack") {
        repeat(300) {
            val pack = boosterGenerator.generateBooster("VOW")
            withClue("pack ${pack.map { it.name }}") {
                pack.count { it.isDoubleFaced } shouldBe 2
                pack.count { it.isDoubleFaced && it.metadata.rarity == Rarity.COMMON } shouldBe 1
                pack.count { it.metadata.rarity == Rarity.RARE || it.metadata.rarity == Rarity.MYTHIC } shouldBe 1
            }
        }
    }
})

private fun CardDefinition.isPlaneswalker() = CardType.PLANESWALKER in typeLine.cardTypes
