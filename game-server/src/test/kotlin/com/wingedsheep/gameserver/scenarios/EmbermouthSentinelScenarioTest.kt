package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Embermouth Sentinel (TDM #242).
 *
 * "When this creature enters, you may search your library for a basic land card, reveal it,
 *  then shuffle and put that card on top. If you control a Dragon, put that card onto the
 *  battlefield tapped instead."
 *
 * Verifies both branches of the conditional ETB search:
 *  - no Dragon controlled → the found basic land returns to the library,
 *  - a Dragon controlled → the found basic land enters the battlefield tapped.
 */
class EmbermouthSentinelScenarioTest : ScenarioTestBase() {

    init {
        // A simple Dragon to satisfy "If you control a Dragon".
        cardRegistry.register(
            CardDefinition.creature(
                name = "Test Dragon",
                manaCost = ManaCost.parse("{4}{R}"),
                subtypes = setOf(Subtype("Dragon")),
                power = 4,
                toughness = 4
            )
        )

        context("Embermouth Sentinel") {

            test("no Dragon: searched basic land goes on top of library") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Embermouth Sentinel")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Embermouth Sentinel")
                withClue("Casting Embermouth Sentinel should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                // ETB prompts to search for a basic land; pick the Island.
                if (game.hasPendingDecision()) {
                    val decision = game.getPendingDecision()!!
                    val options = (decision as? com.wingedsheep.engine.core.SelectCardsDecision)?.options
                        ?: emptyList()
                    if (options.isNotEmpty()) {
                        game.selectCards(listOf(options.first()))
                    } else {
                        game.skipSelection()
                    }
                    game.resolveStack()
                }

                withClue("Embermouth Sentinel should be on the battlefield") {
                    game.isOnBattlefield("Embermouth Sentinel") shouldBe true
                }
                withClue("Without a Dragon, the Island should not be on the battlefield") {
                    game.findPermanents("Island").isEmpty() shouldBe true
                }
                withClue("Without a Dragon, the Island returns to the library") {
                    game.findCardsInLibrary(1, "Island").size shouldBe 1
                }
            }

            test("with a Dragon: searched basic land enters the battlefield tapped") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Embermouth Sentinel")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardOnBattlefield(1, "Test Dragon")
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Embermouth Sentinel")
                withClue("Casting Embermouth Sentinel should succeed: ${cast.error}") {
                    cast.error shouldBe null
                }
                game.resolveStack()

                if (game.hasPendingDecision()) {
                    val decision = game.getPendingDecision()!!
                    val options = (decision as? com.wingedsheep.engine.core.SelectCardsDecision)?.options
                        ?: emptyList()
                    if (options.isNotEmpty()) {
                        game.selectCards(listOf(options.first()))
                    } else {
                        game.skipSelection()
                    }
                    game.resolveStack()
                }

                withClue("Embermouth Sentinel should be on the battlefield") {
                    game.isOnBattlefield("Embermouth Sentinel") shouldBe true
                }
                val islandId = game.findPermanents("Island").singleOrNull()
                withClue("With a Dragon, the Island should be on the battlefield") {
                    (islandId != null) shouldBe true
                }
                withClue("The Island should enter tapped") {
                    (game.state.getEntity(islandId!!)?.get<TappedComponent>() != null) shouldBe true
                }
                withClue("The Island should no longer be in the library") {
                    game.findCardsInLibrary(1, "Island").size shouldBe 0
                }
            }
        }
    }
}
