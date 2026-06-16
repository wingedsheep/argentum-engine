package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.HardbristleBandit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Hardbristle Bandit — {1}{G} 1/1 Creature — Plant Rogue
 *
 * "{T}: Add one mana of any color.
 *  Whenever you commit a crime, untap this creature. This ability triggers only once each turn."
 *
 * Verifies: committing a crime untaps the (tapped) Bandit; a second crime the same turn does NOT
 * untap it again, because the ability triggers only once each turn.
 */
class HardbristleBanditScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(HardbristleBandit)
        return driver
    }

    test("committing a crime untaps the Bandit, but only once each turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val bandit = driver.putCreatureOnBattlefield(me, "Hardbristle Bandit")

        // Tap the Bandit (e.g. as if it had been used for mana).
        driver.tapPermanent(bandit)
        driver.isTapped(bandit) shouldBe true

        // Commit a crime: cast Lightning Bolt targeting the opponent.
        val bolt1 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt1, targets = listOf(opp))
        driver.bothPass() // resolve Bolt -> commit-crime event -> untap trigger on stack
        driver.bothPass() // resolve untap trigger

        // The Bandit is untapped by the crime trigger.
        driver.isTapped(bandit) shouldBe false

        // Tap it again, then commit a SECOND crime the same turn.
        driver.tapPermanent(bandit)
        driver.isTapped(bandit) shouldBe true

        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(opp))
        driver.bothPass()
        driver.bothPass()

        // The ability triggers only once each turn, so the Bandit stays tapped.
        driver.isTapped(bandit) shouldBe true
    }
})
