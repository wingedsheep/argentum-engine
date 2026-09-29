package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class RoyalTrooperScenarioTest : ScenarioTestBase() {
    init {
        test("blocking pumps the trooper before damage and wears off next turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Royal Trooper")
                .withActivePlayer(1)
                .build()
            val trooper = game.findPermanent("Royal Trooper")!!

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Royal Trooper" to listOf("Hill Giant"))).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(trooper) shouldBe 4
            game.state.projectedState.getToughness(trooper) shouldBe 4
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.isInGraveyard(1, "Hill Giant") shouldBe true
            game.isOnBattlefield("Royal Trooper") shouldBe true
            game.getLifeTotal(2) shouldBe 20

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.projectedState.getPower(trooper) shouldBe 2
            game.state.projectedState.getToughness(trooper) shouldBe 2
        }

        test("being blocked does not pump an attacking trooper") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Royal Trooper")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Royal Trooper" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Royal Trooper"))).error shouldBe null

            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.isInGraveyard(1, "Royal Trooper") shouldBe true
            game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
