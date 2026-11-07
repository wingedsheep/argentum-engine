package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.m13.cards.Archaeomancer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Archaeomancer (Magic 2013 #41) — {2}{U}{U} Creature — Human Wizard 1/2.
 *
 * "When this creature enters, return target instant or sorcery card from your graveyard to your hand."
 */
class ArchaeomancerScenarioTest : FunSpec({

    test("entering returns an instant from your graveyard to your hand; creatures and the opponent's cards are not targets") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + Archaeomancer)
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val bolt = driver.putCardInGraveyard(me, "Lightning Bolt")
        val bears = driver.putCardInGraveyard(me, "Grizzly Bears")
        val theirBolt = driver.putCardInGraveyard(opponent, "Lightning Bolt")

        val mancer = driver.putCardInHand(me, "Archaeomancer")
        driver.giveMana(me, Color.BLUE, 4)
        driver.castSpell(me, mancer).outcome shouldBe Outcome.Done
        driver.bothPass() // creature resolves; enters trigger asks for a target

        val decision = driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets[0].orEmpty()
        legal shouldContain bolt
        legal shouldNotContain bears
        legal shouldNotContain theirBolt

        driver.submitTargetSelection(me, listOf(bolt))
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        driver.getHand(me) shouldContain bolt
        driver.getGraveyard(me) shouldNotContain bolt
    }
})
