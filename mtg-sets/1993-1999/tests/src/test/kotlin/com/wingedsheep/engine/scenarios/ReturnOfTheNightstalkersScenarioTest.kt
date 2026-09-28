package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Return of the Nightstalkers (Portal Second Age).
 *
 * Oracle: "Return all Nightstalker permanent cards from your graveyard to the battlefield.
 * Then destroy all Swamps you control."
 */
class ReturnOfTheNightstalkersScenarioTest : ScenarioTestBase() {

    init {
        context("Return of the Nightstalkers") {

            test("returns your Nightstalkers, leaves other cards, then destroys only your Swamps") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Return of the Nightstalkers")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withLandsOnBattlefield(2, "Swamp", 1)
                    .withCardInGraveyard(1, "Abyssal Nightstalker")
                    .withCardInGraveyard(1, "Brutal Nightstalker")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Raiding Nightstalker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Return of the Nightstalkers")
                game.resolveStack()

                game.isOnBattlefield("Abyssal Nightstalker") shouldBe true
                game.isOnBattlefield("Brutal Nightstalker") shouldBe true
                withClue("a non-Nightstalker stays in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("only cards from your own graveyard return") {
                    game.isInGraveyard(2, "Raiding Nightstalker") shouldBe true
                }
                withClue("your Swamps are destroyed, the opponent's is not") {
                    val swamps = game.findPermanents("Swamp")
                    swamps.size shouldBe 1
                    game.state.getBattlefield(game.player2Id).contains(swamps.single()) shouldBe true
                }
                game.isOnBattlefield("Plains") shouldBe true
            }
        }
    }
}
