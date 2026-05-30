package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Saproling Symbiosis.
 *
 * Saproling Symbiosis: {3}{G}
 * Sorcery
 * You may cast this spell as though it had flash if you pay {2} more to cast it.
 * Create a 1/1 green Saproling creature token for each creature you control.
 */
class SaprolingSymbiosisScenarioTest : ScenarioTestBase() {

    init {
        context("Saproling Symbiosis") {

            test("creates one Saproling for each creature you control") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Willow Dryad")
                    .withCardOnBattlefield(1, "Jungle Lion")
                    // The opponent controls a creature too — it must NOT be counted ("each
                    // creature you control"), so the token count stays at 2.
                    .withCardOnBattlefield(2, "Jungle Lion")
                    .withCardInHand(1, "Saproling Symbiosis")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("three creatures in play before casting (two yours, one opponent's)") {
                    (game.findPermanents("Willow Dryad").size + game.findPermanents("Jungle Lion").size) shouldBe 3
                }

                val castResult = game.castSpell(1, "Saproling Symbiosis")
                withClue("cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                withClue("two creatures YOU control → two Saproling tokens (opponent's creature and the tokens are not counted)") {
                    game.findPermanents("Saproling Token").size shouldBe 2
                }
            }
        }
    }
}
