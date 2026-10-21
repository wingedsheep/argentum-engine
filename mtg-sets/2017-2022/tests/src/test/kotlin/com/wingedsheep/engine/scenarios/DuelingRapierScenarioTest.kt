package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.DuelingRapier
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Dueling Rapier (AFR #140) — {R} Artifact — Equipment.
 *
 * "Flash
 *  When this Equipment enters, attach it to target creature you control.
 *  Equipped creature gets +2/+0.
 *  Equip {4}"
 */
class DuelingRapierScenarioTest : FunSpec({

    val projector = StateProjector()

    test("flashed in during the end step, the ETB trigger attaches it to a target creature you control") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DuelingRapier)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // 2/2
        val card = driver.putCardInHand(me, "Dueling Rapier")

        // Flash: cast outside a main phase.
        driver.passPriorityUntil(Step.END)
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, card)
        driver.bothPass() // resolve the spell -> it enters -> ETB trigger
        driver.pendingDecision shouldNotBe null
        driver.submitTargetSelection(me, listOf(bear))
        driver.bothPass() // resolve the ETB trigger

        val equipment = driver.findPermanent(me, "Dueling Rapier")!!
        driver.state.getEntity(equipment)?.get<AttachedToComponent>()?.targetId shouldBe bear
        projector.getProjectedPower(driver.state, bear) shouldBe 2 + 2
        projector.getProjectedToughness(driver.state, bear) shouldBe 2 + 0
    }
})
