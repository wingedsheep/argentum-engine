package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.PrimalHuntbeast
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Primal Huntbeast {3}{G} — Creature — Beast 3/3
 *   Hexproof
 */
class PrimalHuntbeastScenarioTest : FunSpec({

    test("opponents can't target it, but its controller can") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(PrimalHuntbeast))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val beast = driver.putCreatureOnBattlefield(opp, "Primal Huntbeast")

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(beast)).error shouldNotBe null

        val mine = driver.putCreatureOnBattlefield(me, "Primal Huntbeast")
        driver.castSpell(me, bolt, listOf(mine)).error shouldBe null
    }
})
