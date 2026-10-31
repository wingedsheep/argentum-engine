package com.wingedsheep.ai.engine

import com.wingedsheep.mtg.sets.definitions.fra.cards.BloodlineRecollector
import com.wingedsheep.mtg.sets.definitions.fra.cards.CarnivorousCultivator
import com.wingedsheep.mtg.sets.definitions.fra.cards.HeartwoodCrafter
import com.wingedsheep.mtg.sets.definitions.fra.cards.HexhavenDuelingArena
import com.wingedsheep.mtg.sets.definitions.fra.cards.MeticulousCommons
import com.wingedsheep.mtg.sets.definitions.fra.cards.StingerquillAnnex
import com.wingedsheep.mtg.sets.definitions.hob.cards.GoblinTown
import com.wingedsheep.mtg.sets.definitions.mh3.cards.UnfathomableTruths
import com.wingedsheep.mtg.sets.definitions.ons.cards.WoodedFoothills
import com.wingedsheep.mtg.sets.definitions.roe.cards.EvolvingWilds
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe

/**
 * Regression for engine-vs-engine sealed games where the heuristic builder put a G/W deck on
 * 2x Goblin-town (a {B}/{R} land) and an R/W deck on 4x Unfathomable Truths ({4}{U}, devoid):
 * non-basic lands were added regardless of the colors they tap for, and castability was read off
 * [CardDefinition.colors], which devoid empties.
 */
