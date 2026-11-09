package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Soul Swallower (SOI #230) — {2}{G}{G} 3/3 Wurm with trample.
 *
 *   Delirium — At the beginning of your upkeep, if there are four or more card types among cards in
 *   your graveyard, put three +1/+1 counters on this creature.
 *
 * Pins whose upkeep it is (its controller's, not the opponent's) and the intervening-if. The
 * no-delirium graveyard already holds a land, so a cleanup discard can't tip it over to four types.
 */
class SoulSwallowerScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, player, driver.getOpponent(player))
    }

    test("with delirium it gets three +1/+1 counters on your upkeep, not the opponent's") {
        val (driver, player, opponent) = newGame()
        val swallower = driver.putCreatureOnBattlefield(player, "Soul Swallower")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Lava Spike")      // sorcery
        driver.putCardInGraveyard(player, "Ornithopter")     // artifact

        driver.passPriorityUntil(Step.UPKEEP, opponent)
        driver.stackSize shouldBe 0

        driver.passPriorityUntil(Step.UPKEEP, player)
        driver.stackSize shouldBe 1
        while (driver.state.stack.isNotEmpty()) driver.bothPass()

        driver.state.projectedState.getPower(swallower) shouldBe 6
        driver.state.projectedState.getToughness(swallower) shouldBe 6
    }

    test("without delirium nothing happens on your upkeep") {
        val (driver, player, _) = newGame()
        val swallower = driver.putCreatureOnBattlefield(player, "Soul Swallower")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Forest")          // land

        driver.passPriorityUntil(Step.UPKEEP, player)
        driver.stackSize shouldBe 0

        driver.state.projectedState.getPower(swallower) shouldBe 3
        driver.state.projectedState.getToughness(swallower) shouldBe 3
    }
})
