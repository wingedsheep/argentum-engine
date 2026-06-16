package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.RavenOfFellOmens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec

/**
 * Raven of Fell Omens (OTJ #101) — {1}{B} Creature — Bird 1/2.
 *
 * "Flying. Whenever you commit a crime, each opponent loses 1 life and you gain 1 life.
 *  This ability triggers only once each turn."
 *
 * Verifies the crime-triggered drain fires once (each opponent loses 1, controller gains 1) and
 * does not fire a second time in the same turn even after a second crime. Committing a crime is
 * exercised authentically by casting a spell that targets the opponent.
 */
class RavenOfFellOmensScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RavenOfFellOmens)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("committing a crime drains each opponent for 1 and gains the controller 1, once per turn") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.putCreatureOnBattlefield(me, "Raven of Fell Omens")

        // Commit a crime: cast Lightning Bolt at the opponent. Bolt deals 3, then the Raven drain
        // resolves (opponent -1, me +1).
        val bolt1 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt1, targets = listOf(opp))
        driver.bothPass() // resolve Bolt -> crime event -> drain trigger on stack
        driver.bothPass() // resolve the drain trigger

        driver.assertLifeTotal(opp, 20 - 3 - 1)
        driver.assertLifeTotal(me, 21)

        // Commit a SECOND crime the same turn — triggers only once each turn, so no extra drain.
        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(opp))
        driver.bothPass()
        driver.bothPass()

        driver.assertLifeTotal(opp, 20 - 3 - 1 - 3) // only the second Bolt's 3, no second drain
        driver.assertLifeTotal(me, 21)
    }
})
