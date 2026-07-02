package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ColorChosenResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.blb.cards.IridescentVinelasher
import com.wingedsheep.mtg.sets.definitions.neo.cards.UnchartedHaven
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Regression test for the interaction between Uncharted Haven (a land with an
 * "as it enters, choose a color" replacement effect) and Iridescent Vinelasher
 * (a landfall trigger that deals 1 damage to target opponent).
 *
 * Bug: When a land carries an [EntersWithChoice] replacement effect, PlayLandHandler
 * pauses for the choice and never runs trigger detection on the ZoneChangeEvent.
 * The resumer then committed the chosen value without firing entry triggers, so
 * landfall abilities silently failed to trigger.
 */
class IridescentVinelasherUnchartedHavenTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(IridescentVinelasher, UnchartedHaven))
        return driver
    }

    test("Uncharted Haven entering triggers Iridescent Vinelasher's landfall") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(player1, "Iridescent Vinelasher")

        val haven = driver.putCardInHand(player1, "Uncharted Haven")
        driver.playLand(player1, haven)

        // Uncharted Haven pauses for "choose a color" as it enters.
        driver.isPaused shouldBe true
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<ChooseColorDecision>()
        driver.submitDecision(player1, ColorChosenResponse(decision.id, Color.BLUE))

        // Landfall trigger should now be on the stack — auto-targets the only
        // opponent in a 2-player game. Resolve it.
        driver.passPriority(player1)
        driver.passPriority(player2)

        driver.getLifeTotal(player2) shouldBe 19
    }
})
