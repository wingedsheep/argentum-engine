package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Oxidda Finisher (ONE #143) — {5}{R}{R} 7/5 Ogre Rebel.
 * Affinity for Equipment; Trample.
 */
class OxiddaFinisherScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("two Equipment reduce the cost to {3}{R}{R}") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putPermanentOnBattlefield(player, "Bonesplitter")
        driver.putPermanentOnBattlefield(player, "Short Sword")
        val finisher = driver.putCardInHand(player, "Oxidda Finisher")
        driver.giveMana(player, Color.RED, 2)
        driver.giveColorlessMana(player, 3)

        val cast = driver.castSpell(player, finisher)
        withClue("{5}{R}{R} minus two Equipment = {3}{R}{R}: ${cast.error}") { cast.error shouldBe null }
        driver.bothPass()

        driver.state.getBattlefield().contains(finisher) shouldBe true
        driver.state.projectedState.hasKeyword(finisher, Keyword.TRAMPLE) shouldBe true
    }

    test("non-Equipment artifacts don't reduce the cost") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putPermanentOnBattlefield(player, "Bonesplitter")
        driver.putPermanentOnBattlefield(player, "Ornithopter")
        val finisher = driver.putCardInHand(player, "Oxidda Finisher")
        driver.giveMana(player, Color.RED, 2)
        driver.giveColorlessMana(player, 3)

        val cast = driver.castSpell(player, finisher)
        withClue("only one Equipment: {4}{R}{R} can't be paid with five mana") { (cast.error != null) shouldBe true }
        driver.state.getBattlefield().contains(finisher) shouldBe false
    }
})
