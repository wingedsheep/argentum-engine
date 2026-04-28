package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class SteamVentsScenarioTest : ScenarioTestBase() {

    init {
        context("Steam Vents") {
            test("enters untapped when player pays 2 life") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Steam Vents")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Steam Vents"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Steam Vents should pause for yes/no decision") {
                    result.isPaused shouldBe true
                }

                // Answer yes to pay 2 life
                game.answerYesNo(true)

                val permanentId = game.findPermanent("Steam Vents")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Steam Vents should enter untapped when life is paid") {
                    isTapped shouldBe false
                }

                withClue("Player should have lost 2 life") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("enters tapped when player declines to pay life") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Steam Vents")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Steam Vents"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                withClue("Playing Steam Vents should pause for yes/no decision") {
                    result.isPaused shouldBe true
                }

                // Answer no to decline paying life
                game.answerYesNo(false)

                val permanentId = game.findPermanent("Steam Vents")!!
                val isTapped = game.state.getEntity(permanentId)?.get<TappedComponent>() != null
                withClue("Steam Vents should enter tapped when life is not paid") {
                    isTapped shouldBe true
                }

                withClue("Player should not have lost any life") {
                    game.getLifeTotal(1) shouldBe 20
                }
            }

            test("has basic land types Island and Mountain") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Steam Vents")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Steam Vents"
                }!!

                val result = game.execute(PlayLand(game.player1Id, cardId))
                game.answerYesNo(true)

                val permanentId = game.findPermanent("Steam Vents")!!
                val cardComponent = game.state.getEntity(permanentId)?.get<CardComponent>()!!
                withClue("Steam Vents should have Island subtype") {
                    cardComponent.typeLine.subtypes.any { it.value == "Island" } shouldBe true
                }
                withClue("Steam Vents should have Mountain subtype") {
                    cardComponent.typeLine.subtypes.any { it.value == "Mountain" } shouldBe true
                }
            }

            test("can be tapped manually for {U}") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Steam Vents", tapped = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val landId = game.findPermanent("Steam Vents")!!
                val result = game.execute(
                    ActivateAbility(game.player1Id, landId, AbilityId.intrinsicMana(Color.BLUE.symbol))
                )
                withClue("Activating Steam Vents for {U} should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
                withClue("One blue mana should have been added") { pool.blue shouldBe 1 }
                withClue("No red mana should have been added") { pool.red shouldBe 0 }
                withClue("Steam Vents should be tapped after activation") {
                    (game.state.getEntity(landId)?.get<TappedComponent>() != null) shouldBe true
                }
            }

            test("can be tapped manually for {R}") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Steam Vents", tapped = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val landId = game.findPermanent("Steam Vents")!!
                val result = game.execute(
                    ActivateAbility(game.player1Id, landId, AbilityId.intrinsicMana(Color.RED.symbol))
                )
                withClue("Activating Steam Vents for {R} should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                val pool = game.state.getEntity(game.player1Id)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
                withClue("One red mana should have been added") { pool.red shouldBe 1 }
                withClue("No blue mana should have been added") { pool.blue shouldBe 0 }
            }

            test("rejects intrinsic colors not granted by its subtypes") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Steam Vents", tapped = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val landId = game.findPermanent("Steam Vents")!!
                val result = game.execute(
                    ActivateAbility(game.player1Id, landId, AbilityId.intrinsicMana(Color.WHITE.symbol))
                )
                withClue("Steam Vents has no Plains subtype, so {W} should be rejected") {
                    result.error shouldNotBe null
                }
            }

            test("uses a land drop") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Steam Vents")
                    .withCardInHand(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cardId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Steam Vents"
                }!!

                game.execute(PlayLand(game.player1Id, cardId))
                game.answerYesNo(false)

                // Try to play another land - should fail
                val islandId = game.state.getHand(game.player1Id).find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Island"
                }!!

                val result2 = game.execute(PlayLand(game.player1Id, islandId))
                withClue("Should not be able to play a second land") {
                    result2.error shouldNotBe null
                }
            }
        }
    }
}
