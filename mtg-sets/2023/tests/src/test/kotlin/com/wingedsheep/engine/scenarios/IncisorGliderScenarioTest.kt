package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Incisor Glider (ONE #15) — {1}{W} 1/3 Artifact Creature — Phyrexian Construct.
 *
 * "Flying
 *  Corrupted — Whenever this creature attacks, if an opponent has three or more poison counters,
 *  creatures you control get +1/+1 until end of turn."
 */
class IncisorGliderScenarioTest : ScenarioTestBase() {

    private fun TestGame.setPoison(playerId: EntityId, count: Int) {
        state = state.updateEntity(playerId) { it.with(CountersComponent(mapOf(CounterType.POISON to count))) }
    }

    private fun attackWithGlider(opponentPoison: Int): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Incisor Glider")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.setPoison(game.player2Id, opponentPoison)
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Incisor Glider" to 2)).error shouldBe null
        game.resolveStack()
        return game
    }

    private fun TestGame.bearsOf(player: EntityId): EntityId =
        findPermanents("Grizzly Bears").single {
            state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()?.playerId == player
        }

    init {
        test("with an opponent at three poison, creatures you control get +1/+1 until end of turn") {
            val game = attackWithGlider(opponentPoison = 3)
            val glider = game.findPermanent("Incisor Glider")!!
            val myBears = game.bearsOf(game.player1Id)
            val theirBears = game.bearsOf(game.player2Id)

            withClue("the attacking Glider and the non-attacking Bears both get +1/+1") {
                game.state.projectedState.getPower(glider) shouldBe 2
                game.state.projectedState.getToughness(glider) shouldBe 4
                game.state.projectedState.getPower(myBears) shouldBe 3
                game.state.projectedState.getToughness(myBears) shouldBe 3
            }
            withClue("the opponent's creature is untouched") {
                game.state.projectedState.getPower(theirBears) shouldBe 2
            }

            game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            withClue("the pump ends at end of turn") {
                game.state.projectedState.getPower(myBears) shouldBe 2
            }
        }

        test("with the opponent at two poison, the trigger does nothing") {
            val game = attackWithGlider(opponentPoison = 2)
            val glider = game.findPermanent("Incisor Glider")!!
            val myBears = game.bearsOf(game.player1Id)

            game.state.projectedState.getPower(glider) shouldBe 1
            game.state.projectedState.getToughness(glider) shouldBe 3
            game.state.projectedState.getPower(myBears) shouldBe 2
        }
    }
}
