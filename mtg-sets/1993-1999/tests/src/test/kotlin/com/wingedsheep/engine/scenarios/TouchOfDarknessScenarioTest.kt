package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.leg.cards.TouchOfDarkness
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Touch of Darkness (LEG #122) — {B} Instant. "One or more target creatures become black until end
 * of turn."
 *
 * Pins "one or more": any number of targets (no cap), but at least one (CR 601.2c) — an empty
 * target list is rejected, and with no creature on the battlefield the spell can't be cast.
 */
class TouchOfDarknessScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(TouchOfDarkness)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("every chosen creature becomes black until end of turn") {
        val driver = newDriver()
        val me = driver.player1
        val foe = driver.player2
        val mine = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirs = driver.putCreatureOnBattlefield(foe, "Grizzly Bears")
        val untouched = driver.putCreatureOnBattlefield(foe, "Grizzly Bears")

        val touch = driver.putCardInHand(me, "Touch of Darkness")
        driver.giveMana(me, Color.BLACK, 1)
        driver.castSpell(me, touch, listOf(mine, theirs)).outcome shouldBe Outcome.Done
        driver.bothPass()

        val projected = driver.state.projectedState
        projected.getColors(mine) shouldBe setOf("BLACK")
        projected.getColors(theirs) shouldBe setOf("BLACK")
        projected.getColors(untouched) shouldBe setOf("GREEN")

        driver.passPriorityUntil(Step.END)
        driver.passPriorityUntil(Step.UPKEEP)
        driver.state.projectedState.getColors(mine) shouldBe setOf("GREEN")
    }

    test("choosing no targets is rejected") {
        val driver = newDriver()
        val me = driver.player1
        driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val touch = driver.putCardInHand(me, "Touch of Darkness")
        driver.giveMana(me, Color.BLACK, 1)
        driver.submitExpectFailure(
            CastSpell(playerId = me, cardId = touch, targets = emptyList(), paymentStrategy = PaymentStrategy.FromPool)
        )
    }

    test("with no creature on the battlefield it can't be cast") {
        val driver = newDriver()
        val me = driver.player1

        val touch = driver.putCardInHand(me, "Touch of Darkness")
        driver.giveMana(me, Color.BLACK, 1)
        // "Any number of target creatures" would still be castable here; a floor of one is not.
        driver.legalActions(me).none { (it.action as? CastSpell)?.cardId == touch } shouldBe true
    }
})
