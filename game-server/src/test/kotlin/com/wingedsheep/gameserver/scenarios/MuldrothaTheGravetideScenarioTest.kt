package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Muldrotha, the Gravetide.
 *
 * Card reference:
 * - Muldrotha, the Gravetide ({3}{B}{G}{U}): Legendary Creature — Elemental Avatar 6/6
 *   During each of your turns, you may play a land and cast a permanent spell of each
 *   permanent type from your graveyard.
 */
class MuldrothaTheGravetideScenarioTest : ScenarioTestBase() {

    init {
        context("Muldrotha, the Gravetide - cast creatures from graveyard") {

            test("can cast a creature from graveyard with Muldrotha on battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Muldrotha, the Gravetide")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellFromGraveyard(1, "Llanowar Elves")
                game.resolveStack()

                game.isOnBattlefield("Llanowar Elves") shouldBe true
                game.isInGraveyard(1, "Llanowar Elves") shouldBe false
            }

            test("cannot cast a second creature from graveyard in same turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Muldrotha, the Gravetide")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withCardInGraveyard(1, "Serra Angel")
                    .withLandsOnBattlefield(1, "Forest", 6)
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast first creature from graveyard - should succeed
                game.castSpellFromGraveyard(1, "Llanowar Elves")
                game.resolveStack()

                game.isOnBattlefield("Llanowar Elves") shouldBe true

                // Try to cast second creature from graveyard - should fail
                val result = game.castSpellFromGraveyard(1, "Serra Angel")
                result.error shouldNotBe null
            }
        }

        context("Muldrotha, the Gravetide - cast different permanent types") {

            test("can cast creature and artifact from graveyard in same turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Muldrotha, the Gravetide")
                    .withCardInGraveyard(1, "Llanowar Elves")     // Creature
                    .withCardInGraveyard(1, "Short Sword")        // Artifact (Equipment)
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast creature from graveyard
                game.castSpellFromGraveyard(1, "Llanowar Elves")
                game.resolveStack()
                game.isOnBattlefield("Llanowar Elves") shouldBe true

                // Cast artifact from graveyard (different type, should succeed)
                game.castSpellFromGraveyard(1, "Short Sword")
                game.resolveStack()
                game.isOnBattlefield("Short Sword") shouldBe true
            }
        }

        context("Muldrotha, the Gravetide - play lands from graveyard") {

            test("can play a land from graveyard with Muldrotha on battlefield") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Muldrotha, the Gravetide")
                    .withCardInGraveyard(1, "Forest")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forestId = game.findCardsInGraveyard(1, "Forest").first()
                val result = game.execute(PlayLand(game.player1Id, forestId))

                result.error shouldBe null
                game.isOnBattlefield("Forest") shouldBe true
                game.isInGraveyard(1, "Forest") shouldBe false
            }

            test("playing a land from graveyard uses land drop") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Muldrotha, the Gravetide")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInHand(1, "Island")
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Play the land from graveyard
                val forestId = game.findCardsInGraveyard(1, "Forest").first()
                game.execute(PlayLand(game.player1Id, forestId))

                // Try to play another land from hand - should fail (land drop used)
                val islandId = game.findCardsInHand(1, "Island").first()
                val result = game.execute(PlayLand(game.player1Id, islandId))
                result.error shouldNotBe null
            }
        }

        context("Muldrotha, the Gravetide - without Muldrotha") {

            test("cannot cast a creature from graveyard without Muldrotha") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val result = game.castSpellFromGraveyard(1, "Llanowar Elves")
                result.error shouldNotBe null
            }
        }

        context("Muldrotha, the Gravetide - only on controller's turn") {

            test("cannot cast from graveyard on opponent's turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Muldrotha, the Gravetide")
                    .withCardInGraveyard(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .withActivePlayer(2) // Opponent's turn
                    .build()

                val result = game.castSpellFromGraveyard(1, "Llanowar Elves")
                result.error shouldNotBe null
            }
        }
    }
}
