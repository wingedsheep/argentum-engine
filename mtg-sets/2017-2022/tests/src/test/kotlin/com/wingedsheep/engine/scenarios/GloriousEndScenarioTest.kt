package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.akh.cards.GloriousEnd
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Glorious End (AKH #133).
 *
 *   End the turn.
 *   At the beginning of your next end step, you lose the game.
 *
 * The delayed trigger is scheduled in the same resolution that ends the turn, so these pin that it
 * survives the turn ending, never fires on an opponent's end step, and lands on the caster's next
 * own end step — the following turn's when cast on your own turn (this turn's end step is skipped),
 * and the very next turn's when cast on an opponent's turn.
 */
class GloriousEndScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + GloriousEnd)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    fun GameTestDriver.castGloriousEnd(caster: EntityId) {
        val spell = putCardInHand(caster, "Glorious End")
        giveMana(caster, Color.RED, 1)
        giveColorlessMana(caster, 2)
        castSpell(caster, spell).error shouldBe null
        bothPass() // resolve Glorious End -> end the turn
    }

    test("cast on your own turn: the turn ends, and you lose at the end step of your following turn") {
        val driver = createDriver()
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.castGloriousEnd(p1)

        withClue("Glorious End is exiled with the stack, not put into the graveyard") {
            driver.getExileCardNames(p1) shouldContain "Glorious End"
            driver.getGraveyardCardNames(p1) shouldNotContain "Glorious End"
        }
        withClue("the turn ended into the opponent's turn") { driver.activePlayer shouldBe p2 }

        driver.passPriorityUntil(Step.END, p2)
        driver.bothPass()
        withClue("the opponent's end step is not yours: no loss yet") { driver.state.gameOver shouldBe false }

        driver.passPriorityUntil(Step.END, p1)
        driver.state.gameOver shouldBe false
        driver.bothPass() // resolve the delayed trigger
        driver.state.gameOver shouldBe true
        driver.state.winnerId shouldBe p2
    }

    test("cast on an opponent's turn: you lose at the end step of your very next turn") {
        val driver = createDriver()
        val p1 = driver.player1
        val p2 = driver.player2

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, p2)
        driver.passPriority(p2) // hand priority to the non-active caster
        driver.castGloriousEnd(p1)

        // The opponent's ended turn still runs its cleanup (discard to hand size), then the caster's turn begins.
        driver.passPriorityUntil(Step.UPKEEP, p1)
        withClue("the opponent's turn ended into the caster's very next turn") { driver.state.turnNumber shouldBe 3 }
        driver.state.gameOver shouldBe false

        driver.passPriorityUntil(Step.END, p1)
        driver.bothPass() // resolve the delayed trigger
        driver.state.gameOver shouldBe true
        driver.state.winnerId shouldBe p2
    }
})
