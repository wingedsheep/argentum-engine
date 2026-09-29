package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class RoyalFalconScenarioTest : ScenarioTestBase() {
    init {
        test("flies over a ground blocker and deals one combat damage") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Royal Falcon")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Royal Falcon" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Royal Falcon"))).error shouldNotBe null

            game.declareBlockers(emptyMap()).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(2) shouldBe 19
        }

        test("can block a flyer and dies to its combat damage") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Wind Drake")
                .withCardOnBattlefield(2, "Royal Falcon")
                .withActivePlayer(1)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Wind Drake" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Royal Falcon" to listOf("Wind Drake"))).error shouldBe null

            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.getLifeTotal(2) shouldBe 20
            game.isInGraveyard(2, "Royal Falcon") shouldBe true
            game.isOnBattlefield("Wind Drake") shouldBe true
        }
    }
}
