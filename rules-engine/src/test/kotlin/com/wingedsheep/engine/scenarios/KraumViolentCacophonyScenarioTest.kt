package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.KraumViolentCacophony
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Kraum, Violent Cacophony.
 *
 * Kraum, Violent Cacophony: {2}{U}{R}
 * Legendary Creature — Zombie Horror
 * 2/3
 * Flying
 * Whenever you cast your second spell each turn, put a +1/+1 counter on Kraum and draw a card.
 */
class KraumViolentCacophonyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(KraumViolentCacophony))
        return driver
    }

    test("your second spell each turn grows Kraum and draws a card") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Lightning Bolt" to 30),
            startingLife = 20,
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kraum = driver.putCreatureOnBattlefield(player1, "Kraum, Violent Cacophony")

        // First spell — no trigger.
        val bolt1 = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt1, listOf(player2))
        driver.passPriority(player1)
        driver.passPriority(player2)

        driver.state.projectedState.getPower(kraum) shouldBe 2
        driver.state.projectedState.getToughness(kraum) shouldBe 3

        // Second spell — trigger fires.
        val bolt2 = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt2, listOf(player2))
        // bolt2 is on the stack; hand size here is the baseline before the trigger draw.
        val handOnStack = driver.getHandSize(player1)

        // Resolve the trigger (top of stack).
        driver.passPriority(player1)
        driver.passPriority(player2)

        // Kraum grew and controller drew a card.
        driver.state.projectedState.getPower(kraum) shouldBe 3
        driver.state.projectedState.getToughness(kraum) shouldBe 4
        driver.getHandSize(player1) shouldBe handOnStack + 1

        // Resolve the bolt.
        driver.passPriority(player1)
        driver.passPriority(player2)
    }

    test("does not trigger on the first spell, nor on the third spell") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Lightning Bolt" to 30),
            startingLife = 20,
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kraum = driver.putCreatureOnBattlefield(player1, "Kraum, Violent Cacophony")

        // First spell.
        val bolt1 = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt1, listOf(player2))
        driver.passPriority(player1)
        driver.passPriority(player2)
        driver.state.projectedState.getPower(kraum) shouldBe 2

        // Second spell — fires once.
        val bolt2 = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt2, listOf(player2))
        driver.passPriority(player1)
        driver.passPriority(player2)
        driver.passPriority(player1)
        driver.passPriority(player2)
        driver.state.projectedState.getPower(kraum) shouldBe 3

        // Third spell — no further trigger.
        val bolt3 = driver.putCardInHand(player1, "Lightning Bolt")
        driver.giveMana(player1, Color.RED, 1)
        driver.castSpell(player1, bolt3, listOf(player2))
        driver.passPriority(player1)
        driver.passPriority(player2)

        driver.state.projectedState.getPower(kraum) shouldBe 3
        driver.state.projectedState.getToughness(kraum) shouldBe 4
    }

    test("opponent casting their second spell does not trigger Kraum") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Island" to 30, "Lightning Bolt" to 30),
            startingLife = 20,
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val kraum = driver.putCreatureOnBattlefield(player1, "Kraum, Violent Cacophony")

        driver.passPriority(player1)

        val bolt1 = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.castSpell(player2, bolt1, listOf(player1))
        driver.passPriority(player2)
        driver.passPriority(player1)

        driver.passPriority(player1)

        val bolt2 = driver.putCardInHand(player2, "Lightning Bolt")
        driver.giveMana(player2, Color.RED, 1)
        driver.castSpell(player2, bolt2, listOf(player1))
        driver.passPriority(player2)
        driver.passPriority(player1)
        driver.passPriority(player1)
        driver.passPriority(player2)

        // Kraum belongs to player1; player2's second spell does not grow it.
        driver.state.projectedState.getPower(kraum) shouldBe 2
        driver.state.projectedState.getToughness(kraum) shouldBe 3
    }
})
