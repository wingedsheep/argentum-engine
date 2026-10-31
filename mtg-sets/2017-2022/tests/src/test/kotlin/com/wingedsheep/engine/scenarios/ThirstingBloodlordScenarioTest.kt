package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Thirsting Bloodlord (M20 #293) — {2}{B}{B} 3/3 Creature — Vampire.
 *
 *   Other Vampires you control get +1/+1.
 */
class ThirstingBloodlordScenarioTest : ScenarioTestBase() {

    init {
        context("Thirsting Bloodlord") {
            test("pumps your other Vampires, not itself or an opponent's") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Thirsting Bloodlord")
                    .withCardOnBattlefield(1, "Vampire Nighthawk")
                    .withCardOnBattlefield(2, "Bloodthirsty Aerialist")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val p = game.state.projectedState
                val lord = game.findPermanent("Thirsting Bloodlord")!!
                val hawk = game.findPermanent("Vampire Nighthawk")!!
                val theirs = game.findPermanent("Bloodthirsty Aerialist")!!
                p.getPower(lord) shouldBe 3
                p.getToughness(lord) shouldBe 3
                p.getPower(hawk) shouldBe 3
                p.getToughness(hawk) shouldBe 4
                p.getPower(theirs) shouldBe 2
            }
        }
    }
}
