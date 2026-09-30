package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class WildGriffinScenarioTest : ScenarioTestBase() {
    init {
        test("flying prevents ground blockers but permits flying blockers") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Wild Griffin")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardOnBattlefield(2, "Wind Drake")
                .withActivePlayer(1)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Wild Griffin" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Wild Griffin"))).error shouldNotBe null
            game.declareBlockers(mapOf("Wind Drake" to listOf("Wild Griffin"))).error shouldBe null

            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(2) shouldBe 20
            game.isInGraveyard(1, "Wild Griffin") shouldBe true
            game.isInGraveyard(2, "Wind Drake") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("unblocked Wild Griffin deals two combat damage") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Wild Griffin")
                .withActivePlayer(1)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Wild Griffin" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

            game.getLifeTotal(2) shouldBe 18
            game.isOnBattlefield("Wild Griffin") shouldBe true
        }
    }
}
