package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class LonelySandbarScenarioTest : ScenarioTestBase() {

    init {
        context("Lonely Sandbar") {
            test("enters the battlefield tapped when played as a land") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Lonely Sandbar")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Lonely Sandbar"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Lonely Sandbar should succeed") {
                    result.error shouldBe null
                }

                withClue("Lonely Sandbar should be on battlefield") {
                    game.isOnBattlefield("Lonely Sandbar") shouldBe true
                }

                val permanentId = game.findPermanent("Lonely Sandbar")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Lonely Sandbar should enter tapped") {
                    isTapped shouldBe true
                }
            }

            test("can be cycled from hand") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Lonely Sandbar")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.cycleCard(1, "Lonely Sandbar")
                withClue("Cycling Lonely Sandbar should succeed") {
                    cycleResult.error shouldBe null
                }

                withClue("Lonely Sandbar should be in graveyard after cycling") {
                    game.isInGraveyard(1, "Lonely Sandbar") shouldBe true
                }

                withClue("Should have drawn a card from cycling") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("cycling triggers Astral Slide") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Astral Slide")
                    .withCardInHand(1, "Lonely Sandbar")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withCardInLibrary(1, "Forest")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Lonely Sandbar")

                withClue("Astral Slide should trigger - pending target selection") {
                    game.hasPendingDecision() shouldBe true
                }
            }
        }
    }
}
