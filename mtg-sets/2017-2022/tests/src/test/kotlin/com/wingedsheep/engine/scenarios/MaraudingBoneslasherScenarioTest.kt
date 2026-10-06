package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Marauding Boneslasher (HOU #70) — {2}{B} 3/3 Creature — Zombie Minotaur.
 *
 * "This creature can't block unless you control another Zombie."
 *
 * The Boneslasher is itself a Zombie, so the restriction only works if the block check
 * excludes the blocker from the "another Zombie" search; an opponent's Zombie doesn't count.
 */
class MaraudingBoneslasherScenarioTest : ScenarioTestBase() {

    private fun blockAttempt(setup: (ScenarioBuilder) -> Unit): String? {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(2, "Marauding Boneslasher", summoningSickness = false)
            .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
            .withActivePlayer(1)
        setup(builder)
        val game = builder.build()

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        return game.declareBlockers(mapOf("Marauding Boneslasher" to listOf("Grizzly Bears"))).error
    }

    init {
        context("Marauding Boneslasher — block restriction") {

            test("cannot block when it is the only Zombie you control") {
                withClue("the Boneslasher itself must not count as 'another Zombie'") {
                    blockAttempt { } shouldNotBe null
                }
            }

            test("an opponent's Zombie does not enable blocking") {
                withClue("only Zombies you control count") {
                    blockAttempt { it.withCardOnBattlefield(1, "Wayward Servant") } shouldNotBe null
                }
            }

            test("can block when you control another Zombie") {
                withClue("another Zombie you control lifts the restriction") {
                    blockAttempt { it.withCardOnBattlefield(2, "Wayward Servant") } shouldBe null
                }
            }
        }
    }
}
