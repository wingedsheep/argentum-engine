package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.UreniTheSongUnending
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Ureni, the Song Unending (TDM, {5}{G}{U}{R}, 10/10).
 *
 * ETB: deals X damage divided among any number of target creatures/planeswalkers your opponents
 * control, X = lands you control. Exercises the new dynamic-total divided-damage path.
 */
class UreniTheSongUnendingScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UreniTheSongUnending))
        return driver
    }

    fun GameTestDriver.advanceToPlayer1Main() {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.PRECOMBAT_MAIN)
            safety++
        }
    }

    test("ETB divides X = lands you control among opponent creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.advanceToPlayer1Main()

        // Eight lands → enough to cast {5}{G}{U}{R} and X = 8.
        repeat(6) { driver.putLandOnBattlefield(driver.player1, "Forest") }
        driver.putLandOnBattlefield(driver.player1, "Island")
        driver.putLandOnBattlefield(driver.player1, "Mountain")

        val bearA = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears") // 2/2
        val bearB = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears") // 2/2

        val ureni = driver.putCardInHand(driver.player1, "Ureni, the Song Unending")
        driver.castSpell(driver.player1, ureni)

        // Resolve Ureni; its ETB trigger asks for targets.
        var safety = 0
        while (driver.pendingDecision == null && driver.state.stack.isNotEmpty() && safety < 20) {
            driver.bothPass(); safety++
        }
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitMultiTargetSelection(driver.player1, mapOf(0 to listOf(bearA, bearB)))

        // Now divide X = 8 damage: 5 to one bear, 3 to the other (both die regardless).
        val distribute = driver.pendingDecision as DistributeDecision
        distribute.totalAmount shouldBe 8
        driver.submitDecision(
            driver.player1,
            DistributionResponse(distribute.id, mapOf(bearA to 5, bearB to 3))
        )

        driver.bothPass()

        // Both 2/2 bears took lethal and are gone.
        val gone = driver.state.getBattlefield().none { it == bearA || it == bearB }
        gone shouldBe true
    }

    test("ETB with zero targets chosen does nothing") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.advanceToPlayer1Main()

        repeat(6) { driver.putLandOnBattlefield(driver.player1, "Forest") }
        driver.putLandOnBattlefield(driver.player1, "Island")
        driver.putLandOnBattlefield(driver.player1, "Mountain")

        val bear = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")

        val ureni = driver.putCardInHand(driver.player1, "Ureni, the Song Unending")
        driver.castSpell(driver.player1, ureni)

        var safety = 0
        while (driver.pendingDecision == null && driver.state.stack.isNotEmpty() && safety < 20) {
            driver.bothPass(); safety++
        }
        // Optional targeting — choose none.
        driver.pendingDecision as ChooseTargetsDecision
        driver.submitMultiTargetSelection(driver.player1, mapOf(0 to emptyList()))

        driver.bothPass()

        // The opponent's creature is unscathed.
        val projected = projector.project(driver.state)
        projected.getToughness(bear) shouldBe 2
        driver.state.getBattlefield().contains(bear) shouldBe true
    }
})
