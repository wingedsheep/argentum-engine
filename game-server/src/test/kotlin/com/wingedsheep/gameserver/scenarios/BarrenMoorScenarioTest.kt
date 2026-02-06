package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class BarrenMoorScenarioTest : ScenarioTestBase() {

    init {
        context("Barren Moor") {
            test("enters the battlefield tapped when played as a land") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Barren Moor")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Find the card in hand
                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Barren Moor"
                }!!

                // Play the land
                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Barren Moor should succeed") {
                    result.error shouldBe null
                }

                // Verify it's on the battlefield
                withClue("Barren Moor should be on battlefield") {
                    game.isOnBattlefield("Barren Moor") shouldBe true
                }

                // Verify it entered tapped
                val permanentId = game.findPermanent("Barren Moor")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Barren Moor should enter tapped") {
                    isTapped shouldBe true
                }
            }

            test("can be cycled from hand") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Barren Moor")
                    .withLandsOnBattlefield(1, "Swamp", 1) // To pay cycling cost {B}
                    .withCardInLibrary(1, "Mountain") // Card to draw
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.cycleCard(1, "Barren Moor")
                withClue("Cycling Barren Moor should succeed") {
                    cycleResult.error shouldBe null
                }

                withClue("Barren Moor should be in graveyard after cycling") {
                    game.isInGraveyard(1, "Barren Moor") shouldBe true
                }

                withClue("Should have drawn a card from cycling") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("cycling triggers Astral Slide") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Astral Slide")
                    .withCardInHand(1, "Barren Moor")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle Barren Moor - should trigger Astral Slide
                game.cycleCard(1, "Barren Moor")

                withClue("Astral Slide should trigger - pending target selection") {
                    game.hasPendingDecision() shouldBe true
                }
            }
        }
    }
}
