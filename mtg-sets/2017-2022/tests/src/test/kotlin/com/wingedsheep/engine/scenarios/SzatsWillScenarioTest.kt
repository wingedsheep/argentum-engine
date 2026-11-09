package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Szat's Will {4}{B} — Instant (Commander Legends), the second mode's token count in particular:
 * X is the greatest power among creature cards exiled from opponents' graveyards.
 *
 * Mode selection is driven through `castSpellWithMode`; the commander-conditional "both" count is the
 * same `dynamicChooseCount` Jeska's Will uses.
 */
class SzatsWillScenarioTest : ScenarioTestBase() {
    init {
        context("Szat's Will") {
            test("second mode exiles every opponent graveyard and makes X Thrulls, X the greatest creature power exiled") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Szat's Will")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardInGraveyard(2, "Grizzly Bears")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInGraveyard(2, "Lightning Bolt")
                    .withCardInGraveyard(1, "Darksteel Colossus")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithMode(1, "Szat's Will", 1).error shouldBe null
                game.resolveStack()

                withClue("opponent's graveyard exiled") { game.graveyardSize(2) shouldBe 0 }
                withClue("your own graveyard is left alone") { game.graveyardSize(1) shouldBe 2 }
                withClue("Hill Giant's 3 power is the greatest creature card exiled; the Bolt has no power") {
                    game.findPermanents("Thrull Token").size shouldBe 3
                }
            }

            test("first mode: each opponent sacrifices a creature with the greatest power") {
                val game = scenario()
                    .withPlayers("You", "Opponent")
                    .withCardInHand(1, "Szat's Will")
                    .withLandsOnBattlefield(1, "Swamp", 5)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithMode(1, "Szat's Will", 0).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isInGraveyard(2, "Grizzly Bears") shouldBe false
                withClue("your own creatures aren't touched") { game.isOnBattlefield("Grizzly Bears") shouldBe true }
            }
        }
    }
}
