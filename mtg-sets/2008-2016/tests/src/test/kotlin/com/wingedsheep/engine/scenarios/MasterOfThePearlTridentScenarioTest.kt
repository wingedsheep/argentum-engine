package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Master of the Pearl Trident (M13 #59) — {U}{U} 2/2 Creature — Merfolk.
 *   Other Merfolk creatures you control get +1/+1 and have islandwalk.
 */
class MasterOfThePearlTridentScenarioTest : ScenarioTestBase() {
    init {
        test("pumps and grants islandwalk to your other Merfolk only") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Master of the Pearl Trident")
                .withCardOnBattlefield(1, "Coral Merfolk")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Merfolk of the Pearl Trident")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val projected = game.state.projectedState
            val master = game.findPermanent("Master of the Pearl Trident")!!
            val coral = game.findPermanent("Coral Merfolk")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val theirs = game.findPermanent("Merfolk of the Pearl Trident")!!

            withClue("your other Merfolk gets +1/+1 and islandwalk") {
                projected.getPower(coral) shouldBe 3
                projected.getToughness(coral) shouldBe 2
                projected.hasKeyword(coral, Keyword.ISLANDWALK) shouldBe true
            }
            withClue("the Master doesn't buff itself") {
                projected.getPower(master) shouldBe 2
                projected.hasKeyword(master, Keyword.ISLANDWALK) shouldBe false
            }
            withClue("non-Merfolk and opposing Merfolk are untouched") {
                projected.getPower(bears) shouldBe 2
                projected.getPower(theirs) shouldBe 1
                projected.hasKeyword(theirs, Keyword.ISLANDWALK) shouldBe false
            }
        }
    }
}
