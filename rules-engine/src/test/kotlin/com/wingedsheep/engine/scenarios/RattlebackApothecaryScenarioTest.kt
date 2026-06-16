package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.RattlebackApothecary
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rattleback Apothecary — {2}{B} 3/2 Creature — Gorgon Warlock
 *
 * "Deathtouch"
 * "Whenever you commit a crime, target creature you control gains your choice of menace or lifelink
 * until end of turn."
 *
 * Verifies the crime trigger targets a creature you control and grants the chosen keyword (menace or
 * lifelink) until end of turn, plus the static Deathtouch.
 */
class RattlebackApothecaryScenarioTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(RattlebackApothecary)
        return driver
    }

    test("Rattleback Apothecary has deathtouch") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val apothecary = driver.putCreatureOnBattlefield(me, "Rattleback Apothecary")
        projector.project(driver.state).hasKeyword(apothecary, Keyword.DEATHTOUCH) shouldBe true
    }

    test("committing a crime grants the chosen creature menace until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val apothecary = driver.putCreatureOnBattlefield(me, "Rattleback Apothecary")

        // Commit a crime: cast Lightning Bolt at the opponent.
        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp))

        // The crime trigger goes on the stack above Bolt and asks for its target first.
        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(me, listOf(apothecary))
        driver.bothPass() // resolve the crime trigger -> modal choice

        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        modeDecision.options shouldBe listOf("Menace", "Lifelink")
        driver.submitDecision(me, OptionChosenResponse(modeDecision.id, 0))

        val projected = projector.project(driver.state)
        projected.hasKeyword(apothecary, Keyword.MENACE) shouldBe true
        projected.hasKeyword(apothecary, Keyword.LIFELINK) shouldBe false
    }

    test("choosing lifelink grants lifelink instead") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Mountain" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)
        val apothecary = driver.putCreatureOnBattlefield(me, "Rattleback Apothecary")

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, targets = listOf(opp))

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        driver.submitTargetSelection(me, listOf(apothecary))
        driver.bothPass()

        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(me, OptionChosenResponse(modeDecision.id, 1))

        val projected = projector.project(driver.state)
        projected.hasKeyword(apothecary, Keyword.LIFELINK) shouldBe true
        projected.hasKeyword(apothecary, Keyword.MENACE) shouldBe false
    }
})
