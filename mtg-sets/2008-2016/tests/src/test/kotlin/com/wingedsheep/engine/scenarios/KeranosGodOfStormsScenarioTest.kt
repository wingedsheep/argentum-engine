package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.jou.cards.KeranosGodOfStorms
import com.wingedsheep.mtg.sets.definitions.scg.cards.PrimitiveEtchings
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Keranos, God of Storms — "Reveal the first card you draw on each of your turns. Whenever you
 * reveal a land card this way, draw a card. Whenever you reveal a nonland card this way, Keranos
 * deals 3 damage to any target."
 *
 * `RevealFirstDrawEachTurn(yourTurnsOnly = true)`: the reveal applies only on its controller's
 * turns, and the two triggers fire only off Keranos's own reveal (CR 607.1), so Primitive Etchings
 * revealing a draw on an opponent's turn doesn't fire them.
 */
class KeranosGodOfStormsScenarioTest : FunSpec({

    val quickDraw = card("Test Quick Draw") {
        manaCost = "{U}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }

    // Starting player's first turn: no draw-step draw, so the instant's draw is the turn's first.
    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KeranosGodOfStorms, PrimitiveEtchings, quickDraw))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && pendingDecision == null && guard++ < 30) bothPass()
    }

    test("revealing a land on your turn draws a card") {
        val driver = createDriver()
        val me = driver.player1
        driver.putPermanentOnBattlefield(me, "Keranos, God of Storms")
        val forest = driver.putCardOnTopOfLibrary(me, "Forest")
        val libBefore = driver.state.getLibrary(me).size

        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, driver.putCardInHand(me, "Test Quick Draw"))
        driver.resolveStack()

        driver.events.filterIsInstance<CardRevealedFromDrawEvent>().map { it.cardEntityId } shouldBe listOf(forest)
        driver.state.getLibrary(me).size shouldBe libBefore - 2
    }

    test("revealing a nonland on your turn deals 3 damage to any target") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2
        driver.putPermanentOnBattlefield(me, "Keranos, God of Storms")
        driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val libBefore = driver.state.getLibrary(me).size

        driver.giveMana(me, Color.BLUE, 1)
        driver.castSpell(me, driver.putCardInHand(me, "Test Quick Draw"))
        driver.resolveStack()

        (driver.pendingDecision is ChooseTargetsDecision) shouldBe true
        driver.submitTargetSelection(me, listOf(opponent))
        driver.resolveStack()

        driver.getLifeTotal(opponent) shouldBe 17
        driver.state.getLibrary(me).size shouldBe libBefore - 1
    }

    test("the first card drawn on an opponent's turn isn't revealed") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2
        driver.putPermanentOnBattlefield(me, "Keranos, God of Storms")
        driver.passPriorityUntil(Step.UPKEEP)
        (driver.activePlayer == opponent) shouldBe true

        driver.putCardOnTopOfLibrary(me, "Forest")
        val libBefore = driver.state.getLibrary(me).size
        driver.giveMana(me, Color.BLUE, 1)
        driver.passPriority(opponent)
        driver.castSpell(me, driver.putCardInHand(me, "Test Quick Draw"))
        driver.resolveStack()

        driver.events.filterIsInstance<CardRevealedFromDrawEvent>().size shouldBe 0
        driver.state.getLibrary(me).size shouldBe libBefore - 1
    }

    test("another source's reveal on an opponent's turn doesn't fire Keranos's triggers") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2
        val keranos = driver.putPermanentOnBattlefield(me, "Keranos, God of Storms")
        val etchings = driver.putPermanentOnBattlefield(me, "Primitive Etchings")
        driver.passPriorityUntil(Step.UPKEEP)

        val forest = driver.putCardOnTopOfLibrary(me, "Forest")
        val libBefore = driver.state.getLibrary(me).size
        driver.giveMana(me, Color.BLUE, 1)
        driver.passPriority(opponent)
        driver.castSpell(me, driver.putCardInHand(me, "Test Quick Draw"))
        driver.resolveStack()

        val reveal = driver.events.filterIsInstance<CardRevealedFromDrawEvent>().single()
        reveal.cardEntityId shouldBe forest
        reveal.revealedBy shouldBe listOf(etchings)
        (keranos in reveal.revealedBy) shouldBe false
        // Etchings revealed a land (no creature, no draw); Keranos's land trigger must not fire.
        driver.state.getLibrary(me).size shouldBe libBefore - 1
    }

    test("Keranos isn't a creature while devotion to blue and red is below seven") {
        val driver = createDriver()
        val keranos = driver.putPermanentOnBattlefield(driver.player1, "Keranos, God of Storms")

        driver.state.projectedState.isCreature(keranos) shouldBe false
    }
})
