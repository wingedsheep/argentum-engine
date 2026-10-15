package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Topiary Stomper (SNC #160) — {1}{G}{G} 4/4 Creature — Plant Dinosaur, Vigilance.
 *
 * "This creature can't attack or block unless you control seven or more lands."
 */
class TopiaryStomperScenarioTest : ScenarioTestBase() {

    private fun attackAttempt(lands: Int): String? {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Topiary Stomper")
            .withLandsOnBattlefield(1, "Forest", lands)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        return game.declareAttackers(mapOf("Topiary Stomper" to 2)).error
    }

    private fun blockAttempt(lands: Int): String? {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(2, "Topiary Stomper")
            .withLandsOnBattlefield(2, "Forest", lands)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game.declareBlockers(mapOf("Topiary Stomper" to listOf("Grizzly Bears"))).error
    }

    init {
        context("Topiary Stomper — seven-land restriction") {
            test("can't attack or block with six lands") {
                withClue("six lands is not enough to attack") { attackAttempt(6) shouldNotBe null }
                withClue("six lands is not enough to block") { blockAttempt(6) shouldNotBe null }
            }

            test("can attack and block with seven lands") {
                withClue("seven lands lifts the attack restriction") { attackAttempt(7) shouldBe null }
                withClue("seven lands lifts the block restriction") { blockAttempt(7) shouldBe null }
            }
        }
    }
}
