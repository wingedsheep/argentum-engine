package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class TranquilThicketScenarioTest : ScenarioTestBase() {

    init {
        context("Tranquil Thicket") {
            test("enters the battlefield tapped when played as a land") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Tranquil Thicket")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Tranquil Thicket"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Tranquil Thicket should succeed") {
                    result.error shouldBe null
                }

                withClue("Tranquil Thicket should be on battlefield") {
                    game.isOnBattlefield("Tranquil Thicket") shouldBe true
                }

                val permanentId = game.findPermanent("Tranquil Thicket")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Tranquil Thicket should enter tapped") {
                    isTapped shouldBe true
                }
            }

            test("can be cycled from hand") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Tranquil Thicket")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.cycleCard(1, "Tranquil Thicket")
                withClue("Cycling Tranquil Thicket should succeed") {
                    cycleResult.error shouldBe null
                }

                withClue("Tranquil Thicket should be in graveyard after cycling") {
                    game.isInGraveyard(1, "Tranquil Thicket") shouldBe true
                }

                withClue("Should have drawn a card from cycling") {
                    game.handSize(1) shouldBe 1
                }
            }
        }
    }
}
