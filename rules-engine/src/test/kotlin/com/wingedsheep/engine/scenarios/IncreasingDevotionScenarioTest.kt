package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Increasing Devotion (DKA #11) — five Human tokens from hand, ten when cast from a graveyard via
 * flashback {7}{W}{W}.
 */
class IncreasingDevotionScenarioTest : ScenarioTestBase() {
    init {
        test("cast from hand creates five 1/1 Human tokens") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Increasing Devotion")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Increasing Devotion").error shouldBe null
            game.resolveStack()

            withClue("5 Plains + 5 Human tokens") {
                game.findPermanents("Human Token").size shouldBe 5
            }
        }

        test("flashback from the graveyard creates ten instead") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInGraveyard(1, "Increasing Devotion")
                .withLandsOnBattlefield(1, "Plains", 9)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val cast = game.castSpellFromGraveyard(1, "Increasing Devotion")
            withClue("flashback {7}{W}{W} off nine Plains: ${cast.error}") { cast.error shouldBe null }
            game.resolveStack()

            withClue("Conditions.WasCastFromGraveyard flips the count to ten") {
                game.findPermanents("Human Token").size shouldBe 10
            }
            withClue("a flashed-back card is exiled, not returned to the graveyard") {
                game.isInGraveyard(1, "Increasing Devotion") shouldBe false
                game.isInExile(1, "Increasing Devotion") shouldBe true
            }
        }
    }
}
