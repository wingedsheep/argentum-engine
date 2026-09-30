package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class HollowDogsScenarioTest : ScenarioTestBase() {
    init {
        test("attacking pumps only Hollow Dogs and the bonus expires at cleanup") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hollow Dogs", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val dogs = game.findPermanent("Hollow Dogs")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hollow Dogs" to 2)).error shouldBe null
            game.state.projectedState.getPower(dogs) shouldBe 3
            game.resolveStack()
            game.state.projectedState.getPower(dogs) shouldBe 5
            game.state.projectedState.getToughness(dogs) shouldBe 3
            game.state.projectedState.getPower(bears) shouldBe 2

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(2) shouldBe 15
            game.state.projectedState.getPower(dogs) shouldBe 5
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.state.projectedState.getPower(dogs) shouldBe 3
            game.state.projectedState.getToughness(dogs) shouldBe 3
        }

        test("another creature attacking does not trigger Hollow Dogs") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hollow Dogs", summoningSickness = false)
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val dogs = game.findPermanent("Hollow Dogs")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()
            game.state.projectedState.getPower(dogs) shouldBe 3
            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            game.getLifeTotal(2) shouldBe 18
        }
    }
}
