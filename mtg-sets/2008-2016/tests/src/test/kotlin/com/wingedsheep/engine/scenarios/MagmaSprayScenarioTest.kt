package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Magma Spray (ALA #108) — {R} Instant.
 *
 *   Magma Spray deals 2 damage to target creature. If that creature would die this turn, exile it
 *   instead.
 */
class MagmaSprayScenarioTest : ScenarioTestBase() {
    init {
        context("Magma Spray") {

            test("a creature killed by the damage is exiled instead of dying") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Magma Spray")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Magma Spray", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()

                withClue("Grizzly Bears is exiled, not put into the graveyard") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                    game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                }
            }

            test("a creature that survives the damage stays on the battlefield") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Magma Spray")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Magma Spray", game.findPermanent("Hill Giant")!!).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Hill Giant") shouldBe true
            }
        }
    }
}
