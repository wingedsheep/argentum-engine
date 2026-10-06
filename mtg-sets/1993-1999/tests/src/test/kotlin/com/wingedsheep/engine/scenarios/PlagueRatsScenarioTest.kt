package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class PlagueRatsScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(Deck.of("Swamp" to 40))
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("alone it is a 1/1") {
        val d = driver(); val me = d.activePlayer!!
        val rats = d.putCreatureOnBattlefield(me, "Plague Rats")
        d.putCreatureOnBattlefield(me, "Grizzly Bears")
        d.state.projectedState.getPower(rats) shouldBe 1
        d.state.projectedState.getToughness(rats) shouldBe 1
    }

    test("counts Plague Rats controlled by every player") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        val mine = d.putCreatureOnBattlefield(me, "Plague Rats")
        d.putCreatureOnBattlefield(me, "Plague Rats")
        val theirs = d.putCreatureOnBattlefield(other, "Plague Rats")
        d.state.projectedState.getPower(mine) shouldBe 3
        d.state.projectedState.getToughness(mine) shouldBe 3
        d.state.projectedState.getPower(theirs) shouldBe 3
    }

    test("shrinks when another Plague Rats dies") {
        val d = driver(); val me = d.activePlayer!!; val other = d.getOpponent(me)
        val mine = d.putCreatureOnBattlefield(me, "Plague Rats")
        val theirs = d.putCreatureOnBattlefield(other, "Plague Rats")
        d.state.projectedState.getToughness(theirs) shouldBe 2
        d.giveMana(me, Color.RED)
        d.castSpell(me, d.putCardInHand(me, "Lightning Bolt"), listOf(theirs)).error shouldBe null
        d.bothPass()
        d.state.projectedState.getPower(mine) shouldBe 1
        d.state.projectedState.getToughness(mine) shouldBe 1
    }
})
