package com.wingedsheep.ai.engine

import com.wingedsheep.ai.assist.DeckBuildRequest
import com.wingedsheep.ai.assist.DraftsimDeckBuildAdvisor
import com.wingedsheep.ai.draftsim.DraftsimData
import com.wingedsheep.ai.draftsim.DraftsimScorer
import com.wingedsheep.ai.draftsim.toScorerCard
import com.wingedsheep.ai.engine.deck.ManaFixing
import com.wingedsheep.ai.llm.CardSummary
import com.wingedsheep.mtg.sets.MtgSetCatalog
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaSymbol
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.maps.shouldContainKey
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe

/**
 * Oath of the Gatewatch's `{C}` pips in AI Limited play. A `{C}` pip is paid only with colorless
 * mana, which no basic a Limited deck adds makes — the deck has to play the Wastes (or other
 * colorless lands) it drafted. Both deck builders used to treat Thought-Knot Seer ({3}{C}) as
 * either uncastable (the heuristic one) or as a plain four-drop castable off any basics (Draftsim),
 * never played Wastes, and the drafter took Wastes as a basic it could get for free.
 */
class ColorlessSourceDeckbuildTest : FunSpec({

    val ogw by lazy {
        val set = MtgSetCatalog.all.single { it.code == "OGW" }
        (set.cards + set.basicLands).distinctBy { it.name }.associateBy { it.name }
    }
    fun ogw(name: String) = ogw[name] ?: error("$name is not an OGW card")
    val wastes by lazy { ogw("Wastes") }
    val seer by lazy { ogw("Thought-Knot Seer") }
    val displacer by lazy { ogw("Eldrazi Displacer") }

    fun basic(name: String) = MtgSetCatalog.all.flatMap { it.basicLands }.first { it.name == name }

    fun colorlessPips(card: CardDefinition) =
        (listOf(card.manaCost) + ActivationMana.costs(card)).sumOf { cost -> cost.symbols.count { it is ManaSymbol.Colorless } }

    // Twenty G/W spells: too few to fill 23 slots, so a castable Thought-Knot Seer always makes the cut.
    val gwSpells by lazy {
        ogw.values.filter { card ->
            !card.typeLine.isLand && colorlessPips(card) == 0 && card.manaCost.symbols.isNotEmpty() &&
                card.manaCost.colors.isNotEmpty() && card.manaCost.colors.all { it == Color.GREEN || it == Color.WHITE }
        }.sortedBy { it.name }.take(20)
    }

    fun draftsimDeck(pool: List<CardDefinition>): Map<String, Int> {
        val result = DraftsimDeckBuildAdvisor.buildDeck(DeckBuildRequest(pool = pool, setCodes = listOf("OGW")))
        return result.builds[result.recommended].deckList
    }

    test("Wastes makes colorless mana a {C} pip can take; Forest doesn't") {
        ManaFixing.makesColorless(wastes) shouldBe true
        ManaFixing.makesColorless(basic("Forest")) shouldBe false
        CardSummary(name = "Wastes", typeLine = "Basic Land", oracleText = "({T}: Add {C}.)").toScorerCard().makesColorless shouldBe true
    }

    test("the Draftsim builder plays Thought-Knot Seer off the pool's Wastes") {
        gwSpells.size shouldBe 20
        val deck = draftsimDeck(gwSpells + seer + List(5) { wastes })
        deck.values.sum() shouldBe 40
        deck shouldContainKey "Thought-Knot Seer"
        // The Seer's one pip plus the three a splash gets — out of the five the pool holds.
        deck.getValue("Wastes") shouldBe 4
    }

    test("the Draftsim builder leaves Thought-Knot Seer out without colorless sources") {
        val deck = draftsimDeck(gwSpells + seer)
        deck.values.sum() shouldBe 40
        deck shouldNotContainKey "Thought-Knot Seer"
        deck shouldNotContainKey "Wastes"
    }

    test("two Wastes are too few: the Draftsim builder leaves Thought-Knot Seer and them out") {
        val deck = draftsimDeck(gwSpells + seer + List(2) { wastes })
        deck.values.sum() shouldBe 40
        deck shouldNotContainKey "Thought-Knot Seer"
        deck shouldNotContainKey "Wastes"
    }

    test("a {C} activation needs colorless sources too: Eldrazi Displacer's {2}{C} blink") {
        displacer.manaCost.symbols.none { it is ManaSymbol.Colorless } shouldBe true
        val without = draftsimDeck(gwSpells + displacer)
        without shouldNotContainKey "Eldrazi Displacer"
        buildHeuristicSealedDeck(gwSpells + displacer) shouldNotContainKey "Eldrazi Displacer"

        val with = draftsimDeck(gwSpells + displacer + List(5) { wastes })
        with shouldContainKey "Eldrazi Displacer"
        with.getValue("Wastes") shouldBe 4
        val heuristic = buildHeuristicSealedDeck(gwSpells + displacer + List(5) { wastes })
        heuristic shouldContainKey "Eldrazi Displacer"
        heuristic.getValue("Wastes") shouldBe 4
    }

    test("no {C} spell, no Wastes: the Draftsim builder plays basics of its colours instead") {
        val deck = draftsimDeck(gwSpells + List(5) { wastes })
        deck.values.sum() shouldBe 40
        deck shouldNotContainKey "Wastes"
    }

    test("the heuristic builder plays Thought-Knot Seer and its Wastes when the pool has them") {
        val deck = buildHeuristicSealedDeck(gwSpells + seer + List(5) { wastes })
        deck.values.sum() shouldBe 40
        deck shouldContainKey "Thought-Knot Seer"
        deck.getValue("Wastes") shouldBe 4
    }

    test("the heuristic builder leaves Thought-Knot Seer out with fewer than three colorless sources") {
        val deck = buildHeuristicSealedDeck(gwSpells + seer + List(2) { wastes })
        deck.values.sum() shouldBe 40
        deck shouldNotContainKey "Thought-Knot Seer"
        deck shouldNotContainKey "Wastes"
    }

    test("the drafter takes Wastes while its {C} spells need them, not before or after") {
        val scorer = DraftsimScorer(DraftsimData.tablesFor(listOf("OGW")))
        val pack = listOf(wastes.toScorerCard())
        fun wastesPick(picks: List<CardDefinition>) =
            scorer.scoreBoosterAuto(pack, picks.map { it.toScorerCard() }).getValue("Wastes").total

        val wanted = wastesPick(gwSpells.take(8) + seer + wastes)
        val unneeded = wastesPick(gwSpells.take(8))
        val enough = wastesPick(gwSpells.take(8) + seer + List(4) { wastes })
        wanted shouldBeGreaterThan unneeded
        wanted shouldBeGreaterThan enough
        // A source the Seer needs is a real pick — no longer a basic's "never draft".
        wanted shouldBeGreaterThan 2.0
    }

    test("Wastes rates as a pool land, not as a free basic") {
        LimitedCardRater.rate(wastes) shouldBeGreaterThan 0.0
        LimitedCardRater.rate(basic("Plains")) shouldBe 0.0
    }
})
