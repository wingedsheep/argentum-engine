package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.arb.cards.EnlistedWurm
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Enlisted Wurm (Alara Reborn #68) — {4}{G}{W} Creature — Wurm 5/5, Cascade.
 *
 * The CASCADE keyword alone is display-only; this pins that the cast trigger is wired, so casting
 * the Wurm cascades into a cheaper creature that is cast for free.
 */
class EnlistedWurmScenarioTest : FunSpec({

    test("casting it cascades into a cheaper creature, cast without paying its mana cost") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + EnlistedWurm)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        // Cascade walks from the top: a land, then Grizzly Bears (MV 2 < 6) as the hit.
        driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(me, "Forest")

        val wurm = driver.putCardInHand(me, "Enlisted Wurm")
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveMana(me, Color.WHITE, 1)
        driver.giveColorlessMana(me, 4)
        driver.castSpell(me, wurm).outcome shouldBe Outcome.Done
        driver.bothPass() // cascade trigger resolves -> finds Grizzly Bears -> may-cast pause

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.findPermanent(me, "Grizzly Bears").shouldNotBeNull()
        driver.findPermanent(me, "Enlisted Wurm").shouldNotBeNull()
    }
})
