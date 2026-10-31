package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.iko.cards.Grimdancer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Grimdancer — "This creature enters with your choice of two different counters on it from among
 * menace, deathtouch, and lifelink."
 *
 * Pins the pair-mode choice: each keyword counter is gated on an `AnyCondition` over the two pairs
 * that contain it, so picking one pair must place exactly those two counters and not the third.
 */
class GrimdancerScenarioTest : FunSpec({

    test("choosing menace and lifelink puts exactly those two counters on it, not deathtouch") {
        val d = GameTestDriver().apply {
            registerCards(TestCards.all + listOf(Grimdancer))
            initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
            passPriorityUntil(Step.PRECOMBAT_MAIN)
        }
        val me = d.activePlayer!!
        val grimdancer = d.putCardInHand(me, "Grimdancer")
        d.giveMana(me, Color.BLACK, 3)

        d.castSpell(me, grimdancer).error shouldBe null
        d.bothPass()

        val pick = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        pick.playerId shouldBe me
        pick.options.size shouldBe 3
        val index = pick.options.indexOf("Menace and lifelink counters")
        (index >= 0) shouldBe true
        d.submitDecision(me, OptionChosenResponse(pick.id, index)).error shouldBe null

        (grimdancer in d.state.getBattlefield()) shouldBe true
        val counters = d.state.getEntity(grimdancer)!!.get<CountersComponent>()!!
        counters.getCount(CounterType.MENACE) shouldBe 1
        counters.getCount(CounterType.LIFELINK) shouldBe 1
        counters.getCount(CounterType.DEATHTOUCH) shouldBe 0

        val projected = d.state.projectedState
        projected.hasKeyword(grimdancer, Keyword.MENACE) shouldBe true
        projected.hasKeyword(grimdancer, Keyword.LIFELINK) shouldBe true
        projected.hasKeyword(grimdancer, Keyword.DEATHTOUCH) shouldBe false
    }
})
