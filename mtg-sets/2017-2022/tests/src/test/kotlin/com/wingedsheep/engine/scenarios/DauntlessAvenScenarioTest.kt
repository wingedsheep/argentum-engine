package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dauntless Aven (HOU) — {2}{W} 2/1 flying.
 * "Whenever this creature attacks, untap target creature you control."
 */
class DauntlessAvenScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Plains" to 30), startingLife = 20, skipMulligans = true)
        return d
    }

    test("attacking untaps a tapped creature you control; opponents' creatures aren't legal targets") {
        val d = driver()
        val aven = d.putCreatureOnBattlefield(d.player1, "Dauntless Aven")
        val bears = d.putCreatureOnBattlefield(d.player1, "Grizzly Bears")
        val theirs = d.putCreatureOnBattlefield(d.player2, "Grizzly Bears")
        d.removeSummoningSickness(aven)
        d.tapPermanent(bears)
        d.state.projectedState.hasKeyword(aven, Keyword.FLYING) shouldBe true

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(aven), d.player2)
        val decision = d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets.values.flatten()
        legal.contains(bears) shouldBe true
        legal.contains(theirs) shouldBe false
        d.submitTargetSelection(d.player1, listOf(bears)).error shouldBe null
        d.bothPass()

        d.isTapped(bears) shouldBe false
    }
})
