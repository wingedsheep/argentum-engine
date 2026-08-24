package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Murk Dwellers.
 *
 * Oracle: "Whenever this creature attacks and isn't blocked, it gets +2/+0 until end of combat."
 *
 * The card composes two existing primitives, but their *combination* is what these cover: the
 * [com.wingedsheep.sdk.dsl.Triggers.AttacksAndIsntBlocked] trigger resolving a self-targeted pump
 * (rather than Merchant Ship's player-targeted life gain), and that pump expiring on
 * [com.wingedsheep.sdk.scripting.Duration.EndOfCombat] rather than at end of turn.
 */
class MurkDwellersScenarioTest : ScenarioTestBase() {

    init {
        context("Murk Dwellers — attacks and isn't blocked → +2/+0 until end of combat") {

            test("gets +2/+0 when it attacks unblocked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Murk Dwellers", summoningSickness = false)
                    .withActivePlayer(1)
                    .build()

                val dwellers = game.findPermanent("Murk Dwellers")!!
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Murk Dwellers" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                // Defender declines to block — Murk Dwellers is unblocked.
                game.declareBlockers(emptyMap()).error shouldBe null
                game.resolveStack()

                withClue("unblocked Murk Dwellers should be a 4/2 while combat lasts") {
                    game.state.projectedState.getPower(dwellers) shouldBe 4
                    game.state.projectedState.getToughness(dwellers) shouldBe 2
                }
            }

            test("the bonus is gone after combat ends") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Murk Dwellers", summoningSickness = false)
                    .withActivePlayer(1)
                    .build()

                val dwellers = game.findPermanent("Murk Dwellers")!!
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Murk Dwellers" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(emptyMap()).error shouldBe null
                game.resolveStack()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("+2/+0 lasts until end of combat, not until end of turn") {
                    game.state.projectedState.getPower(dwellers) shouldBe 2
                }
            }

            test("no bonus when it is blocked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Murk Dwellers", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .build()

                val dwellers = game.findPermanent("Murk Dwellers")!!
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Murk Dwellers" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Murk Dwellers"))).error shouldBe null
                game.resolveStack()

                withClue("a blocked Murk Dwellers stays a 2/2") {
                    game.state.projectedState.getPower(dwellers) shouldBe 2
                }
            }
        }
    }
}
