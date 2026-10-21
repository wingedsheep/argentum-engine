package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.afr.cards.PaladinsShield
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Paladin's Shield (AFR #30) — {1}{W} Artifact — Equipment.
 *
 * "Flash
 *  When this Equipment enters, attach it to target creature you control.
 *  Equipped creature gets +0/+2.
 *  Equip {3}"
 */
class PaladinsShieldScenarioTest : FunSpec({

    val projector = StateProjector()

    test("flashed in during the end step, the ETB trigger attaches it to a target creature you control") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(PaladinsShield)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val me = driver.activePlayer!!
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears") // 2/2
        val card = driver.putCardInHand(me, "Paladin's Shield")

        // Flash: cast outside a main phase.
        driver.passPriorityUntil(Step.END)
        driver.giveMana(me, Color.WHITE, 2)
        driver.castSpell(me, card)
        driver.bothPass() // resolve the spell -> it enters -> ETB trigger
        driver.pendingDecision shouldNotBe null
        driver.submitTargetSelection(me, listOf(bear))
        driver.bothPass() // resolve the ETB trigger

        val equipment = driver.findPermanent(me, "Paladin's Shield")!!
        driver.state.getEntity(equipment)?.get<AttachedToComponent>()?.targetId shouldBe bear
        projector.getProjectedPower(driver.state, bear) shouldBe 2 + 0
        projector.getProjectedToughness(driver.state, bear) shouldBe 2 + 2
    }
})
