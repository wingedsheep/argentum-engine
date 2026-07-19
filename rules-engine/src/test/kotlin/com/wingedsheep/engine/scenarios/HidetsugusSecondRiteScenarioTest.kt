package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Hidetsugu's Second Rite (canonical SOK #102, reprinted FDN #202) —
 * {3}{R} Instant.
 *
 * "If target player has exactly 10 life, Hidetsugu's Second Rite deals 10 damage to that player."
 *
 * The spell always targets a player; the "exactly 10 life" check is a resolution-time condition
 * (not an intervening-if that fizzles). The two tests pin both branches: a target at exactly 10
 * takes 10 (→ 0), and a target at any other life total takes nothing.
 */
class HidetsugusSecondRiteScenarioTest : ScenarioTestBase() {

    init {
        context("Hidetsugu's Second Rite") {

            test("deals 10 damage when the targeted player has exactly 10 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hidetsugu's Second Rite")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLifeTotal(2, 10)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Hidetsugu's Second Rite", 2).error shouldBe null
                game.resolveStack()

                withClue("A player at exactly 10 life takes 10 damage → 0") {
                    game.getLifeTotal(2) shouldBe 0
                }
            }

            test("deals no damage when the targeted player's life is not exactly 10") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Hidetsugu's Second Rite")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withLifeTotal(2, 11)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellTargetingPlayer(1, "Hidetsugu's Second Rite", 2).error shouldBe null
                game.resolveStack()

                withClue("11 life is not exactly 10 — the spell resolves doing nothing") {
                    game.getLifeTotal(2) shouldBe 11
                }
            }
        }
    }
}
