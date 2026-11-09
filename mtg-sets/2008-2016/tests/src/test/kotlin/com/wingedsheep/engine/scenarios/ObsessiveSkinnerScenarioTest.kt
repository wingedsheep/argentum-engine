package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Obsessive Skinner (SOI #220) — {1}{G} 1/1 Human Rogue.
 *
 *   When this creature enters, put a +1/+1 counter on target creature.
 *   Delirium — At the beginning of each opponent's upkeep, if there are four or more card types
 *   among cards in your graveyard, put a +1/+1 counter on target creature.
 *
 * The no-delirium graveyard already holds a land, so a cleanup discard can't tip it to four types.
 */
class ObsessiveSkinnerScenarioTest : FunSpec({

    fun newGame(): Triple<GameTestDriver, EntityId, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return Triple(driver, player, driver.getOpponent(player))
    }

    /** Resolve the stack, aiming any targeted trigger at [target]. */
    fun settle(driver: GameTestDriver, player: EntityId, target: EntityId) {
        var guard = 0
        while (guard++ < 20) {
            when {
                driver.pendingDecision is ChooseTargetsDecision ->
                    driver.submitTargetSelection(player, listOf(target)).error shouldBe null
                driver.state.stack.isNotEmpty() -> driver.bothPass()
                else -> break
            }
        }
    }

    test("entering puts a +1/+1 counter on target creature") {
        val (driver, player, _) = newGame()
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        val skinner = driver.putCardInHand(player, "Obsessive Skinner")
        driver.giveMana(player, Color.GREEN, 2)

        driver.castSpell(player, skinner).error shouldBe null
        settle(driver, player, bears)

        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.getToughness(bears) shouldBe 3
    }

    test("with delirium it puts a +1/+1 counter on target creature on the opponent's upkeep") {
        val (driver, player, opponent) = newGame()
        driver.putCreatureOnBattlefield(player, "Obsessive Skinner")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Lava Spike")      // sorcery
        driver.putCardInGraveyard(player, "Ornithopter")     // artifact

        driver.passPriorityUntil(Step.UPKEEP, opponent)
        settle(driver, player, bears)

        driver.state.projectedState.getPower(bears) shouldBe 3
        driver.state.projectedState.getToughness(bears) shouldBe 3
    }

    test("without delirium nothing happens on the opponent's upkeep") {
        val (driver, player, opponent) = newGame()
        driver.putCreatureOnBattlefield(player, "Obsessive Skinner")
        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.putCardInGraveyard(player, "Grizzly Bears")   // creature
        driver.putCardInGraveyard(player, "Lightning Bolt")  // instant
        driver.putCardInGraveyard(player, "Forest")          // land

        driver.passPriorityUntil(Step.UPKEEP, opponent)
        driver.stackSize shouldBe 0
        (driver.pendingDecision is ChooseTargetsDecision) shouldBe false

        driver.state.projectedState.getPower(bears) shouldBe 2
    }
})
