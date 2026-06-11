package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.LeonardoCuttingEdge
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Leonardo, Cutting Edge (TMT #15) — Lifelink + "Whenever you gain life, put a
 * +1/+1 counter on Leonardo."
 */
class LeonardoCuttingEdgeTest : FunSpec({
    test("lifelink life gain triggers a +1/+1 counter on Leonardo") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(LeonardoCuttingEdge))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val leo = driver.putCreatureOnBattlefield(player, "Leonardo, Cutting Edge")
        driver.removeSummoningSickness(leo)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(leo), opponent)
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Lifelink: dealt 1 → gained 1 (life 21). Trigger: +1/+1 counter → Leonardo is 2/2.
        driver.assertLifeTotal(player, 21)
        driver.state.projectedState.getPower(leo) shouldBe 2
        driver.state.projectedState.getToughness(leo) shouldBe 2
    }
})
