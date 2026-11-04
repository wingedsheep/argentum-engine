package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class AerialGuideScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20, skipMulligans = true)
        return d
    }

    test("grants flying to another attacker, excludes itself and creatures not attacking, then expires") {
        val d = driver()
        val guide = d.putCreatureOnBattlefield(d.player1, "Aerial Guide")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val idle = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val blocker = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.removeSummoningSickness(guide)
        d.removeSummoningSickness(bears)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(guide, bears), d.player2)
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets.values.flatten()
        legal.contains(guide) shouldBe false
        legal.contains(idle) shouldBe false
        legal.contains(bears) shouldBe true
        d.submitTargetSelection(d.player1, listOf(bears)).error shouldBe null
        d.bothPass()
        d.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe true
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.submitExpectFailure(DeclareBlockers(d.player2, mapOf(blocker to listOf(bears))))
        d.declareBlockers(d.player2, emptyMap()).error shouldBe null
        d.passPriorityUntil(Step.UPKEEP, d.player2)
        d.state.projectedState.hasKeyword(bears, Keyword.FLYING) shouldBe false
    }
})
