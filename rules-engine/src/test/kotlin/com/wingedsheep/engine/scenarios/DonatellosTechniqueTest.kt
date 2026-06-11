package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Donatello's Technique (TMT #39) — {2}{U} Sorcery, Sneak {U}. "Draw two cards."
 * (Generic Sneak behavior is covered by SneakTest.)
 */
class DonatellosTechniqueTest : FunSpec({
    test("draws two cards") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20)
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, "Donatello's Technique")
        driver.giveMana(player, Color.BLUE, 3)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val before = driver.getHandSize(player)
        driver.castSpell(player, spell).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty()) driver.bothPass()
        // -1 for the spell leaving hand, +2 drawn.
        driver.getHandSize(player) shouldBe before - 1 + 2
    }
})
