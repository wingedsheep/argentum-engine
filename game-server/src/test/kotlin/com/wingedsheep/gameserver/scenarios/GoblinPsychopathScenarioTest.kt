package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.CoinFlipEvent
import com.wingedsheep.engine.core.DamageDealtEvent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Goblin Psychopath's coin flip combat damage redirection.
 *
 * Goblin Psychopath: {3}{R}
 * Creature — Goblin Mutant 5/5
 * Whenever Goblin Psychopath attacks or blocks, flip a coin. If you lose
 * the flip, the next time it would deal combat damage this turn, it deals
 * that damage to you instead.
 */
class GoblinPsychopathScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )
        return driver
    }

    test("attacking unblocked - losing flip redirects combat damage to controller") {
        var foundLostFlip = false
        repeat(50) {
            if (foundLostFlip) return@repeat

            val driver = createDriver()
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val attacker = driver.activePlayer!!
            val defender = driver.otherPlayer(attacker)

            val psychopath = driver.putCreatureOnBattlefield(attacker, "Goblin Psychopath")
            driver.removeSummoningSickness(psychopath)

            // Move to declare attackers
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(attacker, listOf(psychopath), defender)

            // Resolve the attack trigger (coin flip)
            val triggerResult = driver.bothPass()
            val coinEvent = triggerResult.events.filterIsInstance<CoinFlipEvent>().firstOrNull()
                ?: return@repeat

            if (!coinEvent.won) {
                // Lost the flip - pass to combat damage
                driver.declareNoBlockers(defender)

                // Pass through first strike damage step and regular combat damage
                driver.passPriorityUntil(Step.POSTCOMBAT_MAIN, maxPasses = 20)

                // Attacker should have taken 5 damage (redirected to self)
                driver.getLifeTotal(attacker) shouldBe 15
                // Defender should be unharmed
                driver.getLifeTotal(defender) shouldBe 20
                foundLostFlip = true
            }
        }
        foundLostFlip shouldBe true
    }

    test("attacking unblocked - winning flip deals normal combat damage") {
        var foundWonFlip = false
        repeat(50) {
            if (foundWonFlip) return@repeat

            val driver = createDriver()
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val attacker = driver.activePlayer!!
            val defender = driver.otherPlayer(attacker)

            val psychopath = driver.putCreatureOnBattlefield(attacker, "Goblin Psychopath")
            driver.removeSummoningSickness(psychopath)

            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(attacker, listOf(psychopath), defender)

            val triggerResult = driver.bothPass()
            val coinEvent = triggerResult.events.filterIsInstance<CoinFlipEvent>().firstOrNull()
                ?: return@repeat

            if (coinEvent.won) {
                driver.declareNoBlockers(defender)
                driver.passPriorityUntil(Step.POSTCOMBAT_MAIN, maxPasses = 20)

                // Defender took 5 combat damage
                driver.getLifeTotal(defender) shouldBe 15
                // Attacker is unharmed
                driver.getLifeTotal(attacker) shouldBe 20
                foundWonFlip = true
            }
        }
        foundWonFlip shouldBe true
    }

    test("blocking - losing flip redirects combat damage to controller") {
        var foundLostFlip = false
        repeat(50) {
            if (foundLostFlip) return@repeat

            val driver = createDriver()
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val activePlayer = driver.activePlayer!!
            val otherPlayer = driver.otherPlayer(activePlayer)

            // Put a 2/2 creature on the active player's side
            val smallCreature = driver.putCreatureOnBattlefield(activePlayer, "Goblin Brigand")
            driver.removeSummoningSickness(smallCreature)

            // Put Goblin Psychopath on the other player's side (it will block)
            val psychopath = driver.putCreatureOnBattlefield(otherPlayer, "Goblin Psychopath")
            driver.removeSummoningSickness(psychopath)

            // Declare attack with the small creature
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
            driver.declareAttackers(activePlayer, listOf(smallCreature), otherPlayer)

            // Both pass to move to declare blockers
            driver.bothPass()

            // Block with Goblin Psychopath
            driver.declareBlockers(otherPlayer, mapOf(psychopath to listOf(smallCreature)))

            // Resolve the block trigger (coin flip)
            val triggerResult = driver.bothPass()
            val coinEvent = triggerResult.events.filterIsInstance<CoinFlipEvent>().firstOrNull()
                ?: return@repeat

            if (!coinEvent.won) {
                // Lost the flip - pass through combat damage
                driver.passPriorityUntil(Step.POSTCOMBAT_MAIN, maxPasses = 20)

                // Goblin Psychopath's controller took 5 damage (redirected)
                driver.getLifeTotal(otherPlayer) shouldBe 15
                foundLostFlip = true
            }
        }
        foundLostFlip shouldBe true
    }
})
