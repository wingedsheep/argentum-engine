package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.arb.cards.ViolentOutburst
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Violent Outburst (Alara Reborn #63) — {1}{R}{G} Instant, Cascade.
 * "Creatures you control get +1/+0 until end of turn."
 *
 * The CASCADE keyword alone is display-only; this pins that the cast trigger is wired and that the
 * cascaded creature, resolving first, is pumped by the Outburst too.
 */
class ViolentOutburstScenarioTest : FunSpec({

    test("cascades into a cheaper creature, then pumps every creature you control") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ViolentOutburst)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val mine = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirs = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Cascade walks from the top: a land, then Grizzly Bears (MV 2 < 3) as the hit.
        driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        driver.putCardOnTopOfLibrary(me, "Forest")

        val outburst = driver.putCardInHand(me, "Violent Outburst")
        driver.giveMana(me, Color.RED, 1)
        driver.giveMana(me, Color.GREEN, 1)
        driver.giveColorlessMana(me, 1)
        driver.castSpell(me, outburst).outcome shouldBe Outcome.Done
        driver.bothPass() // cascade trigger resolves -> finds Grizzly Bears -> may-cast pause

        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        val cascaded = driver.state.getBattlefield(me)
            .filter { driver.getCardName(it) == "Grizzly Bears" && it != mine }
            .single()
        driver.state.projectedState.getPower(mine) shouldBe 3
        driver.state.projectedState.getPower(cascaded) shouldBe 3
        driver.state.projectedState.getPower(theirs) shouldBe 2
    }
})
