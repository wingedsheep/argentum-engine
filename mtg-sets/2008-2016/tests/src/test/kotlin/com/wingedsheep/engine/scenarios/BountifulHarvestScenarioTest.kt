package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m10.cards.BountifulHarvest
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bountiful Harvest {4}{G} — Sorcery
 *   You gain 1 life for each land you control.
 */
class BountifulHarvestScenarioTest : FunSpec({

    test("gains 1 life per land you control, not your opponent's") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BountifulHarvest))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        repeat(6) { driver.putLandOnBattlefield(me, "Forest") }
        repeat(3) { driver.putLandOnBattlefield(opp, "Forest") }

        val harvest = driver.putCardInHand(me, "Bountiful Harvest")
        driver.castSpell(me, harvest).error shouldBe null
        driver.bothPass()

        driver.getLifeTotal(me) shouldBe 26
        driver.getLifeTotal(opp) shouldBe 20
    }
})
