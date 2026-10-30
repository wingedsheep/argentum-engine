package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Hampering Snare (IKO #55) — {1}{U} Instant.
 *
 *   "Creatures your opponents control get -2/-0 until end of turn.
 *    Cycling {2}"
 */
class HamperingSnareScenarioTest : ScenarioTestBase() {
    init {
        test("only creatures your opponents control get -2/-0") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Hampering Snare")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")       // 3/3
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val giant = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Hampering Snare").error shouldBe null
            game.resolveStack()

            withClue("the opponent's Hill Giant is 1/3") {
                game.state.projectedState.getPower(giant) shouldBe 1
                game.state.projectedState.getToughness(giant) shouldBe 3
            }
            withClue("your own Grizzly Bears is untouched") {
                game.state.projectedState.getPower(bears) shouldBe 2
            }
        }
    }
}
