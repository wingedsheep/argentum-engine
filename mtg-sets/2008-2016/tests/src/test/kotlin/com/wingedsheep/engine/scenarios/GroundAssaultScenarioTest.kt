package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Ground Assault (GTC) — deals damage to target creature equal to the number of lands you control. */
class GroundAssaultScenarioTest : ScenarioTestBase() {
    init {
        test("three lands deal three damage, killing a 3/3") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Ground Assault")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Ground Assault", giant).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant took 3 damage and died") {
                game.isInGraveyard(2, "Hill Giant") shouldBe true
            }
        }

        test("two lands deal only two damage, so a 3/3 survives") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withLandsOnBattlefield(1, "Forest", 1)
                .withCardOnBattlefield(2, "Hill Giant")
                .withCardInHand(1, "Ground Assault")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Ground Assault", giant).error shouldBe null
            game.resolveStack()

            withClue("Hill Giant survived 2 damage") {
                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
