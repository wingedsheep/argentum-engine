package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Warped Tusker (MH3) — "When you cast or cycle Warped Tusker, create a 0/1 colorless Eldrazi
 * Spawn creature token…" plus Cycling {2}{G}.
 */
class WarpedTuskerScenarioTest : ScenarioTestBase() {

    init {
        context("Warped Tusker") {
            test("casting it puts a trigger above the spell and makes a Spawn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Warped Tusker")
                    .withLandsOnBattlefield(1, "Forest", 7)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Warped Tusker").error shouldBe null
                withClue("Cast trigger sits on the stack above the creature spell") {
                    game.state.stack.size shouldBe 2
                }
                game.resolveStack()

                game.findPermanent("Warped Tusker").shouldNotBeNull()
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 1
            }

            test("cycling it draws a card and makes a Spawn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Warped Tusker")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .withPriorityPlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycle = game.cycleCard(1, "Warped Tusker")
                withClue("Cycling should succeed: ${cycle.error}") { cycle.error shouldBe null }
                game.resolveStack()

                game.isInGraveyard(1, "Warped Tusker") shouldBe true
                game.handSize(1) shouldBe 1
                game.findPermanents("Eldrazi Spawn") shouldHaveSize 1
            }
        }
    }
}
