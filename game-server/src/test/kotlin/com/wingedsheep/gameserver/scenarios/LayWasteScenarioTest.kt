package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario test for Lay Waste.
 *
 * Card reference:
 * - Lay Waste (3R): Sorcery
 *   "Destroy target land."
 *   Cycling {2}
 */
class LayWasteScenarioTest : ScenarioTestBase() {

    init {
        context("Lay Waste - destroy target land") {
            test("can target and destroy an opponent's land") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lay Waste")
                    .withCardOnBattlefield(2, "Forest")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!

                // Cast Lay Waste targeting the opponent's Forest
                val castResult = game.castSpell(1, "Lay Waste", forest)
                withClue("Cast should succeed") {
                    castResult.error shouldBe null
                }

                // Resolve
                game.resolveStack()

                // Forest should be destroyed
                withClue("Forest should no longer be on battlefield") {
                    game.isOnBattlefield("Forest") shouldBe false
                }
                withClue("Forest should be in opponent's graveyard") {
                    game.isInGraveyard(2, "Forest") shouldBe true
                }
            }

            test("can target and destroy own land") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lay Waste")
                    .withCardOnBattlefield(1, "Forest")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val forest = game.findPermanent("Forest")!!

                val castResult = game.castSpell(1, "Lay Waste", forest)
                withClue("Cast should succeed targeting own land") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                withClue("Forest should be destroyed") {
                    game.isOnBattlefield("Forest") shouldBe false
                }
            }

            test("cannot target a creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lay Waste")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                // Try to cast Lay Waste targeting a creature
                val castResult = game.castSpell(1, "Lay Waste", bears)
                withClue("Cast should fail when targeting a creature") {
                    castResult.error shouldNotBe null
                }

                // Grizzly Bears should still be on the battlefield
                withClue("Grizzly Bears should still be on battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }
        }

        context("Lay Waste - cycling") {
            test("can cycle Lay Waste to draw a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Lay Waste")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Should start with 1 card in hand") {
                    game.handSize(1) shouldBe 1
                }

                val cycleResult = game.cycleCard(1, "Lay Waste")
                withClue("Cycling should succeed") {
                    cycleResult.error shouldBe null
                }

                withClue("Lay Waste should be in graveyard") {
                    game.isInGraveyard(1, "Lay Waste") shouldBe true
                }
                withClue("Should have drawn a card") {
                    game.handSize(1) shouldBe 1
                }
            }
        }
    }
}
