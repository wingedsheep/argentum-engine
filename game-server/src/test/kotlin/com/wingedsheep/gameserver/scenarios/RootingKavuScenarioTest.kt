package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Rooting Kavu's death trigger:
 * "When this creature dies, you may exile it. If you do, shuffle all creature cards
 * from your graveyard into your library."
 *
 * Uses Path of Peace ("Destroy target creature. Its owner gains 4 life.") to kill it.
 */
class RootingKavuScenarioTest : ScenarioTestBase() {

    init {
        context("Rooting Kavu death trigger") {

            test("exiles itself and shuffles creature cards from graveyard into library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rooting Kavu")
                    .withCardInGraveyard(1, "Grizzly Bears")   // creature card -> library
                    .withCardInGraveyard(1, "Raging Goblin")   // creature card -> library
                    .withCardInGraveyard(1, "Path of Peace")   // non-creature -> stays
                    .withCardInHand(2, "Path of Peace")
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)
                val kavuId = game.findPermanent("Rooting Kavu")!!

                // Opponent destroys Rooting Kavu.
                val castResult = game.castSpell(2, "Path of Peace", kavuId)
                withClue("Path of Peace should cast: ${castResult.error}") {
                    castResult.error shouldBe null
                }
                game.resolveStack()

                // The death trigger is a "may" — accept it.
                withClue("Rooting Kavu's death trigger should prompt a yes/no choice") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)
                game.resolveStack()

                withClue("Rooting Kavu should be exiled (not in graveyard)") {
                    game.isInGraveyard(1, "Rooting Kavu") shouldBe false
                }
                withClue("Two creature cards shuffled from graveyard into library") {
                    game.librarySize(1) shouldBe libraryBefore + 2
                }
                withClue("Grizzly Bears (creature) should leave the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
                withClue("Raging Goblin (creature) should leave the graveyard") {
                    game.isInGraveyard(1, "Raging Goblin") shouldBe false
                }
                withClue("Path of Peace (non-creature) should remain in graveyard") {
                    game.isInGraveyard(1, "Path of Peace") shouldBe true
                }
            }

            test("declining the trigger leaves it in the graveyard and graveyard intact") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Rooting Kavu")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(2, "Path of Peace")
                    .withLandsOnBattlefield(2, "Plains", 4)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val libraryBefore = game.librarySize(1)
                val kavuId = game.findPermanent("Rooting Kavu")!!

                game.castSpell(2, "Path of Peace", kavuId)
                game.resolveStack()

                game.answerYesNo(false)
                game.resolveStack()

                withClue("Rooting Kavu stays in the graveyard when declined") {
                    game.isInGraveyard(1, "Rooting Kavu") shouldBe true
                }
                withClue("Grizzly Bears stays in graveyard when declined") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }
                withClue("Library unchanged when declined") {
                    game.librarySize(1) shouldBe libraryBefore
                }
            }
        }
    }
}
