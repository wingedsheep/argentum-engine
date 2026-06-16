package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Shatterstorm (ATQ #28).
 *
 * "{2}{R}{R} Sorcery — Destroy all artifacts. They can't be regenerated."
 *
 * Several artifacts controlled by both players are on the battlefield; after Shatterstorm
 * resolves, none remain. Non-artifact permanents (a basic land) are unaffected.
 */
class ShatterstormScenarioTest : ScenarioTestBase() {

    init {
        test("Shatterstorm destroys all artifacts controlled by both players") {
            val game = scenario()
                .withPlayers("Caster", "Defender")
                .withCardInHand(1, "Shatterstorm")
                .withLandsOnBattlefield(1, "Mountain", 4)
                // Player 1's artifacts.
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(1, "Su-Chi")
                // Player 2's artifacts.
                .withCardOnBattlefield(2, "Ornithopter")
                .withCardOnBattlefield(2, "Su-Chi")
                // A non-artifact permanent that must survive.
                .withCardOnBattlefield(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("Setup: four artifacts should be on the battlefield") {
                game.findPermanents("Ornithopter").size shouldBe 2
                game.findPermanents("Su-Chi").size shouldBe 2
            }

            val cast = game.castSpell(1, "Shatterstorm")
            withClue("Casting Shatterstorm should succeed: ${cast.error}") {
                cast.error shouldBe null
            }
            if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
            game.resolveStack()

            withClue("All Ornithopters should be destroyed") {
                game.findPermanents("Ornithopter").size shouldBe 0
            }
            withClue("All Su-Chis should be destroyed") {
                game.findPermanents("Su-Chi").size shouldBe 0
            }
            withClue("The non-artifact land should be unaffected") {
                game.isOnBattlefield("Forest") shouldBe true
            }
        }
    }
}
