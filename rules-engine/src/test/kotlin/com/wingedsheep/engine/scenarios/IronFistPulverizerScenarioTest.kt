package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.IronFistPulverizer
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Iron-Fist Pulverizer (OTJ #131) — {4}{R} Creature — Giant Warrior 4/5.
 *
 * "Reach
 *  Whenever you cast your second spell each turn, this creature deals 2 damage to target opponent.
 *  Scry 1."
 *
 * Verifies the second-spell trigger deals 2 to the opponent and then scries; and that it does not
 * fire on the controller's first spell.
 */
class IronFistPulverizerScenarioTest : FunSpec({

    // Drain scry's two prompts (bottom-pick, top-reorder) without reordering.
    fun GameTestDriver.drainScry(player: EntityId) {
        repeat(2) {
            when (val decision = pendingDecision) {
                is SelectCardsDecision ->
                    submitDecision(player, CardsSelectedResponse(decision.id, emptyList()))
                is ReorderLibraryDecision ->
                    submitDecision(player, OrderedResponse(decision.id, decision.cards))
                else -> {}
            }
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(IronFistPulverizer))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 30, "Lightning Bolt" to 30), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("second spell deals 2 to target opponent and scries; first spell does not trigger") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        driver.putCreatureOnBattlefield(me, "Iron-Fist Pulverizer")

        // First spell: no trigger.
        val bolt1 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt1, targets = listOf(opp))
        driver.passPriority(me)
        driver.passPriority(opp)
        driver.getLifeTotal(opp) shouldBe 17 // only the Bolt

        // Second spell: trigger fires (2 damage to opponent, then scry 1).
        val bolt2 = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt2, targets = listOf(opp))
        // Stack: trigger (top), Bolt (bottom). Resolve trigger first.
        driver.passPriority(me)
        driver.passPriority(opp)
        driver.drainScry(me) // resolve the scry prompts
        // Resolve the Bolt.
        driver.passPriority(me)
        driver.passPriority(opp)

        // 17 - 2 (trigger) - 3 (bolt) = 12
        driver.getLifeTotal(opp) shouldBe 12
    }
})
