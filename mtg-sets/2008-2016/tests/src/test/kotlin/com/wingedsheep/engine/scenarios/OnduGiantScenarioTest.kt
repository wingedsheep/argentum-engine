package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.roe.cards.OnduGiant
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ondu Giant (ROE #202) — {3}{G} 2/4 Creature — Giant Druid.
 *
 *   When this creature enters, you may search your library for a basic land card, put it onto
 *   the battlefield tapped, then shuffle.
 */
class OnduGiantScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(OnduGiant))
        d.initMirrorMatch(deck = Deck.of("Grizzly Bears" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun castGiant(d: GameTestDriver) {
        val you = d.activePlayer!!
        val giant = d.putCardInHand(you, "Ondu Giant")
        d.giveMana(you, Color.GREEN, 4)
        d.castSpell(you, giant).outcome shouldBe Outcome.Done
        d.bothPass() // resolve the creature spell
        d.bothPass() // resolve the ETB trigger
    }

    test("entering fetches a basic land onto the battlefield tapped") {
        val d = driver()
        val you = d.activePlayer!!
        val forest = d.putCardOnTopOfLibrary(you, "Forest")
        castGiant(d)

        d.submitYesNo(you, true)
        val search = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        search.options shouldBe listOf(forest)
        search.maxSelections shouldBe 1
        d.submitCardSelection(you, listOf(forest))

        d.getLands(you).contains(forest) shouldBe true
        d.isTapped(forest) shouldBe true
    }

    test("declining searches nothing") {
        val d = driver()
        val you = d.activePlayer!!
        val forest = d.putCardOnTopOfLibrary(you, "Forest")
        castGiant(d)

        d.submitYesNo(you, false)
        d.isPaused shouldBe false
        d.getLands(you).contains(forest) shouldBe false
    }
})
