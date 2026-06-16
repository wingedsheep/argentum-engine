package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.OverzealousMuscle
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Overzealous Muscle — {4}{B} 5/4 Creature — Ogre Mercenary
 *
 * "Whenever you commit a crime during your turn, this creature gains indestructible until end of turn."
 *
 * Verifies: committing a crime on your own turn grants indestructible; committing a crime on an
 * opponent's turn does NOT (the "during your turn" intervening-if fails).
 */
class OverzealousMuscleScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(OverzealousMuscle)
        return driver
    }

    test("committing a crime during your turn grants indestructible") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val muscle = driver.putCreatureOnBattlefield(me, "Overzealous Muscle")

        projector.project(driver.state).hasKeyword(muscle, Keyword.INDESTRUCTIBLE) shouldBe false

        // Commit a crime on my own turn: cast Lightning Bolt targeting the opponent.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp))
        driver.bothPass() // resolve Bolt -> commit-crime event -> indestructible trigger on stack
        driver.bothPass() // resolve indestructible trigger

        projector.project(driver.state).hasKeyword(muscle, Keyword.INDESTRUCTIBLE) shouldBe true
    }

    test("committing a crime on the opponent's turn does not grant indestructible") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val muscle = driver.putCreatureOnBattlefield(me, "Overzealous Muscle")

        // Advance to the opponent's turn.
        driver.passPriorityUntil(Step.END)
        driver.bothPass()
        driver.activePlayer shouldBe opp
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        // Opponent (active player) passes so I get priority to cast an instant in response.
        driver.passPriority(opp)

        // Commit a crime on the opponent's turn: I cast Lightning Bolt targeting the opponent.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp))
        driver.bothPass()
        driver.bothPass()

        // The "during your turn" gate fails — no indestructible.
        projector.project(driver.state).hasKeyword(muscle, Keyword.INDESTRUCTIBLE) shouldBe false
    }
})
