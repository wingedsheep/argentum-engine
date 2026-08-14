package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Divine Resilience (FDN #10) — {W} Instant, Kicker {2}{W}.
 *
 *   "Target creature you control gains indestructible until end of turn. If this spell was
 *    kicked, instead any number of target creatures you control gain indestructible until
 *    end of turn."
 *
 * Verifies the base single-target grant and the kicked multi-target grant (any number of
 * target creatures you control).
 */
class DivineResilienceScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("unkicked: one target creature you control gains indestructible") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Divine Resilience")
        driver.giveMana(player, Color.WHITE, 1) // {W}

        driver.state.projectedState.hasKeyword(bear, Keyword.INDESTRUCTIBLE) shouldBe false

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bear)),
                paymentStrategy = PaymentStrategy.AutoPay,
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve

        driver.state.projectedState.hasKeyword(bear, Keyword.INDESTRUCTIBLE) shouldBe true
    }

    test("kicked: any number of target creatures you control gain indestructible") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bear1 = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val bear2 = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val spell = driver.putCardInHand(player, "Divine Resilience")
        driver.giveMana(player, Color.WHITE, 4) // {W} + kicker {2}{W}

        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bear1), ChosenTarget.Permanent(bear2)),
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay,
            )
        ).isSuccess shouldBe true
        driver.bothPass() // resolve

        driver.state.projectedState.hasKeyword(bear1, Keyword.INDESTRUCTIBLE) shouldBe true
        driver.state.projectedState.hasKeyword(bear2, Keyword.INDESTRUCTIBLE) shouldBe true
    }
})
