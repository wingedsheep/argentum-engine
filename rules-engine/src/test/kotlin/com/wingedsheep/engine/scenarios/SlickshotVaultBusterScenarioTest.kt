package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.SlickshotVaultBuster
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Slickshot Vault-Buster — {2}{U} 1/4 Creature — Human Rogue, Vigilance.
 *
 * "This creature gets +2/+0 as long as you've committed a crime this turn."
 *
 * Verifies the conditional static buff: a 1/4 until any crime is committed this turn, then a
 * 3/4 for the rest of the turn (the crime tracker is turn-scoped).
 */
class SlickshotVaultBusterScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(SlickshotVaultBuster)
        return driver
    }

    test("buff is inactive until a crime is committed, then makes it a 3/4") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 60), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val buster = driver.putCreatureOnBattlefield(me, "Slickshot Vault-Buster")

        // No crime yet — base 1/4.
        projector.getProjectedPower(driver.state, buster) shouldBe 1
        projector.getProjectedToughness(driver.state, buster) shouldBe 4

        // Commit a crime: cast Lightning Bolt at the opponent.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp))
        driver.bothPass() // resolve Bolt -> commit-crime tracker flips

        // Crime committed this turn — now 3/4.
        projector.getProjectedPower(driver.state, buster) shouldBe 3
        projector.getProjectedToughness(driver.state, buster) shouldBe 4
    }
})
