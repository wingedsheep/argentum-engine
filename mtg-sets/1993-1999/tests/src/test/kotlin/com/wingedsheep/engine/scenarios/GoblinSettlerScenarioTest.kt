package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

class GoblinSettlerScenarioTest : ScenarioTestBase() {
    init {
        test("entering destroys the chosen opposing land and does not target creatures") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Goblin Settler")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(2, "Forest")
                .withCardOnBattlefield(2, "Island")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val forest = game.findPermanent("Forest")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "Goblin Settler").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision() as ChooseTargetsDecision
            decision.playerId shouldBe game.player1Id
            decision.legalTargets[0]!! shouldContain forest
            decision.legalTargets[0]!! shouldNotContain bears
            game.selectTargets(listOf(forest)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(2, "Forest") shouldBe true
            game.isOnBattlefield("Island") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Goblin Settler") shouldBe true
        }

        test("must destroy its controller's land when no opponent controls a land") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Goblin Settler")
                .withLandsOnBattlefield(1, "Mountain", 4)
                .withCardOnBattlefield(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val forest = game.findPermanent("Forest")!!
            game.castSpell(1, "Goblin Settler").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(forest)).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Forest") shouldBe true
            game.isOnBattlefield("Goblin Settler") shouldBe true
        }
    }
}
