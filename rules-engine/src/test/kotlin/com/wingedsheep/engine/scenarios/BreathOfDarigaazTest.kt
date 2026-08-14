package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.BreathOfDarigaaz
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Breath of Darigaaz.
 *
 * Breath of Darigaaz: {1}{R}
 * Sorcery — Kicker {2}
 * Deals 1 damage (4 if kicked) to each creature without flying and each player.
 *
 * Exercises the two axes that drove the card: fliers are spared, and the kicker
 * switches the amount from 1 to 4.
 */
class BreathOfDarigaazTest : FunSpec({

    val testFlyer = card("Test Flyer") {
        manaCost = "{2}"
        typeLine = "Creature — Bird"
        power = 2
        toughness = 2
        keywords(Keyword.FLYING)
    }

    // 1 toughness: dies to the unkicked 1 damage.
    val testGrounder = card("Test Grounder") {
        manaCost = "{1}"
        typeLine = "Creature — Beast"
        power = 1
        toughness = 1
    }

    // 4 toughness: survives 1 damage, dies only to the kicked 4 damage.
    val testWall = card("Test Wall") {
        manaCost = "{2}"
        typeLine = "Creature — Wall"
        power = 0
        toughness = 4
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BreathOfDarigaaz, testFlyer, testGrounder, testWall))
        return driver
    }

    test("unkicked deals 1 to non-fliers and each player, fliers spared") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingLife = 20)

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val flyer = driver.putCreatureOnBattlefield(player2, "Test Flyer")
        driver.putCreatureOnBattlefield(player2, "Test Grounder")
        repeat(2) { driver.putLandOnBattlefield(player1, "Mountain") }
        val breath = driver.putCardInHand(player1, "Breath of Darigaaz")

        val result = driver.submit(
            CastSpell(playerId = player1, cardId = breath, declaredCostSlot = null, paymentStrategy = PaymentStrategy.AutoPay)
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // Non-flier dies to 1 damage; flier untouched.
        driver.findPermanent(player2, "Test Grounder") shouldBe null
        driver.findPermanent(player2, "Test Flyer") shouldBe flyer
        // Both players (including the caster) take 1.
        driver.getLifeTotal(player1) shouldBe 19
        driver.getLifeTotal(player2) shouldBe 19
    }

    test("kicked deals 4 to non-fliers and each player, fliers still spared") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingLife = 20)

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val flyer = driver.putCreatureOnBattlefield(player2, "Test Flyer")
        driver.putCreatureOnBattlefield(player2, "Test Wall")
        repeat(4) { driver.putLandOnBattlefield(player1, "Mountain") }
        val breath = driver.putCardInHand(player1, "Breath of Darigaaz")

        val result = driver.submit(
            CastSpell(playerId = player1, cardId = breath, declaredCostSlot = ChoiceSlot.KICKED, paymentStrategy = PaymentStrategy.AutoPay)
        )
        result.isSuccess shouldBe true
        driver.bothPass()

        // 4-toughness non-flier dies only because the spell was kicked; flier untouched.
        driver.findPermanent(player2, "Test Wall") shouldBe null
        driver.findPermanent(player2, "Test Flyer") shouldBe flyer
        driver.getLifeTotal(player1) shouldBe 16
        driver.getLifeTotal(player2) shouldBe 16
    }

    test("unkicked leaves a 4-toughness non-flier alive") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingLife = 20)

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player2, "Test Wall")
        repeat(2) { driver.putLandOnBattlefield(player1, "Mountain") }
        val breath = driver.putCardInHand(player1, "Breath of Darigaaz")

        driver.submit(
            CastSpell(playerId = player1, cardId = breath, declaredCostSlot = null, paymentStrategy = PaymentStrategy.AutoPay)
        )
        driver.bothPass()

        // 1 damage is not lethal to the 4-toughness wall.
        driver.findPermanent(player2, "Test Wall") shouldNotBe null
    }
})
