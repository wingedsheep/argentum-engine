package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tmt.cards.RaphaelTheNightwatcher
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.core.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Raphael, the Nightwatcher (TMT #103) — "Attacking creatures you control have double strike."
 */
class RaphaelTheNightwatcherTest : FunSpec({
    test("grants double strike only to attacking creatures you control") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RaphaelTheNightwatcher))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 20), startingLife = 20)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val raph = driver.putCreatureOnBattlefield(player, "Raphael, the Nightwatcher")
        val bear = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.removeSummoningSickness(raph)
        driver.removeSummoningSickness(bear)

        // Not attacking yet: no double strike.
        driver.state.projectedState.hasKeyword(bear, Keyword.DOUBLE_STRIKE) shouldBe false

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(player, listOf(bear), opponent)
        driver.state.projectedState.hasKeyword(bear, Keyword.DOUBLE_STRIKE) shouldBe true
    }
})
