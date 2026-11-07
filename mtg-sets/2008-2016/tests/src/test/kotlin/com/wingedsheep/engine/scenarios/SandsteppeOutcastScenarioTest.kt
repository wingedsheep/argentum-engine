package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.SandsteppeOutcast
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sandsteppe Outcast (Fate Reforged #25) — {2}{W} Creature — Human Warrior 2/1.
 * "When this creature enters, choose one — • Put a +1/+1 counter on this creature.
 *  • Create a 1/1 white Spirit creature token with flying."
 */
class SandsteppeOutcastScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + SandsteppeOutcast)
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.castAndChoose(me: EntityId, modePrefix: String): EntityId {
        val outcast = putCardInHand(me, "Sandsteppe Outcast")
        giveMana(me, Color.WHITE, 3)
        castSpell(me, outcast).outcome shouldBe Outcome.Done
        var guard = 0
        while (state.pendingDecision !is ChooseOptionDecision && guard++ < 10) bothPass()
        val decision = pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.startsWith(modePrefix) }
        check(index >= 0) { "$modePrefix not offered; options=${decision.options}" }
        submitDecision(me, OptionChosenResponse(decision.id, index))
        guard = 0
        while (state.stack.isNotEmpty() && state.pendingDecision == null && guard++ < 10) bothPass()
        return outcast
    }

    test("first mode makes it a 3/2") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val outcast = driver.castAndChoose(me, "Put a +1/+1 counter")

        driver.state.projectedState.getPower(outcast) shouldBe 3
        driver.state.projectedState.getToughness(outcast) shouldBe 2
        driver.findPermanent(me, "Spirit Token") shouldBe null
    }

    test("second mode creates a 1/1 white flying Spirit and leaves it a 2/1") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val outcast = driver.castAndChoose(me, "Create a 1/1")

        driver.state.projectedState.getPower(outcast) shouldBe 2
        val spirit = driver.findPermanent(me, "Spirit Token").shouldNotBeNull()
        driver.state.projectedState.getPower(spirit) shouldBe 1
        driver.state.projectedState.getToughness(spirit) shouldBe 1
        driver.state.projectedState.hasKeyword(spirit, Keyword.FLYING) shouldBe true
    }
})
