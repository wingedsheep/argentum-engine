package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Dictate of Heliod (JOU #8) — {3}{W}{W} Enchantment.
 *
 *   Flash
 *   Creatures you control get +2/+2.
 */
class DictateOfHeliodScenarioTest : ScenarioTestBase() {
    init {
        test("flashed in on the opponent's turn, it pumps only your creatures") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Dictate of Heliod")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(2)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("flash allows casting on the opponent's turn") {
                game.castSpell(1, "Dictate of Heliod").error shouldBe null
            }
            game.resolveStack()
            game.isOnBattlefield("Dictate of Heliod") shouldBe true

            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!
            val projected = game.state.projectedState
            withClue("your Bears get +2/+2") {
                projected.getPower(bears) shouldBe 4
                projected.getToughness(bears) shouldBe 4
            }
            withClue("the opponent's Hill Giant is unchanged") {
                projected.getPower(giant) shouldBe 3
                projected.getToughness(giant) shouldBe 3
            }
        }
    }
}
