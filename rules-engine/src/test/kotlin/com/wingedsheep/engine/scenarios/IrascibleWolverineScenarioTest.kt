package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Irascible Wolverine (OTJ #130) — {2}{R} Wolverine, 3/2, Plot {2}{R}.
 *
 *   "When this creature enters, exile the top card of your library. Until end of turn, you may
 *    play that card."
 *
 * Exercises the impulse-draw ETB (GatherCards → MoveCollection(EXILE) →
 * GrantMayPlayFromExile with [MayPlayExpiry.EndOfTurn]). Unlike Alania's Pathmaker (until end of
 * NEXT turn), this permission must expire at the cleanup step of the same turn.
 */
class IrascibleWolverineScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        return driver
    }

    test("ETB exiles the top card and grants permission to play it this turn") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(p1, "Mountain")
        val wolverine = driver.putCardInHand(p1, "Irascible Wolverine")
        driver.giveMana(p1, Color.RED, 3) // {2}{R}

        driver.castSpell(p1, wolverine).isSuccess shouldBe true
        driver.bothPass() // resolve the spell (Wolverine enters)
        if (driver.stackSize > 0) driver.bothPass() // resolve the ETB trigger

        // The Mountain is exiled and flagged playable.
        driver.getExileCardNames(p1) shouldBe listOf("Mountain")
        val exiled = driver.getExile(p1).single()
        driver.state.mayPlayPermissions.any { exiled in it.cardIds } shouldBe true

        // And it can actually be played from exile this turn.
        driver.playLand(p1, exiled).isSuccess shouldBe true
        driver.getExile(p1).contains(exiled) shouldBe false
    }

    test("the play permission expires at end of turn") {
        val driver = createDriver()
        val p1 = driver.activePlayer!!
        val p2 = driver.getOpponent(p1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCardOnTopOfLibrary(p1, "Mountain")
        val wolverine = driver.putCardInHand(p1, "Irascible Wolverine")
        driver.giveMana(p1, Color.RED, 3)

        driver.castSpell(p1, wolverine).isSuccess shouldBe true
        driver.bothPass()
        if (driver.stackSize > 0) driver.bothPass()

        val exiled = driver.getExile(p1).single()
        driver.state.mayPlayPermissions.any { exiled in it.cardIds } shouldBe true

        // Advance into the opponent's turn — the until-end-of-turn window is now closed.
        driver.passPriorityUntil(Step.END, maxPasses = 300)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 300)
        driver.state.activePlayerId shouldBe p2

        driver.state.mayPlayPermissions.any { exiled in it.cardIds } shouldBe false
    }
})
