package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.inv.cards.Probe
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Probe.
 *
 * Probe: {2}{U}
 * Sorcery — Kicker {1}{B}
 * Draw three cards, then discard two cards. If this spell was kicked, target player
 * discards two cards.
 *
 * Exercises both kicker branches: unkicked needs no target and only the caster discards;
 * kicked makes the targeted opponent discard two as well.
 */
class ProbeTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Probe))
        return driver
    }

    test("unkicked: caster draws three and discards two, no opponent discard") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Swamp" to 20), skipMulligans = true, startingLife = 20)

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val probe = driver.putCardInHand(player1, "Probe")
        val handBefore = driver.getHandSize(player1)
        val p2HandBefore = driver.getHandSize(player2)
        repeat(3) { driver.putLandOnBattlefield(player1, "Island") }

        driver.submit(
            CastSpell(playerId = player1, cardId = probe, declaredCostSlot = null, paymentStrategy = PaymentStrategy.AutoPay)
        )
        driver.bothPass()

        // Caster chooses two cards to discard.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player1, driver.getHand(player1).take(2))

        // Net hand: -1 cast, +3 drawn, -2 discarded = unchanged from pre-cast count.
        driver.getHandSize(player1) shouldBe handBefore
        // Two cards discarded to the graveyard plus the resolved sorcery.
        driver.getGraveyard(player1).size shouldBe 3
        // Unkicked → opponent untouched.
        driver.getHandSize(player2) shouldBe p2HandBefore
        driver.getGraveyard(player2).size shouldBe 0
    }

    test("kicked: targeted opponent also discards two") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 20, "Swamp" to 20), skipMulligans = true, startingLife = 20)

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val probe = driver.putCardInHand(player1, "Probe")
        val p2HandBefore = driver.getHandSize(player2)
        repeat(3) { driver.putLandOnBattlefield(player1, "Island") }
        repeat(3) { driver.putLandOnBattlefield(player1, "Swamp") }

        driver.submit(
            CastSpell(
                playerId = player1,
                cardId = probe,
                targets = listOf(ChosenTarget.Player(player2)),
                declaredCostSlot = ChoiceSlot.KICKED,
                paymentStrategy = PaymentStrategy.AutoPay
            )
        )
        driver.bothPass()

        // Caster discards two first.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player1, driver.getHand(player1).take(2))

        // Then the kicked clause makes the targeted opponent discard two.
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player2, driver.getHand(player2).take(2))

        driver.getHandSize(player2) shouldBe p2HandBefore - 2
        driver.getGraveyard(player2).size shouldBe 2
    }
})
