package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Scenario tests for Traveling Botanist. */
class TravelingBotanistScenarioTest : ScenarioTestBase() {

    init {
        context("Traveling Botanist") {
            test("becoming tapped via attacking lets the controller take a top land to hand") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Traveling Botanist", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Traveling Botanist" to 2))
                game.resolveStack()

                // Becoming tapped peeks the top card (a Forest) and offers to take it to hand.
                withClue("Tap trigger should prompt to take the land") {
                    game.hasPendingDecision() shouldBe true
                }
                val decision = game.getPendingDecision() as SelectCardsDecision
                game.selectCards(listOf(decision.options.first()))
                game.resolveStack()

                withClue("The Forest should be in hand") {
                    game.findCardsInHand(1, "Forest").size shouldBe 1
                }
                withClue("The Forest should have left the library") {
                    game.findCardsInLibrary(1, "Forest").size shouldBe 0
                }
            }

            test("declining the land, then binning it, puts it into the graveyard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Traveling Botanist", summoningSickness = false)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Traveling Botanist" to 2))
                game.resolveStack()

                // First prompt: may take the land to hand — decline by selecting nothing.
                withClue("First prompt should be the take-to-hand selection") {
                    game.hasPendingDecision() shouldBe true
                }
                game.skipSelection()
                game.resolveStack()

                // Second prompt: may bin the declined land — accept.
                withClue("Second prompt should be the put-in-graveyard selection") {
                    game.hasPendingDecision() shouldBe true
                }
                val binDecision = game.getPendingDecision() as SelectCardsDecision
                game.selectCards(listOf(binDecision.options.first()))
                game.resolveStack()

                withClue("The Forest should be in the graveyard") {
                    game.findCardsInGraveyard(1, "Forest").size shouldBe 1
                }
                withClue("The Forest should not be in hand") {
                    game.findCardsInHand(1, "Forest").size shouldBe 0
                }
            }

            test("a non-land top card still offers the put-into-graveyard choice") {
                // Oracle: "If you don't put the card into your hand, you may put it into your
                // graveyard." "the card" is the looked-at top card, land or not — so a non-land
                // top card (which can never go to hand) must still get the graveyard option.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Traveling Botanist", summoningSickness = false)
                    .withCardInLibrary(1, "Sagu Wildling")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Traveling Botanist" to 2))
                game.resolveStack()

                // No land to take, so the hand step is a silent no-op; the first prompt is the
                // put-into-graveyard choice for the non-land top card.
                withClue("A non-land top card should still prompt the put-into-graveyard choice") {
                    game.hasPendingDecision() shouldBe true
                }
                val binDecision = game.getPendingDecision() as SelectCardsDecision
                withClue("The non-land card should be the bin option") {
                    binDecision.options.size shouldBe 1
                }
                game.selectCards(listOf(binDecision.options.first()))
                game.resolveStack()

                withClue("The Sagu Wildling should be in the graveyard") {
                    game.findCardsInGraveyard(1, "Sagu Wildling").size shouldBe 1
                }
                withClue("The Sagu Wildling should have left the library") {
                    game.findCardsInLibrary(1, "Sagu Wildling").size shouldBe 0
                }
            }

            test("declining a non-land top card leaves it on top of the library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Traveling Botanist", summoningSickness = false)
                    .withCardInLibrary(1, "Sagu Wildling")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Traveling Botanist" to 2))
                game.resolveStack()

                withClue("The non-land top card should prompt the put-into-graveyard choice") {
                    game.hasPendingDecision() shouldBe true
                }
                game.skipSelection()
                game.resolveStack()

                withClue("Declining leaves the Sagu Wildling in the library") {
                    game.findCardsInLibrary(1, "Sagu Wildling").size shouldBe 1
                }
                withClue("The Sagu Wildling should not be in the graveyard") {
                    game.findCardsInGraveyard(1, "Sagu Wildling").size shouldBe 0
                }
            }
        }
    }
}
