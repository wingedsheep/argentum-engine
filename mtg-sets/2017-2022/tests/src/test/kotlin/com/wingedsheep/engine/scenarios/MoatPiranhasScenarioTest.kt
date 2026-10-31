package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Moat Piranhas (M20 #67) — {1}{U} Creature — Fish, 3/3. Defender.
 */
class MoatPiranhasScenarioTest : ScenarioTestBase() {

    init {
        context("Moat Piranhas") {

            test("defender keeps it from attacking, but it still blocks") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Moat Piranhas")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Moat Piranhas" to 2)).error.shouldNotBeNull()
                game.getLifeTotal(2) shouldBe 20
            }

            test("it blocks and kills an attacking 2/2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Moat Piranhas")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Moat Piranhas" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isOnBattlefield("Moat Piranhas") shouldBe true
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
