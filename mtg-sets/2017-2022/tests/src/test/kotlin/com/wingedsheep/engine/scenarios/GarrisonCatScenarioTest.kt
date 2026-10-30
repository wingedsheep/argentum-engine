package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Garrison Cat (IKO #14) — {W} 1/1 Cat.
 *
 *   When this creature dies, create a 1/1 white Human Soldier creature token.
 */
class GarrisonCatScenarioTest : ScenarioTestBase() {

    init {
        test("dying creates a 1/1 Human Soldier token for its controller") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(2, "Garrison Cat")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cat = game.findPermanent("Garrison Cat")!!
            game.findPermanents("Human Soldier Token").size shouldBe 0
            game.castSpell(1, "Lightning Bolt", cat).error shouldBe null
            game.resolveStack()

            withClue("the Cat died") { game.isInGraveyard(2, "Garrison Cat") shouldBe true }
            val tokens = game.findPermanents("Human Soldier Token")
            withClue("its dies trigger created exactly one Human Soldier token") { tokens.size shouldBe 1 }
            withClue("the token belongs to the Cat's controller") {
                game.state.projectedState.getController(tokens.single()) shouldBe game.player2Id
            }
        }
    }
}
