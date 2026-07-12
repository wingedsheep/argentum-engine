package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Grisly Ritual (VOW #116) — {5}{B} Sorcery.
 *
 *   Destroy target creature or planeswalker. Create two Blood tokens.
 *
 * Exercises the destroy + Blood-token composite against a creature target. Note: the target
 * filter also allows planeswalkers, but no planeswalker test card is registered in the shared
 * test pool, so that branch of `Targets.CreatureOrPlaneswalker` is not exercised here.
 */
class GrislyRitualScenarioTest : ScenarioTestBase() {

    init {
        context("Grisly Ritual") {

            test("destroys the target creature and creates two Blood tokens") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Grisly Ritual")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withLandsOnBattlefield(1, "Swamp", 6)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giant = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Grisly Ritual", targetId = giant).error shouldBe null
                game.resolveStack()

                withClue("Hill Giant is destroyed") {
                    game.isOnBattlefield("Hill Giant") shouldBe false
                    game.isInGraveyard(2, "Hill Giant") shouldBe true
                }
                withClue("two Blood tokens are created") {
                    game.findPermanents("Blood").size shouldBe 2
                }
            }
        }
    }
}
