package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hou.cards.UncageTheMenagerie
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Uncage the Menagerie — {X}{G}{G} Sorcery (Hour of Devastation #137)
 *
 * "Search your library for up to X creature cards with different names that each have mana value
 *  X, reveal them, put them into your hand, then shuffle."
 *
 * X drives both the mana-value filter on the pool and the "up to X" count, and "with different
 * names" is enforced server-side by `SelectionRestriction.OnePerCardName`.
 */
class UncageTheMenagerieScenarioTest : FunSpec({

    fun creature(name: String, cost: String) = card(name) {
        manaCost = cost
        typeLine = "Creature — Beast"
        power = 2
        toughness = 2
    }

    val bearA = creature("Test Bear A", "{1}{G}")
    val bearB = creature("Test Bear B", "{1}{G}")
    val bearC = creature("Test Bear C", "{1}{G}")
    val bigBeast = creature("Test Big Beast", "{2}{G}")

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(UncageTheMenagerie, bearA, bearB, bearC, bigBeast))
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.handNames() =
        state.getHand(player1).mapNotNull { state.getEntity(it)?.get<CardComponent>()?.name }

    test("X = 2 offers only mana value 2 creatures and takes at most two with different names") {
        val d = driver()
        val a1 = d.putCardOnTopOfLibrary(d.player1, "Test Bear A")
        val a2 = d.putCardOnTopOfLibrary(d.player1, "Test Bear A")
        val b = d.putCardOnTopOfLibrary(d.player1, "Test Bear B")
        val c = d.putCardOnTopOfLibrary(d.player1, "Test Bear C")
        val big = d.putCardOnTopOfLibrary(d.player1, "Test Big Beast")

        val spell = d.putCardInHand(d.player1, "Uncage the Menagerie")
        d.giveMana(d.player1, Color.GREEN, 4)
        d.castXSpell(d.player1, spell, xValue = 2).error shouldBe null
        d.bothPass()

        val select = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        withClue("only creature cards with mana value exactly X are offered") {
            select.options shouldContainExactlyInAnyOrder listOf(a1, a2, b, c)
            (big in select.options) shouldBe false
        }
        withClue("\"up to X\" caps the selection at two") {
            select.maxSelections shouldBe 2
        }

        d.submitCardSelection(d.player1, listOf(a1, a2)).error shouldBe null

        withClue("\"with different names\" keeps the second copy in the library") {
            d.handNames().count { it == "Test Bear A" } shouldBe 1
            ((a1 in d.state.getLibrary(d.player1)) xor (a2 in d.state.getLibrary(d.player1))) shouldBe true
            (big in d.state.getLibrary(d.player1)) shouldBe true
        }
    }

    test("X = 3 finds the mana value 3 creature and none of the two-drops") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player1, "Test Bear A")
        val big = d.putCardOnTopOfLibrary(d.player1, "Test Big Beast")

        val spell = d.putCardInHand(d.player1, "Uncage the Menagerie")
        d.giveMana(d.player1, Color.GREEN, 5)
        d.castXSpell(d.player1, spell, xValue = 3).error shouldBe null
        d.bothPass()

        val select = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        select.options shouldBe listOf(big)
        d.submitCardSelection(d.player1, listOf(big)).error shouldBe null

        d.handNames().count { it == "Test Big Beast" } shouldBe 1
        d.handNames().count { it == "Test Bear A" } shouldBe 0
    }

    test("X = 0 searches but finds nothing") {
        val d = driver()
        d.putCardOnTopOfLibrary(d.player1, "Test Bear A")
        val spell = d.putCardInHand(d.player1, "Uncage the Menagerie")
        val handBefore = d.getHandSize(d.player1)
        d.giveMana(d.player1, Color.GREEN, 2)
        d.castXSpell(d.player1, spell, xValue = 0).error shouldBe null
        d.bothPass()

        (d.pendingDecision is SelectCardsDecision) shouldBe false
        d.getHandSize(d.player1) shouldBe handBefore - 1
        d.state.getGraveyard(d.player1).any {
            d.state.getEntity(it)?.get<CardComponent>()?.name == "Uncage the Menagerie"
        } shouldBe true
    }
})
