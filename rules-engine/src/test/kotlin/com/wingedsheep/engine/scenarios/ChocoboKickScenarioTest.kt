package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Chocobo Kick (FIN #178) — {1}{G} Sorcery.
 *
 * "Kicker—Return a land you control to its owner's hand.
 *  Target creature you control deals damage equal to its power to target creature an opponent
 *  controls. If this spell was kicked, the creature you control deals twice that much damage
 *  instead."
 *
 * Tests the unkicked one-sided "fight": the controlled creature deals damage equal to its power to
 * the opposing creature, and the dealer takes none back. Composes ConditionalEffect(WasKicked, …)
 * + DealDamage(targetPower(0), source = controlled creature); no new SDK.
 */
class ChocoboKickScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("unkicked: 3-power creature deals 3 damage to the opposing 2/2, killing it; dealer survives") {
        val driver = newDriver()
        val yours = driver.putCreatureOnBattlefield(driver.player1, "Hill Giant") // 3/3
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears") // 2/2

        val kick = driver.putCardInHand(driver.player1, "Chocobo Kick")
        driver.giveMana(driver.player1, Color.GREEN, 1)
        driver.giveColorlessMana(driver.player1, 1)
        driver.castSpell(driver.player1, kick, listOf(yours, theirs)).isSuccess shouldBe true
        driver.bothPass()

        // 3 damage to a 2/2 is lethal; the 3/3 dealer takes nothing back and survives.
        driver.findPermanent(driver.player2, "Grizzly Bears") shouldBe null
        driver.findPermanent(driver.player1, "Hill Giant") shouldBe yours
    }
})
