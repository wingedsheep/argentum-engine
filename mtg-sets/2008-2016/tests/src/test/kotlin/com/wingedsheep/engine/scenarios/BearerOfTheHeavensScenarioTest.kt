package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bearer of the Heavens (JOU #89) — {7}{R} Creature — Giant 10/10.
 *
 *   When this creature dies, destroy all permanents at the beginning of the next end step.
 *
 * The wipe is delayed: everything survives until the end step, then every permanent on both sides
 * — lands included — is destroyed.
 */
class BearerOfTheHeavensScenarioTest : ScenarioTestBase() {
    init {
        test("dying schedules a wipe of all permanents at the next end step, not immediately") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Bearer of the Heavens")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardInHand(1, "Murder")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardOnBattlefield(2, "Hill Giant")
                .withLandsOnBattlefield(2, "Mountain", 2)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bearer = game.findPermanent("Bearer of the Heavens")!!
            game.castSpell(1, "Murder", bearer).error shouldBe null
            game.resolveStack()

            withClue("Bearer died") {
                game.isInGraveyard(1, "Bearer of the Heavens") shouldBe true
            }
            withClue("nothing else is destroyed yet") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.isOnBattlefield("Mountain") shouldBe true
            }

            game.passUntilPhase(Phase.ENDING, Step.END)
            game.resolveStack()

            withClue("at the end step every permanent is destroyed, lands included") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(2, "Hill Giant") shouldBe true
                game.isOnBattlefield("Swamp") shouldBe false
                game.isOnBattlefield("Mountain") shouldBe false
            }
        }
    }
}
