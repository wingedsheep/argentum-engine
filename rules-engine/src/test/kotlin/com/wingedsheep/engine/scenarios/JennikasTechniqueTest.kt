package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Jennika's Technique (TMT #93) — Instant, Sneak {R}. "Deals 2 damage to each creature."
 */
class JennikasTechniqueTest : FunSpec({
    test("deals 2 damage to every creature on the battlefield") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putCreatureOnBattlefield(player, "Grizzly Bears")   // 2/2
        driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2
        val spell = driver.putCardInHand(player, "Jennika's Technique")
        driver.giveMana(player, Color.RED, 3)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.castSpell(player, spell).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // Both 2/2s take 2 damage and die.
        (driver.findPermanent(player, "Grizzly Bears") == null) shouldBe true
        (driver.findPermanent(opponent, "Grizzly Bears") == null) shouldBe true
    }
})
