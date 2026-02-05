package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for Gravedigger's ETB ability when the creature in graveyard
 * died through combat damage (not directly placed there).
 */
class GravediggerCombatDeathTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Gravedigger finds creature that died in combat") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of(
                "Swamp" to 20,
                "Forest" to 20
            ),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        // Advance to main phase
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put creatures on battlefield for BOTH players
        val attackerBears = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val blockerBears = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        // Remove summoning sickness so the creature can attack
        driver.removeSummoningSickness(attackerBears)

        // Verify both creatures are on battlefield
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldNotBe null
        driver.findPermanent(opponent, "Grizzly Bears") shouldNotBe null

        // Advance to declare attackers
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        // Active player attacks with Grizzly Bears
        val attackResult = driver.declareAttackers(activePlayer, listOf(attackerBears), opponent)
        attackResult.isSuccess shouldBe true

        // Advance to declare blockers
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)

        // Opponent blocks with their Grizzly Bears
        val blockResult = driver.declareBlockers(opponent, mapOf(blockerBears to listOf(attackerBears)))
        blockResult.isSuccess shouldBe true

        // Let combat damage happen - both creatures should die
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Check that both creatures are now in graveyards (died from combat damage)
        driver.getGraveyardCardNames(activePlayer) shouldContain "Grizzly Bears"
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null

        // Now test Gravedigger
        val gravedigger = driver.putCardInHand(activePlayer, "Gravedigger")
        driver.giveMana(activePlayer, Color.BLACK, 4)

        // Cast Gravedigger
        val castResult = driver.castSpell(activePlayer, gravedigger)
        castResult.isSuccess shouldBe true

        // Resolve the spell
        driver.bothPass()

        // Gravedigger should be on battlefield
        driver.findPermanent(activePlayer, "Gravedigger") shouldNotBe null

        // The ETB trigger should fire - should prompt for target selection
        driver.isPaused shouldBe true
        driver.pendingDecision.shouldNotBeNull()
        driver.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()

        val targetDecision = driver.pendingDecision as ChooseTargetsDecision
        targetDecision.playerId shouldBe activePlayer

        // Our Grizzly Bears that died in combat should be a legal target
        val legalTargets = targetDecision.legalTargets[0] ?: emptyList()
        legalTargets shouldContain attackerBears
    }
})
