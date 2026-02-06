package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class ForgottenCaveScenarioTest : ScenarioTestBase() {

    init {
        context("Forgotten Cave") {
            test("enters the battlefield tapped when played as a land") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Forgotten Cave")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Forgotten Cave"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Forgotten Cave should succeed") {
                    result.error shouldBe null
                }

                withClue("Forgotten Cave should be on battlefield") {
                    game.isOnBattlefield("Forgotten Cave") shouldBe true
                }

                val permanentId = game.findPermanent("Forgotten Cave")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Forgotten Cave should enter tapped") {
                    isTapped shouldBe true
                }
            }

            test("can be cycled from hand") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Forgotten Cave")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.cycleCard(1, "Forgotten Cave")
                withClue("Cycling Forgotten Cave should succeed") {
                    cycleResult.error shouldBe null
                }

                withClue("Forgotten Cave should be in graveyard after cycling") {
                    game.isInGraveyard(1, "Forgotten Cave") shouldBe true
                }

                withClue("Should have drawn a card from cycling") {
                    game.handSize(1) shouldBe 1
                }
            }

            test("cycling triggers Lightning Rift") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Lightning Rift")
                    .withCardInHand(1, "Forgotten Cave")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Forgotten Cave")

                withClue("Lightning Rift should trigger - pending target selection") {
                    game.hasPendingDecision() shouldBe true
                }
            }
        }
    }
}
