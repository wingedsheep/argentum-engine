package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Honored Crop-Captain (AKH #200) — {R}{W} Creature — Human Warrior 3/2.
 *
 *   Whenever this creature attacks, other attacking creatures get +1/+0 until end of turn.
 */
class HonoredCropCaptainScenarioTest : ScenarioTestBase() {

    init {
        context("Honored Crop-Captain") {

            test("other attacking creatures get +1/+0; the Captain and non-attackers don't") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Honored Crop-Captain")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val captain = game.findPermanent("Honored Crop-Captain")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Honored Crop-Captain" to 2, "Grizzly Bears" to 2)).error shouldBe null
                game.resolveStack()

                withClue("the other attacker gets +1/+0") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
                withClue("the Captain itself is excluded") { game.state.projectedState.getPower(captain) shouldBe 3 }
                withClue("a non-attacking creature is excluded") { game.state.projectedState.getPower(giant) shouldBe 3 }
            }
        }
    }
}
