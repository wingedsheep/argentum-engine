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
            test("returns selected card from graveyard to hand via decision UI") {
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

                // Cast Elven Cache (no targets needed at cast time)
                val castResult = game.castSpell(1, "Elven Cache")
                withClue("Elven Cache should be cast successfully") {
                    castResult.error shouldBe null
                }

                // Resolve the spell — should create a pending decision
                game.resolveStack()

                withClue("There should be a pending decision to select a graveyard card") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Grizzly Bears from the decision
                val bearsInGraveyard = game.findCardsInGraveyard(1, "Grizzly Bears")
                game.selectCards(bearsInGraveyard)

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

                game.castSpell(1, "Elven Cache")
                game.resolveStack()

                withClue("There should be a pending decision") {
                    game.hasPendingDecision() shouldBe true
                }

                val forestInGraveyard = game.findCardsInGraveyard(1, "Forest")
                game.selectCards(forestInGraveyard)

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

                game.castSpell(1, "Elven Cache")
                game.resolveStack()

                withClue("No pending decision when graveyard is empty") {
                    game.hasPendingDecision() shouldBe false
                }

                withClue("Player should be able to pass priority normally") {
                    val passResult = game.passPriority()
                    passResult.error shouldBe null
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

                game.castSpell(1, "Elven Cache")
                game.resolveStack()

                withClue("There should be a pending decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Select Hill Giant specifically
                val hillGiantInGraveyard = game.findCardsInGraveyard(1, "Hill Giant")
                game.selectCards(hillGiantInGraveyard)

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
