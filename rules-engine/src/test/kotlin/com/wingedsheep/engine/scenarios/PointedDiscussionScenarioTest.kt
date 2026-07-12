package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Pointed Discussion (VOW #126) — {2}{B} Sorcery.
 *
 *   You draw two cards, lose 2 life, then create a Blood token.
 *
 * Exercises the composite draw/lose-life/create-token effect: resolving the sorcery draws two
 * cards, costs 2 life, and leaves exactly one Blood token on the battlefield.
 */
class PointedDiscussionScenarioTest : ScenarioTestBase() {

    init {
        context("Pointed Discussion — draw two, lose 2 life, create a Blood token") {

            test("resolves by drawing two cards, losing 2 life, and creating a Blood token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Pointed Discussion")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Mountain")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val handBefore = game.handSize(1)

                game.castSpell(1, "Pointed Discussion").error shouldBe null
                game.resolveStack()

                withClue("hand grows by two cards drawn minus the spell cast (net +1)") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                withClue("the library is depleted by the two cards drawn") {
                    game.librarySize(1) shouldBe 0
                }
                withClue("the caster loses 2 life (20 -> 18)") {
                    game.getLifeTotal(1) shouldBe 18
                }
                withClue("exactly one Blood token is created") {
                    game.findPermanents("Blood").size shouldBe 1
                }
            }
        }
    }
}
