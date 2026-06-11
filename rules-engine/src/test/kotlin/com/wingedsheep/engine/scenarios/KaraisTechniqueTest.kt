package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Karai's Technique (TMT #152) — Sorcery, Sneak {W}{B}. "Choose one or both —
 * target creature gets +3/+3 / target creature gets -3/-3 until end of turn."
 */
class KaraisTechniqueTest : FunSpec({
    test("the +3/+3 mode pumps the targeted creature") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        val player = driver.activePlayer!!

        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears") // 2/2
        val spell = driver.putCardInHand(player, "Karai's Technique")
        driver.giveMana(player, Color.WHITE, 2)
        driver.giveMana(player, Color.BLACK, 1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(bear)),
                paymentStrategy = PaymentStrategy.FromPool,
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(listOf(ChosenTarget.Permanent(bear)))
            )
        ).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.projectedState.getPower(bear) shouldBe 5
        driver.state.projectedState.getToughness(bear) shouldBe 5
    }
})
