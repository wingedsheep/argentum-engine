package com.wingedsheep.ai.engine

import com.wingedsheep.ai.assist.DeckBuildRequest
import com.wingedsheep.ai.assist.DraftsimDeckBuildAdvisor
import com.wingedsheep.mtg.sets.definitions.arn.cards.CityOfBrass
import com.wingedsheep.mtg.sets.definitions.fin.cards.TheWanderingMinstrel
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
 * The deck builders hold a card to the colours its activated abilities need, not just its cast
 * cost. The Wandering Minstrel casts for {G}{U}, but its "{3}{W}{U}{B}{R}{G}: other creatures you
 * control get +X/+X" can't be paid by a G/U deck off basics — so it stays out, unless the pool's
 * lands fix the missing colours.
 */
class UnpayableActivationDeckbuildTest : FunSpec({

    fun creature(name: String, manaCost: String) = CardDefinition.creature(
        name = name,
        manaCost = ManaCost.parse(manaCost),
        subtypes = emptySet(),
        power = 2,
        toughness = 2,
        metadata = ScryfallMetadata(collectorNumber = name.hashCode().toString(), rarity = Rarity.COMMON),
    )

    // A G/U pool one card short of 23 spells without the Minstrel, so a builder that reads only
    // the cast cost would always take it.
    val guPool = (1..12).map { creature("Green $it", "{${1 + it % 4}}{G}") } +
        (1..10).map { creature("Blue $it", "{${1 + it % 4}}{U}") } +
        (1..4).map { creature("Red $it", "{4}{R}") } +
        TheWanderingMinstrel

    test("the Minstrel's five-colour activation is unpayable off G/U basics") {
        ActivationMana.payable(TheWanderingMinstrel, setOf(Color.GREEN, Color.BLUE)) shouldBe false
        ActivationMana.payable(TheWanderingMinstrel, Color.entries.toSet()) shouldBe true
        castableWithColors(TheWanderingMinstrel, setOf(Color.GREEN, Color.BLUE)) shouldBe true
        fitsDeckColors(TheWanderingMinstrel, setOf(Color.GREEN, Color.BLUE)) shouldBe false
    }

    test("Phyrexian and {2/C} activation pips never shut a card out; plain and hybrid pips need a colour, {C} a colorless source") {
        val gu = setOf(Color.GREEN, Color.BLUE)
        ActivationMana.payable(ManaCost.parse("{1}{R/P}"), gu) shouldBe true
        ActivationMana.payable(ManaCost.parse("{2/W}"), gu) shouldBe true
        ActivationMana.payable(ManaCost.parse("{G/W}"), gu) shouldBe true
        ActivationMana.payable(ManaCost.parse("{R/W}"), gu) shouldBe false
        // A {C} pip needs a colorless source (Wastes), which a basic-land mana base doesn't have.
        ActivationMana.payable(ManaCost.parse("{3}{C}"), gu) shouldBe false
        ActivationMana.payable(ManaCost.parse("{3}{C}"), gu, colorless = true) shouldBe true
    }

    test("the heuristic builder leaves the Minstrel out of a G/U deck with no fixing") {
        val deck = buildHeuristicSealedDeck(guPool)
        deck.values.sum() shouldBe 40
        deck shouldNotContainKey "The Wandering Minstrel"
    }

    test("the heuristic builder plays the Minstrel when the pool's lands make every colour") {
        val deck = buildHeuristicSealedDeck(guPool + List(3) { CityOfBrass })
        deck.values.sum() shouldBe 40
        deck shouldContainKey "City of Brass"
        deck shouldContainKey "The Wandering Minstrel"
    }

    test("the Draftsim builder leaves the Minstrel out of a G/U deck with no fixing") {
        val result = DraftsimDeckBuildAdvisor.buildDeck(DeckBuildRequest(pool = guPool, setCodes = listOf("FIN")))
        val deck = result.builds[result.recommended].deckList
        deck.values.sum() shouldBe 40
        deck shouldNotContainKey "The Wandering Minstrel"
    }
})
