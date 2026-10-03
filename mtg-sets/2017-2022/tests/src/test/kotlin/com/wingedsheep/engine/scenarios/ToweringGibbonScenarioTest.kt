package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Towering Gibbon (J22 #46) — {3}{G} Creature — Ape, * /4, Reach.
 *
 *   Towering Gibbon's power is equal to the greatest mana value among creatures you control.
 *
 * The Gibbon counts itself (mana value 4), a bigger creature you control raises it, and an
 * opponent's creature does not.
 */
class ToweringGibbonScenarioTest : ScenarioTestBase() {

    init {
        context("Towering Gibbon") {

            test("alone, its power is its own mana value") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Towering Gibbon")
                    .withCardOnBattlefield(2, "Bramble Wurm") // mana value 7, but the opponent's
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gibbon = game.findPermanent("Towering Gibbon")!!
                withClue("only creatures you control count — the Gibbon itself is mana value 4") {
                    game.state.projectedState.getPower(gibbon) shouldBe 4
                    game.state.projectedState.getToughness(gibbon) shouldBe 4
                }
            }

            test("a higher mana value creature you control sets its power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Towering Gibbon")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Bramble Wurm") // mana value 7
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val gibbon = game.findPermanent("Towering Gibbon")!!
                withClue("the greatest mana value among your creatures is Bramble Wurm's 7") {
                    game.state.projectedState.getPower(gibbon) shouldBe 7
                }
            }
        }
    }
}
