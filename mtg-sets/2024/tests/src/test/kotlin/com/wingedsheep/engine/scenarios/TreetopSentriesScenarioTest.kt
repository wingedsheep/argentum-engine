package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.TreetopSentries
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Treetop Sentries (BLB).
 *
 * Oracle: "Reach / When this creature enters, you may forage. If you do, draw a card. (To forage,
 * exile three cards from your graveyard or sacrifice a Food.)"
 *
 * "**If** you do" — one resolution, not CR 603.12's "**When** you do", so the draw must ride the
 * forage inside the same resolution instead of going on the stack as a second object. The ETB is
 * only reachable by *casting* the creature: placing a permanent directly on the battlefield fires
 * no enters-the-battlefield triggers.
 */
class TreetopSentriesScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TreetopSentries, PredefinedTokens.Food))
        driver.initMirrorMatch(Deck.of("Forest" to 40), startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the Sentries and drain the stack until its ETB "you may forage" question is up. */
    fun GameTestDriver.castSentries(playerId: EntityId): EntityId {
        val card = putCardInHand(playerId, "Treetop Sentries")
        giveMana(playerId, Color.GREEN, 4)
        castSpell(playerId, card).isSuccess shouldBe true
        bothPass() // creature resolves, ETB trigger goes on the stack
        bothPass() // ETB trigger resolves
        return card
    }

    // ---------------------------------------------------------------------------------------------
    // Accept: the forage and the draw happen in one resolution.
    // ---------------------------------------------------------------------------------------------

    test("accepting the forage (sacrifice a Food) draws a card in the same resolution") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        // Empty graveyard, so only the sacrifice mode is feasible — the forage
        // ChooseActionEffect auto-executes it and there is no mode decision to answer.
        val food = driver.putPermanentOnBattlefield(active, "Food")

        val sentries = driver.castSentries(active)
        val handBefore = driver.getHandSize(active)

        driver.submitYesNo(active, true)

        driver.getHandSize(active) shouldBe handBefore + 1
        driver.state.getBattlefield(active) shouldNotContain food
        driver.state.getGraveyard(active) shouldContain food
        driver.findPermanent(active, "Treetop Sentries") shouldBe sentries

        // The draw was NOT a second stack object: answering "yes" both foraged and drew, and left
        // nothing behind on the stack. A `ReflexiveTriggerEffect` ("When you do, draw a card") would
        // have put an ability here instead, and the hand would still be at `handBefore`.
        driver.assertStackSize(0)
    }

    test("accepting the forage (exile three chosen graveyard cards) draws a card") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        // No Food, so only the exile mode is feasible; five candidates means the player
        // still picks which three.
        val grave = (1..5).map { driver.putCardInGraveyard(active, "Forest") }

        driver.castSentries(active)
        val handBefore = driver.getHandSize(active)

        driver.submitYesNo(active, true)

        val chosen = listOf(grave[1], grave[2], grave[4])
        driver.submitCardSelection(active, chosen)

        driver.getHandSize(active) shouldBe handBefore + 1
        driver.state.getExile(active) shouldContainAll chosen
        driver.state.getGraveyard(active) shouldContain grave[0]
        driver.state.getGraveyard(active) shouldContain grave[3]
        driver.assertStackSize(0)
    }

    // ---------------------------------------------------------------------------------------------
    // Decline: the gate that must not leak.
    // ---------------------------------------------------------------------------------------------

    test("declining the forage draws nothing and exiles nothing") {
        val driver = newDriver()
        val active = driver.activePlayer!!
        val food = driver.putPermanentOnBattlefield(active, "Food")
        val grave = (1..5).map { driver.putCardInGraveyard(active, "Forest") }

        driver.castSentries(active)
        val handBefore = driver.getHandSize(active)

        driver.submitYesNo(active, false)

        driver.getHandSize(active) shouldBe handBefore
        driver.state.getBattlefield(active) shouldContain food
        driver.state.getGraveyard(active) shouldContainAll grave
        driver.state.getExile(active).size shouldBe 0
        driver.assertStackSize(0)
    }

    // ---------------------------------------------------------------------------------------------
    // Neither mode feasible: no prompt at all.
    // ---------------------------------------------------------------------------------------------

    test("with an empty graveyard and no Food the may question is never asked and nothing is drawn") {
        // Forage has no "even if you can't" clause, so with neither mode feasible the prompt is
        // skipped outright rather than offered and refused.
        val driver = newDriver()
        val active = driver.activePlayer!!

        val card = driver.putCardInHand(active, "Treetop Sentries")
        driver.giveMana(active, Color.GREEN, 4)
        driver.castSpell(active, card).isSuccess shouldBe true
        driver.bothPass() // creature resolves, ETB trigger goes on the stack
        val handBefore = driver.getHandSize(active)
        driver.bothPass() // ETB trigger resolves — and asks nothing

        driver.pendingDecision.shouldBeNull()
        driver.getHandSize(active) shouldBe handBefore
        driver.state.getExile(active).size shouldBe 0
        driver.findPermanent(active, "Treetop Sentries") shouldBe card
    }
})
