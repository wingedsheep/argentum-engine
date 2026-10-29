package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Persistent Constrictor (DSC 22).
 *
 * "At the beginning of each opponent's upkeep, they lose 1 life and you put a -1/-1 counter on up
 *  to one target creature they control."
 */
class PersistentConstrictorScenarioTest : ScenarioTestBase() {
    init {
        test("opponent's upkeep: they lose 1 life and their targeted creature gets a -1/-1 counter") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Persistent Constrictor")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bears = game.findPermanent("Grizzly Bears")!!
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id

            if (game.state.pendingDecision is ChooseTargetsDecision) {
                game.selectTargets(listOf(bears))
            }
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            withClue("the Bears carry a -1/-1 counter") {
                game.state.projectedState.getPower(bears) shouldBe 1
                game.state.projectedState.getToughness(bears) shouldBe 1
            }
        }

        test("with no creature to target the opponent still loses 1 life") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Persistent Constrictor")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player2Id
            if (game.state.pendingDecision is ChooseTargetsDecision) game.skipTargets()
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 20
        }

        test("on your own upkeep it doesn't trigger") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Persistent Constrictor")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id
            if (game.state.stack.isNotEmpty()) game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
