package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Elven Cache.
 *
 * Card reference:
 * - Elven Cache (2GG): Sorcery
 *   "Return target card from your graveyard to your hand."
 */
class ElvenCacheScenarioTest : ScenarioTestBase() {

    init {
        context("Elven Cache") {
            test("returns target card from graveyard to hand") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Elven Cache")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Grizzly Bears should start in graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                // Cast Elven Cache targeting Grizzly Bears in graveyard
                val bearsInGraveyard = game.findCardsInGraveyard(1, "Grizzly Bears")
                val castResult = game.castSpellTargetingGraveyardCard(1, "Elven Cache", bearsInGraveyard)
                withClue("Elven Cache should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                withClue("Grizzly Bears should now be in hand") {
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }

                withClue("Grizzly Bears should not be in graveyard anymore") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }

                withClue("Elven Cache should be in graveyard after resolving") {
                    game.isInGraveyard(1, "Elven Cache") shouldBe true
                }
            }

            test("can target any card type in graveyard, not just creatures") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Elven Cache")
                    .withCardInGraveyard(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forestInGraveyard = game.findCardsInGraveyard(1, "Forest")
                game.castSpellTargetingGraveyardCard(1, "Elven Cache", forestInGraveyard)
                game.resolveStack()

                withClue("Forest should now be in hand") {
                    game.isInHand(1, "Forest") shouldBe true
                }
            }

            test("resolves with no effect when graveyard is empty") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Elven Cache")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cannot cast — no valid targets in graveyard
                val castResult = game.castSpellTargetingGraveyardCard(1, "Elven Cache", emptyList())
                withClue("Casting with no targets should fail") {
                    castResult.error shouldBe "No valid targets available"
                }
            }

            test("can choose from multiple cards in graveyard") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Elven Cache")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Select Hill Giant specifically
                val hillGiantInGraveyard = game.findCardsInGraveyard(1, "Hill Giant")
                game.castSpellTargetingGraveyardCard(1, "Elven Cache", hillGiantInGraveyard)
                game.resolveStack()

                withClue("Hill Giant should now be in hand") {
                    game.isInHand(1, "Hill Giant") shouldBe true
                }

                withClue("Grizzly Bears should still be in graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
            }
        }
    }
}
