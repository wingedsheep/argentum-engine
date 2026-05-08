package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards.BrightspearZealot
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Brightspear Zealot:
 *  - Vigilance.
 *  - Gets +2/+0 as long as you've cast two or more spells this turn.
 *
 * The +2/+0 is gated by the reusable `YouCastSpellsThisTurn` condition, which reads
 * `GameState.spellsCastThisTurnByPlayer` (populated at cast time, reset at end of
 * turn). These tests pin the threshold behavior end-to-end through the projector.
 */
class BrightspearZealotTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BrightspearZealot)
        return driver
    }

    test("base stats 2/4 when no spells have been cast this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        // Place Brightspear directly on the battlefield so the spell cast isn't counted.
        val zealot = driver.putCreatureOnBattlefield(you, "Brightspear Zealot")

        projector.getProjectedPower(driver.state, zealot) shouldBe 2
        projector.getProjectedToughness(driver.state, zealot) shouldBe 4
    }

    test("still base 2/4 after only one spell has been cast this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val zealot = driver.putCreatureOnBattlefield(you, "Brightspear Zealot")

        // Cast a single spell — count = 1, still under the threshold of 2.
        val bolt = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 1)
        driver.castSpell(you, bolt, listOf(you)).isSuccess shouldBe true

        projector.getProjectedPower(driver.state, zealot) shouldBe 2
        projector.getProjectedToughness(driver.state, zealot) shouldBe 4
    }

    test("gains +2/+0 once a second spell has been cast this turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val zealot = driver.putCreatureOnBattlefield(you, "Brightspear Zealot")

        val bolt1 = driver.putCardInHand(you, "Lightning Bolt")
        val bolt2 = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 2)

        driver.castSpell(you, bolt1, listOf(you)).isSuccess shouldBe true
        driver.castSpell(you, bolt2, listOf(you)).isSuccess shouldBe true

        // Two spells cast this turn — bonus is live even while spells are still on the stack.
        projector.getProjectedPower(driver.state, zealot) shouldBe 4
        projector.getProjectedToughness(driver.state, zealot) shouldBe 4
    }

    test("bonus turns off at end of turn when the spell-cast counter resets") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val you = driver.activePlayer!!
        val zealot = driver.putCreatureOnBattlefield(you, "Brightspear Zealot")

        val bolt1 = driver.putCardInHand(you, "Lightning Bolt")
        val bolt2 = driver.putCardInHand(you, "Lightning Bolt")
        driver.giveMana(you, Color.RED, 2)
        driver.castSpell(you, bolt1, listOf(you)).isSuccess shouldBe true
        driver.castSpell(you, bolt2, listOf(you)).isSuccess shouldBe true
        projector.getProjectedPower(driver.state, zealot) shouldBe 4

        // End the active player's turn — the per-turn cast tracker clears.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()

        projector.getProjectedPower(driver.state, zealot) shouldBe 2
        projector.getProjectedToughness(driver.state, zealot) shouldBe 4
    }
})
