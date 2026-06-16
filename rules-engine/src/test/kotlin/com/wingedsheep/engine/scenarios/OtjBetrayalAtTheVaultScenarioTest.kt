package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BetrayalAtTheVault
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Betrayal at the Vault {4}{G}{G} Instant (OTJ canonical).
 *
 * "Target creature you control deals damage equal to its power to each of two other target
 * creatures."
 *
 * Three target requirements: the source you control (index 0) and two `TargetOther` creatures
 * (indices 1 and 2). Both recipients take damage equal to the source's power, attributed to the
 * source creature.
 */
class OtjBetrayalAtTheVaultScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + BetrayalAtTheVault)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("source creature deals its power to each of two other target creatures, killing both") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Source: a 3/3 you control. Recipients: two 3/3s the opponent controls.
        val source = driver.putCreatureOnBattlefield(me, "Centaur Courser")       // 3/3 power-3 source
        val recipient1 = driver.putCreatureOnBattlefield(opp, "Centaur Courser")  // 3/3
        val recipient2 = driver.putCreatureOnBattlefield(opp, "Centaur Courser")  // 3/3

        driver.giveMana(me, Color.GREEN, 6)
        val spell = driver.putCardInHand(me, "Betrayal at the Vault")
        driver.castSpellWithTargets(
            me,
            spell,
            targets = listOf(
                ChosenTarget.Permanent(source),
                ChosenTarget.Permanent(recipient1),
                ChosenTarget.Permanent(recipient2)
            )
        ).error shouldBe null
        driver.bothPass()

        // Each recipient is a 3/3 that took 3 (lethal) damage → both die. Source is unharmed.
        driver.getPermanents(opp).contains(recipient1) shouldBe false
        driver.getPermanents(opp).contains(recipient2) shouldBe false
        driver.getPermanents(me).contains(source) shouldBe true
    }

    test("if one recipient has left, the source still damages the remaining recipient") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val source = driver.putCreatureOnBattlefield(me, "Centaur Courser")       // 3/3
        val recipient1 = driver.putCreatureOnBattlefield(opp, "Centaur Courser")  // 3/3, will survive removal
        val recipient2 = driver.putCreatureOnBattlefield(opp, "Centaur Courser")  // 3/3

        driver.giveMana(me, Color.GREEN, 6)
        val spell = driver.putCardInHand(me, "Betrayal at the Vault")
        driver.castSpellWithTargets(
            me,
            spell,
            targets = listOf(
                ChosenTarget.Permanent(source),
                ChosenTarget.Permanent(recipient1),
                ChosenTarget.Permanent(recipient2)
            )
        ).error shouldBe null

        // In response, bounce recipient1 off the battlefield (kill it with a Bolt) so only one
        // recipient remains a legal target at resolution.
        driver.giveMana(opp, Color.RED, 1)
        // Register/cast a removal targeting recipient1 — Lightning Bolt is in TestCards.
        val bolt = driver.putCardInHand(opp, "Lightning Bolt")
        driver.castSpellWithTargets(opp, bolt, targets = listOf(ChosenTarget.Permanent(recipient1)))
        driver.bothPass() // resolve Bolt -> recipient1 dies
        driver.bothPass() // resolve Betrayal -> remaining recipient still takes damage

        driver.getPermanents(opp).contains(recipient1) shouldBe false // killed by Bolt
        driver.getPermanents(opp).contains(recipient2) shouldBe false // still took 3 lethal from the source
        driver.getPermanents(me).contains(source) shouldBe true
    }
})
