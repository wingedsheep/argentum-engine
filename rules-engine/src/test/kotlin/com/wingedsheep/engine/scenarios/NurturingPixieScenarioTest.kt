package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.NurturingPixie
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nurturing Pixie (OTJ #20) — {W} Creature — Faerie Rogue 1/1.
 *
 * "Flying. When this creature enters, return up to one target non-Faerie, nonland permanent you
 *  control to its owner's hand. If a permanent was returned this way, put a +1/+1 counter on this
 *  creature."
 *
 * Verifies the ETB bounces a chosen non-Faerie nonland permanent you control and then grows the
 * Pixie to 2/2; and that declining the optional target places no counter (Pixie stays 1/1).
 */
class NurturingPixieScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(NurturingPixie)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("returning a permanent you control puts a +1/+1 counter on the Pixie") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // A non-Faerie nonland permanent you control to bounce.
        val bear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val pixieCard = driver.putCardInHand(me, "Nurturing Pixie")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, pixieCard).isSuccess shouldBe true
        driver.bothPass() // resolve creature -> enters -> ETB trigger on stack

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(bear))
        driver.bothPass() // resolve ETB return + counter

        // Bear returned to hand.
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(bear) shouldBe false
        driver.getHand(me).contains(bear) shouldBe true

        // Pixie grew to 2/2 from the +1/+1 counter.
        val pixie = driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD))
            .single { driver.getCardName(it) == "Nurturing Pixie" }
        driver.state.projectedState.getPower(pixie) shouldBe 2
        driver.state.projectedState.getToughness(pixie) shouldBe 2
    }

    test("declining the optional target places no counter") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val pixieCard = driver.putCardInHand(me, "Nurturing Pixie")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, pixieCard).isSuccess shouldBe true
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, emptyList())
        driver.bothPass()

        val pixie = driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD))
            .single { driver.getCardName(it) == "Nurturing Pixie" }
        // No return happened, so no counter — Pixie stays 1/1.
        driver.state.projectedState.getPower(pixie) shouldBe 1
        driver.state.projectedState.getToughness(pixie) shouldBe 1
    }
})
