package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.GiantBeaver
import com.wingedsheep.mtg.sets.definitions.otj.cards.ShepherdOfTheClouds
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Shepherd of the Clouds (OTJ #28) — {4}{W} Creature — Pegasus 4/3.
 *
 * "Flying, vigilance. When this creature enters, return target permanent card with mana value 3 or
 *  less from your graveyard to your hand. Return that card to the battlefield instead if you
 *  control a Mount."
 *
 * Verifies the destination switch: with no Mount the targeted graveyard permanent goes to hand;
 * controlling a Mount sends it to the battlefield instead.
 */
class ShepherdOfTheCloudsScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(ShepherdOfTheClouds, GiantBeaver))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("with no Mount, returns the graveyard permanent to hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // Grizzly Bears (MV 2, a permanent card) in the graveyard.
        val bear = driver.putCardInGraveyard(me, "Grizzly Bears")

        val shepherd = driver.putCardInHand(me, "Shepherd of the Clouds")
        driver.giveMana(me, Color.WHITE, 5)
        driver.castSpell(me, shepherd).isSuccess shouldBe true
        driver.bothPass() // resolve creature -> enters -> ETB trigger on stack

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(bear))
        driver.bothPass()

        // No Mount controlled -> the bear goes to hand.
        driver.getHand(me).contains(bear) shouldBe true
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(bear) shouldBe false
        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)).contains(bear) shouldBe false
    }

    test("controlling a Mount returns the graveyard permanent to the battlefield instead") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // Control a Mount (Giant Beaver — Creature — Beaver Mount).
        driver.putCreatureOnBattlefield(me, "Giant Beaver")

        val bear = driver.putCardInGraveyard(me, "Grizzly Bears")

        val shepherd = driver.putCardInHand(me, "Shepherd of the Clouds")
        driver.giveMana(me, Color.WHITE, 5)
        driver.castSpell(me, shepherd).isSuccess shouldBe true
        driver.bothPass()

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        driver.submitTargetSelection(me, listOf(bear))
        driver.bothPass()

        // A Mount is controlled -> the bear enters the battlefield instead of going to hand.
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(bear) shouldBe true
        driver.getHand(me).contains(bear) shouldBe false
    }
})
