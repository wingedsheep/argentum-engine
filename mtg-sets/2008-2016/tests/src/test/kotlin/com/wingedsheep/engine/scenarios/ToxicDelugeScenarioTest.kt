package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c13.cards.ToxicDeluge
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Toxic Deluge (C13 #96) — {2}{B} Sorcery.
 *
 * "As an additional cost to cast this spell, pay X life.
 *  All creatures get -X/-X until end of turn."
 *
 * The life paid at cast time is the X the group -X/-X reads at resolution; it hits both
 * players' creatures, and creatures entering afterwards are unaffected.
 */
class ToxicDelugeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ToxicDeluge)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var safety = 0
        while (stackSize > 0 && !isPaused && safety < 20) {
            bothPass(); safety++
        }
    }

    fun GameTestDriver.castDeluge(x: Int) {
        val player = activePlayer!!
        repeat(3) { putLandOnBattlefield(player, "Swamp") }
        val deluge = putCardInHand(player, "Toxic Deluge")
        submitSuccess(
            CastSpell(
                playerId = player,
                cardId = deluge,
                paymentStrategy = PaymentStrategy.AutoPay,
                additionalCostPayment = AdditionalCostPayment(payXLifeAmount = x),
            )
        )
        resolveStack()
    }

    test("pay 2 life: every creature gets -2/-2, killing 2-toughness creatures on both sides") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val giant = driver.putCreatureOnBattlefield(opponent, "Hill Giant")

        driver.castDeluge(2)

        driver.getLifeTotal(player) shouldBe 18
        driver.findPermanent(player, "Grizzly Bears") shouldBe null
        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.findPermanent(opponent, "Hill Giant") shouldNotBe null
        driver.state.projectedState.getPower(giant) shouldBe 1
        driver.state.projectedState.getToughness(giant) shouldBe 1
    }

    test("pay 0 life: no creature changes and no life is lost") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        driver.castDeluge(0)

        driver.getLifeTotal(player) shouldBe 20
        driver.state.projectedState.getPower(bears) shouldBe 2
        driver.state.projectedState.getToughness(bears) shouldBe 2
    }

    test("creatures that enter after resolution are not affected") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.castDeluge(3)
        driver.getLifeTotal(player) shouldBe 17

        val lateBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        driver.state.projectedState.getPower(lateBears) shouldBe 2
        driver.state.projectedState.getToughness(lateBears) shouldBe 2
    }
})
