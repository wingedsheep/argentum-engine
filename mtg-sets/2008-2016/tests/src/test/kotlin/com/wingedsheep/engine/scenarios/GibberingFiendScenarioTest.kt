package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gibbering Fiend (SOI #161) — {1}{R} 2/1 Devil.
 *
 *   When this creature enters, it deals 1 damage to each opponent.
 *   Delirium — At the beginning of each opponent's upkeep, if there are four or more card types
 *   among cards in your graveyard, this creature deals 1 damage to that player.
 *
 * The no-delirium graveyard already holds a land, so a cleanup discard can't tip it to four types.
 */
class GibberingFiendScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, player, driver.getOpponent(player))
    }

    test("entering deals 1 damage to each opponent") {
        val (driver, player, opponent) = newGame()
        val fiend = driver.putCardInHand(player, "Gibbering Fiend")
        driver.giveMana(player, Color.RED, 2)

        driver.castSpell(player, fiend).error shouldBe null
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 19
        driver.getLifeTotal(player) shouldBe 20
    }

    test("with delirium it deals 1 damage to the opponent on their upkeep") {
        val (driver, player, opponent) = newGame()
        driver.putCreatureOnBattlefield(player, "Gibbering Fiend")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Lava Spike")      // sorcery
        driver.putCardInGraveyard(player, "Ornithopter")     // artifact

        driver.passPriorityUntil(Step.UPKEEP, opponent)
        driver.stackSize shouldBe 1
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 19
        driver.getLifeTotal(player) shouldBe 20
    }

    test("without delirium the upkeep trigger doesn't fire") {
        val (driver, player, opponent) = newGame()
        driver.putCreatureOnBattlefield(player, "Gibbering Fiend")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Mountain")        // land

        driver.passPriorityUntil(Step.UPKEEP, opponent)
        driver.stackSize shouldBe 0
        driver.getLifeTotal(opponent) shouldBe 20
    }
})
