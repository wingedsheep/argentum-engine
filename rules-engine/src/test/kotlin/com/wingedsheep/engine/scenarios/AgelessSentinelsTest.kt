package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for Ageless Sentinels.
 *
 * Ageless Sentinels: {3}{W}
 * Creature — Wall
 * 4/4
 * Defender, Flying
 * When this creature blocks, it becomes a Bird Giant, and it loses defender.
 * (This effect lasts indefinitely.)
 */
class AgelessSentinelsTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("Ageless Sentinels starts with Defender and Flying") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val player = driver.activePlayer!!
        val sentinels = driver.putCreatureOnBattlefield(player, "Ageless Sentinels")

        val projected = StateProjector().project(driver.state)
        projected.hasKeyword(sentinels, Keyword.DEFENDER) shouldBe true
        projected.hasKeyword(sentinels, Keyword.FLYING) shouldBe true
        projected.hasSubtype(sentinels, "Wall") shouldBe true
    }

    test("When Ageless Sentinels blocks, it becomes a Bird Giant and loses Defender") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val defender = driver.activePlayer!!
        val attacker = driver.getOpponent(defender)

        val sentinels = driver.putCreatureOnBattlefield(defender, "Ageless Sentinels")
        driver.removeSummoningSickness(sentinels)

        val attackingCreature = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(attackingCreature)

        // Pass to opponent's turn
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        // If it's not the attacker's turn, pass another turn
        if (driver.activePlayer != attacker) {
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        }

        driver.declareAttackers(attacker, listOf(attackingCreature), defender)
        driver.bothPass()

        // Block with Ageless Sentinels
        driver.declareBlockers(defender, mapOf(sentinels to listOf(attackingCreature)))

        // Trigger fires — "When this creature blocks..."
        // Both pass to resolve the trigger
        driver.bothPass()

        // After trigger resolves, check the projected state
        val projected = StateProjector().project(driver.state)

        // Should be a Bird Giant now
        projected.hasSubtype(sentinels, "Bird") shouldBe true
        projected.hasSubtype(sentinels, "Giant") shouldBe true
        // Should no longer be a Wall
        projected.hasSubtype(sentinels, "Wall") shouldBe false

        // Should have lost Defender
        projected.hasKeyword(sentinels, Keyword.DEFENDER) shouldBe false
        // Should still have Flying
        projected.hasKeyword(sentinels, Keyword.FLYING) shouldBe true

        // Advance through combat
        driver.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Sentinels (4/4) should survive blocking a 2/2
        driver.findPermanent(defender, "Ageless Sentinels") shouldNotBe null
        // Grizzly Bears (2/2) should die to the 4/4
        driver.findPermanent(attacker, "Grizzly Bears") shouldBe null
    }

    test("Type change and Defender loss persist across turns") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            startingLife = 20
        )

        val defender = driver.activePlayer!!
        val attacker = driver.getOpponent(defender)

        val sentinels = driver.putCreatureOnBattlefield(defender, "Ageless Sentinels")
        driver.removeSummoningSickness(sentinels)

        val attackingCreature = driver.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        driver.removeSummoningSickness(attackingCreature)

        // Pass to attacker's turn
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        if (driver.activePlayer != attacker) {
            driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        }

        driver.declareAttackers(attacker, listOf(attackingCreature), defender)
        driver.bothPass()

        // Block to trigger the ability
        driver.declareBlockers(defender, mapOf(sentinels to listOf(attackingCreature)))
        driver.bothPass() // resolve trigger

        // Advance to next turn (defender's turn)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        if (driver.activePlayer != defender) {
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        }

        // The effects should still be active (permanent duration)
        val projected = StateProjector().project(driver.state)
        projected.hasSubtype(sentinels, "Bird") shouldBe true
        projected.hasSubtype(sentinels, "Giant") shouldBe true
        projected.hasSubtype(sentinels, "Wall") shouldBe false
        projected.hasKeyword(sentinels, Keyword.DEFENDER) shouldBe false
        projected.hasKeyword(sentinels, Keyword.FLYING) shouldBe true
    }
})
