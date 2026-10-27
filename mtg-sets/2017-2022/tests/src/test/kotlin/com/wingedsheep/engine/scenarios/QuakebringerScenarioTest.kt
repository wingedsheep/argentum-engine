package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khm.cards.Quakebringer
import com.wingedsheep.mtg.sets.definitions.lea.cards.HillGiant
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Quakebringer (KHM #145) — the upkeep trigger functions from the battlefield and the graveyard,
 * and from the graveyard only while you control a Giant. "This ability triggers only if …" is a
 * trigger-time restriction, not an intervening "if": a Giant that leaves after the trigger goes on
 * the stack does not stop the damage.
 */
class QuakebringerScenarioTest : FunSpec({

    /** Player 2 starts, so the next upkeep after turn 1's main phase belongs to player 1. */
    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + Quakebringer + HillGiant)
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20,
            startingPlayer = 1,
            skipMulligans = true,
        )
        return driver
    }

    fun GameTestDriver.drainStack() {
        var guard = 0
        while (stackSize > 0 && guard++ < 50) bothPass()
    }

    test("from the battlefield it deals 2 damage to each opponent with no Giant requirement") {
        val driver = newDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        driver.putPermanentOnBattlefield(me, "Quakebringer")

        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe me
        driver.stackSize shouldBe 1
        driver.drainStack()

        driver.getLifeTotal(opponent) shouldBe 18
        driver.getLifeTotal(me) shouldBe 20
    }

    test("from the graveyard it does not trigger without a Giant") {
        val driver = newDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        driver.putCardInGraveyard(me, "Quakebringer")

        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe me
        withClue("no Giant on my battlefield, so the graveyard half of the restriction fails") {
            driver.stackSize shouldBe 0
        }
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("from the graveyard it triggers while you control a Giant") {
        val driver = newDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        driver.putCardInGraveyard(me, "Quakebringer")
        driver.putPermanentOnBattlefield(me, "Hill Giant")

        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe me
        driver.stackSize shouldBe 1
        driver.drainStack()

        driver.getLifeTotal(opponent) shouldBe 18
    }

    test("an opponent's Giant does not satisfy the graveyard restriction") {
        val driver = newDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        driver.putCardInGraveyard(me, "Quakebringer")
        driver.putPermanentOnBattlefield(opponent, "Hill Giant")

        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe me
        driver.stackSize shouldBe 0
        driver.getLifeTotal(opponent) shouldBe 20
    }

    test("the restriction is not re-checked: losing the Giant in response still deals the damage") {
        val driver = newDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.player1
        val opponent = driver.getOpponent(me)
        driver.putCardInGraveyard(me, "Quakebringer")
        val giant = driver.putPermanentOnBattlefield(me, "Hill Giant")

        driver.passPriorityUntil(Step.UPKEEP)
        driver.stackSize shouldBe 1
        driver.moveToGraveyard(giant)
        driver.drainStack()

        withClue("a re-checked (intervening-if) condition would remove the ability from the stack") {
            driver.getLifeTotal(opponent) shouldBe 18
        }
    }
})
