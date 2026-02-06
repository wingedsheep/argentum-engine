package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class SecludedSteppeScenarioTest : ScenarioTestBase() {

    init {
        context("Secluded Steppe") {
            test("enters the battlefield tapped when played as a land") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Secluded Steppe")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Secluded Steppe"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Secluded Steppe should succeed") {
                    result.error shouldBe null
                }

                withClue("Secluded Steppe should be on battlefield") {
                    game.isOnBattlefield("Secluded Steppe") shouldBe true
                }

                val permanentId = game.findPermanent("Secluded Steppe")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Secluded Steppe should enter tapped") {
                    isTapped shouldBe true
                }
            }

            test("can be cycled from hand") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Secluded Steppe")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.cycleCard(1, "Secluded Steppe")
                withClue("Cycling Secluded Steppe should succeed") {
                    cycleResult.error shouldBe null
                }

                withClue("Secluded Steppe should be in graveyard after cycling") {
                    game.isInGraveyard(1, "Secluded Steppe") shouldBe true
                }

                withClue("Should have drawn a card from cycling") {
                    game.handSize(1) shouldBe 1
                }
            }
        }
    }
}
