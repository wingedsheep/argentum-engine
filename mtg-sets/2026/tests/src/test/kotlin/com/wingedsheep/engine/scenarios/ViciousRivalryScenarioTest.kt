package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Vicious Rivalry (SOS #241) — {2}{B}{G} Sorcery.
 *
 * "As an additional cost to cast this spell, pay X life.
 *  Destroy all artifacts and creatures with mana value X or less."
 *
 * Proves the [com.wingedsheep.sdk.scripting.AdditionalCost.PayXLife] feature: X is declared at cast
 * time (paid as life) and the same X feeds the board wipe's `manaValueAtMostX()` filter via the
 * resolution X value.
 */
class ViciousRivalryScenarioTest : FunSpec({

    fun GameTestDriver.resolveStack() {
        var safety = 0
        while (stackSize > 0 && !isPaused && safety < 20) {
            bothPass(); safety++
        }
    }

    test("pay 2 life destroys artifacts and creatures with mana value 2 or less; higher MV survives") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // MV 2 creature (Grizzly Bears) and MV 0 artifact (Ornithopter) — both <= 2, should die.
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val thopter = driver.putCreatureOnBattlefield(opponent, "Ornithopter")
        // MV 4 creature (Hill Giant) — above the threshold, should survive.
        val giant = driver.putCreatureOnBattlefield(opponent, "Hill Giant")

        // Mana for {2}{B}{G}.
        repeat(2) { driver.putLandOnBattlefield(player, "Swamp") }
        driver.putLandOnBattlefield(player, "Forest")
        repeat(2) { driver.putLandOnBattlefield(player, "Swamp") }

        val rivalry = driver.putCardInHand(player, "Vicious Rivalry")
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = rivalry,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(payXLifeAmount = 2)
            )
        )
        driver.resolveStack()

        // Paid 2 life.
        driver.getLifeTotal(player) shouldBe 18
        // MV <= 2 artifacts and creatures destroyed.
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.findPermanent(opponent, "Ornithopter") shouldBe null
        // MV 4 creature survives.
        (driver.findPermanent(opponent, "Hill Giant") != null) shouldBe true
        // silence unused-warning intent
        listOf(bears, thopter, giant)
    }

    test("pay 0 life destroys only mana value 0 permanents") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val thopter = driver.putCreatureOnBattlefield(opponent, "Ornithopter") // MV 0 — dies
        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")  // MV 2 — survives

        repeat(4) { driver.putLandOnBattlefield(player, "Swamp") }
        driver.putLandOnBattlefield(player, "Forest")

        val rivalry = driver.putCardInHand(player, "Vicious Rivalry")
        driver.submitSuccess(
            CastSpell(
                playerId = player,
                cardId = rivalry,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(payXLifeAmount = 0)
            )
        )
        driver.resolveStack()

        driver.getLifeTotal(player) shouldBe 20
        driver.findPermanent(opponent, "Ornithopter") shouldBe null
        (driver.findPermanent(opponent, "Grizzly Bears") != null) shouldBe true
        listOf(thopter, bears)
    }
})
