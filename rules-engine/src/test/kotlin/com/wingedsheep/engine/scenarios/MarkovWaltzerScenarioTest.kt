package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Markov Waltzer (VOW #242) — {2}{R}{W} Creature — Vampire, 1/3.
 *
 *   Flying, haste
 *   At the beginning of combat on your turn, up to two target creatures you control each get
 *   +1/+0 until end of turn.
 *
 * Exercises the begin-combat "up to two target creatures you control" trigger: choosing two
 * targets pumps both, choosing only one pumps only that one, and Markov Waltzer itself (a
 * creature you control) is a legal target.
 */
class MarkovWaltzerScenarioTest : ScenarioTestBase() {

    init {
        context("Markov Waltzer — begin combat, up to two target creatures you control get +1/+0") {

            test("choosing two targets pumps both by +1/+0") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Markov Waltzer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val waltzer = game.findPermanent("Markov Waltzer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                withClue("the begin-combat trigger asks for its targets") {
                    game.hasPendingDecision() shouldBe true
                }
                game.selectTargets(listOf(waltzer, bears))
                game.resolveStack()

                withClue("Markov Waltzer gets +1/+0 (1 -> 2 power)") {
                    game.state.projectedState.getPower(waltzer) shouldBe 2
                }
                withClue("Grizzly Bears gets +1/+0 (2 -> 3 power)") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                }
            }

            test("choosing only one target pumps only that creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Markov Waltzer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val waltzer = game.findPermanent("Markov Waltzer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.selectTargets(listOf(bears))
                game.resolveStack()

                withClue("Grizzly Bears (the only chosen target) gets +1/+0") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                }
                withClue("Markov Waltzer (not chosen) stays at its base power") {
                    game.state.projectedState.getPower(waltzer) shouldBe 1
                }
            }

            test("choosing zero targets ('up to two') leaves everyone at base power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Markov Waltzer", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val waltzer = game.findPermanent("Markov Waltzer")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                game.skipTargets()
                game.resolveStack()

                withClue("declining all targets leaves Markov Waltzer at base power") {
                    game.state.projectedState.getPower(waltzer) shouldBe 1
                }
                withClue("declining all targets leaves Grizzly Bears at base power") {
                    game.state.projectedState.getPower(bears) shouldBe 2
                }
            }
        }
    }
}
