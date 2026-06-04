package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Gloom Ripper:
 * {3}{B}{B} Creature — Elf Assassin 4/4
 * When this creature enters, target creature you control gets +X/+0 until end of turn
 * and up to one target creature an opponent controls gets -0/-X until end of turn, where X is
 * the number of Elves you control plus the number of Elf cards in your graveyard.
 *
 * Regression: "Elves you control" must count every Elf permanent, not only Elf creatures. A
 * noncreature Changeling permanent (Firdoch Core — Kindred Artifact — Shapeshifter) is an Elf
 * and must count toward X.
 */
class GloomRipperTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("noncreature Changeling artifact counts toward Gloom Ripper's X") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // A noncreature Changeling permanent — Firdoch Core is every creature type, so it is an Elf.
        driver.putPermanentOnBattlefield(activePlayer, "Firdoch Core")

        // The +X/+0 target: a vanilla 2/2 that is NOT an Elf, so it doesn't inflate the count.
        val bears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")

        // Cast Gloom Ripper so its enters trigger fires.
        val ripperCard = driver.putCardInHand(activePlayer, "Gloom Ripper")
        driver.giveMana(activePlayer, Color.BLACK, 5)
        driver.castSpell(activePlayer, ripperCard)
        driver.bothPass() // Resolve Gloom Ripper; enters trigger goes on the stack.

        // Choose the Grizzly Bears for the mandatory "creature you control" target.
        // The opponent controls no creatures, so the "up to one" enemy target is empty.
        driver.submitMultiTargetSelection(activePlayer, mapOf(0 to listOf(bears)))

        driver.bothPass() // Resolve the enters trigger.

        // X = Elves you control (Gloom Ripper itself + Firdoch Core via Changeling) = 2.
        // Grizzly Bears is a 2/2, so +X/+0 makes it 4/2. Before the fix, Firdoch Core was
        // ignored (it isn't a creature) and X would have been 1, yielding 3/2.
        val projected = projector.project(driver.state)
        projected.getPower(bears) shouldBe 4
        projected.getToughness(bears) shouldBe 2
    }
})
