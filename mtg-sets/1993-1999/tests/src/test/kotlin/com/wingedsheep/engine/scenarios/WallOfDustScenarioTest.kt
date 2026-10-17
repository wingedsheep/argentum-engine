package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Wall of Dust (LEG #168) — "Defender. Whenever this creature blocks a creature, that creature can't
 * attack during its controller's next turn."
 *
 * The block happens on the attacker's controller's own turn, which never counts: the lock covers
 * that player's *following* turn and is gone the turn after.
 */
class WallOfDustScenarioTest : ScenarioTestBase() {

    init {
        context("Wall of Dust") {

            test("the blocked creature can't attack during its controller's next turn, then can again") {
                val builder = scenario()
                    .withPlayers("Wall Player", "Attacker")
                    .withCardOnBattlefield(1, "Wall of Dust")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(10) {
                    builder.withCardInLibrary(1, "Island")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder.build()

                val bears = game.findPermanent("Grizzly Bears")!!
                fun locked() = game.state.projectedState.cantAttack(bears)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wall of Dust" to listOf("Grizzly Bears"))).error shouldBe null
                game.resolveStack()
                withClue("the block locks the attacker") { locked() shouldBe true }

                // Wall player's turn.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                withClue("the turn of the block didn't count, even though it was the controller's") {
                    locked() shouldBe true
                }

                // Attacker's next turn: Grizzly Bears can't attack.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                withClue("the blocked creature can't attack during its controller's next turn") {
                    locked() shouldBe true
                }
                game.passUntilPhase(Phase.ENDING, Step.END)
                withClue("…for the whole of that turn") { locked() shouldBe true }

                // The turn after that, it can attack again.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id
                withClue("the restriction ended with that turn") { locked() shouldBe false }
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.state.activePlayerId shouldBe game.player2Id
                withClue("Grizzly Bears attacks again") {
                    game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                }
            }
        }
    }
}
