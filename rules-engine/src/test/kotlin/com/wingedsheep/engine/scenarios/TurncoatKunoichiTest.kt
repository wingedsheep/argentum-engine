package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Turncoat Kunoichi (TMT #26) — ETB exiles a target opponent creature until Turncoat
 * leaves (normal cast). The sneak-paid permanent-exile branch is covered by the
 * SneakCostWasPaid plumbing in SneakTest.
 */
class TurncoatKunoichiTest : FunSpec({
    test("normal-cast ETB exiles a target opponent creature until Turncoat leaves") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val victim = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")
        val turncoat = driver.putCardInHand(player, "Turncoat Kunoichi")
        driver.giveMana(player, Color.WHITE, 3)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.castSpell(player, turncoat).isSuccess shouldBe true
        // Resolve Turncoat; its ETB trigger then asks for the target opponent creature.
        while (driver.pendingDecision == null && driver.state.stack.isNotEmpty()) driver.bothPass()
        driver.submitTargetSelection(player, listOf(victim))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null // exiled
    }
})
