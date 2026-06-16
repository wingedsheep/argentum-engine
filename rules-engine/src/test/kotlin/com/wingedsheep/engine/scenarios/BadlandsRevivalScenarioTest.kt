package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.BadlandsRevival
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Badlands Revival (OTJ #194) — {3}{B}{G} Sorcery.
 *
 * "Return up to one target creature card from your graveyard to the battlefield. Return up to one
 *  target permanent card from your graveyard to your hand."
 *
 * Verifies the two independent "up to one" slots: a creature card is reanimated to the battlefield
 * and a (non-creature) permanent card is returned to hand in the same resolution, and that leaving
 * a slot empty is legal (no second target chosen).
 */
class BadlandsRevivalScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BadlandsRevival)
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 30, "Forest" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("reanimates a creature to the battlefield and returns a permanent to hand") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val bear = driver.putCardInGraveyard(me, "Grizzly Bears")     // creature card
        val land = driver.putCardInGraveyard(me, "Forest")            // permanent (land) card

        val spell = driver.putCardInHand(me, "Badlands Revival")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 3)
        driver.castSpellWithTargets(
            me,
            spell,
            listOf(
                ChosenTarget.Card(bear, me, Zone.GRAVEYARD),
                ChosenTarget.Card(land, me, Zone.GRAVEYARD),
            ),
        ).isSuccess shouldBe true
        driver.bothPass() // resolve the spell

        // Creature reanimated to battlefield; permanent returned to hand.
        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(bear) shouldBe true
        driver.getHand(me).contains(land) shouldBe true
        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)).contains(bear) shouldBe false
        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)).contains(land) shouldBe false
    }

    test("each slot is optional: choosing only the creature leaves the permanent slot empty") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        val bear = driver.putCardInGraveyard(me, "Grizzly Bears")

        val spell = driver.putCardInHand(me, "Badlands Revival")
        driver.giveMana(me, Color.BLACK, 1)
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 3)
        // Only the creature slot is filled; the permanent slot is left empty (up to one).
        driver.castSpellWithTargets(
            me,
            spell,
            listOf(ChosenTarget.Card(bear, me, Zone.GRAVEYARD)),
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD)).contains(bear) shouldBe true
        driver.state.getZone(ZoneKey(me, Zone.GRAVEYARD)).contains(bear) shouldBe false
    }
})
