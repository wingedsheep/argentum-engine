package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.DeepmuckDesperado
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Deepmuck Desperado — {2}{U} 2/4 Creature — Homarid Mercenary
 *
 * "Whenever you commit a crime, each opponent mills three cards. This ability triggers only once each turn."
 */
class DeepmuckDesperadoScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DeepmuckDesperado)
        return driver
    }

    test("committing a crime mills each opponent three, but only once each turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40, "Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        driver.putCreatureOnBattlefield(me, "Deepmuck Desperado")

        val oppGraveyardBefore = driver.getGraveyard(opp).size

        // Commit a crime: cast Lightning Bolt targeting the opponent.
        val bolt1 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt1, targets = listOf(opp))
        driver.bothPass() // resolve Bolt -> commit-crime event -> mill trigger on stack
        driver.bothPass() // resolve mill trigger

        driver.isPaused shouldBe false
        driver.getGraveyard(opp).size shouldBe oppGraveyardBefore + 3

        // Commit a SECOND crime the same turn — the ability triggers only once each turn,
        // so the opponent's graveyard should not grow further.
        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(opp))
        driver.bothPass()
        driver.bothPass()

        driver.isPaused shouldBe false
        driver.getGraveyard(opp).size shouldBe oppGraveyardBefore + 3
    }
})
