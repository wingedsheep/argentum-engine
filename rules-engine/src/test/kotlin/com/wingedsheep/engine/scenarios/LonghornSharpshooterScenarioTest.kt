package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlotCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Longhorn Sharpshooter (OTJ #132) — {2}{R} Minotaur Rogue, 3/3, Reach, Plot {3}{R}.
 *
 *   "When this card becomes plotted, it deals 2 damage to any target."
 *
 * Exercises the SELF-bound [com.wingedsheep.sdk.dsl.Triggers.BecomesPlotted] trigger paired with
 * an "any target" damage effect whose source is the card sitting face up in exile (never on the
 * battlefield). Paying the plot cost (CR 718) fires the trigger.
 */
class LonghornSharpshooterScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("plotting Longhorn Sharpshooter fires its trigger and burns the chosen creature") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val target = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears") // 2/2
        val sharpshooter = driver.putCardInHand(player, "Longhorn Sharpshooter")
        driver.giveMana(player, Color.RED, 4) // plot cost {3}{R}

        // Plotting pauses for the "becomes plotted" trigger's target choice.
        driver.submit(PlotCard(player, sharpshooter)).isPaused shouldBe true
        driver.submitTargetSelection(player, listOf(target))
        driver.bothPass() // resolve the trigger

        // Sharpshooter is now plotted in exile, not on the battlefield.
        driver.getExile(player).contains(sharpshooter) shouldBe true
        driver.getCreatures(player).contains(sharpshooter) shouldBe false

        // 2/2 takes 2 damage and dies.
        driver.getCreatures(opponent).contains(target) shouldBe false
    }

    test("the trigger can deal 2 damage to a player") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val sharpshooter = driver.putCardInHand(player, "Longhorn Sharpshooter")
        driver.giveMana(player, Color.RED, 4)

        driver.submit(PlotCard(player, sharpshooter)).isPaused shouldBe true
        driver.submitTargetSelection(player, listOf(opponent))
        driver.bothPass()

        driver.getLifeTotal(opponent) shouldBe 18
    }
})
