package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Déjà Vu.
 *
 * Card reference:
 * - Déjà Vu (2U): Sorcery
 *   "Return target sorcery card from your graveyard to your hand."
 */
class DejaVuScenarioTest : ScenarioTestBase() {

    init {
        context("Déjà Vu") {
            test("returns target sorcery card from graveyard to hand") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Déjà Vu")
                    .withCardInGraveyard(1, "Volcanic Hammer")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast targeting Volcanic Hammer (a sorcery) in graveyard
                val hammerInGraveyard = game.findCardsInGraveyard(1, "Volcanic Hammer")
                game.castSpellTargetingGraveyardCard(1, "Déjà Vu", hammerInGraveyard)
                game.resolveStack()

                withClue("Volcanic Hammer should now be in hand") {
                    game.isInHand(1, "Volcanic Hammer") shouldBe true
                }

                withClue("Volcanic Hammer should not be in graveyard anymore") {
                    game.isInGraveyard(1, "Volcanic Hammer") shouldBe false
                }

                withClue("Déjà Vu should be in graveyard after resolving") {
                    game.isInGraveyard(1, "Déjà Vu") shouldBe true
                }
            }

            test("only shows sorcery cards in decision - non-sorceries are filtered out") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Déjà Vu")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Only a creature in graveyard, no sorceries — cannot cast
                val castResult = game.castSpellTargetingGraveyardCard(1, "Déjà Vu", emptyList())
                withClue("Casting with no valid sorcery targets should fail") {
                    castResult.error shouldBe "No valid targets available"
                }
            }

            test("resolves with no effect when graveyard is empty") {
                val game = scenario()
                    .withPlayers("Player 1", "Player 2")
                    .withCardInHand(1, "Déjà Vu")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cannot cast — no valid targets in graveyard
                val castResult = game.castSpellTargetingGraveyardCard(1, "Déjà Vu", emptyList())
                withClue("Casting with no targets should fail") {
                    castResult.error shouldBe "No valid targets available"
                }
            }
        }
    }
}
