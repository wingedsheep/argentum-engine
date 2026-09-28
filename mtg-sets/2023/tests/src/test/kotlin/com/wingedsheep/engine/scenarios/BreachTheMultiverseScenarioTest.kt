package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Breach the Multiverse (MOM #94) — {5}{B}{B} Sorcery.
 * Each player mills ten, you pick a creature/planeswalker card from each graveyard, they enter under
 * your control, then each creature you control becomes a Phyrexian.
 */
class BreachTheMultiverseScenarioTest : FunSpec({

    val projector = StateProjector()

    test("takes a creature from each graveyard under your control and makes your creatures Phyrexians") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val mine = driver.putCardInGraveyard(me, "Grizzly Bears")
        val theirs = driver.putCardInGraveyard(opp, "Grizzly Bears")
        val existing = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val spell = driver.putCardInHand(me, "Breach the Multiverse")
        driver.giveMana(me, Color.BLACK, 7)

        driver.castSpell(me, spell)
        driver.bothPass()
        while (driver.pendingDecision != null) {
            val d = driver.pendingDecision!!
            val opts = (d as com.wingedsheep.engine.core.SelectCardsDecision).options
            driver.submitCardSelection(me, listOf(opts.first()))
        }

        val battlefield = driver.state.getZone(ZoneKey(me, Zone.BATTLEFIELD))
        battlefield.contains(mine) shouldBe true
        battlefield.contains(theirs) shouldBe true
        driver.state.getZone(ZoneKey(opp, Zone.BATTLEFIELD)).contains(theirs) shouldBe false

        val projected = projector.project(driver.state)
        listOf(mine, theirs, existing).forEach { projected.hasSubtype(it, "Phyrexian") shouldBe true }
    }
})
