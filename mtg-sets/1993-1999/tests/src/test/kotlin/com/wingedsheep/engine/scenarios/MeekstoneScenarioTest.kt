package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Meekstone — "Creatures with power 3 or greater don't untap during their controllers' untap steps."
 *
 * A zero [com.wingedsheep.sdk.scripting.UntapLimitPerStep] cap: matching creatures stay tapped with
 * no keep-tapped prompt, and the power check reads the final projected state (layer 7 included).
 */
class MeekstoneScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.isTapped(id: EntityId) = state.getEntity(id)?.has<TappedComponent>() == true

    /** From our first main phase, pass through the opponent's turn to our next upkeep. */
    fun GameTestDriver.advanceToOurNextUpkeep(me: EntityId) {
        passPriorityUntil(Step.UPKEEP)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        passPriorityUntil(Step.UPKEEP)
        activePlayer shouldBe me
    }

    test("power 3 or greater stays tapped, smaller creatures untap, and nobody is prompted") {
        val d = driver()
        val me = d.activePlayer!!
        val opponent = d.getOpponent(me)

        // The opponent's Meekstone restricts our untap step too.
        d.putPermanentOnBattlefield(opponent, "Meekstone")
        val giant = d.putPermanentOnBattlefield(me, "Hill Giant")
        val bears = d.putPermanentOnBattlefield(me, "Grizzly Bears")
        d.tapPermanent(giant)
        d.tapPermanent(bears)

        d.advanceToOurNextUpkeep(me)
        d.pendingDecision shouldBe null
        d.isTapped(giant) shouldBe true
        d.isTapped(bears) shouldBe false
    }

    test("a layer-7 pump to power 3 counts at the untap step") {
        val d = driver()
        val me = d.activePlayer!!

        d.putPermanentOnBattlefield(me, "Meekstone")
        d.putPermanentOnBattlefield(me, "Glorious Anthem")
        val bears = d.putPermanentOnBattlefield(me, "Grizzly Bears")
        d.tapPermanent(bears)

        d.advanceToOurNextUpkeep(me)
        d.isTapped(bears) shouldBe true
    }
})
