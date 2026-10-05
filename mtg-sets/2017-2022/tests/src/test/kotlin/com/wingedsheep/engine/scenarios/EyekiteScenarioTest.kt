package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Eyekite (MH1 #49) — {1}{U} 1/2 Drake with flying.
 *
 * "This creature gets +2/+0 as long as you've drawn two or more cards this turn."
 *
 * Draws are made with a free test instant, because only a genuine draw increments the
 * cards-drawn-this-turn tracker (second printed ruling: putting cards into hand doesn't count).
 */
class EyekiteScenarioTest : FunSpec({

    val drawOne = card("Eyekite Draw One Test") {
        manaCost = "{0}"
        typeLine = "Instant"
        oracleText = "Draw a card."
        spell { effect = Effects.DrawCards(1) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + drawOne)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.drawWithSpell(player: EntityId) {
        val spell = putCardInHand(player, "Eyekite Draw One Test")
        castSpell(player, spell).error shouldBe null
        bothPass()
    }

    test("one draw leaves it a 1/2; the second draw makes it a 3/2") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val kite = driver.putCreatureOnBattlefield(player, "Eyekite")

        driver.drawWithSpell(player)
        withClue("one draw is below the threshold") {
            driver.state.projectedState.getPower(kite) shouldBe 1
            driver.state.projectedState.getToughness(kite) shouldBe 2
        }

        driver.drawWithSpell(player)
        withClue("two draws: +2/+0") {
            driver.state.projectedState.getPower(kite) shouldBe 3
            driver.state.projectedState.getToughness(kite) shouldBe 2
        }
    }

    test("draws made before it entered still count") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        repeat(2) { driver.drawWithSpell(player) }
        val kite = driver.putCreatureOnBattlefield(player, "Eyekite")

        driver.state.projectedState.getPower(kite) shouldBe 3
    }

    test("the opponent's draws don't count, and cards merely put into hand don't count") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        val kite = driver.putCreatureOnBattlefield(player, "Eyekite")

        repeat(2) { driver.putCardInHand(player, "Island") }
        repeat(2) {
            val spell = driver.putCardInHand(opponent, "Eyekite Draw One Test")
            driver.passPriority(player)
            driver.castSpell(opponent, spell).error shouldBe null
            driver.bothPass()
        }

        driver.state.projectedState.getPower(kite) shouldBe 1
    }

    test("the bonus ends when the turn does") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val kite = driver.putCreatureOnBattlefield(player, "Eyekite")

        repeat(2) { driver.drawWithSpell(player) }
        driver.state.projectedState.getPower(kite) shouldBe 3

        driver.passPriorityUntil(Step.UPKEEP)
        withClue("new turn, tracker reset") {
            driver.activePlayer shouldBe driver.getOpponent(player)
            driver.state.projectedState.getPower(kite) shouldBe 1
        }
    }
})
