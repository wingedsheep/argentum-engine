package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class BitterbowSharpshootersScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.initMirrorMatch(deck = Deck.of("Island" to 30), startingLife = 20, skipMulligans = true)
        return d
    }

    test("vigilance leaves it untapped after attacking and reach lets it block a flyer") {
        val d = driver()
        val archer = d.putCreatureOnBattlefield(d.player1, "Bitterbow Sharpshooters")
        val flyer = d.putCreatureOnBattlefield(d.player2, "Wind Drake")
        d.removeSummoningSickness(archer)
        d.removeSummoningSickness(flyer)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(d.player1, listOf(archer), d.player2).error shouldBe null
        d.isTapped(archer) shouldBe false
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player2, emptyMap()).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_ATTACKERS, d.player2)
        d.declareAttackers(d.player2, listOf(flyer), d.player1).error shouldBe null
        d.passPriorityUntil(Step.DECLARE_BLOCKERS)
        d.declareBlockers(d.player1, mapOf(archer to listOf(flyer))).error shouldBe null
    }
})
