package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Grim Guardian (JOU #73) — {2}{B} Enchantment Creature — Zombie 1/4.
 *
 *   Constellation — Whenever this creature or another enchantment you control enters, each
 *   opponent loses 1 life.
 */
class GrimGuardianScenarioTest : ScenarioTestBase() {
    init {
        test("its own entry and another enchantment of yours each drain the opponent") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Grim Guardian")
                .withCardInHand(1, "Glorious Anthem")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Grim Guardian").error shouldBe null
            game.resolveStack()
            withClue("the Guardian's own entry triggers constellation") {
                game.getLifeTotal(2) shouldBe 19
            }

            game.castSpell(1, "Glorious Anthem").error shouldBe null
            game.resolveStack()
            withClue("another enchantment entering triggers it again") {
                game.getLifeTotal(2) shouldBe 18
            }
            game.getLifeTotal(1) shouldBe 20
        }

        test("an enchantment the opponent controls does not trigger it") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Grim Guardian")
                .withCardInHand(2, "Glorious Anthem")
                .withLandsOnBattlefield(2, "Plains", 3)
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(2, "Glorious Anthem").error shouldBe null
            game.resolveStack()

            game.getLifeTotal(1) shouldBe 20
            game.getLifeTotal(2) shouldBe 20
        }
    }
}
