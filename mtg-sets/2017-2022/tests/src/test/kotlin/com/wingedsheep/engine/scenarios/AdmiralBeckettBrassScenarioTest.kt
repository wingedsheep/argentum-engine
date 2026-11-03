package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Admiral Beckett Brass (XLN #217) — {1}{U}{B}{R} Legendary Creature — Human Pirate 3/3.
 *
 * "Other Pirates you control get +1/+1.
 *  At the beginning of your end step, gain control of target nonland permanent controlled by a
 *  player who was dealt combat damage by three or more Pirates this turn."
 */
class AdmiralBeckettBrassScenarioTest : ScenarioTestBase() {

    private fun ScenarioTestBase.TestGame.controllerOf(name: String) =
        state.projectedState.getController(findPermanent(name)!!)

    init {
        test("other Pirates you control get +1/+1; Beckett and non-Pirates don't") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Admiral Beckett Brass")
                .withCardOnBattlefield(1, "Brazen Buccaneers")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val projected = game.state.projectedState
            projected.getPower(game.findPermanent("Brazen Buccaneers")!!) shouldBe 3
            projected.getPower(game.findPermanent("Admiral Beckett Brass")!!) shouldBe 3
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
        }

        test("after three Pirates connect, the end-step trigger steals a nonland permanent of that player") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Admiral Beckett Brass", summoningSickness = false)
                .withCardOnBattlefield(1, "Brazen Buccaneers", summoningSickness = false)
                .withCardOnBattlefield(1, "Prosperous Pirates", summoningSickness = false)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(2, "Forest", 1)
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(
                mapOf("Admiral Beckett Brass" to 2, "Brazen Buccaneers" to 2, "Prosperous Pirates" to 2)
            ).error shouldBe null
            game.declareNoBlockers()
            game.passUntilPhase(Phase.ENDING, Step.END)

            withClue("the trigger asks for its target") { game.hasPendingDecision() shouldBe true }
            game.selectTargets(listOf(game.findPermanent("Grizzly Bears")!!)).error shouldBe null
            game.resolveStack()

            game.controllerOf("Grizzly Bears") shouldBe game.player1Id
            withClue("lands can't be targeted") { game.controllerOf("Forest") shouldBe game.player2Id }
        }

        test("two Pirates and a non-Pirate aren't enough: the trigger has no legal target") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Admiral Beckett Brass", summoningSickness = false)
                .withCardOnBattlefield(1, "Brazen Buccaneers", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(
                mapOf("Admiral Beckett Brass" to 2, "Brazen Buccaneers" to 2, "Grizzly Bears" to 2)
            ).error shouldBe null
            game.declareNoBlockers()
            game.passUntilPhase(Phase.ENDING, Step.END)

            game.hasPendingDecision() shouldBe false
            game.resolveStack()
            game.controllerOf("Hill Giant") shouldBe game.player2Id
        }
    }
}
