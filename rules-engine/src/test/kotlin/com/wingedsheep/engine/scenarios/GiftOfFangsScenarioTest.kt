package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Gift of Fangs (VOW #113) — {B} Enchantment — Aura.
 *
 *   Enchant creature
 *   Enchanted creature gets +2/+2 as long as it's a Vampire. Otherwise, it gets -2/-2.
 *
 * Exercises both conditional static branches: attached to a Vampire it's a +2/+2 buff, attached
 * to a non-Vampire it's a -2/-2 debuff.
 */
class GiftOfFangsScenarioTest : ScenarioTestBase() {

    init {
        context("Gift of Fangs") {

            test("enchanting a Vampire grants +2/+2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Belligerent Guest", summoningSickness = false) // 3/2 Vampire
                    .withCardAttachedTo(1, "Gift of Fangs", "Belligerent Guest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val guest = game.findPermanent("Belligerent Guest")!!

                withClue("3/2 base Vampire + 2/2 = 5/4") {
                    game.state.projectedState.getPower(guest) shouldBe 5
                    game.state.projectedState.getToughness(guest) shouldBe 4
                }
            }

            test("enchanting a non-Vampire grants -2/-2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hill Giant", summoningSickness = false) // 3/3 Giant
                    .withCardAttachedTo(1, "Gift of Fangs", "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                withClue("3/3 base Giant - 2/2 = 1/1 (alive, not lethal)") {
                    game.state.projectedState.getPower(giant) shouldBe 1
                    game.state.projectedState.getToughness(giant) shouldBe 1
                }
            }
        }
    }
}
