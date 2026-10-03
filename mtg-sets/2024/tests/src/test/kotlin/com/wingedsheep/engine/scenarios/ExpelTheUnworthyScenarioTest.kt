package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.ChoiceSlot
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Expel the Unworthy (MH3 #25) — {1}{W} Sorcery, Kicker {2}{W}.
 *
 *   "Choose target creature with mana value 3 or less. If this spell was kicked, instead choose
 *    target creature. Exile the chosen creature, then its controller gains life equal to its
 *    mana value."
 *
 * Grizzly Bears (mana value 2) is reachable unkicked; Craw Wurm (mana value 6) only kicked. The
 * life goes to the exiled creature's controller, not the caster.
 */
class ExpelTheUnworthyScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("unkicked: exiles a mana value 3 or less creature and its controller gains that much life") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val bears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Expel the Unworthy")
        driver.giveMana(player, Color.WHITE, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bears)),
                paymentStrategy = PaymentStrategy.AutoPay,
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getExileCardNames(opponent) shouldContain "Grizzly Bears"
        driver.assertLifeTotal(opponent, 22)
        driver.assertLifeTotal(player, 20)
    }

    test("unkicked: cannot target a creature with mana value 4 or greater") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val wurm = driver.putCreatureOnBattlefield(opponent, "Craw Wurm")
        val spell = driver.putCardInHand(player, "Expel the Unworthy")
        driver.giveMana(player, Color.WHITE, 2)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(wurm)),
                paymentStrategy = PaymentStrategy.AutoPay,
            )
        ).outcome shouldNotBe Outcome.Done
        driver.findPermanent(opponent, "Craw Wurm") shouldBe wurm
    }

    test("kicked: exiles any creature and its controller gains life equal to its mana value") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val wurm = driver.putCreatureOnBattlefield(opponent, "Craw Wurm")
        val spell = driver.putCardInHand(player, "Expel the Unworthy")
        driver.giveMana(player, Color.WHITE, 5)

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(wurm)),
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay,
            )
        ).outcome shouldBe Outcome.Done
        driver.bothPass()

        driver.getExileCardNames(opponent) shouldContain "Craw Wurm"
        driver.assertLifeTotal(opponent, 26)
        driver.assertLifeTotal(player, 20)
    }
})
