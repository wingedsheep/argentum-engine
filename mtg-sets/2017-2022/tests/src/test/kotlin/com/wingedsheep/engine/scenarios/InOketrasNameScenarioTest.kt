package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * In Oketra's Name (AKH #19) — {1}{W} Instant.
 *
 *   Zombies you control get +2/+1 until end of turn. Other creatures you control get +1/+1 until
 *   end of turn.
 *
 * "Other creatures" are the non-Zombies: each creature you control gets exactly one bonus, and
 * opponents' creatures get neither.
 */
class InOketrasNameScenarioTest : ScenarioTestBase() {

    init {
        context("In Oketra's Name") {

            test("Zombies get +2/+1, other creatures you control +1/+1, opponents' creatures nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "In Oketra's Name")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardOnBattlefield(1, "Binding Mummy") // 2/2 Zombie
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 non-Zombie
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val mummy = game.findPermanent("Binding Mummy")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "In Oketra's Name").error shouldBe null
                game.resolveStack()

                val projected = game.state.projectedState
                withClue("the Zombie gets only +2/+1, not also +1/+1") {
                    projected.getPower(mummy) shouldBe 4
                    projected.getToughness(mummy) shouldBe 3
                }
                withClue("the non-Zombie gets +1/+1") {
                    projected.getPower(bears) shouldBe 3
                    projected.getToughness(bears) shouldBe 3
                }
                withClue("the opponent's creature is untouched") {
                    projected.getPower(giant) shouldBe 3
                    projected.getToughness(giant) shouldBe 3
                }
            }
        }
    }
}
