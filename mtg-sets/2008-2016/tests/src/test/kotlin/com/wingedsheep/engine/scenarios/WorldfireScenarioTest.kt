package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Worldfire (M13 #158).
 *
 * "Exile all permanents. Exile all cards from all hands and graveyards. Each player's life total
 *  becomes 1."
 *
 * Every permanent (both players', lands included) is exiled, every hand and graveyard is emptied into
 * exile, and both life totals become 1. Worldfire itself is on the stack while it resolves, so it is
 * not exiled — it lands in its owner's graveyard afterwards.
 */
class WorldfireScenarioTest : ScenarioTestBase() {

    init {
        context("Worldfire") {

            test("exiles all permanents, hands and graveyards; each life total becomes 1") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Worldfire")
                    .withCardInHand(1, "Shock")
                    .withLandsOnBattlefield(1, "Mountain", 9)
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(2, "Island", 2)
                    .withCardInHand(2, "Cancel")
                    .withCardInGraveyard(2, "Giant Growth")
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Worldfire").error shouldBe null
                game.resolveStack()

                withClue("All permanents were exiled") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isOnBattlefield("Mountain") shouldBe false
                    game.isOnBattlefield("Island") shouldBe false
                    game.isInExile(1, "Grizzly Bears") shouldBe true
                    game.isInExile(1, "Mountain") shouldBe true
                    game.isInExile(2, "Hill Giant") shouldBe true
                    game.isInExile(2, "Island") shouldBe true
                }
                withClue("Every hand and graveyard was exiled") {
                    game.handSize(1) shouldBe 0
                    game.handSize(2) shouldBe 0
                    game.isInExile(1, "Shock") shouldBe true
                    game.isInExile(1, "Lightning Bolt") shouldBe true
                    game.isInExile(2, "Cancel") shouldBe true
                    game.isInExile(2, "Giant Growth") shouldBe true
                }
                withClue("Worldfire was on the stack, so it goes to the graveyard rather than exile") {
                    game.isInGraveyard(1, "Worldfire") shouldBe true
                    game.isInExile(1, "Worldfire") shouldBe false
                    game.graveyardSize(1) shouldBe 1
                    game.graveyardSize(2) shouldBe 0
                }
                withClue("Each player's life total became 1") {
                    game.getLifeTotal(1) shouldBe 1
                    game.getLifeTotal(2) shouldBe 1
                }
            }
        }
    }
}
