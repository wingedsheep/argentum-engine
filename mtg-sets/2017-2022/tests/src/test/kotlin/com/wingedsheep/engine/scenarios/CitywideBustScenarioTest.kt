package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Citywide Bust (GRN #4) — {1}{W}{W} Sorcery.
 *
 *   Destroy all creatures with toughness 4 or greater.
 */
class CitywideBustScenarioTest : ScenarioTestBase() {

    init {
        test("destroys every creature with toughness 4 or greater on both sides, sparing smaller ones") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Citywide Bust")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withCardOnBattlefield(1, "Aegis Turtle")
                .withCardOnBattlefield(1, "Hill Giant")
                .withCardOnBattlefield(2, "Craw Wurm")
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Citywide Bust").error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Aegis Turtle") shouldBe true
            game.isInGraveyard(2, "Craw Wurm") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
        }
    }
}
