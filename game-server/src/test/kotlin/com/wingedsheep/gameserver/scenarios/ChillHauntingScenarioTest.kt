package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.gameserver.session.GameSession
import com.wingedsheep.gameserver.session.PlayerSession
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.every
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * Scenario tests for Chill Haunting.
 *
 * Chill Haunting:
 * - {1}{B} Instant
 * - "As an additional cost to cast this spell, exile X creature cards from your graveyard.
 *    Target creature gets -X/-X until end of turn."
 */
class ChillHauntingScenarioTest : ScenarioTestBase() {

    init {
        context("Chill Haunting exile cost and effect") {
            test("Exiling 2 creature cards gives target creature -2/-2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Chill Haunting")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Glory Seeker")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellWithExileCost(
                    1, "Chill Haunting",
                    listOf("Grizzly Bears", "Glory Seeker"),
                    "Hill Giant"
                )
                withClue("Chill Haunting should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Exiled cards should no longer be in graveyard
                withClue("Grizzly Bears should no longer be in graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe false
                }
                withClue("Glory Seeker should no longer be in graveyard") {
                    game.isInGraveyard(1, "Glory Seeker") shouldBe false
                }

                // Resolve the spell
                game.resolveStack()

                // Hill Giant (3/3) should get -2/-2, becoming 1/1
                val hillGiantId = game.findPermanent("Hill Giant")
                hillGiantId.shouldNotBeNull()
                val clientState = game.getClientState(2)
                val hillGiantInfo = clientState.cards[hillGiantId]
                withClue("Hill Giant should be 1/1 after -2/-2") {
                    hillGiantInfo.shouldNotBeNull()
                    hillGiantInfo.power shouldBe 1
                    hillGiantInfo.toughness shouldBe 1
                }
            }

            test("Exiling 1 creature card gives target creature -1/-1") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Chill Haunting")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellWithExileCost(
                    1, "Chill Haunting",
                    listOf("Grizzly Bears"),
                    "Hill Giant"
                )
                withClue("Chill Haunting should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Hill Giant (3/3) should get -1/-1, becoming 2/2
                val hillGiantId = game.findPermanent("Hill Giant")
                hillGiantId.shouldNotBeNull()
                val clientState = game.getClientState(2)
                val hillGiantInfo = clientState.cards[hillGiantId]
                withClue("Hill Giant should be 2/2 after -1/-1") {
                    hillGiantInfo.shouldNotBeNull()
                    hillGiantInfo.power shouldBe 2
                    hillGiantInfo.toughness shouldBe 2
                }
            }

            test("Exiling enough creature cards kills the target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Chill Haunting")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Glory Seeker")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val castResult = game.castSpellWithExileCost(
                    1, "Chill Haunting",
                    listOf("Grizzly Bears", "Glory Seeker"),
                    "Glory Seeker"
                )
                withClue("Chill Haunting should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Glory Seeker (2/2) should get -2/-2 and die
                withClue("Glory Seeker should be dead") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                }
            }

            test("Cannot cast Chill Haunting without creature cards in graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Chill Haunting")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val session = GameSession(cardRegistry = cardRegistry)
                val mockWs1 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws1" }
                val mockWs2 = mockk<WebSocketSession>(relaxed = true) { every { id } returns "ws2" }
                val player1Session = PlayerSession(mockWs1, game.player1Id, "Player1")
                val player2Session = PlayerSession(mockWs2, game.player2Id, "Player2")
                session.injectStateForTesting(
                    game.state,
                    mapOf(game.player1Id to player1Session, game.player2Id to player2Session)
                )

                val legalActions = session.getLegalActions(game.player1Id)
                val chillHauntingAction = legalActions.find { it.description == "Cast Chill Haunting" }
                withClue("Chill Haunting should NOT be castable without creature cards in graveyard") {
                    chillHauntingAction.shouldBeNull()
                }
            }
        }
    }
}