class HeuristicDeckBuilderTest : FunSpec({

    fun creature(name: String, manaCost: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(manaCost),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = Rarity.COMMON),
    )

    // A clear G/W pool: green and white creatures, plus a thin red/blue tail that won't be chosen.
    fun greenWhitePool(): List<CardDefinition> =
        (1..12).map { creature("Green $it", "{1}{G}") } +
            (1..12).map { creature("White $it", "{1}{W}") } +
            (1..3).map { creature("Red $it", "{3}{R}") }

    test("a G/W build leaves out a land that only taps for {B} or {R}") {
        val deck = buildHeuristicSealedDeck(greenWhitePool() + GoblinTown + GoblinTown)

        deck shouldNotContainKey "Goblin-town"
        deck.values.sum() shouldBe 40
    }

    test("a B/R build keeps the dual for its pair") {
        val pool = (1..12).map { creature("Black $it", "{1}{B}") } +
            (1..12).map { creature("Red $it", "{1}{R}") } +
            GoblinTown

        buildHeuristicSealedDeck(pool) shouldContainKey "Goblin-town"
    }

    // 2026-10-31 production games 8d182668, aa98…, 7f03…: drafted decks ran off-colour tapped duals
    // (a {B}/{R} Annex in B/G — a tapped Swamp) and colourless utility lands with nothing to do in
    // place of basics. A dual making one deck colour is worse than that colour's basic.
    test("a B/G build leaves out an off-colour tapped dual and colourless utility lands without a payoff") {
        val pool = (1..12).map { creature("Black $it", "{1}{B}") } +
            (1..12).map { creature("Green $it", "{1}{G}") } +
            StingerquillAnnex + HexhavenDuelingArena + HexhavenDuelingArena + GoblinTown

        val deck = buildHeuristicSealedDeck(pool)

        deck shouldNotContainKey "Stingerquill Annex"
        deck shouldNotContainKey "Hexhaven Dueling Arena"
        deck shouldNotContainKey "Goblin-town"
        deck.values.sum() shouldBe 40
    }

    test("a colourless utility land with a payoff in the deck makes it, one copy at most") {
        val pool = (1..10).map { creature("Black $it", "{1}{B}") } +
            (1..10).map { creature("Green $it", "{1}{G}") } +
            List(2) { BloodlineRecollector } + List(2) { CarnivorousCultivator } + List(2) { HeartwoodCrafter } +
            HexhavenDuelingArena + HexhavenDuelingArena

        val deck = buildHeuristicSealedDeck(pool)

        deck["Hexhaven Dueling Arena"] shouldBe 1
        deck.values.sum() shouldBe 40
    }

    test("a land is on-colour only when it adds no colour the deck doesn't play, or fixes two of them") {
        val bg = setOf(Color.BLACK, Color.GREEN)
        landFitsColors(StingerquillAnnex, bg) shouldBe false
        landFitsColors(StingerquillAnnex, setOf(Color.BLACK, Color.RED)) shouldBe true
        // The splash colour counts: B/G splashing red runs the {B}/{R} dual as a red source.
        landFitsColors(StingerquillAnnex, bg + Color.RED) shouldBe true
        landFitsColors(HexhavenDuelingArena, bg) shouldBe false
    }

    test("a devoid spell needing {U} is not a candidate for a deck without blue") {
        val deck = buildHeuristicSealedDeck(greenWhitePool() + List(4) { UnfathomableTruths })

        deck shouldNotContainKey "Unfathomable Truths"
    }

    test("devoid spells still count toward their mana-cost color when picking colors") {
        spellColors(UnfathomableTruths) shouldBe setOf(Color.BLUE)
        castableWithColors(UnfathomableTruths, setOf(Color.BLUE, Color.RED)) shouldBe true
        castableWithColors(UnfathomableTruths, setOf(Color.RED, Color.WHITE)) shouldBe false
    }

    test("hybrid, Phyrexian and {C} pips are judged by what basics can pay") {
        val gw = setOf(Color.GREEN, Color.WHITE)
        castableWithColors(creature("Hybrid", "{1}{G/U}"), gw) shouldBe true
        castableWithColors(creature("Off Hybrid", "{1}{U/B}"), gw) shouldBe false
        castableWithColors(creature("Phyrexian", "{1}{U/P}"), gw) shouldBe true
        castableWithColors(creature("Twobrid", "{2/B}"), gw) shouldBe true
        castableWithColors(creature("Colorless Pip", "{2}{C}"), gw) shouldBe false
        castableWithColors(creature("Artifact", "{3}"), gw) shouldBe true
    }

    test("fetch lands fit by the basic types they name; a generic fetch fits any deck") {
        landFitsColors(WoodedFoothills, setOf(Color.GREEN, Color.RED)) shouldBe true
        // Half off-colour: a Forest that costs life.
        landFitsColors(WoodedFoothills, setOf(Color.GREEN, Color.WHITE)) shouldBe false
        landFitsColors(WoodedFoothills, setOf(Color.BLUE, Color.WHITE)) shouldBe false
        landFitsColors(EvolvingWilds, setOf(Color.BLUE, Color.WHITE)) shouldBe true
    }

    test("completion mode applies the same land and castability filters") {
        val pool = greenWhitePool() + GoblinTown + List(4) { UnfathomableTruths }
        val deck = buildHeuristicSealedDeck(
            pool = pool,
            locked = mapOf("Green 1" to 1, "White 1" to 1),
            resolveCard = { name -> pool.firstOrNull { it.name == name } },
        )

        deck shouldNotContainKey "Goblin-town"
        deck shouldNotContainKey "Unfathomable Truths"
        deck.values.sum() shouldBe 40
    }

    // --- Legend duplicates and planeswalkers -------------------------------------------------
    //
    // Production game 2e28f192 (Reality Fracture draft): the AI's U/B deck ran 2x Yargle, Goliath
    // of Otaria (a vanilla 3/9) and 2x Yargle, Glutton of Urborg while Jace, Reality Sculptor — an
    // on-color rare planeswalker — sat in the sideboard. FRA had no 17Lands/Draftsim table then, so
    // the heuristic rater decided: it gave a walker the generic 1.5 "other permanent" base (2.0 with
    // rarity, under the 3/9's 2.4), and the builder counted a second copy of a legend at full value.

    fun legend(name: String, manaCost: String, power: Int, toughness: Int) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(manaCost),
        subtypes = emptySet(),
        power = power,
        toughness = toughness,
        supertypes = setOf(Supertype.LEGENDARY),
        metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = Rarity.UNCOMMON),
    )

    test("a planeswalker with no rating data outrates a vanilla creature of its cost") {
        val walker = CardDefinition.planeswalker(
            name = "Test Walker",
            manaCost = ManaCost.parse("{3}{U}{U}"),
            subtypes = emptySet(),
            startingLoyalty = 5,
            metadata = ScryfallMetadata(collectorNumber = "pw1", rarity = Rarity.RARE),
        )
        val wall = legend("Vanilla Wall", "{4}{U}", 3, 9)

        (LimitedCardRater.rate(walker) > LimitedCardRater.rate(wall) + 0.5) shouldBe true
    }

    test("a legend's second copy is discounted; other duplicates keep their full rating") {
        val legendary = legend("Filler Legend", "{4}{U}", 3, 9)
        val common = creature("Common Body", "{1}{U}")
        val ratings = mapOf(legendary to 2.4, common to 2.0)

        val ranked = rankByMarginalValue(listOf(legendary, legendary, common, common)) { ratings.getValue(it) }

        ranked.map { it.slot.card.name } shouldContainExactly
            listOf("Filler Legend", "Common Body", "Common Body", "Filler Legend")
        ranked.last().value shouldBe 2.4 * LEGEND_EXTRA_COPY_FACTOR
        // A copy the player already locked into the deck counts as the first.
        rankByMarginalValue(listOf(legendary), alreadyIncluded = { 1 }) { 2.4 }.single().value shouldBe
            2.4 * LEGEND_EXTRA_COPY_FACTOR
    }

    test("production FRA pool: the on-color walker makes the deck") {
        val byName = MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name }
        val maindeck = listOf(
            "Lyra, Tolarian Archangel", "Cast Away Doubt", "Terminal Criticism", "Traxos, Academy Guardian",
            "Screeching Soulbreaker", "Living Library", "Diviner of Victory", "Proft, Consulting Detective",
            "Arni, Humble Scribe", "Jace's Machinations", "Jace's Machinations", "Void Extrapolator",
            "Extrapolate the Impossible", "Divining Duelist", "Divining Duelist", "Yargle, Goliath of Otaria",
            "Yargle, Goliath of Otaria", "Yargle, Glutton of Urborg", "Yargle, Glutton of Urborg",
            "Theorix Metamage", "Eye of Jace", "Semester Foreseer", "Way of the Deathbringer",
            "Transformative Commons",
        )
        val sideboard = listOf(
            "Vraska, the Cutting Glare", "Vraska, Soul of Stone", "Jiang Yanggu, Alone", "Campus Crier",
            "Campus Crier", "Saheeli, Jewel of Avishkar", "Shatterwing Pegasus", "Academic Ascent",
            "Academic Ascent", "Solve for Disappointment", "Solve for Disappointment",
            "Solve for Disappointment", "Jace, Reality Sculptor", "Bestial Incursion",
            "Edgar, Ancient Bloodlord", "Way of the Cryomancer", "Winter, Team Player", "Charge the Sanctum",
        )
        val pool = (maindeck + sideboard).map { byName[it] ?: error("$it is not in the card catalog") }

        val deck = buildHeuristicSealedDeck(pool)

        deck.values.sum() shouldBe 40
        deck shouldContainKey "Jace, Reality Sculptor"
        // No Yargle-count assertion any more: FRA now has a vendored Draftsim table that rates both
        // Yargles 1.0, so whether a second copy makes the deck depends on how thin the U/B pool is,
        // not on the legend discount. The unit test above pins that discount on its own.
        // Still the pool's real colors: nothing white, red or green.
        val spells = deck.keys.mapNotNull { byName[it] }.filterNot { it.typeLine.isLand }
        spells.all { castableWithColors(it, setOf(Color.BLUE, Color.BLACK)) } shouldBe true
        // Its {G}/{U} Commons is a tapped Island in U/B.
        deck shouldNotContainKey "Transformative Commons"
    }

    // A drafted pool with only 19 castable spells in its pair used to make up the four-card gap in
    // basics: 21 lands beside 19 spells (2026-10-31 games 2d31a409, 8d182668, ee882769 ran 22/18).
    // A light black splash fills it instead, and the deck runs at most one land over 17.
    test("19 castable spells in the pair splash for depth rather than pad to 21 lands") {
        val pool = (1..10).map { creature("White $it", "{1}{W}") } +
            (1..9).map { creature("Blue $it", "{2}{U}") } +
            (1..4).map { creature("Black $it", "{3}{B}") } +
            creature("Green Giant", "{3}{G}{G}")

        val deck = buildHeuristicSealedDeck(pool)

        deck.values.sum() shouldBe 40
        val basics = listOf("Plains", "Island", "Swamp", "Forest", "Mountain")
        (basics.sumOf { deck[it] ?: 0 } <= 18) shouldBe true
        (1..4).count { "Black $it" in deck } shouldBe 3
        deck shouldNotContainKey "Green Giant"
        deck["Swamp"] shouldBe 3
    }

    test("a dual making the splash colour and a main colour is a splash source") {
        val pool = (1..10).map { creature("White $it", "{1}{W}") } +
            (1..9).map { creature("Blue $it", "{2}{U}") } +
            (1..4).map { creature("Black $it", "{3}{B}") } +
            MeticulousCommons + StingerquillAnnex

        val deck = buildHeuristicSealedDeck(pool)

        deck.values.sum() shouldBe 40
        deck["Meticulous Commons"] shouldBe 1
        deck shouldNotContainKey "Stingerquill Annex"
        deck["Swamp"] shouldBe 2
    }
})
