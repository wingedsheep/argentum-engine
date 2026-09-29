package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.TargetsComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class DakmorGhoulScenarioTest : ScenarioTestBase() {
    init {
        test("entering targets only the opponent and drains two life when the trigger resolves") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Dakmor Ghoul")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Dakmor Ghoul").error shouldBe null
            // Resolve only the creature: the sole legal opponent is selected automatically.
            game.passPriority().error shouldBe null
            game.passPriority().error shouldBe null
            game.getPendingDecision() shouldBe null
            val trigger = game.state.stack.single()
            game.state.getEntity(trigger)!!.get<TargetsComponent>()!!.targets shouldBe
                listOf(ChosenTarget.Player(game.player2Id))
            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 22
            game.getLifeTotal(2) shouldBe 18
            game.isOnBattlefield("Dakmor Ghoul") shouldBe true
        }
    }
}
