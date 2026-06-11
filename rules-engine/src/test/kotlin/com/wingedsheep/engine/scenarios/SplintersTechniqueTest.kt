package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Splinter's Technique (TMT #80) — Sorcery, Sneak {1}{B}. "Search your library
 * for a card, put that card into your hand, then shuffle."
 */
class SplintersTechniqueTest : FunSpec({
    test("tutors a chosen card from library into hand") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30), startingLife = 20)
        val player = driver.activePlayer!!
        val spell = driver.putCardInHand(player, "Splinter's Technique")
        driver.giveMana(player, Color.BLACK, 4)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val handBefore = driver.getHandSize(player)
        driver.castSpell(player, spell).isSuccess shouldBe true
        while (driver.state.stack.isNotEmpty() && driver.pendingDecision == null) driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, listOf(decision.options.first()))
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        // Spell left hand (-1), tutored card entered hand (+1) → net unchanged.
        driver.getHandSize(player) shouldBe handBefore - 1 + 1
    }
})
