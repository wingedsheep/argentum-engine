package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.zen.cards.IntoTheRoil
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Into the Roil (ZEN #48) — "Return target nonland permanent to its owner's hand. If this spell
 * was kicked, draw a card." Exercises the kicker `WasKicked` branch on the bounce.
 */
class IntoTheRoilScenarioTest : FunSpec({

    fun newGame(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(IntoTheRoil))
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun resolveStack(driver: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && driver.state.stack.isNotEmpty() && !driver.isPaused) driver.bothPass()
    }

    test("unkicked: returns target nonland permanent to owner's hand and does not draw") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        val creature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        repeat(2) { driver.putLandOnBattlefield(you, "Island") } // {1}{U}

        val roil = driver.putCardInHand(you, "Into the Roil")
        val handBefore = driver.getHandSize(you)

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = roil,
                targets = listOf(ChosenTarget.Permanent(creature)),
                declaredCostSlot = null,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        ).isSuccess shouldBe true
        resolveStack(driver)

        // Creature bounced off the battlefield; no card drawn (hand only shrank by the spell itself).
        driver.state.getBattlefield().contains(creature) shouldBe false
        driver.getHandSize(you) shouldBe handBefore - 1
    }

    test("kicked: returns the permanent and draws a card") {
        val driver = newGame()
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)

        val creature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")
        repeat(4) { driver.putLandOnBattlefield(you, "Island") } // {1}{U} + kicker {1}{U}

        val roil = driver.putCardInHand(you, "Into the Roil")
        val handBefore = driver.getHandSize(you)

        driver.submit(
            CastSpell(
                playerId = you,
                cardId = roil,
                targets = listOf(ChosenTarget.Permanent(creature)),
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        ).isSuccess shouldBe true
        resolveStack(driver)

        // Creature bounced, and the kicker drew a card: -1 (spell cast) +1 (draw) = net unchanged.
        driver.state.getBattlefield().contains(creature) shouldBe false
        driver.getHandSize(you) shouldBe handBefore
    }
})
