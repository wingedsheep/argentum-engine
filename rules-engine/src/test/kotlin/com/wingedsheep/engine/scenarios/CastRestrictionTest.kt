package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.YouWereAttackedThisStep
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for cast restriction conditions like "Cast only during declare attackers step
 * and only if you've been attacked this step."
 */
class CastRestrictionTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 20,
                "Grizzly Bears" to 20
            ),
            skipMulligans = true
        )
        return driver
    }

    test("YouWereAttackedThisStep returns false when no attackers declared") {
        val driver = createDriver()
        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        // Put a creature on the battlefield for player 1 so declare attackers step isn't skipped
        val attackerId = driver.putCreatureOnBattlefield(player1, "Grizzly Bears")
        driver.removeSummoningSickness(attackerId)

        // Advance to declare attackers step
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Declare no attackers (empty map)
        driver.declareAttackers(player1, emptyMap())

        // Create context for player2 (the defending player)
        val context = EffectContext(
            sourceId = null,
            controllerId = player2,
            opponentId = player1,
            targets = emptyList(),
            xValue = 0
        )

        val evaluator = ConditionEvaluator()
        val result = evaluator.evaluate(driver.state, YouWereAttackedThisStep, context)

        result shouldBe false
    }

    test("YouWereAttackedThisStep returns true when player is being attacked") {
        val driver = createDriver()
        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        // Put a creature on the battlefield for player 1 (without summoning sickness)
        val attackerId = driver.putCreatureOnBattlefield(player1, "Grizzly Bears")
        driver.removeSummoningSickness(attackerId)

        // Advance to declare attackers step
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Declare the creature as attacking player 2
        driver.declareAttackers(player1, listOf(attackerId), player2)

        // Create context for player2 (the defending player)
        val context = EffectContext(
            sourceId = null,
            controllerId = player2,
            opponentId = player1,
            targets = emptyList(),
            xValue = 0
        )

        val evaluator = ConditionEvaluator()
        val result = evaluator.evaluate(driver.state, YouWereAttackedThisStep, context)

        result shouldBe true
    }

    test("YouWereAttackedThisStep returns false for attacking player") {
        val driver = createDriver()
        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        // Put a creature on the battlefield for player 1 (without summoning sickness)
        val attackerId = driver.putCreatureOnBattlefield(player1, "Grizzly Bears")
        driver.removeSummoningSickness(attackerId)

        // Advance to declare attackers step
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Declare the creature as attacking player 2
        driver.declareAttackers(player1, listOf(attackerId), player2)

        // Create context for player1 (the attacking player - they were NOT attacked)
        val context = EffectContext(
            sourceId = null,
            controllerId = player1,
            opponentId = player2,
            targets = emptyList(),
            xValue = 0
        )

        val evaluator = ConditionEvaluator()
        val result = evaluator.evaluate(driver.state, YouWereAttackedThisStep, context)

        // Player 1 is the attacker, not the defender, so this should be false
        result shouldBe false
    }

    test("AttackingComponent correctly tracks defender") {
        val driver = createDriver()
        val player1 = driver.activePlayer!!
        val player2 = driver.getOpponent(player1)

        // Put a creature on the battlefield for player 1 (without summoning sickness)
        val attackerId = driver.putCreatureOnBattlefield(player1, "Grizzly Bears")
        driver.removeSummoningSickness(attackerId)

        // Advance to declare attackers step
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Declare the creature as attacking player 2
        driver.declareAttackers(player1, listOf(attackerId), player2)

        // Verify the attacker has the correct defender
        val attackingComponent = driver.state.getEntity(attackerId)?.get<AttackingComponent>()
        attackingComponent shouldNotBe null
        attackingComponent!!.defenderId shouldBe player2
    }
})
