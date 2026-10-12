package com.wingedsheep.ai.engine

import com.wingedsheep.mtg.sets.definitions.hob.cards.GoblinTown
import com.wingedsheep.mtg.sets.definitions.mh3.cards.UnfathomableTruths
import com.wingedsheep.mtg.sets.definitions.ons.cards.WoodedFoothills
import com.wingedsheep.mtg.sets.definitions.roe.cards.EvolvingWilds
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.model.ScryfallMetadata
import io.kotest.core.spec.style.FunSpec
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

    test("a B/x build keeps a land that taps for one of its colors") {
        val pool = (1..12).map { creature("Black $it", "{1}{B}") } +
            (1..12).map { creature("Green $it", "{1}{G}") } +
            GoblinTown

        buildHeuristicSealedDeck(pool) shouldContainKey "Goblin-town"
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
        landFitsColors(WoodedFoothills, setOf(Color.GREEN, Color.WHITE)) shouldBe true
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
})
