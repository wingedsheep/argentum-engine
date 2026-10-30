package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Daysquad Marshal (IKO #8) — {3}{W} 3/3 Human Soldier.
 *
 *   When this creature enters, create a 1/1 white Human Soldier creature token.
 */
class DaysquadMarshalScenarioTest : ScenarioTestBase() {

    init {
        test("entering creates a 1/1 Human Soldier token") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Daysquad Marshal")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Daysquad Marshal").error shouldBe null
            game.resolveStack()

            withClue("the Marshal is on the battlefield") {
                game.isOnBattlefield("Daysquad Marshal") shouldBe true
            }
            val tokens = game.findPermanents("Human Soldier Token")
            withClue("its enters trigger created exactly one Human Soldier token") { tokens.size shouldBe 1 }
            val projected = game.state.projectedState
            projected.getPower(tokens.single()) shouldBe 1
            projected.getToughness(tokens.single()) shouldBe 1
        }
    }
}
